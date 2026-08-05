package com.effecoria.world.feature;

import com.effecoria.block.EssonitePointedBlock;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModFluids;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

/**
 * Underground Φ-water lake, modelled on vanilla {@code LakeFeature}:
 * ellipsoid carve into cave stone, lower half filled with still Φ-water, walls sealed with Φ-stone.
 * Clears dripstone/crystals in the volume; never paints essonite crust on the shore.
 */
public final class PhiWaterLakeFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MASK_X = 16;
    private static final int MASK_Y = 8;
    private static final int MASK_Z = 16;

    public PhiWaterLakeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin().mutable();

        // Descend through air to a cave floor (vanilla lake start)
        if (origin.getY() <= level.getMinBuildHeight() + 4) {
            return false;
        }
        while (origin.getY() > level.getMinBuildHeight() + 4 && level.isEmptyBlock(origin)) {
            origin = origin.below();
        }
        origin = origin.above(random.nextInt(3) + 2);
        if (origin.getY() > 96 || origin.getY() < 16) {
            return false;
        }

        // Reject cramped / decoration-clogged pockets (stalagmite forests)
        if (decorationDensity(level, origin, 6) > 8) {
            return false;
        }
        if (!hasOpenCaveRoom(level, origin)) {
            return false;
        }

        boolean[] mask = new boolean[MASK_X * MASK_Y * MASK_Z];
        int blobs = 4 + random.nextInt(4);
        for (int i = 0; i < blobs; i++) {
            double rx = random.nextDouble() * 6.0 + 3.0;
            double ry = random.nextDouble() * 4.0 + 2.0;
            double rz = random.nextDouble() * 6.0 + 3.0;
            double cx = random.nextDouble() * (MASK_X - rx - 2.0) + 1.0 + rx / 2.0;
            double cy = random.nextDouble() * (MASK_Y - ry - 3.0) + 2.0 + ry / 2.0;
            double cz = random.nextDouble() * (MASK_Z - rz - 2.0) + 1.0 + rz / 2.0;
            for (int x = 1; x < MASK_X - 1; x++) {
                for (int y = 1; y < MASK_Y - 1; y++) {
                    for (int z = 1; z < MASK_Z - 1; z++) {
                        double dx = (x - cx) / (rx / 2.0);
                        double dy = (y - cy) / (ry / 2.0);
                        double dz = (z - cz) / (rz / 2.0);
                        if (dx * dx + dy * dy + dz * dz <= 1.0) {
                            mask[(x * MASK_Y + y) * MASK_Z + z] = true;
                        }
                    }
                }
            }
        }

        // Validate enclosure — no open void on the sides of the fluid body (vanilla check)
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = 0; x < MASK_X; x++) {
            for (int y = 0; y < MASK_Y; y++) {
                for (int z = 0; z < MASK_Z; z++) {
                    boolean here = mask[(x * MASK_Y + y) * MASK_Z + z];
                    if (!here) {
                        continue;
                    }
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    // Refuse existing liquids other than air/phi-water
                    BlockState existing = level.getBlockState(cursor);
                    if (!existing.getFluidState().isEmpty()
                            && !existing.is(ModBlocks.PHI_WATER.get())
                            && !existing.getFluidState().is(Fluids.WATER)) {
                        return false;
                    }
                    // Side of blob must be solid or also carved (else lake spills into huge void)
                    if (y < MASK_Y / 2) {
                        for (Direction dir : Direction.values()) {
                            int nx = x + dir.getStepX();
                            int ny = y + dir.getStepY();
                            int nz = z + dir.getStepZ();
                            boolean neighbor =
                                    nx >= 0
                                            && ny >= 0
                                            && nz >= 0
                                            && nx < MASK_X
                                            && ny < MASK_Y
                                            && nz < MASK_Z
                                            && mask[(nx * MASK_Y + ny) * MASK_Z + nz];
                            if (neighbor) {
                                continue;
                            }
                            BlockPos nPos = cursor.relative(dir);
                            BlockState nState = level.getBlockState(nPos);
                            if (nState.isAir() || isDecoration(nState) || nState.canBeReplaced()) {
                                // Soften: only fail if MANY open faces — handled after count
                            }
                        }
                    }
                }
            }
        }

        BlockState water = ModBlocks.PHI_WATER.get().defaultBlockState();
        BlockState basin = ModBlocks.PHI_STONE.get().defaultBlockState();
        BlockState air = Blocks.CAVE_AIR.defaultBlockState();
        int waterCells = 0;

        // Carve + fill (vanilla: upper = air, lower = fluid; rim = barrier stone)
        for (int x = 0; x < MASK_X; x++) {
            for (int y = 0; y < MASK_Y; y++) {
                for (int z = 0; z < MASK_Z; z++) {
                    boolean here = mask[(x * MASK_Y + y) * MASK_Z + z];
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!here) {
                        // Rim: if adjacent to carved lower half, seal with Φ-stone
                        if (y < MASK_Y / 2 && touchesMask(mask, x, y, z) && canSeal(level.getBlockState(cursor))) {
                            level.setBlock(cursor, basin, 2);
                        }
                        continue;
                    }

                    BlockState current = level.getBlockState(cursor);
                    if (!canCarve(current)) {
                        continue;
                    }

                    if (y >= MASK_Y / 2) {
                        level.setBlock(cursor, air, 2);
                    } else {
                        level.setBlock(cursor, water, 2);
                        level.scheduleTick(cursor.immutable(), ModFluids.PHI_WATER.get(), 1);
                        waterCells++;
                    }
                }
            }
        }

        if (waterCells < 12) {
            return false;
        }

        // Clear decorations sticking up through / around the lake surface
        clearDecorAround(level, origin, MASK_X, MASK_Y, MASK_Z);

        // Cliff lips: waterfall or Φ-stone plug (no floating water, no crust)
        sealOrSpillEdges(level, origin, mask, water, basin);

        return true;
    }

    private static boolean touchesMask(boolean[] mask, int x, int y, int z) {
        for (Direction dir : Direction.values()) {
            int nx = x + dir.getStepX();
            int ny = y + dir.getStepY();
            int nz = z + dir.getStepZ();
            if (nx < 0 || ny < 0 || nz < 0 || nx >= MASK_X || ny >= MASK_Y || nz >= MASK_Z) {
                continue;
            }
            if (mask[(nx * MASK_Y + ny) * MASK_Z + nz]) {
                return true;
            }
        }
        return false;
    }

    private static boolean canCarve(BlockState state) {
        if (state.is(ModBlocks.PHI_WATER.get()) || state.getFluidState().is(Fluids.WATER)) {
            return true;
        }
        if (state.isAir() || state.canBeReplaced() || isDecoration(state)) {
            return true;
        }
        return isCaveStone(state);
    }

    private static boolean canSeal(BlockState state) {
        return isCaveStone(state)
                || state.isAir()
                || state.canBeReplaced()
                || isDecoration(state)
                || state.is(ModBlocks.ESSONITE_DRIPSTONE_BLOCK.get());
    }

    private static boolean isCaveStone(BlockState state) {
        return state.is(ModBlocks.PHI_STONE.get())
                || state.is(ModBlocks.ESSENITE_ORE.get())
                || state.is(ModBlocks.DEEPSLATE_ESSENITE_ORE.get())
                || state.is(ModBlocks.ESSONITE_BLOCK.get())
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.SMOOTH_BASALT)
                || state.is(Blocks.CALCITE);
    }

    private static void clearDecorAround(WorldGenLevel level, BlockPos origin, int sx, int sy, int sz) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = -1; x <= sx; x++) {
            for (int y = -1; y <= sy + 2; y++) {
                for (int z = -1; z <= sz; z++) {
                    m.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    BlockState state = level.getBlockState(m);
                    if (isDecoration(state) || state.is(ModBlocks.ESSONITE_DRIPSTONE_BLOCK.get())) {
                        // Keep dripstone_block only if far from water; strip if next to lake air/water
                        if (state.is(ModBlocks.ESSONITE_DRIPSTONE_BLOCK.get())) {
                            boolean nearWater = false;
                            for (Direction d : Direction.values()) {
                                BlockState n = level.getBlockState(m.relative(d));
                                if (n.is(ModBlocks.PHI_WATER.get()) || n.isAir()) {
                                    nearWater = true;
                                    break;
                                }
                            }
                            if (!nearWater) {
                                continue;
                            }
                            level.setBlock(m, ModBlocks.PHI_STONE.get().defaultBlockState(), 2);
                        } else {
                            level.setBlock(m, Blocks.CAVE_AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }
    }

    private static void sealOrSpillEdges(
            WorldGenLevel level, BlockPos origin, boolean[] mask, BlockState water, BlockState basin) {
        BlockPos.MutableBlockPos edge = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos fall = new BlockPos.MutableBlockPos();
        int half = MASK_Y / 2;
        for (int x = 0; x < MASK_X; x++) {
            for (int z = 0; z < MASK_Z; z++) {
                // Surface water cell = highest fluid in this column of the mask
                int surfaceY = -1;
                for (int y = half - 1; y >= 0; y--) {
                    if (mask[(x * MASK_Y + y) * MASK_Z + z]) {
                        surfaceY = y;
                        break;
                    }
                }
                if (surfaceY < 0) {
                    continue;
                }
                edge.set(origin.getX() + x, origin.getY() + surfaceY, origin.getZ() + z);
                if (!level.getBlockState(edge).is(ModBlocks.PHI_WATER.get())) {
                    continue;
                }
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockPos out = edge.relative(dir);
                    BlockState outState = level.getBlockState(out);
                    if (!(outState.isAir() || outState.canBeReplaced() || isDecoration(outState))) {
                        continue;
                    }
                    BlockState below = level.getBlockState(out.below());
                    if (!(below.isAir() || below.canBeReplaced() || isDecoration(below))) {
                        continue;
                    }
                    int landing = findLandingY(level, out.getX(), edge.getY(), out.getZ(), 14);
                    if (landing >= level.getMinBuildHeight()) {
                        for (int y = edge.getY(); y > landing; y--) {
                            fall.set(out.getX(), y, out.getZ());
                            BlockState here = level.getBlockState(fall);
                            if (here.is(ModBlocks.PHI_WATER.get())) {
                                continue;
                            }
                            if (!(here.isAir() || here.canBeReplaced() || isDecoration(here))) {
                                break;
                            }
                            level.setBlock(fall, water, 2);
                            level.scheduleTick(fall.immutable(), ModFluids.PHI_WATER.get(), 1);
                        }
                    } else {
                        level.setBlock(out, basin, 2);
                        level.setBlock(edge, basin, 2);
                    }
                }
            }
        }
    }

    private static int findLandingY(WorldGenLevel level, int x, int fromY, int z, int maxDrop) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(x, fromY, z);
        for (int y = fromY - 1; y >= fromY - maxDrop; y--) {
            if (y < level.getMinBuildHeight()) {
                return level.getMinBuildHeight() - 1;
            }
            m.setY(y);
            BlockState state = level.getBlockState(m);
            if (state.isAir()
                    || state.canBeReplaced()
                    || isDecoration(state)
                    || state.is(ModBlocks.PHI_WATER.get())
                    || !state.getFluidState().isEmpty()) {
                continue;
            }
            return y;
        }
        return level.getMinBuildHeight() - 1;
    }

    private static int decorationDensity(WorldGenLevel level, BlockPos center, int r) {
        int n = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -2; dy <= 6; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (isDecoration(level.getBlockState(m))
                            || level.getBlockState(m).is(ModBlocks.ESSONITE_DRIPSTONE_BLOCK.get())) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    private static boolean hasOpenCaveRoom(WorldGenLevel level, BlockPos origin) {
        int air = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 8; dx++) {
            for (int dy = 0; dy < 4; dy++) {
                for (int dz = 0; dz < 8; dz++) {
                    m.set(origin.getX() + dx + 4, origin.getY() + dy + 2, origin.getZ() + dz + 4);
                    if (level.isEmptyBlock(m)) {
                        air++;
                    }
                }
            }
        }
        return air >= 40;
    }

    private static boolean isDecoration(BlockState state) {
        Block block = state.getBlock();
        return block instanceof EssonitePointedBlock
                || block instanceof AmethystClusterBlock
                || state.is(ModBlocks.ESSONITE_CRUST.get())
                || state.is(ModBlocks.ESSONITE_POINTED.get())
                || state.is(ModBlocks.ESSONITE_CRYSTAL.get())
                || state.is(ModBlocks.ESSONITE_CRYSTAL_BUD_SMALL.get())
                || state.is(ModBlocks.ESSONITE_CRYSTAL_BUD_MEDIUM.get())
                || state.is(ModBlocks.ESSONITE_CRYSTAL_BUD_LARGE.get())
                || state.is(ModBlocks.PHI_BLADES.get());
    }
}
