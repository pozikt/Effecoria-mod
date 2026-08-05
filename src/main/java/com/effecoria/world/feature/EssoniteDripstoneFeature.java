package com.effecoria.world.feature;

import com.effecoria.block.EssonitePointedBlock;
import com.effecoria.content.ModBlocks;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Places essonite dripstone clusters (stalactites / stalagmites / occasional Φ-columns) in cave air.
 */
public final class EssoniteDripstoneFeature extends Feature<NoneFeatureConfiguration> {
    public EssoniteDripstoneFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        if (!level.isEmptyBlock(origin)) {
            return false;
        }

        boolean placed = false;
        int attempts = 4 + random.nextInt(5);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < attempts; i++) {
            cursor.setWithOffset(origin, random.nextInt(7) - 3, random.nextInt(5) - 2, random.nextInt(7) - 3);
            if (!level.isEmptyBlock(cursor)) {
                continue;
            }
            if (nearPhiWater(level, cursor)) {
                continue;
            }
            // Prefer ceiling hangers, sometimes floor risers
            if (random.nextFloat() < 0.62f) {
                placed |= tryHang(level, cursor, random);
            } else {
                placed |= tryRise(level, cursor, random);
            }
        }
        return placed;
    }

    private static boolean nearPhiWater(WorldGenLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos m = pos.mutable();
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    m.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (level.getBlockState(m).is(ModBlocks.PHI_WATER.get())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean tryHang(WorldGenLevel level, BlockPos air, RandomSource random) {
        BlockPos.MutableBlockPos m = air.mutable();
        for (int i = 0; i < 12; i++) {
            m.move(Direction.UP);
            BlockState above = level.getBlockState(m);
            if (above.isAir()) {
                continue;
            }
            if (!isSupport(above)) {
                return false;
            }
            BlockPos attach = m.immutable();
            // patch support to dripstone base sometimes
            if (random.nextFloat() < 0.35f) {
                level.setBlock(attach, ModBlocks.ESSONITE_DRIPSTONE_BLOCK.get().defaultBlockState(), 2);
            }
            int height = 2 + random.nextInt(5);
            return placeColumn(level, attach.below(), Direction.DOWN, height, random);
        }
        return false;
    }

    private static boolean tryRise(WorldGenLevel level, BlockPos air, RandomSource random) {
        BlockPos.MutableBlockPos m = air.mutable();
        for (int i = 0; i < 12; i++) {
            m.move(Direction.DOWN);
            BlockState below = level.getBlockState(m);
            if (below.isAir()) {
                continue;
            }
            if (!isSupport(below)) {
                return false;
            }
            BlockPos attach = m.immutable();
            if (random.nextFloat() < 0.4f) {
                level.setBlock(attach, ModBlocks.ESSONITE_DRIPSTONE_BLOCK.get().defaultBlockState(), 2);
            }
            int height = 2 + random.nextInt(4);
            boolean up = placeColumn(level, attach.above(), Direction.UP, height, random);
            // rare merge into Φ-column if ceiling is close
            if (up && random.nextFloat() < 0.18f) {
                tryMergeColumn(level, attach.above(), height);
            }
            return up;
        }
        return false;
    }

    private static void tryMergeColumn(WorldGenLevel level, BlockPos base, int height) {
        BlockPos tip = base.above(height - 1);
        BlockPos.MutableBlockPos m = tip.mutable();
        for (int i = 0; i < 8; i++) {
            m.move(Direction.UP);
            if (!level.isEmptyBlock(m)) {
                if (!isSupport(level.getBlockState(m))) {
                    return;
                }
                int gap = m.getY() - tip.getY() - 1;
                if (gap <= 0 || gap > 6) {
                    return;
                }
                placeColumn(level, m.below(), Direction.DOWN, gap, RandomSource.create());
                // mark merge tips
                BlockState upTip = level.getBlockState(tip);
                BlockState downTip = level.getBlockState(m.below());
                if (upTip.getBlock() instanceof EssonitePointedBlock) {
                    level.setBlock(
                            tip,
                            EssonitePointedBlock.withThickness(upTip, DripstoneThickness.TIP_MERGE),
                            2);
                }
                if (downTip.getBlock() instanceof EssonitePointedBlock) {
                    level.setBlock(
                            m.below(),
                            EssonitePointedBlock.withThickness(downTip, DripstoneThickness.TIP_MERGE),
                            2);
                }
                return;
            }
        }
    }

    private static boolean placeColumn(
            WorldGenLevel level, BlockPos start, Direction tipDir, int height, RandomSource random) {
        BlockPos.MutableBlockPos m = start.mutable();
        boolean any = false;
        for (int i = 0; i < height; i++) {
            if (!level.isEmptyBlock(m) && !level.getBlockState(m).canBeReplaced()) {
                break;
            }
            DripstoneThickness thickness;
            if (i == height - 1) {
                thickness = DripstoneThickness.TIP;
            } else if (i == height - 2) {
                thickness = DripstoneThickness.FRUSTUM;
            } else if (i == 0) {
                thickness = DripstoneThickness.BASE;
            } else {
                thickness = DripstoneThickness.MIDDLE;
            }
            BlockState state = ModBlocks.ESSONITE_POINTED
                    .get()
                    .defaultBlockState()
                    .setValue(EssonitePointedBlock.TIP_DIRECTION, tipDir)
                    .setValue(EssonitePointedBlock.THICKNESS, thickness)
                    .setValue(EssonitePointedBlock.WATERLOGGED, false);
            level.setBlock(m, state, 2);
            any = true;
            m.move(tipDir);
        }
        return any;
    }

    private static boolean isSupport(BlockState state) {
        return state.is(ModBlocks.PHI_STONE.get())
                || state.is(ModBlocks.ESSONITE_DRIPSTONE_BLOCK.get())
                || state.is(ModBlocks.ESSENITE_ORE.get())
                || state.is(ModBlocks.DEEPSLATE_ESSENITE_ORE.get())
                || state.is(ModBlocks.ESSONITE_BLOCK.get())
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
    }
}
