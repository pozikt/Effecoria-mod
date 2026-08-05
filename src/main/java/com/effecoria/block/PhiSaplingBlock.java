package com.effecoria.block;

import com.effecoria.content.ModBlocks;
import com.effecoria.world.ModTreeGrowers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Φ-sapling — only roots in Φ-soil. */
public final class PhiSaplingBlock extends SaplingBlock {
    public PhiSaplingBlock(TreeGrower treeGrower, BlockBehaviour.Properties properties) {
        super(treeGrower, properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModBlocks.PHI_GRASS.get()) || state.is(ModBlocks.PHI_DIRT.get());
    }
}
