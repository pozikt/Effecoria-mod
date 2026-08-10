package com.effecoria.client.gui.alchemy;

import net.minecraft.client.gui.GuiGraphics;

/** Vanilla stonecutter recipe grid (4×3 visible cells). */
public final class StonecutterRecipeGrid {
    public static final int COLS = 4;
    public static final int ROWS = 3;
    public static final int CELL = 16;
    public static final int ORIGIN_X = 52;
    public static final int ORIGIN_Y = 14;

    /** Selected recipe slot overlay in {@code stonecutter.png}. */
    public static final int SELECT_U = 176;
    public static final int SELECT_V = 0;

    private StonecutterRecipeGrid() {}

    public static int cellX(int left, int index) {
        return left + ORIGIN_X + (index % COLS) * CELL;
    }

    public static int cellY(int top, int index) {
        return top + ORIGIN_Y + (index / COLS) * CELL;
    }

    public static boolean hit(int left, int top, int index, double mouseX, double mouseY) {
        int x = cellX(left, index);
        int y = cellY(top, index);
        return mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
    }

    public static void blitSelected(GuiGraphics graphics, int left, int top, int index) {
        int x = cellX(left, index);
        int y = cellY(top, index);
        graphics.blit(
                ArtifactStationGui.STONECUTTER,
                x,
                y,
                SELECT_U,
                SELECT_V,
                CELL,
                CELL,
                ArtifactStationGui.TEXTURE_SIZE,
                ArtifactStationGui.TEXTURE_SIZE);
    }
}
