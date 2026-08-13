package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.tower.TowerGhostService;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class TowerGhostEvents {
    private TowerGhostEvents() {}

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer sp && TowerGhostService.isGhost(sp)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && TowerGhostService.isGhost(sp)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && TowerGhostService.isGhost(sp)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onOutgoingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer sp && TowerGhostService.isGhost(sp)) {
            event.setCanceled(true);
        }
    }
}
