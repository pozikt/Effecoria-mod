package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.progression.HarpyFlightService;
import com.effecoria.core.progression.RaceService;
import com.effecoria.core.progression.RaceTraitsService;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class RaceEvents {
    private RaceEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && RaceService.hasRace(PsiHelper.get(player))) {
            RaceTraitsService.reapplyAttributes(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        HarpyFlightService.clear(player);
        if (event.isWasDeath() && RaceService.hasRace(PsiHelper.get(player))) {
            RaceTraitsService.reapplyAttributes(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HarpyFlightService.clear(player);
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RaceTraitsService.tick(player);
            HarpyFlightService.tick(player);
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PsiHelper.get(player).race().ifPresent(race -> {
            if (race == com.effecoria.core.progression.PlayerRace.HARPY) {
                event.setDistance(event.getDistance() * 0.5f);
            }
        });
        HarpyFlightService.onFallLanding(player, event);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getLevel().isClientSide()) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND && event.getHand() != InteractionHand.OFF_HAND) {
            return;
        }
        if (RaceTraitsService.tryDrinkBlood(player, event.getItemStack())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}
