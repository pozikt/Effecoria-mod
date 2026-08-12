package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Hyper-tunnel film — mirror membrane reflecting hyperspace (BER).
 * Not crafted — machine-placed only. Registry id remains {@code portal_gate}.
 */
public final class PortalGateBlock extends BaseEntityBlock {
    public static final MapCodec<PortalGateBlock> CODEC = simpleCodec(PortalGateBlock::new);

    private static final VoxelShape SHAPE = Shapes.block();

    public PortalGateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // BER draws the hyperspace mirror; block model stays invisible.
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PortalGateBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntities.PORTAL_GATE.get(), PortalGateBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        for (int i = 0; i < 2; i++) {
            level.addParticle(
                    ParticleTypes.REVERSE_PORTAL,
                    x + (random.nextDouble() - 0.5) * 0.7,
                    y + (random.nextDouble() - 0.5) * 0.7,
                    z + (random.nextDouble() - 0.5) * 0.7,
                    (random.nextDouble() - 0.5) * 0.02,
                    (random.nextDouble() - 0.5) * 0.02,
                    (random.nextDouble() - 0.5) * 0.02);
        }
        if (random.nextFloat() < 0.35f) {
            level.addParticle(
                    ParticleTypes.END_ROD,
                    x + (random.nextDouble() - 0.5) * 0.5,
                    y + (random.nextDouble() - 0.5) * 0.5,
                    z + (random.nextDouble() - 0.5) * 0.5,
                    0,
                    0.02,
                    0);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof PortalGateBlockEntity film)) {
            return;
        }
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        film.tryTeleport(server, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof PortalGateBlockEntity film) {
            film.onFilmBroken();
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected void neighborChanged(
            BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide() && neighborBlock instanceof MithrilBlock) {
            PortalModulatorBlock.notifyNearby(level, neighborPos);
        }
    }
}
