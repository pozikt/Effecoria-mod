package com.effecoria.world.feature;

import com.effecoria.content.ModBlocks;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Surface void-obsidian scar cracks — jagged glass wounds in the ash crust. */
public final class OmegaScarCrackFeature extends Feature<NoneFeatureConfiguration> {
    public OmegaScarCrackFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        BlockState floor = level.getBlockState(origin.below());
        if (!floor.is(ModBlocks.ASH_SOIL.get()) && !floor.is(ModBlocks.VOID_OBSIDIAN.get())) {
            return false;
        }

        BlockState voidObs = ModBlocks.VOID_OBSIDIAN.get().defaultBlockState();
        boolean placed = false;
        int length = 3 + random.nextInt(5);
        int dx = random.nextBoolean() ? 1 : 0;
        int dz = dx == 0 ? 1 : (random.nextBoolean() ? 1 : 0);
        if (random.nextBoolean()) {
            dx = -dx;
        }
        if (random.nextBoolean()) {
            dz = -dz;
        }

        BlockPos.MutableBlockPos cursor = origin.below().mutable();
        for (int i = 0; i < length; i++) {
            cursor.move(dx, 0, dz);
            if (random.nextFloat() < 0.25f) {
                cursor.move(0, random.nextBoolean() ? -1 : 0, 0);
            }
            BlockState current = level.getBlockState(cursor);
            if (current.is(ModBlocks.ASH_SOIL.get()) || current.is(ModBlocks.VOID_OBSIDIAN.get())) {
                level.setBlock(cursor, voidObs, 2);
                placed = true;
                if (random.nextFloat() < 0.35f) {
                    BlockPos above = cursor.above();
                    if (level.getBlockState(above).canBeReplaced()) {
                        level.setBlock(above, ModBlocks.ELDRITCH_BLOOD_PUDDLE.get().defaultBlockState(), 2);
                    }
                }
            }
        }
        return placed;
    }
}
