package com.effecoria.content;

import com.effecoria.armor.EssonitePhoneme;
import com.effecoria.core.artifact.AssembledGearData;
import com.effecoria.core.artifact.StaffStats;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Assembled modular staff. */
public class ModularStaffItem extends Item {
    public ModularStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.effecoria.modular_staff.hint"));
        if (AssembledGearData.isStaff(stack)) {
            StaffStats.Bundle stats = StaffStats.of(stack);
            if (stats.lengthMeters() > 0f) {
                tooltip.add(Component.translatable(
                        "item.effecoria.modular_staff.length", String.format("%.1f", stats.lengthMeters())));
            }
            tooltip.add(Component.translatable(
                    "item.effecoria.modular_staff.conductivity",
                    String.format("%.0f%%", stats.conductivity() * 100f)));
            tooltip.add(Component.translatable(
                    "item.effecoria.modular_staff.stats",
                    String.format("%.2f", stats.castCostMul()),
                    String.format("%.2f", stats.powerMul()),
                    stats.sealCapacity()));
            for (EssonitePhoneme phoneme : AssembledGearData.allPhonemes(stack)) {
                tooltip.add(Component.translatable("item.effecoria.essonite_armor.phoneme." + phoneme.id()));
            }
            AssembledGearData.seals(stack).forEach((id, lvl) -> {
                String name = Component.translatable("item_seal.effecoria." + id.getPath()).getString();
                tooltip.add(Component.literal(name + " " + toRoman(lvl)));
            });
        }
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }
}
