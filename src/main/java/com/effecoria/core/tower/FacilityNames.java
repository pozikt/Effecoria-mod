package com.effecoria.core.tower;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Shared sanitize / NBT helpers for {@link NamedFacilityDevice}. */
public final class FacilityNames {
    public static final String NBT_KEY = "FacilityName";
    public static final int MAX_LEN = 32;

    private FacilityNames() {}

    public static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.length() > MAX_LEN) {
            trimmed = trimmed.substring(0, MAX_LEN);
        }
        return trimmed;
    }

    public static void save(CompoundTag tag, String name) {
        if (name != null && !name.isEmpty()) {
            tag.putString(NBT_KEY, name);
        }
    }

    public static String load(CompoundTag tag) {
        return tag.contains(NBT_KEY) ? sanitize(tag.getString(NBT_KEY)) : "";
    }

    /** Persist + sync clients after a rename. */
    public static void markNamed(BlockEntity be) {
        be.setChanged();
        if (be.getLevel() != null && !be.getLevel().isClientSide()) {
            var pos = be.getBlockPos();
            var state = be.getBlockState();
            be.getLevel().sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
