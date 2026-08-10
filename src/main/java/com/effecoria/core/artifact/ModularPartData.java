package com.effecoria.core.artifact;

import com.effecoria.armor.EssonitePhoneme;
import com.effecoria.content.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CustomData for carved shafts / faceted foci. */
public final class ModularPartData {
    public static final String ROOT = "effecoria_part";
    public static final String KIND = "kind";
    public static final String MATERIAL = "material";
    public static final String FORM_OR_CUT = "form_or_cut";
    public static final String PHONEMES = "phonemes";
    public static final String CONDUCTIVITY = "conductivity";
    public static final String LENGTH_M = "length_m";

    public static final String KIND_SHAFT = "shaft";
    public static final String KIND_FOCUS = "focus";
    public static final String KIND_BAND = "band";
    public static final String KIND_GEM = "gem";

    private ModularPartData() {}

    public static boolean isPart(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(ModItems.CARVED_SHAFT.get())
                        || stack.is(ModItems.FACETED_FOCUS.get())
                        || stack.is(ModItems.JEWELRY_BAND.get())
                        || stack.is(ModItems.JEWELRY_GEM.get()));
    }

    public static boolean isShaft(ItemStack stack) {
        return stack.is(ModItems.CARVED_SHAFT.get()) && KIND_SHAFT.equals(kind(stack));
    }

    public static boolean isFocus(ItemStack stack) {
        return stack.is(ModItems.FACETED_FOCUS.get()) && KIND_FOCUS.equals(kind(stack));
    }

    public static boolean isBand(ItemStack stack) {
        return stack.is(ModItems.JEWELRY_BAND.get());
    }

    public static boolean isGem(ItemStack stack) {
        return stack.is(ModItems.JEWELRY_GEM.get());
    }

    public static String kind(ItemStack stack) {
        return partTag(stack).map(t -> t.getString(KIND)).orElse("");
    }

    public static ResourceLocation material(ItemStack stack) {
        return partTag(stack)
                .filter(t -> t.contains(MATERIAL))
                .map(t -> ResourceLocation.parse(t.getString(MATERIAL)))
                .orElse(ResourceLocation.withDefaultNamespace("air"));
    }

    public static ResourceLocation formOrCut(ItemStack stack) {
        return partTag(stack)
                .filter(t -> t.contains(FORM_OR_CUT))
                .map(t -> ResourceLocation.parse(t.getString(FORM_OR_CUT)))
                .orElse(ResourceLocation.fromNamespaceAndPath("effecoria", "unknown"));
    }

    /** Stamped conductivity, or -1 if missing (caller may fall back to material table). */
    public static float conductivity(ItemStack stack) {
        return partTag(stack)
                .filter(t -> t.contains(CONDUCTIVITY))
                .map(t -> t.getFloat(CONDUCTIVITY))
                .orElse(-1f);
    }

    public static float lengthMeters(ItemStack stack) {
        return partTag(stack)
                .filter(t -> t.contains(LENGTH_M))
                .map(t -> t.getFloat(LENGTH_M))
                .orElse(0f);
    }

    public static void setConductivity(ItemStack stack, float value) {
        if (!isPart(stack)) {
            return;
        }
        float c = net.minecraft.util.Mth.clamp(value, 0f, 1f);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag part = root.contains(ROOT) ? root.getCompound(ROOT) : new CompoundTag();
            part.putFloat(CONDUCTIVITY, c);
            root.put(ROOT, part);
        });
    }

    public static void setLengthMeters(ItemStack stack, float meters) {
        if (!isPart(stack)) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag part = root.contains(ROOT) ? root.getCompound(ROOT) : new CompoundTag();
            part.putFloat(LENGTH_M, Math.max(0.1f, meters));
            root.put(ROOT, part);
        });
    }

    public static List<EssonitePhoneme> phonemes(ItemStack stack) {
        List<EssonitePhoneme> out = new ArrayList<>();
        partTag(stack).ifPresent(tag -> {
            ListTag list = tag.getList(PHONEMES, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                EssonitePhoneme.fromId(list.getString(i)).ifPresent(out::add);
            }
        });
        return out;
    }

    public static boolean hasPhoneme(ItemStack stack, EssonitePhoneme phoneme) {
        return phonemes(stack).contains(phoneme);
    }

    public static void addPhoneme(ItemStack stack, EssonitePhoneme phoneme) {
        if (!isPart(stack)) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag part = root.contains(ROOT) ? root.getCompound(ROOT) : new CompoundTag();
            ListTag list = part.contains(PHONEMES) ? part.getList(PHONEMES, Tag.TAG_STRING) : new ListTag();
            String id = phoneme.id();
            for (int i = 0; i < list.size(); i++) {
                if (id.equals(list.getString(i))) {
                    part.put(PHONEMES, list);
                    root.put(ROOT, part);
                    return;
                }
            }
            list.add(StringTag.valueOf(id));
            part.put(PHONEMES, list);
            root.put(ROOT, part);
        });
    }

    public static ItemStack createShaft(Item material, ResourceLocation formId, float lengthMeters) {
        ItemStack stack = new ItemStack(ModItems.CARVED_SHAFT.get());
        writePart(stack, KIND_SHAFT, BuiltInRegistries.ITEM.getKey(material), formId);
        setConductivity(stack, MaterialConductivity.ofItem(material));
        setLengthMeters(stack, lengthMeters);
        return stack;
    }

    public static ItemStack createFocus(Item material, ResourceLocation cutId) {
        ItemStack stack = new ItemStack(ModItems.FACETED_FOCUS.get());
        writePart(stack, KIND_FOCUS, BuiltInRegistries.ITEM.getKey(material), cutId);
        setConductivity(stack, MaterialConductivity.ofItem(material));
        return stack;
    }

    public static ItemStack createBand(Item material) {
        ItemStack stack = new ItemStack(ModItems.JEWELRY_BAND.get());
        writePart(stack, KIND_BAND, BuiltInRegistries.ITEM.getKey(material),
                ResourceLocation.fromNamespaceAndPath("effecoria", "plain_band"));
        setConductivity(stack, MaterialConductivity.ofItem(material));
        return stack;
    }

    public static ItemStack createGem(Item material) {
        ItemStack stack = new ItemStack(ModItems.JEWELRY_GEM.get());
        writePart(stack, KIND_GEM, BuiltInRegistries.ITEM.getKey(material),
                ResourceLocation.fromNamespaceAndPath("effecoria", "plain_gem"));
        setConductivity(stack, MaterialConductivity.ofItem(material));
        return stack;
    }

    public static CompoundTag copyPartTag(ItemStack stack) {
        return partTag(stack).map(CompoundTag::copy).orElse(new CompoundTag());
    }

    private static void writePart(ItemStack stack, String kind, ResourceLocation material, ResourceLocation form) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag part = new CompoundTag();
            part.putString(KIND, kind);
            part.putString(MATERIAL, material.toString());
            part.putString(FORM_OR_CUT, form.toString());
            part.put(PHONEMES, new ListTag());
            root.put(ROOT, part);
        });
    }

    private static Optional<CompoundTag> partTag(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag root = data.copyTag();
        if (!root.contains(ROOT)) {
            return Optional.empty();
        }
        return Optional.of(root.getCompound(ROOT));
    }
}
