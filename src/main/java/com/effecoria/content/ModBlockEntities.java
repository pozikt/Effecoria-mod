package com.effecoria.content;

import com.effecoria.EffecoriaMod;
import com.effecoria.block.ArtifactAssemblerBlockEntity;
import com.effecoria.block.ClayCrucibleBlock;
import com.effecoria.block.EssenceAlembicBlockEntity;
import com.effecoria.block.EssenceBurnerBlockEntity;
import com.effecoria.block.FacetCutterBlockEntity;
import com.effecoria.block.MortarBlockEntity;
import com.effecoria.block.PhiCampfireBlock;
import com.effecoria.block.PhiFurnaceBlock;
import com.effecoria.block.PhiGeyserBlockEntity;
import com.effecoria.block.PhiTelegraphBlock;
import com.effecoria.block.PsiImprinterBlockEntity;
import com.effecoria.block.SealInscriberBlockEntity;
import com.effecoria.block.ShaftLatheBlockEntity;
import com.effecoria.block.SparkReactorBlockEntity;
import com.effecoria.block.HeartReactorBlockEntity;
import com.effecoria.block.HeartReactorPartBlockEntity;
import com.effecoria.block.ForgeReactorBlockEntity;
import com.effecoria.block.ForgeReactorPartBlockEntity;
import com.effecoria.block.GeoWellBlockEntity;
import com.effecoria.block.GeoWellPartBlockEntity;
import com.effecoria.block.ClimateArrayBlockEntity;
import com.effecoria.block.PortalGateBlockEntity;
import com.effecoria.block.MithrilBlockEntity;
import com.effecoria.block.PortalModulatorBlockEntity;
import com.effecoria.block.PhiBeaconBlockEntity;
import com.effecoria.block.TowerAnchorBlockEntity;
import com.effecoria.block.PhiBusBlockEntity;
import com.effecoria.block.PhiCrusherBlockEntity;
import com.effecoria.block.PhiCrusherHopperBlockEntity;
import com.effecoria.block.OmegaAnchorBlockEntity;
import com.effecoria.block.PhiTurretBlockEntity;
import com.effecoria.block.SubspacePortalBlockEntity;
import com.effecoria.block.WhisperingSpireVentBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EffecoriaMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SubspacePortalBlockEntity>> SUBSPACE_PORTAL =
            BLOCK_ENTITY_TYPES.register(
                    "subspace_portal",
                    () -> BlockEntityType.Builder.of(SubspacePortalBlockEntity::new, ModBlocks.SUBSPACE_PORTAL.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PhiGeyserBlockEntity>> PHI_GEYSER =
            BLOCK_ENTITY_TYPES.register(
                    "phi_geyser",
                    () -> BlockEntityType.Builder.of(PhiGeyserBlockEntity::new, ModBlocks.PHI_GEYSER.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EssenceBurnerBlockEntity>> ESSENCE_BURNER =
            BLOCK_ENTITY_TYPES.register(
                    "essence_burner",
                    () -> BlockEntityType.Builder.of(EssenceBurnerBlockEntity::new, ModBlocks.ESSENCE_BURNER.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SparkReactorBlockEntity>> SPARK_REACTOR =
            BLOCK_ENTITY_TYPES.register(
                    "spark_reactor",
                    () -> BlockEntityType.Builder.of(SparkReactorBlockEntity::new, ModBlocks.SPARK_REACTOR.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeartReactorBlockEntity>> HEART_REACTOR_CORE =
            BLOCK_ENTITY_TYPES.register(
                    "heart_reactor_core",
                    () -> BlockEntityType.Builder.of(HeartReactorBlockEntity::new, ModBlocks.HEART_REACTOR_CORE.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeartReactorPartBlockEntity>>
            HEART_REACTOR_PART = BLOCK_ENTITY_TYPES.register(
                    "heart_reactor_part",
                    () -> BlockEntityType.Builder.of(
                                    HeartReactorPartBlockEntity::new, ModBlocks.HEART_REACTOR_PART.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ForgeReactorBlockEntity>> FORGE_REACTOR_CORE =
            BLOCK_ENTITY_TYPES.register(
                    "forge_reactor_core",
                    () -> BlockEntityType.Builder.of(ForgeReactorBlockEntity::new, ModBlocks.FORGE_REACTOR_CORE.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ForgeReactorPartBlockEntity>>
            FORGE_REACTOR_PART = BLOCK_ENTITY_TYPES.register(
                    "forge_reactor_part",
                    () -> BlockEntityType.Builder.of(
                                    ForgeReactorPartBlockEntity::new, ModBlocks.FORGE_REACTOR_PART.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeoWellBlockEntity>> GEO_WELL_CORE =
            BLOCK_ENTITY_TYPES.register(
                    "geo_well_core",
                    () -> BlockEntityType.Builder.of(GeoWellBlockEntity::new, ModBlocks.GEO_WELL_CORE.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeoWellPartBlockEntity>> GEO_WELL_PART =
            BLOCK_ENTITY_TYPES.register(
                    "geo_well_part",
                    () -> BlockEntityType.Builder.of(GeoWellPartBlockEntity::new, ModBlocks.GEO_WELL_PART.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ClimateArrayBlockEntity>> CLIMATE_ARRAY =
            BLOCK_ENTITY_TYPES.register(
                    "climate_array",
                    () -> BlockEntityType.Builder.of(ClimateArrayBlockEntity::new, ModBlocks.CLIMATE_ARRAY.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PortalGateBlockEntity>> PORTAL_GATE =
            BLOCK_ENTITY_TYPES.register(
                    "portal_gate",
                    () -> BlockEntityType.Builder.of(PortalGateBlockEntity::new, ModBlocks.PORTAL_GATE.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PortalModulatorBlockEntity>> PORTAL_MODULATOR =
            BLOCK_ENTITY_TYPES.register(
                    "portal_modulator",
                    () -> BlockEntityType.Builder.of(
                                    PortalModulatorBlockEntity::new, ModBlocks.PORTAL_MODULATOR.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PhiBeaconBlockEntity>> PHI_BEACON =
            BLOCK_ENTITY_TYPES.register(
                    "phi_beacon",
                    () -> BlockEntityType.Builder.of(PhiBeaconBlockEntity::new, ModBlocks.PHI_BEACON.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TowerAnchorBlockEntity>> TOWER_ANCHOR =
            BLOCK_ENTITY_TYPES.register(
                    "tower_anchor",
                    () -> BlockEntityType.Builder.of(TowerAnchorBlockEntity::new, ModBlocks.TOWER_ANCHOR.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PhiBusBlockEntity>> PHI_BUS =
            BLOCK_ENTITY_TYPES.register(
                    "phi_bus",
                    () -> BlockEntityType.Builder.of(PhiBusBlockEntity::new, ModBlocks.PHI_BUS.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MithrilBlockEntity>> MITHRIL_BLOCK =
            BLOCK_ENTITY_TYPES.register(
                    "mithril_block",
                    () -> BlockEntityType.Builder.of(MithrilBlockEntity::new, ModBlocks.MITHRIL_BLOCK.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PhiTurretBlockEntity>> PHI_TURRET =
            BLOCK_ENTITY_TYPES.register(
                    "phi_turret",
                    () -> BlockEntityType.Builder.of(PhiTurretBlockEntity::new, ModBlocks.TURRET_MOUNT.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PhiCrusherBlockEntity>> PHI_CRUSHER =
            BLOCK_ENTITY_TYPES.register(
                    "phi_crusher",
                    () -> BlockEntityType.Builder.of(PhiCrusherBlockEntity::new, ModBlocks.PHI_CRUSHER.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PhiCrusherHopperBlockEntity>>
            PHI_CRUSHER_HOPPER = BLOCK_ENTITY_TYPES.register(
                    "phi_crusher_hopper",
                    () -> BlockEntityType.Builder.of(
                                    PhiCrusherHopperBlockEntity::new, ModBlocks.PHI_CRUSHER_HOPPER.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OmegaAnchorBlockEntity>> OMEGA_ANCHOR =
            BLOCK_ENTITY_TYPES.register(
                    "omega_anchor",
                    () -> BlockEntityType.Builder.of(OmegaAnchorBlockEntity::new, ModBlocks.OMEGA_ANCHOR.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EssenceAlembicBlockEntity>> ESSENCE_ALEMBIC =
            BLOCK_ENTITY_TYPES.register(
                    "essence_alembic",
                    () -> BlockEntityType.Builder.of(EssenceAlembicBlockEntity::new, ModBlocks.ESSENCE_ALEMBIC.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MortarBlockEntity>> MORTAR_AND_PESTLE =
            BLOCK_ENTITY_TYPES.register(
                    "mortar_and_pestle",
                    () -> BlockEntityType.Builder.of(MortarBlockEntity::new, ModBlocks.MORTAR_AND_PESTLE.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PhiCampfireBlock.PhiCampfireBlockEntity>>
            PHI_CAMPFIRE = BLOCK_ENTITY_TYPES.register(
                    "phi_campfire",
                    () -> BlockEntityType.Builder.of(
                                    PhiCampfireBlock.PhiCampfireBlockEntity::new, ModBlocks.PHI_CAMPFIRE.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ClayCrucibleBlock.ClayCrucibleBlockEntity>>
            CLAY_CRUCIBLE = BLOCK_ENTITY_TYPES.register(
                    "clay_crucible",
                    () -> BlockEntityType.Builder.of(
                                    ClayCrucibleBlock.ClayCrucibleBlockEntity::new, ModBlocks.CLAY_CRUCIBLE.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PhiFurnaceBlock.PhiFurnaceBlockEntity>>
            PHI_FURNACE = BLOCK_ENTITY_TYPES.register(
                    "phi_furnace",
                    () -> BlockEntityType.Builder.of(
                                    PhiFurnaceBlock.PhiFurnaceBlockEntity::new, ModBlocks.PHI_FURNACE.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PsiImprinterBlockEntity>>
            PSI_IMPRINTER = BLOCK_ENTITY_TYPES.register(
                    "psi_imprinter",
                    () -> BlockEntityType.Builder.of(PsiImprinterBlockEntity::new, ModBlocks.PSI_IMPRINTER.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShaftLatheBlockEntity>> SHAFT_LATHE =
            BLOCK_ENTITY_TYPES.register(
                    "shaft_lathe",
                    () -> BlockEntityType.Builder.of(ShaftLatheBlockEntity::new, ModBlocks.SHAFT_LATHE.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FacetCutterBlockEntity>> FACET_CUTTER =
            BLOCK_ENTITY_TYPES.register(
                    "facet_cutter",
                    () -> BlockEntityType.Builder.of(FacetCutterBlockEntity::new, ModBlocks.FACET_CUTTER.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArtifactAssemblerBlockEntity>>
            ARTIFACT_ASSEMBLER = BLOCK_ENTITY_TYPES.register(
                    "artifact_assembler",
                    () -> BlockEntityType.Builder.of(
                                    ArtifactAssemblerBlockEntity::new, ModBlocks.ARTIFACT_ASSEMBLER.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SealInscriberBlockEntity>> SEAL_INSCRIBER =
            BLOCK_ENTITY_TYPES.register(
                    "seal_inscriber",
                    () -> BlockEntityType.Builder.of(SealInscriberBlockEntity::new, ModBlocks.SEAL_INSCRIBER.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PhiTelegraphBlock.PhiTelegraphBlockEntity>>
            PHI_TELEGRAPH = BLOCK_ENTITY_TYPES.register(
                    "phi_telegraph",
                    () -> BlockEntityType.Builder.of(
                                    PhiTelegraphBlock.PhiTelegraphBlockEntity::new, ModBlocks.PHI_TELEGRAPH.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WhisperingSpireVentBlockEntity>>
            WHISPERING_SPIRE_VENT = BLOCK_ENTITY_TYPES.register(
                    "whispering_spire_vent",
                    () -> BlockEntityType.Builder.of(
                                    WhisperingSpireVentBlockEntity::new, ModBlocks.WHISPERING_SPIRE_VENT.get())
                            .build(null));
}
