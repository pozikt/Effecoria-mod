package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Shared turret mount (half-slab). Bottom attaches to floor / wall / ceiling.
 * Φ-power is drawn at this block. Barrel sits on the outward side to form the turret.
 */
public final class TurretMountBlock extends BaseEntityBlock {
    public static final MapCodec<TurretMountBlock> CODEC = simpleCodec(TurretMountBlock::new);
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final EnumProperty<TurretKind> KIND = EnumProperty.create("kind", TurretKind.class);

    private static final VoxelShape FLOOR = Block.box(0, 0, 0, 16, 8, 16);
    private static final VoxelShape CEILING = Block.box(0, 8, 0, 16, 16, 16);
    private static final VoxelShape WALL_N = Block.box(2, 4, 8, 14, 12, 16);
    private static final VoxelShape WALL_S = Block.box(2, 4, 0, 14, 12, 8);
    private static final VoxelShape WALL_W = Block.box(8, 4, 2, 16, 12, 14);
    private static final VoxelShape WALL_E = Block.box(0, 4, 2, 8, 12, 14);

    public TurretMountBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition
                .any()
                .setValue(FACE, AttachFace.FLOOR)
                .setValue(FACING, Direction.NORTH)
                .setValue(FORMED, false)
                .setValue(LIT, false)
                .setValue(KIND, TurretKind.NONE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING, FORMED, LIT, KIND);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> FLOOR;
            case CEILING -> CEILING;
            case WALL -> switch (state.getValue(FACING)) {
                case SOUTH -> WALL_S;
                case WEST -> WALL_W;
                case EAST -> WALL_E;
                default -> WALL_N;
            };
        };
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction dir : context.getNearestLookingDirections()) {
            BlockState state;
            if (dir.getAxis() == Direction.Axis.Y) {
                state = defaultBlockState()
                        .setValue(FACE, dir == Direction.UP ? AttachFace.CEILING : AttachFace.FLOOR)
                        .setValue(FACING, context.getHorizontalDirection());
            } else {
                state = defaultBlockState()
                        .setValue(FACE, AttachFace.WALL)
                        .setValue(FACING, dir.getOpposite());
            }
            if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                return state;
            }
        }
        return null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canAttach(level, pos, getConnectedDirection(state).getOpposite());
    }

    private static Direction getConnectedDirection(BlockState state) {
        return switch (state.getValue(FACE)) {
            case CEILING -> Direction.DOWN;
            case FLOOR -> Direction.UP;
            default -> state.getValue(FACING);
        };
    }

    private static boolean canAttach(LevelReader level, BlockPos pos, Direction supportDir) {
        BlockPos support = pos.relative(supportDir);
        return level.getBlockState(support).isFaceSturdy(level, support, supportDir.getOpposite());
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        if (getConnectedDirection(state).getOpposite() == direction && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, 1);
        }
        return state;
    }

    @Override
    protected void tick(
            BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        TurretAssembly.syncFormed(level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (!level.isClientSide()) {
            TurretAssembly.syncFormed(level, pos);
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // Formed hull is drawn by BER; keep collision from the mount slab.
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PhiTurretBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntities.PHI_TURRET.get(), PhiTurretBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof PhiTurretBlockEntity turret) {
            if (!TechnomagicGates.checkOperate(serverPlayer, TechnomagicEra.IV)) {
                return InteractionResult.FAIL;
            }
            if (!state.getValue(FORMED)) {
                serverPlayer.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("gui.effecoria.phi_turret.need_barrel"), true);
                return InteractionResult.FAIL;
            }
            serverPlayer.openMenu(turret, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockPos barrel = TurretAssembly.barrelPos(pos, state);
            BlockState barrelState = level.getBlockState(barrel);
            if (barrelState.getBlock() instanceof PhiTurretBlock && barrelState.getValue(PhiTurretBlock.FORMED)) {
                level.setBlock(barrel, barrelState.setValue(PhiTurretBlock.FORMED, false), 3);
            }
            if (level.getBlockEntity(pos) instanceof PhiTurretBlockEntity turret) {
                net.minecraft.world.Containers.dropContents(level, pos, turret);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
