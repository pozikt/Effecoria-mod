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

/** Mirage blood — water-like fluid with a forced red client tint. */
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

    public static final DeferredHolder<Fluid, FlowingFluid> BLOOD =
            FLUIDS.register("blood", () -> new BaseFlowingFluid.Source(bloodProperties()));
    public static final DeferredHolder<Fluid, FlowingFluid> BLOOD_FLOWING =
            FLUIDS.register("blood_flowing", () -> new BaseFlowingFluid.Flowing(bloodProperties()));

    private static BaseFlowingFluid.Properties bloodProperties() {
        return new BaseFlowingFluid.Properties(BLOOD_TYPE, BLOOD, BLOOD_FLOWING)
                .block(ModBlocks.BLOOD_FLUID)
                .explosionResistance(100f)
                // Keep blood from spreading into ordinary water on the client.
                .slopeFindDistance(0)
                .levelDecreasePerBlock(8);
    }
}
