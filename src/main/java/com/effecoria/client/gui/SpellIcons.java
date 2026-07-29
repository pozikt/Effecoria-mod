package com.effecoria.client.gui;

import com.effecoria.EffecoriaMod;

import net.minecraft.resources.ResourceLocation;

public final class SpellIcons {
    public static final int TEXTURE_SIZE = 64;

    private SpellIcons() {}

    /** Sprite id under {@code textures/gui/sprites/spells/}. */
    public static ResourceLocation forSpell(ResourceLocation spellId) {
        return EffecoriaMod.id("spells/" + spellId.getPath());
    }
}
