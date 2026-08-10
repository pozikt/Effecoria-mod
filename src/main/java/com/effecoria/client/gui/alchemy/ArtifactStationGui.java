package com.effecoria.client.gui.alchemy;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Stonecutter-style layout for shaft lathe / facet cutter (vanilla slot coords). */
public final class ArtifactStationGui {
    public static final int WIDTH = 176;
    public static final int HEIGHT = 166;
    public static final int TEXTURE_SIZE = 256;

    public static final ResourceLocation STONECUTTER =
            ResourceLocation.withDefaultNamespace("textures/gui/container/stonecutter.png");

    public static final int SLOT_INPUT_X = 20;
    public static final int SLOT_INPUT_Y = 33;
    public static final int SLOT_OUTPUT_X = 143;
    public static final int SLOT_OUTPUT_Y = 33;

    public static final int ARROW_X = 61;
    public static final int ARROW_Y = 35;

    public static final int SCROLL_LEFT_X = 57;
    public static final int SCROLL_RIGHT_X = 119;
    public static final int SCROLL_Y = 14;
    public static final int SCROLL_W = 12;
    public static final int SCROLL_H = 15;

    private ArtifactStationGui() {}

    public static void blitStonecutter(GuiGraphics graphics, int left, int top) {
        graphics.blit(STONECUTTER, left, top, 0, 0, WIDTH, HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
    }
}
