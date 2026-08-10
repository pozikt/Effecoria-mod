package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import org.joml.Vector3f;

import javax.annotation.Nullable;

/** Compact Φ-reactor «Искра» — wireless power for nearby alchemy machines. */
public final class SparkReactorBlock extends BaseEntityBlock {
    public static final MapCodec<SparkReactorBlock> CODEC = simpleCodec(SparkReactorBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final DustParticleOptions BLUE =
            new DustParticleOptions(new Vector3f(0.2f, 0.55f, 1.0f), 0.85f);
    private static final DustParticleOptions GOLD =
            new DustParticleOptions(new Vector3f(1.0f, 0.8f, 0.25f), 1.0f);

    public SparkReactorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SparkReactorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntities.SPARK_REACTOR.get(), SparkReactorBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof SparkReactorBlockEntity reactor) {
            serverPlayer.openMenu(reactor, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.05;
        double z = pos.getZ() + 0.5;
        DustParticleOptions particle = BLUE;
        if (level.getBlockEntity(pos) instanceof SparkReactorBlockEntity reactor && reactor.boostTicks() > 0) {
            particle = GOLD;
        }
        level.addParticle(
                particle, x + (random.nextDouble() - 0.5) * 0.35, y, z + (random.nextDouble() - 0.5) * 0.35, 0, 0.02, 0);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SparkReactorBlockEntity reactor) {
            net.minecraft.world.Containers.dropContents(level, pos, reactor);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SparkReactorBlockEntity reactor) {
            return reactor.supplying() ? Math.max(1, (int) (reactor.powerFactor() * 5f)) : 0;
        }
        return 0;
    }
}
