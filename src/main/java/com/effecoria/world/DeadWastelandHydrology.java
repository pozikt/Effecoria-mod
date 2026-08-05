package com.effecoria.world;

import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Dead Wasteland hydrology helpers.
 *
 * <p>Water drying / fluid rejection is currently <b>off</b> ({@link #DRYING_ENABLED}) —
 * lakes and ocean edges stay until we decide the final arid look. Re-enable by flipping
 * that flag and restoring the {@code wasteland_strip_water} biome modifier.
 *
 * <p>Never run bulk drying from {@code ChunkEvent.Load} — that cascades into neighbor
 * fluid updates and can OOM during {@code /locate biome}.
 */
public final class DeadWastelandHydrology {
    private DeadWastelandHydrology() {}

    /** Master switch — leave false until ocean/edge drying is redesigned. */
    public static final boolean DRYING_ENABLED = false;

    /** Clients only — avoid neighbor fluid schedules that re-enter drying. */
    private static final int QUIET_FLAGS = Block.UPDATE_CLIENTS;

    /** Chebyshev radius: leave water alone near non-wasteland neighbors (coasts / rivers). */
    private static final int BORDER_BUFFER = 3;

    private static final ThreadLocal<Boolean> DRYING = ThreadLocal.withInitial(() -> false);

    public static boolean isForbiddenWater(FluidState fluid) {
        if (fluid.isEmpty()) {
            return false;
        }
        if (fluid.is(FluidTags.WATER)) {
            return true;
        }
        return fluid.is(ModFluids.PHI_WATER.get())
                || fluid.is(ModFluids.PHI_WATER_FLOWING.get())
                || fluid.is(ModFluids.BLOOD.get())
                || fluid.is(ModFluids.BLOOD_FLOWING.get());
    }

    public static boolean isForbiddenWater(BlockState state) {
        if (isForbiddenWater(state.getFluidState())) {
            return true;
        }
        return state.hasProperty(BlockStateProperties.WATERLOGGED)
                && Boolean.TRUE.equals(state.getValue(BlockStateProperties.WATERLOGGED));
    }

    /**
     * True only deep inside the wasteland — not on the rim next to ocean / river / other biomes.
     * Worldgen strip and runtime drying both use this so sea edges stay continuous.
     */
    public static boolean isInteriorDryCell(LevelAccessor level, BlockPos pos) {
        if (!DRYING_ENABLED) {
            return false;
        }
        if (!DeadWastelandService.isBiome(level, pos)) {
            return false;
        }
        BlockPos.MutableBlockPos sample = new BlockPos.MutableBlockPos();
        for (int dx = -BORDER_BUFFER; dx <= BORDER_BUFFER; dx++) {
            for (int dz = -BORDER_BUFFER; dz <= BORDER_BUFFER; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                sample.set(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                if (!DeadWastelandService.isBiome(level, sample)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Replace water with air. Returns true if changed. Skips biome-border cells. */
    public static boolean dryAt(LevelAccessor level, BlockPos pos) {
        if (!DRYING_ENABLED || DRYING.get()) {
            return false;
        }
        if (!isInteriorDryCell(level, pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!isForbiddenWater(state)) {
            return false;
        }

        DRYING.set(true);
        try {
            if (state.hasProperty(BlockStateProperties.WATERLOGGED)
                    && Boolean.TRUE.equals(state.getValue(BlockStateProperties.WATERLOGGED))) {
                level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, false), QUIET_FLAGS);
                return true;
            }

            if (state.getBlock() instanceof LiquidBlock || !state.getFluidState().isEmpty()) {
                // Evaporate only — never sprinkle gravel/clay over the sand/ash surface.
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET_FLAGS);
                return true;
            }
        } finally {
            DRYING.set(false);
        }
        return false;
    }

    /** Surface palette sediment for carved dry channels (sand top / ash under). */
    public static BlockState riverbedSediment(RandomSource random) {
        return random.nextBoolean()
                ? ModBlocks.ASH_SOIL.get().defaultBlockState()
                : ModBlocks.PARCHED_SAND.get().defaultBlockState();
    }

    public static BlockState channelFloor() {
        return ModBlocks.ASH_SOIL.get().defaultBlockState();
    }

    public static BlockState channelBank() {
        return ModBlocks.PARCHED_SAND.get().defaultBlockState();
    }

    /**
     * Light local strip near a player already inside the wasteland.
     * Skips unloaded chunks; only touches cells that already hold water.
     */
    public static void dryAround(ServerLevel level, BlockPos center, int radiusXZ, int radiusY) {
        if (!DRYING_ENABLED || DRYING.get()) {
            return;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int changed = 0;
        final int budget = 64; // hard cap per call — never flood the tick
        for (int dx = -radiusXZ; dx <= radiusXZ && changed < budget; dx++) {
            for (int dz = -radiusXZ; dz <= radiusXZ && changed < budget; dz++) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                if (!level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
                    continue;
                }
                for (int dy = -radiusY; dy <= radiusY && changed < budget; dy++) {
                    cursor.set(x, center.getY() + dy, z);
                    FluidState fluid = level.getFluidState(cursor);
                    if (fluid.isEmpty()) {
                        BlockState state = level.getBlockState(cursor);
                        if (!state.hasProperty(BlockStateProperties.WATERLOGGED)
                                || !Boolean.TRUE.equals(state.getValue(BlockStateProperties.WATERLOGGED))) {
                            continue;
                        }
                    } else if (!isForbiddenWater(fluid)) {
                        continue;
                    }
                    if (dryAt(level, cursor)) {
                        changed++;
                    }
                }
            }
        }
    }

    /**
     * Reject a prospective fluid block placement inside the wasteland interior.
     * Border cells keep ocean/river water so the coastline does not collapse into a trench.
     */
    public static BlockState rejectOrDry(Level level, BlockPos pos, BlockState proposed) {
        if (!DRYING_ENABLED || !isInteriorDryCell(level, pos)) {
            return proposed;
        }
        if (!isForbiddenWater(proposed) && !isForbiddenWater(proposed.getFluidState())) {
            return proposed;
        }
        return Blocks.AIR.defaultBlockState();
    }

    public static boolean isVanillaWaterFluid(net.minecraft.world.level.material.Fluid fluid) {
        return fluid.isSame(Fluids.WATER) || fluid.isSame(Fluids.FLOWING_WATER);
    }
}
