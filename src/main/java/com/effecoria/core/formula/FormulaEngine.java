package com.effecoria.core.formula;

import com.effecoria.core.progression.ExhaustionService;
import com.effecoria.config.BalanceConfig;
import com.effecoria.core.magic.SpellDefinition;

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
                * ExhaustionService.regenMultiplier(ctx.exhaustion());
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
        float resonance = resonance(ctx.frequencyHz(), spell.frequencyHz());
        return ctx.currentPsi()
                * phi.effectiveValue()
                * resonance
                * spell.powerMultiplier()
                * ctx.mastery()
                * BalanceConfig.SPELL_POWER_SCALE.get().floatValue();
    }

    /**
     * cost = base_cost × low_Φ_penalty × mastery_cost_reduction
     */
    public static float spellCost(PsiContext ctx, PhiSample phi, SpellDefinition spell) {
        float phiPenalty = 1f + BalanceConfig.LOW_PHI_COST_FACTOR.get().floatValue()
                * (1f - Math.min(1f, phi.effectiveValue()));
        return spell.baseCost()
                * phiPenalty
                * Mastery.costMultiplier(ctx.mastery())
                * ExhaustionService.costMultiplier(ctx.exhaustion());
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
        if (phi.zeroFlux()) {
            return false;
        }
        if (spell.requiredSchool() != null && !ctx.hasAffinity(spell.requiredSchool())) {
            return false;
        }
        if (phi.effectiveValue() < spell.minPhi()) {
            return false;
        }
        if (ctx.breathingMastery() < spell.minMastery()) {
            return false;
        }
        if (spell.minPower() > 0f && spellPower(ctx, phi, spell) < spell.minPower()) {
            return false;
        }
        return availablePsi >= spellCost(ctx, phi, spell);
    }

    /** True when Φ and Ψ are sufficient but breathing mastery or spell power is too low. */
    public static boolean failsConcentration(PsiContext ctx, PhiSample phi, SpellDefinition spell, float availablePsi) {
        if (phi.zeroFlux() || phi.effectiveValue() < spell.minPhi()) {
            return false;
        }
        if (availablePsi < spellCost(ctx, phi, spell)) {
            return false;
        }
        return ctx.breathingMastery() < spell.minMastery()
                || (spell.minPower() > 0f && spellPower(ctx, phi, spell) < spell.minPower());
    }

    /**
     * Spectral purity: 1.0 when frequencies match, falls off with delta.
     * Replaces literal Hz comparison for gameplay feel.
     */
    public static float resonance(float operatorHz, float spellHz) {
        float delta = Math.abs(operatorHz - spellHz);
        float width = BalanceConfig.RESONANCE_WIDTH_HZ.get().floatValue();
        if (width <= 0f) {
            return operatorHz == spellHz ? 1f : 0f;
        }
        return Math.max(0f, 1f - delta / width);
    }
}
