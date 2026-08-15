package com.effecoria.core.fabricator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Loads datapack fabricator_recipes JSON files. */
public final class FabricatorCatalog {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Map<ResourceLocation, FabricatorRecipeDefinition> RECIPES = new HashMap<>();

    public static final SimpleJsonResourceReloadListener RELOAD_LISTENER =
            new SimpleJsonResourceReloadListener(GSON, "fabricator_recipes") {
                @Override
                protected void apply(
                        Map<ResourceLocation, JsonElement> jsonMap,
                        ResourceManager resourceManager,
                        ProfilerFiller profiler) {
                    RECIPES.clear();
                    for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
                        try {
                            FabricatorRecipeDefinition def = parse(entry.getValue().getAsJsonObject(), entry.getKey());
                            RECIPES.put(def.id(), def);
                        } catch (RuntimeException ex) {
                            LOGGER.error("Failed to load fabricator recipe {}: {}", entry.getKey(), ex.getMessage());
                        }
                    }
                    LOGGER.info("Loaded {} fabricator recipes", RECIPES.size());
                }
            };

    private FabricatorCatalog() {}

    public static Optional<FabricatorRecipeDefinition> byId(ResourceLocation id) {
        return Optional.ofNullable(RECIPES.get(id));
    }

    public static List<FabricatorRecipeDefinition> all() {
        List<FabricatorRecipeDefinition> out = new ArrayList<>(RECIPES.values());
        out.sort(Comparator.comparing(r -> r.id().toString()));
        return List.copyOf(out);
    }

    /**
     * Find a recipe whose result item matches {@code sample}, with {@code minClass <= maxClass}.
     * Prefers exact result count match, then lowest minClass.
     */
    public static Optional<FabricatorRecipeDefinition> findForScan(ItemStack sample, int maxClass) {
        if (sample.isEmpty()) {
            return Optional.empty();
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(sample.getItem());
        List<FabricatorRecipeDefinition> matches = new ArrayList<>();
        for (FabricatorRecipeDefinition recipe : RECIPES.values()) {
            if (recipe.minClass() <= maxClass && recipe.resultItem().equals(itemId)) {
                matches.add(recipe);
            }
        }
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        matches.sort(Comparator
                .comparingInt((FabricatorRecipeDefinition r) -> r.resultCount() == sample.getCount() ? 0 : 1)
                .thenComparingInt(FabricatorRecipeDefinition::minClass)
                .thenComparing(r -> r.id().toString()));
        return Optional.of(matches.get(0));
    }

    public static ItemStack resultStack(FabricatorRecipeDefinition recipe) {
        Item item = BuiltInRegistries.ITEM.get(recipe.resultItem());
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, Math.max(1, recipe.resultCount()));
    }

    public static Map<ResourceLocation, FabricatorRecipeDefinition> map() {
        return Collections.unmodifiableMap(RECIPES);
    }

    private static FabricatorRecipeDefinition parse(JsonObject json, ResourceLocation fileId) {
        ResourceLocation id = json.has("id") ? ResourceLocation.parse(json.get("id").getAsString()) : fileId;
        JsonObject result = json.getAsJsonObject("result");
        ResourceLocation resultItem = ResourceLocation.parse(result.get("item").getAsString());
        int resultCount = result.has("count") ? result.get("count").getAsInt() : 1;
        List<FabricatorIngredient> ingredients = new ArrayList<>();
        JsonArray arr = json.getAsJsonArray("ingredients");
        for (JsonElement el : arr) {
            JsonObject line = el.getAsJsonObject();
            int count = line.has("count") ? line.get("count").getAsInt() : 1;
            if (line.has("item")) {
                ingredients.add(FabricatorIngredient.ofItem(ResourceLocation.parse(line.get("item").getAsString()), count));
            } else if (line.has("tag")) {
                ingredients.add(FabricatorIngredient.ofTag(ResourceLocation.parse(line.get("tag").getAsString()), count));
            } else {
                throw new IllegalArgumentException("ingredient needs item or tag");
            }
        }
        if (ingredients.isEmpty() || ingredients.size() > 4) {
            throw new IllegalArgumentException("ingredients must be 1..4");
        }
        return new FabricatorRecipeDefinition(
                id,
                resultItem,
                Math.max(1, resultCount),
                List.copyOf(ingredients),
                json.has("cook_ticks") ? Math.max(1, json.get("cook_ticks").getAsInt()) : 100,
                json.has("power_per_tick") ? Math.max(1, json.get("power_per_tick").getAsInt()) : 1,
                json.has("min_class") ? Math.max(1, Math.min(3, json.get("min_class").getAsInt())) : 1,
                json.has("omega_percent") ? Math.max(0, json.get("omega_percent").getAsInt()) : 1);
    }
}
