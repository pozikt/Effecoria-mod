package com.effecoria.block;

import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Village mortar — instant grind of essonite materials into dust (Φ-piezo sparks).
 */
public final class MortarAndPestleBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 8.0, 14.0);

    public MortarAndPestleBlock(Properties properties) {
        super(properties);
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
        int dustCount = dustFrom(stack);
        if (dustCount <= 0) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide()) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            ItemStack dust = new ItemStack(ModItems.ESSENITE_DUST.get(), dustCount);
            if (!player.getInventory().add(dust)) {
                player.drop(dust, false);
            }
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.55f, 1.35f);
            if (level.random.nextFloat() < 0.45f) {
                for (int i = 0; i < 4; i++) {
                    level.addParticle(
                            ParticleTypes.END_ROD,
                            pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.3,
                            pos.getY() + 0.55,
                            pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.3,
                            0,
                            0.02,
                            0);
                }
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    private static int dustFrom(ItemStack stack) {
        if (stack.is(ModItems.ESSONITE_SHARD.get())) {
            return 2;
        }
        if (stack.is(ModItems.PURE_ESSONITE.get())) {
            return 4;
        }
        if (stack.is(ModBlocks.ESSENITE_ORE.get().asItem())
                || stack.is(ModBlocks.DEEPSLATE_ESSENITE_ORE.get().asItem())
                || stack.is(ModBlocks.GRANITE_ESSENITE_ORE.get().asItem())
                || stack.is(ModBlocks.ANDESITE_ESSENITE_ORE.get().asItem())
                || stack.is(ModBlocks.DIORITE_ESSENITE_ORE.get().asItem())
                || stack.is(ModBlocks.TUFF_ESSENITE_ORE.get().asItem())
                || stack.is(ModBlocks.BASALT_ESSENITE_ORE.get().asItem())
                || stack.is(ModBlocks.ESSONITE_CRYSTAL.get().asItem())
                || stack.is(ModBlocks.ESSONITE_BLOCK.get().asItem())) {
            return 2;
        }
        return 0;
    }
}
