package com.effecoria.world.feature;

import com.effecoria.content.ModBlocks;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Twisted Ω-trees — black cracked trunks, unnatural branch angles, occasional weeping resin streaks.
 */
public final class OmegaScarTreeFeature extends Feature<NoneFeatureConfiguration> {
    public OmegaScarTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        BlockPos ground = origin.below();
        BlockState floor = level.getBlockState(ground);
        if (!floor.is(ModBlocks.ASH_SOIL.get()) && !floor.is(ModBlocks.VOID_OBSIDIAN.get())) {
            return false;
        }
        if (!level.getBlockState(origin).canBeReplaced()) {
            return false;
        }

        BlockState log = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();
        int height = 4 + random.nextInt(5);
        BlockPos.MutableBlockPos cursor = origin.mutable();
        Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int leanAt = 1 + random.nextInt(Math.max(1, height - 1));

        for (int y = 0; y < height; y++) {
            int ox = y >= leanAt && (y - leanAt) % 2 == 0 ? lean.getStepX() : 0;
            int oz = y >= leanAt && (y - leanAt) % 2 == 0 ? lean.getStepZ() : 0;
            cursor.set(origin.getX() + ox, origin.getY() + y, origin.getZ() + oz);
            if (!level.getBlockState(cursor).canBeReplaced()) {
                break;
            }
            level.setBlock(cursor, log, 2);
            // Occasional black "weep" — hanging roots as resin stand-in.
            if (y > 1 && random.nextFloat() < 0.18f) {
                BlockPos weep = cursor.below();
                if (level.getBlockState(weep).canBeReplaced()) {
                    level.setBlock(weep, Blocks.HANGING_ROOTS.defaultBlockState(), 2);
                }
            }
        }

        int branches = 2 + random.nextInt(3);
        for (int i = 0; i < branches; i++) {
            Direction bend = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            int by = Math.max(1, height - 1 - random.nextInt(3));
            BlockPos branch = origin.above(by).relative(bend);
            if (level.getBlockState(branch).canBeReplaced()) {
                level.setBlock(branch, log.setValue(RotatedPillarBlock.AXIS, bend.getAxis()), 2);
            }
            BlockPos tip = branch.relative(bend);
            if (random.nextBoolean()) {
                tip = tip.relative(Direction.UP);
            } else if (random.nextBoolean()) {
                tip = tip.relative(bend);
            }
            if (level.getBlockState(tip).canBeReplaced()) {
                Direction.Axis axis = tip.getY() != branch.getY() ? Direction.Axis.Y : bend.getAxis();
                level.setBlock(tip, log.setValue(RotatedPillarBlock.AXIS, axis), 2);
            }
            // Unnatural upward / sideways claw.
            if (random.nextBoolean()) {
                BlockPos claw = tip.above();
                if (level.getBlockState(claw).canBeReplaced()) {
                    level.setBlock(claw, Blocks.DEAD_BUSH.defaultBlockState(), 2);
                }
            }
        }
        return true;
    }
}
