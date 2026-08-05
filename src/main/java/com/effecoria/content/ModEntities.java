package com.effecoria.content;

import com.effecoria.EffecoriaMod;
import com.effecoria.entity.CrystalCrabEntity;
import com.effecoria.entity.DeathShadowEntity;
import com.effecoria.entity.EidosEntity;
import com.effecoria.entity.MirageHorrorEntity;
import com.effecoria.entity.PhiLarvaEntity;
import com.effecoria.entity.PhiScorpionEntity;
import com.effecoria.entity.RootCageEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
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

    public static final DeferredHolder<EntityType<?>, EntityType<MirageHorrorEntity>> MIRAGE_HORROR =
            ENTITY_TYPES.register(
                    "mirage_horror",
                    () -> EntityType.Builder.<MirageHorrorEntity>of(MirageHorrorEntity::new, MobCategory.MONSTER)
                            .sized(3.4f, 4.6f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .fireImmune()
                            .build(EffecoriaMod.id("mirage_horror").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<PhiLarvaEntity>> PHI_LARVA =
            ENTITY_TYPES.register(
                    "phi_larva",
                    () -> EntityType.Builder.<PhiLarvaEntity>of(PhiLarvaEntity::new, MobCategory.CREATURE)
                            .sized(0.55f, 0.35f)
                            .clientTrackingRange(8)
                            .build(EffecoriaMod.id("phi_larva").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<CrystalCrabEntity>> CRYSTAL_CRAB =
            ENTITY_TYPES.register(
                    "crystal_crab",
                    () -> EntityType.Builder.<CrystalCrabEntity>of(CrystalCrabEntity::new, MobCategory.MONSTER)
                            .sized(1.1f, 0.75f)
                            .clientTrackingRange(10)
                            .build(EffecoriaMod.id("crystal_crab").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<PhiScorpionEntity>> PHI_SCORPION =
            ENTITY_TYPES.register(
                    "phi_scorpion",
                    () -> EntityType.Builder.<PhiScorpionEntity>of(PhiScorpionEntity::new, MobCategory.MONSTER)
                            .sized(1.0f, 0.7f)
                            .clientTrackingRange(12)
                            .fireImmune()
                            .build(EffecoriaMod.id("phi_scorpion").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<EidosEntity>> EIDOS =
            ENTITY_TYPES.register(
                    "eidos",
                    () -> EntityType.Builder.<EidosEntity>of(EidosEntity::new, MobCategory.CREATURE)
                            .sized(0.7f, 1.4f)
                            .clientTrackingRange(16)
                            .fireImmune()
                            .build(EffecoriaMod.id("eidos").toString()));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(DEATH_SHADOW.get(), LivingEntity.createLivingAttributes().build());
        event.put(MIRAGE_HORROR.get(), MirageHorrorEntity.createAttributes().build());
        event.put(PHI_LARVA.get(), PhiLarvaEntity.createAttributes().build());
        event.put(CRYSTAL_CRAB.get(), CrystalCrabEntity.createAttributes().build());
        event.put(PHI_SCORPION.get(), PhiScorpionEntity.createAttributes().build());
        event.put(EIDOS.get(), EidosEntity.createAttributes().build());
    }

    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                PHI_LARVA.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                CRYSTAL_CRAB.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                PHI_SCORPION.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                EIDOS.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getRawBrightness(pos, 0) > 7,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
