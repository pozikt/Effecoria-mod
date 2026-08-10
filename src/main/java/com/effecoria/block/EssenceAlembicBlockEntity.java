package com.effecoria.block;

import com.effecoria.alchemy.menu.AlembicMenu;
import com.effecoria.alchemy.recipe.AlembicRecipes;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.alchemy.HeatLevel;
import com.effecoria.core.alchemy.PhiHeat;
import com.effecoria.core.alchemy.PhiPower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

import java.util.UUID;

/** Essence alembic — water + reagents + heat-gated brew into output slot. */
public final class EssenceAlembicBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_WATER = 0;
    public static final int SLOT_REAGENT1 = 1;
    public static final int SLOT_REAGENT2 = 2;
    public static final int SLOT_REAGENT3 = 3;
    public static final int SLOT_OUTPUT = 4;
    public static final int SLOT_COUNT = 5;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX = 1;
    public static final int DATA_HEAT = 2;
    public static final int DATA_COUNT = 3;

    public static final int COOK_TIME = EssenceAlembicBlock.COOK_TIME;
    public static final String NBT_BREW_HEAT = "PhiBrewHeat";
    public static final String NBT_DURATION_FACTOR = "PhiDurationFactor";
    public static final String NBT_BLOOD_DONOR = "BloodDonor";
    public static final String NBT_BLOOD_ANCHORED = "BloodAnchored";

    private static final int[] INPUT_SLOTS = {SLOT_WATER, SLOT_REAGENT1, SLOT_REAGENT2, SLOT_REAGENT3};
    private static final int[] OUTPUT_SLOTS = {SLOT_OUTPUT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int cookProgress;
    private int cookTotal = COOK_TIME;
    private Item cookingResultItem;
    private float cookingDurationFactor = 1f;
    private int cookingHeatOrdinal;
    @Nullable
    private UUID cookingBloodDonor;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> cookProgress;
                case DATA_MAX -> cookTotal;
                case DATA_HEAT -> level != null
                        ? PhiHeat.getNeighborHeat(level, worldPosition).ordinal()
                        : HeatLevel.NONE.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_PROGRESS -> cookProgress = value;
                case DATA_MAX -> cookTotal = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public EssenceAlembicBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ESSENCE_ALEMBIC.get(), pos, state);
    }

    public ContainerData getData() {
        return data;
    }

    public boolean isCooking() {
        return cookingResultItem != null && cookProgress < cookTotal;
    }

    /** Legacy accessors for particles / old call sites. */
    public ItemStack getBase() {
        return items.get(SLOT_WATER);
    }

    public ItemStack getResult() {
        return items.get(SLOT_OUTPUT);
    }

    public ItemStack takeResult() {
        ItemStack out = items.get(SLOT_OUTPUT).copy();
        items.set(SLOT_OUTPUT, ItemStack.EMPTY);
        setChanged();
        sync();
        return out;
    }

    public void drops(Level level, BlockPos pos) {
        net.minecraft.world.Containers.dropContents(level, pos, this);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EssenceAlembicBlockEntity be) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (be.cookingResultItem == null) {
            be.tryStartCook(server);
            return;
        }
        if (be.cookProgress >= be.cookTotal) {
            return;
        }
        HeatLevel heat = PhiHeat.getNeighborHeat(server, pos);
        if (!heat.isPresent()) {
            return;
        }
        if (!PhiHeat.consumeNeighborHeat(server, pos)) {
            return;
        }
        int steps = PhiPower.hasPower(server, pos) ? 2 : 1;
        be.cookProgress = Math.min(be.cookTotal, be.cookProgress + steps);
        be.setChanged();
        if (be.cookProgress >= be.cookTotal) {
            be.finishCook(server);
        }
    }

    private void tryStartCook(ServerLevel level) {
        if (!items.get(SLOT_OUTPUT).isEmpty()) {
            return;
        }
        ItemStack water = items.get(SLOT_WATER);
        ItemStack r1 = items.get(SLOT_REAGENT1);
        if (!AlembicRecipes.isWater(water) || !AlembicRecipes.optionalReagentsOk(items.get(SLOT_REAGENT2), items.get(SLOT_REAGENT3))) {
            return;
        }
        Item potion = AlembicRecipes.potionForPowerReagent(r1);
        if (potion == null) {
            return;
        }
        HeatLevel heat = PhiHeat.getNeighborHeat(level, worldPosition);
        if (!heat.isPresent()) {
            return;
        }
        cookingResultItem = potion;
        cookingDurationFactor = AlembicRecipes.durationFactor(heat)
                * AlembicRecipes.bloodDurationMultiplier(items.get(SLOT_REAGENT2), items.get(SLOT_REAGENT3));
        cookingHeatOrdinal = heat.ordinal();
        cookProgress = 0;
        cookTotal = COOK_TIME;
        // consume inputs at start (like brewing stand)
        water.shrink(1);
        r1.shrink(1);
        ItemStack r2 = items.get(SLOT_REAGENT2);
        ItemStack r3 = items.get(SLOT_REAGENT3);
        cookingBloodDonor = AlembicRecipes.bloodDonorUuid(r2, r3);
        if (!r2.isEmpty()) {
            r2.shrink(1);
        }
        if (!r3.isEmpty()) {
            r3.shrink(1);
        }
        level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.5f, 1.3f);
        setChanged();
        sync();
    }

    private void finishCook(ServerLevel level) {
        ItemStack potion = new ItemStack(cookingResultItem);
        CompoundTag tag = new CompoundTag();
        tag.putInt(NBT_BREW_HEAT, cookingHeatOrdinal);
        tag.putFloat(NBT_DURATION_FACTOR, cookingDurationFactor);
        if (cookingBloodDonor != null) {
            tag.putUUID(NBT_BLOOD_DONOR, cookingBloodDonor);
            tag.putBoolean(NBT_BLOOD_ANCHORED, true);
        }
        potion.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        items.set(SLOT_OUTPUT, potion);
        cookingResultItem = null;
        cookProgress = 0;
        cookingDurationFactor = 1f;
        cookingBloodDonor = null;
        level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8f, 0.9f);
        setChanged();
        sync();
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.effecoria.alembic");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new AlembicMenu(id, inv, this, data);
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
            case SLOT_WATER -> AlembicRecipes.isWater(stack);
            case SLOT_REAGENT1 -> AlembicRecipes.isPowerReagent(stack);
            case SLOT_REAGENT2, SLOT_REAGENT3 -> stack.is(com.effecoria.content.ModItemTags.ALEMBIC_REAGENT_OPTIONAL);
            case SLOT_OUTPUT -> false;
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Cook", cookProgress);
        tag.putInt("CookTotal", cookTotal);
        tag.putFloat("CookFactor", cookingDurationFactor);
        tag.putInt("CookHeat", cookingHeatOrdinal);
        if (cookingResultItem != null) {
            tag.putString(
                    "CookItem",
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(cookingResultItem).toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        cookProgress = tag.getInt("Cook");
        cookTotal = tag.contains("CookTotal") ? tag.getInt("CookTotal") : COOK_TIME;
        cookingDurationFactor = tag.contains("CookFactor") ? tag.getFloat("CookFactor") : 1f;
        cookingHeatOrdinal = tag.getInt("CookHeat");
        cookingResultItem = null;
        if (tag.contains("CookItem")) {
            cookingResultItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    net.minecraft.resources.ResourceLocation.parse(tag.getString("CookItem")));
        }
        // migrate legacy single-stack NBT
        if (tag.contains("Base") && items.get(SLOT_WATER).isEmpty()) {
            items.set(
                    SLOT_WATER,
                    ItemStack.parse(registries, tag.getCompound("Base")).orElse(ItemStack.EMPTY));
        }
        if (tag.contains("Result") && items.get(SLOT_OUTPUT).isEmpty()) {
            items.set(
                    SLOT_OUTPUT,
                    ItemStack.parse(registries, tag.getCompound("Result")).orElse(ItemStack.EMPTY));
        }
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
