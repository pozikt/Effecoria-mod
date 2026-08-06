package com.effecoria.content;

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
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/** Φ-hydrolat bucket — places lakes; sneak+use drinks (Φ-poison). */
public final class PhiWaterBucketItem extends BucketItem {
    public PhiWaterBucketItem(Fluid fluid, Properties properties) {
        super(fluid, properties.craftRemainder(Items.BUCKET).stacksTo(1));
    }

    /**
     * Empty buckets are returned by drink/place, not by crafting — so gold filtration
     * can convert this stack into a purified bucket without duplicating the vessel.
     */
    @Override
    public ItemStack getCraftingRemainingItem(ItemStack itemStack) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (player.isShiftKeyDown() && hit.getType() == HitResult.Type.MISS) {
            return ItemUtils.startUsingInstantly(level, player, hand);
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
            player.awardStat(Stats.ITEM_USED.get(this));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
            player.hurt(player.damageSources().magic(), 2f);
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.8f, 1.1f);
            player.displayClientMessage(Component.translatable("message.effecoria.phi_water_poison"), true);
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
        tooltip.add(Component.translatable("item.effecoria.phi_water_bucket.hint"));
    }
}
