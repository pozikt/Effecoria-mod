package com.effecoria.core.technomagic;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

/** One node in the technomagic tree (catalog entry). */
public record TechnomagicNode(
        ResourceLocation id,
        TechnomagicEra era,
        ResourceLocation icon,
        TechnomagicStatus status,
        List<ResourceLocation> requires,
        List<ResourceLocation> displayUnlocks) {
    public enum TechnomagicStatus {
        AVAILABLE,
        PLANNED;

        public static TechnomagicStatus fromString(String raw) {
            return "planned".equalsIgnoreCase(raw) ? PLANNED : AVAILABLE;
        }
    }

    public String translationKey() {
        return "technomagic.effecoria.node." + id.getPath();
    }

    public String descKey() {
        return translationKey() + ".desc";
    }
}
