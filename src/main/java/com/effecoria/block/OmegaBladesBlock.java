package com.effecoria.block;

import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModParticleTypes;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Razor Ω-grass — black blades with purple tips; cuts living things that walk through. */
public final class OmegaBladesBlock extends BushBlock {
    public static final MapCodec<OmegaBladesBlock> CODEC = simpleCodec(OmegaBladesBlock::new);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 11.0, 14.0);

    public OmegaBladesBlock(BlockBehaviour.Properties properties) {
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
        return state.is(ModBlocks.ASH_SOIL.get()) || state.is(ModBlocks.VOID_OBSIDIAN.get());
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return mayPlaceOn(level.getBlockState(below), level, below);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity living && entity.tickCount % 10 == 0) {
            living.hurt(level.damageSources().cactus(), 1.0f);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0) {
            level.addParticle(
                    ModParticleTypes.CORRUPTION_MIASMA.get(),
                    pos.getX() + 0.3 + random.nextDouble() * 0.4,
                    pos.getY() + 0.5 + random.nextDouble() * 0.3,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4,
                    0,
                    0.01,
                    0);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(22) != 0) {
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
