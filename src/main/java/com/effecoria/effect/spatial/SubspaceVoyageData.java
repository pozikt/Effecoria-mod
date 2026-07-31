package com.effecoria.effect.spatial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.UUID;

import javax.annotation.Nullable;

/** Persistent subspace voyage state on the player. */
public final class SubspaceVoyageData {
    private boolean active;
    private boolean pendingEntry;
    private boolean returnOriginOnRespawn;
    @Nullable
    private UUID sessionId;
    @Nullable
    private ResourceKey<Level> originDim;
    @Nullable
    private BlockPos originPos;
    @Nullable
    private BlockPos entryPortalPos;
    @Nullable
    private BlockPos entrySubspacePos;
    @Nullable
    private BlockPos exitPortalSubspacePos;
    @Nullable
    private BlockPos exitPortalOverworldPos;

    public static SubspaceVoyageData createDefault() {
        return new SubspaceVoyageData();
    }

    public boolean active() {
        return active;
    }

    public boolean pendingEntry() {
        return pendingEntry;
    }

    public boolean returnOriginOnRespawn() {
        return returnOriginOnRespawn;
    }

    @Nullable
    public UUID sessionId() {
        return sessionId;
    }

    @Nullable
    public ResourceKey<Level> originDim() {
        return originDim;
    }

    @Nullable
    public BlockPos originPos() {
        return originPos;
    }

    @Nullable
    public BlockPos entryPortalPos() {
        return entryPortalPos;
    }

    @Nullable
    public BlockPos entrySubspacePos() {
        return entrySubspacePos;
    }

    @Nullable
    public BlockPos exitPortalSubspacePos() {
        return exitPortalSubspacePos;
    }

    @Nullable
    public BlockPos exitPortalOverworldPos() {
        return exitPortalOverworldPos;
    }

    public void beginPending(
            UUID sessionId,
            ResourceKey<Level> originDim,
            BlockPos originPos,
            BlockPos entryPortalPos) {
        clear();
        this.sessionId = sessionId;
        this.originDim = originDim;
        this.originPos = originPos.immutable();
        this.entryPortalPos = entryPortalPos.immutable();
        this.pendingEntry = true;
        this.active = false;
    }

    public void markEntered(BlockPos entrySubspacePos) {
        this.entrySubspacePos = entrySubspacePos.immutable();
        this.pendingEntry = false;
        this.active = true;
    }

    public void setExitPortals(BlockPos subspaceExit, BlockPos overworldExit) {
        this.exitPortalSubspacePos = subspaceExit.immutable();
        this.exitPortalOverworldPos = overworldExit.immutable();
    }

    public void markReturnOriginOnRespawn() {
        this.returnOriginOnRespawn = true;
    }

    public void clearReturnOriginOnRespawn() {
        this.returnOriginOnRespawn = false;
    }

    public void prepareRespawnAtOrigin(ResourceKey<Level> originDim, BlockPos originPos) {
        clear();
        this.originDim = originDim;
        this.originPos = originPos.immutable();
        this.returnOriginOnRespawn = true;
    }

    public void clear() {
        active = false;
        pendingEntry = false;
        returnOriginOnRespawn = false;
        sessionId = null;
        originDim = null;
        originPos = null;
        entryPortalPos = null;
        entrySubspacePos = null;
        exitPortalSubspacePos = null;
        exitPortalOverworldPos = null;
    }

    public void load(HolderLookup.Provider provider, CompoundTag tag) {
        clear();
        active = tag.getBoolean("Active");
        pendingEntry = tag.getBoolean("PendingEntry");
        returnOriginOnRespawn = tag.getBoolean("ReturnOriginOnRespawn");
        if (tag.hasUUID("SessionId")) {
            sessionId = tag.getUUID("SessionId");
        }
        if (tag.contains("OriginDim", Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("OriginDim"));
            if (id != null) {
                originDim = ResourceKey.create(Registries.DIMENSION, id);
            }
        }
        originPos = readPos(tag, "Origin");
        entryPortalPos = readPos(tag, "EntryPortal");
        entrySubspacePos = readPos(tag, "EntrySubspace");
        exitPortalSubspacePos = readPos(tag, "ExitSubspace");
        exitPortalOverworldPos = readPos(tag, "ExitOverworld");
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Active", active);
        tag.putBoolean("PendingEntry", pendingEntry);
        tag.putBoolean("ReturnOriginOnRespawn", returnOriginOnRespawn);
        if (sessionId != null) {
            tag.putUUID("SessionId", sessionId);
        }
        if (originDim != null) {
            tag.putString("OriginDim", originDim.location().toString());
        }
        writePos(tag, "Origin", originPos);
        writePos(tag, "EntryPortal", entryPortalPos);
        writePos(tag, "EntrySubspace", entrySubspacePos);
        writePos(tag, "ExitSubspace", exitPortalSubspacePos);
        writePos(tag, "ExitOverworld", exitPortalOverworldPos);
        return tag;
    }

    private static void writePos(CompoundTag tag, String prefix, @Nullable BlockPos pos) {
        if (pos == null) {
            return;
        }
        tag.putInt(prefix + "X", pos.getX());
        tag.putInt(prefix + "Y", pos.getY());
        tag.putInt(prefix + "Z", pos.getZ());
    }

    @Nullable
    private static BlockPos readPos(CompoundTag tag, String prefix) {
        String xKey = prefix + "X";
        if (!tag.contains(xKey)) {
            return null;
        }
        return new BlockPos(tag.getInt(xKey), tag.getInt(prefix + "Y"), tag.getInt(prefix + "Z"));
    }
}
