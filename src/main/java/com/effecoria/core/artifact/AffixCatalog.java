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

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads jewelry affix definitions from data/effecoria/artifact/affixes. */
public final class AffixCatalog {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Map<ResourceLocation, AffixDefinition> AFFIXES = new HashMap<>();

    public static final SimpleJsonResourceReloadListener RELOAD_LISTENER =
            new SimpleJsonResourceReloadListener(GSON, "artifact/affixes") {
                @Override
                protected void apply(
                        Map<ResourceLocation, JsonElement> jsonMap,
                        ResourceManager resourceManager,
                        ProfilerFiller profiler) {
                    AFFIXES.clear();
                    for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
                        try {
                            AffixDefinition def = parse(entry.getValue().getAsJsonObject(), entry.getKey());
                            AFFIXES.put(def.id(), def);
                        } catch (RuntimeException ex) {
                            LOGGER.error("Failed to load affix {}: {}", entry.getKey(), ex.getMessage());
                        }
                    }
                    LOGGER.info("Loaded {} jewelry affixes", AFFIXES.size());
                }
            };

    private AffixCatalog() {}

    public static Optional<AffixDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(AFFIXES.get(id));
    }

    public static List<AffixDefinition> sorted() {
        List<AffixDefinition> out = new ArrayList<>(AFFIXES.values());
        out.sort(Comparator.comparing(d -> d.id().getPath()));
        return out;
    }

    public static List<AffixDefinition> forPolarityAndTemplate(String polarity, String template) {
        List<AffixDefinition> out = new ArrayList<>();
        for (AffixDefinition def : AFFIXES.values()) {
            if (!def.polarity().equals(polarity)) {
                continue;
            }
            if (!def.templates().isEmpty() && !def.templates().contains(template)) {
                continue;
            }
            out.add(def);
        }
        return out;
    }

    private static AffixDefinition parse(JsonObject json, ResourceLocation fileId) {
        ResourceLocation id = json.has("id") ? ResourceLocation.parse(json.get("id").getAsString()) : fileId;
        List<String> templates = new ArrayList<>();
        if (json.has("templates")) {
            JsonArray arr = json.getAsJsonArray("templates");
            for (JsonElement el : arr) {
                templates.add(el.getAsString());
            }
        }
        Map<String, Float> params = new HashMap<>();
        if (json.has("params") && json.get("params").isJsonObject()) {
            JsonObject p = json.getAsJsonObject("params");
            for (Map.Entry<String, JsonElement> e : p.entrySet()) {
                params.put(e.getKey(), e.getValue().getAsFloat());
            }
        }
        return new AffixDefinition(
                id,
                json.has("polarity") ? json.get("polarity").getAsString() : "positive",
                json.has("effect") ? json.get("effect").getAsString() : "none",
                json.has("weight") ? json.get("weight").getAsInt() : 10,
                List.copyOf(templates),
                Map.copyOf(params),
                json.has("max_tier") ? json.get("max_tier").getAsInt() : 1);
    }

    public static Map<ResourceLocation, AffixDefinition> all() {
        return Collections.unmodifiableMap(AFFIXES);
    }
}
