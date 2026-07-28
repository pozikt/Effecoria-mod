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

    public static final ModConfigSpec.BooleanValue CREATIVE_GOD_MODE = BUILDER
            .comment("In creative mode: infinite Φ, free casts, full Ψ, no backlash (for testing)")
            .define("creative_god_mode", true);

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
            .comment("Mastery gained per progression tick while meditating (standing calm)")
            .defineInRange("breathing_meditation_gain", 0.002, 0.0, 1.0);

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

    public static final ModConfigSpec SPEC = BUILDER.build();
}
