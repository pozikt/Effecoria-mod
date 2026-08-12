package com.effecoria.core.glue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-dimension union-find of Φ-glued block positions — backbone for smart structures. */
public final class EssenceGlueData extends SavedData {
    private final Map<Long, Long> parent = new HashMap<>();

    public EssenceGlueData() {}

    public static EssenceGlueData get(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(EssenceGlueData::new, EssenceGlueData::load), "effecoria_essence_glue");
    }

    private long find(long key) {
        Long p = parent.get(key);
        if (p == null) {
            parent.put(key, key);
            return key;
        }
        if (p == key) {
            return key;
        }
        long root = find(p);
        parent.put(key, root);
        return root;
    }

    public void ensure(BlockPos pos) {
        long k = pos.asLong();
        if (!parent.containsKey(k)) {
            parent.put(k, k);
            setDirty();
        }
    }

    public void connect(BlockPos a, BlockPos b) {
        ensure(a);
        ensure(b);
        long ra = find(a.asLong());
        long rb = find(b.asLong());
        if (ra != rb) {
            parent.put(rb, ra);
            setDirty();
        }
    }

    public void remove(BlockPos pos) {
        long k = pos.asLong();
        if (!parent.containsKey(k)) {
            return;
        }
        long root = find(k);
        parent.remove(k);
        parent.entrySet().removeIf(e -> e.getKey() == k);
        Set<Long> members = new HashSet<>();
        for (Map.Entry<Long, Long> e : new HashMap<>(parent).entrySet()) {
            if (find(e.getKey()) == root) {
                members.add(e.getKey());
            }
        }
        members.remove(k);
        for (long m : members) {
            parent.put(m, m);
        }
        setDirty();
    }

    public Set<BlockPos> component(BlockPos pos) {
        if (!parent.containsKey(pos.asLong())) {
            return Set.of();
        }
        long root = find(pos.asLong());
        Set<BlockPos> out = new HashSet<>();
        for (long key : parent.keySet()) {
            if (find(key) == root) {
                out.add(BlockPos.of(key));
            }
        }
        return out;
    }

    public boolean isGlued(BlockPos pos) {
        return parent.containsKey(pos.asLong());
    }

    public int size() {
        return parent.size();
    }

    /** All glued blocks within squared distance of center (for client outlines). */
    public Set<BlockPos> allInRadius(BlockPos center, int radius) {
        Set<BlockPos> out = new HashSet<>();
        long r2 = (long) radius * radius;
        for (long key : parent.keySet()) {
            BlockPos p = BlockPos.of(key);
            if (p.distSqr(center) <= r2) {
                out.add(p);
            }
        }
        return out;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, Long> e : parent.entrySet()) {
            CompoundTag pair = new CompoundTag();
            pair.putLong("K", e.getKey());
            pair.putLong("P", e.getValue());
            list.add(pair);
        }
        tag.put("Parent", list);
        return tag;
    }

    public static EssenceGlueData load(CompoundTag tag, HolderLookup.Provider registries) {
        EssenceGlueData data = new EssenceGlueData();
        ListTag list = tag.getList("Parent", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag pair = (CompoundTag) t;
            data.parent.put(pair.getLong("K"), pair.getLong("P"));
        }
        return data;
    }
}
