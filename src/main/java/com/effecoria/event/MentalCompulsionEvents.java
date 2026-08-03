package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.effect.mental.MentalCompulsionService;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

/** Keep mental compulsions from being overridden by vanilla retargeting. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class MentalCompulsionEvents {
    private MentalCompulsionEvents() {}

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !MentalCompulsionService.hasActive(mob)) {
            return;
        }
        MentalCompulsionService.Type type = MentalCompulsionService.typeOf(mob);
        LivingEntity next = event.getNewAboutToBeSetTarget();
        switch (type) {
            case TERROR, CLIFF, DROWN, WHISPER, DOMINATE -> {
                if (next != null) {
                    event.setCanceled(true);
                }
            }
            case DEPRESS -> {
                if (next != null) {
                    event.setCanceled(true);
                }
            }
            case FRENZY -> {
                // Frenzy may retarget freely.
            }
        }
    }
}
