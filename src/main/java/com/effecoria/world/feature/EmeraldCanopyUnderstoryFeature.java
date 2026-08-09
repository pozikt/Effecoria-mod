package com.effecoria.world.feature;

import com.effecoria.content.ModBlocks;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Understory litter, tall Φ-blades, and snare vines for Emerald Canopy. */
public final class EmeraldCanopyUnderstoryFeature extends Feature<NoneFeatureConfiguration> {
    public EmeraldCanopyUnderstoryFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        BlockState floor = level.getBlockState(origin.below());
        if (!floor.is(ModBlocks.PHI_GRASS.get()) && !floor.is(ModBlocks.PHI_DIRT.get())) {
            return false;
        }
        boolean any = false;
        int count = 8 + random.nextInt(12);
        for (int i = 0; i < count; i++) {
            BlockPos pos = origin.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
            if (!level.getBlockState(pos).canBeReplaced()) {
                continue;
            }
            BlockState below = level.getBlockState(pos.below());
            if (!below.is(ModBlocks.PHI_GRASS.get()) && !below.is(ModBlocks.PHI_DIRT.get())) {
                continue;
            }
            float roll = random.nextFloat();
            if (roll < 0.45f) {
                level.setBlock(pos, ModBlocks.PHI_BLADES.get().defaultBlockState(), 2);
                // Tall fern illusion — stack blades
                if (random.nextBoolean() && level.getBlockState(pos.above()).canBeReplaced()) {
                    level.setBlock(pos.above(), ModBlocks.PHI_BLADES.get().defaultBlockState(), 2);
                }
                any = true;
            } else if (roll < 0.7f) {
                level.setBlock(pos, Blocks.MOSS_CARPET.defaultBlockState(), 2);
                any = true;
            } else if (roll < 0.88f) {
                level.setBlock(pos, ModBlocks.PHI_SNARE_VINE.get().defaultBlockState(), 2);
                any = true;
            } else {
                level.setBlock(pos, Blocks.HANGING_ROOTS.defaultBlockState(), 2);
                any = true;
            }
        }
        // Occasional wall vine on nearby solid face
        if (random.nextFloat() < 0.4f) {
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos wall = origin.relative(dir);
            if (level.getBlockState(wall).canBeReplaced()
                    && level.getBlockState(wall.relative(dir)).isFaceSturdy(level, wall.relative(dir), dir.getOpposite())) {
                level.setBlock(wall, ModBlocks.PHI_SNARE_VINE.get().defaultBlockState(), 2);
                any = true;
            }
        }
        return any;
    }
}
