package com.effecoria.block;

import com.effecoria.alchemy.menu.PhiWaterPurifierMenu;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModItems;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.core.tower.TowerFacility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
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

/** Tower life-support: Φ-water bucket + filter → purified Φ-water. */
public final class PhiWaterPurifierBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FILTER = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_COUNT = 3;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_POWER = 1;
    public static final int DATA_COUNT = 2;

    public static final int PROCESS_TICKS = 80;

    private static final int[] INPUT_SLOTS = {SLOT_INPUT, SLOT_FILTER};
    private static final int[] OUTPUT_SLOTS = {SLOT_OUTPUT};
    private static final int[] NO_SLOTS = {};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int progress;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_POWER -> level != null && PhiPower.hasPower(level, worldPosition) ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == DATA_PROGRESS) {
                progress = value;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public PhiWaterPurifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHI_WATER_PURIFIER.get(), pos, state);
    }

    public ContainerData getData() {
        return data;
    }

    public int progress() {
        return progress;
    }

    public static boolean isFilter(ItemStack stack) {
        return stack.is(ModItems.GOLD_FILTER.get()) || stack.is(ModItems.LEAD_FILTER.get());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PhiWaterPurifierBlockEntity be) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        boolean live = TowerFacility.findComputer(server, pos)
                .filter(a -> a.consecrated() && a.bound())
                .isPresent();
        ItemStack input = be.items.get(SLOT_INPUT);
        ItemStack filter = be.items.get(SLOT_FILTER);
        ItemStack output = be.items.get(SLOT_OUTPUT);
        boolean canWork = live
                && input.is(ModItems.PHI_WATER_BUCKET.get())
                && isFilter(filter)
                && output.isEmpty();
        if (!canWork) {
            if (be.progress != 0) {
                be.progress = 0;
                be.setChanged();
            }
            return;
        }
        if (!PhiPower.consumeTick(level, pos, 2)) {
            be.progress = 0;
            be.setChanged();
            return;
        }
        be.progress++;
        if (be.progress >= PROCESS_TICKS) {
            be.progress = 0;
            input.shrink(1);
            if (input.isEmpty()) {
                be.items.set(SLOT_INPUT, ItemStack.EMPTY);
            }
            be.items.set(SLOT_OUTPUT, new ItemStack(ModItems.PURIFIED_PHI_WATER_BUCKET.get()));
            if (filter.isDamageableItem()) {
                filter.setDamageValue(filter.getDamageValue() + 1);
                if (filter.getDamageValue() >= filter.getMaxDamage()) {
                    be.items.set(SLOT_FILTER, ItemStack.EMPTY);
                }
            } else {
                filter.shrink(1);
                if (filter.isEmpty()) {
                    be.items.set(SLOT_FILTER, ItemStack.EMPTY);
                }
            }
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        } else {
            be.setChanged();
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.effecoria.phi_water_purifier");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new PhiWaterPurifierMenu(id, inv, this, data);
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
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_INPUT -> stack.is(ModItems.PHI_WATER_BUCKET.get());
            case SLOT_FILTER -> isFilter(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return OUTPUT_SLOTS;
        }
        if (side == Direction.UP) {
            return new int[] {SLOT_INPUT};
        }
        return INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        progress = tag.getInt("Progress");
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
