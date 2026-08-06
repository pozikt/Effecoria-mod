package com.effecoria.world;

import com.effecoria.content.ModBlocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.placement.CaveSurface;

public final class EssencePlateauSurfaceRules {
    private EssencePlateauSurfaceRules() {}

    /** Paint Φ-stone through the full mountain mass under the surface floor. */
    private static final int PHI_STONE_DEPTH = 384;

    public static SurfaceRules.RuleSource makeRules() {
        SurfaceRules.RuleSource phiGrass = state(ModBlocks.PHI_GRASS.get());
        SurfaceRules.RuleSource phiDirt = state(ModBlocks.PHI_DIRT.get());
        SurfaceRules.RuleSource phiStone = state(ModBlocks.PHI_STONE.get());

        SurfaceRules.ConditionSource isAtOrAboveWaterLevel = SurfaceRules.waterBlockCheck(-1, 0);
        SurfaceRules.RuleSource plateauTop =
                SurfaceRules.sequence(SurfaceRules.ifTrue(isAtOrAboveWaterLevel, phiGrass), phiDirt);

        SurfaceRules.ConditionSource phiStoneBand =
                SurfaceRules.stoneDepthCheck(0, true, PHI_STONE_DEPTH, CaveSurface.FLOOR);

        SurfaceRules.RuleSource plateauBody = SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, plateauTop),
                SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, phiDirt),
                SurfaceRules.ifTrue(phiStoneBand, phiStone),
                SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, phiStone),
                SurfaceRules.ifTrue(SurfaceRules.UNDER_CEILING, phiStone));

        SurfaceRules.RuleSource cavernShell = SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, phiStone),
                SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, phiStone),
                SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, phiStone),
                SurfaceRules.ifTrue(SurfaceRules.UNDER_CEILING, phiStone));

        SurfaceRules.RuleSource plateauColumn = SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), plateauBody),
                cavernShell);

        // Dead Wasteland — vanilla-desert style: sand sheet, one ash underlayer, sandstone below.
        SurfaceRules.RuleSource sand = state(ModBlocks.PARCHED_SAND.get());
        SurfaceRules.RuleSource ash = state(ModBlocks.ASH_SOIL.get());
        SurfaceRules.RuleSource sandstone = state(ModBlocks.PARCHED_SANDSTONE.get());

        SurfaceRules.RuleSource wastelandStack = SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, sand),
                SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, ash),
                SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(0, true, 16, CaveSurface.FLOOR), sandstone));

        SurfaceRules.RuleSource wastelandColumn = SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), wastelandStack));

        // Vitrified Wastes — black glass sand / dirt crust over fused stone.
        SurfaceRules.RuleSource vSand = state(ModBlocks.VITRIFIED_SAND.get());
        SurfaceRules.RuleSource vDirt = state(ModBlocks.VITRIFIED_DIRT.get());
        SurfaceRules.RuleSource vStone = state(ModBlocks.VITRIFIED_STONE.get());

        SurfaceRules.RuleSource vitrifiedFloor = SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.noiseCondition(
                        net.minecraft.world.level.levelgen.Noises.SURFACE, -0.2, 0.05), vSand),
                vDirt);

        SurfaceRules.RuleSource vitrifiedStack = SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, vitrifiedFloor),
                SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, vDirt),
                SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(0, true, 6, CaveSurface.FLOOR), vDirt),
                SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(0, true, 24, CaveSurface.FLOOR), vStone));

        SurfaceRules.RuleSource vitrifiedColumn = SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), vitrifiedStack));

        SurfaceRules.RuleSource vanillaGrassSurface = SurfaceRules.sequence(
                SurfaceRules.ifTrue(isAtOrAboveWaterLevel, state(Blocks.GRASS_BLOCK)), state(Blocks.DIRT));

        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.ESSENCE_PLATEAU), plateauColumn),
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.DEAD_WASTELAND), wastelandColumn),
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.VITRIFIED_WASTES), vitrifiedColumn),
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, vanillaGrassSurface));
    }

    private static SurfaceRules.RuleSource state(Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }
}
