package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Cooldown + last-scan clock for the tower Φ-sonar emitter. */
public final class PhiSonarBlockEntity extends BlockEntity {
    public static final int SCAN_COOLDOWN_TICKS = 160; // 8s

    private int cooldownTicks;
    private long lastScanGameTime = -1L;

    public PhiSonarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHI_SONAR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PhiSonarBlockEntity be) {
        if (be.cooldownTicks > 0) {
            be.cooldownTicks--;
            if (be.cooldownTicks == 0) {
                be.setChanged();
            }
        }
    }

    public boolean ready() {
        return cooldownTicks <= 0;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public long lastScanGameTime() {
        return lastScanGameTime;
    }

    /** Marks a successful scan and starts the cooldown. */
    public void markScanned(long gameTime) {
        lastScanGameTime = gameTime;
        cooldownTicks = SCAN_COOLDOWN_TICKS;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("Cooldown", cooldownTicks);
        tag.putLong("LastScan", lastScanGameTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        cooldownTicks = Math.max(0, tag.getInt("Cooldown"));
        lastScanGameTime = tag.contains("LastScan") ? tag.getLong("LastScan") : -1L;
    }
}
