package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.joml.Vector3f;

import javax.annotation.Nullable;

/** Caldera vent of a Whispering Spire — anchors the Φ-plasma column. */
public final class WhisperingSpireVentBlock extends BaseEntityBlock {
    public static final MapCodec<WhisperingSpireVentBlock> CODEC = simpleCodec(WhisperingSpireVentBlock::new);
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    private static final DustParticleOptions ULTRAMARINE =
            new DustParticleOptions(new Vector3f(0.15f, 0.35f, 1.0f), 1.4f);
    private static final DustParticleOptions GOLD =
            new DustParticleOptions(new Vector3f(1.0f, 0.85f, 0.35f), 1.1f);

    public WhisperingSpireVentBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WhisperingSpireVentBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.WHISPERING_SPIRE_VENT.get(), WhisperingSpireVentBlockEntity::tick);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.9;
        double z = pos.getZ() + 0.5;
        for (int i = 0; i < 3; i++) {
            double h = random.nextDouble() * 8.0;
            DustParticleOptions dust = h < 3.0 ? ULTRAMARINE : GOLD;
            level.addParticle(
                    dust,
                    x + (random.nextDouble() - 0.5) * 0.35,
                    y + h,
                    z + (random.nextDouble() - 0.5) * 0.35,
                    0,
                    0.08 + random.nextDouble() * 0.06,
                    0);
        }
        if (random.nextFloat() < 0.35f) {
            level.addParticle(ParticleTypes.END_ROD, x, y + random.nextDouble() * 12.0, z, 0, 0.12, 0);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
            com.effecoria.world.WhisperingSpireService.unregister(server, pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
