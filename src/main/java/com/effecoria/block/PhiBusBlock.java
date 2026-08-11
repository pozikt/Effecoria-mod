package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModBlocks;
import com.effecoria.core.alchemy.PhiBusNetwork;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Φ-bus cable — thin continuous wire with per-face arms (PipeBlock-style).
 * Arms only exist toward connected neighbors so a row forms one unbroken line.
 */
public final class PhiBusBlock extends BaseEntityBlock {
    public static final MapCodec<PhiBusBlock> CODEC = simpleCodec(PhiBusBlock::new);

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST = PipeBlock.WEST;
    public static final BooleanProperty UP = PipeBlock.UP;
    public static final BooleanProperty DOWN = PipeBlock.DOWN;

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION;

    // 4px wire cross-section (6..10) — reads as a cable, not a block.
    private static final VoxelShape CORE = Block.box(6.0, 6.0, 6.0, 10.0, 10.0, 10.0);
    private static final VoxelShape ARM_N = Block.box(6.0, 6.0, 0.0, 10.0, 10.0, 10.0);
    private static final VoxelShape ARM_S = Block.box(6.0, 6.0, 6.0, 10.0, 10.0, 16.0);
    private static final VoxelShape ARM_W = Block.box(0.0, 6.0, 6.0, 10.0, 10.0, 10.0);
    private static final VoxelShape ARM_E = Block.box(6.0, 6.0, 6.0, 16.0, 10.0, 10.0);
    private static final VoxelShape ARM_D = Block.box(6.0, 0.0, 6.0, 10.0, 10.0, 10.0);
    private static final VoxelShape ARM_U = Block.box(6.0, 6.0, 6.0, 10.0, 16.0, 10.0);

    public PhiBusBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition
                .any()
                .setValue(POWERED, false)
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        if (state.getValue(NORTH)) shape = Shapes.or(shape, ARM_N);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, ARM_S);
        if (state.getValue(WEST)) shape = Shapes.or(shape, ARM_W);
        if (state.getValue(EAST)) shape = Shapes.or(shape, ARM_E);
        if (state.getValue(UP)) shape = Shapes.or(shape, ARM_U);
        if (state.getValue(DOWN)) shape = Shapes.or(shape, ARM_D);
        return shape;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PhiBusBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntities.PHI_BUS.get(), PhiBusBlockEntity::serverTick);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (!level.isClientSide() && !oldState.is(state.getBlock())) {
            updateConnections(level, pos, state);
            updateNeighborBuses(level, pos);
        }
    }

    @Override
    protected void neighborChanged(
            BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide()) {
            updateConnections(level, pos, state);
            if (level.getBlockEntity(pos) instanceof PhiBusBlockEntity bus) {
                bus.markDirtyNetwork();
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
        if (level instanceof Level realLevel && !realLevel.isClientSide()) {
            BooleanProperty prop = PROPERTY_BY_DIRECTION.get(direction);
            boolean connect = canConnectTo(realLevel, neighborPos);
            if (state.getValue(prop) != connect) {
                return state.setValue(prop, connect);
            }
        }
        return state;
    }

    private static void updateConnections(Level level, BlockPos pos, BlockState state) {
        BlockState next = withConnections(state, level, pos);
        if (next != state) {
            level.setBlock(pos, next, Block.UPDATE_ALL);
        }
    }

    private static void updateNeighborBuses(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos adj = pos.relative(dir);
            BlockState ns = level.getBlockState(adj);
            if (ns.is(ModBlocks.PHI_BUS.get())) {
                BlockState updated = withConnections(ns, level, adj);
                if (updated != ns) {
                    level.setBlock(adj, updated, Block.UPDATE_ALL);
                }
            }
        }
    }

    /** Preserve POWERED; recompute all face connections. */
    public static BlockState withConnections(BlockState state, Level level, BlockPos pos) {
        return state
                .setValue(NORTH, canConnectTo(level, pos.north()))
                .setValue(EAST, canConnectTo(level, pos.east()))
                .setValue(SOUTH, canConnectTo(level, pos.south()))
                .setValue(WEST, canConnectTo(level, pos.west()))
                .setValue(UP, canConnectTo(level, pos.above()))
                .setValue(DOWN, canConnectTo(level, pos.below()));
    }

    /** Wire joins other buses, Φ injectors, and any adjacent machine (block entity). */
    public static boolean canConnectTo(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (state.is(ModBlocks.PHI_BUS.get())) {
            return true;
        }
        if (PhiBusNetwork.resolveInjector(level, pos) != null) {
            return true;
        }
        return level.getBlockEntity(pos) != null;
    }
}
