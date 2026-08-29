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
    public static final String AFFIXES = "affixes";
    public static final String SEAL_ID = "id";
    public static final String SEAL_LVL = "lvl";
    public static final String AFFIX_ID = "id";
    public static final String AFFIX_TIER = "tier";
    public static final String AFFIX_ROLL = "roll";
    public static final String CONDUCTIVITY = "conductivity";
    public static final String LENGTH_M = "length_m";

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

    public static boolean hasGearConductivity(ItemStack stack) {
        return gearTag(stack).filter(t -> t.contains(CONDUCTIVITY)).isPresent();
    }

    public static float conductivity(ItemStack stack) {
        return gearTag(stack)
                .filter(t -> t.contains(CONDUCTIVITY))
                .map(t -> t.getFloat(CONDUCTIVITY))
                .orElseGet(() -> computeConductivityFromParts(stack));
    }

    public static float lengthMeters(ItemStack stack) {
        return gearTag(stack)
                .filter(t -> t.contains(LENGTH_M))
                .map(t -> t.getFloat(LENGTH_M))
                .orElseGet(() -> shaftPart(stack)
                        .filter(t -> t.contains(ModularPartData.LENGTH_M))
                        .map(t -> t.getFloat(ModularPartData.LENGTH_M))
                        .orElse(0f));
    }

    private static float computeConductivityFromParts(ItemStack stack) {
        float sum = 0f;
        float weight = 0f;
        for (String key : List.of(SHAFT, FOCUS, BAND, GEM)) {
            Optional<CompoundTag> part = gearTag(stack).filter(t -> t.contains(key)).map(t -> t.getCompound(key));
            if (part.isEmpty()) {
                continue;
            }
            CompoundTag tag = part.get();
            float w = SHAFT.equals(key) || BAND.equals(key) ? 0.55f : 0.45f;
            float c = tag.contains(ModularPartData.CONDUCTIVITY)
                    ? tag.getFloat(ModularPartData.CONDUCTIVITY)
                    : MaterialConductivity.DEFAULT;
            if (tag.contains(ModularPartData.MATERIAL) && !tag.contains(ModularPartData.CONDUCTIVITY)) {
                c = MaterialConductivity.ofItemId(ResourceLocation.parse(tag.getString(ModularPartData.MATERIAL)));
            }
            sum += c * w;
            weight += w;
        }
        return weight <= 0f ? MaterialConductivity.DEFAULT : sum / weight;
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

    public static int sealCapacity(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (isStaff(stack)) {
            return Math.max(3, StaffStats.of(stack).sealCapacity());
        }
        int ward = sealLevel(stack, ResourceLocation.fromNamespaceAndPath("effecoria", "ward_bind"));
        return 3 + Math.max(0, ward);
    }

    public static boolean hasInscribedSeals(ItemStack stack) {
        return !seals(stack).isEmpty();
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

    public record AffixEntry(ResourceLocation id, int tier, String rollKind) {}

    public static List<AffixEntry> affixes(ItemStack stack) {
        List<AffixEntry> out = new ArrayList<>();
        gearTag(stack).ifPresent(tag -> {
            ListTag list = tag.getList(AFFIXES, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                if (!entry.contains(AFFIX_ID)) {
                    continue;
                }
                ResourceLocation id = ResourceLocation.parse(entry.getString(AFFIX_ID));
                int tier = Math.max(1, entry.getInt(AFFIX_TIER));
                String roll = entry.contains(AFFIX_ROLL) ? entry.getString(AFFIX_ROLL) : "standard";
                out.add(new AffixEntry(id, tier, roll));
            }
        });
        return out;
    }

    public static boolean hasAffix(ItemStack stack, ResourceLocation affixId) {
        for (AffixEntry entry : affixes(stack)) {
            if (entry.id().equals(affixId)) {
                return true;
            }
        }
        return false;
    }

    public static int affixTier(ItemStack stack, ResourceLocation affixId) {
        for (AffixEntry entry : affixes(stack)) {
            if (entry.id().equals(affixId)) {
                return entry.tier();
            }
        }
        return 0;
    }

    public static void setAffixes(ItemStack stack, List<AffixEntry> affixes) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag gear = root.contains(ROOT) ? root.getCompound(ROOT) : new CompoundTag();
            ListTag list = new ListTag();
            for (AffixEntry entry : affixes) {
                CompoundTag tag = new CompoundTag();
                tag.putString(AFFIX_ID, entry.id().toString());
                tag.putInt(AFFIX_TIER, Math.max(1, entry.tier()));
                tag.putString(AFFIX_ROLL, entry.rollKind());
                list.add(tag);
            }
            gear.put(AFFIXES, list);
            root.put(ROOT, gear);
        });
    }

    public static ItemStack assembleStaff(ItemStack shaft, ItemStack focus) {
        ItemStack out = new ItemStack(ModItems.MODULAR_STAFF.get());
        writeGear(out, TEMPLATE_STAFF, Map.of(
                SHAFT, ModularPartData.copyPartTag(shaft),
                FOCUS, ModularPartData.copyPartTag(focus)));
        stampMergedStats(out, shaft, focus, true);
        ShaftVisuals.applyStaffModelFromShaft(out, shaft);
        return out;
    }

    public static ItemStack assembleJewelry(
            String template,
            net.minecraft.world.item.Item item,
            ItemStack band,
            ItemStack gem,
            net.minecraft.util.RandomSource random) {
        ItemStack out = new ItemStack(item);
        writeGear(out, template, Map.of(
                BAND, ModularPartData.copyPartTag(band),
                GEM, ModularPartData.copyPartTag(gem)));
        stampMergedStats(out, band, gem, false);
        AffixRollService.rollOnAssemble(out, template, band, gem, random);
        return out;
    }

    private static void stampMergedStats(ItemStack out, ItemStack a, ItemStack b, boolean staff) {
        float ca = MaterialConductivity.ofStack(a);
        float cb = MaterialConductivity.ofStack(b);
        float conductivity = staff ? ca * 0.55f + cb * 0.45f : ca * 0.5f + cb * 0.5f;
        float length = staff ? ModularPartData.lengthMeters(a) : 0f;
        CustomData.update(DataComponents.CUSTOM_DATA, out, root -> {
            CompoundTag gear = root.contains(ROOT) ? root.getCompound(ROOT) : new CompoundTag();
            gear.putFloat(CONDUCTIVITY, conductivity);
            if (length > 0f) {
                gear.putFloat(LENGTH_M, length);
            }
            root.put(ROOT, gear);
        });
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
