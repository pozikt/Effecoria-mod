package com.effecoria.content;

import com.effecoria.EffecoriaMod;
import com.effecoria.block.EssenceAlembicBlockEntity;
import com.effecoria.block.EssenceBurnerBlockEntity;
import com.effecoria.block.MortarBlockEntity;
import com.effecoria.block.PhiGeyserBlockEntity;
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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WhisperingSpireVentBlockEntity>>
            WHISPERING_SPIRE_VENT = BLOCK_ENTITY_TYPES.register(
                    "whispering_spire_vent",
                    () -> BlockEntityType.Builder.of(
                                    WhisperingSpireVentBlockEntity::new, ModBlocks.WHISPERING_SPIRE_VENT.get())
                            .build(null));
}
