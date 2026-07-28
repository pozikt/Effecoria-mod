package com.effecoria.core.formula;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.magic.SpellDefinition;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class FormulaEngineTest {
    private static final SpellDefinition MENTAL_PUSH = new SpellDefinition(
            ResourceLocation.fromNamespaceAndPath("effecoria", "mental_push"),
            MagicSchool.MENTAL,
            15.2f,
            12f,
            1f,
            0.08f,
            0.2f);

    @Test
    void regenPsi_scalesWithPhiAndBiology() {
        PsiContext ctx = PsiContext.defaultHuman(MagicSchool.MENTAL);
        float low = FormulaEngine.regenPsi(ctx, new PhiSample(0.5f, false), 1f);
        float high = FormulaEngine.regenPsi(ctx, new PhiSample(1.5f, false), 1f);
        assertTrue(high > low);
    }

    @Test
    void regenPsi_zeroInZnPhi() {
        PsiContext ctx = PsiContext.defaultHuman(MagicSchool.ELEMENTAL);
        assertEquals(0f, FormulaEngine.regenPsi(ctx, PhiSample.ZERO_ZONE, 1f));
    }

    @Test
    void spellPower_respectsZeroFlux() {
        PsiContext ctx = PsiContext.defaultHuman(MagicSchool.MENTAL);
        assertEquals(0f, FormulaEngine.spellPower(ctx, PhiSample.ZERO_ZONE, MENTAL_PUSH));
    }

    @Test
    void canCast_requiresSchoolAndPsi() {
        PsiContext mental = PsiContext.defaultHuman(MagicSchool.MENTAL);
        PsiContext elemental = PsiContext.defaultHuman(MagicSchool.ELEMENTAL);
        PhiSample phi = PhiSample.DEFAULT;

        assertTrue(FormulaEngine.canCast(mental, phi, MENTAL_PUSH, 100f));
        assertFalse(FormulaEngine.canCast(elemental, phi, MENTAL_PUSH, 100f));
    }

    @Test
    void resonance_peaksAtMatchingFrequency() {
        float match = FormulaEngine.resonance(7.8f, 7.8f);
        float far = FormulaEngine.resonance(7.8f, 50f);
        assertEquals(1f, match, 0.001f);
        assertTrue(far < match);
    }

    @Test
    void entropy_accumulatesWithPower() {
        float next = FormulaEngine.accumulateEntropy(0f, 10f, 0.1f);
        assertTrue(next > 0f);
    }
}
