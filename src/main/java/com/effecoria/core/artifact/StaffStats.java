package com.effecoria.core.artifact;

import com.effecoria.armor.EssonitePhoneme;
import com.effecoria.EffecoriaMod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Merged cast/combat stats from a held modular staff. */
public final class StaffStats {
    public record Bundle(
            float castCostMul,
            float powerMul,
            float reach,
            float lengthMeters,
            float conductivity,
            int focusTier,
            int sealCapacity) {
        public static final Bundle NONE = new Bundle(1f, 1f, 1f, 0f, MaterialConductivity.DEFAULT, 0, 1);
    }

    private StaffStats() {}

    public static Bundle ofHeld(Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (AssembledGearData.isStaff(main)) {
            return of(main);
        }
        if (AssembledGearData.isStaff(off)) {
            return of(off);
        }
        return Bundle.NONE;
    }

    public static Bundle of(ItemStack staff) {
        if (!AssembledGearData.isStaff(staff)) {
            return Bundle.NONE;
        }
        float cost = 1f;
        float power = 1f;
        float reach = 1f;
        float length = AssembledGearData.lengthMeters(staff);
        float conductivity = AssembledGearData.conductivity(staff);
        int tier = 0;
        CompoundTag shaft = AssembledGearData.shaftPart(staff).orElse(null);
        CompoundTag focus = AssembledGearData.focusPart(staff).orElse(null);
        if (shaft != null && shaft.contains(ModularPartData.FORM_OR_CUT)) {
            ResourceLocation formId = ResourceLocation.parse(shaft.getString(ModularPartData.FORM_OR_CUT));
            ShaftFormDefinition form = ArtifactCatalog.shaftForm(formId).orElse(null);
            if (form != null) {
                cost *= form.castCostMul();
                reach = form.reach();
                if (length <= 0f) {
                    length = form.lengthMeters();
                }
            }
            if (shaft.contains(ModularPartData.CONDUCTIVITY)) {
                // shaft dominates conduction path
                conductivity = shaft.getFloat(ModularPartData.CONDUCTIVITY) * 0.55f
                        + (focus != null && focus.contains(ModularPartData.CONDUCTIVITY)
                                ? focus.getFloat(ModularPartData.CONDUCTIVITY)
                                : MaterialConductivity.DEFAULT)
                                * 0.45f;
            }
        }
        if (focus != null && focus.contains(ModularPartData.FORM_OR_CUT)) {
            ResourceLocation cutId = ResourceLocation.parse(focus.getString(ModularPartData.FORM_OR_CUT));
            FocusCutDefinition cut = ArtifactCatalog.focusCut(cutId).orElse(null);
            if (cut != null) {
                power *= cut.power();
                tier = cut.focusTier();
            }
        }
        // High conductivity: cheaper casts, stronger delivery. Low: lossy.
        float cond = Mth.clamp(conductivity, 0f, 1f);
        cost *= Mth.lerp(cond, 1.18f, 0.78f);
        power *= Mth.lerp(cond, 0.88f, 1.22f);
        if (AssembledGearData.hasPhoneme(staff, EssonitePhoneme.FIRMITAS)) {
            cost *= 0.95f;
        }
        if (AssembledGearData.hasPhoneme(staff, EssonitePhoneme.SERVARE)) {
            cost *= 0.97f;
        }
        int ward = AssembledGearData.sealLevel(
                staff, ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "ward_bind"));
        int attune = AssembledGearData.sealLevel(
                staff, ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "phi_attune"));
        if (attune > 0) {
            cost *= Math.max(0.7f, 1f - 0.05f * attune);
            power *= 1f + 0.03f * attune;
        }
        int capacity = 1 + Math.max(0, tier) + ward;
        return new Bundle(cost, power, reach, length, cond, tier, capacity);
    }
}
