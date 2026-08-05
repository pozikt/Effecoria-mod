package com.effecoria.world;

import com.effecoria.EffecoriaMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public final class ModBiomes {
    private ModBiomes() {}

    public static final ResourceKey<Biome> ESSENCE_PLATEAU =
            ResourceKey.create(Registries.BIOME, EffecoriaMod.id("essence_plateau"));
}
