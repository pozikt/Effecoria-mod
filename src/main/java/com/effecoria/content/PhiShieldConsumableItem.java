package com.effecoria.content;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Consumable Φ-radiation mitigators (salve / lead pills / resistance draught). Works for non-mages. */
public final class PhiShieldConsumableItem extends Item {
    private final Supplier<Holder<MobEffect>> primary;
    private final int durationTicks;
    private final String hintKey;
    private final boolean leadPoison;

    public PhiShieldConsumableItem(
            Properties properties,
            Supplier<Holder<MobEffect>> primary,
            int durationTicks,
            String hintKey,
            boolean leadPoison) {
        super(properties.stacksTo(16));
        this.primary = primary;
        this.durationTicks = durationTicks;
        this.hintKey = hintKey;
        this.leadPoison = leadPoison;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
            player.awardStat(Stats.ITEM_USED.get(this));
            player.addEffect(new MobEffectInstance(primary.get(), durationTicks, 0, false, true, true));
            if (leadPoison) {
                player.addEffect(new MobEffectInstance(MobEffects.POISON, Math.min(200, durationTicks / 4), 0));
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, durationTicks / 2, 0));
                player.displayClientMessage(Component.translatable("message.effecoria.lead_pill_risk"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.effecoria.phi_shield_applied"), true);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.6f, 1.1f);
        }
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return leadPoison ? 28 : 24;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(hintKey));
    }
}
