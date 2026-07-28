package com.effecoria.content;

import com.effecoria.EffecoriaMod;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EffecoriaMod.MOD_ID);

    public static final DeferredItem<BlockItem> ESSENITE_ORE = ITEMS.registerSimpleBlockItem("essonite_ore", ModBlocks.ESSENITE_ORE);

    /** Initiation focus — fixes magic school on first use (phase 1) */
    public static final DeferredItem<Item> RESONANCE_FOCUS = ITEMS.register(
            "resonance_focus",
            () -> new ResonanceFocusItem(new Item.Properties().stacksTo(1)));

    /** Teaches breathing forms — consumed on use. */
    public static final DeferredItem<Item> BREATHING_SCROLL = ITEMS.register(
            "breathing_scroll",
            () -> new BreathingScrollItem(new Item.Properties().stacksTo(16)));
}
