package com.effecoria.core.formula;

import com.effecoria.core.magic.MagicSchool;

/**
 * Player/NPC Ψ-operator state used by {@link FormulaEngine}.
 *
 * @param soulStrength Ψ_soul — base operator strength (race + progression)
 * @param currentPsi   stored E_Ψ
 * @param biologyQ     Q_biology — Orkanum conversion coefficient
 * @param frequencyHz  fundamental Ψ frequency; defines magic school
 * @param school       resolved magic affinity (immutable after initiation)
 * @param entropyB     accumulated imaginary / side component (backlash meter)
 */
public record PsiContext(
        float soulStrength,
        float currentPsi,
        float biologyQ,
        float frequencyHz,
        MagicSchool school,
        float entropyB,
        float breathingMastery,
        int essence) {

    public static PsiContext defaultHuman(MagicSchool school) {
        return new PsiContext(1f, 50f, 0.6f, school.nominalFrequencyHz(), school, 0f, 0f, 0);
    }

    public float mastery() {
        return Mastery.factor(breathingMastery, essence);
    }

    public boolean hasAffinity(MagicSchool required) {
        return school == required;
    }
}
