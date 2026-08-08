package com.effecoria.world;

import net.minecraft.world.level.GameRules;

/**
 * Custom gamerules. {@link #SUBSPACE_ESSENTIALIZE_SPEED} mirrors {@code randomTickSpeed}
 * for hyperspace matter aging (which does <em>not</em> use block random ticks).
 */
public final class ModGameRules {
    /**
     * Multiplier for Φ-essentialization age in {@code effecoria:subspace}.
     * {@code 0} pauses conversion; {@code 1} is real-time config ages; higher values compress waits
     * (e.g. {@code 100} ≈ hundredfold faster), analogous to {@code /gamerule randomTickSpeed}.
     */
    public static final GameRules.Key<GameRules.IntegerValue> SUBSPACE_ESSENTIALIZE_SPEED =
            GameRules.register(
                    "subspaceEssentializeSpeed",
                    GameRules.Category.UPDATES,
                    GameRules.IntegerValue.create(1));

    private ModGameRules() {}

    /** Force class load so the rule registers during mod init. */
    public static void bootstrap() {
        // no-op — static init registers the rule
    }
}
