package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.alchemy.PhiBusNetwork;
import com.effecoria.core.alchemy.PhiPowerProvider;
import com.effecoria.core.circuit.PhiChannel;
import com.effecoria.core.circuit.PhiTuned;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Stores ΔQ_Φ. Charges from an island injector; supplies radius-1 like a bus outlet. */
public final class PhiAccumulatorBlockEntity extends BlockEntity implements PhiPowerProvider, PhiTuned {
    public static final int MAX_CHARGE = 2400;
    public static final int POWER_RADIUS = 1;

    private int charge;
    private float omega;
    private int chargeCooldown;

    public PhiAccumulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHI_ACCUMULATOR.get(), pos, state);
    }

    public int charge() {
        return charge;
    }

    public int chargePercent() {
        return Math.round(100f * charge / (float) MAX_CHARGE);
    }

    /**
     * Forced ΔQ discharge for Phoenix inrush — bypasses UPS {@link #supplying()} gate.
     * @return amount actually removed
     */
    public int takeCharge(int amount) {
        if (amount <= 0 || charge <= 0) {
            return 0;
        }
        int taken = Math.min(amount, charge);
        charge -= taken;
        setChanged();
        return taken;
    }

    public float omegaPercent() {
        return omega;
    }

    public void addOmega(float amount) {
        if (amount <= 0f) {
            return;
        }
        omega = Mth.clamp(omega + amount, 0f, 100f);
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

    /**
     * Must not call {@link PhiBusNetwork#findSource} — the BFS treats this BE as
     * {@link PhiTuned} and would recurse until StackOverflow.
     * Island ω comes from adjacent couplers during network scan.
     */
    @Override
    public PhiChannel phiChannel() {
        return PhiChannel.BROADBAND;
    }

    /**
     * UPS mode: radiate to neighbors only when the island has no live reactor/injector.
     * Otherwise an adjacent crusher prefers this buffer, empties it in a few ticks, then
     * the buffer recharges — visible as on/off flicker.
     */
    @Override
    public boolean supplying() {
        if (charge <= 0 || omega >= 95f || level == null) {
            return false;
        }
        PhiBusNetwork.Source island = PhiBusNetwork.findSource(level, worldPosition, false);
        return island == null || island.injector() == null || !island.injector().supplying();
    }

    @Override
    public int radius() {
        return POWER_RADIUS;
    }

    @Override
    public float powerFactor() {
        if (!supplying()) {
            return 0f;
        }
        return 0.7f + 0.5f * (charge / (float) MAX_CHARGE);
    }

    @Override
    public boolean drainFuel(int ticks) {
        if (!supplying() || ticks <= 0 || charge < ticks) {
            return false;
        }
        charge -= ticks;
        setChanged();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PhiAccumulatorBlockEntity be) {
        if (be.chargeCooldown > 0) {
            be.chargeCooldown--;
            return;
        }
        be.chargeCooldown = 5;
        if (be.charge >= MAX_CHARGE) {
            return;
        }
        PhiBusNetwork.Source source = PhiBusNetwork.findSource(level, pos, false);
        if (source == null || source.injector() == null || source.injector() == be) {
            return;
        }
        if (source.injector().drainFuel(4)) {
            be.charge = Math.min(MAX_CHARGE, be.charge + 4);
            be.setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("Charge", charge);
        tag.putFloat("Omega", omega);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        charge = tag.getInt("Charge");
        omega = tag.getFloat("Omega");
    }
}
