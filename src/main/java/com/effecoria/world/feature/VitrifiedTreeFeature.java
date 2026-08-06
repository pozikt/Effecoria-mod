package com.effecoria.world.feature;

import com.effecoria.content.ModBlocks;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Single petrified glass tree — black trunk + sharp branches. */
public final class VitrifiedTreeFeature extends Feature<NoneFeatureConfiguration> {
    public VitrifiedTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return placeTree(context.level(), context.origin(), context.random());
    }

    public static boolean placeTree(WorldGenLevel level, BlockPos origin, RandomSource random) {
        BlockPos ground = origin.below();
        BlockState floor = level.getBlockState(ground);
        if (!floor.is(ModBlocks.VITRIFIED_DIRT.get())
                && !floor.is(ModBlocks.VITRIFIED_SAND.get())
                && !floor.is(ModBlocks.VITRIFIED_STONE.get())) {
            return false;
        }
        if (!level.getBlockState(origin).canBeReplaced() && !level.getBlockState(origin).isAir()) {
            return false;
        }

        BlockState log = ModBlocks.VITRIFIED_LOG.get().defaultBlockState();
        BlockState branch = ModBlocks.VITRIFIED_BRANCHES.get().defaultBlockState();
        int height = 5 + random.nextInt(6);
        boolean giant = random.nextFloat() < 0.08f;
        if (giant) {
            height += 4 + random.nextInt(4);
        }

        for (int y = 0; y < height; y++) {
            BlockPos p = origin.above(y);
            if (!level.getBlockState(p).canBeReplaced() && !level.getBlockState(p).isAir()) {
                height = y;
                break;
            }
            level.setBlock(p, log, 2);
            if (y > 2 && random.nextFloat() < 0.35f) {
                Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                BlockPos b = p.relative(dir);
                if (level.getBlockState(b).canBeReplaced() || level.getBlockState(b).isAir()) {
                    level.setBlock(b, branch, 2);
                    if (random.nextBoolean()) {
                        BlockPos tip = b.relative(dir).above();
                        if (level.getBlockState(tip).canBeReplaced() || level.getBlockState(tip).isAir()) {
                            level.setBlock(tip, branch, 2);
                        }
                    }
                }
            }
        }
        if (height < 3) {
            return false;
        }
        for (int i = 0; i < 3 + random.nextInt(3); i++) {
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos tip = origin.above(height - 1).relative(dir);
            if (level.getBlockState(tip).canBeReplaced() || level.getBlockState(tip).isAir()) {
                level.setBlock(tip, log.setValue(RotatedPillarBlock.AXIS, dir.getAxis()), 2);
            }
        }
        if (giant) {
            BlockPos hollow = origin.above(1);
            level.setBlock(hollow, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            if (random.nextFloat() < 0.6f) {
                level.setBlock(
                        hollow,
                        net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(),
                        2);
            }
        }
        return true;
    }
}
