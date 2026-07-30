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

                    id("sense_phi"),

                    id("mind_lance"),

                    id("psychic_focus"));

            case ELEMENTAL -> List.of(
                    id("fire_burst"),
                    id("water_stream"),
                    id("wind_push"),
                    id("ember_volley"),
                    id("steam_jet"),
                    id("steam_veil"),
                    id("ice_shard"),
                    id("frost_bastion"),
                    id("hydro_slice"),
                    id("great_fireball"),
                    id("plasma_bolt"),
                    id("steam_flight"));

            case ORGANIC -> List.of(

                    id("vitality_pulse"),

                    id("thorn_lash"),

                    id("root_bind"),

                    id("briar_surge"),

                    id("verdant_mend"));

            case NECROMANCY -> List.of(

                    id("soul_drain"),

                    id("wither_touch"),

                    id("shade_summon"),

                    id("grave_leech"),

                    id("shade_swarm"));

            case SPATIAL -> List.of(

                    id("blink"),

                    id("rift_yank"),

                    id("phase_veil"),

                    id("void_step"),

                    id("gravity_well"));

            case CORRUPTION -> List.of(

                    id("corrupt_mark"),

                    id("binding_seal"),

                    id("blight_pulse"),

                    id("blight_brand"),

                    id("pestilence_wave"));

            case SEALS -> List.of(

                    id("trap_seal"),

                    id("fortify_seal"),

                    id("glow_seal"),

                    id("snare_glyph"),

                    id("beacon_seal"));

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


