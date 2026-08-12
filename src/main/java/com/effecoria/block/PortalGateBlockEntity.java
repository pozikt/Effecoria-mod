package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

import javax.annotation.Nullable;

/** Thin hyper-tunnel cell owned by a {@link PortalModulatorBlockEntity}. */
public final class PortalGateBlockEntity extends BlockEntity {
    private Optional<BlockPos> modulatorPos = Optional.empty();

    public PortalGateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PORTAL_GATE.get(), pos, state);
    }

    public void bindModulator(BlockPos modulator) {
        this.modulatorPos = Optional.of(modulator.immutable());
        setChanged();
    }

    public Optional<BlockPos> modulatorPos() {
        return modulatorPos;
    }

    public void tryTeleport(ServerLevel level, ServerPlayer player) {
        if (modulatorPos.isEmpty()) {
            return;
        }
        if (!(level.getBlockEntity(modulatorPos.get()) instanceof PortalModulatorBlockEntity mod)) {
            level.removeBlock(worldPosition, false);
            return;
        }
        if (!mod.isOpen() || mod.isPlayerOnCooldown(player)) {
            return;
        }
        mod.teleportPlayer(level, player);
    }

    public void onFilmBroken() {
        if (level == null || level.isClientSide() || modulatorPos.isEmpty()) {
            return;
        }
        if (level.getBlockEntity(modulatorPos.get()) instanceof PortalModulatorBlockEntity mod && mod.isOpen()) {
            mod.forceClose();
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PortalGateBlockEntity be) {
        if (be.modulatorPos.isEmpty()) {
            return;
        }
        if (level.getGameTime() % 20L != 0L) {
            return;
        }
        if (!(level.getBlockEntity(be.modulatorPos.get()) instanceof PortalModulatorBlockEntity mod) || !mod.isOpen()) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        modulatorPos.ifPresent(p -> tag.putLong("Modulator", p.asLong()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("Modulator")) {
            modulatorPos = Optional.of(BlockPos.of(tag.getLong("Modulator")));
        } else {
            modulatorPos = Optional.empty();
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
}
