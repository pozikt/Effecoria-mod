package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.alchemy.PhiBusNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Marks the island as frequency-matched (Δω→0 for consumers). */
public final class PhiMatcherBlockEntity extends BlockEntity {
    private int refreshCooldown;

    public PhiMatcherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHI_MATCHER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PhiMatcherBlockEntity be) {
        if (be.refreshCooldown > 0) {
            be.refreshCooldown--;
            return;
        }
        be.refreshCooldown = 10;
        boolean energized = PhiBusNetwork.findSource(level, pos) != null;
        if (state.getValue(PhiMatcherBlock.POWERED) != energized) {
            level.setBlock(pos, state.setValue(PhiMatcherBlock.POWERED, energized), Block.UPDATE_CLIENTS);
        }
    }
}
