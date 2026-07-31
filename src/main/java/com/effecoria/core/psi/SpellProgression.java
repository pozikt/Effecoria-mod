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
                    id("mental_push"),
                    id("mind_bolt"),
                    id("mental_sting"),
                    id("sense_phi"),
                    id("psychic_barrier"),
                    id("psychic_amplify"),
                    id("neural_lock"),
                    id("mind_terror"),
                    id("mind_lance"),
                    id("thought_lance"),
                    id("telekinetic_crush"),
                    id("psychic_scream"),
                    id("cliff_urge"),
                    id("drown_urge"),
                    id("mind_probe"),
                    id("mass_confusion"),
                    id("psychic_frenzy"),
                    id("synaptic_overload"),
                    id("psychic_drain"),
                    id("psychic_focus"),
                    id("psychic_storm"),
                    id("mental_fortress"),
                    id("thought_bomb"),
                    id("mass_hysteria"),
                    id("omega_mind"));

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
                    id("death_sense"),
                    id("bone_chill"),
                    id("necrotic_bolt"),
                    id("soul_drain"),
                    id("wither_touch"),
                    id("death_mark"),
                    id("grave_whisper"),
                    id("curse_of_frailty"),
                    id("siphon_pulse"),
                    id("bone_armor"),
                    id("phantom_step"),
                    id("grave_bind"),
                    id("life_tap"),
                    id("haunting_visage"),
                    id("soul_shackle"),
                    id("bone_volley"),
                    id("wither_wave"),
                    id("necrotic_aura"),
                    id("dark_pact"),
                    id("grave_leech"),
                    id("corpse_burst"),
                    id("grave_field"),
                    id("soul_anchor"),
                    id("lich_ward"),
                    id("death_gate"),
                    id("soul_reaper"),
                    id("death_coil"),
                    id("phylactery_surge"),
                    id("soul_cataclysm"),
                    id("death_apotheosis"));

            case SPATIAL -> List.of(
                    id("blink"),
                    id("warp_bolt"),
                    id("phase_veil"),
                    id("spatial_ward"),
                    id("rift_yank"),
                    id("fold_repulse"),
                    id("rift_slash"),
                    id("gravity_snare"),
                    id("void_step"),
                    id("dimensional_anchor"),
                    id("far_blink"),
                    id("subspace_voyage"),
                    id("gravity_well"),
                    id("warp_exchange"),
                    id("spatial_surge"),
                    id("void_lance"),
                    id("rift_burst"),
                    id("spatial_singularity"),
                    id("absolute_fold"));

            case CORRUPTION -> List.of(
                    id("corrupt_mark"),
                    id("blight_pulse"),
                    id("rot_touch"),
                    id("entropy_lash"),
                    id("binding_seal"),
                    id("plague_bolt"),
                    id("festering_wound"),
                    id("miasma_cloak"),
                    id("blight_brand"),
                    id("blight_surge"),
                    id("decay_bind"),
                    id("pestilence_wave"),
                    id("blight_field"),
                    id("entropy_aegis"),
                    id("tainted_leech"),
                    id("virulent_wave"),
                    id("plague_crown"),
                    id("omega_blight"));

            case SEALS -> List.of(
                    id("trap_seal"),
                    id("glow_seal"),
                    id("snare_glyph"),
                    id("fortify_seal"),
                    id("beacon_seal"),
                    id("shock_glyph"),
                    id("ward_glyph"),
                    id("repulsion_seal"),
                    id("anchor_fortify"),
                    id("permanent_glow"),
                    id("snare_matrix"),
                    id("shock_trap"),
                    id("omega_ward"));

            default -> List.of();

        };

    }



    public static boolean schoolHasLoadedSpells(MagicSchool school) {

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


