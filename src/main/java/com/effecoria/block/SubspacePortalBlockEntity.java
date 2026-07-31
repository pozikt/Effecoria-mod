package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.effect.spatial.SubspaceVoyageService;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

import javax.annotation.Nullable;

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
    private long cooldownUntil;

    public SubspacePortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUBSPACE_PORTAL.get(), pos, state);
    }

    public void configure(UUID owner, Role role, UUID sessionId) {
        this.owner = owner;
        this.role = role;
        this.sessionId = sessionId;
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

    public void tryTransport(ServerLevel level, Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        if (owner == null || sessionId == null || !owner.equals(player.getUUID())) {
            return;
        }
        long now = level.getGameTime();
        if (now < cooldownUntil) {
            return;
        }
        cooldownUntil = now + 20L;
        SubspaceVoyageService.onPortalTouch(player, this);
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
        tag.putLong("CooldownUntil", cooldownUntil);
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
        cooldownUntil = tag.getLong("CooldownUntil");
    }
}
