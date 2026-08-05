package com.effecoria.content;

import com.effecoria.EffecoriaMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EffecoriaMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EFFECORIA_TAB = CREATIVE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.effecoria"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> ModItems.RESONANCE_FOCUS.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModItems.ESSENITE_ORE.get());
                        output.accept(ModItems.DEEPSLATE_ESSENITE_ORE.get());
                        output.accept(ModItems.GRANITE_ESSENITE_ORE.get());
                        output.accept(ModItems.ANDESITE_ESSENITE_ORE.get());
                        output.accept(ModItems.DIORITE_ESSENITE_ORE.get());
                        output.accept(ModItems.TUFF_ESSENITE_ORE.get());
                        output.accept(ModItems.BASALT_ESSENITE_ORE.get());
                        output.accept(ModItems.ESSONITE_BLOCK.get());
                        output.accept(ModItems.PARCHED_SAND.get());
                        output.accept(ModItems.ASH_SOIL.get());
                        output.accept(ModItems.PARCHED_SANDSTONE.get());
                        output.accept(ModItems.PHI_STONE.get());
                        output.accept(ModItems.PHI_DIRT.get());
                        output.accept(ModItems.PHI_GRASS.get());
                        output.accept(ModItems.ESSONITE_CRYSTAL.get());
                        output.accept(ModItems.ESSONITE_CRYSTAL_BUD_SMALL.get());
                        output.accept(ModItems.ESSONITE_CRYSTAL_BUD_MEDIUM.get());
                        output.accept(ModItems.ESSONITE_CRYSTAL_BUD_LARGE.get());
                        output.accept(ModItems.ESSONITE_DRIPSTONE_BLOCK.get());
                        output.accept(ModItems.ESSONITE_POINTED.get());
                        output.accept(ModItems.PHI_GEYSER.get());
                        output.accept(ModItems.VOID_OBSIDIAN.get());
                        output.accept(ModItems.ESSONITE_CRUST.get());
                        output.accept(ModItems.PHI_BLADES.get());
                        output.accept(ModItems.PHI_LOG.get());
                        output.accept(ModItems.PHI_LEAVES.get());
                        output.accept(ModItems.PHI_SAPLING.get());
                        output.accept(ModItems.PHI_NUT.get());
                        output.accept(ModItems.PHI_WATER_BUCKET.get());
                        output.accept(ModItems.ESSONITE_SHARD.get());
                        output.accept(ModItems.PURE_ESSONITE.get());
                        output.accept(ModItems.PHI_CHITIN.get());
                        output.accept(ModItems.ESSENITE_DUST.get());
                        output.accept(ModItems.RESONANCE_FOCUS.get());
                        output.accept(ModItems.PHI_CELL.get());
                        output.accept(ModItems.BREATHING_SCROLL.get());
                        output.accept(ModItems.MAGIC_PRIMER.get());
                        output.accept(ModItems.PHI_LARVA_SPAWN_EGG.get());
                        output.accept(ModItems.CRYSTAL_CRAB_SPAWN_EGG.get());
                        output.accept(ModItems.EIDOS_SPAWN_EGG.get());
                    })
                    .build());
}
