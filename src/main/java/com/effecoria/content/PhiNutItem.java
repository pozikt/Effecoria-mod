package com.effecoria.content;

import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;

import java.util.List;

/** Concentrated Φ-nut — restores Ψ for mages, poisons the uninitiated. */
public final class PhiNutItem extends Item {
    public static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(2)
            .saturationModifier(0.3f)
            .alwaysEdible()
            .build();

    public PhiNutItem(Properties properties) {
        super(properties.food(FOOD));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return result;
        }
        PlayerPsiData data = PsiHelper.get(player);
        if (data.initiated()) {
            float gain = Math.max(8f, data.maxPsi() * 0.18f);
            data.setCurrentPsi(data.currentPsi() + gain);
            PsiHelper.set(player, data);
            player.syncData(com.effecoria.core.psi.ModAttachments.PSI.get());
            level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.4f);
            player.displayClientMessage(Component.translatable("message.effecoria.phi_nut_charged"), true);
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 0));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
            player.hurt(player.damageSources().magic(), 3f);
            player.displayClientMessage(Component.translatable("message.effecoria.phi_nut_poison"), true);
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.effecoria.phi_nut.hint"));
    }
}
