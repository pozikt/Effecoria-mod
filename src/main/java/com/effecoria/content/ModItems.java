package com.effecoria.content;

import com.effecoria.EffecoriaMod;
import com.effecoria.item.EssenceGlueItem;
import com.effecoria.armor.EssoniteArmorItem;
import com.effecoria.armor.EssoniteArmorTier;
import com.effecoria.armor.EssonitePhoneme;
import com.effecoria.core.disease.PhiDisease;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
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
    public static final DeferredItem<BlockItem> LEAD_ORE =
            ITEMS.registerSimpleBlockItem("lead_ore", ModBlocks.LEAD_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_LEAD_ORE =
            ITEMS.registerSimpleBlockItem("deepslate_lead_ore", ModBlocks.DEEPSLATE_LEAD_ORE);
    public static final DeferredItem<BlockItem> LEAD_BLOCK =
            ITEMS.registerSimpleBlockItem("lead_block", ModBlocks.LEAD_BLOCK);
    public static final DeferredItem<BlockItem> MITHRIL_ORE =
            ITEMS.registerSimpleBlockItem("mithril_ore", ModBlocks.MITHRIL_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_MITHRIL_ORE =
            ITEMS.registerSimpleBlockItem("deepslate_mithril_ore", ModBlocks.DEEPSLATE_MITHRIL_ORE);
    public static final DeferredItem<BlockItem> MITHRIL_BLOCK = ITEMS.register(
            "mithril_block",
            () -> new HintBlockItem(
                    ModBlocks.MITHRIL_BLOCK.get(), new Item.Properties(), "tooltip.effecoria.mithril_block"));
    public static final DeferredItem<BlockItem> ESSONITE_BLOCK =
            ITEMS.registerSimpleBlockItem("essonite_block", ModBlocks.ESSONITE_BLOCK);
    public static final DeferredItem<BlockItem> STAR_ESSONITE_BLOCK =
            ITEMS.registerSimpleBlockItem("star_essonite_block", ModBlocks.STAR_ESSONITE_BLOCK);
    public static final DeferredItem<BlockItem> WHISPERING_SPIRE_VENT =
            ITEMS.registerSimpleBlockItem("whispering_spire_vent", ModBlocks.WHISPERING_SPIRE_VENT);
    public static final DeferredItem<BlockItem> PARCHED_SAND =
            ITEMS.registerSimpleBlockItem("parched_sand", ModBlocks.PARCHED_SAND);
    public static final DeferredItem<BlockItem> ASH_SOIL =
            ITEMS.registerSimpleBlockItem("ash_soil", ModBlocks.ASH_SOIL);
    public static final DeferredItem<BlockItem> PARCHED_SANDSTONE =
            ITEMS.registerSimpleBlockItem("parched_sandstone", ModBlocks.PARCHED_SANDSTONE);
    public static final DeferredItem<BlockItem> VITRIFIED_DIRT =
            ITEMS.registerSimpleBlockItem("vitrified_dirt", ModBlocks.VITRIFIED_DIRT);
    public static final DeferredItem<BlockItem> VITRIFIED_STONE =
            ITEMS.registerSimpleBlockItem("vitrified_stone", ModBlocks.VITRIFIED_STONE);
    public static final DeferredItem<BlockItem> VITRIFIED_SAND =
            ITEMS.registerSimpleBlockItem("vitrified_sand", ModBlocks.VITRIFIED_SAND);
    public static final DeferredItem<BlockItem> VITRIFIED_LOG =
            ITEMS.registerSimpleBlockItem("vitrified_log", ModBlocks.VITRIFIED_LOG);
    public static final DeferredItem<BlockItem> VITRIFIED_BRANCHES =
            ITEMS.registerSimpleBlockItem("vitrified_branches", ModBlocks.VITRIFIED_BRANCHES);
    public static final DeferredItem<BlockItem> VITRIFIED_GEYSER_CRACK =
            ITEMS.registerSimpleBlockItem("vitrified_geyser_crack", ModBlocks.VITRIFIED_GEYSER_CRACK);
    public static final DeferredItem<BlockItem> PHI_STONE =
            ITEMS.registerSimpleBlockItem("phi_stone", ModBlocks.PHI_STONE);
    public static final DeferredItem<BlockItem> PHI_COBBLE =
            ITEMS.registerSimpleBlockItem("phi_cobble", ModBlocks.PHI_COBBLE);
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

    public static final DeferredItem<BlockItem> OMEGA_CRYSTAL =
            ITEMS.registerSimpleBlockItem("omega_crystal", ModBlocks.OMEGA_CRYSTAL);
    public static final DeferredItem<BlockItem> OMEGA_CRYSTAL_BUD_SMALL =
            ITEMS.registerSimpleBlockItem("omega_crystal_bud_small", ModBlocks.OMEGA_CRYSTAL_BUD_SMALL);
    public static final DeferredItem<BlockItem> OMEGA_CRYSTAL_BUD_MEDIUM =
            ITEMS.registerSimpleBlockItem("omega_crystal_bud_medium", ModBlocks.OMEGA_CRYSTAL_BUD_MEDIUM);
    public static final DeferredItem<BlockItem> OMEGA_CRYSTAL_BUD_LARGE =
            ITEMS.registerSimpleBlockItem("omega_crystal_bud_large", ModBlocks.OMEGA_CRYSTAL_BUD_LARGE);
    public static final DeferredItem<BlockItem> OMEGA_BLADES =
            ITEMS.registerSimpleBlockItem("omega_blades", ModBlocks.OMEGA_BLADES);
    public static final DeferredItem<BlockItem> ROTTEN_MOSS =
            ITEMS.registerSimpleBlockItem("rotten_moss", ModBlocks.ROTTEN_MOSS);
    public static final DeferredItem<BlockItem> ELDRITCH_BLOOD_PUDDLE =
            ITEMS.registerSimpleBlockItem("eldritch_blood_puddle", ModBlocks.ELDRITCH_BLOOD_PUDDLE);

    /** Concentrated Ω (b) crystal fragment — forbidden craft / ritual fuel. */
    public static final DeferredItem<Item> OMEGA_CRYSTAL_SHARD = ITEMS.registerSimpleItem("omega_crystal_shard");

    /** Ω-stained bone from Scar fauna — material for later Ω gear. */
    public static final DeferredItem<Item> DISTORTED_BONE = ITEMS.registerSimpleItem("distorted_bone");

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
    public static final DeferredItem<BlockItem> PHI_LOG = ITEMS.register(
            "phi_log",
            () -> new FuelBlockItem(ModBlocks.PHI_LOG.get(), new Item.Properties(), 300));
    public static final DeferredItem<BlockItem> PHI_PLANKS = ITEMS.register(
            "phi_planks",
            () -> new FuelBlockItem(ModBlocks.PHI_PLANKS.get(), new Item.Properties(), 300));
    public static final DeferredItem<BlockItem> PHI_GLASS =
            ITEMS.registerSimpleBlockItem("phi_glass", ModBlocks.PHI_GLASS);
    public static final DeferredItem<BlockItem> PHI_LEAVES =
            ITEMS.registerSimpleBlockItem("phi_leaves", ModBlocks.PHI_LEAVES);
    public static final DeferredItem<BlockItem> PHI_SAPLING =
            ITEMS.registerSimpleBlockItem("phi_sapling", ModBlocks.PHI_SAPLING);
    public static final DeferredItem<BlockItem> ANCIENT_ESSENCE_WOOD = ITEMS.register(
            "ancient_essence_wood",
            () -> new FuelBlockItem(ModBlocks.ANCIENT_ESSENCE_WOOD.get(), new Item.Properties(), 400));
    public static final DeferredItem<BlockItem> GOLDEN_BARK =
            ITEMS.registerSimpleBlockItem("golden_bark", ModBlocks.GOLDEN_BARK);
    public static final DeferredItem<BlockItem> PHI_SNARE_VINE =
            ITEMS.registerSimpleBlockItem("phi_snare_vine", ModBlocks.PHI_SNARE_VINE);

    public static final DeferredItem<Item> PHI_NUT = ITEMS.register(
            "phi_nut", () -> new PhiNutItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> GIANT_PHI_NUT = ITEMS.register(
            "giant_phi_nut", () -> new GiantPhiNutItem(new Item.Properties().stacksTo(8)));
    public static final DeferredItem<Item> ANCIENT_HEARTWOOD = ITEMS.register(
            "ancient_heartwood", () -> new AncientHeartwoodItem(new Item.Properties()));

    public static final DeferredItem<Item> ESSONITE_SHARD = ITEMS.registerSimpleItem("essonite_shard");
    public static final DeferredItem<Item> PURE_ESSONITE = ITEMS.registerSimpleItem("pure_essonite");
    public static final DeferredItem<Item> STAR_ESSONITE = ITEMS.registerSimpleItem("star_essonite");
    public static final DeferredItem<Item> RAW_LEAD = ITEMS.registerSimpleItem("raw_lead");
    public static final DeferredItem<Item> LEAD_INGOT = ITEMS.registerSimpleItem("lead_ingot");
    public static final DeferredItem<Item> LEAD_NUGGET = ITEMS.registerSimpleItem("lead_nugget");
    public static final DeferredItem<Item> RAW_MITHRIL = ITEMS.registerSimpleItem("raw_mithril");
    public static final DeferredItem<Item> MITHRIL_INGOT = ITEMS.registerSimpleItem("mithril_ingot");
    public static final DeferredItem<Item> MITHRIL_NUGGET = ITEMS.registerSimpleItem("mithril_nugget");
    public static final DeferredItem<Item> MITHRIL_WIRE = ITEMS.register(
            "mithril_wire", () -> new MithrilWireItem(new Item.Properties()));
    /** Synthetic Φ-fuel — Era II workshop products compressed for Era IV reactors. */
    public static final DeferredItem<Item> PHI_FLUX_SLUG = ITEMS.registerSimpleItem("phi_flux_slug");
    public static final DeferredItem<Item> PHI_CHITIN = ITEMS.registerSimpleItem("phi_chitin");

    public static final DeferredItem<Item> PHI_WATER_BUCKET = ITEMS.register(
            "phi_water_bucket",
            () -> new PhiWaterBucketItem(ModFluids.PHI_WATER.get(), new Item.Properties()));

    /** Gold-filtered Φ-water — drinkable Ψ tonic (does not place fluid). */
    public static final DeferredItem<Item> PURIFIED_PHI_WATER_BUCKET = ITEMS.register(
            "purified_phi_water_bucket",
            () -> new PurifiedPhiWaterBucketItem(new Item.Properties()));

    public static final DeferredItem<BlockItem> MORTAR_AND_PESTLE =
            ITEMS.registerSimpleBlockItem("mortar_and_pestle", ModBlocks.MORTAR_AND_PESTLE);
    public static final DeferredItem<BlockItem> PHI_TORCH = ITEMS.register(
            "phi_torch",
            () -> new HintBlockItem(ModBlocks.PHI_TORCH.get(), new Item.Properties(), "tooltip.effecoria.phi_torch"));
    public static final DeferredItem<BlockItem> PHI_CAMPFIRE = ITEMS.register(
            "phi_campfire",
            () -> new HintBlockItem(
                    ModBlocks.PHI_CAMPFIRE.get(), new Item.Properties(), "tooltip.effecoria.phi_campfire"));
    public static final DeferredItem<BlockItem> CLAY_CRUCIBLE = ITEMS.register(
            "clay_crucible",
            () -> new HintBlockItem(
                    ModBlocks.CLAY_CRUCIBLE.get(), new Item.Properties(), "tooltip.effecoria.clay_crucible"));
    public static final DeferredItem<BlockItem> PHI_FURNACE = ITEMS.register(
            "phi_furnace",
            () -> new HintBlockItem(
                    ModBlocks.PHI_FURNACE.get(), new Item.Properties(), "tooltip.effecoria.phi_furnace"));
    public static final DeferredItem<BlockItem> PSI_IMPRINTER = ITEMS.register(
            "psi_imprinter",
            () -> new HintBlockItem(
                    ModBlocks.PSI_IMPRINTER.get(), new Item.Properties(), "tooltip.effecoria.psi_imprinter"));
    public static final DeferredItem<BlockItem> SHAFT_LATHE = ITEMS.register(
            "shaft_lathe",
            () -> new HintBlockItem(
                    ModBlocks.SHAFT_LATHE.get(), new Item.Properties(), "tooltip.effecoria.shaft_lathe"));
    public static final DeferredItem<BlockItem> FACET_CUTTER = ITEMS.register(
            "facet_cutter",
            () -> new HintBlockItem(
                    ModBlocks.FACET_CUTTER.get(), new Item.Properties(), "tooltip.effecoria.facet_cutter"));
    public static final DeferredItem<BlockItem> ARTIFACT_ASSEMBLER = ITEMS.register(
            "artifact_assembler",
            () -> new HintBlockItem(
                    ModBlocks.ARTIFACT_ASSEMBLER.get(),
                    new Item.Properties(),
                    "tooltip.effecoria.artifact_assembler"));
    public static final DeferredItem<BlockItem> SEAL_INSCRIBER = ITEMS.register(
            "seal_inscriber",
            () -> new HintBlockItem(
                    ModBlocks.SEAL_INSCRIBER.get(), new Item.Properties(), "tooltip.effecoria.seal_inscriber"));
    public static final DeferredItem<BlockItem> PHI_TELEGRAPH =
            ITEMS.registerSimpleBlockItem("phi_telegraph", ModBlocks.PHI_TELEGRAPH);
    public static final DeferredItem<Item> GOLEM_CHASSIS = ITEMS.register(
            "golem_chassis", () -> new GolemChassisItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> TELEGRAPH_MODULE = ITEMS.register(
            "telegraph_module",
            () -> new HintItem(new Item.Properties().stacksTo(16), "tooltip.effecoria.telegraph_module"));

    public static final DeferredItem<Item> CARVED_SHAFT = ITEMS.register(
            "carved_shaft", () -> new ModularPartItem(new Item.Properties().stacksTo(16), "shaft"));
    public static final DeferredItem<Item> FACETED_FOCUS = ITEMS.register(
            "faceted_focus", () -> new ModularPartItem(new Item.Properties().stacksTo(16), "focus"));
    public static final DeferredItem<Item> JEWELRY_BAND = ITEMS.register(
            "jewelry_band", () -> new ModularPartItem(new Item.Properties().stacksTo(16), "band"));
    public static final DeferredItem<Item> JEWELRY_GEM = ITEMS.register(
            "jewelry_gem", () -> new ModularPartItem(new Item.Properties().stacksTo(16), "gem"));
    public static final DeferredItem<Item> MODULAR_STAFF = ITEMS.register(
            "modular_staff", () -> new ModularStaffItem(new Item.Properties().stacksTo(1).durability(500)));
    public static final DeferredItem<Item> ASSEMBLED_RING = ITEMS.register(
            "assembled_ring",
            () -> new AssembledJewelryItem(
                    new Item.Properties(), "item.effecoria.assembled_ring.hint", 0.3f, "ring"));
    public static final DeferredItem<Item> ASSEMBLED_AMULET = ITEMS.register(
            "assembled_amulet",
            () -> new AssembledJewelryItem(
                    new Item.Properties(), "item.effecoria.assembled_amulet.hint", 0.5f, "amulet"));
    public static final DeferredItem<Item> ASSEMBLED_CHARM = ITEMS.register(
            "assembled_charm",
            () -> new AssembledJewelryItem(
                    new Item.Properties(), "item.effecoria.assembled_charm.hint", 0.45f, "charm"));
    public static final DeferredItem<Item> ITEM_SEAL_PRIMER = ITEMS.register(
            "item_seal_primer", () -> new ItemSealPrimerItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<BlockItem> ESSENCE_BURNER = ITEMS.register(
            "essence_burner",
            () -> new EssenceBurnerBlockItem(ModBlocks.ESSENCE_BURNER.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> SPARK_REACTOR = ITEMS.register(
            "spark_reactor",
            () -> new HintBlockItem(
                    ModBlocks.SPARK_REACTOR.get(), new Item.Properties(), "tooltip.effecoria.spark_reactor"));
    public static final DeferredItem<BlockItem> HEART_REACTOR_CORE = ITEMS.register(
            "heart_reactor_core",
            () -> new HintBlockItem(
                    ModBlocks.HEART_REACTOR_CORE.get(), new Item.Properties(), "tooltip.effecoria.heart_reactor"));
    public static final DeferredItem<BlockItem> FORGE_REACTOR_CORE = ITEMS.register(
            "forge_reactor_core",
            () -> new HintBlockItem(
                    ModBlocks.FORGE_REACTOR_CORE.get(), new Item.Properties(), "tooltip.effecoria.forge_reactor"));
    public static final DeferredItem<BlockItem> GEO_CASING =
            ITEMS.registerSimpleBlockItem("geo_casing", ModBlocks.GEO_CASING);
    public static final DeferredItem<BlockItem> GEO_WELL_CORE = ITEMS.register(
            "geo_well_core",
            () -> new HintBlockItem(
                    ModBlocks.GEO_WELL_CORE.get(), new Item.Properties(), "tooltip.effecoria.geo_well"));
    public static final DeferredItem<BlockItem> CLIMATE_ARRAY = ITEMS.register(
            "climate_array",
            () -> new HintBlockItem(
                    ModBlocks.CLIMATE_ARRAY.get(), new Item.Properties(), "tooltip.effecoria.climate_array"));
    public static final DeferredItem<BlockItem> PORTAL_MODULATOR = ITEMS.register(
            "portal_modulator",
            () -> new HintBlockItem(
                    ModBlocks.PORTAL_MODULATOR.get(), new Item.Properties(), "tooltip.effecoria.portal_modulator"));
    public static final DeferredItem<BlockItem> PHI_BEACON = ITEMS.register(
            "phi_beacon",
            () -> new HintBlockItem(
                    ModBlocks.PHI_BEACON.get(), new Item.Properties(), "tooltip.effecoria.phi_beacon"));

    public static final DeferredItem<BlockItem> TOWER_ANCHOR = ITEMS.register(
            "tower_anchor",
            () -> new HintBlockItem(
                    ModBlocks.TOWER_ANCHOR.get(), new Item.Properties(), "tooltip.effecoria.tower_anchor"));
    public static final DeferredItem<BlockItem> FOUNDATION_AMULET = ITEMS.register(
            "foundation_amulet", () -> new HintBlockItem(ModBlocks.FOUNDATION_AMULET.get(), new Item.Properties(), "tooltip.effecoria.foundation_amulet"));
    public static final DeferredItem<BlockItem> OMEGA_DAMPER = ITEMS.register(
            "omega_damper", () -> new HintBlockItem(ModBlocks.OMEGA_DAMPER.get(), new Item.Properties(), "tooltip.effecoria.omega_damper"));
    public static final DeferredItem<BlockItem> PHI_AIR_SYNTH = ITEMS.register(
            "phi_air_synth", () -> new HintBlockItem(ModBlocks.PHI_AIR_SYNTH.get(), new Item.Properties(), "tooltip.effecoria.phi_air_synth"));
    public static final DeferredItem<BlockItem> PHI_WATER_PURIFIER = ITEMS.register(
            "phi_water_purifier", () -> new HintBlockItem(ModBlocks.PHI_WATER_PURIFIER.get(), new Item.Properties(), "tooltip.effecoria.phi_water_purifier"));
    public static final DeferredItem<BlockItem> REGEN_CHAMBER = ITEMS.register(
            "regen_chamber", () -> new HintBlockItem(ModBlocks.REGEN_CHAMBER.get(), new Item.Properties(), "tooltip.effecoria.regen_chamber"));
    public static final DeferredItem<BlockItem> TOWER_CONSOLE = ITEMS.register(
            "tower_console", () -> new HintBlockItem(ModBlocks.TOWER_CONSOLE.get(), new Item.Properties(), "tooltip.effecoria.tower_console"));
    public static final DeferredItem<BlockItem> PHI_SONAR = ITEMS.register(
            "phi_sonar", () -> new HintBlockItem(ModBlocks.PHI_SONAR.get(), new Item.Properties(), "tooltip.effecoria.phi_sonar"));
    public static final DeferredItem<BlockItem> PHI_CARTOGRAPHY_TABLE = ITEMS.register(
            "phi_cartography_table",
            () -> new HintBlockItem(
                    ModBlocks.PHI_CARTOGRAPHY_TABLE.get(),
                    new Item.Properties(),
                    "tooltip.effecoria.phi_cartography_table"));

    public static final DeferredItem<Item> ESSENCE_GLUE = ITEMS.register(
            "essence_glue",
            () -> new EssenceGlueItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<BlockItem> REACTOR_CASING =
            ITEMS.registerSimpleBlockItem("reactor_casing", ModBlocks.REACTOR_CASING);
    public static final DeferredItem<BlockItem> PURIFIED_OBSIDIAN =
            ITEMS.registerSimpleBlockItem("purified_obsidian", ModBlocks.PURIFIED_OBSIDIAN);
    public static final DeferredItem<BlockItem> PHI_CONCRETE = ITEMS.register(
            "phi_concrete",
            () -> new HintBlockItem(
                    ModBlocks.PHI_CONCRETE.get(), new Item.Properties(), "tooltip.effecoria.phi_concrete"));
    public static final DeferredItem<BlockItem> OMEGA_ANCHOR = ITEMS.register(
            "omega_anchor",
            () -> new HintBlockItem(
                    ModBlocks.OMEGA_ANCHOR.get(), new Item.Properties(), "tooltip.effecoria.omega_anchor"));
    public static final DeferredItem<BlockItem> OMEGA_TAINTED_OBSIDIAN = ITEMS.register(
            "omega_tainted_obsidian",
            () -> new HintBlockItem(
                    ModBlocks.OMEGA_TAINTED_OBSIDIAN.get(),
                    new Item.Properties(),
                    "tooltip.effecoria.omega_tainted_obsidian"));
    public static final DeferredItem<BlockItem> PHI_BUS = ITEMS.register(
            "phi_bus",
            () -> new HintBlockItem(ModBlocks.PHI_BUS.get(), new Item.Properties(), "tooltip.effecoria.phi_bus"));
    public static final DeferredItem<BlockItem> PHI_CONTACTOR = ITEMS.register(
            "phi_contactor",
            () -> new HintBlockItem(
                    ModBlocks.PHI_CONTACTOR.get(), new Item.Properties(), "tooltip.effecoria.phi_contactor"));
    public static final DeferredItem<BlockItem> PHI_COUPLER = ITEMS.register(
            "phi_coupler",
            () -> new HintBlockItem(ModBlocks.PHI_COUPLER.get(), new Item.Properties(), "tooltip.effecoria.phi_coupler"));
    public static final DeferredItem<BlockItem> PHI_MATCHER = ITEMS.register(
            "phi_matcher",
            () -> new HintBlockItem(ModBlocks.PHI_MATCHER.get(), new Item.Properties(), "tooltip.effecoria.phi_matcher"));
    public static final DeferredItem<BlockItem> PHI_ACCUMULATOR = ITEMS.register(
            "phi_accumulator",
            () -> new HintBlockItem(
                    ModBlocks.PHI_ACCUMULATOR.get(), new Item.Properties(), "tooltip.effecoria.phi_accumulator"));
    public static final DeferredItem<BlockItem> TURRET_MOUNT = ITEMS.register(
            "turret_mount",
            () -> new HintBlockItem(ModBlocks.TURRET_MOUNT.get(), new Item.Properties(), "tooltip.effecoria.turret_mount"));
    public static final DeferredItem<BlockItem> PLASMA_TURRET = ITEMS.register(
            "plasma_turret",
            () -> new HintBlockItem(
                    ModBlocks.PLASMA_TURRET.get(), new Item.Properties(), "tooltip.effecoria.plasma_turret"));
    public static final DeferredItem<BlockItem> KINETIC_TURRET = ITEMS.register(
            "kinetic_turret",
            () -> new HintBlockItem(
                    ModBlocks.KINETIC_TURRET.get(), new Item.Properties(), "tooltip.effecoria.kinetic_turret"));
    public static final DeferredItem<BlockItem> SPATIAL_TURRET = ITEMS.register(
            "spatial_turret",
            () -> new HintBlockItem(
                    ModBlocks.SPATIAL_TURRET.get(), new Item.Properties(), "tooltip.effecoria.spatial_turret"));
    public static final DeferredItem<BlockItem> MENTAL_TURRET = ITEMS.register(
            "mental_turret",
            () -> new HintBlockItem(
                    ModBlocks.MENTAL_TURRET.get(), new Item.Properties(), "tooltip.effecoria.mental_turret"));
    public static final DeferredItem<BlockItem> OMEGA_TURRET = ITEMS.register(
            "omega_turret",
            () -> new HintBlockItem(ModBlocks.OMEGA_TURRET.get(), new Item.Properties(), "tooltip.effecoria.omega_turret"));
    public static final DeferredItem<BlockItem> PHI_CRUSHER = ITEMS.register(
            "phi_crusher",
            () -> new HintBlockItem(ModBlocks.PHI_CRUSHER.get(), new Item.Properties(), "tooltip.effecoria.phi_crusher"));
    public static final DeferredItem<BlockItem> PHI_CRUSHER_HOPPER = ITEMS.register(
            "phi_crusher_hopper",
            () -> new HintBlockItem(
                    ModBlocks.PHI_CRUSHER_HOPPER.get(),
                    new Item.Properties(),
                    "tooltip.effecoria.phi_crusher_hopper"));
    public static final DeferredItem<Item> MITHRIL_BOLT = ITEMS.registerSimpleItem("mithril_bolt");
    public static final DeferredItem<BlockItem> ESSENCE_ALEMBIC =
            ITEMS.registerSimpleBlockItem("essence_alembic", ModBlocks.ESSENCE_ALEMBIC);

    public static final DeferredItem<Item> PHI_PAPER = ITEMS.registerSimpleItem("phi_paper");
    public static final DeferredItem<Item> PHI_FLASK = ITEMS.registerSimpleItem("phi_flask");
    public static final DeferredItem<Item> ESSENCE_DEW = ITEMS.registerSimpleItem("essence_dew");
    public static final DeferredItem<Item> PHI_FLASK_WATER = ITEMS.register(
            "phi_flask_water", () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> BLOOD_VIAL_EMPTY = ITEMS.register(
            "blood_vial_empty",
            () -> new BloodVialEmptyItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> BLOOD_VIAL = ITEMS.register(
            "blood_vial", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> MAGE_BLOOD_VIAL = ITEMS.register(
            "mage_blood_vial", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> WYVERN_BLOOD_VIAL = ITEMS.register(
            "wyvern_blood_vial", () -> new Item(new Item.Properties().stacksTo(16)));
    /** Forbidden Ω-reagent — scooped from Eldritch Blood puddles in the Scar. */
    public static final DeferredItem<Item> OMEGA_BLOOD_VIAL = ITEMS.register(
            "omega_blood_vial", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> LONVER_BLOOD_VIAL = ITEMS.register(
            "lonver_blood_vial", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> PHI_NECTAR = ITEMS.registerSimpleItem("phi_nectar");
    public static final DeferredItem<Item> FIREFLOWER = ITEMS.registerSimpleItem("fireflower");
    public static final DeferredItem<Item> PHI_STEEL_INGOT = ITEMS.register(
            "phi_steel_ingot", () -> new HintItem(new Item.Properties(), "item.effecoria.phi_steel_ingot.hint"));
    public static final DeferredItem<Item> OMEGA_DUST = ITEMS.register(
            "omega_dust", () -> new HintItem(new Item.Properties(), "item.effecoria.omega_dust.hint"));
    public static final DeferredItem<Item> PHI_STONE_GRIT = ITEMS.register(
            "phi_stone_grit", () -> new HintItem(new Item.Properties(), "item.effecoria.phi_stone_grit.hint"));
    public static final DeferredItem<Item> BONE_GRIT =
            ITEMS.register("bone_grit", () -> new BoneGritItem(new Item.Properties()));
    public static final DeferredItem<Item> PHI_BONE_PASTE = ITEMS.register(
            "phi_bone_paste", () -> new HintItem(new Item.Properties(), "item.effecoria.phi_bone_paste.hint"));
    public static final DeferredItem<Item> PHI_WOOD_SHAVINGS = ITEMS.register(
            "phi_wood_shavings",
            () -> new FuelItem(new Item.Properties(), 300, "item.effecoria.phi_wood_shavings.hint"));
    public static final DeferredItem<Item> PHI_FIBER = ITEMS.register(
            "phi_fiber", () -> new HintItem(new Item.Properties(), "item.effecoria.phi_fiber.hint"));
    public static final DeferredItem<Item> PHI_CLOTH = ITEMS.register(
            "phi_cloth", () -> new HintItem(new Item.Properties(), "item.effecoria.phi_cloth.hint"));
    public static final DeferredItem<Item> PHI_ROPE = ITEMS.register(
            "phi_rope", () -> new HintItem(new Item.Properties(), "item.effecoria.phi_rope.hint"));
    public static final DeferredItem<Item> OBSIDIAN_GRIT = ITEMS.register(
            "obsidian_grit", () -> new HintItem(new Item.Properties(), "item.effecoria.obsidian_grit.hint"));
    public static final DeferredItem<Item> OMEGA_NUGGET = ITEMS.register(
            "omega_nugget", () -> new HintItem(new Item.Properties(), "item.effecoria.omega_nugget.hint"));
    public static final DeferredItem<Item> SOUL_SHARD = ITEMS.register(
            "soul_shard", () -> new HintItem(new Item.Properties(), "item.effecoria.soul_shard.hint"));
    public static final DeferredItem<Item> LEAD_FOIL = ITEMS.register(
            "lead_foil", () -> new HintItem(new Item.Properties(), "item.effecoria.lead_foil.hint"));
    public static final DeferredItem<Item> OMEGA_WASTE =
            ITEMS.register("omega_waste", () -> new OmegaWasteItem(new Item.Properties()));
    public static final DeferredItem<Item> OMEGA_FILTER =
            ITEMS.register("omega_filter", () -> new OmegaFilterItem(new Item.Properties()));
    public static final DeferredItem<Item> DEEP_PHI_CATALYST = ITEMS.register(
            "deep_phi_catalyst",
            () -> new HintItem(new Item.Properties(), "item.effecoria.deep_phi_catalyst.hint"));

    public static final DeferredItem<Item> GOLD_FILTER = ITEMS.register(
            "gold_filter", () -> new GoldFilterItem(new Item.Properties()));
    public static final DeferredItem<Item> LEAD_FILTER = ITEMS.register(
            "lead_filter", () -> new LeadFilterItem(new Item.Properties()));

    /** Heavy Φ-insulating cloak (lead mesh stand-in). */
    public static final DeferredItem<Item> LEAD_CLOAK = ITEMS.register(
            "lead_cloak", () -> new Item(new Item.Properties().stacksTo(1)));
    /** Light gold Φ-reflector — Curios amulet. */
    public static final DeferredItem<Item> GOLD_AMULET = ITEMS.register(
            "gold_amulet",
            () -> new JewelryItem(
                    new Item.Properties(), "item.effecoria.gold_amulet.hint", 0.55f));
    /** Star essonite amulet — stronger Φ reflection. */
    public static final DeferredItem<Item> STAR_AMULET = ITEMS.register(
            "star_amulet",
            () -> new JewelryItem(
                    new Item.Properties(), "item.effecoria.star_amulet.hint", 0.7f));
    /** Essonite ring — modest Φ damp. */
    public static final DeferredItem<Item> ESSONITE_RING = ITEMS.register(
            "essonite_ring",
            () -> new JewelryItem(
                    new Item.Properties(), "item.effecoria.essonite_ring.hint", 0.35f));
    /** Φ-band ring — light Ψ subsidy marker (shield). */
    public static final DeferredItem<Item> PHI_BAND = ITEMS.register(
            "phi_band",
            () -> new JewelryItem(
                    new Item.Properties(), "item.effecoria.phi_band.hint", 0.4f));
    /** Lead charm — heavy damp in charm slot. */
    public static final DeferredItem<Item> LEAD_CHARM = ITEMS.register(
            "lead_charm",
            () -> new JewelryItem(
                    new Item.Properties(), "item.effecoria.lead_charm.hint", 0.6f));
    /** Curios head — overlays tower Φ-sonar ore/anomaly feed in the world. */
    public static final DeferredItem<Item> PHI_SONAR_GOGGLES = ITEMS.register(
            "phi_sonar_goggles",
            () -> new JewelryItem(
                    new Item.Properties(), "item.effecoria.phi_sonar_goggles.hint", 0.15f));
    /** Hand-held Φ shade — gold foil on a frame. */
    public static final DeferredItem<Item> ESSENCE_PARASOL = ITEMS.register(
            "essence_parasol", () -> new Item(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> POTION_PHI_RESISTANCE = ITEMS.register(
            "potion_phi_resistance",
            () -> new PhiShieldConsumableItem(
                    new Item.Properties(),
                    ModMobEffects::phiResistance,
                    90 * 20,
                    "item.effecoria.potion_phi_resistance.hint",
                    false));
    public static final DeferredItem<Item> ESSENCE_CLAY_SALVE = ITEMS.register(
            "essence_clay_salve",
            () -> new PhiShieldConsumableItem(
                    new Item.Properties(),
                    ModMobEffects::claySalve,
                    45 * 20,
                    "item.effecoria.essence_clay_salve.hint",
                    false));
    public static final DeferredItem<Item> LEAD_PILL = ITEMS.register(
            "lead_pill",
            () -> new PhiShieldConsumableItem(
                    new Item.Properties(),
                    ModMobEffects::leadSaturation,
                    60 * 20,
                    "item.effecoria.lead_pill.hint",
                    true));

    /** Ω-cleansing draught — Omega Sickness / early Omega Rot. */
    public static final DeferredItem<Item> POTION_OMEGA_CLEANSE = ITEMS.register(
            "potion_omega_cleanse",
            () -> DiseaseCureItem.of(
                    new Item.Properties(),
                    "item.effecoria.potion_omega_cleanse.hint",
                    PhiDisease.OMEGA_SICKNESS,
                    PhiDisease.OMEGA_ROT));

    /** Anti-Φ serum — Crystal Fever. */
    public static final DeferredItem<Item> ANTI_PHI_SERUM = ITEMS.register(
            "anti_phi_serum",
            () -> DiseaseCureItem.of(
                    new Item.Properties(),
                    "item.effecoria.anti_phi_serum.hint",
                    PhiDisease.CRYSTAL_FEVER));

    /** Lung rinse — Dust Lung. */
    public static final DeferredItem<Item> LUNG_RINSE = ITEMS.register(
            "lung_rinse",
            () -> DiseaseCureItem.of(
                    new Item.Properties(),
                    "item.effecoria.lung_rinse.hint",
                    PhiDisease.DUST_LUNG));

    /** Surgical kit — Essentocytosis (costly). */
    public static final DeferredItem<Item> ESSENTOCYTE_KIT = ITEMS.register(
            "essentocyte_kit",
            () -> new DiseaseCureItem(
                    new Item.Properties(),
                    java.util.EnumSet.of(PhiDisease.ESSENTOCYTOSIS),
                    "item.effecoria.essentocyte_kit.hint",
                    player -> player.hurt(player.damageSources().magic(), 6f),
                    net.minecraft.world.item.UseAnim.BOW,
                    48));

    /** Ψ-therapy resonator — Soul Dissonance / Ghost Echo. */
    public static final DeferredItem<Item> PSI_RESONATOR_THERAPY = ITEMS.register(
            "psi_resonator_therapy",
            () -> DiseaseCureItem.of(
                    new Item.Properties(),
                    "item.effecoria.psi_resonator_therapy.hint",
                    PhiDisease.SOUL_DISSONANCE,
                    PhiDisease.GHOST_ECHO));

    /** Orkanum stimulant — early Atrophy (stage ≤2 auto via DiseaseService helper). */
    public static final DeferredItem<Item> ORKANUMN_STIMULANT = ITEMS.register(
            "orkanumn_stimulant",
            () -> new DiseaseCureItem(
                    new Item.Properties(),
                    java.util.EnumSet.of(PhiDisease.ORKANUMN_ATROPHY),
                    "item.effecoria.orkanumn_stimulant.hint",
                    null,
                    net.minecraft.world.item.UseAnim.DRINK,
                    28));

    /** Gold salve / amputation draught — Omega Rot. */
    public static final DeferredItem<Item> OMEGA_AMPUTATION_SALVE = ITEMS.register(
            "omega_amputation_salve",
            () -> new DiseaseCureItem(
                    new Item.Properties(),
                    java.util.EnumSet.of(PhiDisease.OMEGA_ROT),
                    "item.effecoria.omega_amputation_salve.hint",
                    player -> player.hurt(player.damageSources().magic(), 8f),
                    net.minecraft.world.item.UseAnim.BOW,
                    40));

    public static final DeferredItem<Item> POTION_PHI_TONIC = ITEMS.register(
            "potion_phi_tonic",
            () -> new AlchemyPotionItem(
                    new Item.Properties(), ModMobEffects::tonic, 90 * 20, "item.effecoria.potion_phi_tonic.hint"));
    public static final DeferredItem<Item> POTION_PHI_RESONANCE = ITEMS.register(
            "potion_phi_resonance",
            () -> new AlchemyPotionItem(
                    new Item.Properties(),
                    ModMobEffects::resonance,
                    75 * 20,
                    "item.effecoria.potion_phi_resonance.hint"));
    public static final DeferredItem<Item> POTION_PHI_STIMULANT = ITEMS.register(
            "potion_phi_stimulant",
            () -> new AlchemyPotionItem(
                    new Item.Properties(),
                    ModMobEffects::stimulant,
                    45 * 20,
                    "item.effecoria.potion_phi_stimulant.hint"));

    /** Hyperspace Φ-membrane (creative / debug; worldgen places it in subspace). */
    public static final DeferredItem<BlockItem> PHI_VEIL =
            ITEMS.registerSimpleBlockItem("phi_veil", ModBlocks.PHI_VEIL);

    /** Refined Φ-conductive dust — smelted from essonite ore. */
    public static final DeferredItem<Item> ESSENITE_DUST = ITEMS.register(
            "essonite_dust", () -> new EssoniteDustItem(new Item.Properties()));

    /** Initiation focus — school select + tiered resonance bonuses. */
    public static final DeferredItem<Item> RESONANCE_FOCUS = ITEMS.register(
            "resonance_focus",
            () -> new ResonanceFocusItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PSI_FOCUS = ITEMS.register(
            "psi_focus", () -> new PsiFocusItem(new Item.Properties()));

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

    public static final DeferredItem<Item> VITRIFIED_GLASS_SHARD = ITEMS.registerSimpleItem("vitrified_glass_shard");
    public static final DeferredItem<Item> VITRIFIED_GOLEM_CORE = ITEMS.register(
            "vitrified_golem_core",
            () -> new VitrifiedGolemCoreItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<ArmorItem> PHI_CHITIN_HELMET = ITEMS.register(
            "phi_chitin_helmet",
            () -> new EssoniteArmorItem(
                    ModMaterials.PHI_CHITIN,
                    ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(15)),
                    EssoniteArmorTier.BASIC));

    public static final DeferredItem<ArmorItem> PHI_CHITIN_CHESTPLATE = ITEMS.register(
            "phi_chitin_chestplate",
            () -> new EssoniteArmorItem(
                    ModMaterials.PHI_CHITIN,
                    ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(15)),
                    EssoniteArmorTier.BASIC));

    public static final DeferredItem<ArmorItem> PHI_CHITIN_LEGGINGS = ITEMS.register(
            "phi_chitin_leggings",
            () -> new EssoniteArmorItem(
                    ModMaterials.PHI_CHITIN,
                    ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(15)),
                    EssoniteArmorTier.BASIC));

    public static final DeferredItem<ArmorItem> PHI_CHITIN_BOOTS = ITEMS.register(
            "phi_chitin_boots",
            () -> new EssoniteArmorItem(
                    ModMaterials.PHI_CHITIN,
                    ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(15)),
                    EssoniteArmorTier.BASIC));

    public static final DeferredItem<ArmorItem> CRYSTAL_ESSONITE_HELMET = ITEMS.register(
            "crystal_essonite_helmet",
            () -> new EssoniteArmorItem(
                    ModMaterials.CRYSTAL_ESSONITE,
                    ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(25)),
                    EssoniteArmorTier.CRYSTAL));

    public static final DeferredItem<ArmorItem> CRYSTAL_ESSONITE_CHESTPLATE = ITEMS.register(
            "crystal_essonite_chestplate",
            () -> new EssoniteArmorItem(
                    ModMaterials.CRYSTAL_ESSONITE,
                    ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(25)),
                    EssoniteArmorTier.CRYSTAL));

    public static final DeferredItem<ArmorItem> CRYSTAL_ESSONITE_LEGGINGS = ITEMS.register(
            "crystal_essonite_leggings",
            () -> new EssoniteArmorItem(
                    ModMaterials.CRYSTAL_ESSONITE,
                    ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(25)),
                    EssoniteArmorTier.CRYSTAL));

    public static final DeferredItem<ArmorItem> CRYSTAL_ESSONITE_BOOTS = ITEMS.register(
            "crystal_essonite_boots",
            () -> new EssoniteArmorItem(
                    ModMaterials.CRYSTAL_ESSONITE,
                    ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(25)),
                    EssoniteArmorTier.CRYSTAL));

    public static final DeferredItem<ArmorItem> STAR_ESSONITE_HELMET = ITEMS.register(
            "star_essonite_helmet",
            () -> new EssoniteArmorItem(
                    ModMaterials.STAR_ESSONITE,
                    ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(37)),
                    EssoniteArmorTier.STAR));

    public static final DeferredItem<ArmorItem> STAR_ESSONITE_CHESTPLATE = ITEMS.register(
            "star_essonite_chestplate",
            () -> new EssoniteArmorItem(
                    ModMaterials.STAR_ESSONITE,
                    ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(37)),
                    EssoniteArmorTier.STAR));

    public static final DeferredItem<ArmorItem> STAR_ESSONITE_LEGGINGS = ITEMS.register(
            "star_essonite_leggings",
            () -> new EssoniteArmorItem(
                    ModMaterials.STAR_ESSONITE,
                    ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(37)),
                    EssoniteArmorTier.STAR));

    public static final DeferredItem<ArmorItem> STAR_ESSONITE_BOOTS = ITEMS.register(
            "star_essonite_boots",
            () -> new EssoniteArmorItem(
                    ModMaterials.STAR_ESSONITE,
                    ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(37)),
                    EssoniteArmorTier.STAR));

    public static final DeferredItem<ArmorItem> MITHRIL_HELMET = ITEMS.register(
            "mithril_helmet",
            () -> new EssoniteArmorItem(
                    ModMaterials.MITHRIL,
                    ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(33)),
                    EssoniteArmorTier.MITHRIL));

    public static final DeferredItem<ArmorItem> MITHRIL_CHESTPLATE = ITEMS.register(
            "mithril_chestplate",
            () -> new EssoniteArmorItem(
                    ModMaterials.MITHRIL,
                    ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(33)),
                    EssoniteArmorTier.MITHRIL));

    public static final DeferredItem<ArmorItem> MITHRIL_LEGGINGS = ITEMS.register(
            "mithril_leggings",
            () -> new EssoniteArmorItem(
                    ModMaterials.MITHRIL,
                    ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(33)),
                    EssoniteArmorTier.MITHRIL));

    public static final DeferredItem<ArmorItem> MITHRIL_BOOTS = ITEMS.register(
            "mithril_boots",
            () -> new EssoniteArmorItem(
                    ModMaterials.MITHRIL,
                    ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(33)),
                    EssoniteArmorTier.MITHRIL));

    public static final DeferredItem<Item> VOID_OBSIDIAN_INSERT = ITEMS.registerSimpleItem("void_obsidian_insert");
    public static final DeferredItem<Item> PSI_KEY = ITEMS.registerSimpleItem("psi_key");

    public static final DeferredItem<Item> PHI_PHONEME_FIRMITAS = ITEMS.register(
            "phi_phoneme_firmitas",
            () -> new PhiPhonemeScrollItem(new Item.Properties().stacksTo(16), EssonitePhoneme.FIRMITAS));
    public static final DeferredItem<Item> PHI_PHONEME_UMBRA = ITEMS.register(
            "phi_phoneme_umbra",
            () -> new PhiPhonemeScrollItem(new Item.Properties().stacksTo(16), EssonitePhoneme.UMBRA));
    public static final DeferredItem<Item> PHI_PHONEME_ABNEGATIO = ITEMS.register(
            "phi_phoneme_abnegatio",
            () -> new PhiPhonemeScrollItem(new Item.Properties().stacksTo(16), EssonitePhoneme.ABNEGATIO));
    public static final DeferredItem<Item> PHI_PHONEME_SERVARE = ITEMS.register(
            "phi_phoneme_servare",
            () -> new PhiPhonemeScrollItem(new Item.Properties().stacksTo(16), EssonitePhoneme.SERVARE));
    public static final DeferredItem<Item> PHI_PHONEME_CLAUSURA = ITEMS.register(
            "phi_phoneme_clausura",
            () -> new PhiPhonemeScrollItem(new Item.Properties().stacksTo(16), EssonitePhoneme.CLAUSURA));

    public static final DeferredItem<SwordItem> VITRIFIED_GLASS_SWORD = ITEMS.register(
            "vitrified_glass_sword",
            () -> new SwordItem(
                    ModMaterials.VITRIFIED_GLASS,
                    new Item.Properties()
                            .attributes(SwordItem.createAttributes(ModMaterials.VITRIFIED_GLASS, 3, -2.4f))));

    public static final DeferredItem<PickaxeItem> VITRIFIED_GLASS_PICKAXE = ITEMS.register(
            "vitrified_glass_pickaxe",
            () -> new PickaxeItem(
                    ModMaterials.VITRIFIED_GLASS,
                    new Item.Properties()
                            .attributes(PickaxeItem.createAttributes(ModMaterials.VITRIFIED_GLASS, 1.0f, -2.8f))));

    public static final DeferredItem<AxeItem> VITRIFIED_GLASS_AXE = ITEMS.register(
            "vitrified_glass_axe",
            () -> new AxeItem(
                    ModMaterials.VITRIFIED_GLASS,
                    new Item.Properties()
                            .attributes(AxeItem.createAttributes(ModMaterials.VITRIFIED_GLASS, 6.0f, -3.1f))));

    public static final DeferredItem<ShovelItem> VITRIFIED_GLASS_SHOVEL = ITEMS.register(
            "vitrified_glass_shovel",
            () -> new ShovelItem(
                    ModMaterials.VITRIFIED_GLASS,
                    new Item.Properties()
                            .attributes(ShovelItem.createAttributes(ModMaterials.VITRIFIED_GLASS, 1.5f, -3.0f))));

    public static final DeferredItem<SwordItem> MITHRIL_SWORD = ITEMS.register(
            "mithril_sword",
            () -> new SwordItem(
                    ModMaterials.MITHRIL_TOOLS,
                    new Item.Properties()
                            .attributes(SwordItem.createAttributes(ModMaterials.MITHRIL_TOOLS, 3, -2.4f))));

    public static final DeferredItem<PickaxeItem> MITHRIL_PICKAXE = ITEMS.register(
            "mithril_pickaxe",
            () -> new PickaxeItem(
                    ModMaterials.MITHRIL_TOOLS,
                    new Item.Properties()
                            .attributes(PickaxeItem.createAttributes(ModMaterials.MITHRIL_TOOLS, 1.0f, -2.8f))));

    public static final DeferredItem<AxeItem> MITHRIL_AXE = ITEMS.register(
            "mithril_axe",
            () -> new AxeItem(
                    ModMaterials.MITHRIL_TOOLS,
                    new Item.Properties()
                            .attributes(AxeItem.createAttributes(ModMaterials.MITHRIL_TOOLS, 5.0f, -3.0f))));

    public static final DeferredItem<ShovelItem> MITHRIL_SHOVEL = ITEMS.register(
            "mithril_shovel",
            () -> new ShovelItem(
                    ModMaterials.MITHRIL_TOOLS,
                    new Item.Properties()
                            .attributes(ShovelItem.createAttributes(ModMaterials.MITHRIL_TOOLS, 1.5f, -3.0f))));

    public static final DeferredItem<HoeItem> MITHRIL_HOE = ITEMS.register(
            "mithril_hoe",
            () -> new HoeItem(
                    ModMaterials.MITHRIL_TOOLS,
                    new Item.Properties()
                            .attributes(HoeItem.createAttributes(ModMaterials.MITHRIL_TOOLS, -3.0f, 0.0f))));

    public static final DeferredItem<ArmorItem> PHI_STEEL_HELMET = ITEMS.register(
            "phi_steel_helmet",
            () -> new ArmorItem(
                    ModMaterials.PHI_STEEL,
                    ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(18))));
    public static final DeferredItem<ArmorItem> PHI_STEEL_CHESTPLATE = ITEMS.register(
            "phi_steel_chestplate",
            () -> new ArmorItem(
                    ModMaterials.PHI_STEEL,
                    ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(18))));
    public static final DeferredItem<ArmorItem> PHI_STEEL_LEGGINGS = ITEMS.register(
            "phi_steel_leggings",
            () -> new ArmorItem(
                    ModMaterials.PHI_STEEL,
                    ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(18))));
    public static final DeferredItem<ArmorItem> PHI_STEEL_BOOTS = ITEMS.register(
            "phi_steel_boots",
            () -> new ArmorItem(
                    ModMaterials.PHI_STEEL,
                    ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(18))));

    public static final DeferredItem<SwordItem> PHI_STEEL_SWORD = ITEMS.register(
            "phi_steel_sword",
            () -> new SwordItem(
                    ModMaterials.PHI_STEEL_TOOLS,
                    new Item.Properties()
                            .attributes(SwordItem.createAttributes(ModMaterials.PHI_STEEL_TOOLS, 3, -2.4f))));
    public static final DeferredItem<PickaxeItem> PHI_STEEL_PICKAXE = ITEMS.register(
            "phi_steel_pickaxe",
            () -> new PickaxeItem(
                    ModMaterials.PHI_STEEL_TOOLS,
                    new Item.Properties()
                            .attributes(PickaxeItem.createAttributes(ModMaterials.PHI_STEEL_TOOLS, 1.0f, -2.8f))));
    public static final DeferredItem<AxeItem> PHI_STEEL_AXE = ITEMS.register(
            "phi_steel_axe",
            () -> new AxeItem(
                    ModMaterials.PHI_STEEL_TOOLS,
                    new Item.Properties()
                            .attributes(AxeItem.createAttributes(ModMaterials.PHI_STEEL_TOOLS, 5.5f, -3.0f))));
    public static final DeferredItem<ShovelItem> PHI_STEEL_SHOVEL = ITEMS.register(
            "phi_steel_shovel",
            () -> new ShovelItem(
                    ModMaterials.PHI_STEEL_TOOLS,
                    new Item.Properties()
                            .attributes(ShovelItem.createAttributes(ModMaterials.PHI_STEEL_TOOLS, 1.5f, -3.0f))));
    public static final DeferredItem<HoeItem> PHI_STEEL_HOE = ITEMS.register(
            "phi_steel_hoe",
            () -> new HoeItem(
                    ModMaterials.PHI_STEEL_TOOLS,
                    new Item.Properties()
                            .attributes(HoeItem.createAttributes(ModMaterials.PHI_STEEL_TOOLS, -2.0f, -1.0f))));

    public static final DeferredItem<SpawnEggItem> VITRIFIED_GOLEM_SPAWN_EGG = ITEMS.register(
            "vitrified_golem_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.VITRIFIED_GOLEM, 0x1a1a2e, 0x00d2ff, new Item.Properties()));

    public static final DeferredItem<SpawnEggItem> ESSENCE_WYVERN_SPAWN_EGG = ITEMS.register(
            "essence_wyvern_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ESSENCE_WYVERN, 0x5a5a58, 0xd4a017, new Item.Properties()));

    public static final DeferredItem<SpawnEggItem> ROTFANG_MINK_SPAWN_EGG = ITEMS.register(
            "rotfang_mink_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ROTFANG_MINK, 0x1a1220, 0x8a2be2, new Item.Properties()));

    public static final DeferredItem<SpawnEggItem> OMEGA_SHADE_SPAWN_EGG = ITEMS.register(
            "omega_shade_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.OMEGA_SHADE, 0x0a0610, 0x6b1f9a, new Item.Properties()));

    public static final DeferredItem<SpawnEggItem> OMEGA_WORM_SPAWN_EGG = ITEMS.register(
            "omega_worm_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.OMEGA_WORM, 0x18101c, 0x5c2878, new Item.Properties()));

    public static final DeferredItem<SpawnEggItem> PHI_ENT_SPAWN_EGG = ITEMS.register(
            "phi_ent_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.PHI_ENT, 0x2d1b69, 0xd4af37, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> PHI_LEMUR_SPAWN_EGG = ITEMS.register(
            "phi_lemur_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.PHI_LEMUR, 0x3a2876, 0xf0c850, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> WAILER_BAT_SPAWN_EGG = ITEMS.register(
            "wailer_bat_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.WAILER_BAT, 0x1a1438, 0x6b5cae, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> GLASS_WORM_SPAWN_EGG = ITEMS.register(
            "glass_worm_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GLASS_WORM, 0x4a4080, 0xc9e8ff, new Item.Properties()));
}
