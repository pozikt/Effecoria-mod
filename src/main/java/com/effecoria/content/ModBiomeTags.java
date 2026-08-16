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

    /** Zero Φ-flow Dead Wasteland — magic sleeps (Φ_nature ≈ 0). */
    public static final TagKey<Biome> DEAD_WASTELAND = TagKey.create(
            Registries.BIOME, EffecoriaMod.id("is_dead_wasteland"));

    /** Vitrified Wastes — Φ-flash black-glass desert. */
    public static final TagKey<Biome> VITRIFIED_WASTES = TagKey.create(
            Registries.BIOME, EffecoriaMod.id("is_vitrified_wastes"));

    /** Crystal Forest — humid Φ woodland. */
    public static final TagKey<Biome> CRYSTAL_FOREST = TagKey.create(
            Registries.BIOME, EffecoriaMod.id("is_crystal_forest"));

    /** Emerald Canopy — giant Φ canopy / Sea of Crowns. */
    public static final TagKey<Biome> EMERALD_CANOPY = TagKey.create(
            Registries.BIOME, EffecoriaMod.id("is_emerald_canopy"));

    /** Ω-Scar — causality rupture biome. */
    public static final TagKey<Biome> OMEGA_SCAR = TagKey.create(
            Registries.BIOME, EffecoriaMod.id("is_omega_scar"));

    /** Deep Φ pocket — high-entropy underground caves. */
    public static final TagKey<Biome> DEEP_PHI_POCKET = TagKey.create(
            Registries.BIOME, EffecoriaMod.id("is_deep_phi_pocket"));
}
