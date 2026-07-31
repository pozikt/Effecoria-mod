package com.effecoria.content;

import com.effecoria.EffecoriaMod;
import com.effecoria.block.SubspacePortalBlock;
import com.effecoria.block.SubspacePortalBlockEntity;

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
}
