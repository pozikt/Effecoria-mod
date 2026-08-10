package com.effecoria.block;

import java.util.Optional;

import com.effecoria.alchemy.menu.MortarMenu;
import com.effecoria.alchemy.recipe.MortarRecipes;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModItemTags;
import com.effecoria.content.ModItems;
import com.effecoria.content.PhiHarnessItems;
import com.effecoria.core.alchemy.PhiPower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Mortar with GUI: input + 3 outputs + Φ-cell drive. Manual pestle or auto via charged cell.
 */
public final class MortarBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_PRIMARY = 1;
    public static final int SLOT_BYPRODUCT = 2;
    public static final int SLOT_WASTE = 3;
    public static final int SLOT_DRIVE = 4;
    public static final int SLOT_COUNT = 5;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX = 1;
    public static final int DATA_AUTO = 2;
    public static final int DATA_COUNT = 3;

    public static final int MANUAL_MAX = 100;
    public static final int AUTO_MAX = 40;
    public static final float PURITY_MANUAL = 0.70f;
    public static final float PURITY_AUTO = 0.90f;
    public static final float BYPRODUCT_CHANCE = 0.08f;
    public static final float CELL_DRAIN = 0.02f;

    private static final int[] INPUT_SLOTS = {SLOT_INPUT};
    private static final int[] OUTPUT_SLOTS = {SLOT_PRIMARY, SLOT_BYPRODUCT, SLOT_WASTE};
    private static final int[] NO_SLOTS = {};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int progress;
    private int maxProgress = MANUAL_MAX;
    private boolean autoMode;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_MAX -> maxProgress;
                case DATA_AUTO -> autoMode ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_PROGRESS -> progress = value;
                case DATA_MAX -> maxProgress = value;
                case DATA_AUTO -> autoMode = value != 0;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public MortarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MORTAR_AND_PESTLE.get(), pos, state);
    }

    public ContainerData getData() {
        return data;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.effecoria.mortar");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new MortarMenu(id, inv, this, data);
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
            case SLOT_INPUT -> MortarRecipes.isInput(stack);
            case SLOT_DRIVE -> stack.is(ModItems.PHI_CELL.get());
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return OUTPUT_SLOTS;
        }
        if (side == Direction.UP) {
            return INPUT_SLOTS;
        }
        return INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == SLOT_INPUT && MortarRecipes.isInput(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == SLOT_PRIMARY || slot == SLOT_BYPRODUCT || slot == SLOT_WASTE;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MortarBlockEntity be) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        ItemStack input = be.items.get(SLOT_INPUT);
        if (!MortarRecipes.isInput(input)) {
            if (be.progress != 0) {
                be.progress = 0;
                be.setChanged();
            }
            be.autoMode = false;
            return;
        }
        boolean auto = be.hasAutoDrive();
        boolean reactorPower = PhiPower.hasPower(server, pos);
        boolean manual = !auto && !reactorPower && be.hasManualPestle(server);
        boolean driven = auto || reactorPower;
        be.autoMode = driven;
        if (!driven && !manual) {
            return;
        }
        be.maxProgress = driven ? AUTO_MAX : MANUAL_MAX;
        be.progress++;
        be.setChanged();
        if (be.progress >= be.maxProgress) {
            be.finishGrind(server, driven, reactorPower);
        }
    }

    private boolean hasAutoDrive() {
        ItemStack cell = items.get(SLOT_DRIVE);
        if (cell.is(ModItems.PHI_CELL.get()) && PhiHarnessItems.cellCharge(cell) > 0.001f) {
            return true;
        }
        if (level == null) {
            return false;
        }
        for (Player player : level.players()) {
            if (player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5)
                    > 64.0) {
                continue;
            }
            if (holdsChargedCell(player)) {
                return true;
            }
        }
        return false;
    }

    private static boolean holdsChargedCell(Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return (main.is(ModItems.PHI_CELL.get()) && PhiHarnessItems.cellCharge(main) > 0.001f)
                || (off.is(ModItems.PHI_CELL.get()) && PhiHarnessItems.cellCharge(off) > 0.001f);
    }

    private boolean hasManualPestle(ServerLevel level) {
        for (Player player : level.players()) {
            if (player.containerMenu instanceof MortarMenu menu && menu.getBlockEntity() == this) {
                return holdsPestle(player);
            }
            if (player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5)
                            <= 36.0
                    && holdsPestle(player)) {
                return true;
            }
        }
        return false;
    }

    private static boolean holdsPestle(Player player) {
        return player.getMainHandItem().is(ModItemTags.PESTLES)
                || player.getOffhandItem().is(ModItemTags.PESTLES);
    }

    private void finishGrind(ServerLevel level, boolean auto, boolean reactorDriven) {
        ItemStack input = items.get(SLOT_INPUT);
        float purity = auto ? PURITY_AUTO : PURITY_MANUAL;
        Optional<MortarRecipes.Result> result =
                MortarRecipes.grind(input, level.random, purity, BYPRODUCT_CHANCE);
        if (result.isEmpty()) {
            progress = 0;
            return;
        }
        MortarRecipes.Result r = result.get();
        if (!canMerge(SLOT_PRIMARY, r.primary())
                || !canMerge(SLOT_BYPRODUCT, r.byproduct())
                || !canMerge(SLOT_WASTE, r.waste())) {
            progress = maxProgress - 1;
            return;
        }
        mergeInto(SLOT_PRIMARY, r.primary());
        mergeInto(SLOT_BYPRODUCT, r.byproduct());
        mergeInto(SLOT_WASTE, r.waste());
        input.shrink(1);
        if (auto && !reactorDriven) {
            drainDriveCell();
        }
        progress = 0;
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.55f, 1.35f);
        setChanged();
    }

    private void drainDriveCell() {
        ItemStack cell = items.get(SLOT_DRIVE);
        if (cell.is(ModItems.PHI_CELL.get())) {
            PhiHarnessItems.setCellCharge(cell, PhiHarnessItems.cellCharge(cell) - CELL_DRAIN);
        }
    }

    private boolean canMerge(int slot, ItemStack add) {
        if (add.isEmpty()) {
            return true;
        }
        ItemStack existing = items.get(slot);
        if (existing.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(existing, add)
                && existing.getCount() + add.getCount() <= existing.getMaxStackSize();
    }

    private void mergeInto(int slot, ItemStack add) {
        if (add.isEmpty()) {
            return;
        }
        ItemStack existing = items.get(slot);
        if (existing.isEmpty()) {
            items.set(slot, add.copy());
        } else {
            existing.grow(add.getCount());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        progress = tag.getInt("Progress");
        maxProgress = tag.contains("MaxProgress") ? tag.getInt("MaxProgress") : MANUAL_MAX;
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
