package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.effect.organic.PainInhibitorService;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class PainInhibitorEvents {
    private PainInhibitorEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || !PainInhibitorService.isActive(victim)) {
            return;
        }
        // Keep damage; remember velocity so knockback can be undone after the hit.
        PainInhibitorService.captureMotion(victim);
    }

    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || !PainInhibitorService.isActive(victim)) {
            return;
        }
        PainInhibitorService.suppressHitFeedback(victim);
    }

    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || !PainInhibitorService.isActive(victim)) {
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide()) {
            return;
        }
        if (!PainInhibitorService.isActive(living)) {
            return;
        }
        // Keep clearing stagger while the buff is up (hurtTime otherwise ticks down visually).
        if (living.hurtTime > 0 || living.hurtDuration > 0) {
            PainInhibitorService.clearStagger(living);
        }
    }
}
