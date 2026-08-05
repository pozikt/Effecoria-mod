package com.effecoria.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Φ-barghan mass — soft essonite dust that sinks and slows entities (quicksand).
 */
public final class EssoniteDustBlock extends Block {
    private static final VoxelShape FALLING_COLLISION = Block.box(0.0, 0.0, 0.0, 16.0, 14.0, 16.0);

    public EssoniteDustBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext) {
            Entity entity = entityContext.getEntity();
            if (entity != null && entity.fallDistance > 2.5f) {
                return Shapes.block();
            }
            if (entity instanceof LivingEntity living && !(living instanceof Player player && player.getAbilities().flying)) {
                return FALLING_COLLISION;
            }
        }
        return Shapes.block();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        living.makeStuckInBlock(state, new Vec3(0.25, 0.08, 0.25));
        if (!level.isClientSide && living.getY() < pos.getY() + 0.35) {
            living.hurt(level.damageSources().inWall(), 1.0f);
        }
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 0.85f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
