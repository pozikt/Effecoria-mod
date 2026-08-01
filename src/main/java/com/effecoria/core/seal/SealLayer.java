package com.effecoria.core.seal;

import net.minecraft.resources.ResourceLocation;

/** Layering rules for seals on a single block. */
public enum SealLayer {
    /** Trap / snare / repulse — at most one per block. */
    OFFENSIVE,
    /** Fortify / glow — one of each type may coexist. */
    UTILITY;

    public static SealLayer of(ResourceLocation typeId) {
        if (typeId.equals(SealTypes.PROGRAM)) {
            return OFFENSIVE;
        }
        if (typeId.equals(SealTypes.DAMAGE_TRAP)
                || typeId.equals(SealTypes.SNARE)
                || typeId.equals(SealTypes.REPULSE)) {
            return OFFENSIVE;
        }
        return UTILITY;
    }
}
