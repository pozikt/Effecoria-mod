package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.command.EffecoriaCommands;
import com.effecoria.content.ModItems;
import com.effecoria.content.PhiHarnessItems;
import com.effecoria.core.formula.CastBlockReason;
import com.effecoria.core.formula.DelayedEffectService;
import com.effecoria.core.formula.FormulaEngine;
import com.effecoria.core.formula.PhiSample;
import com.effecoria.core.magic.ShadeService;
import com.effecoria.core.progression.EntropyService;
import com.effecoria.core.progression.ExhaustionService;
import com.effecoria.core.progression.FirstHourTips;
import com.effecoria.core.progression.ProgressionService;
import com.effecoria.core.alchemy.AlchemyPotionService;
import com.effecoria.core.progression.SpellUnlockService;
import com.effecoria.core.progression.SealWordUnlockService;
import com.effecoria.core.phi.CreativeGodMode;
import com.effecoria.core.phi.PhiFieldService;
import com.effecoria.core.phi.PhiHarness;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.core.psi.SpellProgression;
import com.effecoria.effect.common.CommonWardService;
import com.effecoria.effect.elemental.AirHandService;
import com.effecoria.effect.elemental.ElementalBlockService;
import com.effecoria.effect.elemental.ElementalCageService;
import com.effecoria.effect.elemental.ElementalFieldService;
import com.effecoria.effect.elemental.ElementalShroudService;
import com.effecoria.effect.elemental.MatterBondService;
import com.effecoria.effect.elemental.SteamCloudService;
import com.effecoria.effect.elemental.SteamFlightService;
import com.effecoria.effect.mental.MentalCompulsionService;
import com.effecoria.effect.mental.MentalServitudeService;
import com.effecoria.effect.mental.MentalityService;
import com.effecoria.effect.mental.MirageWorldService;
import com.effecoria.effect.necromancy.DeathMarkService;
import com.effecoria.effect.necromancy.NecroFieldService;
import com.effecoria.effect.necromancy.NecroSummonService;
import com.effecoria.effect.organic.OrganicDiagnosticService;
import com.effecoria.effect.organic.OrganicFieldService;
import com.effecoria.effect.organic.OrganicSpikeWaveService;
import com.effecoria.effect.organic.ParasiteHostService;
import com.effecoria.effect.corruption.CorruptionFieldService;
import com.effecoria.armor.EssoniteArmorService;
import com.effecoria.effect.spatial.SpatialFieldService;
import com.effecoria.effect.spatial.SubspaceEssentializationService;
import com.effecoria.effect.spatial.SubspacePhysicsService;
import com.effecoria.magic.SpellRegistry;
import com.effecoria.core.seal.SealWordRegistry;
import com.effecoria.world.CrystalForestService;
import com.effecoria.world.DeadWastelandService;
import com.effecoria.world.EmeraldCanopyService;
import com.effecoria.world.EssencePlateauService;
import com.effecoria.world.OmegaScarService;
import com.effecoria.world.PhiFogService;
import com.effecoria.world.VitrifiedWastesService;
import com.effecoria.world.WhisperingSpireService;

import net.minecraft.server.MinecraftServer;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
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
        event.addListener(com.effecoria.core.seal.SealWordRegistry.RELOAD_LISTENER);
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
            DelayedEffectService.tick(serverLevel);
            ElementalBlockService.tick(serverLevel);
            SteamCloudService.tick(serverLevel);
            ElementalFieldService.tick(serverLevel);
            OrganicFieldService.tick(serverLevel);
            OrganicSpikeWaveService.tick(serverLevel);
            ParasiteHostService.tick(serverLevel);
            NecroFieldService.tick(serverLevel);
            SpatialFieldService.tick(serverLevel);
            CorruptionFieldService.tick(serverLevel);
            ElementalCageService.tick(serverLevel);
            DeathMarkService.tickMarks(serverLevel);
            MentalCompulsionService.tick(serverLevel);
            MentalServitudeService.tick(serverLevel);
            MentalityService.tick(serverLevel);
            MirageWorldService.tick(serverLevel);
            SubspaceEssentializationService.tick(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player != null) {
            SpellRegistry.syncTo(player);
            SealWordRegistry.syncTo(player);
            return;
        }
        SpellRegistry.syncToAll(event.getPlayerList().getServer());
        SealWordRegistry.syncToAll(event.getPlayerList().getServer());
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) {
            return;
        }
        var crafted = event.getCrafting();
        if (crafted.is(ModItems.PHI_CELL.get()) && PhiHarnessItems.cellCharge(crafted) <= 0.001f) {
            // Fresh cell ships with a starter buffer so the first cave cast can demonstrate assist.
            PhiHarnessItems.setCellCharge(crafted, 0.35f);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SteamCloudService.syncToPlayer(player, serverLevel);
            SpellRegistry.syncTo(player);
            SealWordRegistry.syncTo(player);
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

        // Hyperspace physics runs for everyone (mages and mundanes) before school ticks.
        SubspacePhysicsService.tickPlayer(player);

        EssencePlateauService.tickPlayer(player);
        DeadWastelandService.tickPlayer(player);
        VitrifiedWastesService.tickPlayer(player);
        CrystalForestService.tickPlayer(player);
        EmeraldCanopyService.tickPlayer(player);
        OmegaScarService.tickPlayer(player);
        WhisperingSpireService.tickPlayer(player);

        MirageWorldService.playerTick(player);

        PlayerPsiData flightData = PsiHelper.get(player);
        if (flightData.initiated()) {
            flightData.tickIonCharge();
            if (flightData.steamFlightActive()) {
                SteamFlightService.tick(player);
            }
            AirHandService.tick(player);
            ElementalShroudService.tick(player);
            NecroSummonService.tick(player);
            MatterBondService.tickPlayer(player);
            EssoniteArmorService.tickPlayer(player);
            PsiHelper.set(player, flightData);
        }

        if (player.tickCount % 10 != 0) {
            return;
        }

        PlayerPsiData data = PsiHelper.get(player);
        if (!data.initiated()) {
            return;
        }

        CommonWardService.tickPlayer(player);

        ProgressionService.tick(player, data);
        ShadeService.tick(player);
        SpellUnlockService.tick(player, data);
        SealWordUnlockService.tick(player, data);

        if (CreativeGodMode.isActive(player)) {
            data.setCurrentPsi(data.maxPsi());
            data.setEntropyB(0f);
            data.setExhaustion(0f);
            PsiHelper.set(player, data);
            player.syncData(ModAttachments.PSI.get());
            return;
        }

        ExhaustionService.tick(player, data);
        EntropyService.tick(player, data);

        long gameTime = player.serverLevel().getGameTime();
        if (data.tickLichAscension(gameTime)) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.effecoria.necro.lich_ended"), true);
        }

        PhiSample phi = PhiFieldService.sample(player.level(), player.position(), player);
        float alchemyPhi = AlchemyPotionService.phiMultiplier(player);
        if (alchemyPhi != 1f && !phi.zeroFlux() && !phi.isInfinite()) {
            phi = new PhiSample(phi.value() * alchemyPhi, false, phi.solarDay());
        }
        PhiHarness.tickRecharge(player, phi);
        float regen;
        if (data.isLichAscensionActive(gameTime)) {
            regen = FormulaEngine.regenPsiLich(
                    PsiHelper.toContext(player, data), phi, data.phylacteryEfficiency(), 10f);
        } else {
            regen = FormulaEngine.regenPsi(PsiHelper.toContext(player, data), phi, 10f);
            regen *= EssencePlateauService.regenMultiplier(player.level(), player.position());
            regen *= PhiFogService.regenMultiplier(player);
            regen *= AlchemyPotionService.regenMultiplier(player);
            regen *= com.effecoria.core.progression.RaceTraitsService.regenMultiplier(player);
            regen += com.effecoria.world.weather.PhiWeatherService.psiRegenBonus(player);
        }
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
        data.clearLichAscension();
        ElementalFieldService.clearFor(player.getUUID());
        MatterBondService.clear(player.getUUID());
        OrganicFieldService.clearFor(player.getUUID());
        NecroFieldService.clearFor(player.getUUID());
        SpatialFieldService.clearFor(player.getUUID());
        CorruptionFieldService.clearFor(player.getUUID());
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
