package com.effecoria.core.circuit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Live index of Φ-filament endpoints. Entities register on add/remove; not SavedData. */
public final class PhiFilamentLinks {
    private static final Map<Level, Map<Long, List<BlockPos>>> LINKS = new WeakHashMap<>();

    private PhiFilamentLinks() {}

    public static synchronized void bind(Level level, BlockPos a, BlockPos b) {
        if (level == null || a == null || b == null || a.equals(b)) {
            return;
        }
        Map<Long, List<BlockPos>> map = LINKS.computeIfAbsent(level, k -> new ConcurrentHashMap<>());
        add(map, a, b);
        add(map, b, a);
    }

    public static synchronized void unbind(Level level, BlockPos a, BlockPos b) {
        if (level == null || a == null || b == null) {
            return;
        }
        Map<Long, List<BlockPos>> map = LINKS.get(level);
        if (map == null) {
            return;
        }
        remove(map, a, b);
        remove(map, b, a);
    }

    public static List<BlockPos> neighbors(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return List.of();
        }
        Map<Long, List<BlockPos>> map = LINKS.get(level);
        if (map == null) {
            return List.of();
        }
        List<BlockPos> found = map.get(pos.asLong());
        return found == null ? List.of() : Collections.unmodifiableList(found);
    }

    private static void add(Map<Long, List<BlockPos>> map, BlockPos from, BlockPos to) {
        List<BlockPos> list = map.computeIfAbsent(from.asLong(), k -> new ArrayList<>());
        if (!list.contains(to)) {
            list.add(to.immutable());
        }
    }

    private static void remove(Map<Long, List<BlockPos>> map, BlockPos from, BlockPos to) {
        List<BlockPos> list = map.get(from.asLong());
        if (list == null) {
            return;
        }
        list.remove(to);
        if (list.isEmpty()) {
            map.remove(from.asLong());
        }
    }
}
