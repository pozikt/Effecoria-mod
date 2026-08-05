package com.effecoria.content;

import com.effecoria.EffecoriaMod;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EffecoriaMod.MOD_ID);

    public static final DeferredItem<BlockItem> ESSENITE_ORE = ITEMS.registerSimpleBlockItem("essonite_ore", ModBlocks.ESSENITE_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_ESSENITE_ORE =
            ITEMS.registerSimpleBlockItem("deepslate_essonite_ore", ModBlocks.DEEPSLATE_ESSENITE_ORE);
    public static final DeferredItem<BlockItem> GRANITE_ESSENITE_ORE =
            ITEMS.registerSimpleBlockItem("granite_essonite_ore", ModBlocks.GRANITE_ESSENITE_ORE);
    public static final DeferredItem<BlockItem> ANDESITE_ESSENITE_ORE =
            ITEMS.registerSimpleBlockItem("andesite_essonite_ore", ModBlocks.ANDESITE_ESSENITE_ORE);
    public static final DeferredItem<BlockItem> DIORITE_ESSENITE_ORE =
            ITEMS.registerSimpleBlockItem("diorite_essonite_ore", ModBlocks.DIORITE_ESSENITE_ORE);
    public static final DeferredItem<BlockItem> TUFF_ESSENITE_ORE =
            ITEMS.registerSimpleBlockItem("tuff_essonite_ore", ModBlocks.TUFF_ESSENITE_ORE);
    public static final DeferredItem<BlockItem> BASALT_ESSENITE_ORE =
            ITEMS.registerSimpleBlockItem("basalt_essonite_ore", ModBlocks.BASALT_ESSENITE_ORE);
    public static final DeferredItem<BlockItem> ESSONITE_BLOCK =
            ITEMS.registerSimpleBlockItem("essonite_block", ModBlocks.ESSONITE_BLOCK);
    public static final DeferredItem<BlockItem> PHI_STONE =
            ITEMS.registerSimpleBlockItem("phi_stone", ModBlocks.PHI_STONE);
    public static final DeferredItem<BlockItem> PHI_DIRT =
            ITEMS.registerSimpleBlockItem("phi_dirt", ModBlocks.PHI_DIRT);
    public static final DeferredItem<BlockItem> PHI_GRASS =
            ITEMS.registerSimpleBlockItem("phi_grass", ModBlocks.PHI_GRASS);
    public static final DeferredItem<BlockItem> ESSONITE_CRYSTAL =
            ITEMS.registerSimpleBlockItem("essonite_crystal", ModBlocks.ESSONITE_CRYSTAL);
    public static final DeferredItem<BlockItem> ESSONITE_CRYSTAL_BUD_SMALL =
            ITEMS.registerSimpleBlockItem("essonite_crystal_bud_small", ModBlocks.ESSONITE_CRYSTAL_BUD_SMALL);
    public static final DeferredItem<BlockItem> ESSONITE_CRYSTAL_BUD_MEDIUM =
            ITEMS.registerSimpleBlockItem("essonite_crystal_bud_medium", ModBlocks.ESSONITE_CRYSTAL_BUD_MEDIUM);
    public static final DeferredItem<BlockItem> ESSONITE_CRYSTAL_BUD_LARGE =
            ITEMS.registerSimpleBlockItem("essonite_crystal_bud_large", ModBlocks.ESSONITE_CRYSTAL_BUD_LARGE);

    public static final DeferredItem<BlockItem> PHI_GEYSER =
            ITEMS.registerSimpleBlockItem("phi_geyser", ModBlocks.PHI_GEYSER);
    public static final DeferredItem<BlockItem> VOID_OBSIDIAN =
            ITEMS.registerSimpleBlockItem("void_obsidian", ModBlocks.VOID_OBSIDIAN);
    public static final DeferredItem<BlockItem> ESSONITE_CRUST =
            ITEMS.registerSimpleBlockItem("essonite_crust", ModBlocks.ESSONITE_CRUST);

    public static final DeferredItem<BlockItem> ESSONITE_DRIPSTONE_BLOCK =
            ITEMS.registerSimpleBlockItem("essonite_dripstone_block", ModBlocks.ESSONITE_DRIPSTONE_BLOCK);
    public static final DeferredItem<BlockItem> ESSONITE_POINTED =
            ITEMS.registerSimpleBlockItem("essonite_pointed", ModBlocks.ESSONITE_POINTED);

    public static final DeferredItem<BlockItem> PHI_BLADES =
            ITEMS.registerSimpleBlockItem("phi_blades", ModBlocks.PHI_BLADES);
    public static final DeferredItem<BlockItem> PHI_LOG =
            ITEMS.registerSimpleBlockItem("phi_log", ModBlocks.PHI_LOG);
    public static final DeferredItem<BlockItem> PHI_LEAVES =
            ITEMS.registerSimpleBlockItem("phi_leaves", ModBlocks.PHI_LEAVES);
    public static final DeferredItem<BlockItem> PHI_SAPLING =
            ITEMS.registerSimpleBlockItem("phi_sapling", ModBlocks.PHI_SAPLING);

    public static final DeferredItem<Item> PHI_NUT = ITEMS.register(
            "phi_nut", () -> new PhiNutItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> ESSONITE_SHARD = ITEMS.registerSimpleItem("essonite_shard");
    public static final DeferredItem<Item> PURE_ESSONITE = ITEMS.registerSimpleItem("pure_essonite");
    public static final DeferredItem<Item> PHI_CHITIN = ITEMS.registerSimpleItem("phi_chitin");

    public static final DeferredItem<Item> PHI_WATER_BUCKET = ITEMS.register(
            "phi_water_bucket",
            () -> new PhiWaterBucketItem(ModFluids.PHI_WATER.get(), new Item.Properties()));

    /** Hyperspace Φ-membrane (creative / debug; worldgen places it in subspace). */
    public static final DeferredItem<BlockItem> PHI_VEIL =
            ITEMS.registerSimpleBlockItem("phi_veil", ModBlocks.PHI_VEIL);

    /** Refined Φ-conductive dust — smelted from essonite ore. */
    public static final DeferredItem<Item> ESSENITE_DUST = ITEMS.registerSimpleItem("essonite_dust");

    /** Initiation focus — school select + tiered resonance bonuses. */
    public static final DeferredItem<Item> RESONANCE_FOCUS = ITEMS.register(
            "resonance_focus",
            () -> new ResonanceFocusItem(new Item.Properties().stacksTo(1)));

    /** Portable Φ buffer for low-Φ caves. */
    public static final DeferredItem<Item> PHI_CELL = ITEMS.register(
            "phi_cell",
            () -> new PhiCellItem(new Item.Properties().stacksTo(1)));

    /** Teaches breathing forms — consumed on use. */
    public static final DeferredItem<Item> BREATHING_SCROLL = ITEMS.register(
            "breathing_scroll",
            () -> new BreathingScrollItem(new Item.Properties().stacksTo(16)));

    /** First-hour magic guide (cast loop, Ψ/Φ, entropy, seals). */
    public static final DeferredItem<Item> MAGIC_PRIMER = ITEMS.register(
            "magic_primer",
            () -> new MagicPrimerItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<SpawnEggItem> PHI_LARVA_SPAWN_EGG = ITEMS.register(
            "phi_larva_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.PHI_LARVA, 0x2a6ad4, 0x9ad7ff, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> CRYSTAL_CRAB_SPAWN_EGG = ITEMS.register(
            "crystal_crab_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CRYSTAL_CRAB, 0x1e3f8a, 0xc9a84c, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> EIDOS_SPAWN_EGG = ITEMS.register(
            "eidos_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.EIDOS, 0xf0d060, 0xfff6c8, new Item.Properties()));
}
