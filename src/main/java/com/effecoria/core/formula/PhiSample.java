package com.effecoria.core.formula;

/**
 * Local Φ-field sample at a cast or regen site.
 *
 * @param value     effective Φ flux in [0, ∞); gameplay usually clamps to ~0–2
 * @param zeroFlux  true inside a Zero-Φ Zone (lead chamber, ZNΦ structure)
 */
public record PhiSample(float value, boolean zeroFlux) {
    public static final PhiSample ZERO_ZONE = new PhiSample(0f, true);
    public static final PhiSample DEFAULT = new PhiSample(1f, false);

    public float effectiveValue() {
        return zeroFlux ? 0f : Math.max(0f, value);
    }
}
