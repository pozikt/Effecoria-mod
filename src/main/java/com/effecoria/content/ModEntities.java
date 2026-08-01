package com.effecoria.content;

import com.effecoria.EffecoriaMod;
import com.effecoria.entity.DeathShadowEntity;
import com.effecoria.entity.RootCageEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
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

    public static final DeferredHolder<EntityType<?>, EntityType<DeathShadowEntity>> DEATH_SHADOW =
            ENTITY_TYPES.register(
                    "death_shadow",
                    () -> EntityType.Builder.<DeathShadowEntity>of(DeathShadowEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.0f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .fireImmune()
                            .build(EffecoriaMod.id("death_shadow").toString()));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(DEATH_SHADOW.get(), LivingEntity.createLivingAttributes().build());
    }
}
