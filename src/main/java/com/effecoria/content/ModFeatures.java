package com.effecoria.content;

import com.effecoria.EffecoriaMod;
import com.effecoria.world.feature.EssoniteDripstoneFeature;
import com.effecoria.world.feature.EssoniteDruzeFeature;
import com.effecoria.world.feature.PhiCaveShellFeature;
import com.effecoria.world.feature.PhiGeyserFeature;
import com.effecoria.world.feature.PhiSkyIslandFeature;
import com.effecoria.world.feature.PhiWaterLakeFeature;

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
}
