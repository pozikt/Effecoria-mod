package com.effecoria.content;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bone grit fertilizer — works on Φ-soil like essonite dust, with a stronger second pulse.
 */
public final class BoneGritItem extends Item {
    public BoneGritItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!isPhiGrowable(state)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }

        boolean used = false;
        if (state.is(ModBlocks.PHI_SAPLING.get())) {
            used = BoneMealItem.applyBonemeal(stack.copy(), level, pos, player);
            // Second pulse — 2× bone-meal strength on Φ-saplings.
            if (used) {
                BoneMealItem.applyBonemeal(stack.copy(), level, pos, player);
            }
            if (used && player != null && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        } else if (state.is(ModBlocks.PHI_DIRT.get()) || state.is(ModBlocks.PHI_GRASS.get())) {
            BlockPos above = pos.above();
            if (level.getBlockState(above).isAir()) {
                level.setBlock(above, ModBlocks.PHI_BLADES.get().defaultBlockState(), 3);
                used = true;
                trySpreadBlades(server, above);
            }
        } else if (state.is(ModBlocks.PHI_BLADES.get())) {
            used = trySpreadBlades(server, pos);
            if (used) {
                trySpreadBlades(server, pos);
            }
        }

        if (!used) {
            return InteractionResult.FAIL;
        }
        if (!state.is(ModBlocks.PHI_SAPLING.get())
                && player != null
                && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.levelEvent(1505, pos, 0);
        server.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 0.95f, 0.85f);
        return InteractionResult.SUCCESS;
    }

    private static boolean trySpreadBlades(ServerLevel level, BlockPos origin) {
        for (int i = 0; i < 10; i++) {
            BlockPos target = origin.offset(
                    level.random.nextInt(5) - 2, 0, level.random.nextInt(5) - 2);
            if (target.equals(origin)) {
                continue;
            }
            BlockState below = level.getBlockState(target.below());
            if (level.getBlockState(target).isAir()
                    && (below.is(ModBlocks.PHI_DIRT.get()) || below.is(ModBlocks.PHI_GRASS.get()))) {
                level.setBlock(target, ModBlocks.PHI_BLADES.get().defaultBlockState(), 3);
                return true;
            }
        }
        return false;
    }

    private static boolean isPhiGrowable(BlockState state) {
        return state.is(ModBlocks.PHI_DIRT.get())
                || state.is(ModBlocks.PHI_GRASS.get())
                || state.is(ModBlocks.PHI_BLADES.get())
                || state.is(ModBlocks.PHI_SAPLING.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.effecoria.bone_grit.hint"));
    }
}
