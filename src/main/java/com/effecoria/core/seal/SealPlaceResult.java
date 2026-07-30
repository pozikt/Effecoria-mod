package com.effecoria.core.seal;

/** Result of placing a seal onto a block that may already have layers. */
public enum SealPlaceResult {
    /** First seal of its kind on this block. */
    PLACED,
    /** Same type replaced (refreshed). */
    REPLACED_SAME,
    /** Different offensive seal replaced the previous offensive layer. */
    REPLACED_OFFENSIVE,
    /** Utility stacked alongside an existing different utility. */
    STACKED
}
