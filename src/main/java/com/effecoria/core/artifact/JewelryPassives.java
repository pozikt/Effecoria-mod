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
        best = Math.max(best, bonusIfEquipped(player, ModItems.ASSEMBLED_RING.get()));
        best = Math.max(best, bonusIfEquipped(player, ModItems.ASSEMBLED_AMULET.get()));
        best = Math.max(best, bonusIfEquipped(player, ModItems.ASSEMBLED_CHARM.get()));
        return best;
    }

    private static float bonusIfEquipped(Player player, net.minecraft.world.item.Item item) {
        return CuriosAccess.findEquipped(player, stack -> stack.is(item))
                .map(JewelryPassives::shieldFromStack)
                .orElse(0f);
    }

    public static float shieldFromStack(ItemStack stack) {
        if (stack.getItem() instanceof JewelryItem jewelry) {
            return jewelry.phiShieldBonus();
        }
        return 0f;
    }
}
