package com.effecoria.core.artifact;

import com.effecoria.armor.EssonitePhoneme;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Rolls bonus affixes when jewelry is assembled. */
public final class AffixRollService {
    private AffixRollService() {}

    public static void rollOnAssemble(
            ItemStack out, String template, ItemStack band, ItemStack gem, RandomSource random) {
        List<AssembledGearData.AffixEntry> affixes = new ArrayList<>();
        affixes.addAll(phonemeAffixes(out));
        Optional<AssembledGearData.AffixEntry> bonus = rollBonusAffix(template, band, gem, random);
        bonus.ifPresent(affixes::add);
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

    private static Optional<AssembledGearData.AffixEntry> rollBonusAffix(
            String template, ItemStack band, ItemStack gem, RandomSource random) {
        int roll = random.nextInt(100);
        String polarity;
        String rollKind;
        if (roll < 70) {
            return Optional.empty();
        } else if (roll < 90) {
            polarity = "positive";
            rollKind = "positive";
        } else if (roll < 97) {
            polarity = "negative";
            rollKind = "negative";
        } else {
            polarity = "incredible";
            rollKind = "incredible";
        }
        float conductivity = (MaterialConductivity.ofStack(band) + MaterialConductivity.ofStack(gem)) * 0.5f;
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
}
