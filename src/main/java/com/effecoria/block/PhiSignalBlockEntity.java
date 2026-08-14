package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Marker BE for facility scans; LIT lives on the blockstate. */
public final class PhiSignalBlockEntity extends BlockEntity {
    public PhiSignalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHI_SIGNAL.get(), pos, state);
    }
}
