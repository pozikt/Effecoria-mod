package com.effecoria.block;

import com.effecoria.content.ModParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Sharp glassy branches — thin collision, light contact damage. */
public final class VitrifiedBranchesBlock extends Block {
    private static final VoxelShape SHAPE = box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);

    public VitrifiedBranchesBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && living.tickCount % 10 == 0) {
            living.hurt(level.damageSources().cactus(), 0.5f);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) == 0) {
            Direction dir = Direction.getRandom(random);
            double x = pos.getX() + 0.5 + dir.getStepX() * 0.35;
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + 0.5 + dir.getStepZ() * 0.35;
            level.addParticle(ModParticleTypes.PHI_SPARK.get(), x, y, z, 0, 0.01, 0);
        }
    }
}
