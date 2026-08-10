package com.effecoria.core.artifact;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/** Custom model data indices for shaft length profiles (item model overrides). */
public final class ShaftVisuals {
    private ShaftVisuals() {}

    public static float modelDataForForm(ResourceLocation formId) {
        if (formId == null) {
            return 0f;
        }
        return switch (formId.getPath()) {
            case "wand" -> 1f;
            case "baton" -> 2f;
            case "long_staff" -> 3f;
            case "stature" -> 4f;
            default -> 0f;
        };
    }

    public static void applyShaftModel(ItemStack stack, ResourceLocation formId) {
        float token = modelDataForForm(formId);
        if (token <= 0f) {
            return;
        }
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData((int) token));
    }

    public static void applyStaffModelFromShaft(ItemStack staff, ItemStack shaft) {
        if (!ModularPartData.isShaft(shaft)) {
            return;
        }
        applyShaftModel(staff, ModularPartData.formOrCut(shaft));
    }
}
