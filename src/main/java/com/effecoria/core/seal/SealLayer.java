package com.effecoria.core.seal;

import net.minecraft.resources.ResourceLocation;

/**
 * Layering rules for seals on a single block.
 *
 * <p>At most one {@link #OFFENSIVE} layer. If corrupt/legacy data somehow holds several,
 * tick resolution keeps the highest {@link #offensivePriority} (repulse &gt; snare &gt; trap &gt; program).
 */
public enum SealLayer {
    /** Trap / snare / repulse / word program — at most one per block. */
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

    /**
     * Higher value wins when multiple offensive seals exist (legacy / bad data).
     * Repulse ejects before a trap can reliably stack damage with it.
     */
    public static int offensivePriority(ResourceLocation typeId) {
        if (typeId.equals(SealTypes.REPULSE)) {
            return 40;
        }
        if (typeId.equals(SealTypes.SNARE)) {
            return 30;
        }
        if (typeId.equals(SealTypes.DAMAGE_TRAP)) {
            return 20;
        }
        if (typeId.equals(SealTypes.PROGRAM)) {
            return 10;
        }
        return 0;
    }

    public static boolean isOffensive(ResourceLocation typeId) {
        return of(typeId) == OFFENSIVE;
    }
}
