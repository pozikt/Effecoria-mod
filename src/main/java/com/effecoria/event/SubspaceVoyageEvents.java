package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.effect.spatial.SubspaceVoyageService;
import com.effecoria.world.ModDimensions;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class SubspaceVoyageEvents {
    private SubspaceVoyageEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!ModDimensions.isSubspace(player.level())) {
            return;
        }
        // Soft fog of war — darkness without inventory icon spam every tick refresh.
        if (player.tickCount % 40 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, false, false));
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SubspaceVoyageService.handleSubspaceDeath(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SubspaceVoyageService.onRespawn(player);
        }
    }
}
