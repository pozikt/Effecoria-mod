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
                    id("weak_breeze"),
                    id("water_stream"),
                    id("water_shield"),
                    id("steam_jet"),
                    id("ice_shard"),
                    id("ember_volley"),
                    id("wind_push"),
                    id("air_ionization"),
                    id("steam_veil"),
                    id("breath_bubble"),
                    id("shockwave"),
                    id("mirage"),
                    id("ice_sheet"),
                    id("air_hand"),
                    id("sonic_lance"),
                    id("tornado"),
                    id("ion_storm"),
                    id("hyper_cooling"),
                    id("frost_bastion"),
                    id("hydro_slice"),
                    id("water_prison"),
                    id("great_fireball"),
                    id("vacuum_cage"),
                    id("lightning_spear"),
                    id("water_shroud"),
                    id("air_shroud"),
                    id("ice_prison"),
                    id("atmospheric_pressure"),
                    id("cryo_wave"),
                    id("air_form"),
                    id("hurricane_storm"),
                    id("plasma_bolt"),
                    id("steam_flight"),
                    id("elemental_supremacy"),
                    id("thermonuclear_pulse"),
                    id("absolute_zero"),
                    id("meteorological_cataclysm"),
                    id("quasar"),
                    id("plasma_barrage"));

            case ORGANIC -> List.of(
                    id("diagnostic_glimpse"),
                    id("blood_stasis"),
                    id("life_sense"),
                    id("vitality_pulse"),
                    id("bio_strike"),
                    id("bone_needle"),
                    id("foreign_agent"),
                    id("muscle_spasm"),
                    id("thorn_lash"),
                    id("root_bind"),
                    id("chitin_plates"),
                    id("acid_gland"),
                    id("bone_spur"),
                    id("sense_sharpening"),
                    id("pain_inhibitor"),
                    id("poison_thorns"),
                    id("bio_mimicry"),
                    id("organism_adaptation"),
                    id("metabolic_shock"),
                    id("parasitic_infection"),
                    id("immune_suppression"),
                    id("metabolic_boost"),
                    id("organic_necrosis"),
                    id("briar_surge"),
                    id("verdant_mend"),
                    id("full_restructuring"),
                    id("biological_field"),
                    id("scorched_earth"),
                    id("bio_fission"),
                    id("super_regeneration"),
                    id("population_control"),
                    id("biological_plague"),
                    id("living_armor"),
                    id("beast_form"),
                    id("bio_cataclysm"),
                    id("absolute_regeneration"),
                    id("cellular_dominion"),
                    id("evolutionary_leap"));

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


