package com.effecoria.block;

import com.effecoria.content.ModParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Small residual Φ-crack — particles + contact burn. */
public final class VitrifiedGeyserCrackBlock extends Block {
    public VitrifiedGeyserCrackBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) {
            return;
        }
        level.sendParticles(
                ModParticleTypes.PHI_SPARK.get(),
                pos.getX() + 0.5,
                pos.getY() + 1.0,
                pos.getZ() + 0.5,
                8,
                0.15,
                0.4,
                0.15,
                0.02);
        level.sendParticles(
                ModParticleTypes.ELEMENTAL_PLASMA.get(),
                pos.getX() + 0.5,
                pos.getY() + 0.8,
                pos.getZ() + 0.5,
                3,
                0.1,
                0.25,
                0.1,
                0.01);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            living.hurt(level.damageSources().magic(), 1.5f);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(
                    ModParticleTypes.PHI_SPARK.get(),
                    pos.getX() + 0.3 + random.nextDouble() * 0.4,
                    pos.getY() + 1.0,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4,
                    0,
                    0.05,
                    0);
        }
    }
}
