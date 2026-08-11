package com.effecoria.client.gui.alchemy;

import com.effecoria.EffecoriaMod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared Φ-alchemy GUI helpers.
 * <p>
 * Vanilla contract ({@code AbstractContainerScreen}): panel is 176×166 drawn from a
 * <b>256×256</b> texture via {@code blit(tex, x, y, 0, 0, 176, 166)}. Slot menu coords are the
 * top-left of the 16×16 item icon; painted frames sit at (x-1, y-1).
 */
public final class AlchemyGui {
    public static final int WIDTH = 176;
    public static final int HEIGHT = 166;
    /** Must match GuiGraphics default blit atlas size / our PNG size. */
    public static final int TEXTURE_SIZE = 256;

    public static final int PLAYER_INV_Y = 84;

    public static final ResourceLocation MORTAR = EffecoriaMod.id("textures/gui/mortar.png");
    public static final ResourceLocation BURNER = EffecoriaMod.id("textures/gui/burner.png");
    public static final ResourceLocation ALEMBIC = EffecoriaMod.id("textures/gui/alembic.png");
    public static final ResourceLocation SPARK_REACTOR = EffecoriaMod.id("textures/gui/spark_reactor.png");
    public static final ResourceLocation FORGE_REACTOR = EffecoriaMod.id("textures/gui/forge_reactor.png");

    private AlchemyGui() {}

    public static void blitPanel(GuiGraphics graphics, ResourceLocation texture, int left, int top) {
        blitPanel(graphics, texture, left, top, WIDTH, HEIGHT);
    }

    public static void blitPanel(
            GuiGraphics graphics, ResourceLocation texture, int left, int top, int width, int height) {
        // Explicit texture size — never rely on a non-256 source texture with the short blit overload.
        graphics.blit(texture, left, top, 0, 0, width, height, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    /** Fill the hollow arrow drawn at (arrowX, arrowY) in the panel. */
    public static void progressArrow(GuiGraphics graphics, int left, int top, int arrowX, int arrowY, float ratio, int argb) {
        int w = Math.max(0, Math.min(29, Math.round(29 * Math.max(0f, Math.min(1f, ratio)))));
        if (w <= 0) {
            return;
        }
        int x0 = left + arrowX;
        int y0 = top + arrowY;
        int body = Math.min(w, 22);
        graphics.fill(x0, y0 + 4, x0 + body, y0 + 11, argb);
        if (w > 22) {
            graphics.fill(x0 + 22, y0 + 2, x0 + w, y0 + 13, argb);
        }
    }

    public static void heatGauge(GuiGraphics graphics, int left, int top, int gx, int gy, float fill, int argb) {
        int maxH = 42;
        int h = Math.max(0, Math.min(maxH, Math.round(maxH * Math.max(0f, Math.min(1f, fill)))));
        if (h <= 0) {
            return;
        }
        int x0 = left + gx + 1;
        int y1 = top + gy + maxH;
        graphics.fill(x0, y1 - h, x0 + 7, y1, argb);
    }
}
