package com.effecoria.core.psi;

import com.effecoria.core.magic.MagicSchool;
import com.effecoria.magic.SpellRegistry;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class SpellProgression {
    private SpellProgression() {}

    public static List<ResourceLocation> spellsForSchool(MagicSchool school) {
        return switch (school) {
            case MENTAL -> List.of(
                    id("mental_push"),
                    id("mental_sting"),
                    id("sense_phi"));
            case ELEMENTAL -> List.of(
                    id("fire_burst"),
                    id("wind_push"),
                    id("water_stream"));
            case ORGANIC -> List.of(
                    id("vitality_pulse"),
                    id("thorn_lash"),
                    id("root_bind"));
            case NECROMANCY -> List.of(
                    id("soul_drain"),
                    id("wither_touch"),
                    id("shade_summon"));
            case SPATIAL -> List.of(
                    id("blink"),
                    id("rift_yank"),
                    id("phase_veil"));
            case CORRUPTION -> List.of(
                    id("corrupt_mark"),
                    id("binding_seal"),
                    id("blight_pulse"));
            case SEALS -> List.of(
                    id("trap_seal"),
                    id("fortify_seal"),
                    id("glow_seal"));
            default -> List.of();
        };
    }

    public static boolean schoolHasLoadedSpells(MagicSchool school) {
        return spellsForSchool(school).stream().allMatch(SpellRegistry::contains);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("effecoria", path);
    }
}
