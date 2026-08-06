package com.effecoria.event;

import com.effecoria.core.alchemy.AlchemyBuffEffect;
import com.effecoria.core.alchemy.AlchemyCrashKind;
import com.effecoria.core.alchemy.AlchemyPotionService;
import com.effecoria.content.ModMobEffects;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

@EventBusSubscriber(modid = com.effecoria.EffecoriaMod.MOD_ID)
public final class AlchemyEvents {
    private AlchemyEvents() {}

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        applyCrashFromBuff(event);
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        // Milk / commands also crash — intentional cost of aborting the buzz early
        applyCrashFromBuff(event);
    }

    private static void applyCrashFromBuff(MobEffectEvent event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null || !(instance.getEffect().value() instanceof AlchemyBuffEffect buff)) {
            return;
        }
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        // Don't stack crash if already crashing from another buff
        if (player.hasEffect(ModMobEffects.ALCHEMY_CRASH)) {
            return;
        }
        AlchemyCrashKind kind = buff.crashKind();
        AlchemyPotionService.applyCrash(player, kind);
        switch (kind) {
            case TONIC -> player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 40 * 20, 0));
            case RESONANCE -> player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 5 * 20, 0));
            case STIMULANT -> player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70 * 20, 0));
        }
        player.displayClientMessage(Component.translatable("message.effecoria.alchemy_crash"), true);
    }
}
