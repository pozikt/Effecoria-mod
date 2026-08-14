package com.effecoria.block;

import com.effecoria.alchemy.menu.ImprinterMenu;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModItems;
import com.effecoria.content.PhiHarnessItems;
import com.effecoria.core.alchemy.HeatLevel;
import com.effecoria.core.alchemy.PhiHeat;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.core.technomagic.ImprintData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

/** Imprints chassis / telegraph modules using Φ-cell charge, focus tier, and MED+ heat. */
public final class PsiImprinterBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_DRIVE = 0;
    public static final int SLOT_FOCUS = 1;
    public static final int SLOT_BLANK = 2;
    public static final int SLOT_OUTPUT = 3;
    public static final int SLOT_COUNT = 4;

    public static final int MODE_CONSTRUCT = 0;
    public static final int MODE_TELEGRAPH = 1;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX = 1;
    public static final int DATA_MODE = 2;
    public static final int DATA_COUNT = 3;

    public static final int BASE_COOK = 200;
    public static final float CELL_DRAIN = 0.015f;

    private static final int[] IN_SLOTS = {SLOT_DRIVE, SLOT_FOCUS, SLOT_BLANK};
    private static final int[] OUT_SLOTS = {SLOT_OUTPUT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int progress;
    private int maxProgress = BASE_COOK;
    private int mode = MODE_CONSTRUCT;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_MAX -> maxProgress;
                case DATA_MODE -> mode;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_PROGRESS -> progress = value;
                case DATA_MAX -> maxProgress = value;
                case DATA_MODE -> mode = value == MODE_TELEGRAPH ? MODE_TELEGRAPH : MODE_CONSTRUCT;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public PsiImprinterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PSI_IMPRINTER.get(), pos, state);
    }

    public ContainerData getData() {
        return data;
    }

    public void setMode(int next) {
        mode = next == MODE_TELEGRAPH ? MODE_TELEGRAPH : MODE_CONSTRUCT;
        progress = 0;
        setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.effecoria.psi_imprinter");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new ImprinterMenu(id, inv, this, data);
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
            case SLOT_DRIVE -> stack.is(ModItems.PHI_CELL.get());
            case SLOT_FOCUS -> stack.is(ModItems.RESONANCE_FOCUS.get()) || stack.is(ModItems.SOUL_SHARD.get());
            case SLOT_BLANK -> isValidBlank(stack);
            default -> false;
        };
    }

    private boolean isValidBlank(ItemStack stack) {
        if (mode == MODE_TELEGRAPH) {
            return ImprintData.isBlankTelegraphModule(stack);
        }
        return ImprintData.isBlankChassis(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUT_SLOTS : IN_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(index, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index == SLOT_OUTPUT;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PsiImprinterBlockEntity be) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        ItemStack blank = be.items.get(SLOT_BLANK);
        ItemStack drive = be.items.get(SLOT_DRIVE);
        ItemStack focus = be.items.get(SLOT_FOCUS);
        ItemStack out = be.items.get(SLOT_OUTPUT);
        if (!be.isValidBlank(blank)
                || !drive.is(ModItems.PHI_CELL.get())
                || PhiHarnessItems.cellCharge(drive) < CELL_DRAIN
                || !isValidFocus(focus)
                || !out.isEmpty()) {
            if (be.progress > 0) {
                be.progress = Math.max(0, be.progress - 2);
                be.setChanged();
            }
            return;
        }
        HeatLevel heat = PhiHeat.getNeighborHeat(level, pos);
        if (heat.ordinal() < HeatLevel.MEDIUM.ordinal() || !PhiHeat.consumeNeighborHeat(server, pos)) {
            if (be.progress > 0) {
                be.progress = Math.max(0, be.progress - 1);
                be.setChanged();
            }
            return;
        }
        if (!PhiPower.consumeTick(level, pos, 1)) {
            if (be.progress > 0) {
                be.progress = Math.max(0, be.progress - 1);
                be.setChanged();
            }
            return;
        }
        boolean soulFocus = focus.is(ModItems.SOUL_SHARD.get());
        int tier = soulFocus ? 0 : PhiHarnessItems.focusTier(focus);
        float resonance = Math.max(0.35f, PhiPower.resonanceAt(level, pos));
        be.maxProgress = Math.max(80, Math.round((BASE_COOK - tier * 40) / resonance));
        be.progress++;
        PhiHarnessItems.setCellCharge(drive, PhiHarnessItems.cellCharge(drive) - CELL_DRAIN);
        if (be.progress >= be.maxProgress) {
            be.progress = 0;
            ItemStack result;
            if (be.mode == MODE_TELEGRAPH) {
                result = new ItemStack(ModItems.PHI_TELEGRAPH.get());
                ImprintData.imprintTelegraph(result, tier);
            } else {
                result = blank.copyWithCount(1);
                ImprintData.imprintConstruct(result, tier);
            }
            be.items.set(SLOT_BLANK, ItemStack.EMPTY);
            be.items.set(SLOT_OUTPUT, result);
            if (soulFocus) {
                focus.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.55f, 1.35f);
        }
        be.setChanged();
    }

    private static boolean isValidFocus(ItemStack stack) {
        return stack.is(ModItems.RESONANCE_FOCUS.get()) || stack.is(ModItems.SOUL_SHARD.get());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("Mode", mode);
        ContainerHelper.saveAllItems(tag, items, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        progress = tag.getInt("Progress");
        maxProgress = tag.contains("MaxProgress") ? tag.getInt("MaxProgress") : BASE_COOK;
        mode = tag.getInt("Mode") == MODE_TELEGRAPH ? MODE_TELEGRAPH : MODE_CONSTRUCT;
        ContainerHelper.loadAllItems(tag, items, provider);
    }
}
