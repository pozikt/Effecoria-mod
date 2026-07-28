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

    public static final ModConfigSpec SPEC = BUILDER.build();
}
