package com.effecoria.core.artifact;

import com.effecoria.content.JewelryItem;
import com.effecoria.content.ModItems;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Max Φ-shield bonus from equipped Curios jewelry. */
public final class JewelryPassives {
    private JewelryPassives() {}

    public static float maxPhiShield(Player player) {
        float best = 0f;
        best = Math.max(best, bonusIfEquipped(player, ModItems.GOLD_AMULET.get()));
        best = Math.max(best, bonusIfEquipped(player, ModItems.STAR_AMULET.get()));
        best = Math.max(best, bonusIfEquipped(player, ModItems.ESSONITE_RING.get()));
        best = Math.max(best, bonusIfEquipped(player, ModItems.PHI_BAND.get()));
        best = Math.max(best, bonusIfEquipped(player, ModItems.LEAD_CHARM.get()));
        for (ItemStack stack : CuriosAccess.allEquipped(player, AssembledGearData::isAssembled)) {
            best = Math.max(best, effectiveShield(stack));
        }
        best = Math.max(best, JewelryAffixService.autonomousShieldBonus(player));
        return best;
    }

    private static float bonusIfEquipped(Player player, net.minecraft.world.item.Item item) {
        return CuriosAccess.findEquipped(player, stack -> stack.is(item))
                .map(JewelryPassives::effectiveShield)
                .orElse(0f);
    }

    /** Base jewelry shield scaled by stamped material conductivity and affixes. */
    public static float effectiveShield(ItemStack stack) {
        float base = shieldFromStack(stack);
        if (base <= 0f) {
            return 0f;
        }
        float c = MaterialConductivity.ofStack(stack);
        float shield = base * net.minecraft.util.Mth.lerp(c, 0.7f, 1.1f);
        float affixBonus = 0f;
        for (AssembledGearData.AffixEntry entry : AssembledGearData.affixes(stack)) {
            affixBonus += AffixCatalog.get(entry.id())
                    .filter(def -> "phi_shield".equals(def.effect()))
                    .map(def -> def.param("per_tier", 0.08f) * entry.tier())
                    .orElse(0f);
            affixBonus += AffixCatalog.get(entry.id())
                    .filter(def -> "autonomous".equals(def.effect()))
                    .map(def -> 0.2f)
                    .orElse(0f);
        }
        return Math.min(0.95f, shield + affixBonus);
    }

    public static float shieldFromStack(ItemStack stack) {
        if (stack.getItem() instanceof JewelryItem jewelry) {
            return jewelry.phiShieldBonus();
        }
        return 0f;
    }
}
