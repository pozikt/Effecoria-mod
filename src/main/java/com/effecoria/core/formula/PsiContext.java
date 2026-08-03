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
        boolean breathTrainFatigue,
        float focusCostFloor,
        float focusResonanceWidthBonus,
        float overcastRegenMult) {

    public static PsiContext defaultHuman(MagicSchool school) {
        return new PsiContext(1f, 50f, 0.6f, school.nominalFrequencyHz(), school, 0f, 0f, 0, 0f, 0f, false, 0f, 0f, 1f);
    }

    public PsiContext withCurrentPsi(float psi) {
        return new PsiContext(
                soulStrength,
                psi,
                biologyQ,
                frequencyHz,
                school,
                entropyB,
                breathingMastery,
                essence,
                exhaustion,
                breathTrainRegenBonus,
                breathTrainFatigue,
                focusCostFloor,
                focusResonanceWidthBonus,
                overcastRegenMult);
    }

    public float mastery() {
        return Mastery.factor(breathingMastery, essence);
    }

    public boolean hasAffinity(MagicSchool required) {
        if (required == MagicSchool.COMMON) {
            return school != MagicSchool.NONE && school != null;
        }
        return school == required;
    }
}
