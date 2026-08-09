package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.progression.HarpyClawService;
import com.effecoria.core.progression.HarpyFlightService;
import com.effecoria.core.progression.RaceService;
import com.effecoria.core.progression.RaceTraitsService;
import com.effecoria.core.progression.VaranagiClimbService;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
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
        HarpyClawService.clear(player);
        VaranagiClimbService.clear(player);
        if (event.isWasDeath() && RaceService.hasRace(PsiHelper.get(player))) {
            RaceTraitsService.reapplyAttributes(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HarpyFlightService.clear(player);
            HarpyClawService.clear(player);
            VaranagiClimbService.clear(player);
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RaceTraitsService.tick(player);
            HarpyFlightService.tick(player);
            HarpyClawService.tickDive(player);
            VaranagiClimbService.tick(player);
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
        VaranagiClimbService.onFall(player, event);
    }

    /** Melee swings: add iron-spear-style speed charge on top of claw base attribute. */
    @SubscribeEvent
    public static void onOutgoingMelee(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        if (!HarpyFlightService.isHarpy(attacker) || !event.getSource().is(DamageTypes.PLAYER_ATTACK)) {
            return;
        }
        // Dive collision already baked speed into the hit amount.
        if (HarpyClawService.isDiveHit()) {
            return;
        }
        LivingEntity victim = event.getEntity();
        float bonus = HarpyClawService.speedBonusDamage(attacker, victim);
        if (bonus > 0.05f) {
            event.setAmount(event.getAmount() + bonus);
        }
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
