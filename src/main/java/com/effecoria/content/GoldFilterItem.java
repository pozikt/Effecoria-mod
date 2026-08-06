package com.effecoria.content;

import java.util.List;

import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Gold mesh + Φ-paper — converts a raw Φ-water bucket in the other hand. */
public final class GoldFilterItem extends Item {
    public GoldFilterItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack filter = player.getItemInHand(hand);
        InteractionHand otherHand =
                hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack other = player.getItemInHand(otherHand);
        if (!other.is(ModItems.PHI_WATER_BUCKET.get())) {
            return InteractionResultHolder.pass(filter);
        }
        if (!level.isClientSide()) {
            if (!player.getAbilities().instabuild) {
                filter.shrink(1);
            }
            player.setItemInHand(otherHand, new ItemStack(ModItems.PURIFIED_PHI_WATER_BUCKET.get()));
            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.BOTTLE_FILL,
                    SoundSource.PLAYERS,
                    0.6f,
                    1.4f);
            player.displayClientMessage(Component.translatable("message.effecoria.gold_filter_used"), true);
        }
        return InteractionResultHolder.sidedSuccess(filter, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.effecoria.gold_filter.hint"));
    }
}
