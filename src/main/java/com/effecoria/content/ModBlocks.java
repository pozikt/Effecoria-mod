package com.effecoria.content;

import com.effecoria.EffecoriaMod;
import com.effecoria.block.PhiFieldBlock;
import com.effecoria.block.PhiGrassBlock;
import com.effecoria.block.SubspacePortalBlock;

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
