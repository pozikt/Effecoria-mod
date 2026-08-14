package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/** Essonite ΔQ buffer — charges from the island, discharges as a local injector. */
public final class PhiAccumulatorBlock extends net.minecraft.world.level.block.BaseEntityBlock {
    public static final MapCodec<PhiAccumulatorBlock> CODEC = simpleCodec(PhiAccumulatorBlock::new);

    public PhiAccumulatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<PhiAccumulatorBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PhiAccumulatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntities.PHI_ACCUMULATOR.get(), PhiAccumulatorBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof PhiAccumulatorBlockEntity acc) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.effecoria.phi_accumulator.status",
                            acc.chargePercent(),
                            Math.round(acc.omegaPercent())),
                    true);
        }
        return InteractionResult.CONSUME;
    }
}
