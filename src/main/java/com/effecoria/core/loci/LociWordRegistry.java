package com.effecoria.core.loci;

import com.effecoria.core.seal.SealWordKind;
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

/** Datapack lexicon under {@code data/<namespace>/loci_words/*.json}. Separate from seal words. */
public final class LociWordRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Map<ResourceLocation, LociWordDefinition> WORDS = new HashMap<>();

    public static final SimpleJsonResourceReloadListener RELOAD_LISTENER =
            new SimpleJsonResourceReloadListener(GSON, "loci_words") {
                @Override
                protected void apply(
                        Map<ResourceLocation, JsonElement> jsonMap,
                        ResourceManager resourceManager,
                        ProfilerFiller profiler) {
                    WORDS.clear();
                    for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
                        try {
                            LociWordDefinition word = parse(entry.getValue().getAsJsonObject());
                            WORDS.put(word.id(), word);
                        } catch (RuntimeException ex) {
                            LOGGER.error("Failed to load loci word {}: {}", entry.getKey(), ex.getMessage());
                        }
                    }
                    LOGGER.info("Loaded {} Effecoria loci words", WORDS.size());
                }
            };

    private LociWordRegistry() {}

    public static Optional<LociWordDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(WORDS.get(id));
    }

    public static Map<ResourceLocation, LociWordDefinition> all() {
        return Collections.unmodifiableMap(WORDS);
    }

    public static void replaceAll(Map<ResourceLocation, LociWordDefinition> next) {
        WORDS.clear();
        if (next != null && !next.isEmpty()) {
            WORDS.putAll(next);
        }
        LOGGER.info("Loci word lexicon now has {} entries", WORDS.size());
    }

    public static List<LociWordDefinition> snapshot() {
        List<LociWordDefinition> out = new ArrayList<>(WORDS.values());
        out.sort(Comparator.comparing(w -> w.id().toString()));
        return out;
    }

    public static void syncTo(net.minecraft.server.level.ServerPlayer player) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player, new com.effecoria.network.ModNetworking.LociWordCatalogPayload(snapshot()));
    }

    public static void syncToAll(net.minecraft.server.MinecraftServer server) {
        for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncTo(player);
        }
    }

    private static LociWordDefinition parse(JsonObject json) {
        ResourceLocation id = ResourceLocation.parse(json.get("id").getAsString());
        SealWordKind kind = SealWordKind.fromSerialized(json.get("kind").getAsString());
        String effect = json.has("effect") ? json.get("effect").getAsString() : "";
        return new LociWordDefinition(id, kind, effect);
    }
}
