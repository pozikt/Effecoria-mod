package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.effect.spatial.SubspaceVoyageService;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * Hyperspace gate. Any player may walk through; the creating Spatial mage is {@link #owner}
 * for bookkeeping only.
 */
public final class SubspacePortalBlockEntity extends BlockEntity {
    public enum Role {
        ENTRY,
        EXIT
    }

    @Nullable
    private UUID owner;
    private Role role = Role.ENTRY;
    @Nullable
    private UUID sessionId;
    @Nullable
    private ResourceKey<Level> originDim;
    @Nullable
    private BlockPos originPos;
    @Nullable
    private BlockPos entrySubspacePos;
    @Nullable
    private BlockPos exitOverworldPos;
    /** Per-entity cooldown so one traveler does not lock the gate for others. */
    private final Map<UUID, Long> playerCooldownUntil = new HashMap<>();

    public SubspacePortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUBSPACE_PORTAL.get(), pos, state);
    }

    public void armCooldown(UUID entityId, long gameTimeUntil) {
        playerCooldownUntil.put(entityId, gameTimeUntil);
        if (playerCooldownUntil.size() > 64) {
            long now = getLevel() != null ? getLevel().getGameTime() : 0L;
            playerCooldownUntil.entrySet().removeIf(e -> e.getValue() < now);
        }
    }

    public void configure(
            UUID owner,
            Role role,
            UUID sessionId,
            @Nullable ResourceKey<Level> originDim,
            @Nullable BlockPos originPos,
            @Nullable BlockPos entrySubspacePos,
            @Nullable BlockPos exitOverworldPos) {
        this.owner = owner;
        this.role = role;
        this.sessionId = sessionId;
        this.originDim = originDim;
        this.originPos = originPos == null ? null : originPos.immutable();
        this.entrySubspacePos = entrySubspacePos == null ? null : entrySubspacePos.immutable();
        this.exitOverworldPos = exitOverworldPos == null ? null : exitOverworldPos.immutable();
        setChanged();
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public Role role() {
        return role;
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
    public BlockPos entrySubspacePos() {
        return entrySubspacePos;
    }

    @Nullable
    public BlockPos exitOverworldPos() {
        return exitOverworldPos;
    }

    public void tryTransport(ServerLevel level, Entity entity) {
        if (sessionId == null) {
            return;
        }
        long now = level.getGameTime();
        Long until = playerCooldownUntil.get(entity.getUUID());
        if (until != null && now < until) {
            return;
        }
        // Arm before transport so dimension change / re-entry cannot loop in one tick.
        playerCooldownUntil.put(entity.getUUID(), now + 45L);

        if (entity instanceof ServerPlayer player) {
            SubspaceVoyageService.onPortalTouch(player, this);
            return;
        }
        if (entity instanceof LivingEntity living && !(entity instanceof Player)) {
            SubspaceVoyageService.transportNonPlayer(living, this);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putString("Role", role.name());
        if (sessionId != null) {
            tag.putUUID("SessionId", sessionId);
        }
        if (originDim != null) {
            tag.putString("OriginDim", originDim.location().toString());
        }
        writePos(tag, "Origin", originPos);
        writePos(tag, "EntrySubspace", entrySubspacePos);
        writePos(tag, "ExitOverworld", exitOverworldPos);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        try {
            role = Role.valueOf(tag.getString("Role"));
        } catch (IllegalArgumentException ignored) {
            role = Role.ENTRY;
        }
        sessionId = tag.hasUUID("SessionId") ? tag.getUUID("SessionId") : null;
        originDim = null;
        if (tag.contains("OriginDim", Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("OriginDim"));
            if (id != null) {
                originDim = ResourceKey.create(Registries.DIMENSION, id);
            }
        }
        originPos = readPos(tag, "Origin");
        entrySubspacePos = readPos(tag, "EntrySubspace");
        exitOverworldPos = readPos(tag, "ExitOverworld");
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
        if (!tag.contains(prefix + "X")) {
            return null;
        }
        return new BlockPos(tag.getInt(prefix + "X"), tag.getInt(prefix + "Y"), tag.getInt(prefix + "Z"));
    }
}
