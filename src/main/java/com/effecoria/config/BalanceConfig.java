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

    // --- Race Orkanum baselines ---
    public static final ModConfigSpec.DoubleValue RACE_BASELINE_HUMAN = BUILDER
            .comment("Orkanum baseline — Human")
            .defineInRange("race_baseline_human", 0.60, 0.05, 2.0);
    public static final ModConfigSpec.DoubleValue RACE_BASELINE_ORC = BUILDER
            .comment("Orkanum baseline — Orc")
            .defineInRange("race_baseline_orc", 0.85, 0.05, 2.0);
    public static final ModConfigSpec.DoubleValue RACE_BASELINE_ELF = BUILDER
            .comment("Orkanum baseline — Elf")
            .defineInRange("race_baseline_elf", 0.75, 0.05, 2.0);
    public static final ModConfigSpec.DoubleValue RACE_BASELINE_DWARF = BUILDER
            .comment("Orkanum baseline — Dwarf")
            .defineInRange("race_baseline_dwarf", 0.70, 0.05, 2.0);
    public static final ModConfigSpec.DoubleValue RACE_BASELINE_VARANAGI = BUILDER
            .comment("Orkanum baseline — Varanagi")
            .defineInRange("race_baseline_varanagi", 0.90, 0.05, 2.0);
    public static final ModConfigSpec.DoubleValue RACE_BASELINE_DRYAD = BUILDER
            .comment("Orkanum baseline — Dryad")
            .defineInRange("race_baseline_dryad", 0.95, 0.05, 2.0);
    public static final ModConfigSpec.DoubleValue RACE_BASELINE_LONVER = BUILDER
            .comment("Orkanum baseline — Lonver")
            .defineInRange("race_baseline_lonver", 1.05, 0.05, 2.0);
    public static final ModConfigSpec.DoubleValue RACE_BASELINE_HARPY = BUILDER
            .comment("Orkanum baseline — Harpy")
            .defineInRange("race_baseline_harpy", 0.70, 0.05, 2.0);
    public static final ModConfigSpec.DoubleValue RACE_BASELINE_VAMPIRE = BUILDER
            .comment("Orkanum baseline — Vampire")
            .defineInRange("race_baseline_vampire", 0.35, 0.05, 2.0);

    public static final ModConfigSpec.DoubleValue RACE_LONVER_MAX_PSI_BONUS = BUILDER
            .comment("Extra max Ψ granted to Lonvers on race assign")
            .defineInRange("race_lonver_max_psi_bonus", 15.0, 0.0, 100.0);

    // --- Harpy flight ---
    public static final ModConfigSpec.DoubleValue HARPY_MIN_SPEED = BUILDER
            .comment("Min horizontal speed (blocks/tick) while sprinting to count wind-up jumps")
            .defineInRange("harpy_min_speed", 0.18, 0.05, 1.0);
    public static final ModConfigSpec.IntValue HARPY_JUMP_WINDOW_TICKS = BUILDER
            .comment("Max ticks between wind-up jumps before streak resets")
            .defineInRange("harpy_jump_window_ticks", 35, 10, 100);
    public static final ModConfigSpec.DoubleValue HARPY_LAUNCH_FORWARD = BUILDER
            .comment("Forward launch speed on 3rd jump")
            .defineInRange("harpy_launch_forward", 0.95, 0.2, 2.5);
    public static final ModConfigSpec.DoubleValue HARPY_LAUNCH_UP = BUILDER
            .comment("Upward launch speed on 3rd jump")
            .defineInRange("harpy_launch_up", 0.62, 0.2, 2.0);
    public static final ModConfigSpec.DoubleValue HARPY_FLAP_STRENGTH = BUILDER
            .comment("Firework-style boost strength when flapping (space) in glide")
            .defineInRange("harpy_flap_strength", 1.0, 0.3, 2.5);
    public static final ModConfigSpec.DoubleValue HARPY_GLIDE_EXHAUSTION = BUILDER
            .comment("Hunger exhaustion per tick while harpy-gliding")
            .defineInRange("harpy_glide_exhaustion", 0.005, 0.0, 0.1);
    public static final ModConfigSpec.DoubleValue HARPY_FLAP_EXHAUSTION = BUILDER
            .comment("Hunger exhaustion per space flap")
            .defineInRange("harpy_flap_exhaustion", 0.8, 0.0, 5.0);
    public static final ModConfigSpec.IntValue HARPY_FLAP_COOLDOWN_TICKS = BUILDER
            .comment("Minimum ticks between flaps")
            .defineInRange("harpy_flap_cooldown_ticks", 10, 2, 40);

    // --- Harpy claws (iron-spear-like) ---
    public static final ModConfigSpec.DoubleValue HARPY_CLAW_BASE_DAMAGE = BUILDER
            .comment("Innate ATTACK_DAMAGE add (fist ~1 + 2 ≈ iron spear jab 3)")
            .defineInRange("harpy_claw_base_damage", 2.0, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue HARPY_CLAW_SPEED_FACTOR = BUILDER
            .comment("Charge bonus = relative speed (blocks/s) × this (iron spear ≈ 0.95)")
            .defineInRange("harpy_claw_speed_factor", 0.95, 0.0, 3.0);
    public static final ModConfigSpec.DoubleValue HARPY_CLAW_MIN_SPEED_BPS = BUILDER
            .comment("Min relative speed (blocks/s) before speed bonus applies")
            .defineInRange("harpy_claw_min_speed_bps", 3.0, 0.0, 20.0);
    public static final ModConfigSpec.DoubleValue HARPY_CLAW_SPEED_BONUS_CAP = BUILDER
            .comment("Cap on speed bonus damage (0 = uncapped)")
            .defineInRange("harpy_claw_speed_bonus_cap", 40.0, 0.0, 100.0);
    public static final ModConfigSpec.DoubleValue HARPY_DIVE_MIN_SPEED = BUILDER
            .comment("Min player speed (blocks/tick) to deal glide dive claw hits")
            .defineInRange("harpy_dive_min_speed", 0.22, 0.05, 2.0);
    public static final ModConfigSpec.DoubleValue HARPY_DIVE_REACH = BUILDER
            .comment("Dive claw hit reach in front of the player")
            .defineInRange("harpy_dive_reach", 1.35, 0.5, 4.0);
    public static final ModConfigSpec.IntValue HARPY_DIVE_HIT_COOLDOWN_TICKS = BUILDER
            .comment("Per-target cooldown between dive claw hits")
            .defineInRange("harpy_dive_hit_cooldown_ticks", 10, 2, 40);
    public static final ModConfigSpec.DoubleValue HARPY_CLAW_DIVE_FLOOR = BUILDER
            .comment("Minimum dive hit damage once speed threshold is met")
            .defineInRange("harpy_claw_dive_floor", 3.0, 0.0, 20.0);

    // --- Varanagi climb (vine-like walls/trees + scramble dash) ---
    public static final ModConfigSpec.DoubleValue VARANAGI_CLIMB_SPEED = BUILDER
            .comment("Vertical speed while holding jump on a wall/tree (vine-like)")
            .defineInRange("varanagi_climb_speed", 0.18, 0.05, 0.6);
    public static final ModConfigSpec.DoubleValue VARANAGI_CLIMB_SLIDE = BUILDER
            .comment("Max downward slip while hanging on a wall without jump")
            .defineInRange("varanagi_climb_slide", 0.12, 0.0, 0.5);
    public static final ModConfigSpec.DoubleValue VARANAGI_CLIMB_DASH_UP = BUILDER
            .comment("Upward impulse for sprint+jump scramble dash on walls")
            .defineInRange("varanagi_climb_dash_up", 0.72, 0.2, 2.0);
    public static final ModConfigSpec.DoubleValue VARANAGI_CLIMB_DASH_ALONG = BUILDER
            .comment("Forward (look) impulse for scramble dash")
            .defineInRange("varanagi_climb_dash_along", 0.28, 0.0, 1.5);
    public static final ModConfigSpec.IntValue VARANAGI_CLIMB_DASH_COOLDOWN_TICKS = BUILDER
            .comment("Minimum ticks between scramble dashes")
            .defineInRange("varanagi_climb_dash_cooldown_ticks", 12, 4, 40);
    public static final ModConfigSpec.DoubleValue VARANAGI_CLIMB_DASH_EXHAUSTION = BUILDER
            .comment("Hunger exhaustion per scramble dash")
            .defineInRange("varanagi_climb_dash_exhaustion", 0.45, 0.0, 5.0);
    public static final ModConfigSpec.IntValue VARANAGI_CLIMB_FALL_GRACE_TICKS = BUILDER
            .comment("Ticks after leaving a wall that still cancel fall distance")
            .defineInRange("varanagi_climb_fall_grace_ticks", 8, 0, 40);

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

    // --- Vitrified Wastes (residual Φ flash) ---
    public static final ModConfigSpec.DoubleValue VITRIFIED_PHI_BONUS = BUILDER
            .comment("Additive Φ bonus while in Vitrified Wastes")
            .defineInRange("vitrified_phi_bonus", 0.45, 0.0, 3.0);

    public static final ModConfigSpec.DoubleValue VITRIFIED_STORM_PHI_BONUS = BUILDER
            .comment("Extra additive Φ during a Φ-storm in Vitrified Wastes")
            .defineInRange("vitrified_storm_phi_bonus", 0.55, 0.0, 3.0);

    public static final ModConfigSpec.DoubleValue VITRIFIED_RADIATION_DAMAGE = BUILDER
            .comment("Magic damage per second without Φ-protection in Vitrified Wastes")
            .defineInRange("vitrified_radiation_damage", 0.5, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue VITRIFIED_STORM_DAMAGE = BUILDER
            .comment("Extra magic damage per second during Φ-storm without protection")
            .defineInRange("vitrified_storm_damage", 1.5, 0.0, 20.0);

    public static final ModConfigSpec.DoubleValue VITRIFIED_STORM_CHANCE_PER_SECOND = BUILDER
            .comment("Chance per second (while a player is inside) to start a Φ-storm")
            .defineInRange("vitrified_storm_chance_per_second", 0.00015, 0.0, 0.05);

    public static final ModConfigSpec.IntValue VITRIFIED_STORM_DURATION_TICKS = BUILDER
            .comment("Φ-storm duration in ticks")
            .defineInRange("vitrified_storm_duration_ticks", 2400, 200, 72000);

    public static final ModConfigSpec.DoubleValue VITRIFIED_PROTECTED_PSI_REGEN = BUILDER
            .comment("Ψ restored per second for protected initiated mages in Vitrified Wastes")
            .defineInRange("vitrified_protected_psi_regen", 0.4, 0.0, 10.0);

    // --- Crystal Forest ---
    public static final ModConfigSpec.DoubleValue CRYSTAL_FOREST_PHI_BONUS = BUILDER
            .comment("Additive Φ bonus while in Crystal Forest")
            .defineInRange("crystal_forest_phi_bonus", 0.4, 0.0, 3.0);

    // --- Emerald Canopy ---
    public static final ModConfigSpec.DoubleValue EMERALD_CANOPY_PHI_BONUS = BUILDER
            .comment("Additive Φ bonus while in Emerald Canopy")
            .defineInRange("emerald_canopy_phi_bonus", 0.65, 0.0, 3.0);
    public static final ModConfigSpec.DoubleValue EMERALD_CANOPY_GRAVITY_MULT = BUILDER
            .comment("Gravity multiplier in Emerald Canopy (0.8 = 80% of normal)")
            .defineInRange("emerald_canopy_gravity_mult", 0.8, 0.2, 1.5);
    public static final ModConfigSpec.IntValue EMERALD_CANOPY_TREE_MIN_HEIGHT = BUILDER
            .comment("Minimum giant canopy trunk height")
            .defineInRange("emerald_canopy_tree_min_height", 40, 20, 100);
    public static final ModConfigSpec.IntValue EMERALD_CANOPY_TREE_MAX_HEIGHT = BUILDER
            .comment("Maximum typical canopy trunk height")
            .defineInRange("emerald_canopy_tree_max_height", 64, 30, 120);
    public static final ModConfigSpec.IntValue EMERALD_CANOPY_EMERGENT_MAX_HEIGHT = BUILDER
            .comment("Rare emergent tree max height")
            .defineInRange("emerald_canopy_emergent_max_height", 88, 50, 160);
    public static final ModConfigSpec.DoubleValue EMERALD_CANOPY_EMERGENT_CHANCE = BUILDER
            .comment("Chance a giant tree is an emergent")
            .defineInRange("emerald_canopy_emergent_chance", 0.12, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue EMERALD_CANOPY_MIND_ANGER_DECAY = BUILDER
            .comment("Forest Mind anger lost per second while idle")
            .defineInRange("emerald_canopy_mind_anger_decay", 0.35, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue EMERALD_CANOPY_MIND_WARN_THRESHOLD = BUILDER
            .comment("Anger before mental noise / warnings")
            .defineInRange("emerald_canopy_mind_warn_threshold", 8.0, 1.0, 100.0);
    public static final ModConfigSpec.DoubleValue EMERALD_CANOPY_MIND_HOSTILE_THRESHOLD = BUILDER
            .comment("Anger before predator bias / ent aggro")
            .defineInRange("emerald_canopy_mind_hostile_threshold", 22.0, 5.0, 200.0);

    // --- Ω-Scar ---
    public static final ModConfigSpec.DoubleValue OMEGA_SCAR_PHI_BONUS = BUILDER
            .comment("Additive Φ bonus while in Ω-Scar (distorted residual field; keep low)")
            .defineInRange("omega_scar_phi_bonus", 0.12, -2.0, 3.0);

    public static final ModConfigSpec.DoubleValue OMEGA_SCAR_ENTROPY_PULSE = BUILDER
            .comment("Base entropy (b) pulse while exposed in Ω-Scar (favored schools take less)")
            .defineInRange("omega_scar_entropy_pulse", 0.055, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue OMEGA_SCAR_FAVORED_POWER = BUILDER
            .comment("Spell power multiplier for Necromancy/Corruption inside Ω-Scar")
            .defineInRange("omega_scar_favored_power", 1.4, 1.0, 3.0);

    public static final ModConfigSpec.DoubleValue OMEGA_SCAR_FAVORED_COST = BUILDER
            .comment("Spell Ψ cost multiplier for Necromancy/Corruption inside Ω-Scar")
            .defineInRange("omega_scar_favored_cost", 0.8, 0.1, 1.0);

    public static final ModConfigSpec.DoubleValue OMEGA_SCAR_OTHER_POWER = BUILDER
            .comment("Spell power multiplier for other schools inside Ω-Scar")
            .defineInRange("omega_scar_other_power", 0.82, 0.2, 1.0);

    public static final ModConfigSpec.DoubleValue OMEGA_SCAR_CAST_CHAOS = BUILDER
            .comment("Chance per cast that Ω-noise corrupts a non-favored school spell")
            .defineInRange("omega_scar_cast_chaos", 0.14, 0.0, 1.0);

    public static final ModConfigSpec.IntValue OMEGA_SCAR_GRAVITY_PERIOD = BUILDER
            .comment("Ticks between gravity flips in Ω-Scar")
            .defineInRange("omega_scar_gravity_period", 80, 20, 600);

    public static final ModConfigSpec.DoubleValue OMEGA_SCAR_GRAVITY_LIGHT = BUILDER
            .comment("Light gravity multiplier during Scar instability")
            .defineInRange("omega_scar_gravity_light", 0.55, 0.2, 1.0);

    public static final ModConfigSpec.DoubleValue OMEGA_SCAR_GRAVITY_HEAVY = BUILDER
            .comment("Heavy gravity multiplier during Scar instability")
            .defineInRange("omega_scar_gravity_heavy", 1.45, 1.0, 2.5);

    public static final ModConfigSpec.DoubleValue OMEGA_SCAR_LOOP_CHANCE = BUILDER
            .comment("Chance every 5s (after long exposure) to snag a micro time-loop")
            .defineInRange("omega_scar_loop_chance", 0.04, 0.0, 0.5);

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

    // --- Whispering Spire (Minecraft-scaled zones) ---
    public static final ModConfigSpec.IntValue SPIRE_ZONE_BLACK = BUILDER
            .comment("Horizontal radius of Black (vent) zone")
            .defineInRange("spire_zone_black", 8, 2, 32);
    public static final ModConfigSpec.IntValue SPIRE_ZONE_RED = BUILDER
            .comment("Horizontal radius of Red (caldera) zone")
            .defineInRange("spire_zone_red", 24, 8, 64);
    public static final ModConfigSpec.IntValue SPIRE_ZONE_YELLOW = BUILDER
            .comment("Horizontal radius of Yellow (slopes) zone")
            .defineInRange("spire_zone_yellow", 48, 16, 128);
    public static final ModConfigSpec.IntValue SPIRE_ZONE_GREEN = BUILDER
            .comment("Horizontal radius of Green (foothills) zone")
            .defineInRange("spire_zone_green", 96, 32, 256);
    public static final ModConfigSpec.DoubleValue SPIRE_COLUMN_HEIGHT = BUILDER
            .comment("Φ-plasma column particle height above vent")
            .defineInRange("spire_column_height", 48.0, 8.0, 128.0);
    public static final ModConfigSpec.DoubleValue SPIRE_PHI_GREEN = BUILDER
            .comment("Φ environment bonus in Green zone")
            .defineInRange("spire_phi_green", 1.5, 0.0, 20.0);
    public static final ModConfigSpec.DoubleValue SPIRE_PHI_YELLOW = BUILDER
            .comment("Φ environment bonus in Yellow zone")
            .defineInRange("spire_phi_yellow", 4.0, 0.0, 40.0);
    public static final ModConfigSpec.DoubleValue SPIRE_PHI_RED = BUILDER
            .comment("Φ environment bonus in Red zone")
            .defineInRange("spire_phi_red", 12.0, 0.0, 80.0);
    public static final ModConfigSpec.DoubleValue SPIRE_PHI_BLACK = BUILDER
            .comment("Φ environment bonus in Black zone")
            .defineInRange("spire_phi_black", 40.0, 0.0, 200.0);
    public static final ModConfigSpec.DoubleValue SPIRE_DMG_YELLOW = BUILDER
            .comment("Magic DPS tick damage in Yellow (unprotected)")
            .defineInRange("spire_dmg_yellow", 1.0, 0.0, 20.0);
    public static final ModConfigSpec.DoubleValue SPIRE_DMG_RED = BUILDER
            .comment("Magic DPS tick damage in Red (unprotected)")
            .defineInRange("spire_dmg_red", 4.0, 0.0, 40.0);
    public static final ModConfigSpec.DoubleValue SPIRE_DMG_BLACK = BUILDER
            .comment("Magic DPS tick damage in Black zone")
            .defineInRange("spire_dmg_black", 12.0, 0.0, 80.0);

    public static final ModConfigSpec.DoubleValue MATTER_CAST_MIN_MASTERY = BUILDER
            .comment("Minimum breathing mastery to use environmental matter casting (Elemental)")
            .defineInRange("matter_cast_min_mastery", 0.15, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue MATTER_BOND_RANGE = BUILDER
            .comment("Max distance to maintain a matter bond")
            .defineInRange("matter_bond_range", 8.0, 2.0, 32.0);
    public static final ModConfigSpec.DoubleValue MATTER_THROW_PSI = BUILDER
            .comment("Ψ cost to throw a bonded matter form")
            .defineInRange("matter_throw_psi", 8.0, 0.0, 100.0);
    public static final ModConfigSpec.DoubleValue MATTER_THROW_SOURCE_DRAIN = BUILDER
            .comment("Bond strength drained per throw")
            .defineInRange("matter_throw_source_drain", 1.25, 0.0, 20.0);
    public static final ModConfigSpec.DoubleValue MATTER_CHANNEL_PSI_PER_TICK = BUILDER
            .comment("Ψ drained each tick while holding a matter barrier")
            .defineInRange("matter_channel_psi_per_tick", 0.15, 0.0, 10.0);

    // --- Essonite armor contour ---
    public static final ModConfigSpec.DoubleValue ESSONITE_ARMOR_REGEN_SCALE = BUILDER
            .comment("Global scale for armor Φ-charge regen from ambient Φ")
            .defineInRange("essonite_armor_regen_scale", 1.0, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue ESSONITE_ARMOR_PIEZO_SCALE = BUILDER
            .comment("Charge gained per point of damage taken (piezo-Φ)")
            .defineInRange("essonite_armor_piezo_scale", 0.04, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue ESSONITE_ARMOR_SUBSIDY_SCALE = BUILDER
            .comment("Multiplier on tier cast-subsidy fraction")
            .defineInRange("essonite_armor_subsidy_scale", 1.0, 0.0, 3.0);
    public static final ModConfigSpec.DoubleValue ESSONITE_ARMOR_POOL_PSI = BUILDER
            .comment("How many Ψ a full armor charge pool is worth for cast subsidy")
            .defineInRange("essonite_armor_pool_psi", 40.0, 1.0, 500.0);
    public static final ModConfigSpec.DoubleValue ESSONITE_ARMOR_REPAIR_MIN_CHARGE = BUILDER
            .comment("Minimum pool charge required for self-repair")
            .defineInRange("essonite_armor_repair_min_charge", 0.25, 0.0, 1.0);
    public static final ModConfigSpec.IntValue ESSONITE_ARMOR_REPAIR_PER_SECOND = BUILDER
            .comment("Durability points mended per second while charged")
            .defineInRange("essonite_armor_repair_per_second", 1, 0, 20);
    public static final ModConfigSpec.DoubleValue ESSONITE_ARMOR_SERVARE_HEAL = BUILDER
            .comment("Hearts healed per second from Servare phoneme")
            .defineInRange("essonite_armor_servare_heal", 0.25, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue ESSONITE_ARMOR_VISION_RANGE = BUILDER
            .comment("Helmet Φ-vision glow radius")
            .defineInRange("essonite_armor_vision_range", 24.0, 4.0, 64.0);
    public static final ModConfigSpec.DoubleValue ESSONITE_ARMOR_FLASH_RADIUS = BUILDER
            .comment("Φ-flash knockback radius")
            .defineInRange("essonite_armor_flash_radius", 6.0, 1.0, 24.0);
    public static final ModConfigSpec.DoubleValue ESSONITE_ARMOR_FLASH_KNOCKBACK = BUILDER
            .comment("Φ-flash knockback strength")
            .defineInRange("essonite_armor_flash_knockback", 1.35, 0.1, 5.0);
    public static final ModConfigSpec.IntValue ESSONITE_ARMOR_SKIN_TICKS = BUILDER
            .comment("Crystal Skin duration in ticks")
            .defineInRange("essonite_armor_skin_ticks", 80, 20, 400);
    public static final ModConfigSpec.IntValue ESSONITE_ARMOR_WINGS_TICKS = BUILDER
            .comment("Essence Wings duration in ticks")
            .defineInRange("essonite_armor_wings_ticks", 100, 20, 600);
    public static final ModConfigSpec.DoubleValue ESSONITE_ARMOR_WINGS_DRAIN = BUILDER
            .comment("Ψ drain per tick while Essence Wings flight is active")
            .defineInRange("essonite_armor_wings_drain", 0.08, 0.0, 5.0);
    public static final ModConfigSpec.DoubleValue ESSONITE_ARMOR_WINGS_BOOST = BUILDER
            .comment("Initial boost strength for Essence Wings")
            .defineInRange("essonite_armor_wings_boost", 1.1, 0.2, 4.0);
    public static final ModConfigSpec.IntValue ESSONITE_ARMOR_OMEGA_TICKS = BUILDER
            .comment("Ω-block duration in ticks")
            .defineInRange("essonite_armor_omega_ticks", 160, 20, 600);
    public static final ModConfigSpec.IntValue ESSONITE_ARMOR_OMEGA_INSERTS_CRYSTAL = BUILDER
            .comment("Void obsidian inserts consumed for Ω-block on crystal chest")
            .defineInRange("essonite_armor_omega_inserts_crystal", 3, 1, 16);
    public static final ModConfigSpec.IntValue ESSONITE_ARMOR_OMEGA_INSERTS_STAR = BUILDER
            .comment("Void obsidian inserts consumed for Ω-block on star chest")
            .defineInRange("essonite_armor_omega_inserts_star", 1, 1, 16);
    public static final ModConfigSpec.IntValue ESSONITE_ARMOR_ABILITY_COOLDOWN_TICKS = BUILDER
            .comment("Shared cooldown after any armor ability")
            .defineInRange("essonite_armor_ability_cooldown_ticks", 60, 0, 600);
    public static final ModConfigSpec.DoubleValue ESSONITE_ARMOR_FIRE_REDUCTION = BUILDER
            .comment("Fraction of fire damage negated while armor is charged")
            .defineInRange("essonite_armor_fire_reduction", 0.35, 0.0, 1.0);

    // --- Hyperspace essentialization (playable compressions of lore timescales) ---
    public static final ModConfigSpec.IntValue SUBSPACE_ESSENTIALIZE_INTERVAL_TICKS = BUILDER
            .comment("How often subspace essentialization scans (ticks)")
            .defineInRange("subspace_essentialize_interval_ticks", 40, 5, 1200);
    public static final ModConfigSpec.IntValue SUBSPACE_ORGANIC_TICKS = BUILDER
            .comment("Age before dirt/flesh-like organics glass into Φ analogues")
            .defineInRange("subspace_organic_ticks", 2400, 100, 720000);
    public static final ModConfigSpec.IntValue SUBSPACE_WOOD_TICKS = BUILDER
            .comment("Age before wood/leaves become Φ-wood / Φ-leaves")
            .defineInRange("subspace_wood_ticks", 4800, 200, 720000);
    public static final ModConfigSpec.IntValue SUBSPACE_STONE_TICKS = BUILDER
            .comment("Age before stone becomes Φ-stone")
            .defineInRange("subspace_stone_ticks", 12000, 400, 1440000);
    public static final ModConfigSpec.IntValue SUBSPACE_STONE_ORE_TICKS = BUILDER
            .comment("Extra age for Φ-stone / crust → essonite ore")
            .defineInRange("subspace_stone_ore_ticks", 24000, 800, 2880000);
    public static final ModConfigSpec.IntValue SUBSPACE_QUARTZ_TICKS = BUILDER
            .comment("Age before sand/quartz/glass become crust / Φ-glass")
            .defineInRange("subspace_quartz_ticks", 6000, 200, 720000);
    public static final ModConfigSpec.IntValue SUBSPACE_WATER_TICKS = BUILDER
            .comment("Age before Φ-water crystallizes to blue-ice Φ-hydrate (vanilla water activates instantly)")
            .defineInRange("subspace_water_ticks", 7200, 100, 720000);
    public static final ModConfigSpec.DoubleValue SUBSPACE_EXPOSURE_DAMAGE = BUILDER
            .comment("Magic damage per second to unprotected entities in hyperspace")
            .defineInRange("subspace_exposure_damage", 1.0, 0.0, 40.0);
    public static final ModConfigSpec.DoubleValue SUBSPACE_EXPOSURE_EXHAUSTION = BUILDER
            .comment("Orkanum exhaustion per second while unprotected in hyperspace")
            .defineInRange("subspace_exposure_exhaustion", 4.0, 0.0, 50.0);
    public static final ModConfigSpec.IntValue SUBSPACE_PETRIFY_TICKS = BUILDER
            .comment("Unprotected exposure before glassing/petrify escalation (ticks)")
            .defineInRange("subspace_petrify_ticks", 2400, 100, 720000);
    public static final ModConfigSpec.DoubleValue SUBSPACE_PRESSURE_EXHAUSTION = BUILDER
            .comment("Mild Orkanum pressure every 30s while protected in hyperspace")
            .defineInRange("subspace_pressure_exhaustion", 1.5, 0.0, 30.0);
    public static final ModConfigSpec.IntValue SUBSPACE_IRON_TICKS = BUILDER
            .comment("Reserved age for future Φ-iron (iron currently stable)")
            .defineInRange("subspace_iron_ticks", 48000, 1000, 2880000);
    public static final ModConfigSpec.IntValue SUBSPACE_GOLD_SPIT_TICKS = BUILDER
            .comment("Age before gold is spit back to the overworld")
            .defineInRange("subspace_gold_spit_ticks", 6000, 200, 720000);
    public static final ModConfigSpec.IntValue SUBSPACE_OBSIDIAN_TICKS = BUILDER
            .comment("Age before obsidian becomes void obsidian")
            .defineInRange("subspace_obsidian_ticks", 9000, 200, 720000);
    public static final ModConfigSpec.DoubleValue SUBSPACE_ESSONITE_GROW_CHANCE = BUILDER
            .comment("Per growth pulse chance to sprout/upgrade essonite near a voyager")
            .defineInRange("subspace_essonite_grow_chance", 0.35, 0.0, 1.0);

    // --- Wild mob mages ---
    public static final ModConfigSpec.DoubleValue MOB_MAGIC_SPAWN_CHANCE = BUILDER
            .comment("Chance for a newly spawned hostile mob to initiate with a random magic school (0.10–0.15 recommended)")
            .defineInRange("mob_magic_spawn_chance", 0.12, 0.0, 1.0);

    // --- Φ/Ω weather ---
    public static final ModConfigSpec.DoubleValue PHI_WEATHER_RAIN_PSI_REGEN = BUILDER
            .comment("Ψ restored per second for initiated mages under Essence Rain")
            .defineInRange("phi_weather_rain_psi_regen", 2.0, 0.0, 50.0);
    public static final ModConfigSpec.DoubleValue PHI_WEATHER_MIST_PSI_REGEN = BUILDER
            .comment("Extra Ψ/s for initiated mages in Essence Mist (on top of fog regen mult)")
            .defineInRange("phi_weather_mist_psi_regen", 0.75, 0.0, 20.0);
    public static final ModConfigSpec.IntValue PHI_WEATHER_RAIN_INTOX_TICKS = BUILDER
            .comment("Non-mage exposure ticks under Essence Rain before intoxication")
            .defineInRange("phi_weather_rain_intox_ticks", 1200, 100, 72000);
    public static final ModConfigSpec.DoubleValue PHI_WEATHER_STORM_CAST_CHAOS = BUILDER
            .comment("Chance that casts fail / backlash during Essence Storm")
            .defineInRange("phi_weather_storm_cast_chaos", 0.5, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue PHI_WEATHER_STORM_PHI_BONUS = BUILDER
            .comment("Ambient Φ bonus while an Essence Storm is active")
            .defineInRange("phi_weather_storm_phi_bonus", 0.35, 0.0, 5.0);
    public static final ModConfigSpec.IntValue PHI_WEATHER_POST_STORM_TICKS = BUILDER
            .comment("Ritual window after Essence Storm ends")
            .defineInRange("phi_weather_post_storm_ticks", 6000, 200, 72000);
    public static final ModConfigSpec.DoubleValue PHI_WEATHER_POST_STORM_PHI = BUILDER
            .comment("Φ bonus during the post-storm ritual window")
            .defineInRange("phi_weather_post_storm_phi", 0.55, 0.0, 5.0);
    public static final ModConfigSpec.DoubleValue PHI_WEATHER_PLATEAU_STORM_CHANCE = BUILDER
            .comment("Per-second chance to start a plateau Essence Storm while thundering")
            .defineInRange("phi_weather_plateau_storm_chance", 0.002, 0.0, 1.0);
    public static final ModConfigSpec.IntValue PHI_WEATHER_PLATEAU_STORM_DURATION = BUILDER
            .comment("Plateau Essence Storm duration in ticks")
            .defineInRange("phi_weather_plateau_storm_duration", 2400, 200, 72000);
    public static final ModConfigSpec.DoubleValue PHI_WEATHER_LIGHTNING_CHANCE = BUILDER
            .comment("Per-second chance of an Essence Lightning strike during storm/thunder")
            .defineInRange("phi_weather_lightning_chance", 0.08, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue PHI_WEATHER_LIGHTNING_DAMAGE = BUILDER
            .comment("Magic damage from Essence Lightning to unprotected entities")
            .defineInRange("phi_weather_lightning_damage", 8.0, 0.0, 40.0);
    public static final ModConfigSpec.DoubleValue PHI_WEATHER_LIGHTNING_VITRIFY_CHANCE = BUILDER
            .comment("Chance lightning vitrifies sand into Φ-glass")
            .defineInRange("phi_weather_lightning_vitrify_chance", 0.35, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue PHI_WEATHER_LIGHTNING_ORE_CHANCE = BUILDER
            .comment("Chance lightning turns stone into essonite ore")
            .defineInRange("phi_weather_lightning_ore_chance", 0.08, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue PHI_WEATHER_TORNADO_CHANCE = BUILDER
            .comment("Per-second chance to spawn a local Essence Tornado during storm")
            .defineInRange("phi_weather_tornado_chance", 0.015, 0.0, 1.0);
    public static final ModConfigSpec.IntValue PHI_WEATHER_TORNADO_DURATION = BUILDER
            .comment("Essence Tornado duration in ticks")
            .defineInRange("phi_weather_tornado_duration", 200, 40, 6000);
    public static final ModConfigSpec.DoubleValue PHI_WEATHER_TORNADO_RADIUS = BUILDER
            .comment("Essence Tornado radius")
            .defineInRange("phi_weather_tornado_radius", 10.0, 2.0, 48.0);
    public static final ModConfigSpec.DoubleValue PHI_WEATHER_TORNADO_PULL = BUILDER
            .comment("Horizontal pull strength toward tornado core each tick")
            .defineInRange("phi_weather_tornado_pull", 0.12, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue PHI_WEATHER_OMEGA_ENTROPY = BUILDER
            .comment("Entropy added while exposed to Omega Fog (per 2s pulse)")
            .defineInRange("phi_weather_omega_entropy", 0.06, 0.0, 2.0);
    public static final ModConfigSpec.DoubleValue PHI_WEATHER_OMEGA_RAIN_DAMAGE = BUILDER
            .comment("Magic damage per second under Omega Rain when unprotected")
            .defineInRange("phi_weather_omega_rain_damage", 2.5, 0.0, 40.0);
    public static final ModConfigSpec.DoubleValue PHI_WEATHER_BLOOD_CHANCE = BUILDER
            .comment("Chance to start Blood Rain when necro/high-entropy mages stand in Spire RED/BLACK at night")
            .defineInRange("phi_weather_blood_chance", 0.04, 0.0, 1.0);
    public static final ModConfigSpec.IntValue PHI_WEATHER_BLOOD_DURATION = BUILDER
            .comment("Blood Rain duration in ticks")
            .defineInRange("phi_weather_blood_duration", 1800, 200, 72000);
    public static final ModConfigSpec.DoubleValue PHI_WEATHER_BLOOD_RADIUS = BUILDER
            .comment("Blood Rain radius around Spire vent / player")
            .defineInRange("phi_weather_blood_radius", 48.0, 8.0, 128.0);
    public static final ModConfigSpec.DoubleValue PHI_WEATHER_BLOOD_ENTROPY_MIN = BUILDER
            .comment("Minimum entropyB to qualify for Blood Rain without necro school")
            .defineInRange("phi_weather_blood_entropy_min", 0.45, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue PHI_WEATHER_BLOOD_NECRO_POWER = BUILDER
            .comment("Necromancy spell power multiplier under Blood Rain")
            .defineInRange("phi_weather_blood_necro_power", 1.35, 1.0, 3.0);
    public static final ModConfigSpec.DoubleValue PHI_WEATHER_BLOOD_NECRO_COST = BUILDER
            .comment("Necromancy spell cost factor under Blood Rain")
            .defineInRange("phi_weather_blood_necro_cost", 0.75, 0.1, 1.0);
    public static final ModConfigSpec.IntValue PHI_WEATHER_DEW_WINDOW_TICKS = BUILDER
            .comment("Morning dayTime window (0..N) when Essence Dew is ambient on the plateau")
            .defineInRange("phi_weather_dew_window_ticks", 1000, 100, 6000);
    public static final ModConfigSpec.DoubleValue PHI_WEATHER_DEW_HARVEST_CHANCE = BUILDER
            .comment("Chance to harvest Essence Dew from Φ foliage in the morning")
            .defineInRange("phi_weather_dew_harvest_chance", 0.55, 0.0, 1.0);

    // --- Φ diseases ---
    public static final ModConfigSpec.DoubleValue DISEASE_SCAR_ORKANUM_MULT = BUILDER
            .comment("Orkanum multiplier after stage-3 Essence Burn scarring")
            .defineInRange("disease_scar_orkanumn_mult", 0.7, 0.1, 1.0);
    public static final ModConfigSpec.DoubleValue DISEASE_ATROPHY_STAGE1 = BUILDER
            .comment("Orkanum mult at Orkanum Atrophy stage 1")
            .defineInRange("disease_atrophy_stage1", 0.85, 0.05, 1.0);
    public static final ModConfigSpec.DoubleValue DISEASE_ATROPHY_STAGE2 = BUILDER
            .comment("Orkanum mult at Orkanum Atrophy stage 2")
            .defineInRange("disease_atrophy_stage2", 0.55, 0.05, 1.0);
    public static final ModConfigSpec.DoubleValue DISEASE_ATROPHY_STAGE3 = BUILDER
            .comment("Orkanum mult at Orkanum Atrophy stage 3 (near non-mage)")
            .defineInRange("disease_atrophy_stage3", 0.12, 0.01, 1.0);
    public static final ModConfigSpec.IntValue DISEASE_STAGE_TICKS = BUILDER
            .comment("Base disease ticks (at 10-tick cadence units) before stage may advance")
            .defineInRange("disease_stage_ticks", 2400, 200, 72000);
    public static final ModConfigSpec.IntValue DISEASE_RAD_BURN_TICKS = BUILDER
            .comment("High-radiation exposure ticks before Essence Burn")
            .defineInRange("disease_rad_burn_ticks", 1200, 100, 72000);
    public static final ModConfigSpec.IntValue DISEASE_RAD_CANCER_TICKS = BUILDER
            .comment("High-radiation exposure ticks before Essentocytosis")
            .defineInRange("disease_rad_cancer_ticks", 6000, 200, 144000);
    public static final ModConfigSpec.DoubleValue DISEASE_RAD_BURN_REMAIN = BUILDER
            .comment("PhiRadiation remaining factor required to count as high exposure")
            .defineInRange("disease_rad_burn_remain", 0.55, 0.1, 1.0);
    public static final ModConfigSpec.IntValue DISEASE_ATROPHY_TICKS = BUILDER
            .comment("Low-Φ exposure ticks before Orkanum Atrophy")
            .defineInRange("disease_atrophy_ticks", 6000, 200, 144000);
    public static final ModConfigSpec.IntValue DISEASE_DUST_TICKS = BUILDER
            .comment("Dust exposure points before Dust Lung")
            .defineInRange("disease_dust_ticks", 40, 5, 500);
    public static final ModConfigSpec.IntValue DISEASE_BURN_RECOVER_TICKS = BUILDER
            .comment("Shielded ticks before Essence Burn improves one stage")
            .defineInRange("disease_burn_recover_ticks", 1800, 100, 72000);
    public static final ModConfigSpec.IntValue DISEASE_FEVER_DURATION_TICKS = BUILDER
            .comment("Crystal Fever self-resolve duration (disease tick units)")
            .defineInRange("disease_fever_duration_ticks", 8400, 200, 72000);
    public static final ModConfigSpec.DoubleValue DISEASE_BACKLASH_BURN_CHANCE = BUILDER
            .comment("Chance to gain Essence Burn on entropy backlash")
            .defineInRange("disease_backlash_burn_chance", 0.55, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue DISEASE_OMEGA_SCAR_CHANCE = BUILDER
            .comment("Chance per 10s pulse in Ω-Scar to catch Omega Sickness")
            .defineInRange("disease_omega_scar_chance", 0.08, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue DISEASE_OMEGA_ENTROPY_RATIO = BUILDER
            .comment("Fraction of entropy threshold that risks Omega Sickness")
            .defineInRange("disease_omega_entropy_ratio", 0.75, 0.1, 1.0);
    public static final ModConfigSpec.DoubleValue DISEASE_CRAB_FEVER_CHANCE = BUILDER
            .comment("Chance Crystal Crab melee applies Crystal Fever")
            .defineInRange("disease_crab_fever_chance", 0.35, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue DISEASE_OMEGA_ROT_CHANCE = BUILDER
            .comment("Chance per check while Ω-wound active to escalate to Omega Rot")
            .defineInRange("disease_omega_rot_chance", 0.04, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue DISEASE_CURSE_ROT_CHANCE = BUILDER
            .comment("Chance when corruption curse is present to gain Curse Rot")
            .defineInRange("disease_curse_rot_chance", 0.06, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue DISEASE_GHOST_ECHO_CHANCE = BUILDER
            .comment("Chance on necro/subspace risk hooks to gain Ghost Echo")
            .defineInRange("disease_ghost_echo_chance", 0.12, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue DISEASE_DISSONANCE_CHANCE = BUILDER
            .comment("Chance on foreign-focus / mental conflict hooks to gain Soul Dissonance")
            .defineInRange("disease_dissonance_chance", 0.15, 0.0, 1.0);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
