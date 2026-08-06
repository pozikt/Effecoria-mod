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

/** Φ-crater: shallow bowl of glass + crystal rim + central geyser crack. */
public final class VitrifiedCraterFeature extends Feature<NoneFeatureConfiguration> {
    public VitrifiedCraterFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin().below();
        int radius = 4 + random.nextInt(4);
        BlockState sand = ModBlocks.VITRIFIED_SAND.get().defaultBlockState();
        BlockState stone = ModBlocks.VITRIFIED_STONE.get().defaultBlockState();
        BlockState dirt = ModBlocks.VITRIFIED_DIRT.get().defaultBlockState();
        BlockState crystal = ModBlocks.ESSONITE_CRYSTAL.get()
                .defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.UP);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > radius + 0.4) {
                    continue;
                }
                int depth = (int) Math.max(1, (radius - dist) * 0.7);
                for (int dy = 0; dy <= depth; dy++) {
                    BlockPos p = origin.offset(dx, -dy, dz);
                    BlockState fill = dy == depth ? sand : (dist > radius - 1.2 ? stone : dirt);
                    level.setBlock(p, fill, 2);
                }
                if (dist > radius - 1.5 && dist <= radius && random.nextFloat() < 0.35f) {
                    BlockPos c = origin.offset(dx, 1, dz);
                    if (level.getBlockState(c).canBeReplaced() || level.getBlockState(c).isAir()) {
                        level.setBlock(c, crystal, 2);
                    }
                }
            }
        }
        level.setBlock(origin, ModBlocks.VITRIFIED_GEYSER_CRACK.get().defaultBlockState(), 2);
        return true;
    }
}
