package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.magic.MobMagicService;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/** Natural mage rolls on spawn + re-attach cast goals after chunk reload. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class MobMagicEvents {
    private MobMagicEvents() {}

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof Mob mob) {
            MobMagicService.tryNaturalInitiate(mob);
        }
    }

    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof PathfinderMob pathMob && MobMagicService.isInitiated(pathMob)) {
            pathMob.getPersistentData().remove(MobMagicService.GOAL_TAG);
            MobMagicService.ensureCastGoal(pathMob);
        }
    }
}
