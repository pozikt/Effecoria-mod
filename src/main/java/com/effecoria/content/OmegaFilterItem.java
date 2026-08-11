package com.effecoria.content;

import java.util.List;

import com.effecoria.block.ForgeReactorBlockEntity;
import com.effecoria.block.PhiCrusherBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Obsidian grit + lead foil filter — clears Ω meters on crusher / forge. */
public final class OmegaFilterItem extends Item {
    public OmegaFilterItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        boolean cleared = false;
        if (be instanceof PhiCrusherBlockEntity crusher) {
            cleared = crusher.clearOmegaMeter();
        } else if (be instanceof ForgeReactorBlockEntity forge) {
            cleared = forge.clearOmegaMeter();
        }

        if (!cleared) {
            return InteractionResult.FAIL;
        }
        if (player != null && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.55f, 1.4f);
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.effecoria.omega_filter_used"), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.effecoria.omega_filter.hint"));
    }
}
