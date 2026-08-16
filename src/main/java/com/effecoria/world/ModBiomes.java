package com.effecoria.world;

import com.effecoria.EffecoriaMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public final class ModBiomes {
    private ModBiomes() {}

    public static final ResourceKey<Biome> ESSENCE_PLATEAU =
            ResourceKey.create(Registries.BIOME, EffecoriaMod.id("essence_plateau"));

    /** Zero Φ-flow zone — Dead Wasteland (Φ_nature ≈ 0). */
    public static final ResourceKey<Biome> DEAD_WASTELAND =
            ResourceKey.create(Registries.BIOME, EffecoriaMod.id("dead_wasteland"));

    /** Φ-flash vitrified desert — black glass wastes. */
    public static final ResourceKey<Biome> VITRIFIED_WASTES =
            ResourceKey.create(Registries.BIOME, EffecoriaMod.id("vitrified_wastes"));

    /** Humid Φ-grove — dense mist, frequent essence rain. */
    public static final ResourceKey<Biome> CRYSTAL_FOREST =
            ResourceKey.create(Registries.BIOME, EffecoriaMod.id("crystal_forest"));

    /** Giant Φ-canopy — Emerald Canopy / Sea of Crowns. */
    public static final ResourceKey<Biome> EMERALD_CANOPY =
            ResourceKey.create(Registries.BIOME, EffecoriaMod.id("emerald_canopy"));

    /** Ω causality rupture — fog/rain and Blood Rain eligibility. */
    public static final ResourceKey<Biome> OMEGA_SCAR =
            ResourceKey.create(Registries.BIOME, EffecoriaMod.id("omega_scar"));

    /** Deep Φ pocket — low light, high entropy underground cave biome. */
    public static final ResourceKey<Biome> DEEP_PHI_POCKET =
            ResourceKey.create(Registries.BIOME, EffecoriaMod.id("deep_phi_pocket"));
}
