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
