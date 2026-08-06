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
import net.minecraft.world.item.Item;
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

/** Blue cauldron / essence alembic — campfire-style brew without GUI. */
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
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof EssenceAlembicBlockEntity alembic)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Take finished potion
        if (stack.isEmpty() && !alembic.getResult().isEmpty()) {
            if (!level.isClientSide()) {
                ItemStack out = alembic.takeResult();
                if (!player.getInventory().add(out)) {
                    player.drop(out, false);
                }
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.7f, 1.2f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        // Insert base flask
        if (stack.is(ModItems.PHI_FLASK_WATER.get()) && alembic.getBase().isEmpty() && alembic.getResult().isEmpty()) {
            if (!level.isClientSide()) {
                alembic.setBase(new ItemStack(ModItems.PHI_FLASK_WATER.get()));
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.6f, 1.0f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        // Start brew with reagent
        Item potion = potionForReagent(stack);
        if (potion != null
                && !alembic.getBase().isEmpty()
                && alembic.getResult().isEmpty()
                && !alembic.isCooking()) {
            if (!EssenceBurnerBlock.hasNeighborHeat(level, pos)) {
                if (!level.isClientSide()) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("message.effecoria.alembic_no_heat"),
                            true);
                }
                return ItemInteractionResult.FAIL;
            }
            if (!level.isClientSide()) {
                alembic.startCook(potion);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.5f, 1.3f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Nullable
    static Item potionForReagent(ItemStack stack) {
        if (stack.is(ModItems.ESSENITE_DUST.get())) {
            return ModItems.POTION_PHI_TONIC.get();
        }
        if (stack.is(ModItems.ESSONITE_SHARD.get())) {
            return ModItems.POTION_PHI_RESONANCE.get();
        }
        if (stack.is(ModItems.PURE_ESSONITE.get())) {
            return ModItems.POTION_PHI_STIMULANT.get();
        }
        return null;
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
