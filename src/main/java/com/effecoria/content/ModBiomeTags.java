package com.effecoria.content;

import com.effecoria.EffecoriaMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public final class ModBiomeTags {
    private ModBiomeTags() {}

    /** High-Φ Essence Plateau — rare mountain biome. */
    public static final TagKey<Biome> ESSENCE_PLATEAU = TagKey.create(
            Registries.BIOME, EffecoriaMod.id("is_essence_plateau"));
}
