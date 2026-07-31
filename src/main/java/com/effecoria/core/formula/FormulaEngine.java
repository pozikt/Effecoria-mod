package com.effecoria.core.formula;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.progression.ExhaustionService;
import com.effecoria.core.magic.SpellDefinition;

import net.minecraft.util.Mth;

import java.util.Optional;

/**
 * Single source of truth for Effecoria physics approximations.
 * Lore integrals are discretized per game tick; coefficients live in {@link BalanceConfig}.
 */
public final class FormulaEngine {
    private FormulaEngine() {}

    /**
     * ΔE_Ψ = Ψ_soul × Φ_local × Q_biology × Δt × regen_scale
     * Lich variant (Q=0) uses phylactery factor elsewhere — see {@link #regenPsiLich}.
     */
    public static float regenPsi(PsiContext ctx, PhiSample phi, float deltaTicks) {
        if (phi.zeroFlux() || ctx.biologyQ() <= 0f) {
            return 0f;
        }
        float scale = BalanceConfig.PSI_REGEN_SCALE.get().floatValue();
        return ctx.soulStrength()
                * phi.effectiveValue()
                * ctx.biologyQ()
                * deltaTicks
                * scale
                * ExhaustionService.regenMultiplier(ctx.exhaustion())
                * Math.max(0f, ctx.overcastRegenMult())
                * (1f + Math.max(0f, ctx.breathTrainRegenBonus()))
                * (ctx.breathTrainFatigue()
                        ? BalanceConfig.BREATHING_TRAIN_FATIGUE_REGEN_MULT.get().floatValue()
                        : 1f);
    }

    /**
     * E_Ψ^lich = Ψ_soul × Φ_local × Φ_phyl × Δt × regen_scale
     */
    public static float regenPsiLich(PsiContext ctx, PhiSample phi, float phylEfficiency, float deltaTicks) {
        if (phi.zeroFlux()) {
            return 0f;
        }
        float scale = BalanceConfig.PSI_REGEN_SCALE.get().floatValue();
        return ctx.soulStrength() * phi.effectiveValue() * phylEfficiency * deltaTicks * scale;
    }

    /**
     * power = Ψ_current × Φ_local × resonance × spell_multiplier
     */
    public static float spellPower(PsiContext ctx, PhiSample phi, SpellDefinition spell) {
        if (phi.zeroFlux()) {
            return 0f;
        }
        float resonance = resonance(ctx.frequencyHz(), spell.frequencyHz(), ctx.focusResonanceWidthBonus());
        return ctx.currentPsi()
                * phi.effectiveValue()
                * resonance
                * spell.powerMultiplier()
                * ctx.mastery()
                * BalanceConfig.SPELL_POWER_SCALE.get().floatValue();
    }

    /**
     * cost = base_cost × proficiency_discount × low_Φ_penalty × exhaustion
     * Proficiency drops from ~100% at unlock to ~33% at full breathing mastery.
     */
    public static float spellCost(PsiContext ctx, PhiSample phi, SpellDefinition spell) {
        float phiPenalty = 1f + BalanceConfig.LOW_PHI_COST_FACTOR.get().floatValue()
                * (1f - Math.min(1f, phi.effectiveValue()));
        return spell.baseCost()
                * proficiencyCostFactor(ctx.breathingMastery(), spell.minMastery(), ctx.focusCostFloor())
                * phiPenalty
                * ExhaustionService.costMultiplier(ctx.exhaustion());
    }

    /**
     * 1.0 when the spell is freshly unlocked; falls toward {@link BalanceConfig#SPELL_COST_FLOOR_RATIO}
     * by reference mastery (100%), then toward {@link BalanceConfig#SPELL_COST_ASCENSION_FLOOR} past that.
     */
    public static float proficiencyCostFactor(float breathingMastery, float unlockMastery) {
        return proficiencyCostFactor(breathingMastery, unlockMastery, 0f);
    }

    public static float proficiencyCostFactor(float breathingMastery, float unlockMastery, float focusCostFloor) {
        float anchor = Math.max(0f, unlockMastery);
        float ref = BalanceConfig.BREATHING_MAX_MASTERY.get().floatValue();
        float mortalFloor = focusCostFloor > 0f
                ? focusCostFloor
                : BalanceConfig.SPELL_COST_FLOOR_RATIO.get().floatValue();
        float ascensionFloor = Math.min(
                mortalFloor,
                BalanceConfig.SPELL_COST_ASCENSION_FLOOR.get().floatValue());

        if (breathingMastery <= anchor) {
            return 1f;
        }

        // Mortal band: unlock → 100% reference
        float mortalEnd = Math.max(anchor + 0.01f, ref);
        if (breathingMastery <= mortalEnd) {
            float progress = Mth.clamp((breathingMastery - anchor) / (mortalEnd - anchor), 0f, 1f);
            return Mth.lerp(progress, 1f, mortalFloor);
        }

        // Ascension: 100% → ~500% reaches ascension floor
        float past = (breathingMastery - mortalEnd) / Math.max(0.01f, ref);
        float t = Mth.clamp(past / 4f, 0f, 1f);
        return Mth.lerp(t, mortalFloor, ascensionFloor);
    }

    /**
     * Technomagic (phase 5): Effect ∝ Φ_reactor × C_circuit × K_rune
     */
    public static float technomagicPower(float phiReactor, float circuitQuality, float runePrecision) {
        return phiReactor * circuitQuality * runePrecision * BalanceConfig.TECHNO_POWER_SCALE.get().floatValue();
    }

    /**
     * b_accum += side_ratio × power_used
     */
    public static float accumulateEntropy(float currentB, float powerUsed, float sideRatio) {
        return currentB + sideRatio * powerUsed * BalanceConfig.ENTROPY_SCALE.get().floatValue();
    }

    public static boolean isBacklashTriggered(float entropyB) {
        return entropyB >= BalanceConfig.ENTROPY_THRESHOLD.get().floatValue();
    }

    public static boolean canCast(PsiContext ctx, PhiSample phi, SpellDefinition spell, float availablePsi) {
        return diagnoseCannotCast(ctx, phi, spell, availablePsi).isEmpty();
    }

    /**
     * Why {@link #canCast} fails — first matching reason for player-facing feedback.
     */
    public static Optional<CastBlockReason> diagnoseCannotCast(
            PsiContext ctx, PhiSample phi, SpellDefinition spell, float availablePsi) {
        if (phi.zeroFlux()) {
            return Optional.of(CastBlockReason.ZERO_FLUX);
        }
        if (spell.requiredSchool() != null && !ctx.hasAffinity(spell.requiredSchool())) {
            return Optional.of(CastBlockReason.WRONG_SCHOOL);
        }
        if (phi.effectiveValue() < spell.minPhi()) {
            return Optional.of(CastBlockReason.LOW_PHI);
        }
        // Insufficient Ψ no longer blocks — CastPipeline allows overcast with trauma.
        if (ctx.breathingMastery() < spell.minMastery()) {
            return Optional.of(CastBlockReason.LOW_MASTERY);
        }
        float cost = spellCost(ctx, phi, spell);
        float powerPsi = Math.max(availablePsi, cost);
        PsiContext powerCtx = ctx.withCurrentPsi(powerPsi);
        if (spell.minPower() > 0f && spellPower(powerCtx, phi, spell) < spell.minPower()) {
            return Optional.of(CastBlockReason.LOW_POWER);
        }
        return Optional.empty();
    }

    /** True when Φ and Ψ are sufficient but breathing mastery or spell power is too low. */
    public static boolean failsConcentration(PsiContext ctx, PhiSample phi, SpellDefinition spell, float availablePsi) {
        return diagnoseCannotCast(ctx, phi, spell, availablePsi)
                .filter(r -> r == CastBlockReason.LOW_MASTERY || r == CastBlockReason.LOW_POWER)
                .isPresent();
    }

    /**
     * Spectral purity: 1.0 when frequencies match, falls off with delta.
     * Replaces literal Hz comparison for gameplay feel.
     */
    public static float resonance(float operatorHz, float spellHz) {
        return resonance(operatorHz, spellHz, 0f);
    }

    public static float resonance(float operatorHz, float spellHz, float widthBonus) {
        float delta = Math.abs(operatorHz - spellHz);
        float width = BalanceConfig.RESONANCE_WIDTH_HZ.get().floatValue() + Math.max(0f, widthBonus);
        if (width <= 0f) {
            return operatorHz == spellHz ? 1f : 0f;
        }
        return Math.max(0f, 1f - delta / width);
    }
}
