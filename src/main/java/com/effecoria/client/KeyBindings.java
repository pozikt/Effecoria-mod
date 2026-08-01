package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    private KeyBindings() {}

    public static final KeyMapping CAST_SPELL = new KeyMapping(
            "key.effecoria.cast_spell",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.effecoria");

    public static final KeyMapping OPEN_SPELL_BOOK = new KeyMapping(
            "key.effecoria.open_spell_book",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            "key.categories.effecoria");

    /** Hold this and scroll the mouse wheel to cycle the selected spell without opening the hub. */
    public static final KeyMapping CYCLE_SPELL_MODIFIER = new KeyMapping(
            "key.effecoria.cycle_spell_modifier",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            "key.categories.effecoria");

    /** Open seal word-programming editor while looking at a block (Seals school). */
    public static final KeyMapping OPEN_SEAL_EDITOR = new KeyMapping(
            "key.effecoria.open_seal_editor",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.effecoria");
}
