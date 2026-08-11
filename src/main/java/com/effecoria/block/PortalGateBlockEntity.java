package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.alchemy.PhiPower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

/** Stores optional partner gate position + dimension; per-player teleport cooldown. */
public final class PortalGateBlockEntity extends BlockEntity {
    public static final int TELEPORT_POWER_COST = 80;
    public static final int PLAYER_COOLDOWN_TICKS = 100; // 5s

    private Optional<BlockPos> partnerPos = Optional.empty();
    private String partnerDimension = "";
    private final Map<UUID, Integer> playerCooldowns = new HashMap<>();

    public PortalGateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PORTAL_GATE.get(), pos, state);
    }

    public boolean hasPartner() {
        return partnerPos.isPresent() && !partnerDimension.isEmpty();
    }

    public Optional<BlockPos> partnerPos() {
        return partnerPos;
    }

    public String partnerDimension() {
        return partnerDimension;
    }

    public void linkWith(PortalGateBlockEntity other) {
        if (other == null || other == this || level == null || other.level == null) {
            return;
        }
        this.partnerPos = Optional.of(other.getBlockPos().immutable());
        this.partnerDimension = other.level.dimension().location().toString();
        other.partnerPos = Optional.of(this.getBlockPos().immutable());
        other.partnerDimension = this.level.dimension().location().toString();
        setChanged();
        other.setChanged();
        syncActive();
        other.syncActive();
    }

    public void clearLink() {
        partnerPos = Optional.empty();
        partnerDimension = "";
        setChanged();
        syncActive();
    }

    public boolean isPlayerOnCooldown(Player player) {
        return playerCooldowns.getOrDefault(player.getUUID(), 0) > 0;
    }

    public void teleportPlayer(ServerLevel from, ServerPlayer player) {
        if (partnerPos.isEmpty() || partnerDimension.isEmpty()) {
            return;
        }
        // Same-dimension only (Era V portal gate — no TSE / subspace).
        if (!from.dimension().location().toString().equals(partnerDimension)) {
            clearLink();
            return;
        }
        BlockPos target = partnerPos.get();
        if (!(from.getBlockEntity(target) instanceof PortalGateBlockEntity partnerGate)) {
            clearLink();
            return;
        }

        Direction outbound = partnerGate.getBlockState().hasProperty(PortalGateBlock.FACING)
                ? partnerGate.getBlockState().getValue(PortalGateBlock.FACING)
                : Direction.NORTH;
        double x = target.getX() + 0.5 + outbound.getStepX() * 0.85;
        double y = target.getY() + 0.05;
        double z = target.getZ() + 0.5 + outbound.getStepZ() * 0.85;
        float yaw = outbound.toYRot();

        playerCooldowns.put(player.getUUID(), PLAYER_COOLDOWN_TICKS);
        partnerGate.playerCooldowns.put(player.getUUID(), PLAYER_COOLDOWN_TICKS);
        setChanged();
        partnerGate.setChanged();

        player.teleportTo(x, y, z);
        player.setYRot(yaw);
        player.setXRot(0f);
        from.playSound(null, worldPosition, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.8f, 1.0f);
        from.playSound(null, target, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.8f, 1.15f);
    }

    private void syncActive() {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = level.getBlockState(worldPosition);
        if (!state.hasProperty(PortalGateBlock.ACTIVE)) {
            return;
        }
        boolean active = hasPartner() && PhiPower.hasPower(level, worldPosition);
        if (state.getValue(PortalGateBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(PortalGateBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PortalGateBlockEntity be) {
        if (!be.playerCooldowns.isEmpty()) {
            be.playerCooldowns.entrySet().removeIf(e -> {
                int next = e.getValue() - 1;
                if (next <= 0) {
                    return true;
                }
                e.setValue(next);
                return false;
            });
            if (level.getGameTime() % 20L == 0L) {
                be.setChanged();
            }
        }
        if (level.getGameTime() % 10L == 0L) {
            be.syncActive();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        partnerPos.ifPresent(p -> {
            tag.putLong("Partner", p.asLong());
            tag.putString("PartnerDim", partnerDimension);
        });
        CompoundTag cds = new CompoundTag();
        for (Map.Entry<UUID, Integer> e : playerCooldowns.entrySet()) {
            cds.putInt(e.getKey().toString(), e.getValue());
        }
        tag.put("Cooldowns", cds);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("Partner")) {
            partnerPos = Optional.of(BlockPos.of(tag.getLong("Partner")));
            partnerDimension = tag.getString("PartnerDim");
        } else {
            partnerPos = Optional.empty();
            partnerDimension = "";
        }
        playerCooldowns.clear();
        if (tag.contains("Cooldowns")) {
            CompoundTag cds = tag.getCompound("Cooldowns");
            for (String key : cds.getAllKeys()) {
                try {
                    playerCooldowns.put(UUID.fromString(key), cds.getInt(key));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Nullable
    public ResourceKey<Level> partnerDimensionKey() {
        if (partnerDimension.isEmpty()) {
            return null;
        }
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(partnerDimension));
    }
}
