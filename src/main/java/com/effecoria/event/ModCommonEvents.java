package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.command.EffecoriaCommands;
import com.effecoria.core.formula.FormulaEngine;
import com.effecoria.core.magic.ShadeService;
import com.effecoria.core.progression.ExhaustionService;
import com.effecoria.core.progression.ProgressionService;
import com.effecoria.core.phi.CreativeGodMode;
import com.effecoria.core.phi.PhiFieldService;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.core.psi.SpellProgression;
import com.effecoria.effect.elemental.AirHandService;
import com.effecoria.effect.elemental.ElementalBlockService;
import com.effecoria.effect.elemental.ElementalCageService;
import com.effecoria.effect.elemental.ElementalFieldService;
import com.effecoria.effect.elemental.ElementalShroudService;
import com.effecoria.effect.elemental.SteamCloudService;
import com.effecoria.effect.elemental.SteamFlightService;
import com.effecoria.effect.organic.OrganicDiagnosticService;
import com.effecoria.effect.organic.OrganicFieldService;
import com.effecoria.magic.SpellRegistry;

import net.minecraft.server.MinecraftServer;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class ModCommonEvents {
    private ModCommonEvents() {}

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(SpellRegistry.RELOAD_LISTENER);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        EffecoriaCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        OrganicDiagnosticService.tick(server);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            ElementalBlockService.tick(serverLevel);
            SteamCloudService.tick(serverLevel);
            ElementalFieldService.tick(serverLevel);
            OrganicFieldService.tick(serverLevel);
            ElementalCageService.tick(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SteamCloudService.syncToPlayer(player, serverLevel);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SteamCloudService.syncToPlayer(player, serverLevel);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PlayerPsiData flightData = PsiHelper.get(player);
        if (flightData.initiated()) {
            flightData.tickIonCharge();
            if (flightData.steamFlightActive()) {
                SteamFlightService.tick(player);
            }
            AirHandService.tick(player);
            ElementalShroudService.tick(player);
            PsiHelper.set(player, flightData);
        }

        if (player.tickCount % 10 != 0) {
            return;
        }

        PlayerPsiData data = PsiHelper.get(player);
        if (!data.initiated()) {
            return;
        }

        ProgressionService.tick(player, data);
        ShadeService.tick(player);
        data.mergeMissingSpells(SpellProgression.spellsForSchool(data.school()));

        if (CreativeGodMode.isActive(player)) {
            data.setCurrentPsi(data.maxPsi());
            data.setEntropyB(0f);
            data.setExhaustion(0f);
            PsiHelper.set(player, data);
            player.syncData(ModAttachments.PSI.get());
            return;
        }

        ExhaustionService.tick(player, data);

        float regen = FormulaEngine.regenPsi(
                PsiHelper.toContext(player, data),
                PhiFieldService.sample(player.level(), player.position(), player),
                10f);
        if (regen > 0f) {
            data.setCurrentPsi(data.currentPsi() + regen);
        }

        PsiHelper.set(player, data);
        player.syncData(ModAttachments.PSI.get());
    }

    /**
     * Must run after NeoForge copies {@code copyOnDeath} attachments, otherwise exhaustion
     * from the dead player is written back over a premature clear.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        clearOvercastPenalties(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        clearOvercastPenalties(player);
    }

    private static void clearOvercastPenalties(ServerPlayer player) {
        PlayerPsiData data = PsiHelper.get(player);
        ExhaustionService.clearOnDeath(data);
        data.clearIonCharge();
        ElementalFieldService.clearFor(player.getUUID());
        OrganicFieldService.clearFor(player.getUUID());
        ElementalShroudService.clearFor(player.getUUID());
        AirHandService.clearFor(player);
        PsiHelper.set(player, data);
        ExhaustionService.stripExhaustionEffects(player);
        player.syncData(ModAttachments.PSI.get());
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        SteamFlightService.onFall(event);
    }
}
