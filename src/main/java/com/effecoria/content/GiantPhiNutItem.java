package com.effecoria.content;

import java.util.List;

import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Giant canopy nut — restores full Ψ for initiated mages. */
public final class GiantPhiNutItem extends Item {
    public static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(6)
            .saturationModifier(0.8f)
            .alwaysEdible()
            .build();

    public GiantPhiNutItem(Properties properties) {
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
            data.setCurrentPsi(data.maxPsi());
            PsiHelper.set(player, data);
            player.syncData(ModAttachments.PSI.get());
            level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 0.9f);
            player.displayClientMessage(Component.translatable("message.effecoria.giant_phi_nut_charged"), true);
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 240, 1));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 1));
            player.hurt(player.damageSources().magic(), 6f);
            player.displayClientMessage(Component.translatable("message.effecoria.giant_phi_nut_poison"), true);
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.effecoria.giant_phi_nut.hint"));
    }
}
