package com.effecoria.content;

import com.effecoria.EffecoriaMod;
import com.effecoria.entity.CrystalCrabEntity;
import com.effecoria.entity.DeathShadowEntity;
import com.effecoria.entity.EidosEntity;
import com.effecoria.entity.MirageHorrorEntity;
import com.effecoria.entity.OmegaShadeEntity;
import com.effecoria.entity.GlassWormEntity;
import com.effecoria.entity.PhiEntEntity;
import com.effecoria.entity.PhiLemurEntity;
import com.effecoria.entity.WailerBatEntity;
import com.effecoria.entity.OmegaWormEntity;
import com.effecoria.entity.PhiLarvaEntity;
import com.effecoria.entity.RootCageEntity;
import com.effecoria.entity.RotfangMinkEntity;
import com.effecoria.entity.EssenceWyvernEntity;
import com.effecoria.entity.PhiConstructEntity;
import com.effecoria.entity.VitrifiedGolemEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
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

    public static final DeferredHolder<EntityType<?>, EntityType<EidosEntity>> EIDOS =
            ENTITY_TYPES.register(
                    "eidos",
                    () -> EntityType.Builder.<EidosEntity>of(EidosEntity::new, MobCategory.CREATURE)
                            .sized(0.7f, 1.4f)
                            .clientTrackingRange(16)
                            .fireImmune()
                            .build(EffecoriaMod.id("eidos").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<VitrifiedGolemEntity>> VITRIFIED_GOLEM =
            ENTITY_TYPES.register(
                    "vitrified_golem",
                    () -> EntityType.Builder.<VitrifiedGolemEntity>of(VitrifiedGolemEntity::new, MobCategory.MONSTER)
                            .sized(0.9f, 2.15f)
                            .clientTrackingRange(12)
                            .fireImmune()
                            .build(EffecoriaMod.id("vitrified_golem").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<PhiConstructEntity>> PHI_CONSTRUCT =
            ENTITY_TYPES.register(
                    "phi_construct",
                    () -> EntityType.Builder.<PhiConstructEntity>of(PhiConstructEntity::new, MobCategory.CREATURE)
                            .sized(0.9f, 2.15f)
                            .clientTrackingRange(12)
                            .fireImmune()
                            .build(EffecoriaMod.id("phi_construct").toString()));

    /** Classical wyvern: no forelegs. Adult MVP hitbox (visual wingspan ~8–10 blocks). */
    public static final DeferredHolder<EntityType<?>, EntityType<EssenceWyvernEntity>> ESSENCE_WYVERN =
            ENTITY_TYPES.register(
                    "essence_wyvern",
                    () -> EntityType.Builder.<EssenceWyvernEntity>of(EssenceWyvernEntity::new, MobCategory.MONSTER)
                            .sized(3.2f, 3.4f)
                            .clientTrackingRange(64)
                            .fireImmune()
                            .build(EffecoriaMod.id("essence_wyvern").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<RotfangMinkEntity>> ROTFANG_MINK =
            ENTITY_TYPES.register(
                    "rotfang_mink",
                    () -> EntityType.Builder.<RotfangMinkEntity>of(RotfangMinkEntity::new, MobCategory.MONSTER)
                            .sized(0.7f, 0.6f)
                            .clientTrackingRange(10)
                            .build(EffecoriaMod.id("rotfang_mink").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<OmegaShadeEntity>> OMEGA_SHADE =
            ENTITY_TYPES.register(
                    "omega_shade",
                    () -> EntityType.Builder.<OmegaShadeEntity>of(OmegaShadeEntity::new, MobCategory.MONSTER)
                            .sized(0.5f, 0.9f)
                            .clientTrackingRange(16)
                            .fireImmune()
                            .build(EffecoriaMod.id("omega_shade").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<OmegaWormEntity>> OMEGA_WORM =
            ENTITY_TYPES.register(
                    "omega_worm",
                    () -> EntityType.Builder.<OmegaWormEntity>of(OmegaWormEntity::new, MobCategory.MONSTER)
                            .sized(0.7f, 0.45f)
                            .clientTrackingRange(8)
                            .build(EffecoriaMod.id("omega_worm").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<PhiEntEntity>> PHI_ENT =
            ENTITY_TYPES.register(
                    "phi_ent",
                    () -> EntityType.Builder.<PhiEntEntity>of(PhiEntEntity::new, MobCategory.CREATURE)
                            .sized(1.35f, 2.55f)
                            .clientTrackingRange(12)
                            .build(EffecoriaMod.id("phi_ent").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<PhiLemurEntity>> PHI_LEMUR =
            ENTITY_TYPES.register(
                    "phi_lemur",
                    () -> EntityType.Builder.<PhiLemurEntity>of(PhiLemurEntity::new, MobCategory.CREATURE)
                            .sized(0.5f, 0.55f)
                            .clientTrackingRange(10)
                            .build(EffecoriaMod.id("phi_lemur").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<WailerBatEntity>> WAILER_BAT =
            ENTITY_TYPES.register(
                    "wailer_bat",
                    () -> EntityType.Builder.<WailerBatEntity>of(WailerBatEntity::new, MobCategory.AMBIENT)
                            .sized(0.5f, 0.9f)
                            .clientTrackingRange(6)
                            .build(EffecoriaMod.id("wailer_bat").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<GlassWormEntity>> GLASS_WORM =
            ENTITY_TYPES.register(
                    "glass_worm",
                    () -> EntityType.Builder.<GlassWormEntity>of(GlassWormEntity::new, MobCategory.MONSTER)
                            .sized(0.42f, 0.32f)
                            .clientTrackingRange(8)
                            .build(EffecoriaMod.id("glass_worm").toString()));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(DEATH_SHADOW.get(), LivingEntity.createLivingAttributes().build());
        event.put(MIRAGE_HORROR.get(), MirageHorrorEntity.createAttributes().build());
        event.put(PHI_LARVA.get(), PhiLarvaEntity.createAttributes().build());
        event.put(CRYSTAL_CRAB.get(), CrystalCrabEntity.createAttributes().build());
        event.put(EIDOS.get(), EidosEntity.createAttributes().build());
        event.put(VITRIFIED_GOLEM.get(), VitrifiedGolemEntity.createAttributes().build());
        event.put(PHI_CONSTRUCT.get(), PhiConstructEntity.createAttributes().build());
        event.put(ESSENCE_WYVERN.get(), EssenceWyvernEntity.createAttributes().build());
        event.put(ROTFANG_MINK.get(), RotfangMinkEntity.createAttributes().build());
        event.put(OMEGA_SHADE.get(), OmegaShadeEntity.createAttributes().build());
        event.put(OMEGA_WORM.get(), OmegaWormEntity.createAttributes().build());
        event.put(PHI_ENT.get(), PhiEntEntity.createAttributes().build());
        event.put(PHI_LEMUR.get(), PhiLemurEntity.createAttributes().build());
        event.put(WAILER_BAT.get(), WailerBatEntity.createAttributes().build());
        event.put(GLASS_WORM.get(), GlassWormEntity.createAttributes().build());
    }

    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                PHI_LARVA.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EffecoriaMobSpawns::phiLarva,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                CRYSTAL_CRAB.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EffecoriaMobSpawns::crystalCrab,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                EIDOS.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EffecoriaMobSpawns::eidos,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                VITRIFIED_GOLEM.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EffecoriaMobSpawns::vitrifiedGolem,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                ESSENCE_WYVERN.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EffecoriaMobSpawns::essenceWyvern,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                ROTFANG_MINK.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EffecoriaMobSpawns::rotfangMink,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                OMEGA_SHADE.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EffecoriaMobSpawns::omegaShade,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                OMEGA_WORM.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EffecoriaMobSpawns::omegaWorm,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                PHI_ENT.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EffecoriaMobSpawns::phiEnt,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                PHI_LEMUR.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EffecoriaMobSpawns::phiLemur,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                WAILER_BAT.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EffecoriaMobSpawns::wailerBat,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                GLASS_WORM.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EffecoriaMobSpawns::glassWorm,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
