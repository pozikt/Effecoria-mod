package com.effecoria.content;

import com.effecoria.EffecoriaMod;
import com.effecoria.block.EssoniteCrustBlock;
import com.effecoria.block.EssoniteDustBlock;
import com.effecoria.block.EssonitePointedBlock;
import com.effecoria.block.PhiBladesBlock;
import com.effecoria.block.PhiFieldBlock;
import com.effecoria.block.PhiGeyserBlock;
import com.effecoria.block.PhiGrassBlock;
import com.effecoria.block.PhiLeavesBlock;
import com.effecoria.block.PhiLogBlock;
import com.effecoria.block.PhiSaplingBlock;
import com.effecoria.block.SubspacePortalBlock;
import com.effecoria.world.ModTreeGrowers;

import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.util.valueproviders.UniformInt;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EffecoriaMod.MOD_ID);

    /** Φ-piezo mineral in stone — ultramarine crystalline infection. */
    public static final DeferredBlock<Block> ESSENITE_ORE = registerOre("essonite_ore", MapColor.STONE, 3f, 3f, SoundType.STONE);

    public static final DeferredBlock<Block> DEEPSLATE_ESSENITE_ORE =
            registerOre("deepslate_essonite_ore", MapColor.DEEPSLATE, 4.5f, 3f, SoundType.DEEPSLATE);

    public static final DeferredBlock<Block> GRANITE_ESSENITE_ORE =
            registerOre("granite_essonite_ore", MapColor.DIRT, 3f, 3f, SoundType.STONE);

    public static final DeferredBlock<Block> ANDESITE_ESSENITE_ORE =
            registerOre("andesite_essonite_ore", MapColor.STONE, 3f, 3f, SoundType.STONE);

    public static final DeferredBlock<Block> DIORITE_ESSENITE_ORE =
            registerOre("diorite_essonite_ore", MapColor.QUARTZ, 3f, 3f, SoundType.STONE);

    public static final DeferredBlock<Block> TUFF_ESSENITE_ORE =
            registerOre("tuff_essonite_ore", MapColor.TERRACOTTA_GRAY, 3f, 3f, SoundType.TUFF);

    public static final DeferredBlock<Block> BASALT_ESSENITE_ORE =
            registerOre("basalt_essonite_ore", MapColor.COLOR_BLACK, 3.2f, 3f, SoundType.BASALT);

    /** Nearly pure essonite — Φ-core mass under the plateau (Y ≤ 0). */
    public static final DeferredBlock<Block> ESSONITE_BLOCK = BLOCKS.register(
            "essonite_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .requiresCorrectToolForDrops()
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 12)));

    /** Dead Wasteland surface — bleached, Φ-starved sand. */
    public static final DeferredBlock<Block> PARCHED_SAND = BLOCKS.register(
            "parched_sand",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(0.5f)
                    .sound(SoundType.SAND)));

    /** Dead Wasteland soil — ash-gray cracked earth. */
    public static final DeferredBlock<Block> ASH_SOIL = BLOCKS.register(
            "ash_soil",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.55f)
                    .sound(SoundType.GRAVEL)));

    /** Bleached desert sandstone under the wasteland crust. */
    public static final DeferredBlock<Block> PARCHED_SANDSTONE = BLOCKS.register(
            "parched_sandstone",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .requiresCorrectToolForDrops()
                    .strength(0.8f, 0.8f)
                    .sound(SoundType.STONE)));

    /** Φ-Glass Plain — fused lightning-sand crust (glows at night). */
    public static final DeferredBlock<Block> PHI_GLASS = BLOCKS.register(
            "phi_glass",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(0.4f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .isValidSpawn((s, l, p, t) -> false)
                    .isRedstoneConductor((s, l, p) -> false)
                    .isSuffocating((s, l, p) -> false)
                    .isViewBlocking((s, l, p) -> false)
                    .lightLevel(state -> 6)));

    /** Softer dune sheet of Φ-glass grit. */
    public static final DeferredBlock<Block> PHI_GLASS_DUNE = BLOCKS.register(
            "phi_glass_dune",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(0.35f)
                    .sound(SoundType.SAND)
                    .lightLevel(state -> 4)));

    /** Φ-barghan quicksand — essonite dust mass. */
    public static final DeferredBlock<EssoniteDustBlock> ESSONITE_DUST_BLOCK = BLOCKS.register(
            "essonite_dust_block",
            () -> new EssoniteDustBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.25f)
                    .sound(SoundType.SAND)
                    .dynamicShape()
                    .lightLevel(state -> 3)));

    /** Φ-quartz seam / pocket crystal ore. */
    public static final DeferredBlock<Block> PHI_QUARTZ = BLOCKS.register(
            "phi_quartz",
            () -> new DropExperienceBlock(
                    UniformInt.of(1, 3),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .requiresCorrectToolForDrops()
                            .strength(2.8f, 3.0f)
                            .sound(SoundType.AMETHYST)
                            .lightLevel(state -> 8)));

    /** Φ-saturated stone — glows and slowly converts adjacent stone. */
    public static final DeferredBlock<PhiFieldBlock> PHI_STONE = BLOCKS.register(
            "phi_stone",
            () -> new PhiFieldBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .requiresCorrectToolForDrops()
                    .strength(1.5f, 6f)
                    .sound(ModSoundTypes.PHI_STONE)
                    .lightLevel(state -> 7)));

    /** Φ-saturated earth — glows and slowly converts dirt / grass. */
    public static final DeferredBlock<PhiFieldBlock> PHI_DIRT = BLOCKS.register(
            "phi_dirt",
            () -> new PhiFieldBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(0.6f, 0.6f)
                    .sound(ModSoundTypes.PHI_EARTH)
                    .lightLevel(state -> 5)));

    /** Φ-turf — surface layer over Φ-earth in the plateau. */
    public static final DeferredBlock<PhiGrassBlock> PHI_GRASS = BLOCKS.register(
            "phi_grass",
            () -> new PhiGrassBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(0.6f, 0.6f)
                    .sound(ModSoundTypes.PHI_GRASS)
                    .lightLevel(state -> 6)));

    /** Surface Φ-crystal cluster — amethyst-shaped, drops essonite dust. */
    public static final DeferredBlock<AmethystClusterBlock> ESSONITE_CRYSTAL = BLOCKS.register(
            "essonite_crystal",
            () -> new AmethystClusterBlock(
                    7,
                    3,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLUE)
                            .forceSolidOn()
                            .noOcclusion()
                            .randomTicks()
                            .strength(1.5f)
                            .sound(SoundType.AMETHYST_CLUSTER)
                            .lightLevel(state -> 9)
                            .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<AmethystClusterBlock> ESSONITE_CRYSTAL_BUD_SMALL = BLOCKS.register(
            "essonite_crystal_bud_small",
            () -> new AmethystClusterBlock(
                    3,
                    4,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLUE)
                            .forceSolidOn()
                            .noOcclusion()
                            .randomTicks()
                            .strength(1.5f)
                            .sound(SoundType.SMALL_AMETHYST_BUD)
                            .lightLevel(state -> 3)
                            .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<AmethystClusterBlock> ESSONITE_CRYSTAL_BUD_MEDIUM = BLOCKS.register(
            "essonite_crystal_bud_medium",
            () -> new AmethystClusterBlock(
                    4,
                    3,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLUE)
                            .forceSolidOn()
                            .noOcclusion()
                            .randomTicks()
                            .strength(1.5f)
                            .sound(SoundType.MEDIUM_AMETHYST_BUD)
                            .lightLevel(state -> 5)
                            .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<AmethystClusterBlock> ESSONITE_CRYSTAL_BUD_LARGE = BLOCKS.register(
            "essonite_crystal_bud_large",
            () -> new AmethystClusterBlock(
                    5,
                    3,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLUE)
                            .forceSolidOn()
                            .noOcclusion()
                            .randomTicks()
                            .strength(1.5f)
                            .sound(SoundType.LARGE_AMETHYST_BUD)
                            .lightLevel(state -> 7)
                            .pushReaction(PushReaction.DESTROY)));

    /**
     * Translucent Φ-membrane underfoot in hyperspace — replaces the old end-stone sheet so the
     * ultramarine Φ-ocean shows through.
     */
    public static final DeferredBlock<Block> PHI_VEIL = BLOCKS.register(
            "phi_veil",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(-1.0f, 3600000.0f)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 4)
                    .noOcclusion()
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .pushReaction(PushReaction.BLOCK)
                    .noLootTable()));

    /** Soft spatial rift — two-block puncture used by subspace voyage. */
    public static final DeferredBlock<SubspacePortalBlock> SUBSPACE_PORTAL = BLOCKS.register(
            "subspace_portal",
            () -> new SubspacePortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .noCollission()
                    .noOcclusion()
                    .lightLevel(state -> 6)
                    .strength(-1.0f, 3600000.0f)
                    .sound(SoundType.GLASS)
                    .pushReaction(PushReaction.BLOCK)
                    .noLootTable()
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)));

    /** Planetary Φ-crack — cyclic plasma geyser of the Essence Plateau. */
    public static final DeferredBlock<PhiGeyserBlock> PHI_GEYSER = BLOCKS.register(
            "phi_geyser",
            () -> new PhiGeyserBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(2.5f, 12f)
                    .sound(ModSoundTypes.PHI_STONE)
                    .lightLevel(state -> switch (state.getValue(PhiGeyserBlock.PHASE)) {
                        case DORMANT -> 7;
                        case PRECURSOR -> 11;
                        case ERUPTING -> 15;
                        case COOLDOWN -> 8;
                    })
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    /** Glazed void-glass rim left by Φ-plasma — rare Ω-ward material. */
    public static final DeferredBlock<Block> VOID_OBSIDIAN = BLOCKS.register(
            "void_obsidian",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .requiresCorrectToolForDrops()
                    .strength(50f, 1200f)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 2)));

    /** Cooled essonite melt puddle after a geyser eruption. */
    public static final DeferredBlock<EssoniteCrustBlock> ESSONITE_CRUST = BLOCKS.register(
            "essonite_crust",
            () -> new EssoniteCrustBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.4f, 0.4f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 8)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));

    /** Dense essonite dripstone — base for Φ-conductor spikes. */
    public static final DeferredBlock<Block> ESSONITE_DRIPSTONE_BLOCK = BLOCKS.register(
            "essonite_dripstone_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .requiresCorrectToolForDrops()
                    .strength(1.5f, 1.5f)
                    .sound(SoundType.DRIPSTONE_BLOCK)
                    .lightLevel(state -> 5)));

    /** Stalactites / stalagmites / Φ-columns — vertical Φ-conductors. */
    public static final DeferredBlock<EssonitePointedBlock> ESSONITE_POINTED = BLOCKS.register(
            "essonite_pointed",
            () -> new EssonitePointedBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .noOcclusion()
                    .sound(SoundType.POINTED_DRIPSTONE)
                    .randomTicks()
                    .strength(1.5f, 1.5f)
                    .dynamicShape()
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY)
                    .lightLevel(state -> switch (state.getValue(EssonitePointedBlock.THICKNESS)) {
                        case TIP -> 12;
                        case TIP_MERGE -> 10;
                        case FRUSTUM -> 8;
                        case MIDDLE -> 6;
                        case BASE -> 5;
                    })));

    /** Fiberoptic Φ-grass shoots — plant layer on Φ-turf. */
    public static final DeferredBlock<PhiBladesBlock> PHI_BLADES = BLOCKS.register(
            "phi_blades",
            () -> new PhiBladesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .noCollission()
                    .instabreak()
                    .sound(ModSoundTypes.PHI_GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY)
                    .lightLevel(state -> 4)
                    .randomTicks()));

    public static final DeferredBlock<PhiLogBlock> PHI_LOG = BLOCKS.register(
            "phi_log",
            () -> new PhiLogBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(3.0f, 4.0f)
                    .sound(SoundType.WOOD)
                    .lightLevel(state -> 4)
                    .ignitedByLava()));

    public static final DeferredBlock<PhiLeavesBlock> PHI_LEAVES = BLOCKS.register(
            "phi_leaves",
            () -> new PhiLeavesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(0.2f)
                    .randomTicks()
                    .noOcclusion()
                    .sound(SoundType.CHERRY_LEAVES)
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .lightLevel(state -> 6)));

    public static final DeferredBlock<PhiSaplingBlock> PHI_SAPLING = BLOCKS.register(
            "phi_sapling",
            () -> new PhiSaplingBlock(
                    ModTreeGrowers.PHI,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.PLANT)
                            .noCollission()
                            .randomTicks()
                            .instabreak()
                            .sound(SoundType.GRASS)
                            .pushReaction(PushReaction.DESTROY)
                            .lightLevel(state -> 3)));

    /** Mirage blood lake fluid — client-tinted crimson water. */
    public static final DeferredBlock<LiquidBlock> BLOOD_FLUID = BLOCKS.register(
            "blood_fluid",
            () -> new LiquidBlock(
                    ModFluids.BLOOD.get(),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_RED)
                            .replaceable()
                            .noCollission()
                            .strength(100.0f)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()
                            .liquid()
                            .sound(SoundType.EMPTY)));

    /** Cave Φ-hydrolat — aquamarine essence lakes under the plateau. */
    public static final DeferredBlock<LiquidBlock> PHI_WATER = BLOCKS.register(
            "phi_water",
            () -> new LiquidBlock(
                    ModFluids.PHI_WATER.get(),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .replaceable()
                            .noCollission()
                            .strength(100.0f)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()
                            .liquid()
                            .lightLevel(state -> 8)
                            .sound(SoundType.EMPTY)));

    private static DeferredBlock<Block> registerOre(
            String name, MapColor color, float hardness, float resistance, SoundType sound) {
        return BLOCKS.register(
                name,
                () -> new DropExperienceBlock(
                        UniformInt.of(1, 3),
                        BlockBehaviour.Properties.of()
                                .mapColor(color)
                                .requiresCorrectToolForDrops()
                                .strength(hardness, resistance)
                                .sound(sound)));
    }
}
