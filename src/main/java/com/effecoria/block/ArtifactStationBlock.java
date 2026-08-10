package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/** Shared openable machine block for artifact craft stations. */
public final class ArtifactStationBlock extends BaseEntityBlock {
    public enum Kind {
        LATHE,
        CUTTER,
        ASSEMBLER,
        INSCRIBER
    }

    public static final MapCodec<ArtifactStationBlock> CODEC = simpleCodec(props -> new ArtifactStationBlock(props, Kind.LATHE));
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 12.0, 15.0);

    private final Kind kind;

    public ArtifactStationBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
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
        return switch (kind) {
            case LATHE -> new ShaftLatheBlockEntity(pos, state);
            case CUTTER -> new FacetCutterBlockEntity(pos, state);
            case ASSEMBLER -> new ArtifactAssemblerBlockEntity(pos, state);
            case INSCRIBER -> new SealInscriberBlockEntity(pos, state);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return switch (kind) {
            case LATHE -> createTickerHelper(type, ModBlockEntities.SHAFT_LATHE.get(), ShaftLatheBlockEntity::serverTick);
            case CUTTER -> createTickerHelper(type, ModBlockEntities.FACET_CUTTER.get(), FacetCutterBlockEntity::serverTick);
            case ASSEMBLER -> createTickerHelper(
                    type, ModBlockEntities.ARTIFACT_ASSEMBLER.get(), ArtifactAssemblerBlockEntity::serverTick);
            case INSCRIBER -> null;
        };
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
                && level.getBlockEntity(pos) instanceof net.minecraft.world.MenuProvider menu) {
            serverPlayer.openMenu(menu, buf -> buf.writeBlockPos(pos));
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof net.minecraft.world.Container container) {
            net.minecraft.world.Containers.dropContents(level, pos, container);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
