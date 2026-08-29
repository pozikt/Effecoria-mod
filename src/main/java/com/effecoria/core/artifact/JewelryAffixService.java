package com.effecoria.core.artifact;

import com.effecoria.core.magic.MagicSchool;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Aggregates rolled affixes from all equipped Curios jewelry. */
public final class JewelryAffixService {
    private JewelryAffixService() {}

    public record AffixInstance(AffixDefinition def, int tier, String rollKind) {
        public float param(String key, float fallback) {
            return def.param(key, fallback) * tier;
        }
    }

    public static List<AffixInstance> equippedAffixes(Player player) {
        List<AffixInstance> out = new ArrayList<>();
        for (ItemStack stack : CuriosAccess.allEquipped(player, AssembledGearData::isAssembled)) {
            for (AssembledGearData.AffixEntry entry : AssembledGearData.affixes(stack)) {
                AffixCatalog.get(entry.id()).ifPresent(def -> out.add(new AffixInstance(def, entry.tier(), entry.rollKind())));
            }
        }
        return out;
    }

    public static float sumEffect(Player player, String effect, String paramKey, float perUnit) {
        float total = 0f;
        for (AffixInstance inst : equippedAffixes(player)) {
            if (effect.equals(inst.def().effect())) {
                total += inst.param(paramKey, perUnit);
            }
        }
        return total;
    }

    public static boolean hasEffect(Player player, String effect) {
        for (AffixInstance inst : equippedAffixes(player)) {
            if (effect.equals(inst.def().effect())) {
                return true;
            }
        }
        return false;
    }

    public static float phiShieldBonus(Player player) {
        return sumEffect(player, "phi_shield", "per_tier", 0.08f);
    }

    public static float castCostMul(Player player) {
        float mul = 1f;
        mul *= 1f - sumEffect(player, "cast_efficiency", "per_tier", 0.06f);
        mul *= 1f + sumEffect(player, "instability", "per_tier", 0.05f);
        if (hasEffect(player, "user_fed")) {
            mul *= 0.85f;
        }
        return Math.max(0.5f, mul);
    }

    public static float powerMul(Player player, MagicSchool school) {
        float mul = 1f;
        if (school == MagicSchool.ELEMENTAL) {
            mul += sumEffect(player, "elemental_boost", "per_tier", 0.1f);
        }
        return mul;
    }

    public static float physicalDamageReduction(Player player) {
        return Math.min(0.45f, sumEffect(player, "physical_ward", "per_tier", 0.12f));
    }

    public static float autonomousShieldBonus(Player player) {
        return hasEffect(player, "autonomous") ? 0.2f : 0f;
    }
}
