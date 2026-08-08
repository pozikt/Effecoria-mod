package com.effecoria.armor;

/** Contour grade for Φ-armor. BASIC = Φ-chitin; CRYSTAL/STAR = pure/star essonite sets. */
public enum EssoniteArmorTier {
    BASIC(0, 0.35f, 0.012f, 0.12f, true, false, false),
    CRYSTAL(1, 0.70f, 0.022f, 0.22f, true, true, true),
    STAR(2, 1.00f, 0.035f, 0.35f, true, true, true);

    private final int rank;
    private final float capacityWeight;
    private final float regenPerSecond;
    private final float castSubsidyFraction;
    private final boolean flash;
    private final boolean crystalSkinAndWings;
    private final boolean omegaBlock;

    EssoniteArmorTier(
            int rank,
            float capacityWeight,
            float regenPerSecond,
            float castSubsidyFraction,
            boolean flash,
            boolean crystalSkinAndWings,
            boolean omegaBlock) {
        this.rank = rank;
        this.capacityWeight = capacityWeight;
        this.regenPerSecond = regenPerSecond;
        this.castSubsidyFraction = castSubsidyFraction;
        this.flash = flash;
        this.crystalSkinAndWings = crystalSkinAndWings;
        this.omegaBlock = omegaBlock;
    }

    public int rank() {
        return rank;
    }

    /** Relative contribution of a full set to the Φ charge pool (chest scaled separately). */
    public float capacityWeight() {
        return capacityWeight;
    }

    public float regenPerSecond() {
        return regenPerSecond;
    }

    public float castSubsidyFraction() {
        return castSubsidyFraction;
    }

    public boolean allowsFlash() {
        return flash;
    }

    public boolean allowsCrystalSkin() {
        return crystalSkinAndWings;
    }

    public boolean allowsWings() {
        return crystalSkinAndWings;
    }

    public boolean allowsOmegaBlock() {
        return omegaBlock;
    }

    public boolean atLeast(EssoniteArmorTier other) {
        return rank >= other.rank;
    }
}
