package com.effecoria.client.gui;

import com.effecoria.EffecoriaMod;

import net.minecraft.resources.ResourceLocation;

public final class SpellIcons {
    public static final int TEXTURE_SIZE = 64;

    private SpellIcons() {}

    public static ResourceLocation forSpell(ResourceLocation spellId) {
        return EffecoriaMod.id("gui/spells/" + spellId.getPath());
    }
}
