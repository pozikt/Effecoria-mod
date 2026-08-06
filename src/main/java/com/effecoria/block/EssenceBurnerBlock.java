package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModItems;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
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

/** Flameless Φ-burner — fueled by essonite dust until Φ-oil exists. */
public final class EssenceBurnerBlock extends BaseEntityBlock {
    public static final MapCodec<EssenceBurnerBlock> CODEC = simpleCodec(EssenceBurnerBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 6.0, 14.0);
    private static final DustParticleOptions BLUE =
            new DustParticleOptions(new Vector3f(0.25f, 0.65f, 1.0f), 0.9f);

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
        if (!stack.is(ModItems.ESSENITE_DUST.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide()) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            burner.addFuel(EssenceBurnerBlockEntity.DUST_FUEL_TICKS);
            level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.35f, 1.6f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.35;
        double z = pos.getZ() + 0.5;
        level.addParticle(BLUE, x + (random.nextDouble() - 0.5) * 0.25, y, z + (random.nextDouble() - 0.5) * 0.25, 0, 0.01, 0);
    }

    /** Drain one tick of heat from a neighboring burner; returns true if heat was available. */
    public static boolean consumeNeighborHeat(ServerLevel level, BlockPos alembicPos) {
        for (BlockPos check : new BlockPos[] {
            alembicPos.below(), alembicPos.north(), alembicPos.south(), alembicPos.east(), alembicPos.west()
        }) {
            if (level.getBlockEntity(check) instanceof EssenceBurnerBlockEntity burner && burner.consumeFuelTick()) {
                BlockState st = level.getBlockState(check);
                if (st.getBlock() instanceof EssenceBurnerBlock) {
                    boolean lit = burner.fuelTicks() > 0;
                    if (st.getValue(LIT) != lit) {
                        level.setBlock(check, st.setValue(LIT, lit), Block.UPDATE_CLIENTS);
                    }
                }
                return true;
            }
        }
        return false;
    }

    public static boolean hasNeighborHeat(Level level, BlockPos alembicPos) {
        for (BlockPos check : new BlockPos[] {
            alembicPos.below(), alembicPos.north(), alembicPos.south(), alembicPos.east(), alembicPos.west()
        }) {
            if (level.getBlockEntity(check) instanceof EssenceBurnerBlockEntity burner && burner.fuelTicks() > 0) {
                return true;
            }
        }
        return false;
    }
}
