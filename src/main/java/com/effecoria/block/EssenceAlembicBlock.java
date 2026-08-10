package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.alchemy.PhiHeat;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.joml.Vector3f;

import javax.annotation.Nullable;

/** Blue cauldron / essence alembic — GUI brew with neighbor Φ-heat. */
public final class EssenceAlembicBlock extends BaseEntityBlock {
    public static final MapCodec<EssenceAlembicBlock> CODEC = simpleCodec(EssenceAlembicBlock::new);
    public static final int COOK_TIME = 200;
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);
    private static final DustParticleOptions CYAN =
            new DustParticleOptions(new Vector3f(0.35f, 0.85f, 1.0f), 1.0f);
    private static final DustParticleOptions GOLD =
            new DustParticleOptions(new Vector3f(1.0f, 0.85f, 0.3f), 0.8f);

    public EssenceAlembicBlock(Properties properties) {
        super(properties);
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
        return new EssenceAlembicBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntities.ESSENCE_ALEMBIC.get(), EssenceAlembicBlockEntity::serverTick);
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

    private static void open(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof EssenceAlembicBlockEntity alembic) {
            if (!com.effecoria.core.technomagic.TechnomagicGates.checkOperate(
                    serverPlayer, com.effecoria.core.technomagic.TechnomagicEra.II)) {
                return;
            }
            serverPlayer.openMenu(alembic, buf -> buf.writeBlockPos(pos));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof EssenceAlembicBlockEntity alembic) || !alembic.isCooking()) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.85;
        double z = pos.getZ() + 0.5;
        level.addParticle(CYAN, x + (random.nextDouble() - 0.5) * 0.3, y, z + (random.nextDouble() - 0.5) * 0.3, 0, 0.02, 0);
        if (random.nextBoolean()) {
            level.addParticle(GOLD, x, y + 0.1, z, 0, 0.01, 0);
        }
        if (!PhiHeat.hasNeighborHeat(level, pos) && random.nextFloat() < 0.1f) {
            level.addParticle(
                    net.minecraft.core.particles.ParticleTypes.SMOKE, x, y + 0.2, z, 0, 0.01, 0);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof EssenceAlembicBlockEntity alembic) {
            alembic.drops(level, pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
