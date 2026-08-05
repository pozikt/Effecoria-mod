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

    /** High-Φ Glass Plain — fused-sand dunes (Φ-Пустыня). */
    public static final ResourceKey<Biome> PHI_GLASS_PLAIN =
            ResourceKey.create(Registries.BIOME, EffecoriaMod.id("phi_glass_plain"));
}
