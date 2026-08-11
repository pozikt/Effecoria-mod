package com.effecoria.core.alchemy;

/**
 * Block entities that radiate wireless Φ-power to compatible devices in a radius
 * (line-of-sight), distinct from adjacent-only {@link PhiHeatSource}.
 */
public interface PhiPowerProvider {
    /** True while actively broadcasting usable Φ-power. */
    boolean supplying();

    /** Chebyshev radius in blocks (inclusive). */
    int radius();

    /**
     * Output multiplier: 1.0 baseline, 0.5 overheated, 3.0 boost.
     * Consumers may ignore exact values and only check presence / thresholds.
     */
    float powerFactor();

    /**
     * Drain fuel / runtime budget from this source.
     *
     * @return false if not supplying or cannot cover {@code ticks}
     */
    boolean drainFuel(int ticks);
}
