package com.effecoria.world;

import com.effecoria.EffecoriaMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.Optional;

public final class ModTreeGrowers {
    private ModTreeGrowers() {}

    public static final ResourceKey<ConfiguredFeature<?, ?>> PHI_TREE =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, EffecoriaMod.id("phi_tree"));

    public static final TreeGrower PHI = new TreeGrower(
            "phi",
            Optional.empty(),
            Optional.of(PHI_TREE),
            Optional.empty());
}
