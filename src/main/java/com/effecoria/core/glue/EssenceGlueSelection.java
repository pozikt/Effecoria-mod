package com.effecoria.core.glue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Per-player Φ-glue axe state: pending corner (pos1) + last volume session for outlines.
 * Shift+RMB clears both; glued cells stay in {@link EssenceGlueData}.
 */
public final class EssenceGlueSelection {
    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private EssenceGlueSelection() {}

    public static final class State {
        @Nullable
        public BlockPos pending;
        public final Set<BlockPos> session = new HashSet<>();
    }

    public static State state(Player player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    @Nullable
    public static BlockPos pending(Player player) {
        State s = STATES.get(player.getUUID());
        return s == null ? null : s.pending;
    }

    public static void setPending(Player player, BlockPos pos) {
        State s = state(player);
        s.pending = pos.immutable();
    }

    public static void clearPending(Player player) {
        State s = STATES.get(player.getUUID());
        if (s != null) {
            s.pending = null;
        }
    }

    public static Set<BlockPos> session(Player player) {
        return state(player).session;
    }

    public static void clear(Player player) {
        STATES.remove(player.getUUID());
    }

    public static void removeBlock(BlockPos pos) {
        for (State s : STATES.values()) {
            s.session.remove(pos);
            if (pos.equals(s.pending)) {
                s.pending = null;
            }
        }
    }

    public static void removeBlocks(Set<BlockPos> blocks) {
        for (BlockPos pos : blocks) {
            removeBlock(pos);
        }
    }
}
