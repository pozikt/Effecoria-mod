package com.effecoria.block;

import com.effecoria.core.alchemy.TurretAssembly;
import com.effecoria.core.alchemy.TurretKind;
import com.effecoria.core.technomagic.TechnomagicEra;
import com.effecoria.core.technomagic.TechnomagicGates;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Turret barrel / emitter. Place on the outward side of a {@link TurretMountBlock} to assemble.
 * When formed, rendering is handled by the mount BER.
 */
public final class PhiTurretBlock extends Block {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final TurretKind kind;
    private final MapCodec<PhiTurretBlock> codec;

    public PhiTurretBlock(TurretKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        this.codec = simpleCodec(p -> new PhiTurretBlock(kind, p));
        registerDefaultState(stateDefinition.any().setValue(FORMED, false).setValue(FACING, Direction.NORTH));
    }

    public TurretKind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    /** Formed barrel is drawn by the mount BER — no collision so bolts clear the muzzle. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FORMED) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(FORMED) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Keep a full click-box when formed so the barrel cell remains usable.
        return Shapes.block();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (!level.isClientSide()) {
            BlockPos mount = TurretAssembly.findMountForBarrel(level, pos);
            if (mount != null) {
                TurretAssembly.syncFormed(level, mount);
            }
        }
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        if (!level.isClientSide()) {
            BlockPos mount = TurretAssembly.findMountForBarrel(level, pos);
            if (mount != null) {
                level.scheduleTick(mount, level.getBlockState(mount).getBlock(), 1);
            }
        }
        return state;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            BlockPos mount = TurretAssembly.findMountForBarrel(level, pos);
            // After removal, neighbor update won't see this barrel — check all adjacent mounts.
            for (Direction dir : Direction.values()) {
                BlockPos adj = pos.relative(dir);
                if (level.getBlockState(adj).getBlock() instanceof TurretMountBlock) {
                    TurretAssembly.syncFormed(level, adj);
                }
            }
            if (mount != null) {
                TurretAssembly.syncFormed(level, mount);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockPos mountPos = TurretAssembly.findMountForBarrel(level, pos);
        if (mountPos == null || !(level.getBlockEntity(mountPos) instanceof PhiTurretBlockEntity turret)) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (!TechnomagicGates.checkOperate(serverPlayer, TechnomagicEra.IV)) {
            return InteractionResult.FAIL;
        }
        if (!level.getBlockState(mountPos).getValue(TurretMountBlock.FORMED)) {
            return InteractionResult.FAIL;
        }
        serverPlayer.openMenu(turret, buf -> buf.writeBlockPos(mountPos));
        return InteractionResult.CONSUME;
    }
}
