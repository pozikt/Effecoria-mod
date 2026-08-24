package com.effecoria.content;

import com.effecoria.EffecoriaMod;
import com.effecoria.world.feature.EmeraldCanopyTreeFeature;
import com.effecoria.world.feature.EmeraldCanopyUnderstoryFeature;
import com.effecoria.world.feature.DeadAshTreeFeature;
import com.effecoria.world.feature.DriedRiverbedFeature;
import com.effecoria.world.feature.EssoniteDripstoneFeature;
import com.effecoria.world.feature.EssoniteDruzeFeature;
import com.effecoria.world.feature.OmegaScarCrackFeature;
import com.effecoria.world.feature.OmegaScarTreeFeature;
import com.effecoria.world.feature.PhiCaveShellFeature;
import com.effecoria.world.feature.PhiCoreFeature;
import com.effecoria.world.feature.PhiGeyserFeature;
import com.effecoria.world.feature.PhiSkyIslandFeature;
import com.effecoria.world.feature.PhiWaterLakeFeature;
import com.effecoria.world.feature.StripWastelandWaterFeature;
import com.effecoria.world.feature.VitrifiedCraterFeature;
import com.effecoria.world.feature.VitrifiedFrozenVillageFeature;
import com.effecoria.world.feature.VitrifiedGroveFeature;
import com.effecoria.world.feature.VitrifiedMageTowerFeature;
import com.effecoria.world.feature.VitrifiedTreeFeature;
import com.effecoria.world.feature.WhisperingSpireFeature;
import com.effecoria.world.feature.ZnPhiMutePatchFeature;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFeatures {
    private ModFeatures() {}

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, EffecoriaMod.MOD_ID);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> PHI_SKY_ISLAND =
            FEATURES.register("phi_sky_island", () -> new PhiSkyIslandFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> PHI_CAVE_SHELL =
            FEATURES.register("phi_cave_shell", () -> new PhiCaveShellFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> PHI_GEYSER =
            FEATURES.register("phi_geyser", () -> new PhiGeyserFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> ESSONITE_DRIPSTONE =
            FEATURES.register("essonite_dripstone", () -> new EssoniteDripstoneFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> ESSONITE_DRUZE =
            FEATURES.register("essonite_druze", () -> new EssoniteDruzeFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> PHI_WATER_LAKE =
            FEATURES.register("phi_water_lake", () -> new PhiWaterLakeFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> PHI_CORE =
            FEATURES.register("phi_core", () -> new PhiCoreFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> DEAD_ASH_TREE =
            FEATURES.register("dead_ash_tree", () -> new DeadAshTreeFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> OMEGA_SCAR_TREE =
            FEATURES.register("omega_scar_tree", () -> new OmegaScarTreeFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> EMERALD_CANOPY_TREE =
            FEATURES.register(
                    "emerald_canopy_tree", () -> new EmeraldCanopyTreeFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> EMERALD_CANOPY_UNDERSTORY =
            FEATURES.register(
                    "emerald_canopy_understory",
                    () -> new EmeraldCanopyUnderstoryFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> OMEGA_SCAR_CRACK =
            FEATURES.register("omega_scar_crack", () -> new OmegaScarCrackFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> DRIED_RIVERBED =
            FEATURES.register("dried_riverbed", () -> new DriedRiverbedFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> STRIP_WASTELAND_WATER =
            FEATURES.register(
                    "strip_wasteland_water", () -> new StripWastelandWaterFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> VITRIFIED_TREE =
            FEATURES.register("vitrified_tree", () -> new VitrifiedTreeFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> VITRIFIED_GROVE =
            FEATURES.register("vitrified_grove", () -> new VitrifiedGroveFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> VITRIFIED_CRATER =
            FEATURES.register("vitrified_crater", () -> new VitrifiedCraterFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> VITRIFIED_MAGE_TOWER =
            FEATURES.register(
                    "vitrified_mage_tower", () -> new VitrifiedMageTowerFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> VITRIFIED_FROZEN_VILLAGE =
            FEATURES.register(
                    "vitrified_frozen_village",
                    () -> new VitrifiedFrozenVillageFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> WHISPERING_SPIRE =
            FEATURES.register("whispering_spire", () -> new WhisperingSpireFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> ZNPHI_MUTE_PATCH =
            FEATURES.register("znphi_mute_patch", () -> new ZnPhiMutePatchFeature(NoneFeatureConfiguration.CODEC));
}
