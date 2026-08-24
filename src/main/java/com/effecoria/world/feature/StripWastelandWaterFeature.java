package com.effecoria.world.feature;

import com.effecoria.world.DeadWastelandHydrology;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * One cheap pass per chunk: evaporate inland wasteland water (air).
 * Interior check is once per column, not per block — a dense ring used to stall the server
 * when teleporting into ungenerated Dead Wasteland.
 */
public final class StripWastelandWaterFeature extends Feature<NoneFeatureConfiguration> {
    public StripWastelandWaterFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int minX = SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(origin.getX()));
        int minZ = SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(origin.getZ()));
        boolean any = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = minX + lx;
                int z = minZ + lz;
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                cursor.set(x, surface, z);
                if (!DeadWastelandHydrology.isInteriorDryCell(level, cursor)) {
                    continue;
                }
                int yMin = Math.max(level.getMinBuildHeight() + 1, surface - 24);
                int yMax = Math.min(level.getMaxBuildHeight() - 1, surface + 2);
                for (int y = yMin; y <= yMax; y++) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (!DeadWastelandHydrology.isForbiddenWater(state)) {
                        continue;
                    }
                    level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    any = true;
                }
            }
        }
        return any;
    }
}
