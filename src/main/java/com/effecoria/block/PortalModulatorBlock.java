package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.alchemy.PortalFrameFinder;
import com.effecoria.core.technomagic.TechnomagicEra;
import com.effecoria.core.technomagic.TechnomagicGates;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/** Portal modulator — Ψ-computer that opens a hyper-tunnel in an adjacent mithril frame. */
public final class PortalModulatorBlock extends BaseEntityBlock {
    public static final MapCodec<PortalModulatorBlock> CODEC = simpleCodec(PortalModulatorBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public PortalModulatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PortalModulatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(
                        type, ModBlockEntities.PORTAL_MODULATOR.get(), PortalModulatorBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (!TechnomagicGates.checkOperate(serverPlayer, TechnomagicEra.V)) {
                return InteractionResult.FAIL;
            }
            if (level.getBlockEntity(pos) instanceof PortalModulatorBlockEntity mod) {
                serverPlayer.openMenu(mod, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof PortalModulatorBlockEntity mod) {
            mod.forceClose();
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /** Notify modulators when nearby mithril may have changed. */
    public static void notifyNearby(Level level, BlockPos changed) {
        if (level.isClientSide()) {
            return;
        }
        for (Direction dir : Direction.values()) {
            BlockPos adj = changed.relative(dir);
            if (level.getBlockEntity(adj) instanceof PortalModulatorBlockEntity mod && mod.isOpen()) {
                if (PortalFrameFinder.find(level, adj) == null) {
                    mod.forceClose();
                }
            }
        }
        // Also scan modulators within 8 that own this cell as frame
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -8; dy <= 8; dy++) {
            for (int dx = -8; dx <= 8; dx++) {
                for (int dz = -8; dz <= 8; dz++) {
                    cursor.set(changed.getX() + dx, changed.getY() + dy, changed.getZ() + dz);
                    if (level.getBlockEntity(cursor) instanceof PortalModulatorBlockEntity mod && mod.isOpen()) {
                        if (mod.ownsFrameCell(changed) && PortalFrameFinder.find(level, cursor.immutable()) == null) {
                            mod.forceClose();
                        }
                    }
                }
            }
        }
    }
}
