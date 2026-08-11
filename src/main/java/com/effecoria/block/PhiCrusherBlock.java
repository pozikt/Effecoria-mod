package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItems;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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

/** Φ-crusher base (2×1×1 bottom). Needs hopper above to form. */
public final class PhiCrusherBlock extends BaseEntityBlock {
    public static final MapCodec<PhiCrusherBlock> CODEC = simpleCodec(PhiCrusherBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public PhiCrusherBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false)
                .setValue(FORMED, false));
    }

    public static boolean isFormed(LevelAccessor level, BlockPos basePos) {
        BlockState above = level.getBlockState(basePos.above());
        if (!(above.getBlock() instanceof PhiCrusherHopperBlock)) {
            return false;
        }
        BlockState base = level.getBlockState(basePos);
        if (!(base.getBlock() instanceof PhiCrusherBlock)) {
            return false;
        }
        return above.getValue(PhiCrusherHopperBlock.FACING) == base.getValue(FACING);
    }

    public static void syncFormed(LevelAccessor level, BlockPos basePos) {
        BlockState base = level.getBlockState(basePos);
        if (!(base.getBlock() instanceof PhiCrusherBlock)) {
            return;
        }
        boolean formed = isFormed(level, basePos);
        if (base.getValue(FORMED) != formed) {
            level.setBlock(basePos, base.setValue(FORMED, formed), 3);
        }
        BlockPos above = basePos.above();
        BlockState hopper = level.getBlockState(above);
        if (hopper.getBlock() instanceof PhiCrusherHopperBlock
                && hopper.getValue(PhiCrusherHopperBlock.FORMED) != formed) {
            level.setBlock(above, hopper.setValue(PhiCrusherHopperBlock.FORMED, formed), 3);
        }
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, FORMED);
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
        return new PhiCrusherBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntities.PHI_CRUSHER.get(), PhiCrusherBlockEntity::serverTick);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (!level.isClientSide()) {
            syncFormed(level, pos);
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
            level.scheduleTick(pos, this, 1);
        }
        return state;
    }

    @Override
    protected void tick(
            BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        syncFormed(level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        PhiCrusherBlockEntity.openGui(level, pos, player);
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
        if (stack.is(ModItems.LEAD_FOIL.get())
                && level.getBlockEntity(pos) instanceof PhiCrusherBlockEntity crusher
                && crusher.cleanOmega(player)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        PhiCrusherBlockEntity.openGui(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof PhiCrusherBlockEntity crusher) {
                net.minecraft.world.Containers.dropContents(level, pos, crusher);
            }
            BlockPos above = pos.above();
            if (level.getBlockState(above).is(ModBlocks.PHI_CRUSHER_HOPPER.get())) {
                // leave hopper; it just unforms
                syncFormed(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
