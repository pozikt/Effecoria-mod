package com.effecoria.content;

import com.effecoria.EffecoriaMod;
import com.effecoria.entity.RootCageEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, EffecoriaMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<RootCageEntity>> ROOT_CAGE =
            ENTITY_TYPES.register(
                    "root_cage",
                    () -> EntityType.Builder.<RootCageEntity>of(RootCageEntity::new, MobCategory.MISC)
                            .sized(1.0f, 1.8f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .fireImmune()
                            .build(EffecoriaMod.id("root_cage").toString()));
}
