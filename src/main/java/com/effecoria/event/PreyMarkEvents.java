package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.effect.corruption.PreyMarkService;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class PreyMarkEvents {
    private PreyMarkEvents() {}

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide()) {
            return;
        }
        if (!PreyMarkService.isMarked(living)) {
            return;
        }
        if (living.tickCount % PreyMarkService.RETARGET_INTERVAL != 0) {
            return;
        }
        PreyMarkService.forceHunt(living);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob hunter) || hunter.level().isClientSide()) {
            return;
        }
        LivingEntity forced = PreyMarkService.redirectTarget(hunter, event.getNewAboutToBeSetTarget());
        if (forced != null && forced != event.getNewAboutToBeSetTarget()) {
            event.setNewAboutToBeSetTarget(forced);
        }
    }
}
