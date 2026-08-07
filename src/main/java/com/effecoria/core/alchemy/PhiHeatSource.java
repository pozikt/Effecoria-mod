package com.effecoria.core.alchemy;

/**
 * Block entities that radiate Φ-heat to adjacent consumers (sides + above the source).
 */
public interface PhiHeatSource {
    HeatLevel heatLevel();

    /** Drain one cook-tick of fuel/heat. Returns false if no heat available. */
    boolean consumeHeatTick();
}
