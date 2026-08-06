package com.effecoria.core.alchemy;

public enum AlchemyCrashKind {
    TONIC(0, 40 * 20, 0.7f, 1.0f),
    RESONANCE(1, 50 * 20, 1.0f, 0.75f),
    STIMULANT(2, 70 * 20, 0.5f, 0.6f);

    private final int amplifier;
    private final int durationTicks;
    private final float regenMultiplier;
    private final float phiMultiplier;

    AlchemyCrashKind(int amplifier, int durationTicks, float regenMultiplier, float phiMultiplier) {
        this.amplifier = amplifier;
        this.durationTicks = durationTicks;
        this.regenMultiplier = regenMultiplier;
        this.phiMultiplier = phiMultiplier;
    }

    public int amplifier() {
        return amplifier;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public float regenMultiplier() {
        return regenMultiplier;
    }

    public float phiMultiplier() {
        return phiMultiplier;
    }

    public static AlchemyCrashKind fromAmplifier(int amplifier) {
        for (AlchemyCrashKind kind : values()) {
            if (kind.amplifier == amplifier) {
                return kind;
            }
        }
        return TONIC;
    }
}
