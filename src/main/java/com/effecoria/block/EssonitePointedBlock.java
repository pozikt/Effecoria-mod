package com.effecoria.block;

import com.google.common.annotations.VisibleForTesting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Essonite Φ-conductor spikes — dripstone geometry with self-stacking columns (Φ-columns when tips merge).
 */
public final class EssonitePointedBlock extends Block implements SimpleWaterloggedBlock {
    public static final EnumProperty<Direction> TIP_DIRECTION = BlockStateProperties.VERTICAL_DIRECTION;
    public static final EnumProperty<DripstoneThickness> THICKNESS = BlockStateProperties.DRIPSTONE_THICKNESS;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape TIP_SHAPE_UP = Block.box(5.0, 0.0, 5.0, 11.0, 11.0, 11.0);
    private static final VoxelShape TIP_SHAPE_DOWN = Block.box(5.0, 5.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape FRUSTUM_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    private static final VoxelShape MIDDLE_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
    private static final VoxelShape BASE_SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    public EssonitePointedBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition
                .any()
                .setValue(TIP_DIRECTION, Direction.UP)
                .setValue(THICKNESS, DripstoneThickness.TIP)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIP_DIRECTION, THICKNESS, WATERLOGGED);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isValidPlacement(level, pos, state.getValue(TIP_DIRECTION));
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (direction != Direction.UP && direction != Direction.DOWN) {
            return state;
        }
        Direction tip = state.getValue(TIP_DIRECTION);
        if (direction == tip.getOpposite() && !this.canSurvive(state, level, pos)) {
            level.scheduleTick(pos, this, 1);
            return state;
        }
        DripstoneThickness thickness = calculateThickness(level, pos, tip);
        return state.setValue(THICKNESS, thickness);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!this.canSurvive(state, level, pos)) {
            if (state.getValue(TIP_DIRECTION) == Direction.UP) {
                level.destroyBlock(pos, true);
            } else {
                FallingBlockEntity.fall(level, pos, state.setValue(THICKNESS, DripstoneThickness.TIP));
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(THICKNESS) != DripstoneThickness.TIP
                && state.getValue(THICKNESS) != DripstoneThickness.TIP_MERGE) {
            return;
        }
        if (random.nextInt(16) != 0) {
            return;
        }
        Direction tip = state.getValue(TIP_DIRECTION);
        double x = pos.getX() + 0.5;
        double y = tip == Direction.UP ? pos.getY() + 0.85 : pos.getY() + 0.15;
        double z = pos.getZ() + 0.5;
        level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, tip == Direction.UP ? 0.01 : -0.01, 0.0);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelAccessor level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction preferred =
                context.getNearestLookingVerticalDirection().getOpposite();
        Direction tip = calculateTipDirection(level, pos, preferred);
        if (tip == null) {
            return null;
        }
        boolean water = level.getFluidState(pos).getType() == Fluids.WATER;
        return this.defaultBlockState()
                .setValue(TIP_DIRECTION, tip)
                .setValue(THICKNESS, calculateThickness(level, pos, tip))
                .setValue(WATERLOGGED, water);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        DripstoneThickness thickness = state.getValue(THICKNESS);
        VoxelShape shape;
        if (thickness == DripstoneThickness.TIP_MERGE) {
            shape = BASE_SHAPE;
        } else if (thickness == DripstoneThickness.TIP) {
            shape = state.getValue(TIP_DIRECTION) == Direction.DOWN ? TIP_SHAPE_DOWN : TIP_SHAPE_UP;
        } else if (thickness == DripstoneThickness.FRUSTUM) {
            shape = FRUSTUM_SHAPE;
        } else if (thickness == DripstoneThickness.MIDDLE) {
            shape = MIDDLE_SHAPE;
        } else {
            shape = BASE_SHAPE;
        }
        return shape;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    public static BlockState withThickness(BlockState state, DripstoneThickness thickness) {
        return state.setValue(THICKNESS, thickness);
    }

    @VisibleForTesting
    public static DripstoneThickness calculateThickness(LevelReader level, BlockPos pos, Direction tip) {
        Direction opp = tip.getOpposite();
        BlockState towardTip = level.getBlockState(pos.relative(tip));
        if (isSamePointed(towardTip) && towardTip.getValue(TIP_DIRECTION) != tip) {
            return DripstoneThickness.TIP_MERGE;
        }
        if (!isSamePointed(towardTip)) {
            return DripstoneThickness.TIP;
        }
        DripstoneThickness next = towardTip.getValue(THICKNESS);
        if (next == DripstoneThickness.TIP || next == DripstoneThickness.TIP_MERGE) {
            return DripstoneThickness.FRUSTUM;
        }
        BlockState towardBase = level.getBlockState(pos.relative(opp));
        return !isSamePointed(towardBase) ? DripstoneThickness.BASE : DripstoneThickness.MIDDLE;
    }

    @Nullable
    private static Direction calculateTipDirection(LevelReader level, BlockPos pos, Direction preferred) {
        Direction secondary = preferred.getOpposite();
        if (isValidPlacement(level, pos, preferred)) {
            return preferred;
        }
        return isValidPlacement(level, pos, secondary) ? secondary : null;
    }

    private static boolean isValidPlacement(LevelReader level, BlockPos pos, Direction tip) {
        BlockPos support = pos.relative(tip.getOpposite());
        BlockState supportState = level.getBlockState(support);
        if (isSamePointed(supportState)) {
            return supportState.getValue(TIP_DIRECTION) == tip;
        }
        return supportState.isFaceSturdy(level, support, tip);
    }

    private static boolean isSamePointed(BlockState state) {
        return state.getBlock() instanceof EssonitePointedBlock;
    }
}
