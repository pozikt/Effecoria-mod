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
                        output.accept(ModItems.PHI_STONE.get());
                        output.accept(ModItems.PHI_DIRT.get());
                        output.accept(ModItems.PHI_GRASS.get());
                        output.accept(ModItems.ESSONITE_CRYSTAL.get());
                        output.accept(ModItems.ESSONITE_CRYSTAL_BUD_SMALL.get());
                        output.accept(ModItems.ESSONITE_CRYSTAL_BUD_MEDIUM.get());
                        output.accept(ModItems.ESSONITE_CRYSTAL_BUD_LARGE.get());
                        output.accept(ModItems.ESSENITE_DUST.get());
                        output.accept(ModItems.RESONANCE_FOCUS.get());
                        output.accept(ModItems.PHI_CELL.get());
                        output.accept(ModItems.BREATHING_SCROLL.get());
                        output.accept(ModItems.MAGIC_PRIMER.get());
                    })
                    .build());
}
