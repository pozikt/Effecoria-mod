package com.effecoria.client.glue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

/** Client cache of Φ-glued blocks, last volume session, and pending axe corner. */
public final class EssenceGlueClient {
    private static final Set<BlockPos> GLUED = Collections.synchronizedSet(new HashSet<>());
    private static final Set<BlockPos> SESSION = Collections.synchronizedSet(new HashSet<>());

    @Nullable
    private static volatile BlockPos PENDING;

    private EssenceGlueClient() {}

    public static void apply(Set<BlockPos> glued, Set<BlockPos> session, @Nullable BlockPos pending) {
        GLUED.clear();
        GLUED.addAll(glued);
        SESSION.clear();
        SESSION.addAll(session);
        PENDING = pending == null ? null : pending.immutable();
    }

    public static Set<BlockPos> glued() {
        return GLUED;
    }

    public static Set<BlockPos> session() {
        return SESSION;
    }

    @Nullable
    public static BlockPos pending() {
        return PENDING;
    }
}
