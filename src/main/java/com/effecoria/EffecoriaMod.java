package com.effecoria;



import org.slf4j.Logger;



import com.effecoria.config.BalanceConfig;

import com.effecoria.content.ModBlockEntities;

import com.effecoria.content.ModBlocks;

import com.effecoria.content.ModCreativeTabs;

import com.effecoria.content.ModEntities;

import com.effecoria.content.ModItems;

import com.effecoria.content.ModParticleTypes;

import com.effecoria.core.psi.ModAttachments;

import com.effecoria.network.ModNetworking;

import com.mojang.logging.LogUtils;



import net.minecraft.resources.ResourceLocation;

import net.neoforged.bus.api.IEventBus;

import net.neoforged.fml.ModContainer;

import net.neoforged.fml.common.Mod;

import net.neoforged.fml.config.ModConfig;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;



@Mod(EffecoriaMod.MOD_ID)

public class EffecoriaMod {

    public static final String MOD_ID = "effecoria";

    public static final Logger LOGGER = LogUtils.getLogger();



    public EffecoriaMod(IEventBus modEventBus, ModContainer modContainer) {

        ModAttachments.register(modEventBus);

        ModBlocks.BLOCKS.register(modEventBus);

        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);

        ModItems.ITEMS.register(modEventBus);

        ModEntities.ENTITY_TYPES.register(modEventBus);

        ModParticleTypes.PARTICLE_TYPES.register(modEventBus);

        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);



        modContainer.registerConfig(ModConfig.Type.COMMON, BalanceConfig.SPEC);

        modEventBus.addListener(EffecoriaMod::registerPayloads);
        modEventBus.addListener(ModEntities::registerAttributes);



        LOGGER.info("Effecoria {} loaded — phase 2 magic core", modContainer.getModInfo().getVersion());

    }



    public static ResourceLocation id(String path) {

        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);

    }



    private static void registerPayloads(RegisterPayloadHandlersEvent event) {

        PayloadRegistrar registrar = event.registrar(MOD_ID);

        registrar.playToServer(
                ModNetworking.InitiateSchoolPayload.TYPE,
                ModNetworking.InitiateSchoolPayload.STREAM_CODEC,
                ModNetworking.InitiateSchoolPayload::handle);
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
    }

}


