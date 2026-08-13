package com.effecoria;



import org.slf4j.Logger;



import com.effecoria.config.BalanceConfig;

import com.effecoria.content.ModBlockEntities;

import com.effecoria.content.ModBlocks;

import com.effecoria.content.ModCreativeTabs;

import com.effecoria.content.ModEntities;

import com.effecoria.content.ModFeatures;

import com.effecoria.content.ModFluids;

import com.effecoria.content.ModItems;

import com.effecoria.content.ModMaterials;

import com.effecoria.content.ModMenus;

import com.effecoria.content.ModMobEffects;

import com.effecoria.content.ModParticleTypes;

import com.effecoria.content.ModPlacementModifiers;

import com.effecoria.core.psi.ModAttachments;

import com.effecoria.network.ModNetworking;

import com.effecoria.world.CrystalForestRegion;
import com.effecoria.world.DeadWastelandRegion;
import com.effecoria.world.EmeraldCanopyRegion;
import com.effecoria.world.OmegaScarRegion;
import com.effecoria.world.EssencePlateauRegion;
import com.effecoria.world.VitrifiedWastesRegion;
import com.effecoria.world.EssencePlateauSurfaceRules;

import com.mojang.logging.LogUtils;



import net.minecraft.resources.ResourceLocation;

import net.neoforged.bus.api.IEventBus;

import net.neoforged.fml.ModContainer;

import net.neoforged.fml.common.Mod;

import net.neoforged.fml.config.ModConfig;

import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;



@Mod(EffecoriaMod.MOD_ID)

public class EffecoriaMod {

    public static final String MOD_ID = "effecoria";

    public static final Logger LOGGER = LogUtils.getLogger();



    public EffecoriaMod(IEventBus modEventBus, ModContainer modContainer) {

        ModAttachments.register(modEventBus);

        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);

        ModFeatures.FEATURES.register(modEventBus);
        ModPlacementModifiers.PLACEMENT_MODIFIERS.register(modEventBus);

        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);

        ModMenus.MENUS.register(modEventBus);

        ModMaterials.ARMOR_MATERIALS.register(modEventBus);
        ModMobEffects.MOB_EFFECTS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);

        ModEntities.ENTITY_TYPES.register(modEventBus);

        ModParticleTypes.PARTICLE_TYPES.register(modEventBus);

        com.effecoria.content.ModSounds.SOUND_EVENTS.register(modEventBus);

        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);

        com.effecoria.world.ModGameRules.bootstrap();

        modContainer.registerConfig(ModConfig.Type.COMMON, BalanceConfig.SPEC);

        modEventBus.addListener(EffecoriaMod::registerPayloads);
        modEventBus.addListener(ModEntities::registerAttributes);
        modEventBus.addListener(ModEntities::registerSpawnPlacements);
        modEventBus.addListener(EffecoriaMod::commonSetup);



        LOGGER.info("Effecoria {} loaded — phase 2 magic core", modContainer.getModInfo().getVersion());

    }



    public static ResourceLocation id(String path) {

        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);

    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info(
                    "Registering Essence Plateau / Dead Wasteland / Vitrified Wastes / Crystal Forest / Emerald Canopy / Ω-Scar TerraBlender regions");
            Regions.register(new EssencePlateauRegion(id("overworld"), 6));
            Regions.register(new DeadWastelandRegion(id("dead_wasteland"), 8));
            Regions.register(new VitrifiedWastesRegion(id("vitrified_wastes"), 8));
            Regions.register(new CrystalForestRegion(id("crystal_forest"), 6));
            // Higher weight than Crystal Forest — canopy needs wide contiguous jungle climates.
            Regions.register(new EmeraldCanopyRegion(id("emerald_canopy"), 12));
            Regions.register(new OmegaScarRegion(id("omega_scar"), 4));
            SurfaceRuleManager.addSurfaceRules(
                    SurfaceRuleManager.RuleCategory.OVERWORLD,
                    MOD_ID,
                    EssencePlateauSurfaceRules.makeRules());
        });
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {

        PayloadRegistrar registrar = event.registrar(MOD_ID);

        registrar.playToServer(
                ModNetworking.InitiateSchoolPayload.TYPE,
                ModNetworking.InitiateSchoolPayload.STREAM_CODEC,
                ModNetworking.InitiateSchoolPayload::handle);
        registrar.playToServer(
                ModNetworking.SelectRacePayload.TYPE,
                ModNetworking.SelectRacePayload.STREAM_CODEC,
                ModNetworking.SelectRacePayload::handle);
        registrar.playToClient(
                ModNetworking.OpenSchoolSelectPayload.TYPE,
                ModNetworking.OpenSchoolSelectPayload.STREAM_CODEC,
                ModNetworking.OpenSchoolSelectPayload::handle);
        registrar.playToServer(
                ModNetworking.HarpyFlapPayload.TYPE,
                ModNetworking.HarpyFlapPayload.STREAM_CODEC,
                ModNetworking.HarpyFlapPayload::handle);
        registrar.playToServer(
                ModNetworking.VaranagiClimbDashPayload.TYPE,
                ModNetworking.VaranagiClimbDashPayload.STREAM_CODEC,
                ModNetworking.VaranagiClimbDashPayload::handle);
        registrar.playToServer(
                ModNetworking.VaranagiClimbJumpPayload.TYPE,
                ModNetworking.VaranagiClimbJumpPayload.STREAM_CODEC,
                ModNetworking.VaranagiClimbJumpPayload::handle);
        registrar.playToServer(
                ModNetworking.DeferSchoolPayload.TYPE,
                ModNetworking.DeferSchoolPayload.STREAM_CODEC,
                ModNetworking.DeferSchoolPayload::handle);
        registrar.playToServer(
                ModNetworking.MatterLinkPayload.TYPE,
                ModNetworking.MatterLinkPayload.STREAM_CODEC,
                ModNetworking.MatterLinkPayload::handle);
        registrar.playToServer(
                ModNetworking.MatterChannelPayload.TYPE,
                ModNetworking.MatterChannelPayload.STREAM_CODEC,
                ModNetworking.MatterChannelPayload::handle);
        registrar.playToServer(
                ModNetworking.MatterThrowPayload.TYPE,
                ModNetworking.MatterThrowPayload.STREAM_CODEC,
                ModNetworking.MatterThrowPayload::handle);
        registrar.playToServer(
                ModNetworking.ArmorAbilityPayload.TYPE,
                ModNetworking.ArmorAbilityPayload.STREAM_CODEC,
                ModNetworking.ArmorAbilityPayload::handle);
        registrar.playToServer(
                ModNetworking.ArmorAbilityCyclePayload.TYPE,
                ModNetworking.ArmorAbilityCyclePayload.STREAM_CODEC,
                ModNetworking.ArmorAbilityCyclePayload::handle);
        registrar.playToClient(
                ModNetworking.MatterBondSyncPayload.TYPE,
                ModNetworking.MatterBondSyncPayload.STREAM_CODEC,
                ModNetworking.MatterBondSyncPayload::handle);
        registrar.playToClient(
                ModNetworking.TelegraphPulsePayload.TYPE,
                ModNetworking.TelegraphPulsePayload.STREAM_CODEC,
                ModNetworking.TelegraphPulsePayload::handle);
        registrar.playToServer(
                ModNetworking.SelectSpellPayload.TYPE,
                ModNetworking.SelectSpellPayload.STREAM_CODEC,
                ModNetworking.SelectSpellPayload::handle);
        registrar.playToServer(

                ModNetworking.CastSpellPayload.TYPE,

                ModNetworking.CastSpellPayload.STREAM_CODEC,

                ModNetworking.CastSpellPayload::handle);

        registrar.playToServer(
                ModNetworking.CycleSpellPayload.TYPE,
                ModNetworking.CycleSpellPayload.STREAM_CODEC,
                ModNetworking.CycleSpellPayload::handle);
        registrar.playToClient(
                ModNetworking.SteamCloudsPayload.TYPE,
                ModNetworking.SteamCloudsPayload.STREAM_CODEC,
                ModNetworking.SteamCloudsPayload::handle);
        registrar.playToClient(
                ModNetworking.BlurredLocusPayload.TYPE,
                ModNetworking.BlurredLocusPayload.STREAM_CODEC,
                ModNetworking.BlurredLocusPayload::handle);
        registrar.playToClient(
                ModNetworking.MirageStartPayload.TYPE,
                ModNetworking.MirageStartPayload.STREAM_CODEC,
                ModNetworking.MirageStartPayload::handle);
        registrar.playToClient(
                ModNetworking.MirageHurtPayload.TYPE,
                ModNetworking.MirageHurtPayload.STREAM_CODEC,
                ModNetworking.MirageHurtPayload::handle);
        registrar.playToClient(
                ModNetworking.MirageEndPayload.TYPE,
                ModNetworking.MirageEndPayload.STREAM_CODEC,
                ModNetworking.MirageEndPayload::handle);
        registrar.playToClient(
                ModNetworking.SingularityFxPayload.TYPE,
                ModNetworking.SingularityFxPayload.STREAM_CODEC,
                ModNetworking.SingularityFxPayload::handle);
        registrar.playToClient(
                ModNetworking.QuasarFxPayload.TYPE,
                ModNetworking.QuasarFxPayload.STREAM_CODEC,
                ModNetworking.QuasarFxPayload::handle);
        registrar.playToClient(
                ModNetworking.SpatialCutFxPayload.TYPE,
                ModNetworking.SpatialCutFxPayload.STREAM_CODEC,
                ModNetworking.SpatialCutFxPayload::handle);
        registrar.playToClient(
                ModNetworking.LightningArcFxPayload.TYPE,
                ModNetworking.LightningArcFxPayload.STREAM_CODEC,
                ModNetworking.LightningArcFxPayload::handle);
        registrar.playToClient(
                ModNetworking.SpatialRippleFxPayload.TYPE,
                ModNetworking.SpatialRippleFxPayload.STREAM_CODEC,
                ModNetworking.SpatialRippleFxPayload::handle);
        registrar.playToClient(
                ModNetworking.SpatialSensePayload.TYPE,
                ModNetworking.SpatialSensePayload.STREAM_CODEC,
                ModNetworking.SpatialSensePayload::handle);
        registrar.playToClient(
                ModNetworking.SpatialWarpFxPayload.TYPE,
                ModNetworking.SpatialWarpFxPayload.STREAM_CODEC,
                ModNetworking.SpatialWarpFxPayload::handle);
        registrar.playToClient(
                ModNetworking.SpatialWaveFxPayload.TYPE,
                ModNetworking.SpatialWaveFxPayload.STREAM_CODEC,
                ModNetworking.SpatialWaveFxPayload::handle);
        registrar.playToClient(
                ModNetworking.SpellCatalogPayload.TYPE,
                ModNetworking.SpellCatalogPayload.STREAM_CODEC,
                ModNetworking.SpellCatalogPayload::handle);
        registrar.playToClient(
                ModNetworking.SealWordCatalogPayload.TYPE,
                ModNetworking.SealWordCatalogPayload.STREAM_CODEC,
                ModNetworking.SealWordCatalogPayload::handle);
        registrar.playToServer(
                ModNetworking.BreathTrainHitPayload.TYPE,
                ModNetworking.BreathTrainHitPayload.STREAM_CODEC,
                ModNetworking.BreathTrainHitPayload::handle);
        registrar.playToServer(
                ModNetworking.BreathTrainMissPayload.TYPE,
                ModNetworking.BreathTrainMissPayload.STREAM_CODEC,
                ModNetworking.BreathTrainMissPayload::handle);
        registrar.playToServer(
                ModNetworking.ApplySealProgramPayload.TYPE,
                ModNetworking.ApplySealProgramPayload.STREAM_CODEC,
                ModNetworking.ApplySealProgramPayload::handle);
        registrar.playToServer(
                ModNetworking.ClearSealProgramPayload.TYPE,
                ModNetworking.ClearSealProgramPayload.STREAM_CODEC,
                ModNetworking.ClearSealProgramPayload::handle);
        registrar.playToServer(
                ModNetworking.SaveSealExpressionPayload.TYPE,
                ModNetworking.SaveSealExpressionPayload.STREAM_CODEC,
                ModNetworking.SaveSealExpressionPayload::handle);
        registrar.playToServer(
                ModNetworking.HubOpenedPayload.TYPE,
                ModNetworking.HubOpenedPayload.STREAM_CODEC,
                ModNetworking.HubOpenedPayload::handle);
        registrar.playToServer(
                ModNetworking.MarkPrimerChapterSeenPayload.TYPE,
                ModNetworking.MarkPrimerChapterSeenPayload.STREAM_CODEC,
                ModNetworking.MarkPrimerChapterSeenPayload::handle);
        registrar.playToClient(
                ModNetworking.OpenGeneEditorPayload.TYPE,
                ModNetworking.OpenGeneEditorPayload.STREAM_CODEC,
                ModNetworking.OpenGeneEditorPayload::handle);
        registrar.playToServer(
                ModNetworking.ApplyGeneModsPayload.TYPE,
                ModNetworking.ApplyGeneModsPayload.STREAM_CODEC,
                ModNetworking.ApplyGeneModsPayload::handle);
        registrar.playToServer(
                ModNetworking.ClearGeneModsPayload.TYPE,
                ModNetworking.ClearGeneModsPayload.STREAM_CODEC,
                ModNetworking.ClearGeneModsPayload::handle);
        registrar.playToClient(
                ModNetworking.PhiWeatherSyncPayload.TYPE,
                ModNetworking.PhiWeatherSyncPayload.STREAM_CODEC,
                ModNetworking.PhiWeatherSyncPayload::handle);
        registrar.playToServer(
                ModNetworking.PhiBeaconRenamePayload.TYPE,
                ModNetworking.PhiBeaconRenamePayload.STREAM_CODEC,
                ModNetworking.PhiBeaconRenamePayload::handle);
        registrar.playToServer(
                ModNetworking.PortalModulatorConfigPayload.TYPE,
                ModNetworking.PortalModulatorConfigPayload.STREAM_CODEC,
                ModNetworking.PortalModulatorConfigPayload::handle);
        registrar.playToClient(
                ModNetworking.EssenceGlueSyncPayload.TYPE,
                ModNetworking.EssenceGlueSyncPayload.STREAM_CODEC,
                ModNetworking.EssenceGlueSyncPayload::handle);
        registrar.playToClient(
                ModNetworking.TowerDomeSyncPayload.TYPE,
                ModNetworking.TowerDomeSyncPayload.STREAM_CODEC,
                ModNetworking.TowerDomeSyncPayload::handle);
        registrar.playToServer(
                ModNetworking.PhiSonarRequestPayload.TYPE,
                ModNetworking.PhiSonarRequestPayload.STREAM_CODEC,
                ModNetworking.PhiSonarRequestPayload::handle);
        registrar.playToClient(
                ModNetworking.PhiSonarMapPayload.TYPE,
                ModNetworking.PhiSonarMapPayload.STREAM_CODEC,
                ModNetworking.PhiSonarMapPayload::handle);
        registrar.playToServer(
                ModNetworking.TowerRemoteCommandPayload.TYPE,
                ModNetworking.TowerRemoteCommandPayload.STREAM_CODEC,
                ModNetworking.TowerRemoteCommandPayload::handle);
    }

}


