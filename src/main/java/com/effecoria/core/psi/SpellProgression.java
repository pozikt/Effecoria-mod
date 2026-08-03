package com.effecoria.core.psi;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.magic.MagicSchool;
import com.effecoria.magic.SpellRegistry;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class SpellProgression {
    private SpellProgression() {}

    public static List<ResourceLocation> spellsForSchool(MagicSchool school) {
        return switch (school) {
            case MENTAL -> List.of(
                    // I — basic
                    id("sense_phi"),
                    id("psi_whisper"),
                    id("psychic_barrier"),
                    id("mental_push"),
                    // II — advanced
                    id("mind_probe"),
                    id("locus_echo"),
                    id("mind_illusion"),
                    id("psychic_scream"),
                    id("neural_lock"),
                    id("mind_terror"),
                    id("psychic_drain"),
                    // III — master
                    id("thought_lance"),
                    id("mind_dominate"),
                    id("cliff_urge"),
                    id("drown_urge"),
                    id("psychic_frenzy"),
                    id("false_memory"),
                    id("dream_lock"),
                    // IV — legendary
                    id("mass_hysteria"),
                    id("hive_mind"),
                    id("psi_echo"),
                    id("total_veil"));

            case ELEMENTAL -> List.of(
                    // I — four states entry
                    id("fire_burst"),
                    id("sear"),
                    id("weak_breeze"),
                    id("water_stream"),
                    id("ice_shard"),
                    // II — form control
                    id("water_shield"),
                    id("steam_jet"),
                    id("wind_push"),
                    id("air_ionization"),
                    id("breath_bubble"),
                    id("ice_sheet"),
                    id("shockwave"),
                    id("steam_veil"),
                    id("ore_smelt"),
                    // III — shaped working
                    id("hydro_slice"),
                    id("frost_bastion"),
                    id("plasma_bolt"),
                    id("air_hand"),
                    id("sonic_lance"),
                    id("mirage"),
                    id("ice_prison"),
                    id("vacuum_cage"),
                    id("tornado"),
                    // IV — legendary
                    id("atmospheric_pressure"),
                    id("air_form"),
                    id("steam_flight"),
                    id("lightning_spear"),
                    id("elemental_supremacy"),
                    id("quasar"));

            case ORGANIC -> List.of(
                    id("diagnostic_glimpse"),
                    id("blood_stasis"),
                    id("life_sense"),
                    id("vitality_pulse"),
                    id("soothing_sap"),
                    id("vital_infusion"),
                    id("vital_ward"),
                    id("adrenal_gift"),
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
                    id("symbiotic_graft"),
                    id("full_restructuring"),
                    id("biological_field"),
                    id("verdant_bloom"),
                    id("scorched_earth"),
                    id("bio_fission"),
                    id("super_regeneration"),
                    id("limb_regeneration"),
                    id("population_control"),
                    id("genetic_lock"),
                    id("gene_engineering"),
                    id("biological_plague"),
                    id("biological_cleaving"),
                    id("living_armor"),
                    id("beast_form"),
                    id("full_transformation"),
                    id("spore_storm"),
                    id("bio_cataclysm"),
                    id("absolute_regeneration"),
                    id("cellular_dominion"),
                    id("biological_singularity"),
                    id("life_creation"),
                    id("biological_immortality"),
                    id("evolutionary_leap"));

            case NECROMANCY -> List.of(
                    // I — basic
                    id("death_sense"),
                    id("death_mark"),
                    id("grave_whisper"),
                    id("raise_skeleton"),
                    id("death_shadow"),
                    // II — advanced
                    id("shade_summon"),
                    id("soul_shackle"),
                    id("lich_ward"),
                    id("phantom_step"),
                    id("haunting_visage"),
                    id("corpse_burst"),
                    // III — master
                    id("death_gate"),
                    id("soul_anchor"),
                    id("soul_cataclysm"),
                    id("lich_ascension"),
                    id("phylactery_surge"),
                    id("army_of_dead"),
                    // IV — legendary
                    id("dark_pact"),
                    id("soul_reaper"));

            case SPATIAL -> List.of(
                    // I — basic
                    id("warp_bolt"),
                    id("blink"),
                    id("void_step"),
                    id("phase_veil"),
                    // II — advanced
                    id("far_blink"),
                    id("spatial_ward"),
                    id("gravity_well"),
                    id("fold_repulse"),
                    id("rift_yank"),
                    id("warp_exchange"),
                    // III — master
                    id("absolute_fold"),
                    id("gravity_snare"),
                    id("rift_slash"),
                    id("rift_excise"),
                    // IV — legendary
                    id("spatial_singularity"),
                    id("dimensional_anchor"),
                    id("subspace_voyage"));

            case CORRUPTION -> List.of(
                    // I — mark / rot
                    id("corrupt_mark"),
                    id("rot_touch"),
                    id("blight_pulse"),
                    // II — plague / bind
                    id("plague_bolt"),
                    id("festering_wound"),
                    id("decay_bind"),
                    id("entropy_aegis"),
                    // III — field
                    id("miasma_cloak"),
                    id("blight_field"),
                    id("tainted_leech"),
                    // IV — crown / omega
                    id("virulent_wave"),
                    id("plague_crown"),
                    id("omega_blight"));

            case SEALS -> List.of();

            default -> List.of();
        };
    }

    public static boolean schoolHasLoadedSpells(MagicSchool school) {
        if (school == MagicSchool.SEALS) {
            return true;
        }
        return spellsForSchool(school).stream().allMatch(SpellRegistry::contains);
    }

    public static List<ResourceLocation> starterSpells(MagicSchool school) {
        List<ResourceLocation> all = spellsForSchool(school);
        int count = Math.min(BalanceConfig.SPELL_STARTER_COUNT.get(), all.size());
        return List.copyOf(all.subList(0, count));
    }

    public static int progressionIndex(MagicSchool school, ResourceLocation spellId) {
        List<ResourceLocation> all = spellsForSchool(school);
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).equals(spellId)) {
                return i;
            }
        }
        return -1;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("effecoria", path);
    }
}
