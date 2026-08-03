package com.effecoria.content;

import com.effecoria.EffecoriaMod;
import com.effecoria.block.SubspacePortalBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EffecoriaMod.MOD_ID);

    /** Placeholder ore — Φ-piezo mineral (phase 0) */
    public static final DeferredBlock<Block> ESSENITE_ORE = BLOCKS.registerSimpleBlock(
            "essonite_ore",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3f, 3f));

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
}
