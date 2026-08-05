package com.effecoria.world.feature;

import com.effecoria.content.ModBlocks;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Places multi-facing essonite crystal druze on cave walls / floors / ceilings.
 */
public final class EssoniteDruzeFeature extends Feature<NoneFeatureConfiguration> {
    public EssoniteDruzeFeature(Codec<NoneFeatureConfiguration> codec) {
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
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int tries = 10 + random.nextInt(10);
        for (int i = 0; i < tries; i++) {
            cursor.setWithOffset(origin, random.nextInt(9) - 4, random.nextInt(7) - 3, random.nextInt(9) - 4);
            if (!level.isEmptyBlock(cursor) || nearPhiWater(level, cursor)) {
                continue;
            }
            for (Direction face : Direction.values()) {
                BlockPos support = cursor.relative(face.getOpposite());
                BlockState supportState = level.getBlockState(support);
                if (!isDruzeHost(supportState)) {
                    continue;
                }
                if (!supportState.isFaceSturdy(level, support, face)) {
                    continue;
                }
                Block crystal = pickCrystal(random);
                BlockState state = crystal
                        .defaultBlockState()
                        .setValue(AmethystClusterBlock.FACING, face)
                        .setValue(AmethystClusterBlock.WATERLOGGED, false);
                level.setBlock(cursor, state, 2);
                placed = true;
                break;
            }
        }
        return placed;
    }

    private static boolean nearPhiWater(WorldGenLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos m = pos.mutable();
        for (int dy = -1; dy <= 1; dy++) {
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

    private static Block pickCrystal(RandomSource random) {
        float roll = random.nextFloat();
        if (roll < 0.45f) {
            return ModBlocks.ESSONITE_CRYSTAL_BUD_SMALL.get();
        }
        if (roll < 0.7f) {
            return ModBlocks.ESSONITE_CRYSTAL_BUD_MEDIUM.get();
        }
        if (roll < 0.9f) {
            return ModBlocks.ESSONITE_CRYSTAL_BUD_LARGE.get();
        }
        return ModBlocks.ESSONITE_CRYSTAL.get();
    }

    private static boolean isDruzeHost(BlockState state) {
        return state.is(ModBlocks.PHI_STONE.get())
                || state.is(ModBlocks.ESSONITE_DRIPSTONE_BLOCK.get())
                || state.is(ModBlocks.ESSENITE_ORE.get())
                || state.is(ModBlocks.DEEPSLATE_ESSENITE_ORE.get())
                || state.is(ModBlocks.ESSONITE_CRUST.get())
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
    }
}
