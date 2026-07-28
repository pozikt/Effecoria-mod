package com.effecoria.core.formula;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FormulaEngineTest {
    @Test
    void resonance_peaksAtMatchingFrequency() {
        float match = FormulaEngine.resonance(7.8f, 7.8f);
        assertEquals(1f, match, 0.001f);
    }
}
