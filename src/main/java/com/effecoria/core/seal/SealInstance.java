package com.effecoria.core.seal;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * A single seal inscribed on a block.
 *
 * @param expireAt game time when the seal expires; {@code -1} means permanent
 */
public record SealInstance(
        ResourceLocation typeId,
        UUID casterId,
        long placedAt,
        long expireAt,
        float strength,
        CompoundTag params) {

    public static final long PERMANENT = -1L;

    public boolean isPermanent() {
        return expireAt == PERMANENT;
    }

    public boolean isExpired(long gameTime) {
        return !isPermanent() && gameTime >= expireAt;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", typeId.toString());
        tag.putUUID("caster", casterId);
        tag.putLong("placedAt", placedAt);
        tag.putLong("expireAt", expireAt);
        tag.putFloat("strength", strength);
        if (params != null && !params.isEmpty()) {
            tag.put("params", params.copy());
        }
        return tag;
    }

    public static SealInstance load(CompoundTag tag) {
        ResourceLocation type = ResourceLocation.parse(tag.getString("type"));
        UUID caster = tag.hasUUID("caster") ? tag.getUUID("caster") : new UUID(0L, 0L);
        long placedAt = tag.getLong("placedAt");
        long expireAt = tag.contains("expireAt") ? tag.getLong("expireAt") : PERMANENT;
        float strength = tag.getFloat("strength");
        CompoundTag params = tag.contains("params") ? tag.getCompound("params").copy() : new CompoundTag();
        return new SealInstance(type, caster, placedAt, expireAt, strength, params);
    }
}
