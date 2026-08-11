package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModItems;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.core.technomagic.TechnomagicEra;
import com.effecoria.core.technomagic.TechnomagicGates;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.WeakHashMap;

import javax.annotation.Nullable;

/** Era V Portal Gate — linked pair teleports players for Φ-power cost. */
public final class PortalGateBlock extends BaseEntityBlock {
    public static final MapCodec<PortalGateBlock> CODEC = simpleCodec(PortalGateBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private static final VoxelShape SHAPE_NS = Block.box(1.0, 0.0, 6.5, 15.0, 16.0, 9.5);
    private static final VoxelShape SHAPE_EW = Block.box(6.5, 0.0, 1.0, 9.5, 16.0, 15.0);

    /** First RMB with Resonance Focus stores this gate; second links both. */
    private static final WeakHashMap<Player, BlockPos> PENDING_LINK = new WeakHashMap<>();

    public PortalGateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PortalGateBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntities.PORTAL_GATE.get(), PortalGateBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
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
        if (!stack.is(ModItems.RESONANCE_FOCUS.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(level.getBlockEntity(pos) instanceof PortalGateBlockEntity gate)) {
            return ItemInteractionResult.FAIL;
        }
        if (!TechnomagicGates.checkOperate(serverPlayer, TechnomagicEra.V)) {
            return ItemInteractionResult.FAIL;
        }

        BlockPos pending = PENDING_LINK.get(player);
        if (pending == null || pending.equals(pos)) {
            PENDING_LINK.put(player, pos.immutable());
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.effecoria.portal_gate_pending"), true);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.55f, 1.2f);
            return ItemInteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(pending) instanceof PortalGateBlockEntity other)) {
            PENDING_LINK.put(player, pos.immutable());
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.effecoria.portal_gate_pending"), true);
            return ItemInteractionResult.SUCCESS;
        }

        gate.linkWith(other);
        PENDING_LINK.remove(player);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.7f, 1.1f);
        level.playSound(null, pending, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.7f, 1.1f);
        player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.effecoria.portal_gate_linked"), true);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof PortalGateBlockEntity gate)) {
            return;
        }
        if (!state.getValue(ACTIVE) || !gate.hasPartner()) {
            return;
        }
        if (gate.isPlayerOnCooldown(player)) {
            return;
        }
        if (!PhiPower.consumeTick(level, pos, PortalGateBlockEntity.TELEPORT_POWER_COST)) {
            return;
        }
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        gate.teleportPlayer(server, player);
    }
}
