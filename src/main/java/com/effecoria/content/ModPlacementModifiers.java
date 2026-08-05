package com.effecoria.content;

import com.effecoria.EffecoriaMod;
import com.effecoria.world.placement.UnderPlateauSurfacePlacement;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModPlacementModifiers {
    private ModPlacementModifiers() {}

    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, EffecoriaMod.MOD_ID);

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<UnderPlateauSurfacePlacement>>
            UNDER_PLATEAU_SURFACE = PLACEMENT_MODIFIERS.register(
                    "under_plateau_surface", () -> () -> UnderPlateauSurfacePlacement.CODEC);
}
