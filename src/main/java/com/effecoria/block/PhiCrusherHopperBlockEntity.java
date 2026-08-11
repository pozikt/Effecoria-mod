package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** Forwards hopper inserts from the funnel into the crusher base below. */
public final class PhiCrusherHopperBlockEntity extends BlockEntity implements WorldlyContainer {
    private static final int[] INPUT = {PhiCrusherBlockEntity.SLOT_INPUT};
    private static final int[] NONE = {};

    public PhiCrusherHopperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHI_CRUSHER_HOPPER.get(), pos, state);
    }

    @Nullable
    private PhiCrusherBlockEntity base() {
        if (level == null) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(worldPosition.below());
        return be instanceof PhiCrusherBlockEntity crusher ? crusher : null;
    }

    @Override
    public int getContainerSize() {
        PhiCrusherBlockEntity base = base();
        return base == null ? 0 : base.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        PhiCrusherBlockEntity base = base();
        return base == null || base.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        PhiCrusherBlockEntity base = base();
        return base == null ? ItemStack.EMPTY : base.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        PhiCrusherBlockEntity base = base();
        if (base != null) {
            base.setItem(slot, stack);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void clearContent() {}

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN || side == Direction.UP ? INPUT : NONE;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        PhiCrusherBlockEntity base = base();
        return base != null && base.canPlaceItemThroughFace(index, stack, Direction.UP);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return false;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        PhiCrusherBlockEntity base = base();
        return base != null && base.canPlaceItem(index, stack);
    }
}
