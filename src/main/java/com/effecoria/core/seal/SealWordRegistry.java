package com.effecoria.core.seal;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Datapack lexicon under {@code data/<namespace>/seal_words/*.json}. */
public final class SealWordRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Map<ResourceLocation, SealWordDefinition> WORDS = new HashMap<>();

    public static final SimpleJsonResourceReloadListener RELOAD_LISTENER =
            new SimpleJsonResourceReloadListener(GSON, "seal_words") {
                @Override
                protected void apply(
                        Map<ResourceLocation, JsonElement> jsonMap,
                        ResourceManager resourceManager,
                        ProfilerFiller profiler) {
                    WORDS.clear();
                    for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
                        try {
                            SealWordDefinition word = parse(entry.getValue().getAsJsonObject());
                            WORDS.put(word.id(), word);
                        } catch (RuntimeException ex) {
                            LOGGER.error("Failed to load seal word {}: {}", entry.getKey(), ex.getMessage());
                        }
                    }
                    LOGGER.info("Loaded {} Effecoria seal words", WORDS.size());
                }
            };

    private SealWordRegistry() {}

    public static Optional<SealWordDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(WORDS.get(id));
    }

    public static boolean contains(ResourceLocation id) {
        return WORDS.containsKey(id);
    }

    public static Map<ResourceLocation, SealWordDefinition> all() {
        return Collections.unmodifiableMap(WORDS);
    }

    public static List<SealWordDefinition> starters() {
        List<SealWordDefinition> out = new ArrayList<>();
        for (SealWordDefinition word : WORDS.values()) {
            if (word.starter()) {
                out.add(word);
            }
        }
        out.sort(Comparator.comparing(w -> w.id().toString()));
        return out;
    }

    public static List<SealWordDefinition> progressionOrder() {
        List<SealWordDefinition> out = new ArrayList<>(WORDS.values());
        out.sort(Comparator
                .comparingDouble(SealWordDefinition::minMastery)
                .thenComparing(w -> w.id().toString()));
        return out;
    }

    private static SealWordDefinition parse(JsonObject json) {
        ResourceLocation id = ResourceLocation.parse(json.get("id").getAsString());
        SealWordKind kind = SealWordKind.fromSerialized(json.get("kind").getAsString());
        String effect = json.has("effect") ? json.get("effect").getAsString() : "";
        float numberValue = json.has("value") ? json.get("value").getAsFloat() : 0f;
        ResourceLocation sound = json.has("sound")
                ? ResourceLocation.parse(json.get("sound").getAsString())
                : null;
        float psiCost = json.has("psi_cost") ? json.get("psi_cost").getAsFloat() : 2f;
        float minMastery = json.has("min_mastery") ? json.get("min_mastery").getAsFloat() : 0f;
        boolean starter = json.has("starter") && json.get("starter").getAsBoolean();
        return new SealWordDefinition(id, kind, effect, numberValue, sound, psiCost, minMastery, starter);
    }
}
