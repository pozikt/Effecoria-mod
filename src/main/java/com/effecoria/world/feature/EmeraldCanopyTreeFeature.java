package com.effecoria.world.feature;

import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModBlocks;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Giant Emerald Canopy tree — buttresses, thick trunk, multi-tier canopy, rare emergents.
 * Kept within ~12 blocks horizontally of origin for chunk-safe generation.
 */
public final class EmeraldCanopyTreeFeature extends Feature<NoneFeatureConfiguration> {
    public EmeraldCanopyTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        BlockPos ground = origin.below();
        BlockState floor = level.getBlockState(ground);
        if (!floor.is(ModBlocks.PHI_GRASS.get()) && !floor.is(ModBlocks.PHI_DIRT.get())) {
            return false;
        }
        if (!level.getBlockState(origin).canBeReplaced()) {
            return false;
        }

        boolean emergent = random.nextFloat() < BalanceConfig.EMERALD_CANOPY_EMERGENT_CHANCE.get();
        int minH = BalanceConfig.EMERALD_CANOPY_TREE_MIN_HEIGHT.get();
        int maxH = BalanceConfig.EMERALD_CANOPY_TREE_MAX_HEIGHT.get();
        int height = minH + random.nextInt(Math.max(1, maxH - minH + 1));
        if (emergent) {
            int emMax = BalanceConfig.EMERALD_CANOPY_EMERGENT_MAX_HEIGHT.get();
            height = Math.max(height, maxH) + random.nextInt(Math.max(1, emMax - maxH + 1));
        }
        int topY = origin.getY() + height;
        if (topY >= level.getMaxBuildHeight() - 4) {
            height = Math.max(minH, level.getMaxBuildHeight() - 8 - origin.getY());
            topY = origin.getY() + height;
        }

        int radius = 1 + random.nextInt(emergent ? 3 : 2); // 1–3 (emergent up to 3)
        placeButtresses(level, origin, radius, random);
        placeTrunk(level, origin, height, radius, random);
        placeCanopyTiers(level, origin, height, radius, emergent, random);
        return true;
    }

    private static void placeButtresses(WorldGenLevel level, BlockPos origin, int radius, RandomSource random) {
        BlockState wood = ModBlocks.ANCIENT_ESSENCE_WOOD.get().defaultBlockState();
        BlockState bark = ModBlocks.GOLDEN_BARK.get().defaultBlockState();
        int count = 4 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            float angle = (float) (i * (Math.PI * 2.0 / count) + random.nextFloat() * 0.2);
            int len = 4 + random.nextInt(5);
            for (int d = 1; d <= len; d++) {
                int x = origin.getX() + Mth.floor(Math.cos(angle) * (radius + d * 0.85));
                int z = origin.getZ() + Mth.floor(Math.sin(angle) * (radius + d * 0.85));
                int y = origin.getY() + Math.max(0, (len - d) / 2);
                BlockPos pos = new BlockPos(x, y, z);
                if (Math.abs(x - origin.getX()) > 11 || Math.abs(z - origin.getZ()) > 11) {
                    break;
                }
                setIfReplaceable(level, pos, d <= 2 && random.nextFloat() < 0.35f ? bark : wood);
                setIfReplaceable(level, pos.above(), wood);
            }
        }
    }

    private static void placeTrunk(
            WorldGenLevel level, BlockPos origin, int height, int radius, RandomSource random) {
        BlockState log = ModBlocks.PHI_LOG.get().defaultBlockState();
        BlockState ancient = ModBlocks.ANCIENT_ESSENCE_WOOD.get().defaultBlockState();
        BlockState bark = ModBlocks.GOLDEN_BARK.get().defaultBlockState();
        for (int y = 0; y < height; y++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius + 1) {
                        continue;
                    }
                    BlockPos pos = origin.offset(dx, y, dz);
                    boolean core = dx == 0 && dz == 0;
                    boolean shell = dx * dx + dz * dz >= radius * radius - 1;
                    BlockState state;
                    if (core && y < height / 3) {
                        state = ancient;
                    } else if (shell && random.nextFloat() < 0.12f) {
                        state = bark.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
                    } else if (core && y < height / 2) {
                        state = ancient;
                    } else {
                        state = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
                    }
                    setIfReplaceable(level, pos, state);
                }
            }
            // Occasional snare vine on trunk face.
            if (y > 8 && y < height - 10 && random.nextFloat() < 0.04f) {
                Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                BlockPos vine = origin.offset(dir.getStepX() * (radius + 1), y, dir.getStepZ() * (radius + 1));
                setIfReplaceable(level, vine, ModBlocks.PHI_SNARE_VINE.get().defaultBlockState());
            }
        }
    }

    private static void placeCanopyTiers(
            WorldGenLevel level,
            BlockPos origin,
            int height,
            int radius,
            boolean emergent,
            RandomSource random) {
        BlockState leaves = ModBlocks.PHI_LEAVES.get()
                .defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, true)
                .setValue(LeavesBlock.DISTANCE, 1);
        BlockState log = ModBlocks.PHI_LOG.get().defaultBlockState();
        int firstTier = Math.max(height / 3, height - 28);
            int tiers = emergent ? 5 : 4;
        for (int t = 0; t < tiers; t++) {
            int ty = firstTier + (height - firstTier) * t / Math.max(1, tiers - 1);
            int canopyR = emergent ? 7 - t : 6 - t / 2;
            canopyR = Mth.clamp(canopyR, 3, 8);
            // Branches
            int branches = 5 + random.nextInt(4);
            for (int b = 0; b < branches; b++) {
                Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                int blen = 3 + random.nextInt(canopyR);
                BlockPos cursor = origin.offset(0, ty, 0);
                for (int s = 0; s < blen; s++) {
                    cursor = cursor.relative(dir);
                    if (Math.abs(cursor.getX() - origin.getX()) > 11
                            || Math.abs(cursor.getZ() - origin.getZ()) > 11) {
                        break;
                    }
                    if (random.nextBoolean()) {
                        cursor = cursor.above();
                    }
                    setIfReplaceable(
                            level,
                            cursor,
                            log.setValue(RotatedPillarBlock.AXIS, dir.getAxis()));
                    blobLeaves(level, cursor, 2 + random.nextInt(2), leaves, random);
                    if (random.nextFloat() < 0.15f) {
                        setIfReplaceable(
                                level,
                                cursor.below(),
                                ModBlocks.PHI_BLADES.get().defaultBlockState());
                    }
                    if (random.nextFloat() < 0.08f) {
                        setIfReplaceable(
                                level,
                                cursor.below(),
                                Blocks.HANGING_ROOTS.defaultBlockState());
                    }
                    if (random.nextFloat() < 0.04f) {
                        setIfReplaceable(
                                level,
                                cursor.below(),
                                ModBlocks.PHI_SNARE_VINE.get().defaultBlockState());
                    }
                }
            }
            // Soft disk canopy around trunk at this tier
            for (int dx = -canopyR; dx <= canopyR; dx++) {
                for (int dz = -canopyR; dz <= canopyR; dz++) {
                    if (dx * dx + dz * dz > canopyR * canopyR) {
                        continue;
                    }
                    if (random.nextFloat() < 0.35f) {
                        continue;
                    }
                    BlockPos leafPos = origin.offset(dx, ty + random.nextInt(3) - 1, dz);
                    if (Math.abs(dx) > 11 || Math.abs(dz) > 11) {
                        continue;
                    }
                    setIfReplaceable(level, leafPos, leaves);
                }
            }
        }

        // Giant nut nest near upper canopy
        if (random.nextFloat() < 0.55f) {
            BlockPos nest = origin.above(height - 4 - random.nextInt(6)).relative(
                    Direction.Plane.HORIZONTAL.getRandomDirection(random), radius + 2);
            setIfReplaceable(level, nest, leaves);
            // Drop handled via loot on leaves; stash a marker by placing golden bark shelf.
            setIfReplaceable(level, nest.below(), ModBlocks.GOLDEN_BARK.get().defaultBlockState());
        }
    }

    private static void blobLeaves(
            WorldGenLevel level, BlockPos center, int r, BlockState leaves, RandomSource random) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r * r + 1) {
                        continue;
                    }
                    if (random.nextFloat() < 0.25f) {
                        continue;
                    }
                    BlockPos p = center.offset(dx, dy, dz);
                    if (Math.abs(p.getX() - center.getX()) + Math.abs(p.getZ() - center.getZ()) > 10) {
                        continue;
                    }
                    setIfReplaceable(level, p, leaves);
                }
            }
        }
    }

    private static void setIfReplaceable(WorldGenLevel level, BlockPos pos, BlockState state) {
        BlockState current = level.getBlockState(pos);
        if (current.canBeReplaced() || current.isAir()) {
            level.setBlock(pos, state, 2);
        }
    }
}
