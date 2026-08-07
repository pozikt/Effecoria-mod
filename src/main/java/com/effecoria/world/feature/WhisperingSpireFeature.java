package com.effecoria.world.feature;

import com.effecoria.content.ModBiomeTags;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItems;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Rare Whispering Spire: hollow truncated cone embedded into plateau rock with a recessed caldera.
 * Placement is bounded and shell-based to avoid watchdog timeouts and out-of-height crashes.
 */
public final class WhisperingSpireFeature extends Feature<NoneFeatureConfiguration> {
    /** Max horizontal biome ring — must stay within worldgen-accessible chunks (~1 chunk). */
    private static final int BIOME_MARGIN = 16;
    private static final int MAX_FOOTPRINT_RELIEF = 14;
    private static final int EMBED_DEPTH = 8;
    /** Worldgen bulk writes — no neighbor block updates (avoids cascade lag). */
    private static final int GEN_FLAGS = Block.UPDATE_CLIENTS;

    public WhisperingSpireFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        if (!isPlateau(level, origin)) {
            return false;
        }
        if (!isPlateauInterior(level, origin.getX(), origin.getZ(), BIOME_MARGIN)) {
            return false;
        }

        int height = 28 + random.nextInt(13);
        int baseR = 10 + random.nextInt(5);
        int calderaR = 4 + random.nextInt(2);
        int rimR = calderaR + 2;
        int bowlDepth = 5;
        int throatDepth = 4;
        int scanR = baseR + 2;

        int minSurf = Integer.MAX_VALUE;
        int maxSurf = Integer.MIN_VALUE;
        int sumSurf = 0;
        int samples = 0;
        for (int dx = -scanR; dx <= scanR; dx += 2) {
            for (int dz = -scanR; dz <= scanR; dz += 2) {
                if (dx * dx + dz * dz > scanR * scanR) {
                    continue;
                }
                int sx = origin.getX() + dx;
                int sz = origin.getZ() + dz;
                if (!isPlateau(level, new BlockPos(sx, origin.getY(), sz))) {
                    return false;
                }
                int sy = surfaceY(level, sx, sz);
                minSurf = Math.min(minSurf, sy);
                maxSurf = Math.max(maxSurf, sy);
                sumSurf += sy;
                samples++;
            }
        }
        if (samples < 8) {
            return false;
        }
        if (maxSurf - minSurf > MAX_FOOTPRINT_RELIEF) {
            return false;
        }
        int centerSurf = surfaceY(level, origin.getX(), origin.getZ());
        int avgSurf = sumSurf / samples;
        if (centerSurf < avgSurf - 3 || centerSurf < maxSurf - 6) {
            return false;
        }

        int baseY = minSurf - EMBED_DEPTH;
        int topY = baseY + height + 3;
        int minY = level.getMinBuildHeight();
        int maxYWorld = level.getMaxBuildHeight() - 1;
        if (baseY < minY + 4 || topY > maxYWorld) {
            return false;
        }

        BlockPos base = new BlockPos(origin.getX(), baseY, origin.getZ());

        if (!structureFootprintAccessible(level, base, baseR + 2)) {
            return false;
        }

        BlockState essonite = ModBlocks.ESSONITE_BLOCK.get().defaultBlockState();
        BlockState voidObs = ModBlocks.VOID_OBSIDIAN.get().defaultBlockState();
        BlockState star = ModBlocks.STAR_ESSONITE_BLOCK.get().defaultBlockState();
        BlockState phiStone = ModBlocks.PHI_STONE.get().defaultBlockState();
        BlockState crystal = ModBlocks.ESSONITE_CRYSTAL.get()
                .defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.UP);

        int floorY = height - bowlDepth;
        int ventY = floorY - throatDepth;
        int maxY = height;

        // Light foundation skirt (replaceable blocks only)
        for (int dx = -baseR; dx <= baseR; dx++) {
            for (int dz = -baseR; dz <= baseR; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > baseR + 0.4) {
                    continue;
                }
                int wx = base.getX() + dx;
                int wz = base.getZ() + dz;
                int surf = surfaceY(level, wx, wz);
                int fillTop = Math.min(surf, baseY + EMBED_DEPTH);
                for (int wy = baseY; wy <= fillTop; wy++) {
                    BlockPos p = new BlockPos(wx, wy, wz);
                    if (!inWorld(level, p)) {
                        continue;
                    }
                    BlockState cur = level.getBlockState(p);
                    if (canReplaceForFoundation(cur)) {
                        safeSet(
                                level,
                                p,
                                dist > baseR - 1.5 ? voidObs : phiStone,
                                GEN_FLAGS);
                    }
                }
            }
        }

        // Hollow cone shell (not solid fill — prevents 30k+ setBlock watchdog kills)
        for (int y = 0; y <= maxY; y++) {
            float t = y / (float) Math.max(1, maxY);
            float radius = Mth.lerp(t, baseR, rimR);
            int r = Math.max(rimR, Math.round(radius));

            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > r + 0.35) {
                        continue;
                    }
                    boolean shell = dist > r - 1.25;
                    boolean corePlug = y < 3 && dist < 2.5;
                    if (!shell && !corePlug) {
                        continue;
                    }
                    BlockPos p = base.offset(dx, y, dz);
                    if (!inWorld(level, p)) {
                        continue;
                    }
                    BlockState material;
                    if (y >= floorY && dist <= rimR + 0.5) {
                        if (dist >= calderaR - 0.2 && dist <= rimR + 0.2) {
                            material = random.nextFloat() < 0.65f ? star : essonite;
                        } else if ((y + dx + dz) % 4 == 0) {
                            material = voidObs;
                        } else {
                            material = essonite;
                        }
                    } else if ((y + dx + dz) % 5 == 0) {
                        material = voidObs;
                    } else if (shell && random.nextFloat() < 0.18f) {
                        material = voidObs;
                    } else if (y < EMBED_DEPTH && dist < r - 1) {
                        material = phiStone;
                    } else {
                        material = essonite;
                    }
                    safeSet(level, p, material, GEN_FLAGS);

                    if (shell && y > height / 3 && y < floorY - 1 && random.nextFloat() < 0.035f) {
                        BlockPos above = p.above();
                        if (inWorld(level, above) && level.getBlockState(above).isAir()) {
                            safeSet(level, above, crystal, GEN_FLAGS);
                        }
                    }
                }
            }
        }

        for (int y = floorY + 1; y <= maxY + 2; y++) {
            for (int dx = -calderaR; dx <= calderaR; dx++) {
                for (int dz = -calderaR; dz <= calderaR; dz++) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist < calderaR - 0.15) {
                        BlockPos p = base.offset(dx, y, dz);
                        if (inWorld(level, p)) {
                            safeSet(level, p, Blocks.AIR.defaultBlockState(), GEN_FLAGS);
                        }
                    }
                }
            }
        }

        for (int dx = -calderaR; dx <= calderaR; dx++) {
            for (int dz = -calderaR; dz <= calderaR; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist >= calderaR - 0.15) {
                    continue;
                }
                BlockPos floor = base.offset(dx, floorY, dz);
                if (inWorld(level, floor)) {
                    safeSet(level, floor, dist <= 2.2 ? voidObs : star, GEN_FLAGS);
                }
            }
        }

        for (int dx = -rimR; dx <= rimR; dx++) {
            for (int dz = -rimR; dz <= rimR; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist >= calderaR - 0.3 && dist <= rimR + 0.35) {
                    BlockPos rim = base.offset(dx, maxY, dz);
                    if (inWorld(level, rim)) {
                        safeSet(level, rim, star, GEN_FLAGS);
                    }
                    if (dist >= calderaR + 0.4 && random.nextFloat() < 0.35f) {
                        BlockPos tip = base.offset(dx, maxY + 1, dz);
                        if (inWorld(level, tip) && level.getBlockState(tip).isAir()) {
                            safeSet(level, tip, crystal, GEN_FLAGS);
                        }
                    }
                }
            }
        }

        int throatR = 2;
        for (int y = ventY; y <= floorY; y++) {
            for (int dx = -throatR; dx <= throatR; dx++) {
                for (int dz = -throatR; dz <= throatR; dz++) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    BlockPos p = base.offset(dx, y, dz);
                    if (!inWorld(level, p)) {
                        continue;
                    }
                    if (dist <= 1.15) {
                        safeSet(
                                level,
                                p,
                                y > ventY ? Blocks.AIR.defaultBlockState() : voidObs,
                                GEN_FLAGS);
                    } else if (dist <= throatR + 0.2) {
                        safeSet(level, p, voidObs, GEN_FLAGS);
                    }
                }
            }
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx * dx + dz * dz <= 1) {
                    BlockPos mouth = base.offset(dx, floorY, dz);
                    if (inWorld(level, mouth)) {
                        safeSet(level, mouth, Blocks.AIR.defaultBlockState(), GEN_FLAGS);
                    }
                    if ((dx != 0 || dz != 0) && inWorld(level, base.offset(dx, floorY - 1, dz))) {
                        safeSet(level, base.offset(dx, floorY - 1, dz), voidObs, GEN_FLAGS);
                    }
                }
            }
        }

        BlockPos ventPos = base.offset(0, ventY + 1, 0);
        if (!inWorld(level, ventPos)) {
            return false;
        }
        safeSet(level, base.offset(0, ventY, 0), voidObs, GEN_FLAGS);
        safeSet(level, ventPos, ModBlocks.WHISPERING_SPIRE_VENT.get().defaultBlockState(), GEN_FLAGS | Block.UPDATE_NEIGHBORS);

        for (int y = ventY + 2; y <= maxY + 3; y++) {
            BlockPos col = base.offset(0, y, 0);
            if (inWorld(level, col)) {
                safeSet(level, col, Blocks.AIR.defaultBlockState(), GEN_FLAGS);
            }
            if (y <= floorY + 1) {
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockPos side = base.offset(dir.getStepX(), y, dir.getStepZ());
                    if (inWorld(level, side)) {
                        safeSet(level, side, Blocks.AIR.defaultBlockState(), GEN_FLAGS);
                    }
                }
            }
        }

        int chestRelY = Math.max(EMBED_DEPTH + 2, Math.min(height / 3, EMBED_DEPTH + 8));
        BlockPos chestPos = base.offset(Math.max(3, baseR / 2), chestRelY, 0);
        if (!inWorld(level, chestPos)) {
            return true;
        }
        if (!level.getBlockState(chestPos).isAir()) {
            chestPos = chestPos.above();
        }
        if (!inWorld(level, chestPos) || !level.getBlockState(chestPos).isAir()) {
            return true;
        }
        safeSet(level, chestPos, Blocks.CHEST.defaultBlockState(), GEN_FLAGS | Block.UPDATE_NEIGHBORS);
        BlockEntity be = level.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(ModItems.STAR_ESSONITE.get(), 1 + random.nextInt(3)));
            chest.setItem(1, new ItemStack(ModItems.PURE_ESSONITE.get(), 1 + random.nextInt(2)));
            chest.setItem(2, new ItemStack(ModItems.PHI_CELL.get()));
            chest.setItem(3, new ItemStack(ModItems.ESSENITE_DUST.get(), 8 + random.nextInt(8)));
            if (random.nextBoolean()) {
                chest.setItem(4, new ItemStack(ModItems.VOID_OBSIDIAN.get(), 2 + random.nextInt(4)));
            }
        }
        return true;
    }

    private static void safeSet(WorldGenLevel level, BlockPos pos, BlockState state, int flags) {
        if (!inWorld(level, pos) || !canReadColumn(level, pos.getX(), pos.getZ())) {
            return;
        }
        level.setBlock(pos, state, flags);
    }

    /** WorldGenRegion throws if biome/height/block is queried outside generating chunks. */
    private static boolean canReadColumn(WorldGenLevel level, int x, int z) {
        return level.hasChunk(x >> 4, z >> 4);
    }

    private static boolean structureFootprintAccessible(WorldGenLevel level, BlockPos base, int horizontalRadius) {
        int minX = base.getX() - horizontalRadius;
        int maxX = base.getX() + horizontalRadius;
        int minZ = base.getZ() - horizontalRadius;
        int maxZ = base.getZ() + horizontalRadius;
        for (int x = minX; x <= maxX; x += 4) {
            for (int z = minZ; z <= maxZ; z += 4) {
                if (!canReadColumn(level, x, z)) {
                    return false;
                }
            }
        }
        return canReadColumn(level, base.getX(), base.getZ());
    }

    private static boolean inWorld(WorldGenLevel level, BlockPos pos) {
        return pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight();
    }

    private static boolean isPlateauInterior(WorldGenLevel level, int x, int z, int margin) {
        if (!isPlateauColumn(level, x, z)) {
            return false;
        }
        for (int i = 0; i < 8; i++) {
            double ang = (Math.PI * 2 * i) / 8.0;
            int px = x + (int) Math.round(Math.cos(ang) * margin);
            int pz = z + (int) Math.round(Math.sin(ang) * margin);
            if (!isPlateauColumn(level, px, pz)) {
                return false;
            }
        }
        int half = margin / 2;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (!isPlateauColumn(level, x + dir.getStepX() * half, z + dir.getStepZ() * half)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPlateauColumn(WorldGenLevel level, int x, int z) {
        if (!canReadColumn(level, x, z)) {
            return false;
        }
        int sy = surfaceY(level, x, z);
        return isPlateau(level, new BlockPos(x, sy, z));
    }

    private static boolean isPlateau(WorldGenLevel level, BlockPos pos) {
        if (!canReadColumn(level, pos.getX(), pos.getZ())) {
            return false;
        }
        return level.getBiome(pos).is(ModBiomeTags.ESSENCE_PLATEAU);
    }

    private static int surfaceY(WorldGenLevel level, int x, int z) {
        if (!canReadColumn(level, x, z)) {
            return level.getMinBuildHeight();
        }
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
    }

    private static boolean canReplaceForFoundation(BlockState state) {
        return state.isAir()
                || state.canBeReplaced()
                || state.is(BlockTags.LEAVES)
                || state.is(Blocks.SNOW)
                || state.is(Blocks.POWDER_SNOW)
                || state.is(ModBlocks.PHI_GRASS.get())
                || state.is(ModBlocks.PHI_DIRT.get())
                || state.is(ModBlocks.PHI_BLADES.get());
    }
}
