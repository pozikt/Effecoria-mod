package com.effecoria.core.artifact;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Datapack Φ-conductivity for craft materials: data/effecoria/artifact/materials/*.json
 * Values are clamped to 0..1 (0 insulator → 1 excellent Φ conductor).
 */
public final class MaterialConductivity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Map<ResourceLocation, Float> BY_ITEM = new HashMap<>();

    /** Fallback when material is unknown (dry wood / generic). */
    public static final float DEFAULT = 0.25f;

    public static final SimpleJsonResourceReloadListener RELOAD_LISTENER =
            new SimpleJsonResourceReloadListener(GSON, "artifact/materials") {
                @Override
                protected void apply(
                        Map<ResourceLocation, JsonElement> jsonMap,
                        ResourceManager resourceManager,
                        ProfilerFiller profiler) {
                    BY_ITEM.clear();
                    for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
                        try {
                            JsonObject json = entry.getValue().getAsJsonObject();
                            ResourceLocation itemId = json.has("item")
                                    ? ResourceLocation.parse(json.get("item").getAsString())
                                    : entry.getKey();
                            float c = json.has("conductivity") ? json.get("conductivity").getAsFloat() : DEFAULT;
                            BY_ITEM.put(itemId, Mth.clamp(c, 0f, 1f));
                        } catch (RuntimeException ex) {
                            LOGGER.error("Failed to load material {}: {}", entry.getKey(), ex.getMessage());
                        }
                    }
                    LOGGER.info("Loaded {} material conductivity entries", BY_ITEM.size());
                }
            };

    private MaterialConductivity() {}

    public static float ofItem(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return ofItemId(id);
    }

    public static float ofItemId(ResourceLocation itemId) {
        if (itemId == null) {
            return DEFAULT;
        }
        Float direct = BY_ITEM.get(itemId);
        if (direct != null) {
            return direct;
        }
        // Planks / wood family fallback
        String path = itemId.getPath();
        if (path.endsWith("_planks") || path.equals("stick") || path.contains("log") || path.contains("wood")) {
            return BY_ITEM.getOrDefault(
                    ResourceLocation.withDefaultNamespace("stick"), 0.2f);
        }
        return DEFAULT;
    }

    public static float ofStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return DEFAULT;
        }
        float stamped = ModularPartData.conductivity(stack);
        if (stamped >= 0f) {
            return stamped;
        }
        if (AssembledGearData.hasGearConductivity(stack)) {
            return AssembledGearData.conductivity(stack);
        }
        return ofItem(stack.getItem());
    }

    public static Map<ResourceLocation, Float> all() {
        return Collections.unmodifiableMap(BY_ITEM);
    }

    public static Optional<Float> lookup(ResourceLocation itemId) {
        return Optional.ofNullable(BY_ITEM.get(itemId));
    }
}
