package com.effecoria.core.seal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/** Per-chunk seal map — one seal per block position. */
public final class ChunkSealData {
    public static final StreamCodec<RegistryFriendlyByteBuf, ChunkSealData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                ByteBufCodecs.VAR_INT.encode(buf, data.seals.size());
                for (Map.Entry<BlockPos, SealInstance> entry : data.seals.entrySet()) {
                    BlockPos.STREAM_CODEC.encode(buf, entry.getKey());
                    encodeSeal(buf, entry.getValue());
                }
            },
            buf -> {
                ChunkSealData data = new ChunkSealData();
                int count = ByteBufCodecs.VAR_INT.decode(buf);
                for (int i = 0; i < count; i++) {
                    BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
                    data.seals.put(pos.immutable(), decodeSeal(buf));
                }
                return data;
            });

    private final Map<BlockPos, SealInstance> seals = new HashMap<>();

    public Map<BlockPos, SealInstance> seals() {
        return seals;
    }

    public Optional<SealInstance> get(BlockPos pos) {
        return Optional.ofNullable(seals.get(pos.immutable()));
    }

    public void put(BlockPos pos, SealInstance seal) {
        seals.put(pos.immutable(), seal);
    }

    public SealInstance remove(BlockPos pos) {
        return seals.remove(pos.immutable());
    }

    public boolean isEmpty() {
        return seals.isEmpty();
    }

    /** Removes expired seals; returns true if anything changed. */
    public boolean purgeExpired(long gameTime) {
        boolean changed = false;
        Iterator<Map.Entry<BlockPos, SealInstance>> it = seals.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isExpired(gameTime)) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, SealInstance> entry : seals.entrySet()) {
            CompoundTag entryTag = entry.getValue().save();
            entryTag.putInt("x", entry.getKey().getX());
            entryTag.putInt("y", entry.getKey().getY());
            entryTag.putInt("z", entry.getKey().getZ());
            list.add(entryTag);
        }
        tag.put("seals", list);
        return tag;
    }

    public void load(HolderLookup.Provider provider, CompoundTag tag) {
        seals.clear();
        ListTag list = tag.getList("seals", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            BlockPos pos = new BlockPos(entryTag.getInt("x"), entryTag.getInt("y"), entryTag.getInt("z"));
            seals.put(pos.immutable(), SealInstance.load(entryTag));
        }
    }

    public ChunkSealData copy() {
        ChunkSealData copy = new ChunkSealData();
        for (Map.Entry<BlockPos, SealInstance> entry : seals.entrySet()) {
            SealInstance seal = entry.getValue();
            copy.seals.put(entry.getKey(), new SealInstance(
                    seal.typeId(),
                    seal.casterId(),
                    seal.placedAt(),
                    seal.expireAt(),
                    seal.strength(),
                    seal.params() == null ? new CompoundTag() : seal.params().copy()));
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
        java.util.UUID caster = buf.readUUID();
        long placedAt = buf.readLong();
        long expireAt = buf.readLong();
        float strength = buf.readFloat();
        CompoundTag params = ByteBufCodecs.COMPOUND_TAG.decode(buf);
        return new SealInstance(type, caster, placedAt, expireAt, strength, params);
    }
}
