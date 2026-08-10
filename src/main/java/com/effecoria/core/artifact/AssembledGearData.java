package com.effecoria.core.artifact;

import com.effecoria.armor.EssonitePhoneme;
import com.effecoria.content.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** CustomData for assembled staffs / jewelry. */
public final class AssembledGearData {
    public static final String ROOT = "effecoria_gear";
    public static final String TEMPLATE = "template";
    public static final String SHAFT = "shaft";
    public static final String FOCUS = "focus";
    public static final String BAND = "band";
    public static final String GEM = "gem";
    public static final String SEALS = "seals";
    public static final String SEAL_ID = "id";
    public static final String SEAL_LVL = "lvl";

    public static final String TEMPLATE_STAFF = "staff";
    public static final String TEMPLATE_RING = "ring";
    public static final String TEMPLATE_AMULET = "amulet";
    public static final String TEMPLATE_CHARM = "charm";

    private AssembledGearData() {}

    public static boolean isAssembled(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(ModItems.MODULAR_STAFF.get())
                        || stack.is(ModItems.ASSEMBLED_RING.get())
                        || stack.is(ModItems.ASSEMBLED_AMULET.get())
                        || stack.is(ModItems.ASSEMBLED_CHARM.get())
                        || gearTag(stack).isPresent());
    }

    public static boolean isStaff(ItemStack stack) {
        return stack.is(ModItems.MODULAR_STAFF.get()) && TEMPLATE_STAFF.equals(template(stack));
    }

    public static String template(ItemStack stack) {
        return gearTag(stack).map(t -> t.getString(TEMPLATE)).orElse("");
    }

    public static Optional<CompoundTag> shaftPart(ItemStack stack) {
        return gearTag(stack).filter(t -> t.contains(SHAFT)).map(t -> t.getCompound(SHAFT));
    }

    public static Optional<CompoundTag> focusPart(ItemStack stack) {
        return gearTag(stack).filter(t -> t.contains(FOCUS)).map(t -> t.getCompound(FOCUS));
    }

    public static Map<ResourceLocation, Integer> seals(ItemStack stack) {
        Map<ResourceLocation, Integer> out = new LinkedHashMap<>();
        gearTag(stack).ifPresent(tag -> {
            ListTag list = tag.getList(SEALS, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                if (entry.contains(SEAL_ID)) {
                    out.put(ResourceLocation.parse(entry.getString(SEAL_ID)), Math.max(1, entry.getInt(SEAL_LVL)));
                }
            }
        });
        return out;
    }

    public static int sealLevel(ItemStack stack, ResourceLocation sealId) {
        return seals(stack).getOrDefault(sealId, 0);
    }

    public static boolean hasSeal(ItemStack stack, ResourceLocation sealId) {
        return sealLevel(stack, sealId) > 0;
    }

    public static List<EssonitePhoneme> allPhonemes(ItemStack stack) {
        List<EssonitePhoneme> out = new ArrayList<>();
        for (String key : List.of(SHAFT, FOCUS, BAND, GEM)) {
            gearTag(stack).filter(t -> t.contains(key)).ifPresent(t -> {
                CompoundTag part = t.getCompound(key);
                ListTag list = part.getList(ModularPartData.PHONEMES, Tag.TAG_STRING);
                for (int i = 0; i < list.size(); i++) {
                    EssonitePhoneme.fromId(list.getString(i)).ifPresent(p -> {
                        if (!out.contains(p)) {
                            out.add(p);
                        }
                    });
                }
            });
        }
        return out;
    }

    public static boolean hasPhoneme(ItemStack stack, EssonitePhoneme phoneme) {
        return allPhonemes(stack).contains(phoneme);
    }

    public static void setSeals(ItemStack stack, Map<ResourceLocation, Integer> seals) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag gear = root.contains(ROOT) ? root.getCompound(ROOT) : new CompoundTag();
            if (!gear.contains(TEMPLATE)) {
                gear.putString(TEMPLATE, "inscribed");
            }
            ListTag list = new ListTag();
            seals.forEach((id, lvl) -> {
                CompoundTag entry = new CompoundTag();
                entry.putString(SEAL_ID, id.toString());
                entry.putInt(SEAL_LVL, Math.max(1, lvl));
                list.add(entry);
            });
            gear.put(SEALS, list);
            root.put(ROOT, gear);
        });
    }

    public static void putSeal(ItemStack stack, ResourceLocation sealId, int level) {
        Map<ResourceLocation, Integer> map = new LinkedHashMap<>(seals(stack));
        map.put(sealId, Math.max(1, level));
        setSeals(stack, map);
    }

    public static void clearSeals(ItemStack stack) {
        setSeals(stack, Map.of());
    }

    public static ItemStack assembleStaff(ItemStack shaft, ItemStack focus) {
        ItemStack out = new ItemStack(ModItems.MODULAR_STAFF.get());
        writeGear(out, TEMPLATE_STAFF, Map.of(
                SHAFT, ModularPartData.copyPartTag(shaft),
                FOCUS, ModularPartData.copyPartTag(focus)));
        return out;
    }

    public static ItemStack assembleJewelry(
            String template, net.minecraft.world.item.Item item, ItemStack band, ItemStack gem) {
        ItemStack out = new ItemStack(item);
        writeGear(out, template, Map.of(
                BAND, ModularPartData.copyPartTag(band),
                GEM, ModularPartData.copyPartTag(gem)));
        return out;
    }

    private static void writeGear(ItemStack stack, String template, Map<String, CompoundTag> parts) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag gear = new CompoundTag();
            gear.putString(TEMPLATE, template);
            parts.forEach(gear::put);
            gear.put(SEALS, new ListTag());
            root.put(ROOT, gear);
        });
    }

    private static Optional<CompoundTag> gearTag(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag root = data.copyTag();
        if (!root.contains(ROOT)) {
            return Optional.empty();
        }
        return Optional.of(root.getCompound(ROOT));
    }
}
