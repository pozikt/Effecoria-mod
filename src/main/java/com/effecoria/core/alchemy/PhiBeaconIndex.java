package com.effecoria.core.alchemy;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** In-memory index of named Φ-beacons per dimension (rebuilt from loaded BEs). */
public final class PhiBeaconIndex {
    public record Entry(ResourceKey<Level> dimension, BlockPos pos, String name) {}

    private static final Map<ResourceKey<Level>, Map<String, BlockPos>> BY_DIM = new ConcurrentHashMap<>();

    private PhiBeaconIndex() {}

    public static void register(ResourceKey<Level> dim, BlockPos pos, String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        String key = name.trim();
        BY_DIM.computeIfAbsent(dim, d -> new ConcurrentHashMap<>()).put(key, pos.immutable());
    }

    public static void unregister(ResourceKey<Level> dim, BlockPos pos) {
        Map<String, BlockPos> map = BY_DIM.get(dim);
        if (map == null) {
            return;
        }
        map.entrySet().removeIf(e -> e.getValue().equals(pos));
    }

    public static void unregisterName(ResourceKey<Level> dim, String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        Map<String, BlockPos> map = BY_DIM.get(dim);
        if (map != null) {
            map.remove(name.trim());
        }
    }

    public static Optional<BlockPos> find(ResourceKey<Level> dim, String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        Map<String, BlockPos> map = BY_DIM.get(dim);
        if (map == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(map.get(name.trim()));
    }

    public static Map<String, BlockPos> allIn(ResourceKey<Level> dim) {
        Map<String, BlockPos> map = BY_DIM.get(dim);
        if (map == null || map.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(map);
    }

    public static boolean isNameTaken(ResourceKey<Level> dim, String name, BlockPos self) {
        Optional<BlockPos> existing = find(dim, name);
        return existing.isPresent() && !existing.get().equals(self);
    }

    public static void clearDimension(ResourceKey<Level> dim) {
        BY_DIM.remove(dim);
    }
}
