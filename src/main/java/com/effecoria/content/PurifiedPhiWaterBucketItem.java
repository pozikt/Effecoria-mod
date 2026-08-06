package com.effecoria.content;

import java.util.List;

import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Gold-filtered Φ-water — drink only (does not place fluid). Restores Ψ for initiated mages.
 */
public final class PurifiedPhiWaterBucketItem extends Item {
    private static final int COOLDOWN_TICKS = 80;

    public PurifiedPhiWaterBucketItem(Properties properties) {
        super(properties.stacksTo(1).craftRemainder(Items.BUCKET));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
            player.awardStat(Stats.ITEM_USED.get(this));
            PlayerPsiData data = PsiHelper.get(player);
            if (data.initiated()) {
                float gain = Math.max(10f, data.maxPsi() * 0.22f);
                data.setCurrentPsi(data.currentPsi() + gain);
                PsiHelper.set(player, data);
                player.syncData(ModAttachments.PSI.get());
                level.playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.PLAYER_LEVELUP,
                        SoundSource.PLAYERS,
                        0.35f,
                        1.6f);
                player.displayClientMessage(
                        Component.translatable("message.effecoria.purified_phi_water_charged"), true);
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                player.displayClientMessage(
                        Component.translatable("message.effecoria.purified_phi_water_uneasy"), true);
            }
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            return new ItemStack(Items.BUCKET);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.effecoria.purified_phi_water_bucket.hint"));
    }
}
