package com.effecoria.block;

import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModParticleTypes;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Φ-grass blades — fiberoptic shoots that only root in Φ-soil and flash on contact.
 */
public final class PhiBladesBlock extends BushBlock {
    public static final MapCodec<PhiBladesBlock> CODEC = simpleCodec(PhiBladesBlock::new);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);

    public PhiBladesBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModBlocks.PHI_GRASS.get()) || state.is(ModBlocks.PHI_DIRT.get());
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return mayPlaceOn(level.getBlockState(below), level, below);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        boolean night = !level.isDay();
        if (night || random.nextInt(12) == 0) {
            level.addParticle(
                    ModParticleTypes.PHI_SPARK.get(),
                    pos.getX() + 0.3 + random.nextDouble() * 0.4,
                    pos.getY() + 0.4 + random.nextDouble() * 0.5,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4,
                    0,
                    0.01,
                    0);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof Player && level instanceof ServerLevel server) {
            if (entity.tickCount % 8 == 0) {
                server.sendParticles(
                        ModParticleTypes.PHI_SPARK.get(),
                        pos.getX() + 0.5,
                        pos.getY() + 0.55,
                        pos.getZ() + 0.5,
                        8,
                        0.25,
                        0.2,
                        0.25,
                        0.01);
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(18) != 0) {
            return;
        }
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockPos target = pos.relative(dir);
        BlockPos ground = target.below();
        if (level.isEmptyBlock(target) && mayPlaceOn(level.getBlockState(ground), level, ground)) {
            level.setBlock(target, defaultBlockState(), 3);
        }
    }
}
