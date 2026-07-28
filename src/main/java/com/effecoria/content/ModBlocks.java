package com.effecoria.content;

import com.effecoria.EffecoriaMod;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EffecoriaMod.MOD_ID);

    /** Placeholder ore — Φ-piezo mineral (phase 0) */
    public static final DeferredBlock<Block> ESSENITE_ORE = BLOCKS.registerSimpleBlock(
            "essonite_ore",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3f, 3f));
}
