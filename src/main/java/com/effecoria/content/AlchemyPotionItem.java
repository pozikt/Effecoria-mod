package com.effecoria.content;

import java.util.List;
import java.util.function.Supplier;

import com.effecoria.core.psi.PsiHelper;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
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

/** Drinkable Φ-alchemy potion — returns empty flask; crash applied when buff expires. */
public final class AlchemyPotionItem extends Item {
    private final Supplier<Holder<MobEffect>> buffEffect;
    private final int durationTicks;
    private final String hintKey;

    public AlchemyPotionItem(
            Properties properties,
            Supplier<Holder<MobEffect>> buffEffect,
            int durationTicks,
            String hintKey) {
        super(properties.stacksTo(16));
        this.buffEffect = buffEffect;
        this.durationTicks = durationTicks;
        this.hintKey = hintKey;
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
            if (PsiHelper.get(player).initiated()) {
                int duration = scaledDuration(stack);
                player.addEffect(new MobEffectInstance(buffEffect.get(), duration, 0, false, true, true));
                level.playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.GENERIC_DRINK,
                        SoundSource.PLAYERS,
                        0.7f,
                        1.2f);
                player.displayClientMessage(Component.translatable("message.effecoria.alchemy_potion_ok"), true);
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
                player.displayClientMessage(Component.translatable("message.effecoria.alchemy_potion_uneasy"), true);
            }
        }
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
            ItemStack flask = new ItemStack(ModItems.PHI_FLASK.get());
            if (stack.isEmpty()) {
                return flask;
            }
            if (!player.getInventory().add(flask)) {
                player.drop(flask, false);
            }
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
        tooltip.add(Component.translatable(hintKey));
        tooltip.add(Component.translatable("item.effecoria.alchemy_potion.crash_hint"));
        float factor = durationFactor(stack);
        if (Math.abs(factor - 1f) > 0.01f) {
            tooltip.add(Component.translatable("item.effecoria.alchemy_potion.heat_factor", Math.round(factor * 100f)));
        }
    }

    private int scaledDuration(ItemStack stack) {
        return Math.max(1, Math.round(durationTicks * durationFactor(stack)));
    }

    private static float durationFactor(ItemStack stack) {
        var custom = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return 1f;
        }
        CompoundTag tag = custom.copyTag();
        if (tag.contains(com.effecoria.block.EssenceAlembicBlockEntity.NBT_DURATION_FACTOR)) {
            return tag.getFloat(com.effecoria.block.EssenceAlembicBlockEntity.NBT_DURATION_FACTOR);
        }
        return 1f;
    }
}
