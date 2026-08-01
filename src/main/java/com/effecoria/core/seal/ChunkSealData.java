package com.effecoria.core.seal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Per-chunk seal map. Each block may hold multiple layers:
 * one offensive + fortify and/or glow (see {@link SealLayer}).
 */
public final class ChunkSealData {
    public static final StreamCodec<RegistryFriendlyByteBuf, ChunkSealData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                ByteBufCodecs.VAR_INT.encode(buf, data.seals.size());
                for (Map.Entry<BlockPos, List<SealInstance>> entry : data.seals.entrySet()) {
                    BlockPos.STREAM_CODEC.encode(buf, entry.getKey());
                    List<SealInstance> layers = entry.getValue();
                    ByteBufCodecs.VAR_INT.encode(buf, layers.size());
                    for (SealInstance seal : layers) {
                        encodeSeal(buf, seal);
                    }
                }
            },
            buf -> {
                ChunkSealData data = new ChunkSealData();
                int posCount = ByteBufCodecs.VAR_INT.decode(buf);
                for (int i = 0; i < posCount; i++) {
                    BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
                    int layerCount = ByteBufCodecs.VAR_INT.decode(buf);
                    List<SealInstance> layers = new ArrayList<>(layerCount);
                    for (int j = 0; j < layerCount; j++) {
                        layers.add(decodeSeal(buf));
                    }
                    if (!layers.isEmpty()) {
                        data.seals.put(pos.immutable(), new ArrayList<>(ChunkSealData.normalizeLayers(layers)));
                    }
                }
                return data;
            });

    private final Map<BlockPos, List<SealInstance>> seals = new HashMap<>();

    public Map<BlockPos, List<SealInstance>> seals() {
        return seals;
    }

    /** First layer if any — prefer fortify for break-speed callers when present. */
    public Optional<SealInstance> get(BlockPos pos) {
        List<SealInstance> layers = seals.get(pos.immutable());
        if (layers == null || layers.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(layers.getFirst());
    }

    public List<SealInstance> getAll(BlockPos pos) {
        List<SealInstance> layers = seals.get(pos.immutable());
        if (layers == null || layers.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(layers);
    }

    public Optional<SealInstance> find(BlockPos pos, ResourceLocation typeId) {
        for (SealInstance seal : getAll(pos)) {
            if (seal.typeId().equals(typeId)) {
                return Optional.of(seal);
            }
        }
        return Optional.empty();
    }

    public Optional<SealInstance> findOffensive(BlockPos pos) {
        SealInstance best = null;
        int bestPriority = Integer.MIN_VALUE;
        long bestPlaced = Long.MIN_VALUE;
        for (SealInstance seal : getAll(pos)) {
            if (SealLayer.of(seal.typeId()) != SealLayer.OFFENSIVE) {
                continue;
            }
            int priority = SealLayer.offensivePriority(seal.typeId());
            if (best == null
                    || priority > bestPriority
                    || (priority == bestPriority && seal.placedAt() >= bestPlaced)) {
                best = seal;
                bestPriority = priority;
                bestPlaced = seal.placedAt();
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * Enforces builder rules: one offensive (highest priority), unique fortify/glow types.
     * Returns a new list; does not mutate the input.
     */
    public static List<SealInstance> normalizeLayers(List<SealInstance> layers) {
        if (layers == null || layers.isEmpty()) {
            return List.of();
        }
        List<SealInstance> out = new ArrayList<>(layers.size());
        SealInstance offensive = null;
        int offensivePriority = Integer.MIN_VALUE;
        long offensivePlaced = Long.MIN_VALUE;
        for (SealInstance seal : layers) {
            if (SealLayer.of(seal.typeId()) == SealLayer.OFFENSIVE) {
                int priority = SealLayer.offensivePriority(seal.typeId());
                if (offensive == null
                        || priority > offensivePriority
                        || (priority == offensivePriority && seal.placedAt() >= offensivePlaced)) {
                    offensive = seal;
                    offensivePriority = priority;
                    offensivePlaced = seal.placedAt();
                }
                continue;
            }
            boolean duplicateUtility = false;
            for (int i = 0; i < out.size(); i++) {
                if (out.get(i).typeId().equals(seal.typeId())) {
                    // Keep the newer utility of the same type.
                    if (seal.placedAt() >= out.get(i).placedAt()) {
                        out.set(i, seal);
                    }
                    duplicateUtility = true;
                    break;
                }
            }
            if (!duplicateUtility) {
                out.add(seal);
            }
        }
        if (offensive != null) {
            out.add(offensive);
        }
        return out;
    }

    public void putLayers(BlockPos pos, List<SealInstance> layers) {
        BlockPos key = pos.immutable();
        List<SealInstance> normalized = normalizeLayers(layers);
        if (normalized.isEmpty()) {
            seals.remove(key);
        } else {
            seals.put(key, new ArrayList<>(normalized));
        }
    }

    public List<SealInstance> removeAll(BlockPos pos) {
        List<SealInstance> removed = seals.remove(pos.immutable());
        return removed == null ? List.of() : removed;
    }

    public boolean isEmpty() {
        return seals.isEmpty();
    }

    /** Removes expired seals; returns true if anything changed. */
    public boolean purgeExpired(long gameTime) {
        boolean changed = false;
        Iterator<Map.Entry<BlockPos, List<SealInstance>>> it = seals.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, List<SealInstance>> entry = it.next();
            List<SealInstance> layers = entry.getValue();
            boolean layerChanged = layers.removeIf(seal -> seal.isExpired(gameTime));
            if (layerChanged) {
                changed = true;
            }
            if (layers.isEmpty()) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, List<SealInstance>> entry : seals.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putInt("x", entry.getKey().getX());
            entryTag.putInt("y", entry.getKey().getY());
            entryTag.putInt("z", entry.getKey().getZ());
            ListTag layerList = new ListTag();
            for (SealInstance seal : entry.getValue()) {
                layerList.add(seal.save());
            }
            entryTag.put("layers", layerList);
            list.add(entryTag);
        }
        tag.put("seals", list);
        tag.putInt("version", 2);
        return tag;
    }

    public void load(HolderLookup.Provider provider, CompoundTag tag) {
        seals.clear();
        ListTag list = tag.getList("seals", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            BlockPos pos = new BlockPos(entryTag.getInt("x"), entryTag.getInt("y"), entryTag.getInt("z"));
            List<SealInstance> layers = new ArrayList<>();
            if (entryTag.contains("layers", Tag.TAG_LIST)) {
                ListTag layerList = entryTag.getList("layers", Tag.TAG_COMPOUND);
                for (int j = 0; j < layerList.size(); j++) {
                    layers.add(SealInstance.load(layerList.getCompound(j)));
                }
            } else {
                // Legacy: single seal fields on the entry itself
                layers.add(SealInstance.load(entryTag));
            }
            if (!layers.isEmpty()) {
                seals.put(pos.immutable(), new ArrayList<>(normalizeLayers(layers)));
            }
        }
    }

    public ChunkSealData copy() {
        ChunkSealData copy = new ChunkSealData();
        for (Map.Entry<BlockPos, List<SealInstance>> entry : seals.entrySet()) {
            List<SealInstance> layers = new ArrayList<>();
            for (SealInstance seal : entry.getValue()) {
                layers.add(new SealInstance(
                        seal.typeId(),
                        seal.casterId(),
                        seal.placedAt(),
                        seal.expireAt(),
                        seal.strength(),
                        seal.params() == null ? new CompoundTag() : seal.params().copy()));
            }
            copy.seals.put(entry.getKey(), layers);
        }
        return copy;
    }

    private static void encodeSeal(RegistryFriendlyByteBuf buf, SealInstance seal) {
        ResourceLocation.STREAM_CODEC.encode(buf, seal.typeId());
        buf.writeUUID(seal.casterId());
        buf.writeLong(seal.placedAt());
        buf.writeLong(seal.expireAt());
        buf.writeFloat(seal.strength());
        CompoundTag params = seal.params() == null ? new CompoundTag() : seal.params();
        ByteBufCodecs.COMPOUND_TAG.encode(buf, params);
    }

    private static SealInstance decodeSeal(RegistryFriendlyByteBuf buf) {
        ResourceLocation type = ResourceLocation.STREAM_CODEC.decode(buf);
        UUID caster = buf.readUUID();
        long placedAt = buf.readLong();
        long expireAt = buf.readLong();
        float strength = buf.readFloat();
        CompoundTag params = ByteBufCodecs.COMPOUND_TAG.decode(buf);
        return new SealInstance(type, caster, placedAt, expireAt, strength, params);
    }
}
