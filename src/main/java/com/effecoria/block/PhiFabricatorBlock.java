package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.fabricator.FabricatorClass;
import com.effecoria.core.technomagic.TechnomagicEra;
import com.effecoria.core.technomagic.TechnomagicGates;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

import javax.annotation.Nullable;

/** Φ-fabricator block — class stored on the block instance (I / II / III). */
public final class PhiFabricatorBlock extends BaseEntityBlock {
    public static final MapCodec<PhiFabricatorBlock> CODEC =
            simpleCodec(props -> new PhiFabricatorBlock(props, FabricatorClass.I));
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private final FabricatorClass fabricatorClass;

    public PhiFabricatorBlock(Properties properties, FabricatorClass fabricatorClass) {
        super(properties);
        this.fabricatorClass = fabricatorClass;
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    public FabricatorClass fabricatorClass() {
        return fabricatorClass;
    }

    private TechnomagicEra requiredEra() {
        return switch (fabricatorClass) {
            case I, II -> TechnomagicEra.IV;
            case III -> TechnomagicEra.VI;
        };
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PhiFabricatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntities.PHI_FABRICATOR.get(), PhiFabricatorBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        open(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        open(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    private void open(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof PhiFabricatorBlockEntity fabricator) {
            if (!TechnomagicGates.checkOperate(serverPlayer, requiredEra())) {
                return;
            }
            serverPlayer.openMenu(fabricator, buf -> buf.writeBlockPos(pos));
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof PhiFabricatorBlockEntity fabricator) {
            Containers.dropContents(level, pos, fabricator);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
