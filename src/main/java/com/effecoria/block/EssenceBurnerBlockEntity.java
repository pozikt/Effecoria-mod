package com.effecoria.block;

import com.effecoria.alchemy.menu.BurnerMenu;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModItemTags;
import com.effecoria.content.ModItems;
import com.effecoria.core.alchemy.HeatLevel;
import com.effecoria.core.alchemy.PhiHeatSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Φ-burner with fuel/catalyst slots, selectable temperature, overheat, and heat broadcast.
 */
public final class EssenceBurnerBlockEntity extends BaseContainerBlockEntity
        implements WorldlyContainer, PhiHeatSource {
    public static final int DUST_FUEL_TICKS = 400;
    public static final int SLOT_FUEL = 0;
    public static final int SLOT_CATALYST = 1;
    public static final int SLOT_COUNT = 2;

    public static final int DATA_FUEL = 0;
    public static final int DATA_TEMP = 1;
    public static final int DATA_OVERHEAT = 2;
    public static final int DATA_LIT = 3;
    public static final int DATA_COUNT = 4;

    public static final int OVERHEAT_THRESHOLD = 200;
    public static final int OVERHEAT_COOLDOWN = 200;

    private static final int[] FUEL_SLOTS = {SLOT_FUEL};
    private static final int[] NO_SLOTS = {};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int fuelTicks;
    private HeatLevel selectedTemp = HeatLevel.MEDIUM;
    private int highTicks;
    private int overheatCooldown;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_FUEL -> fuelTicks;
                case DATA_TEMP -> selectedTemp.ordinal();
                case DATA_OVERHEAT -> overheatCooldown;
                case DATA_LIT -> fuelTicks > 0 && overheatCooldown <= 0 ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_FUEL -> fuelTicks = value;
                case DATA_TEMP -> selectedTemp = HeatLevel.byId(value);
                case DATA_OVERHEAT -> overheatCooldown = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public EssenceBurnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ESSENCE_BURNER.get(), pos, state);
    }

    public ContainerData getData() {
        return data;
    }

    public int fuelTicks() {
        return fuelTicks;
    }

    public void addFuel(int ticks) {
        fuelTicks = Math.min(fuelTicks + ticks, 8000);
        setChanged();
    }

    public HeatLevel selectedTemp() {
        return selectedTemp;
    }

    public void setSelectedTemp(HeatLevel temp) {
        if (temp == HeatLevel.NONE) {
            temp = HeatLevel.LOW;
        }
        selectedTemp = temp;
        setChanged();
    }

    public int overheatCooldown() {
        return overheatCooldown;
    }

    @Override
    public HeatLevel heatLevel() {
        if (fuelTicks <= 0 || overheatCooldown > 0) {
            return HeatLevel.NONE;
        }
        return selectedTemp;
    }

    @Override
    public boolean consumeHeatTick() {
        if (fuelTicks <= 0 || overheatCooldown > 0) {
            return false;
        }
        int drain = drainPerCookTick();
        fuelTicks = Math.max(0, fuelTicks - drain);
        setChanged();
        syncLit();
        return true;
    }

    /** Legacy helper used by older call sites. */
    public boolean consumeFuelTick() {
        return consumeHeatTick();
    }

    private int drainPerCookTick() {
        return switch (selectedTemp) {
            case HIGH -> 3;
            case LOW -> 1;
            default -> 2;
        };
    }

    private int idleDrainInterval() {
        return switch (selectedTemp) {
            case HIGH -> 10;
            case LOW -> 30;
            default -> 20;
        };
    }

    private boolean hasCatalyst() {
        ItemStack stack = items.get(SLOT_CATALYST);
        return !stack.isEmpty() && stack.is(ModItemTags.BURNER_CATALYSTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EssenceBurnerBlockEntity be) {
        boolean changed = false;
        if (be.overheatCooldown > 0) {
            be.overheatCooldown--;
            changed = true;
            if (be.overheatCooldown == 0) {
                be.highTicks = 0;
            }
        }

        if (be.fuelTicks <= 0) {
            be.tryPullFuel();
        }

        if (be.fuelTicks > 0 && be.overheatCooldown <= 0) {
            if (level.getGameTime() % be.idleDrainInterval() == 0) {
                be.fuelTicks = Math.max(0, be.fuelTicks - 1);
                changed = true;
            }
            if (be.selectedTemp == HeatLevel.HIGH && !be.hasCatalyst()) {
                be.highTicks++;
                changed = true;
                if (be.highTicks >= OVERHEAT_THRESHOLD) {
                    be.overheatCooldown = OVERHEAT_COOLDOWN;
                    be.highTicks = 0;
                    level.levelEvent(1501, pos, 0);
                }
            } else {
                be.highTicks = 0;
            }
        } else if (be.fuelTicks <= 0) {
            be.highTicks = 0;
        }

        boolean shouldLit = be.fuelTicks > 0 && be.overheatCooldown <= 0;
        if (state.getValue(EssenceBurnerBlock.LIT) != shouldLit) {
            level.setBlock(pos, state.setValue(EssenceBurnerBlock.LIT, shouldLit), Block.UPDATE_CLIENTS);
        }
        if (changed) {
            be.setChanged();
        }
    }

    private void tryPullFuel() {
        ItemStack fuel = items.get(SLOT_FUEL);
        if (fuel.is(ModItems.ESSENITE_DUST.get())) {
            fuel.shrink(1);
            addFuel(DUST_FUEL_TICKS);
        }
    }

    private void syncLit() {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        boolean shouldLit = fuelTicks > 0 && overheatCooldown <= 0;
        if (state.getValue(EssenceBurnerBlock.LIT) != shouldLit) {
            level.setBlock(worldPosition, state.setValue(EssenceBurnerBlock.LIT, shouldLit), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.effecoria.burner");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new BurnerMenu(id, inv, this, data);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> stacks) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.set(i, i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY);
        }
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FUEL -> stack.is(ModItems.ESSENITE_DUST.get());
            case SLOT_CATALYST -> stack.is(ModItemTags.BURNER_CATALYSTS);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? NO_SLOTS : FUEL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == SLOT_FUEL && stack.is(ModItems.ESSENITE_DUST.get());
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Fuel", fuelTicks);
        tag.putInt("Temp", selectedTemp.ordinal());
        tag.putInt("HighTicks", highTicks);
        tag.putInt("Overheat", overheatCooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        fuelTicks = tag.getInt("Fuel");
        selectedTemp = HeatLevel.byId(tag.contains("Temp") ? tag.getInt("Temp") : HeatLevel.MEDIUM.ordinal());
        if (selectedTemp == HeatLevel.NONE) {
            selectedTemp = HeatLevel.MEDIUM;
        }
        highTicks = tag.getInt("HighTicks");
        overheatCooldown = tag.getInt("Overheat");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
