package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.alchemy.PhiBusNetwork;
import com.effecoria.core.circuit.PhiChannel;
import com.effecoria.core.circuit.PhiTuned;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** Stamps ω on the island; accumulates Ω from frequency mismatch. */
public final class PhiCouplerBlockEntity extends BlockEntity implements PhiTuned {
    public static final float OMEGA_MAX = 100f;

    private float omega;
    private int refreshCooldown;

    public PhiCouplerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHI_COUPLER.get(), pos, state);
    }

    @Override
    public PhiChannel phiChannel() {
        return getBlockState().getValue(PhiCouplerBlock.CHANNEL);
    }

    public void setChannel(PhiChannel channel) {
        setChanged();
    }

    public float omegaPercent() {
        return omega;
    }

    public void addOmega(float amount) {
        if (amount <= 0f) {
            return;
        }
        omega = Mth.clamp(omega + amount, 0f, OMEGA_MAX);
        setChanged();
    }

    public boolean clearOmegaMeter() {
        if (omega <= 0.01f) {
            return false;
        }
        omega = 0f;
        setChanged();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PhiCouplerBlockEntity be) {
        if (be.refreshCooldown > 0) {
            be.refreshCooldown--;
            return;
        }
        be.refreshCooldown = 10;
        PhiBusNetwork.findSource(level, pos);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putFloat("Omega", omega);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        omega = tag.getFloat("Omega");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        saveAdditional(tag, provider);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
