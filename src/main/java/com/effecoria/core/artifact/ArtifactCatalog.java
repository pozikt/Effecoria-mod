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

/** Loads shaft forms, focus cuts, and assemble recipes. */
public final class ArtifactCatalog {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    private static final Map<ResourceLocation, ShaftFormDefinition> SHAFT_FORMS = new HashMap<>();
    private static final Map<ResourceLocation, FocusCutDefinition> FOCUS_CUTS = new HashMap<>();
    private static final Map<ResourceLocation, AssembleRecipeDefinition> ASSEMBLE = new HashMap<>();

    public static final SimpleJsonResourceReloadListener SHAFT_FORMS_LISTENER =
            listener("artifact/shaft_forms", (id, json) -> SHAFT_FORMS.put(id, parseShaft(json, id)));

    public static final SimpleJsonResourceReloadListener FOCUS_CUTS_LISTENER =
            listener("artifact/focus_cuts", (id, json) -> FOCUS_CUTS.put(id, parseFocus(json, id)));

    public static final SimpleJsonResourceReloadListener ASSEMBLE_LISTENER =
            listener("artifact/assemble_recipes", (id, json) -> ASSEMBLE.put(id, parseAssemble(json, id)));

    private ArtifactCatalog() {}

    public static void clearAll() {
        SHAFT_FORMS.clear();
        FOCUS_CUTS.clear();
        ASSEMBLE.clear();
    }

    public static Optional<ShaftFormDefinition> shaftForm(ResourceLocation id) {
        return Optional.ofNullable(SHAFT_FORMS.get(id));
    }

    public static Optional<FocusCutDefinition> focusCut(ResourceLocation id) {
        return Optional.ofNullable(FOCUS_CUTS.get(id));
    }

    public static List<ShaftFormDefinition> shaftForms() {
        List<ShaftFormDefinition> out = new ArrayList<>(SHAFT_FORMS.values());
        out.sort(Comparator.comparing(d -> d.id().getPath()));
        return out;
    }

    public static List<FocusCutDefinition> focusCuts() {
        List<FocusCutDefinition> out = new ArrayList<>(FOCUS_CUTS.values());
        out.sort(Comparator.comparing(d -> d.id().getPath()));
        return out;
    }

    public static List<AssembleRecipeDefinition> assembleRecipes() {
        return List.copyOf(ASSEMBLE.values());
    }

    public static Optional<AssembleRecipeDefinition> assembleFor(String template) {
        return ASSEMBLE.values().stream().filter(r -> r.template().equals(template)).findFirst();
    }

    public static boolean materialMatchesShaft(ItemStack material, ShaftFormDefinition form) {
        return matchesAnyTag(material, form.materialTags());
    }

    public static boolean materialMatchesFocus(ItemStack material, FocusCutDefinition cut) {
        return matchesAnyTag(material, cut.materialTags());
    }

    private static boolean matchesAnyTag(ItemStack stack, List<TagKey<Item>> tags) {
        if (tags.isEmpty()) {
            return true;
        }
        for (TagKey<Item> tag : tags) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static ShaftFormDefinition parseShaft(JsonObject json, ResourceLocation fileId) {
        ResourceLocation id = json.has("id") ? ResourceLocation.parse(json.get("id").getAsString()) : fileId;
        return new ShaftFormDefinition(
                id,
                readTags(json, "material_tags"),
                json.has("reach") ? json.get("reach").getAsFloat() : 1f,
                json.has("cast_cost_mul") ? json.get("cast_cost_mul").getAsFloat() : 1f,
                json.has("durability") ? json.get("durability").getAsInt() : 250,
                json.has("cook_ticks") ? json.get("cook_ticks").getAsInt() : 100);
    }

    private static FocusCutDefinition parseFocus(JsonObject json, ResourceLocation fileId) {
        ResourceLocation id = json.has("id") ? ResourceLocation.parse(json.get("id").getAsString()) : fileId;
        return new FocusCutDefinition(
                id,
                readTags(json, "material_tags"),
                json.has("power") ? json.get("power").getAsFloat() : 1f,
                json.has("focus_tier") ? json.get("focus_tier").getAsInt() : 1,
                json.has("school_bias") ? json.get("school_bias").getAsFloat() : 0f,
                json.has("cook_ticks") ? json.get("cook_ticks").getAsInt() : 100);
    }

    private static AssembleRecipeDefinition parseAssemble(JsonObject json, ResourceLocation fileId) {
        ResourceLocation id = json.has("id") ? ResourceLocation.parse(json.get("id").getAsString()) : fileId;
        return new AssembleRecipeDefinition(
                id,
                json.get("template").getAsString(),
                json.get("input_a").getAsString(),
                json.get("input_b").getAsString(),
                ResourceLocation.parse(json.get("result").getAsString()),
                json.has("cook_ticks") ? json.get("cook_ticks").getAsInt() : 80);
    }

    private static List<TagKey<Item>> readTags(JsonObject json, String key) {
        List<TagKey<Item>> out = new ArrayList<>();
        if (!json.has(key)) {
            return out;
        }
        JsonArray arr = json.getAsJsonArray(key);
        for (JsonElement el : arr) {
            out.add(TagKey.create(Registries.ITEM, ResourceLocation.parse(el.getAsString())));
        }
        return List.copyOf(out);
    }

    private interface Loader {
        void load(ResourceLocation id, JsonObject json);
    }

    private static SimpleJsonResourceReloadListener listener(String path, Loader loader) {
        return new SimpleJsonResourceReloadListener(GSON, path) {
            @Override
            protected void apply(
                    Map<ResourceLocation, JsonElement> jsonMap,
                    ResourceManager resourceManager,
                    ProfilerFiller profiler) {
                if (path.endsWith("shaft_forms")) {
                    SHAFT_FORMS.clear();
                } else if (path.endsWith("focus_cuts")) {
                    FOCUS_CUTS.clear();
                } else {
                    ASSEMBLE.clear();
                }
                for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
                    try {
                        loader.load(entry.getKey(), entry.getValue().getAsJsonObject());
                    } catch (RuntimeException ex) {
                        LOGGER.error("Failed to load {}: {}", entry.getKey(), ex.getMessage());
                    }
                }
                LOGGER.info("Loaded artifact datapack folder {}", path);
            }
        };
    }

    public static Map<ResourceLocation, ShaftFormDefinition> shaftFormMap() {
        return Collections.unmodifiableMap(SHAFT_FORMS);
    }
}
