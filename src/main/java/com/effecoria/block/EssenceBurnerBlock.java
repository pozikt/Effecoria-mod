package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModItems;
import com.effecoria.core.alchemy.HeatLevel;
import com.effecoria.core.alchemy.PhiHeat;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.joml.Vector3f;

import javax.annotation.Nullable;

/** Flameless Φ-burner — GUI fuel/catalyst/temp; radiates {@link HeatLevel} to neighbors. */
public final class EssenceBurnerBlock extends BaseEntityBlock {
    public static final MapCodec<EssenceBurnerBlock> CODEC = simpleCodec(EssenceBurnerBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 8.0, 14.0);
    private static final DustParticleOptions BLUE =
            new DustParticleOptions(new Vector3f(0.25f, 0.65f, 1.0f), 0.9f);
    private static final DustParticleOptions GOLD =
            new DustParticleOptions(new Vector3f(1.0f, 0.85f, 0.35f), 1.0f);

    public EssenceBurnerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EssenceBurnerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntities.ESSENCE_BURNER.get(), EssenceBurnerBlockEntity::serverTick);
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
        if (!(level.getBlockEntity(pos) instanceof EssenceBurnerBlockEntity burner)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // Quick-feed dust into fuel slot without opening
        if (stack.is(ModItems.ESSENITE_DUST.get()) && player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                ItemStack fuel = burner.getItem(EssenceBurnerBlockEntity.SLOT_FUEL);
                if (fuel.isEmpty()) {
                    burner.setItem(EssenceBurnerBlockEntity.SLOT_FUEL, stack.split(1));
                } else if (ItemStack.isSameItemSameComponents(fuel, stack)
                        && fuel.getCount() < fuel.getMaxStackSize()) {
                    fuel.grow(1);
                    stack.shrink(1);
                } else {
                    burner.addFuel(EssenceBurnerBlockEntity.DUST_FUEL_TICKS);
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
                if (burner.fuelTicks() > 0) {
                    level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
                }
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.35f, 1.6f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        open(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    private static void open(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof EssenceBurnerBlockEntity burner) {
            serverPlayer.openMenu(burner, buf -> buf.writeBlockPos(pos));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.55;
        double z = pos.getZ() + 0.5;
        DustParticleOptions particle = BLUE;
        if (level.getBlockEntity(pos) instanceof EssenceBurnerBlockEntity burner
                && burner.heatLevel() == HeatLevel.HIGH) {
            particle = GOLD;
        }
        level.addParticle(
                particle, x + (random.nextDouble() - 0.5) * 0.25, y, z + (random.nextDouble() - 0.5) * 0.25, 0, 0.01, 0);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof EssenceBurnerBlockEntity burner) {
            net.minecraft.world.Containers.dropContents(level, pos, burner);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /** @deprecated use {@link PhiHeat#consumeNeighborHeat} */
    @Deprecated
    public static boolean consumeNeighborHeat(ServerLevel level, BlockPos alembicPos) {
        return PhiHeat.consumeNeighborHeat(level, alembicPos);
    }

    /** @deprecated use {@link PhiHeat#hasNeighborHeat} */
    @Deprecated
    public static boolean hasNeighborHeat(Level level, BlockPos alembicPos) {
        return PhiHeat.hasNeighborHeat(level, alembicPos);
    }
}
