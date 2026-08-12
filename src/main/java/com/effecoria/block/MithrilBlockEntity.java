package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.alchemy.PhiBusNetwork;
import com.effecoria.core.alchemy.PhiPowerProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Energized mithril — local PhiPowerProvider (radius 1), same role as Φ-bus outlets. */
public final class MithrilBlockEntity extends BlockEntity implements PhiPowerProvider {
    public static final int POWER_RADIUS = 1;

    private boolean energized;
    private float cachedFactor;
    private int refreshCooldown;

    public MithrilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MITHRIL_BLOCK.get(), pos, state);
    }

    public void markDirtyNetwork() {
        refreshCooldown = 0;
    }

    @Override
    public boolean supplying() {
        return energized && cachedFactor > 0.01f;
    }

    @Override
    public int radius() {
        return POWER_RADIUS;
    }

    @Override
    public float powerFactor() {
        return supplying() ? cachedFactor : 0f;
    }

    @Override
    public boolean drainFuel(int ticks) {
        if (!supplying() || ticks <= 0 || level == null) {
            return false;
        }
        PhiBusNetwork.Source source = PhiBusNetwork.findSource(level, worldPosition);
        if (source == null || source.injector() == null) {
            return false;
        }
        return source.injector().drainFuel(ticks);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MithrilBlockEntity be) {
        if (be.refreshCooldown > 0) {
            be.refreshCooldown--;
            return;
        }
        be.refreshCooldown = 10;
        PhiBusNetwork.Source source = PhiBusNetwork.findSource(level, pos);
        boolean nextEnergized = source != null;
        float nextFactor = source != null ? source.powerFactor() : 0f;

        if (nextEnergized != be.energized || Math.abs(nextFactor - be.cachedFactor) > 0.01f) {
            be.energized = nextEnergized;
            be.cachedFactor = nextFactor;
            be.setChanged();
        }
        if (state.hasProperty(MithrilBlock.POWERED) && state.getValue(MithrilBlock.POWERED) != nextEnergized) {
            level.setBlock(pos, state.setValue(MithrilBlock.POWERED, nextEnergized), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putBoolean("Energized", energized);
        tag.putFloat("Factor", cachedFactor);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        energized = tag.getBoolean("Energized");
        cachedFactor = tag.getFloat("Factor");
    }
}
