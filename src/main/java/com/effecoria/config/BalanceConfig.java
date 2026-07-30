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
            .comment("Minimum spell cost as fraction of base_cost once breathing mastery is fully past unlock")
            .defineInRange("spell_cost_floor_ratio", 0.33, 0.1, 1.0);

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
            .comment("Φ multiplier while the sun is above the horizon (solar flux)")
            .defineInRange("phi_day_multiplier", 1.1, 0.0, 5.0);

    public static final ModConfigSpec.DoubleValue PHI_NIGHT_MULTIPLIER = BUILDER
            .comment("Φ multiplier at night — less stellar flux reaches the surface")
            .defineInRange("phi_night_multiplier", 0.5, 0.0, 5.0);

    public static final ModConfigSpec.DoubleValue BREATHING_MAX_MASTERY = BUILDER
            .comment("Maximum breathing technique mastery (1.0 = fully mastered)")
            .defineInRange("breathing_max_mastery", 1.0, 0.1, 10.0);

    public static final ModConfigSpec.DoubleValue BREATHING_MEDITATION_GAIN = BUILDER
            .comment("Deprecated — meditation breathing gains removed; kept for config compatibility")
            .defineInRange("breathing_meditation_gain", 0.002, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue BREATHING_TRAIN_REGEN_BONUS = BUILDER
            .comment("Permanent Ψ regen bonus per successful breathing mini-game hit (0.001 = +0.1%)")
            .defineInRange("breathing_train_regen_bonus", 0.001, 0.0, 0.1);

    public static final ModConfigSpec.DoubleValue BREATHING_TRAIN_MASTERY_GAIN = BUILDER
            .comment("Breathing mastery gained per successful mini-game hit")
            .defineInRange("breathing_train_mastery_gain", 0.01, 0.0, 1.0);

    public static final ModConfigSpec.IntValue BREATHING_TRAIN_MISS_LIMIT = BUILDER
            .comment("Failed timing clicks before breathing-train fatigue applies")
            .defineInRange("breathing_train_miss_limit", 3, 1, 20);

    public static final ModConfigSpec.IntValue BREATHING_TRAIN_FATIGUE_MS = BUILDER
            .comment("Real-time fatigue after miss limit (ms) — blocks retraining and cuts regen")
            .defineInRange("breathing_train_fatigue_ms", 300_000, 0, 3_600_000);

    public static final ModConfigSpec.DoubleValue BREATHING_TRAIN_FATIGUE_REGEN_MULT = BUILDER
            .comment("Ψ regen multiplier while breathing-train fatigue is active")
            .defineInRange("breathing_train_fatigue_regen_mult", 0.5, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue BREATHING_SCROLL_GAIN = BUILDER
            .comment("Mastery granted by one breathing technique scroll")
            .defineInRange("breathing_scroll_gain", 0.08, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue BREATHING_BIOLOGY_BONUS_MAX = BUILDER
            .comment("Max Orkanum (biologyQ) multiplier bonus at full breathing mastery — e.g. 0.3 = +30% regen")
            .defineInRange("breathing_biology_bonus_max", 0.30, 0.0, 2.0);

    public static final ModConfigSpec.DoubleValue TRAINING_XP_SPRINT = BUILDER
            .comment("Training XP gained per progression tick while sprinting on ground")
            .defineInRange("training_xp_sprint", 0.08, 0.0, 1000.0);

    public static final ModConfigSpec.DoubleValue TRAINING_XP_SWIM = BUILDER
            .comment("Training XP gained per progression tick while swimming")
            .defineInRange("training_xp_swim", 0.05, 0.0, 1000.0);

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
            .defineInRange("training_xp_breath_train", 3.5, 0.0, 1000.0);

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
            .comment("Max Ψ increase per training milestone")
            .defineInRange("training_max_psi_gain", 5.0, 0.0, 500.0);

    public static final ModConfigSpec.DoubleValue TRAINING_MAX_PSI_CAP = BUILDER
            .comment("Maximum Ψ capacity from training")
            .defineInRange("training_max_psi_cap", 150.0, 10.0, 10000.0);

    public static final ModConfigSpec.DoubleValue WHIFF_COST_FRACTION = BUILDER
            .comment("Ψ spent when a targeted spell finds no valid target (fraction of full cost)")
            .defineInRange("whiff_cost_fraction", 0.25, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue MASTERY_BREATHING_MAX = BUILDER
            .comment("Spell mastery bonus from breathing at full mastery (linear below cap)")
            .defineInRange("mastery_breathing_max", 0.10, 0.0, 1.0);

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
            .defineInRange("spell_starter_count", 5, 1, 20);

    public static final ModConfigSpec.IntValue SPELL_UNLOCK_ESSENCE_STEP = BUILDER
            .comment("Default essence cost grows by this amount per progression tier after starters (when JSON omits unlock_essence_cost)")
            .defineInRange("spell_unlock_essence_step", 2, 0, 50);

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

    // --- Phi environment ---
    public static final ModConfigSpec.DoubleValue PHI_RAIN_MULTIPLIER = BUILDER
            .comment("Φ multiplier while it is raining (overworld surface)")
            .defineInRange("phi_rain_multiplier", 1.08, 0.5, 2.0);

    public static final ModConfigSpec.DoubleValue PHI_THUNDER_MULTIPLIER = BUILDER
            .comment("Φ multiplier during thunderstorms")
            .defineInRange("phi_thunder_multiplier", 1.18, 0.5, 2.5);

    public static final ModConfigSpec.DoubleValue PHI_UNDERGROUND_MULTIPLIER = BUILDER
            .comment("Φ multiplier when sky is not visible (caves)")
            .defineInRange("phi_underground_multiplier", 0.82, 0.1, 1.5);

    public static final ModConfigSpec.DoubleValue PHI_OPEN_SKY_BONUS = BUILDER
            .comment("Extra Φ multiplier when sky is visible on surface")
            .defineInRange("phi_open_sky_bonus", 1.05, 0.5, 2.0);

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
            .comment("Magic damage taken on each cast while Collapsing")
            .defineInRange("exhaustion_collapse_cast_damage", 2.0, 0.0, 20.0);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
