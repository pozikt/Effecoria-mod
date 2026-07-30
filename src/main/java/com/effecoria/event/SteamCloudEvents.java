package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.effect.elemental.SteamCloudService;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/** Steam fog conceals players and blocks mob line of sight like a soft wall. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class SteamCloudEvents {
    private SteamCloudEvents() {}

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity newTarget = event.getNewAboutToBeSetTarget();
        if (newTarget == null) {
            return;
        }
        LivingEntity viewer = event.getEntity();
        if (!(viewer instanceof Mob)) {
            return;
        }
        if (SteamCloudService.obscuresVision(viewer, newTarget)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        LivingEntity subject = event.getEntity();
        if (subject.level().isClientSide()) {
            return;
        }
        if (!(subject.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        if (!SteamCloudService.isInsideCloud(level, subject.getEyePosition())
                && !SteamCloudService.isInsideCloud(level, subject.position().add(0, subject.getBbHeight() * 0.5, 0))) {
            return;
        }
        // Nearly invisible while inside steam — AI sensing treats fog as cover.
        event.modifyVisibility(0.0);
    }
}
