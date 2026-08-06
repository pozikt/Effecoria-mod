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

/** Dense metal mesh (iron stand-in for lead) — bleeds off entropy. */
public final class LeadFilterItem extends Item {
    private static final int COOLDOWN_TICKS = 100;
    private static final float ENTROPY_CLEAR = 0.35f;

    public LeadFilterItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PlayerPsiData data = PsiHelper.get(serverPlayer);
            data.setEntropyB(Math.max(0f, data.entropyB() - ENTROPY_CLEAR));
            PsiHelper.set(serverPlayer, data);
            serverPlayer.syncData(ModAttachments.PSI.get());
            serverPlayer.removeEffect(MobEffects.CONFUSION);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.ANVIL_LAND,
                    SoundSource.PLAYERS,
                    0.25f,
                    1.8f);
            player.displayClientMessage(Component.translatable("message.effecoria.lead_filter_used"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.effecoria.lead_filter.hint"));
    }
}
