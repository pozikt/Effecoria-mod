package com.effecoria.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Runtime balance knobs. Lore formulas use these scales — tweak without recompiling logic.
 */
public final class BalanceConfig {
    private BalanceConfig() {}

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue PSI_REGEN_SCALE = BUILDER
            .comment("Global multiplier for Ψ regen per tick (ΔE_Ψ discretization)")
            .defineInRange("psi_regen_scale", 0.05, 0.001, 10.0);

    public static final ModConfigSpec.DoubleValue SPELL_POWER_SCALE = BUILDER
            .comment("Global multiplier for spell effect power")
            .defineInRange("spell_power_scale", 1.0, 0.01, 100.0);

    public static final ModConfigSpec.DoubleValue SPELL_POWER_HARD_CAP = BUILDER
            .comment("Hard ceiling on delivered spell power for ALL casters (stops freezes at absurd Φ/XP). 0 = uncapped.")
            .defineInRange("spell_power_hard_cap", 72.0, 0.0, 500.0);

    public static final ModConfigSpec.DoubleValue PHI_MULTIPLIER_BONUS_CAP = BUILDER
            .comment("Max ambient Φ bonus from player phi_multiplier (bonus = clamp(mult,0,cap) - 1)")
            .defineInRange("phi_multiplier_bonus_cap", 2.5, 0.0, 50.0);

    public static final ModConfigSpec.DoubleValue TECHNO_POWER_SCALE = BUILDER
            .comment("Technomagic: Φ_reactor × C × K scale (phase 5)")
            .defineInRange("techno_power_scale", 1.0, 0.01, 100.0);

    public static final ModConfigSpec.DoubleValue LOW_PHI_COST_FACTOR = BUILDER
            .comment("Extra cost multiplier when casting in low-Φ zones")
            .defineInRange("low_phi_cost_factor", 0.5, 0.0, 5.0);

    public static final ModConfigSpec.DoubleValue ENTROPY_SCALE = BUILDER
            .comment("b-component accumulation scale (backlash / Ω bleed)")
            .defineInRange("entropy_scale", 0.02, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue ENTROPY_THRESHOLD = BUILDER
            .comment("Entropy level that triggers backlash event")
            .defineInRange("entropy_threshold", 1.0, 0.1, 100.0);

    public static final ModConfigSpec.DoubleValue ENTROPY_DECAY_PER_TICK = BUILDER
            .comment("Entropy recovered every 10 server ticks while not casting")
            .defineInRange("entropy_decay_per_tick", 0.005, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue ENTROPY_MEDITATION_DECAY_BONUS = BUILDER
            .comment("Extra entropy decay while meditating with full breath")
            .defineInRange("entropy_meditation_decay_bonus", 0.008, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue ENTROPY_WARN_RATIO = BUILDER
            .comment("Fraction of entropy threshold that shows HUD warn / first tip")
            .defineInRange("entropy_warn_ratio", 0.55, 0.1, 1.0);

    public static final ModConfigSpec.DoubleValue RESONANCE_WIDTH_HZ = BUILDER
            .comment("Hz tolerance for spectral resonance (wider = easier off-frequency casts)")
            .defineInRange("resonance_width_hz", 5.0, 0.1, 50.0);

    public static final ModConfigSpec.DoubleValue DEFAULT_MAX_PSI = BUILDER
            .comment("Default maximum Ψ energy for new players")
            .defineInRange("default_max_psi", 100.0, 10.0, 1000.0);

    public static final ModConfigSpec.DoubleValue DEFAULT_STARTING_PSI = BUILDER
            .comment("Ψ energy granted on initiation")
            .defineInRange("default_starting_psi", 50.0, 0.0, 1000.0);

    public static final ModConfigSpec.DoubleValue BACKLASH_DAMAGE = BUILDER
            .comment("Hearts of damage when entropy backlash triggers")
            .defineInRange("backlash_damage", 4.0, 0.0, 20.0);

    public static final ModConfigSpec.DoubleValue SPELL_COST_FLOOR_RATIO = BUILDER
            .comment("Spell cost floor once breathing reaches reference mastery (100%) past unlock")
            .defineInRange("spell_cost_floor_ratio", 0.33, 0.1, 1.0);

    public static final ModConfigSpec.DoubleValue SPELL_COST_ASCENSION_FLOOR = BUILDER
            .comment("Further spell cost floor for post-100% breathing ascension (must be <= spell_cost_floor_ratio)")
            .defineInRange("spell_cost_ascension_floor", 0.18, 0.05, 1.0);

    public static final ModConfigSpec.DoubleValue FOCUS_COST_FLOOR_PER_TIER = BUILDER
            .comment("How much each Resonance Focus tier lowers the spell cost floor ratio")
            .defineInRange("focus_cost_floor_per_tier", 0.04, 0.0, 0.2);

    public static final ModConfigSpec.DoubleValue FOCUS_RESONANCE_WIDTH_PER_TIER = BUILDER
            .comment("Extra resonance Hz width per Resonance Focus tier")
            .defineInRange("focus_resonance_width_per_tier", 1.5, 0.0, 20.0);

    public static final ModConfigSpec.DoubleValue PHI_CELL_ASSIST_THRESHOLD = BUILDER
            .comment("Ambient Φ below this triggers Phi Cell assist on cast")
            .defineInRange("phi_cell_assist_threshold", 0.75, 0.1, 5.0);

    public static final ModConfigSpec.DoubleValue PHI_CELL_PHI_PER_CHARGE = BUILDER
            .comment("Φ gained per 1.0 of Phi Cell charge spent")
            .defineInRange("phi_cell_phi_per_charge", 0.9, 0.1, 5.0);

    public static final ModConfigSpec.DoubleValue PHI_CELL_RECHARGE_PER_TICK = BUILDER
            .comment("Phi Cell recharge while held in high ambient Φ (per progression tick)")
            .defineInRange("phi_cell_recharge_per_tick", 0.01, 0.0, 1.0);

    public static final ModConfigSpec.BooleanValue CREATIVE_GOD_MODE = BUILDER
            .comment("In creative mode: free casts, full Ψ, no backlash (for testing)")
            .define("creative_god_mode", true);

    public static final ModConfigSpec.DoubleValue CREATIVE_PHI_OVERRIDE = BUILDER
            .comment("Ambient Φ while creative god mode is active (was 999 — caused one-shot spell scaling)")
            .defineInRange("creative_phi_override", 1.0, 0.1, 10.0);

    public static final ModConfigSpec.DoubleValue CREATIVE_SPELL_POWER_CAP = BUILDER
            .comment("Maximum effective spell power in creative god mode (0 = no cap)")
            .defineInRange("creative_spell_power_cap", 52.0, 0.0, 500.0);

    public static final ModConfigSpec.DoubleValue PHI_DAY_MULTIPLIER = BUILDER
            .comment("Day Φ as factor around 1.0; stacked additively as (value - 1). Default 1.1 → +0.1")
            .defineInRange("phi_day_multiplier", 1.1, 0.0, 5.0);

    public static final ModConfigSpec.DoubleValue PHI_NIGHT_MULTIPLIER = BUILDER
            .comment("Night Φ as factor around 1.0; stacked additively as (value - 1). Default 0.75 → -0.25")
            .defineInRange("phi_night_multiplier", 0.75, 0.0, 5.0);

    public static final ModConfigSpec.DoubleValue BREATHING_MAX_MASTERY = BUILDER
            .comment("Reference breathing mastery shown as 100%. Unlock thresholds and baseline bonuses scale from this — not a hard cap.")
            .defineInRange("breathing_max_mastery", 1.0, 0.1, 10.0);

    public static final ModConfigSpec.DoubleValue BREATHING_HARD_CAP = BUILDER
            .comment("Absolute breathing mastery ceiling (0 = uncapped). Default 10.0 ≈ 1000% for ascension beyond mortal mastery.")
            .defineInRange("breathing_hard_cap", 10.0, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue BREATHING_MEDITATION_GAIN = BUILDER
            .comment("Deprecated — meditation breathing gains removed; kept for config compatibility")
            .defineInRange("breathing_meditation_gain", 0.002, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue BREATHING_TRAIN_REGEN_BONUS = BUILDER
            .comment("Permanent Ψ regen bonus per successful breathing mini-game hit (0.001 = +0.1%)")
            .defineInRange("breathing_train_regen_bonus", 0.001, 0.0, 0.1);

    public static final ModConfigSpec.DoubleValue BREATHING_TRAIN_MASTERY_GAIN = BUILDER
            .comment("Breathing mastery gained per successful mini-game hit")
            .defineInRange("breathing_train_mastery_gain", 0.007, 0.0, 1.0);

    public static final ModConfigSpec.IntValue BREATHING_TRAIN_MISS_LIMIT = BUILDER
            .comment("Failed timing clicks before breathing-train fatigue applies")
            .defineInRange("breathing_train_miss_limit", 3, 1, 20);

    public static final ModConfigSpec.IntValue BREATHING_TRAIN_FATIGUE_MS = BUILDER
            .comment("Real-time fatigue after miss limit (ms) — blocks retraining and cuts regen")
            .defineInRange("breathing_train_fatigue_ms", 120_000, 0, 3_600_000);

    public static final ModConfigSpec.DoubleValue BREATHING_TRAIN_FATIGUE_REGEN_MULT = BUILDER
            .comment("Ψ regen multiplier while breathing-train fatigue is active")
            .defineInRange("breathing_train_fatigue_regen_mult", 0.5, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue BREATHING_SCROLL_GAIN = BUILDER
            .comment("Mastery granted by one breathing technique scroll")
            .defineInRange("breathing_scroll_gain", 0.08, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue BREATHING_BIOLOGY_BONUS_MAX = BUILDER
            .comment("Orkanum (biologyQ) bonus per reference mastery (100%). Scales past 100% — e.g. 0.3 = +30% at 100%, +60% at 200%.")
            .defineInRange("breathing_biology_bonus_max", 0.30, 0.0, 2.0);

    public static final ModConfigSpec.DoubleValue TRAINING_XP_WALK_PER_BLOCK = BUILDER
            .comment("Training XP per block walked on ground (sampled every progression tick)")
            .defineInRange("training_xp_walk_per_block", 0.28, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue TRAINING_XP_SPRINT_PER_BLOCK = BUILDER
            .comment("Training XP per block sprinted on ground")
            .defineInRange("training_xp_sprint_per_block", 0.48, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue TRAINING_XP_SWIM_PER_BLOCK = BUILDER
            .comment("Training XP per block swum")
            .defineInRange("training_xp_swim_per_block", 0.36, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue TRAINING_XP_MEDITATE = BUILDER
            .comment("Training XP per progression tick while meditating (standing calm, full breath)")
            .defineInRange("training_xp_meditate", 0.14, 0.0, 1000.0);

    public static final ModConfigSpec.DoubleValue TRAINING_XP_CAST = BUILDER
            .comment("Training XP for a full successful spell cast")
            .defineInRange("training_xp_cast", 1.2, 0.0, 1000.0);

    public static final ModConfigSpec.DoubleValue TRAINING_XP_CAST_STREAK = BUILDER
            .comment("Bonus training XP per consecutive full cast (after the first)")
            .defineInRange("training_xp_cast_streak", 0.35, 0.0, 1000.0);

    public static final ModConfigSpec.IntValue TRAINING_XP_CAST_STREAK_CAP = BUILDER
            .comment("Max cast streak steps that grant bonus XP")
            .defineInRange("training_xp_cast_streak_cap", 8, 1, 50);

    public static final ModConfigSpec.DoubleValue TRAINING_XP_BREATH_TRAIN = BUILDER
            .comment("Training XP granted on a successful breathing-train hit")
            .defineInRange("training_xp_breath_train", 2.5, 0.0, 1000.0);

    public static final ModConfigSpec.DoubleValue TRAINING_XP_THRESHOLD = BUILDER
            .comment("Training XP required for one soul/psi milestone")
            .defineInRange("training_xp_threshold", 100.0, 1.0, 10000.0);

    public static final ModConfigSpec.DoubleValue TRAINING_SOUL_GAIN = BUILDER
            .comment("Ψ_soul increase per training milestone")
            .defineInRange("training_soul_gain", 0.03, 0.0, 5.0);

    public static final ModConfigSpec.DoubleValue TRAINING_MAX_SOUL = BUILDER
            .comment("Maximum Ψ_soul from training")
            .defineInRange("training_max_soul", 2.0, 1.0, 50.0);

    public static final ModConfigSpec.DoubleValue TRAINING_MAX_PSI_GAIN = BUILDER
            .comment("Max Ψ (internal cast energy) added per training milestone — movement fills the bar")
            .defineInRange("training_max_psi_gain", 1.0, 0.0, 500.0);

    public static final ModConfigSpec.DoubleValue TRAINING_MAX_PSI_CAP = BUILDER
            .comment("Maximum max Ψ reachable from training milestones (ignored if below default_max_psi + training_max_psi_bonus)")
            .defineInRange("training_max_psi_cap", 250.0, 10.0, 10000.0);

    public static final ModConfigSpec.DoubleValue TRAINING_MAX_PSI_BONUS = BUILDER
            .comment("Minimum total max-Ψ headroom above default_max_psi from training (floor for the cap)")
            .defineInRange("training_max_psi_bonus", 150.0, 0.0, 10000.0);

    public static final ModConfigSpec.DoubleValue WHIFF_COST_FRACTION = BUILDER
            .comment("Ψ spent when a targeted spell finds no valid target (fraction of full cost)")
            .defineInRange("whiff_cost_fraction", 0.25, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue MASTERY_BREATHING_MAX = BUILDER
            .comment("Spell power mastery bonus per reference breathing mastery (100%). Scales linearly past 100%.")
            .defineInRange("mastery_breathing_max", 0.10, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue DEBUFF_DURATION_PER_REFERENCE = BUILDER
            .comment("Extra harmful/neutral effect duration per reference breathing mastery (0.35 = +35% at 100%, +70% at 200%)")
            .defineInRange("debuff_duration_per_reference", 0.12, 0.0, 2.0);

    public static final ModConfigSpec.DoubleValue DEBUFF_AMPLIFIER_PER_REFERENCE = BUILDER
            .comment("Extra harmful/neutral effect amplifier levels per reference breathing mastery (1.0 = +1 at 100%)")
            .defineInRange("debuff_amplifier_per_reference", 0.25, 0.0, 5.0);

    public static final ModConfigSpec.IntValue DEBUFF_AMPLIFIER_MAX_BONUS = BUILDER
            .comment("Cap on breathing-granted amplifier bonus for debuffs")
            .defineInRange("debuff_amplifier_max_bonus", 2, 0, 20);

    public static final ModConfigSpec.DoubleValue MASTERY_ESSENCE_PER_POINT = BUILDER
            .comment("Spell mastery bonus per absorbed essence point")
            .defineInRange("mastery_essence_per_point", 0.01, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue MASTERY_ESSENCE_CAP = BUILDER
            .comment("Maximum mastery bonus from essence alone")
            .defineInRange("mastery_essence_cap", 0.12, 0.0, 2.0);

    public static final ModConfigSpec.DoubleValue MASTERY_COST_REDUCTION_RATIO = BUILDER
            .comment("Fraction of (mastery - 1) applied as spell cost reduction")
            .defineInRange("mastery_cost_reduction_ratio", 0.4, 0.0, 1.0);

    public static final ModConfigSpec.IntValue ESSENCE_PER_TRAINING_MILESTONE = BUILDER
            .comment("Essence absorbed per physical training milestone")
            .defineInRange("essence_per_training_milestone", 1, 0, 100);

    public static final ModConfigSpec.IntValue SPELL_STARTER_COUNT = BUILDER
            .comment("First N spells in each school progression unlock without essence (mastery gates still apply)")
            .defineInRange("spell_starter_count", 3, 1, 20);

    public static final ModConfigSpec.IntValue SPELL_UNLOCK_ESSENCE_STEP = BUILDER
            .comment("Default essence cost grows by this amount per progression tier after starters (when JSON omits unlock_essence_cost)")
            .defineInRange("spell_unlock_essence_step", 3, 0, 50);

    public static final ModConfigSpec.DoubleValue NECRO_SUMMON_PSI_RESERVE = BUILDER
            .comment("Legacy flat thrall reserve (unused — Death Mark thralls reserve max health instead)")
            .defineInRange("necro_summon_psi_reserve", 18.0, 1.0, 100.0);

    // --- Biology (hunger / air) ---
    public static final ModConfigSpec.DoubleValue BIOLOGY_HUNGER_MIN = BUILDER
            .comment("biologyQ multiplier at starving (food level <= 6)")
            .defineInRange("biology_hunger_min", 0.55, 0.1, 1.0);

    public static final ModConfigSpec.DoubleValue BIOLOGY_SATURATION_BONUS = BUILDER
            .comment("biologyQ bonus when well-fed with saturation")
            .defineInRange("biology_saturation_bonus", 0.05, 0.0, 0.5);

    public static final ModConfigSpec.DoubleValue BIOLOGY_AIR_MIN = BUILDER
            .comment("biologyQ multiplier at very low air supply")
            .defineInRange("biology_air_min", 0.6, 0.1, 1.0);

    public static final ModConfigSpec.DoubleValue BIOLOGY_DEFAULT_BASELINE = BUILDER
            .comment("Default Orkanum baseline for humans (Stage II races override via BiologyService.applyRaceBaseline)")
            .defineInRange("biology_default_baseline", 0.6, 0.05, 2.0);

    public static final ModConfigSpec.DoubleValue BIOLOGY_SPELL_POWER_WEIGHT = BUILDER
            .comment("How much effective Orkanum (biologyQ×body) soft-scales spell power; 0 = regen-only")
            .defineInRange("biology_spell_power_weight", 0.12, 0.0, 0.5);

    public static final ModConfigSpec.DoubleValue BIOLOGY_SPELL_POWER_MIN = BUILDER
            .comment("Floor for Orkanum spell-power multiplier")
            .defineInRange("biology_spell_power_min", 0.88, 0.5, 1.0);

    public static final ModConfigSpec.DoubleValue BIOLOGY_SPELL_POWER_MAX = BUILDER
            .comment("Ceiling for Orkanum spell-power multiplier")
            .defineInRange("biology_spell_power_max", 1.12, 1.0, 1.5);

    public static final ModConfigSpec.IntValue BREATHING_TRAIN_MIN_INTERVAL_MS = BUILDER
            .comment("Server-side minimum real-time gap between accepted breathing-train hits (anti-spam)")
            .defineInRange("breathing_train_min_interval_ms", 280, 50, 5000);

    // --- Essence Plateau biome ---
    public static final ModConfigSpec.DoubleValue PLATEAU_PHI_BONUS = BUILDER
            .comment("Additive Φ value bonus while standing in Essence Plateau (high-Φ field)")
            .defineInRange("plateau_phi_bonus", 0.55, 0.0, 2.0);

    public static final ModConfigSpec.DoubleValue PLATEAU_SPELL_POWER_MULT = BUILDER
            .comment("Spell power multiplier inside Essence Plateau")
            .defineInRange("plateau_spell_power_mult", 1.25, 1.0, 2.5);

    public static final ModConfigSpec.DoubleValue PLATEAU_SPELL_COST_MULT = BUILDER
            .comment("Spell Ψ cost multiplier inside Essence Plateau (< 1 = cheaper)")
            .defineInRange("plateau_spell_cost_mult", 0.75, 0.1, 1.0);

    public static final ModConfigSpec.DoubleValue PLATEAU_REGEN_MULT = BUILDER
            .comment("Passive Ψ regen multiplier for initiated mages in the plateau")
            .defineInRange("plateau_regen_mult", 1.35, 1.0, 3.0);

    public static final ModConfigSpec.DoubleValue PLATEAU_GRAVITY_MULT = BUILDER
            .comment("Gravity multiplier in plateau (0.8 = lighter jumps)")
            .defineInRange("plateau_gravity_mult", 0.8, 0.3, 1.2);

    public static final ModConfigSpec.DoubleValue PLATEAU_EXHAUSTION_SPIKE = BUILDER
            .comment("Periodic exhaustion for unprotected mages exposed to raw Φ")
            .defineInRange("plateau_exhaustion_spike", 8.0, 0.0, 50.0);

    public static final ModConfigSpec.IntValue PLATEAU_BURN_INTERVAL_TICKS = BUILDER
            .comment("Ticks between Φ-burn pulses when unprotected (0 = disabled)")
            .defineInRange("plateau_burn_interval_ticks", 24000, 0, 200000);

    public static final ModConfigSpec.DoubleValue PLATEAU_BURN_DAMAGE = BUILDER
            .comment("Magic damage per Φ-burn pulse")
            .defineInRange("plateau_burn_damage", 2.0, 0.0, 20.0);

    public static final ModConfigSpec.DoubleValue PLATEAU_BURN_EXHAUSTION = BUILDER
            .comment("Exhaustion added per Φ-burn pulse for initiated mages")
            .defineInRange("plateau_burn_exhaustion", 12.0, 0.0, 50.0);

    public static final ModConfigSpec.IntValue PLATEAU_CAVE_MAX_Y = BUILDER
            .comment("Top of Crystal Caverns layer (below crust / surface peaks)")
            .defineInRange("plateau_cave_max_y", 128, -64, 320);

    public static final ModConfigSpec.IntValue PLATEAU_ROOT_MAX_Y = BUILDER
            .comment("Top of Φ-core (Φ-ядро) layer — extreme radiation at/below this (toward bedrock)")
            .defineInRange("plateau_root_max_y", 0, -64, 320);

    public static final ModConfigSpec.IntValue PLATEAU_CRUST_MAX_Y = BUILDER
            .comment("Top of dense Φ-crust band; between crust and sky = surface highland")
            .defineInRange("plateau_crust_max_y", 192, -64, 320);

    public static final ModConfigSpec.IntValue PLATEAU_SKY_MIN_Y = BUILDER
            .comment("Sky-island / extreme peak air layer starts at this Y")
            .defineInRange("plateau_sky_min_y", 220, 64, 320);

    public static final ModConfigSpec.DoubleValue PLATEAU_CAVE_PHI_BONUS = BUILDER
            .comment("Extra additive Φ bonus in Crystal Caverns (Y between root and cave max)")
            .defineInRange("plateau_cave_phi_bonus", 0.35, 0.0, 2.0);

    public static final ModConfigSpec.DoubleValue PLATEAU_ROOT_PHI_BONUS = BUILDER
            .comment("Extra additive Φ bonus in Φ-core (Y <= root max)")
            .defineInRange("plateau_root_phi_bonus", 0.85, 0.0, 3.0);

    public static final ModConfigSpec.DoubleValue PLATEAU_ROOT_RADIATION_DAMAGE = BUILDER
            .comment("Magic damage per second in Φ-core without protection (lethal without shield)")
            .defineInRange("plateau_root_radiation_damage", 4.0, 0.0, 20.0);

    // --- Dead Wasteland (Zero Φ-flow) ---
    public static final ModConfigSpec.IntValue WASTELAND_MAGE_COMA_TICKS = BUILDER
            .comment("Ticks in Dead Wasteland before initiated mages enter Orkanum coma (default ~15 min)")
            .defineInRange("wasteland_mage_coma_ticks", 18000, 200, 240000);

    public static final ModConfigSpec.DoubleValue WASTELAND_CELL_DRAIN_PER_SECOND = BUILDER
            .comment("Φ-cell charge drained per second while in Dead Wasteland")
            .defineInRange("wasteland_cell_drain_per_second", 0.008, 0.0, 0.2);

    public static final ModConfigSpec.DoubleValue WASTELAND_CRYSTAL_BLEED_CHANCE = BUILDER
            .comment("Per-second chance to destroy one essonite dust/shard/crystal in inventory")
            .defineInRange("wasteland_crystal_bleed_chance", 0.02, 0.0, 1.0);

    // --- Φ-Glass Plain (Стеклянная Равнина) ---
    public static final ModConfigSpec.DoubleValue GLASS_PLAIN_PHI_BONUS = BUILDER
            .comment("Additive Φ bonus on the Glass Plain surface")
            .defineInRange("glass_plain_phi_bonus", 0.55, 0.0, 3.0);
    public static final ModConfigSpec.DoubleValue GLASS_PLAIN_STORM_PHI_BONUS = BUILDER
            .comment("Extra Φ during Glass Plain storms")
            .defineInRange("glass_plain_storm_phi_bonus", 0.35, 0.0, 2.0);
    public static final ModConfigSpec.DoubleValue GLASS_PLAIN_ELEMENTAL_SPELL_MULT = BUILDER
            .comment("Fire/lightning elemental spell power on Glass Plain")
            .defineInRange("glass_plain_elemental_spell_mult", 1.25, 0.1, 3.0);
    public static final ModConfigSpec.DoubleValue GLASS_PLAIN_WATER_SPELL_MULT = BUILDER
            .comment("Water/ice/steam elemental spell power on Glass Plain")
            .defineInRange("glass_plain_water_spell_mult", 0.6, 0.05, 2.0);
    public static final ModConfigSpec.DoubleValue GLASS_PLAIN_SPATIAL_SPELL_MULT = BUILDER
            .comment("Spatial spell power on Glass Plain")
            .defineInRange("glass_plain_spatial_spell_mult", 0.85, 0.05, 2.0);
    public static final ModConfigSpec.DoubleValue GLASS_PLAIN_SPATIAL_STORM_ENTROPY = BUILDER
            .comment("Extra entropy added to spatial casts during Glass Plain storms")
            .defineInRange("glass_plain_spatial_storm_entropy", 0.08, 0.0, 1.0);
    public static final ModConfigSpec.IntValue GLASS_PLAIN_STORM_DURATION_TICKS = BUILDER
            .comment("Glass Plain storm window length")
            .defineInRange("glass_plain_storm_duration_ticks", 2400, 200, 48000);
    public static final ModConfigSpec.IntValue GLASS_PLAIN_STORM_CALM_TICKS = BUILDER
            .comment("Calm window between Glass Plain storms")
            .defineInRange("glass_plain_storm_calm_ticks", 6000, 200, 120000);
    public static final ModConfigSpec.DoubleValue GLASS_PLAIN_STORM_ABRASION = BUILDER
            .comment("Magic damage per second during Glass Plain storms")
            .defineInRange("glass_plain_storm_abrasion", 0.75, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue GLASS_PLAIN_STORM_EXHAUSTION = BUILDER
            .comment("Orkanum exhaustion added per second in storm for initiated mages")
            .defineInRange("glass_plain_storm_exhaustion", 0.015, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue GLASS_PLAIN_STORM_LIGHTNING_CHANCE = BUILDER
            .comment("Per 2-second chance of a Φ-lightning near the player during storms")
            .defineInRange("glass_plain_storm_lightning_chance", 0.08, 0.0, 1.0);

    public static final ModConfigSpec.IntValue GEYSER_DORMANT_MIN_TICKS = BUILDER
            .comment("Minimum Φ-geyser dormant phase duration")
            .defineInRange("geyser_dormant_min_ticks", 4800, 200, 240000);
    public static final ModConfigSpec.IntValue GEYSER_DORMANT_MAX_TICKS = BUILDER
            .comment("Maximum Φ-geyser dormant phase duration")
            .defineInRange("geyser_dormant_max_ticks", 16000, 400, 480000);
    public static final ModConfigSpec.IntValue GEYSER_PRECURSOR_TICKS = BUILDER
            .comment("Φ-geyser precursor (rumble) duration")
            .defineInRange("geyser_precursor_ticks", 200, 40, 6000);
    public static final ModConfigSpec.IntValue GEYSER_ERUPTION_TICKS = BUILDER
            .comment("Φ-geyser eruption duration")
            .defineInRange("geyser_eruption_ticks", 180, 40, 12000);
    public static final ModConfigSpec.IntValue GEYSER_COOLDOWN_TICKS = BUILDER
            .comment("Φ-geyser cooldown after eruption")
            .defineInRange("geyser_cooldown_ticks", 500, 40, 24000);
    public static final ModConfigSpec.DoubleValue GEYSER_NEAR_RADIUS = BUILDER
            .comment("Radius for near-geyser regen / non-mage strain")
            .defineInRange("geyser_near_radius", 8.0, 2.0, 32.0);
    public static final ModConfigSpec.DoubleValue GEYSER_COLUMN_HEIGHT = BUILDER
            .comment("Plasma column height in blocks")
            .defineInRange("geyser_column_height", 48.0, 8.0, 160.0);
    public static final ModConfigSpec.DoubleValue GEYSER_NEAR_REGEN = BUILDER
            .comment("Relative regen intensity while near a dormant/cooling geyser (mages)")
            .defineInRange("geyser_near_regen", 3.5, 1.0, 10.0);
    public static final ModConfigSpec.DoubleValue GEYSER_NEAR_REGEN_ERUPT = BUILDER
            .comment("Relative regen intensity while near an erupting geyser (mages)")
            .defineInRange("geyser_near_regen_erupt", 5.0, 1.0, 12.0);
    public static final ModConfigSpec.IntValue GEYSER_SAFE_COLUMN_TICKS = BUILDER
            .comment("Ticks a mage may stand in the plasma column before Orkanum burn")
            .defineInRange("geyser_safe_column_ticks", 60, 5, 400);
    public static final ModConfigSpec.DoubleValue GEYSER_COLUMN_EXHAUSTION = BUILDER
            .comment("Exhaustion per tick after safe column window")
            .defineInRange("geyser_column_exhaustion", 2.5, 0.0, 20.0);
    public static final ModConfigSpec.DoubleValue GEYSER_TOUCH_EXHAUSTION = BUILDER
            .comment("Exhaustion when a mage touches the crack")
            .defineInRange("geyser_touch_exhaustion", 18.0, 0.0, 50.0);
    public static final ModConfigSpec.IntValue GEYSER_DUST_MIN = BUILDER
            .comment("Minimum essonite dust dropped after eruption")
            .defineInRange("geyser_dust_min", 3, 0, 64);
    public static final ModConfigSpec.IntValue GEYSER_DUST_MAX = BUILDER
            .comment("Maximum essonite dust dropped after eruption")
            .defineInRange("geyser_dust_max", 9, 0, 64);

    public static final ModConfigSpec.IntValue PHI_FOG_BASE_DENSITY = BUILDER
            .comment("Base Φ-fog density on Essence Plateau (0–3)")
            .defineInRange("phi_fog_base_density", 1, 0, 3);
    public static final ModConfigSpec.IntValue PHI_FOG_GEYSER_RADIUS = BUILDER
            .comment("Block radius around Φ-geysers that thickens fog")
            .defineInRange("phi_fog_geyser_radius", 12, 2, 32);
    public static final ModConfigSpec.BooleanValue PHI_FOG_STORM_ENABLED = BUILDER
            .comment("Thunderstorms escalate Φ-fog to storm density")
            .define("phi_fog_storm_enabled", true);
    public static final ModConfigSpec.DoubleValue PHI_FOG_REGEN_HAZE = BUILDER
            .comment("Ψ regen multiplier in light Φ-haze")
            .defineInRange("phi_fog_regen_haze", 1.5, 1.0, 4.0);
    public static final ModConfigSpec.DoubleValue PHI_FOG_REGEN_DENSE = BUILDER
            .comment("Ψ regen multiplier in dense Φ-fog")
            .defineInRange("phi_fog_regen_dense", 2.0, 1.0, 5.0);
    public static final ModConfigSpec.DoubleValue PHI_FOG_REGEN_STORM = BUILDER
            .comment("Ψ regen multiplier in Φ-storm fog (<1 drains feel via separate drain)")
            .defineInRange("phi_fog_regen_storm", 0.55, 0.0, 2.0);
    public static final ModConfigSpec.DoubleValue PHI_FOG_FAR_HAZE = BUILDER
            .comment("Client fog far plane (blocks) for haze")
            .defineInRange("phi_fog_far_haze", 28.0, 4.0, 96.0);
    public static final ModConfigSpec.DoubleValue PHI_FOG_FAR_DENSE = BUILDER
            .comment("Client fog far plane (blocks) for dense fog")
            .defineInRange("phi_fog_far_dense", 12.0, 3.0, 64.0);
    public static final ModConfigSpec.DoubleValue PHI_FOG_FAR_STORM = BUILDER
            .comment("Client fog far plane (blocks) for storm fog")
            .defineInRange("phi_fog_far_storm", 6.0, 2.0, 32.0);
    public static final ModConfigSpec.IntValue PHI_FOG_MAGE_INTOX_TICKS = BUILDER
            .comment("Ticks in fog before mage Φ-intoxication starts")
            .defineInRange("phi_fog_mage_intox_ticks", 6000, 200, 240000);
    public static final ModConfigSpec.IntValue PHI_FOG_NON_MAGE_INTOX_TICKS = BUILDER
            .comment("Ticks in fog before non-mage headache / nausea")
            .defineInRange("phi_fog_non_mage_intox_ticks", 1200, 100, 72000);
    public static final ModConfigSpec.DoubleValue PHI_FOG_STORM_PSI_DRAIN = BUILDER
            .comment("Ψ drained per second during Φ-storm fog (mages)")
            .defineInRange("phi_fog_storm_psi_drain", 2.5, 0.0, 50.0);
    public static final ModConfigSpec.DoubleValue PHI_FOG_STORM_EXHAUSTION = BUILDER
            .comment("Exhaustion per second during Φ-storm fog")
            .defineInRange("phi_fog_storm_exhaustion", 1.2, 0.0, 20.0);

    public static final ModConfigSpec.IntValue PLATEAU_DRIPSTONE_COUNT = BUILDER
            .comment("Worldgen count attempts for essonite dripstone clusters (datapack also sets count)")
            .defineInRange("plateau_dripstone_count", 48, 0, 256);
    public static final ModConfigSpec.IntValue PLATEAU_DRUZE_COUNT = BUILDER
            .comment("Worldgen count attempts for essonite druze patches")
            .defineInRange("plateau_druze_count", 64, 0, 256);
    public static final ModConfigSpec.IntValue PLATEAU_LAKE_COUNT = BUILDER
            .comment("Worldgen count attempts for Φ-water cave lakes")
            .defineInRange("plateau_lake_count", 10, 0, 64);

    // --- Phi environment (factors around 1.0; PhiFieldService adds (factor - 1)) ---
    public static final ModConfigSpec.DoubleValue PHI_RAIN_MULTIPLIER = BUILDER
            .comment("Rain Φ factor around 1.0 (additive as value-1)")
            .defineInRange("phi_rain_multiplier", 1.08, 0.5, 2.0);

    public static final ModConfigSpec.DoubleValue PHI_THUNDER_MULTIPLIER = BUILDER
            .comment("Thunder Φ factor around 1.0 (additive as value-1)")
            .defineInRange("phi_thunder_multiplier", 1.18, 0.5, 2.5);

    public static final ModConfigSpec.DoubleValue PHI_UNDERGROUND_MULTIPLIER = BUILDER
            .comment("Underground / no-sky Φ factor around 1.0 (additive as value-1)")
            .defineInRange("phi_underground_multiplier", 0.82, 0.1, 1.5);

    public static final ModConfigSpec.DoubleValue PHI_OPEN_SKY_BONUS = BUILDER
            .comment("Open-sky Φ factor around 1.0 (additive as value-1)")
            .defineInRange("phi_open_sky_bonus", 1.05, 0.5, 2.0);

    public static final ModConfigSpec.DoubleValue PHI_UNDERWATER_MULTIPLIER = BUILDER
            .comment("Fully submerged in water Φ factor around 1.0 (additive as value-1)")
            .defineInRange("phi_underwater_multiplier", 0.72, 0.1, 1.5);

    public static final ModConfigSpec.DoubleValue PHI_IN_WATER_MULTIPLIER = BUILDER
            .comment("Standing/swimming in water (not fully submerged) Φ factor around 1.0")
            .defineInRange("phi_in_water_multiplier", 0.88, 0.1, 1.5);

    // --- Necro thrall control (Death Mark army limits by breathing mastery) ---
    public static final ModConfigSpec.DoubleValue NECRO_CONTROL_BUDGET_BASE = BUILDER
            .comment("Total thrall HP budget at breathing mastery 0")
            .defineInRange("necro_control_budget_base", 24.0, 5.0, 500.0);

    public static final ModConfigSpec.DoubleValue NECRO_CONTROL_BUDGET_PER_MASTERY = BUILDER
            .comment("Extra thrall HP budget at breathing mastery 1.0")
            .defineInRange("necro_control_budget_per_mastery", 300.0, 0.0, 1000.0);

    public static final ModConfigSpec.DoubleValue NECRO_MAX_SINGLE_HP_BASE = BUILDER
            .comment("Strongest single thrall HP at mastery 0")
            .defineInRange("necro_max_single_hp_base", 22.0, 5.0, 300.0);

    public static final ModConfigSpec.DoubleValue NECRO_MAX_SINGLE_HP_PER_MASTERY = BUILDER
            .comment("Extra max single thrall HP at mastery 1.0 (base+this ≈ 300)")
            .defineInRange("necro_max_single_hp_per_mastery", 278.0, 0.0, 1000.0);

    public static final ModConfigSpec.IntValue NECRO_MAX_THRALLS_BASE = BUILDER
            .comment("Max thrall count at mastery 0")
            .defineInRange("necro_max_thralls_base", 1, 1, 10);

    public static final ModConfigSpec.DoubleValue NECRO_MAX_THRALLS_PER_MASTERY = BUILDER
            .comment("Extra thrall slots gained by mastery 1.0 (floored)")
            .defineInRange("necro_max_thralls_per_mastery", 4.0, 0.0, 20.0);

    // --- Exhaustion ---
    public static final ModConfigSpec.DoubleValue EXHAUSTION_WARM = BUILDER
            .comment("Exhaustion level for Tired band")
            .defineInRange("exhaustion_warm", 25.0, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue EXHAUSTION_TIRED = BUILDER
            .comment("Exhaustion level for Strained band")
            .defineInRange("exhaustion_tired", 50.0, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue EXHAUSTION_STRAINED = BUILDER
            .comment("Exhaustion level for Collapsing band")
            .defineInRange("exhaustion_strained", 75.0, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue EXHAUSTION_GAIN_PER_COST = BUILDER
            .comment("Exhaustion gained per Ψ spent on cast")
            .defineInRange("exhaustion_gain_per_cost", 0.35, 0.0, 5.0);

    public static final ModConfigSpec.DoubleValue EXHAUSTION_GAIN_PER_ENTROPY = BUILDER
            .comment("Exhaustion gained per spell side_entropy ratio")
            .defineInRange("exhaustion_gain_per_entropy", 8.0, 0.0, 50.0);

    public static final ModConfigSpec.DoubleValue EXHAUSTION_LOW_PSI_BONUS = BUILDER
            .comment("Extra exhaustion when casting below 20% Ψ")
            .defineInRange("exhaustion_low_psi_bonus", 6.0, 0.0, 50.0);

    public static final ModConfigSpec.DoubleValue EXHAUSTION_BACKLASH_SPIKE = BUILDER
            .comment("Exhaustion added on entropy backlash")
            .defineInRange("exhaustion_backlash_spike", 25.0, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue EXHAUSTION_DECAY_PER_TICK = BUILDER
            .comment("Exhaustion recovered every 10 server ticks")
            .defineInRange("exhaustion_decay_per_tick", 0.8, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue EXHAUSTION_MEDITATION_DECAY_BONUS = BUILDER
            .comment("Extra exhaustion decay while meditating with full breath")
            .defineInRange("exhaustion_meditation_decay_bonus", 0.6, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue EXHAUSTION_REGEN_TIRED = BUILDER
            .defineInRange("exhaustion_regen_tired", 0.85, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue EXHAUSTION_REGEN_STRAINED = BUILDER
            .defineInRange("exhaustion_regen_strained", 0.6, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue EXHAUSTION_REGEN_COLLAPSING = BUILDER
            .defineInRange("exhaustion_regen_collapsing", 0.25, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue EXHAUSTION_COST_TIRED = BUILDER
            .defineInRange("exhaustion_cost_tired", 1.1, 1.0, 3.0);

    public static final ModConfigSpec.DoubleValue EXHAUSTION_COST_STRAINED = BUILDER
            .defineInRange("exhaustion_cost_strained", 1.25, 1.0, 3.0);

    public static final ModConfigSpec.DoubleValue EXHAUSTION_COST_COLLAPSING = BUILDER
            .defineInRange("exhaustion_cost_collapsing", 1.5, 1.0, 3.0);

    public static final ModConfigSpec.DoubleValue EXHAUSTION_COLLAPSE_CAST_DAMAGE = BUILDER
            .comment("Deprecated — casts no longer deal HP damage while collapsing. Kept for config compatibility.")
            .defineInRange("exhaustion_collapse_cast_damage", 0.0, 0.0, 20.0);

    // --- Overcast (casting when cost > usable Ψ) ---
    public static final ModConfigSpec.IntValue OVERCAST_DURATION_BASE = BUILDER
            .comment("Base overcast trauma duration in ticks")
            .defineInRange("overcast_duration_base", 200, 40, 6000);

    public static final ModConfigSpec.DoubleValue OVERCAST_DURATION_PER_SEVERITY = BUILDER
            .comment("Extra trauma ticks at full overcast severity (deficit/cost = 1)")
            .defineInRange("overcast_duration_per_severity", 500.0, 0.0, 6000.0);

    public static final ModConfigSpec.DoubleValue OVERCAST_EXHAUSTION_BASE = BUILDER
            .comment("Exhaustion spiked on any overcast")
            .defineInRange("overcast_exhaustion_base", 45.0, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue OVERCAST_EXHAUSTION_PER_SEVERITY = BUILDER
            .comment("Extra exhaustion at full overcast severity")
            .defineInRange("overcast_exhaustion_per_severity", 50.0, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue OVERCAST_DAMAGE = BUILDER
            .comment("Deprecated — overcast no longer deals HP damage (channel trauma only). Kept for config compatibility.")
            .defineInRange("overcast_damage", 0.0, 0.0, 20.0);

    public static final ModConfigSpec.DoubleValue OVERCAST_ENTROPY_BUMP = BUILDER
            .comment("Entropy added at full overcast severity")
            .defineInRange("overcast_entropy_bump", 0.35, 0.0, 5.0);

    public static final ModConfigSpec.DoubleValue OVERCAST_REGEN_MIN = BUILDER
            .comment("Ψ regen multiplier at full overcast severity")
            .defineInRange("overcast_regen_min", 0.02, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue OVERCAST_REGEN_MAX = BUILDER
            .comment("Ψ regen multiplier at mild overcast severity")
            .defineInRange("overcast_regen_max", 0.35, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue OVERCAST_BREATH_MIN = BUILDER
            .comment("Effective breathing mastery factor at full overcast severity")
            .defineInRange("overcast_breath_min", 0.1, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue OVERCAST_BREATH_MAX = BUILDER
            .comment("Effective breathing mastery factor at mild overcast severity")
            .defineInRange("overcast_breath_max", 0.55, 0.0, 1.0);

    public static final ModConfigSpec.IntValue CAST_CHARGE_MS = BUILDER
            .comment("Milliseconds of holding the cast key to reach full charge")
            .defineInRange("cast_charge_ms", 900, 200, 4000);

    public static final ModConfigSpec.DoubleValue CAST_CHARGE_MIN_POWER = BUILDER
            .comment("Power/cost multiplier at a tap (0 hold); ramps to 1.0 at full charge")
            .defineInRange("cast_charge_min_power", 0.45, 0.15, 1.0);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
