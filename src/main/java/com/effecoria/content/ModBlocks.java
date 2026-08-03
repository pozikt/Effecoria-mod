package com.effecoria.content;

import com.effecoria.EffecoriaMod;
import com.effecoria.block.SubspacePortalBlock;

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
