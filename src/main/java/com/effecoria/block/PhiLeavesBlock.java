package com.effecoria.block;

import com.effecoria.content.ModItems;
import com.effecoria.content.ModParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Indigo Φ-canopy — night glow, rare nut drop while decaying / random ticks. */
public final class PhiLeavesBlock extends LeavesBlock {
    public PhiLeavesBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (!level.isDay() && random.nextInt(8) == 0) {
            level.addParticle(
                    ModParticleTypes.PHI_SPARK.get(),
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(),
                    0,
                    -0.01,
                    0);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        if (!state.getValue(PERSISTENT) && random.nextInt(80) == 0) {
            ItemEntity nut = new ItemEntity(
                    level,
                    pos.getX() + 0.5,
                    pos.getY() + 0.2,
                    pos.getZ() + 0.5,
                    new ItemStack(ModItems.PHI_NUT.get()));
            nut.setDefaultPickUpDelay();
            level.addFreshEntity(nut);
        }
    }
}
