package com.effecoria.core.technomagic;

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

/** Datapack catalog: data/effecoria/technomagic/*.json */
public final class TechnomagicCatalog {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Map<ResourceLocation, TechnomagicNode> NODES = new HashMap<>();

    public static final SimpleJsonResourceReloadListener RELOAD_LISTENER =
            new SimpleJsonResourceReloadListener(GSON, "technomagic") {
                @Override
                protected void apply(
                        Map<ResourceLocation, JsonElement> jsonMap,
                        ResourceManager resourceManager,
                        ProfilerFiller profiler) {
                    NODES.clear();
                    for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
                        try {
                            TechnomagicNode node = parse(entry.getValue().getAsJsonObject(), entry.getKey());
                            NODES.put(node.id(), node);
                        } catch (RuntimeException ex) {
                            LOGGER.error("Failed to load technomagic node {}: {}", entry.getKey(), ex.getMessage());
                        }
                    }
                    LOGGER.info("Loaded {} technomagic nodes", NODES.size());
                }
            };

    private TechnomagicCatalog() {}

    public static Optional<TechnomagicNode> get(ResourceLocation id) {
        return Optional.ofNullable(NODES.get(id));
    }

    public static Map<ResourceLocation, TechnomagicNode> all() {
        return Collections.unmodifiableMap(NODES);
    }

    public static List<TechnomagicNode> byEra(TechnomagicEra era) {
        List<TechnomagicNode> out = new ArrayList<>();
        for (TechnomagicNode node : NODES.values()) {
            if (node.era() == era) {
                out.add(node);
            }
        }
        out.sort(Comparator.comparing(n -> n.id().getPath()));
        return out;
    }

    public static List<TechnomagicNode> sorted() {
        List<TechnomagicNode> out = new ArrayList<>(NODES.values());
        out.sort(Comparator.comparingInt((TechnomagicNode n) -> n.era().number())
                .thenComparing(n -> n.id().getPath()));
        return out;
    }

    public static void replaceAll(Map<ResourceLocation, TechnomagicNode> next) {
        NODES.clear();
        if (next != null) {
            NODES.putAll(next);
        }
    }

    private static TechnomagicNode parse(JsonObject json, ResourceLocation fileId) {
        ResourceLocation id = json.has("id")
                ? ResourceLocation.parse(json.get("id").getAsString())
                : fileId;
        TechnomagicEra era = TechnomagicEra.fromNumber(json.get("era").getAsInt());
        ResourceLocation icon = ResourceLocation.parse(json.get("icon").getAsString());
        TechnomagicNode.TechnomagicStatus status = TechnomagicNode.TechnomagicStatus.fromString(
                json.has("status") ? json.get("status").getAsString() : "available");
        List<ResourceLocation> requires = readIds(json, "requires");
        List<ResourceLocation> unlocks = readIds(json, "display_unlocks");
        return new TechnomagicNode(id, era, icon, status, requires, unlocks);
    }

    private static List<ResourceLocation> readIds(JsonObject json, String key) {
        List<ResourceLocation> out = new ArrayList<>();
        if (!json.has(key)) {
            return out;
        }
        JsonArray array = json.getAsJsonArray(key);
        for (JsonElement el : array) {
            out.add(ResourceLocation.parse(el.getAsString()));
        }
        return List.copyOf(out);
    }
}
