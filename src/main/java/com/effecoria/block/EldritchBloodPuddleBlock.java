package com.effecoria.block;

import com.effecoria.content.ModItems;
import com.effecoria.content.ModParticleTypes;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Eldritch Blood puddle — oily black-purple Ω fluid near active Scar cracks.
 * Empty blood vial scoops an Ω-Blood Vial.
 */
public final class EldritchBloodPuddleBlock extends Block {
    public static final MapCodec<EldritchBloodPuddleBlock> CODEC = simpleCodec(EldritchBloodPuddleBlock::new);
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);

    public EldritchBloodPuddleBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
        if (!stack.is(ModItems.BLOOD_VIAL_EMPTY.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        ItemStack filled = new ItemStack(ModItems.OMEGA_BLOOD_VIAL.get());
        stack.shrink(1);
        if (stack.isEmpty()) {
            player.setItemInHand(hand, filled);
        } else if (!player.getInventory().add(filled)) {
            player.drop(filled, false);
        }
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.7f, 0.7f);
        if (player.getRandom().nextFloat() < 0.55f) {
            level.removeBlock(pos, false);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            level.addParticle(
                    ModParticleTypes.CORRUPTION_BLOOD.get(),
                    pos.getX() + 0.2 + random.nextDouble() * 0.6,
                    pos.getY() + 0.12,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                    0,
                    0.01,
                    0);
        }
    }
}
