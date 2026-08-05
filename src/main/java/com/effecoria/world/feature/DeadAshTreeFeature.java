package com.effecoria.world.feature;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Leafless ash-gray trunk — twisted dead tree for the Dead Wasteland. */
public final class DeadAshTreeFeature extends Feature<NoneFeatureConfiguration> {
    public DeadAshTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        BlockPos ground = origin.below();
        BlockState floor = level.getBlockState(ground);
        if (!floor.is(BlockTags.DIRT) && !floor.is(BlockTags.SAND) && !floor.is(Blocks.SANDSTONE)
                && !floor.is(com.effecoria.content.ModBlocks.PARCHED_SAND.get())
                && !floor.is(com.effecoria.content.ModBlocks.ASH_SOIL.get())) {
            return false;
        }
        if (!level.getBlockState(origin).isAir()) {
            return false;
        }

        BlockState log = Blocks.STRIPPED_OAK_LOG.defaultBlockState();
        int height = 3 + random.nextInt(3);
        BlockPos.MutableBlockPos cursor = origin.mutable();
        for (int y = 0; y < height; y++) {
            cursor.set(origin.getX(), origin.getY() + y, origin.getZ());
            if (!level.getBlockState(cursor).canBeReplaced()) {
                break;
            }
            level.setBlock(cursor, log, 2);
        }
        // Bent branch
        Direction bend = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockPos branch = origin.above(Math.max(1, height - 1)).relative(bend);
        if (level.getBlockState(branch).canBeReplaced()) {
            level.setBlock(
                    branch,
                    log.setValue(RotatedPillarBlock.AXIS, bend.getAxis()),
                    2);
        }
        if (random.nextBoolean()) {
            BlockPos tip = branch.relative(bend);
            if (level.getBlockState(tip).canBeReplaced()) {
                level.setBlock(tip, Blocks.DEAD_BUSH.defaultBlockState(), 2);
            }
        }
        return true;
    }
}
