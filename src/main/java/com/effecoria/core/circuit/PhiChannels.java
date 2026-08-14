package com.effecoria.core.circuit;

import com.effecoria.block.PhiAccumulatorBlockEntity;
import com.effecoria.block.PhiCouplerBlockEntity;
import com.effecoria.content.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Maps machines to preferred Φ channels when they do not implement {@link PhiTuned}. */
public final class PhiChannels {
    private PhiChannels() {}

    public static PhiChannel ofDevice(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return PhiChannel.BROADBAND;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PhiTuned tuned) {
            return tuned.phiChannel();
        }
        return ofBlock(level.getBlockState(pos));
    }

    public static PhiChannel ofBlock(BlockState state) {
        if (state.is(ModBlocks.REGEN_CHAMBER.get())
                || state.is(ModBlocks.REGEN_CHAMBER_PART.get())
                || state.is(ModBlocks.PSI_IMPRINTER.get())
                || state.is(ModBlocks.OMEGA_DAMPER.get())
                || state.is(ModBlocks.PHI_AIR_SYNTH.get())
                || state.is(ModBlocks.PHI_WATER_PURIFIER.get())) {
            return PhiChannel.LIFE;
        }
        if (state.is(ModBlocks.TOWER_ANCHOR.get())
                || state.is(ModBlocks.TOWER_CONSOLE.get())
                || state.is(ModBlocks.PHI_TELEGRAPH.get())
                || state.is(ModBlocks.PHI_SONAR.get())
                || state.is(ModBlocks.PHI_CARTOGRAPHY_TABLE.get())) {
            return PhiChannel.PSI;
        }
        if (state.is(ModBlocks.TURRET_MOUNT.get())
                || state.is(ModBlocks.PLASMA_TURRET.get())
                || state.is(ModBlocks.KINETIC_TURRET.get())
                || state.is(ModBlocks.MENTAL_TURRET.get())
                || state.is(ModBlocks.SPATIAL_TURRET.get())
                || state.is(ModBlocks.OMEGA_TURRET.get())) {
            return PhiChannel.DEFENSE;
        }
        if (state.is(ModBlocks.PHI_CRUSHER.get())
                || state.is(ModBlocks.PHI_CRUSHER_HOPPER.get())
                || state.is(ModBlocks.CLIMATE_ARRAY.get())
                || state.is(ModBlocks.PORTAL_MODULATOR.get())) {
            return PhiChannel.INDUSTRY;
        }
        return PhiChannel.BROADBAND;
    }

    public static void leakOmega(Level level, BlockPos near, float amount) {
        if (level == null || amount <= 0f) {
            return;
        }
        BlockEntity be = level.getBlockEntity(near);
        if (be instanceof PhiCouplerBlockEntity coupler) {
            coupler.addOmega(amount);
            return;
        }
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            BlockEntity adj = level.getBlockEntity(near.relative(dir));
            if (adj instanceof PhiCouplerBlockEntity coupler) {
                coupler.addOmega(amount);
                return;
            }
            if (adj instanceof PhiAccumulatorBlockEntity acc) {
                acc.addOmega(amount);
                return;
            }
        }
    }
}
