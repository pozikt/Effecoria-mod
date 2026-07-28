package com.effecoria.magic;

/** How a spell resolved after Ψ was committed. */
public enum CastDelivery {
    /** Full effect at full cost. */
    FULL,
    /** Targeted spell with no valid entity target — whiff cost only. */
    WHIFF_NO_TARGET,
    /** Block-seal spell with no valid block — whiff cost only. */
    WHIFF_NO_BLOCK
}
