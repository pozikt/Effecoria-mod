package com.effecoria.content;

import com.effecoria.EffecoriaMod;
import com.effecoria.block.AncientEssenceWoodBlock;
import com.effecoria.block.ArtifactStationBlock;
import com.effecoria.block.ClayCrucibleBlock;
import com.effecoria.block.MithrilBlock;
import com.effecoria.block.EldritchBloodPuddleBlock;
import com.effecoria.block.EssoniteCrustBlock;
import com.effecoria.block.EssonitePointedBlock;
import com.effecoria.block.EssenceAlembicBlock;
import com.effecoria.block.EssenceBurnerBlock;
import com.effecoria.block.MortarAndPestleBlock;
import com.effecoria.block.OmegaBladesBlock;
import com.effecoria.block.OmegaAnchorBlock;
import com.effecoria.block.GeoWellBlock;
import com.effecoria.block.GeoWellPartBlock;
import com.effecoria.block.ClimateArrayBlock;
import com.effecoria.block.PortalGateBlock;
import com.effecoria.block.PortalModulatorBlock;
import com.effecoria.block.PhiBeaconBlock;
import com.effecoria.block.PhiBladesBlock;
import com.effecoria.block.PhiCampfireBlock;
import com.effecoria.block.PhiFieldBlock;
import com.effecoria.block.PhiFurnaceBlock;
import com.effecoria.block.PhiGeyserBlock;
import com.effecoria.block.PhiGrassBlock;
import com.effecoria.block.PhiLeavesBlock;
import com.effecoria.block.PhiLogBlock;
import com.effecoria.block.PhiSaplingBlock;
import com.effecoria.block.PhiSnareVineBlock;
import com.effecoria.block.PhiTorchBlock;
import com.effecoria.block.PhiTelegraphBlock;
import com.effecoria.block.PsiImprinterBlock;
import com.effecoria.block.RottenMossBlock;
import com.effecoria.block.SparkReactorBlock;
import com.effecoria.block.HeartReactorBlock;
import com.effecoria.block.HeartReactorPartBlock;
import com.effecoria.block.ForgeReactorBlock;
import com.effecoria.block.ForgeReactorPartBlock;
import com.effecoria.block.PhiBusBlock;
import com.effecoria.block.PhiCrusherBlock;
import com.effecoria.block.PhiCrusherHopperBlock;
import com.effecoria.block.PhiTurretBlock;
import com.effecoria.block.TurretMountBlock;
import com.effecoria.block.SubspacePortalBlock;
import com.effecoria.core.alchemy.TurretKind;
import com.effecoria.block.VitrifiedBranchesBlock;
import com.effecoria.block.VitrifiedGeyserCrackBlock;
import com.effecoria.block.VitrifiedLogBlock;
import com.effecoria.block.VitrifiedSandBlock;
import com.effecoria.block.WhisperingSpireVentBlock;
import com.effecoria.world.ModTreeGrowers;

import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
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

    /** Dense Φ-damping metal — casings for technomagic machines. */
    public static final DeferredBlock<Block> LEAD_ORE = registerOre("lead_ore", MapColor.STONE, 3f, 3f, SoundType.STONE);

    public static final DeferredBlock<Block> DEEPSLATE_LEAD_ORE =
            registerOre("deepslate_lead_ore", MapColor.DEEPSLATE, 4.5f, 3f, SoundType.DEEPSLATE);

    public static final DeferredBlock<Block> LEAD_BLOCK = BLOCKS.register(
            "lead_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .requiresCorrectToolForDrops()
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.METAL)));

    /** Φ-superconductor — rare veins in the Essence Plateau. */
    public static final DeferredBlock<Block> MITHRIL_ORE =
            registerOre("mithril_ore", MapColor.STONE, 3.5f, 3f, SoundType.STONE);

    public static final DeferredBlock<Block> DEEPSLATE_MITHRIL_ORE =
            registerOre("deepslate_mithril_ore", MapColor.DEEPSLATE, 5.0f, 3f, SoundType.DEEPSLATE);

    public static final DeferredBlock<MithrilBlock> MITHRIL_BLOCK = BLOCKS.register(
            "mithril_block",
            () -> new MithrilBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(MithrilBlock.POWERED) ? 4 : 0)));

    /** Nearly pure essonite — Φ-core mass under the plateau (Y ≤ 0). */
    public static final DeferredBlock<Block> ESSONITE_BLOCK = BLOCKS.register(
            "essonite_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .requiresCorrectToolForDrops()
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 12)));

    /** Ultra-pure condensed essonite from Whispering Spire calderas (99.9% fiction). */
    public static final DeferredBlock<Block> STAR_ESSONITE_BLOCK = BLOCKS.register(
            "star_essonite_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .requiresCorrectToolForDrops()
                    .strength(6.0f, 8.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 15)));

    /** Caldera vent — Φ-plasma column of a Whispering Spire. */
    public static final DeferredBlock<WhisperingSpireVentBlock> WHISPERING_SPIRE_VENT = BLOCKS.register(
            "whispering_spire_vent",
            () -> new WhisperingSpireVentBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(50f, 1200f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 15)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

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

    /** Vitrified Wastes — fused Φ-flash dirt (black glass soil). */
    public static final DeferredBlock<Block> VITRIFIED_DIRT = BLOCKS.register(
            "vitrified_dirt",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .requiresCorrectToolForDrops()
                    .strength(1.4f, 4.0f)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 2)));

    /** Obsidian-like fused stone with gold Φ-veins. */
    public static final DeferredBlock<Block> VITRIFIED_STONE = BLOCKS.register(
            "vitrified_stone",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .requiresCorrectToolForDrops()
                    .strength(50.0f, 1200.0f)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 4)));

    /** Slow-falling black glass grit / Φ-barghan. */
    public static final DeferredBlock<VitrifiedSandBlock> VITRIFIED_SAND = BLOCKS.register(
            "vitrified_sand",
            () -> new VitrifiedSandBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(0.4f)
                    .sound(SoundType.SAND)
                    .lightLevel(state -> 1)));

    /** Petrified glassy trunk. */
    public static final DeferredBlock<VitrifiedLogBlock> VITRIFIED_LOG = BLOCKS.register(
            "vitrified_log",
            () -> new VitrifiedLogBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .requiresCorrectToolForDrops()
                    .strength(2.5f, 6.0f)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 3)));

    /** Sharp leafless branches. */
    public static final DeferredBlock<VitrifiedBranchesBlock> VITRIFIED_BRANCHES = BLOCKS.register(
            "vitrified_branches",
            () -> new VitrifiedBranchesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(0.8f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .lightLevel(state -> 5)));

    /** Small residual Φ-crack / mini-geyser. */
    public static final DeferredBlock<VitrifiedGeyserCrackBlock> VITRIFIED_GEYSER_CRACK = BLOCKS.register(
            "vitrified_geyser_crack",
            () -> new VitrifiedGeyserCrackBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 10)
                    .randomTicks()));

    /** Φ-saturated stone — glows and slowly converts adjacent stone. */
    public static final DeferredBlock<PhiFieldBlock> PHI_STONE = BLOCKS.register(
            "phi_stone",
            () -> new PhiFieldBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .requiresCorrectToolForDrops()
                    .strength(1.5f, 6f)
                    .sound(ModSoundTypes.PHI_STONE)
                    .lightLevel(state -> 7)));

    /** Coarse crush of Φ-stone. */
    public static final DeferredBlock<Block> PHI_COBBLE = BLOCKS.register(
            "phi_cobble",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .requiresCorrectToolForDrops()
                    .strength(1.6f, 6f)
                    .sound(ModSoundTypes.PHI_STONE)
                    .lightLevel(state -> 4)));

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

    /** Ω-crystal cluster — concentrated b-component lattice in Scar cracks. */
    public static final DeferredBlock<AmethystClusterBlock> OMEGA_CRYSTAL = BLOCKS.register(
            "omega_crystal",
            () -> new AmethystClusterBlock(
                    7,
                    3,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .forceSolidOn()
                            .noOcclusion()
                            .randomTicks()
                            .strength(1.6f)
                            .sound(SoundType.AMETHYST_CLUSTER)
                            .lightLevel(state -> 8)
                            .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<AmethystClusterBlock> OMEGA_CRYSTAL_BUD_SMALL = BLOCKS.register(
            "omega_crystal_bud_small",
            () -> new AmethystClusterBlock(
                    3,
                    4,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .forceSolidOn()
                            .noOcclusion()
                            .randomTicks()
                            .strength(1.4f)
                            .sound(SoundType.SMALL_AMETHYST_BUD)
                            .lightLevel(state -> 3)
                            .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<AmethystClusterBlock> OMEGA_CRYSTAL_BUD_MEDIUM = BLOCKS.register(
            "omega_crystal_bud_medium",
            () -> new AmethystClusterBlock(
                    4,
                    3,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .forceSolidOn()
                            .noOcclusion()
                            .randomTicks()
                            .strength(1.5f)
                            .sound(SoundType.MEDIUM_AMETHYST_BUD)
                            .lightLevel(state -> 5)
                            .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<AmethystClusterBlock> OMEGA_CRYSTAL_BUD_LARGE = BLOCKS.register(
            "omega_crystal_bud_large",
            () -> new AmethystClusterBlock(
                    5,
                    3,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .forceSolidOn()
                            .noOcclusion()
                            .randomTicks()
                            .strength(1.5f)
                            .sound(SoundType.LARGE_AMETHYST_BUD)
                            .lightLevel(state -> 7)
                            .pushReaction(PushReaction.DESTROY)));

    /** Razor Ω-grass on Scar ash / void-obsidian. */
    public static final DeferredBlock<OmegaBladesBlock> OMEGA_BLADES = BLOCKS.register(
            "omega_blades",
            () -> new OmegaBladesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY)
                    .lightLevel(state -> 2)
                    .randomTicks()));

    /** Glowing purple moss — Ω-background indicator. */
    public static final DeferredBlock<RottenMossBlock> ROTTEN_MOSS = BLOCKS.register(
            "rotten_moss",
            () -> new RottenMossBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.2f)
                    .sound(SoundType.MOSS_CARPET)
                    .lightLevel(state -> 5)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));

    /** Oily Eldritch Blood puddle near active Scar cracks. */
    public static final DeferredBlock<EldritchBloodPuddleBlock> ELDRITCH_BLOOD_PUDDLE = BLOCKS.register(
            "eldritch_blood_puddle",
            () -> new EldritchBloodPuddleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.35f, 0.2f)
                    .sound(SoundType.SLIME_BLOCK)
                    .lightLevel(state -> 4)
                    .noOcclusion()
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

    /** Φ-planks — conductive wood boards. */
    public static final DeferredBlock<Block> PHI_PLANKS = BLOCKS.register(
            "phi_planks",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(2.5f, 3.0f)
                    .sound(SoundType.WOOD)
                    .lightLevel(state -> 2)
                    .ignitedByLava()));

    /** Φ-glass — sand fused with essonite dust; translucent to Φ. */
    public static final DeferredBlock<Block> PHI_GLASS = BLOCKS.register(
            "phi_glass",
            () -> new TransparentBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.4f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .lightLevel(state -> 3)));

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

    /** Ancient canopy timber — steel-hard Φ wood. */
    public static final DeferredBlock<AncientEssenceWoodBlock> ANCIENT_ESSENCE_WOOD = BLOCKS.register(
            "ancient_essence_wood",
            () -> new AncientEssenceWoodBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(8.0f, 12.0f)
                    .sound(SoundType.WOOD)
                    .lightLevel(state -> 7)
                    .requiresCorrectToolForDrops()
                    .ignitedByLava()));

    /** Concentrated Φ bark — alchemical concentrate. */
    public static final DeferredBlock<RotatedPillarBlock> GOLDEN_BARK = BLOCKS.register(
            "golden_bark",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(3.5f, 5.0f)
                    .sound(SoundType.WOOD)
                    .lightLevel(state -> 9)
                    .ignitedByLava()));

    /** Carnivorous Φ-vine trap. */
    public static final DeferredBlock<PhiSnareVineBlock> PHI_SNARE_VINE = BLOCKS.register(
            "phi_snare_vine",
            () -> new PhiSnareVineBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .noCollission()
                    .strength(0.4f)
                    .sound(SoundType.WEEPING_VINES)
                    .lightLevel(state -> 5)
                    .randomTicks()
                    .pushReaction(PushReaction.DESTROY)));

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

    /** Village mortar — grind essonite materials into dust. */
    public static final DeferredBlock<MortarAndPestleBlock> MORTAR_AND_PESTLE = BLOCKS.register(
            "mortar_and_pestle",
            () -> new MortarAndPestleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(1.2f, 4f)
                    .sound(ModSoundTypes.PHI_STONE)
                    .noOcclusion()));

    /** Era I Φ-torch — blue light, wind-resistant ambience. */
    public static final DeferredBlock<PhiTorchBlock> PHI_TORCH = BLOCKS.register(
            "phi_torch",
            () -> new PhiTorchBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .noCollission()
                    .instabreak()
                    .lightLevel(state -> 12)
                    .sound(SoundType.WOOD)
                    .pushReaction(PushReaction.DESTROY)));

    /** Era I Φ-campfire — LOW heat source fueled by essonite dust/shards. */
    public static final DeferredBlock<PhiCampfireBlock> PHI_CAMPFIRE = BLOCKS.register(
            "phi_campfire",
            () -> new PhiCampfireBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.2f, 3f)
                    .sound(SoundType.WOOD)
                    .lightLevel(state -> state.getValue(PhiCampfireBlock.LIT) ? 10 : 0)
                    .noOcclusion()));

    /** Era I clay crucible — impure ore→shard over heat. */
    public static final DeferredBlock<ClayCrucibleBlock> CLAY_CRUCIBLE = BLOCKS.register(
            "clay_crucible",
            () -> new ClayCrucibleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN)
                    .strength(1.0f, 3f)
                    .sound(SoundType.DECORATED_POT)
                    .noOcclusion()));

    /** Era II Φ-furnace — refine shards / bake Φ-glass using neighbor heat. */
    public static final DeferredBlock<PhiFurnaceBlock> PHI_FURNACE = BLOCKS.register(
            "phi_furnace",
            () -> new PhiFurnaceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(2.5f, 6f)
                    .sound(ModSoundTypes.PHI_STONE)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 4)));

    /** Flameless Φ-burner — dust fuel until Φ-oil exists. */
    public static final DeferredBlock<EssenceBurnerBlock> ESSENCE_BURNER = BLOCKS.register(
            "essence_burner",
            () -> new EssenceBurnerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(1.5f, 4f)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(EssenceBurnerBlock.LIT) ? 9 : 0)
                    .noOcclusion()));

    /** Era IV Spark Reactor («Искра») — wireless Φ-power for nearby machines. */
    public static final DeferredBlock<SparkReactorBlock> SPARK_REACTOR = BLOCKS.register(
            "spark_reactor",
            () -> new SparkReactorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(3.0f, 6f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(SparkReactorBlock.LIT) ? 7 : 0)));

    /** Era IV Heart Reactor core — 3×3×3 multiblock controller. */
    public static final DeferredBlock<HeartReactorBlock> HEART_REACTOR_CORE = BLOCKS.register(
            "heart_reactor_core",
            () -> new HeartReactorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(4.0f, 8f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(HeartReactorBlock.LIT) ? 12 : 0)));

    /** Invisible solid cell of an assembled Heart hull (no BlockItem). */
    public static final DeferredBlock<HeartReactorPartBlock> HEART_REACTOR_PART = BLOCKS.register(
            "heart_reactor_part",
            () -> new HeartReactorPartBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(4.0f, 8f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noLootTable()
                    .isViewBlocking((state, level, pos) -> true)
                    .isSuffocating((state, level, pos) -> true)));

    /** Era IV Forge Reactor («Кузница») — 3×4×3 industrial controller. */
    public static final DeferredBlock<ForgeReactorBlock> FORGE_REACTOR_CORE = BLOCKS.register(
            "forge_reactor_core",
            () -> new ForgeReactorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(5.0f, 10f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(ForgeReactorBlock.LIT) ? 15 : 0)));

    /** Invisible solid cell of an assembled Forge hull (no BlockItem). */
    public static final DeferredBlock<ForgeReactorPartBlock> FORGE_REACTOR_PART = BLOCKS.register(
            "forge_reactor_part",
            () -> new ForgeReactorPartBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(5.0f, 10f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noLootTable()
                    .isViewBlocking((state, level, pos) -> true)
                    .isSuffocating((state, level, pos) -> true)));

    /** Purified void-obsidian product of Forge smelting. */
    public static final DeferredBlock<Block> PURIFIED_OBSIDIAN = BLOCKS.register(
            "purified_obsidian",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(50.0f, 1200f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    /** Φ-concrete — grit + clay building block. */
    public static final DeferredBlock<Block> PHI_CONCRETE = BLOCKS.register(
            "phi_concrete",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 3)));

    /** Ω-anchor — mild entropy / Ω-sickness suppression. */
    public static final DeferredBlock<OmegaAnchorBlock> OMEGA_ANCHOR = BLOCKS.register(
            "omega_anchor",
            () -> new OmegaAnchorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(8.0f, 600f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 5)));

    /** Era V Geo Well casing — shell edges. */
    public static final DeferredBlock<Block> GEO_CASING = BLOCKS.register(
            "geo_casing",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(4.0f, 8f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    /** Era V Geo Well core — 3×3×3 planetary Φ tap. */
    public static final DeferredBlock<GeoWellBlock> GEO_WELL_CORE = BLOCKS.register(
            "geo_well_core",
            () -> new GeoWellBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(5.0f, 10f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(GeoWellBlock.LIT) ? 12 : 0)));

    /** Invisible solid cell of an assembled Geo Well hull. */
    public static final DeferredBlock<GeoWellPartBlock> GEO_WELL_PART = BLOCKS.register(
            "geo_well_part",
            () -> new GeoWellPartBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(5.0f, 10f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noLootTable()
                    .isViewBlocking((state, level, pos) -> true)
                    .isSuffocating((state, level, pos) -> true)));

    /** Era V Climate Array — local Φ-weather emitter. */
    public static final DeferredBlock<ClimateArrayBlock> CLIMATE_ARRAY = BLOCKS.register(
            "climate_array",
            () -> new ClimateArrayBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(3.5f, 6f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(ClimateArrayBlock.LIT) ? 8 : 0)));

    /** Hyper-tunnel film — placed by portal modulator inside a mithril frame (not crafted). */
    public static final DeferredBlock<PortalGateBlock> PORTAL_GATE = BLOCKS.register(
            "portal_gate",
            () -> new PortalGateBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0f, 3600000f)
                    .sound(SoundType.GLASS)
                    .noLootTable()
                    .lightLevel(state -> 12)
                    .noOcclusion()
                    .noCollission()));

    /** Portal modulator — Ψ-computer for mithril-frame hyper-tunnels. */
    public static final DeferredBlock<PortalModulatorBlock> PORTAL_MODULATOR = BLOCKS.register(
            "portal_modulator",
            () -> new PortalModulatorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(4.0f, 8f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(PortalModulatorBlock.LIT) ? 10 : 0)));

    /** Named Φ-beacon — destination star for modulators. */
    public static final DeferredBlock<PhiBeaconBlock> PHI_BEACON = BLOCKS.register(
            "phi_beacon",
            () -> new PhiBeaconBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(3.0f, 6f)
                    .sound(SoundType.AMETHYST)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 11)));

    /** Ω-tainted obsidian — cleansed in Forge. */
    public static final DeferredBlock<Block> OMEGA_TAINTED_OBSIDIAN = BLOCKS.register(
            "omega_tainted_obsidian",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(40.0f, 600f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 3)));

    /** Structural shell for Heart Reactor multiblock. */
    public static final DeferredBlock<Block> REACTOR_CASING = BLOCKS.register(
            "reactor_casing",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(4.0f, 8f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    /** Φ-bus cable — relays PhiPower along a tower. */
    public static final DeferredBlock<PhiBusBlock> PHI_BUS = BLOCKS.register(
            "phi_bus",
            () -> new PhiBusBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(1.5f, 3f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(PhiBusBlock.POWERED) ? 6 : 0)));

    /** Shared turret mount (half-slab). Φ-power intake; barrels assemble on the outward face. */
    public static final DeferredBlock<TurretMountBlock> TURRET_MOUNT = BLOCKS.register(
            "turret_mount",
            () -> new TurretMountBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(3.0f, 8f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(s -> s.getValue(TurretMountBlock.LIT) ? 8 : 0)));

    public static final DeferredBlock<PhiTurretBlock> PLASMA_TURRET = BLOCKS.register(
            "plasma_turret",
            () -> new PhiTurretBlock(
                    TurretKind.PLASMA,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(3.5f, 8f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()));

    public static final DeferredBlock<PhiTurretBlock> KINETIC_TURRET = BLOCKS.register(
            "kinetic_turret",
            () -> new PhiTurretBlock(
                    TurretKind.KINETIC,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_GRAY)
                            .strength(3.5f, 8f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()));

    public static final DeferredBlock<PhiTurretBlock> SPATIAL_TURRET = BLOCKS.register(
            "spatial_turret",
            () -> new PhiTurretBlock(
                    TurretKind.SPATIAL,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(4.0f, 10f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()));

    public static final DeferredBlock<PhiTurretBlock> MENTAL_TURRET = BLOCKS.register(
            "mental_turret",
            () -> new PhiTurretBlock(
                    TurretKind.MENTAL,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLUE)
                            .strength(3.5f, 8f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()));

    public static final DeferredBlock<PhiTurretBlock> OMEGA_TURRET = BLOCKS.register(
            "omega_turret",
            () -> new PhiTurretBlock(
                    TurretKind.OMEGA,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(4.0f, 10f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()));

    /** Essence alembic — blue cauldron for Φ-water potions. */
    public static final DeferredBlock<EssenceAlembicBlock> ESSENCE_ALEMBIC = BLOCKS.register(
            "essence_alembic",
            () -> new EssenceAlembicBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.8f, 4f)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 4)
                    .noOcclusion()));

    /** Era III Ψ-imprinter — writes imprint into chassis / telegraph blanks. */
    public static final DeferredBlock<PsiImprinterBlock> PSI_IMPRINTER = BLOCKS.register(
            "psi_imprinter",
            () -> new PsiImprinterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.0f, 5f)
                    .sound(ModSoundTypes.PHI_STONE)
                    .noOcclusion()));

    public static final DeferredBlock<ArtifactStationBlock> SHAFT_LATHE = BLOCKS.register(
            "shaft_lathe",
            () -> new ArtifactStationBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.0f, 4f)
                            .sound(SoundType.WOOD)
                            .noOcclusion(),
                    ArtifactStationBlock.Kind.LATHE));

    public static final DeferredBlock<ArtifactStationBlock> FACET_CUTTER = BLOCKS.register(
            "facet_cutter",
            () -> new ArtifactStationBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(2.0f, 5f)
                            .sound(ModSoundTypes.PHI_STONE)
                            .noOcclusion(),
                    ArtifactStationBlock.Kind.CUTTER));

    public static final DeferredBlock<ArtifactStationBlock> ARTIFACT_ASSEMBLER = BLOCKS.register(
            "artifact_assembler",
            () -> new ArtifactStationBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.2f, 5f)
                            .sound(SoundType.METAL)
                            .noOcclusion(),
                    ArtifactStationBlock.Kind.ASSEMBLER));

    public static final DeferredBlock<ArtifactStationBlock> SEAL_INSCRIBER = BLOCKS.register(
            "seal_inscriber",
            () -> new ArtifactStationBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(2.0f, 5f)
                            .sound(ModSoundTypes.PHI_STONE)
                            .noOcclusion(),
                    ArtifactStationBlock.Kind.INSCRIBER));

    /** Era III Φ-telegraph — paired same-dimension messaging. */
    public static final DeferredBlock<PhiTelegraphBlock> PHI_TELEGRAPH = BLOCKS.register(
            "phi_telegraph",
            () -> new PhiTelegraphBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.8f, 4f)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 5)));

    /** Era III Φ-crusher base — needs hopper above to form. */
    public static final DeferredBlock<PhiCrusherBlock> PHI_CRUSHER = BLOCKS.register(
            "phi_crusher",
            () -> new PhiCrusherBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(3.0f, 6f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(PhiCrusherBlock.LIT) ? 8 : 0)
                    .noOcclusion()));

    /** Era III Φ-crusher hopper — place on crusher base. */
    public static final DeferredBlock<PhiCrusherHopperBlock> PHI_CRUSHER_HOPPER = BLOCKS.register(
            "phi_crusher_hopper",
            () -> new PhiCrusherHopperBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(2.5f, 5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(PhiCrusherHopperBlock.LIT) ? 6 : 0)
                    .noOcclusion()));

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
