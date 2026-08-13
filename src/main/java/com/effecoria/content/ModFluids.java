package com.effecoria.content;

import com.effecoria.EffecoriaMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Fluids: mirage blood + cave Φ-hydrolat. */
public final class ModFluids {
    private ModFluids() {}

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, EffecoriaMod.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, EffecoriaMod.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> BLOOD_TYPE = FLUID_TYPES.register(
            "blood",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.effecoria.blood")
                    .fallDistanceModifier(0f)
                    .canExtinguish(true)
                    .supportsBoating(false)
                    .density(1400)
                    .viscosity(2000)
                    .temperature(310)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)));

    public static final DeferredHolder<FluidType, FluidType> PHI_WATER_TYPE = FLUID_TYPES.register(
            "phi_water",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.effecoria.phi_water")
                    .fallDistanceModifier(0f)
                    .canExtinguish(true)
                    .canHydrate(true)
                    .supportsBoating(true)
                    .canSwim(true)
                    .density(1100)
                    .viscosity(1200)
                    .temperature(278)
                    .lightLevel(8)
                    .motionScale(0.012)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)));

    public static final DeferredHolder<FluidType, FluidType> PURIFIED_PHI_WATER_TYPE = FLUID_TYPES.register(
            "purified_phi_water",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.effecoria.purified_phi_water")
                    .fallDistanceModifier(0f)
                    .canExtinguish(true)
                    .canHydrate(true)
                    .supportsBoating(false)
                    .canSwim(true)
                    .density(1150)
                    .viscosity(1600)
                    .temperature(288)
                    .lightLevel(10)
                    .motionScale(0.008)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)));

    public static final DeferredHolder<Fluid, FlowingFluid> BLOOD =
            FLUIDS.register("blood", () -> new BaseFlowingFluid.Source(bloodProperties()));
    public static final DeferredHolder<Fluid, FlowingFluid> BLOOD_FLOWING =
            FLUIDS.register("blood_flowing", () -> new BaseFlowingFluid.Flowing(bloodProperties()));

    public static final DeferredHolder<Fluid, FlowingFluid> PHI_WATER =
            FLUIDS.register("phi_water", () -> new PhiWaterFluid.Source(phiWaterProperties()));
    public static final DeferredHolder<Fluid, FlowingFluid> PHI_WATER_FLOWING =
            FLUIDS.register("phi_water_flowing", () -> new PhiWaterFluid.Flowing(phiWaterProperties()));

    /** Contained regen-capsule bath — almost no spread (placed by chamber fill only). */
    public static final DeferredHolder<Fluid, FlowingFluid> PURIFIED_PHI_WATER =
            FLUIDS.register("purified_phi_water", () -> new BaseFlowingFluid.Source(purifiedPhiWaterProperties()));
    public static final DeferredHolder<Fluid, FlowingFluid> PURIFIED_PHI_WATER_FLOWING =
            FLUIDS.register(
                    "purified_phi_water_flowing", () -> new BaseFlowingFluid.Flowing(purifiedPhiWaterProperties()));

    private static BaseFlowingFluid.Properties bloodProperties() {
        return new BaseFlowingFluid.Properties(BLOOD_TYPE, BLOOD, BLOOD_FLOWING)
                .block(ModBlocks.BLOOD_FLUID)
                .explosionResistance(100f)
                .slopeFindDistance(0)
                .levelDecreasePerBlock(8);
    }

    private static BaseFlowingFluid.Properties phiWaterProperties() {
        return new BaseFlowingFluid.Properties(PHI_WATER_TYPE, PHI_WATER, PHI_WATER_FLOWING)
                .block(ModBlocks.PHI_WATER)
                .bucket(ModItems.PHI_WATER_BUCKET)
                .explosionResistance(100f)
                // Vanilla-like fall/spread so cliff edges become waterfalls
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1);
    }

    private static BaseFlowingFluid.Properties purifiedPhiWaterProperties() {
        return new BaseFlowingFluid.Properties(PURIFIED_PHI_WATER_TYPE, PURIFIED_PHI_WATER, PURIFIED_PHI_WATER_FLOWING)
                .block(ModBlocks.PURIFIED_PHI_WATER)
                .explosionResistance(100f)
                .slopeFindDistance(0)
                .levelDecreasePerBlock(8);
    }
}
