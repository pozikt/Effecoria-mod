package com.effecoria.core.artifact;

import com.effecoria.armor.EssonitePhoneme;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Rolls bonus affixes when jewelry is assembled. */
public final class AffixRollService {
    private AffixRollService() {}

    public static void rollOnAssemble(
            ItemStack out, String template, ItemStack band, ItemStack gem, RandomSource random) {
        List<AssembledGearData.AffixEntry> affixes = new ArrayList<>();
        affixes.addAll(phonemeAffixes(out));
        affixes.addAll(materialAffixes(band, gem, random));
        dedupeAffixes(affixes);
        Optional<AssembledGearData.AffixEntry> bonus = rollBonusAffix(template, band, gem, random);
        bonus.ifPresent(entry -> addIfAbsent(affixes, entry));
        if (!affixes.isEmpty()) {
            AssembledGearData.setAffixes(out, affixes);
        }
    }

    private static List<AssembledGearData.AffixEntry> phonemeAffixes(ItemStack out) {
        List<AssembledGearData.AffixEntry> outList = new ArrayList<>();
        for (EssonitePhoneme phoneme : AssembledGearData.allPhonemes(out)) {
            ResourceLocation id = switch (phoneme) {
                case FIRMITAS -> ResourceLocation.fromNamespaceAndPath("effecoria", "physical_ward");
                case UMBRA -> ResourceLocation.fromNamespaceAndPath("effecoria", "cast_efficiency");
                case ABNEGATIO -> ResourceLocation.fromNamespaceAndPath("effecoria", "mental_ward");
                case SERVARE -> ResourceLocation.fromNamespaceAndPath("effecoria", "phi_regen");
                case CLAUSURA -> ResourceLocation.fromNamespaceAndPath("effecoria", "phi_shield");
            };
            if (AffixCatalog.get(id).isPresent()) {
                outList.add(new AssembledGearData.AffixEntry(id, 1, "phoneme"));
            }
        }
        return outList;
    }

    private static List<AssembledGearData.AffixEntry> materialAffixes(
            ItemStack band, ItemStack gem, RandomSource random) {
        List<AssembledGearData.AffixEntry> out = new ArrayList<>();
        for (ItemStack part : List.of(band, gem)) {
            if (part.isEmpty()) {
                continue;
            }
            MaterialDefinition def = MaterialConductivity.resolveStack(part);
            for (MaterialDefinition.ImplicitAffix implicit : def.implicitAffixes()) {
                if (AffixCatalog.get(implicit.affixId()).isEmpty()) {
                    continue;
                }
                if (random.nextFloat() <= implicit.chance()) {
                    out.add(new AssembledGearData.AffixEntry(implicit.affixId(), implicit.tier(), "material"));
                }
            }
        }
        return out;
    }

    private static Optional<AssembledGearData.AffixEntry> rollBonusAffix(
            String template, ItemStack band, ItemStack gem, RandomSource random) {
        MaterialDefinition bandDef = MaterialConductivity.resolveStack(band);
        MaterialDefinition gemDef = MaterialConductivity.resolveStack(gem);
        float posBias = (bandDef.positiveBias() + gemDef.positiveBias()) * 0.5f;
        float negBias = (bandDef.negativeBias() + gemDef.negativeBias()) * 0.5f;
        float conductivity = (bandDef.conductivity() + gemDef.conductivity()) * 0.5f;

        int roll = random.nextInt(100);
        int standardCap = Mth.clamp(Math.round(70f - posBias * 18f + negBias * 10f), 45, 82);
        int positiveCap = Mth.clamp(Math.round(90f - posBias * 8f + negBias * 6f), standardCap + 5, 94);
        int negativeCap = Mth.clamp(Math.round(97f + negBias * 4f - posBias * 6f), positiveCap + 2, 99);

        String polarity;
        String rollKind;
        if (roll < standardCap) {
            return Optional.empty();
        } else if (roll < positiveCap) {
            polarity = "positive";
            rollKind = "positive";
        } else if (roll < negativeCap) {
            polarity = "negative";
            rollKind = "negative";
        } else {
            polarity = "incredible";
            rollKind = "incredible";
        }
        return pickWeighted(polarity, template, conductivity, random)
                .map(id -> new AssembledGearData.AffixEntry(id, tierForRoll(rollKind, random), rollKind));
    }

    private static int tierForRoll(String rollKind, RandomSource random) {
        return switch (rollKind) {
            case "incredible" -> 1;
            case "negative" -> random.nextBoolean() ? 1 : 2;
            default -> random.nextInt(2) + 1;
        };
    }

    private static Optional<ResourceLocation> pickWeighted(
            String polarity, String template, float conductivity, RandomSource random) {
        List<AffixDefinition> pool = AffixCatalog.forPolarityAndTemplate(polarity, template);
        if (pool.isEmpty()) {
            return Optional.empty();
        }
        int total = 0;
        for (AffixDefinition def : pool) {
            total += adjustedWeight(def, conductivity, polarity);
        }
        if (total <= 0) {
            return Optional.empty();
        }
        int pick = random.nextInt(total);
        int acc = 0;
        for (AffixDefinition def : pool) {
            acc += adjustedWeight(def, conductivity, polarity);
            if (pick < acc) {
                return Optional.of(def.id());
            }
        }
        return Optional.of(pool.getLast().id());
    }

    private static int adjustedWeight(AffixDefinition def, float conductivity, String polarity) {
        int w = Math.max(1, def.weight());
        if ("negative".equals(polarity) && conductivity < 0.35f) {
            w = (int) (w * 1.35f);
        }
        if ("positive".equals(polarity) && conductivity > 0.75f) {
            w = (int) (w * 1.25f);
        }
        return Math.max(1, w);
    }

    private static void dedupeAffixes(List<AssembledGearData.AffixEntry> affixes) {
        Set<ResourceLocation> seen = new LinkedHashSet<>();
        affixes.removeIf(entry -> !seen.add(entry.id()));
    }

    private static void addIfAbsent(List<AssembledGearData.AffixEntry> affixes, AssembledGearData.AffixEntry entry) {
        if (affixes.stream().noneMatch(a -> a.id().equals(entry.id()))) {
            affixes.add(entry);
        }
    }
}
