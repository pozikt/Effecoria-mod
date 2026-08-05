package com.effecoria.world.feature;

import com.effecoria.content.ModBlocks;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Φ-core (Φ-ядро): near-solid essonite mass at the lowest heights under the plateau.
 */
public final class PhiCoreFeature extends Feature<NoneFeatureConfiguration> {
    public PhiCoreFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        if (origin.getY() > 0) {
            return false;
        }

        BlockState core = ModBlocks.ESSONITE_BLOCK.get().defaultBlockState();
        BlockState ore = ModBlocks.DEEPSLATE_ESSENITE_ORE.get().defaultBlockState();
        BlockState phiStone = ModBlocks.PHI_STONE.get().defaultBlockState();

        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int radius = 7 + random.nextInt(6);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double dist2 = dx * dx + dy * dy * 0.85 + dz * dz;
                    if (dist2 > radius * radius) {
                        continue;
                    }
                    int y = origin.getY() + dy;
                    if (y < level.getMinBuildHeight() + 1 || y > 0) {
                        continue;
                    }
                    cursor.set(origin.getX() + dx, y, origin.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (!isReplaceable(state)) {
                        continue;
                    }

                    float roll = random.nextFloat();
                    // ~88% pure essonite, sparse ore veins, rare Φ-stone seams
                    BlockState next;
                    if (roll < 0.88f) {
                        next = core;
                    } else if (roll < 0.96f) {
                        next = ore;
                    } else {
                        next = phiStone;
                    }
                    level.setBlock(cursor, next, 2);
                    placed = true;
                }
            }
        }
        return placed;
    }

    private static boolean isReplaceable(BlockState state) {
        if (state.is(Blocks.BEDROCK) || state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                || state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.ANDESITE)
                || state.is(ModBlocks.PHI_STONE.get())
                || state.is(ModBlocks.ESSENITE_ORE.get())
                || state.is(ModBlocks.DEEPSLATE_ESSENITE_ORE.get());
    }
}
