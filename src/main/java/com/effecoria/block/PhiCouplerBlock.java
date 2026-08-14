package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.circuit.PhiChannel;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/** Resonant coupler — stamps a frequency channel on the conductor island. */
public final class PhiCouplerBlock extends net.minecraft.world.level.block.BaseEntityBlock {
    public static final MapCodec<PhiCouplerBlock> CODEC = simpleCodec(PhiCouplerBlock::new);
    public static final EnumProperty<PhiChannel> CHANNEL = EnumProperty.create("channel", PhiChannel.class);

    public PhiCouplerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CHANNEL, PhiChannel.LIFE));
    }

    @Override
    protected MapCodec<PhiCouplerBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHANNEL);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PhiCouplerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntities.PHI_COUPLER.get(), PhiCouplerBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        PhiChannel next = state.getValue(CHANNEL).next();
        level.setBlock(pos, state.setValue(CHANNEL, next), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof PhiCouplerBlockEntity be) {
            be.setChannel(next);
        }
        player.displayClientMessage(
                Component.translatable("message.effecoria.phi_coupler.channel", Component.translatable(nextTranslation(next))),
                true);
        return InteractionResult.CONSUME;
    }

    public static String nextTranslation(PhiChannel channel) {
        return "phi.effecoria.channel." + channel.getSerializedName();
    }
}
