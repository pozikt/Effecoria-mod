package com.effecoria.core.artifact;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
 * Datapack Φ-material profiles: data/effecoria/artifact/materials/*.json
 * Values are clamped to 0..1 (0 insulator → 1 excellent Φ conductor).
 */
public final class MaterialConductivity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Map<ResourceLocation, MaterialDefinition> BY_ITEM = new HashMap<>();

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
                            MaterialDefinition def = parse(entry.getValue().getAsJsonObject(), entry.getKey());
                            BY_ITEM.put(def.itemId(), def);
                        } catch (RuntimeException ex) {
                            LOGGER.error("Failed to load material {}: {}", entry.getKey(), ex.getMessage());
                        }
                    }
                    LOGGER.info("Loaded {} artifact material profiles", BY_ITEM.size());
                }
            };

    private MaterialConductivity() {}

    public static Optional<MaterialDefinition> definitionOf(Item item) {
        return definitionOfId(BuiltInRegistries.ITEM.getKey(item));
    }

    public static Optional<MaterialDefinition> definitionOfId(ResourceLocation itemId) {
        if (itemId == null) {
            return Optional.empty();
        }
        MaterialDefinition direct = BY_ITEM.get(itemId);
        if (direct != null) {
            return Optional.of(direct);
        }
        return Optional.empty();
    }

    public static MaterialDefinition resolve(Item item) {
        return definitionOf(item).orElseGet(() -> MaterialDefinition.fallback(BuiltInRegistries.ITEM.getKey(item)));
    }

    public static MaterialDefinition resolveStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return MaterialDefinition.fallback(ResourceLocation.withDefaultNamespace("air"));
        }
        ResourceLocation material = ModularPartData.material(stack);
        if (material != null && !material.getPath().equals("air")) {
            MaterialDefinition fromPart = BY_ITEM.get(material);
            if (fromPart != null) {
                return fromPart;
            }
        }
        return resolve(stack.getItem());
    }

    public static float ofItem(Item item) {
        return resolve(item).conductivity();
    }

    public static float ofItemId(ResourceLocation itemId) {
        if (itemId == null) {
            return DEFAULT;
        }
        MaterialDefinition def = BY_ITEM.get(itemId);
        if (def != null) {
            return def.conductivity();
        }
        String path = itemId.getPath();
        if (path.endsWith("_planks") || path.equals("stick") || path.contains("log") || path.contains("wood")) {
            return BY_ITEM.getOrDefault(
                            ResourceLocation.withDefaultNamespace("stick"), MaterialDefinition.fallback(itemId))
                    .conductivity();
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

    public static Map<ResourceLocation, MaterialDefinition> all() {
        return Collections.unmodifiableMap(BY_ITEM);
    }

    public static Optional<Float> lookup(ResourceLocation itemId) {
        MaterialDefinition def = BY_ITEM.get(itemId);
        return def == null ? Optional.empty() : Optional.of(def.conductivity());
    }

    private static MaterialDefinition parse(JsonObject json, ResourceLocation fileId) {
        ResourceLocation id = json.has("id") ? ResourceLocation.parse(json.get("id").getAsString()) : fileId;
        ResourceLocation itemId = json.has("item")
                ? ResourceLocation.parse(json.get("item").getAsString())
                : fileId;
        float conductivity = json.has("conductivity") ? json.get("conductivity").getAsFloat() : DEFAULT;
        float positiveBias = json.has("positive_bias") ? json.get("positive_bias").getAsFloat() : 0f;
        float negativeBias = json.has("negative_bias") ? json.get("negative_bias").getAsFloat() : 0f;
        List<MaterialDefinition.ImplicitAffix> implicit = parseImplicit(json);
        String notes = json.has("notes") ? json.get("notes").getAsString() : "";
        return new MaterialDefinition(
                id,
                itemId,
                Mth.clamp(conductivity, 0f, 1f),
                Mth.clamp(positiveBias, 0f, 1f),
                Mth.clamp(negativeBias, 0f, 1f),
                List.copyOf(implicit),
                notes);
    }

    private static List<MaterialDefinition.ImplicitAffix> parseImplicit(JsonObject json) {
        if (!json.has("implicit_affixes")) {
            return List.of();
        }
        JsonArray arr = json.getAsJsonArray("implicit_affixes");
        java.util.ArrayList<MaterialDefinition.ImplicitAffix> out = new java.util.ArrayList<>();
        for (JsonElement el : arr) {
            JsonObject obj = el.getAsJsonObject();
            ResourceLocation affix = ResourceLocation.parse(obj.get("affix").getAsString());
            int tier = obj.has("tier") ? obj.get("tier").getAsInt() : 1;
            float chance = obj.has("chance") ? obj.get("chance").getAsFloat() : 1f;
            out.add(new MaterialDefinition.ImplicitAffix(affix, Math.max(1, tier), Mth.clamp(chance, 0f, 1f)));
        }
        return out;
    }
}
