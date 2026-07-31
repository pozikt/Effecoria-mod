package com.effecoria.effect.corruption;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import com.effecoria.EffecoriaMod;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class CorruptionCurseEvents {
    private CorruptionCurseEvents() {}

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide()) {
            return;
        }
        CorruptionCurseService.tickEntity(living);
    }

    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof PathfinderMob pathMob && CorruptionCurseService.hasCurse(pathMob)) {
            // Allow re-adding goal after reload (GOAL_TAG may already be set from prior session).
            pathMob.getPersistentData().remove(CorruptionCurseService.GOAL_TAG);
            CorruptionCurseService.ensureSeekGoal(pathMob);
            if (pathMob instanceof Mob mob) {
                CompoundPickup.enable(mob);
            }
        }
    }

    /** Restores pickup flag after world reload if we had enabled it for the curse. */
    private static final class CompoundPickup {
        static void enable(Mob mob) {
            var tag = mob.getPersistentData().getCompound(CorruptionCurseService.CURSE_TAG);
            if (tag.getBoolean("EnabledPickup") && !mob.canPickUpLoot()) {
                mob.setCanPickUpLoot(true);
            }
        }
    }
}
