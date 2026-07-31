package com.effecoria.world;

import com.effecoria.EffecoriaMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

import javax.annotation.Nullable;

/** Custom Effecoria dimensions. */
public final class ModDimensions {
    private ModDimensions() {}

    public static final ResourceKey<Level> SUBSPACE =
            ResourceKey.create(Registries.DIMENSION, EffecoriaMod.id("subspace"));

    public static final ResourceKey<DimensionType> SUBSPACE_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, EffecoriaMod.id("subspace"));

    public static final ResourceKey<LevelStem> SUBSPACE_STEM =
            ResourceKey.create(Registries.LEVEL_STEM, EffecoriaMod.id("subspace"));

    public static boolean isSubspace(Level level) {
        return level != null && level.dimension().equals(SUBSPACE);
    }

    @Nullable
    public static ServerLevel subspace(MinecraftServer server) {
        return server.getLevel(SUBSPACE);
    }
}
