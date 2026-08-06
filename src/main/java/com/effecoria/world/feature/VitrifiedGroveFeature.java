package com.effecoria.world.feature;

import com.effecoria.content.ModBlocks;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Petrified grove — cluster of glassy trunks on vitrified ground, occasional giant with hollow.
 */
public final class VitrifiedGroveFeature extends Feature<NoneFeatureConfiguration> {
    public VitrifiedGroveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int count = 10 + random.nextInt(21);
        int placed = 0;
        BlockState dirt = ModBlocks.VITRIFIED_DIRT.get().defaultBlockState();
        BlockState crystal = ModBlocks.ESSONITE_CRYSTAL.get()
                .defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.UP);

        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                if (dx * dx + dz * dz > 72) {
                    continue;
                }
                BlockPos floor = origin.offset(dx, -1, dz);
                BlockState under = level.getBlockState(floor);
                if (under.is(ModBlocks.VITRIFIED_SAND.get())
                        || under.is(ModBlocks.VITRIFIED_STONE.get())
                        || under.is(ModBlocks.VITRIFIED_DIRT.get())) {
                    level.setBlock(floor, dirt, 2);
                }
            }
        }

        for (int i = 0; i < count * 3 && placed < count; i++) {
            int ox = random.nextInt(17) - 8;
            int oz = random.nextInt(17) - 8;
            if (VitrifiedTreeFeature.placeTree(level, origin.offset(ox, 0, oz), random)) {
                placed++;
            }
        }

        for (int i = 0; i < 4 + random.nextInt(5); i++) {
            BlockPos c = origin.offset(random.nextInt(11) - 5, 0, random.nextInt(11) - 5);
            if (level.getBlockState(c).canBeReplaced() || level.getBlockState(c).isAir()) {
                BlockState floor = level.getBlockState(c.below());
                if (floor.is(ModBlocks.VITRIFIED_DIRT.get()) || floor.is(ModBlocks.VITRIFIED_STONE.get())) {
                    level.setBlock(c, crystal, 2);
                }
            }
        }
        return placed > 0;
    }
}
