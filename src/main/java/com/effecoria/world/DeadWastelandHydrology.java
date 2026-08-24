package com.effecoria.world;

import com.effecoria.content.ModBiomeTags;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
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
 * <p>Dry inland lakes only. Desert climate (which this biome replaces) keeps a wide
 * continental shelf that is still {@code dead_wasteland} — a 3-block biome-border
 * ring cannot see the ocean and carves a sponge trench. Coast detection samples
 * noise biomes far out for ocean / beach / river.
 *
 * <p>Never run bulk drying from {@code ChunkEvent.Load}. Never call {@code getBiome}
 * during worldgen (neighbor blend crashes WorldGenRegion).
 */
public final class DeadWastelandHydrology {
    private DeadWastelandHydrology() {}

    /** Inland strip + bucket/fluid reject + player-local seepage. */
    public static final boolean DRYING_ENABLED = true;

    /** Clients only — avoid neighbor fluid schedules that re-enter drying. */
    private static final int QUIET_FLAGS = Block.UPDATE_CLIENTS;

    /** Land–land rim (wasteland vs vitrified / plains). Not enough for ocean shelves. */
    private static final int BORDER_BUFFER = 3;

    /** How far to look for ocean/beach/river climate (block distance). */
    private static final int[] COAST_RINGS = {16, 32, 48, 64, 80};

    private static final int[] COAST_DX = {1, 1, 0, -1, -1, -1, 0, 1};
    private static final int[] COAST_DZ = {0, 1, 1, 1, 0, -1, -1, -1};

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
     * True only deep inland — not on a land biome rim and not on an ocean shelf.
     */
    public static boolean isInteriorDryCell(LevelAccessor level, BlockPos pos) {
        if (!DRYING_ENABLED) {
            return false;
        }
        if (!isWastelandNoise(level, pos.getX(), pos.getY(), pos.getZ())) {
            return false;
        }
        int b = BORDER_BUFFER;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (!isWastelandNoise(level, x - b, y, z - b)
                || !isWastelandNoise(level, x - b, y, z + b)
                || !isWastelandNoise(level, x + b, y, z - b)
                || !isWastelandNoise(level, x + b, y, z + b)) {
            return false;
        }
        return !isNearCoastalBiome(level, x, z);
    }

    /** Ocean / beach / river climate within {@link #COAST_RINGS} — leave the water. */
    private static boolean isNearCoastalBiome(LevelAccessor level, int x, int z) {
        if (isCoastalNoise(level, x, z)) {
            return true;
        }
        for (int ring : COAST_RINGS) {
            for (int i = 0; i < COAST_DX.length; i++) {
                if (isCoastalNoise(level, x + COAST_DX[i] * ring, z + COAST_DZ[i] * ring)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isWastelandNoise(LevelAccessor level, int x, int y, int z) {
        Holder<Biome> biome = noiseBiome(level, x, y, z);
        return biome != null && biome.is(ModBiomeTags.DEAD_WASTELAND);
    }

    private static boolean isCoastalNoise(LevelAccessor level, int x, int z) {
        Holder<Biome> biome = noiseBiome(level, x, 63, z);
        if (biome == null) {
            return false;
        }
        return biome.is(BiomeTags.IS_OCEAN)
                || biome.is(BiomeTags.IS_BEACH)
                || biome.is(BiomeTags.IS_RIVER);
    }

    /**
     * Noise-map biome — no chunk load, safe during feature generation.
     */
    private static Holder<Biome> noiseBiome(LevelAccessor level, int x, int y, int z) {
        ServerLevel server = serverOf(level);
        if (server == null) {
            return null;
        }
        return server.getChunkSource()
                .getGenerator()
                .getBiomeSource()
                .getNoiseBiome(
                        QuartPos.fromBlock(x),
                        QuartPos.fromBlock(y),
                        QuartPos.fromBlock(z),
                        server.getChunkSource().randomState().sampler());
    }

    private static ServerLevel serverOf(LevelAccessor level) {
        if (level instanceof ServerLevel server) {
            return server;
        }
        if (level instanceof WorldGenLevel worldGen) {
            return worldGen.getLevel();
        }
        return null;
    }

    /** Skip proto-chunks and neighbors that would force extra generation (runtime only). */
    public static boolean chunkReadyForHydrology(LevelAccessor level, int x, int z) {
        int cx = SectionPos.blockToSectionCoord(x);
        int cz = SectionPos.blockToSectionCoord(z);
        if (level instanceof ServerLevel server) {
            return server.getChunkSource().getChunkNow(cx, cz) != null;
        }
        return level.hasChunk(cx, cz);
    }

    /** Replace water with air. Returns true if changed. Skips coasts and biome-border cells. */
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
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET_FLAGS);
                return true;
            }
        } finally {
            DRYING.set(false);
        }
        return false;
    }

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
        final int budget = 64;
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
     * Coasts keep ocean water so the shoreline does not collapse into a trench.
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
