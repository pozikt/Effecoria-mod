package com.effecoria.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Black vitrified sand — slow fall + Φ-barghan quicksand. */
public final class VitrifiedSandBlock extends FallingBlock {
    public static final MapCodec<VitrifiedSandBlock> CODEC = simpleCodec(VitrifiedSandBlock::new);
    private static final VoxelShape SINK_SHAPE = box(0.0, 0.0, 0.0, 16.0, 14.0, 16.0);

    public VitrifiedSandBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }

    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return 0x0f3460;
    }

    @Override
    protected void falling(FallingBlockEntity entity) {
        entity.setHurtsEntities(1.0f, 20);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext) {
            Entity entity = entityContext.getEntity();
            if (entity instanceof LivingEntity) {
                return SINK_SHAPE;
            }
        }
        return Shapes.block();
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        living.makeStuckInBlock(state, new Vec3(0.22, 0.06, 0.22));
        if (!level.isClientSide && living.getY() < pos.getY() + 0.4) {
            living.hurt(level.damageSources().inWall(), 1.0f);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Slower collapse than vanilla sand
        if (random.nextInt(3) == 0) {
            super.tick(state, level, pos, random);
        } else {
            level.scheduleTick(pos, this, this.getDelayAfterPlace() + 4);
        }
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, side);
    }
}
