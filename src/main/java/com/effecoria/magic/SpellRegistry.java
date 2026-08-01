package com.effecoria.magic;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.magic.RadialCategory;
import com.effecoria.core.magic.SpellDefinition;
import com.effecoria.core.magic.SpellEffectEntry;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SpellRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Map<ResourceLocation, SpellDefinition> SPELLS = new HashMap<>();

    public static final SimpleJsonResourceReloadListener RELOAD_LISTENER = new SimpleJsonResourceReloadListener(GSON, "spells") {
        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profiler) {
            SPELLS.clear();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
                try {
                    SpellDefinition spell = parse(entry.getValue().getAsJsonObject());
                    SPELLS.put(spell.id(), spell);
                } catch (RuntimeException ex) {
                    LOGGER.error("Failed to load spell {}: {}", entry.getKey(), ex.getMessage());
                }
            }
            LOGGER.info("Loaded {} Effecoria spells", SPELLS.size());
        }
    };

    private SpellRegistry() {}

    public static Optional<SpellDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(SPELLS.get(id));
    }

    public static boolean contains(ResourceLocation id) {
        return SPELLS.containsKey(id);
    }

    public static Map<ResourceLocation, SpellDefinition> all() {
        return Collections.unmodifiableMap(SPELLS);
    }

    /** Replace the in-memory catalog (used when syncing datapack spells to remote clients). */
    public static void replaceAll(Map<ResourceLocation, SpellDefinition> next) {
        SPELLS.clear();
        if (next != null && !next.isEmpty()) {
            SPELLS.putAll(next);
        }
        LOGGER.info("Spell catalog now has {} entries", SPELLS.size());
    }

    public static List<SpellDefinition> snapshot() {
        return List.copyOf(SPELLS.values());
    }

    public static void syncTo(net.minecraft.server.level.ServerPlayer player) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player, new com.effecoria.network.ModNetworking.SpellCatalogPayload(snapshot()));
    }

    public static void syncToAll(net.minecraft.server.MinecraftServer server) {
        for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncTo(player);
        }
    }

    private static SpellDefinition parse(JsonObject json) {
        ResourceLocation id = ResourceLocation.parse(json.get("id").getAsString());
        MagicSchool school = MagicSchool.fromSerializedName(json.get("school").getAsString());
        float frequency = json.get("frequency_hz").getAsFloat();
        float baseCost = json.get("base_cost").getAsFloat();
        float powerMultiplier = json.has("power_multiplier") ? json.get("power_multiplier").getAsFloat() : 1f;
        float sideEntropy = json.has("side_entropy") ? json.get("side_entropy").getAsFloat() : 0.05f;
        float minPhi = json.has("min_phi") ? json.get("min_phi").getAsFloat() : 0.1f;
        float minMastery = json.has("min_mastery") ? json.get("min_mastery").getAsFloat() : 0f;
        float minPower = json.has("min_power") ? json.get("min_power").getAsFloat() : 0f;
        int unlockEssenceCost = json.has("unlock_essence_cost") ? json.get("unlock_essence_cost").getAsInt() : -1;
        if (!json.has("radial_category")) {
            throw new IllegalArgumentException("Missing radial_category for " + id);
        }
        RadialCategory radialCategory = RadialCategory.fromSerializedName(json.get("radial_category").getAsString());

        List<SpellEffectEntry> effects = new ArrayList<>();
        if (json.has("effects")) {
            JsonArray array = json.getAsJsonArray("effects");
            for (JsonElement element : array) {
                JsonObject effectJson = element.getAsJsonObject();
                ResourceLocation type = ResourceLocation.parse(effectJson.get("type").getAsString());
                JsonObject params = effectJson.deepCopy();
                params.remove("type");
                effects.add(new SpellEffectEntry(type, params));
            }
        }

        return new SpellDefinition(
                id, school, frequency, baseCost, powerMultiplier, sideEntropy, minPhi, minMastery, minPower, unlockEssenceCost, radialCategory, effects);
    }
}
