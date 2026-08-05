package com.effecoria.world;

import com.effecoria.content.ModBlocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;

public final class EssencePlateauSurfaceRules {
    private EssencePlateauSurfaceRules() {}

    public static SurfaceRules.RuleSource makeRules() {
        SurfaceRules.RuleSource phiStone = state(ModBlocks.PHI_STONE.get());
        SurfaceRules.ConditionSource isAtOrAboveWaterLevel = SurfaceRules.waterBlockCheck(-1, 0);
        SurfaceRules.RuleSource grassSurface =
                SurfaceRules.sequence(SurfaceRules.ifTrue(isAtOrAboveWaterLevel, state(Blocks.GRASS_BLOCK)), state(Blocks.DIRT));

        SurfaceRules.RuleSource plateauSurface = SurfaceRules.ifTrue(
                SurfaceRules.isBiome(ModBiomes.ESSENCE_PLATEAU),
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, phiStone));

        return SurfaceRules.sequence(plateauSurface, SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, grassSurface));
    }

    private static SurfaceRules.RuleSource state(Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }
}
