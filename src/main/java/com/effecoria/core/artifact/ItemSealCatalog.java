package com.effecoria.core.artifact;

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

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Loads item seal definitions from data/effecoria/item_seals. */
public final class ItemSealCatalog {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Map<ResourceLocation, ItemSealDefinition> SEALS = new HashMap<>();

    public static final SimpleJsonResourceReloadListener RELOAD_LISTENER =
            new SimpleJsonResourceReloadListener(GSON, "item_seals") {
                @Override
                protected void apply(
                        Map<ResourceLocation, JsonElement> jsonMap,
                        ResourceManager resourceManager,
                        ProfilerFiller profiler) {
                    SEALS.clear();
                    for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
                        try {
                            ItemSealDefinition def = parse(entry.getValue().getAsJsonObject(), entry.getKey());
                            SEALS.put(def.id(), def);
                        } catch (RuntimeException ex) {
                            LOGGER.error("Failed to load item seal {}: {}", entry.getKey(), ex.getMessage());
                        }
                    }
                    LOGGER.info("Loaded {} item seals", SEALS.size());
                }
            };

    private ItemSealCatalog() {}

    public static Optional<ItemSealDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(SEALS.get(id));
    }

    public static Map<ResourceLocation, ItemSealDefinition> all() {
        return Collections.unmodifiableMap(SEALS);
    }

    public static List<ItemSealDefinition> sorted() {
        List<ItemSealDefinition> out = new ArrayList<>(SEALS.values());
        out.sort(Comparator.comparing(d -> d.id().getPath()));
        return out;
    }

    public static List<ResourceLocation> starterIds() {
        List<ResourceLocation> out = new ArrayList<>();
        for (ItemSealDefinition def : SEALS.values()) {
            if (def.starter()) {
                out.add(def.id());
            }
        }
        out.sort(Comparator.comparing(ResourceLocation::getPath));
        return out;
    }

    public static boolean appliesTo(ItemSealDefinition def, ItemStack stack) {
        if (def.applicableTags().isEmpty()) {
            return true;
        }
        for (TagKey<Item> tag : def.applicableTags()) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static ItemSealDefinition parse(JsonObject json, ResourceLocation fileId) {
        ResourceLocation id = json.has("id") ? ResourceLocation.parse(json.get("id").getAsString()) : fileId;
        List<TagKey<Item>> tags = new ArrayList<>();
        if (json.has("applicable_tags")) {
            JsonArray arr = json.getAsJsonArray("applicable_tags");
            for (JsonElement el : arr) {
                tags.add(TagKey.create(Registries.ITEM, ResourceLocation.parse(el.getAsString())));
            }
        }
        Map<String, Float> params = new HashMap<>();
        if (json.has("params") && json.get("params").isJsonObject()) {
            JsonObject p = json.getAsJsonObject("params");
            for (Map.Entry<String, JsonElement> e : p.entrySet()) {
                params.put(e.getKey(), e.getValue().getAsFloat());
            }
        }
        return new ItemSealDefinition(
                id,
                json.has("max_level") ? json.get("max_level").getAsInt() : 1,
                List.copyOf(tags),
                json.has("effect") ? json.get("effect").getAsString() : "none",
                Map.copyOf(params),
                json.has("starter") && json.get("starter").getAsBoolean());
    }
}
