package com.effecoria.core.formula;

import com.effecoria.core.magic.MagicSchool;

/** Player/NPC Ψ-operator state used by {@link FormulaEngine}. */
public record PsiContext(
        float soulStrength,
        float currentPsi,
        float biologyQ,
        float frequencyHz,
        MagicSchool school,
        float entropyB,
        float breathingMastery,
        int essence,
        float exhaustion,
        float breathTrainRegenBonus,
        boolean breathTrainFatigue) {

    public static PsiContext defaultHuman(MagicSchool school) {
        return new PsiContext(1f, 50f, 0.6f, school.nominalFrequencyHz(), school, 0f, 0f, 0, 0f, 0f, false);
    }

    public float mastery() {
        return Mastery.factor(breathingMastery, essence);
    }

    public boolean hasAffinity(MagicSchool required) {
        return school == required;
    }
}
