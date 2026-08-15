package com.effecoria.client.gui.alchemy;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Shared technomagic chrome matching {@link TowerConsoleScreen} palette —
 * dark slate panels, cyan Φ accents, no vanilla wood frames.
 */
public final class TowerChrome {
    public static final int BG_OUTER = 0xCC15202C;
    public static final int BG_INNER = 0xEE1E2E3C;
    public static final int BG_BAY = 0xFF162430;
    public static final int BG_INV = 0xFF1A2834;
    public static final int LINE = 0xFF3A5A70;
    public static final int TITLE = 0xFFB8E0FF;
    public static final int LABEL = 0xFFD0D8E0;
    public static final int MUTED = 0xFF8899AA;
    public static final int OK = 0xFF55CC88;
    public static final int WARN = 0xFFE0B040;
    public static final int BAD = 0xFFE05555;
    public static final int IDLE = 0xFF778899;
    public static final int BAR_BG = 0xFF0A1218;
    public static final int ACCENT = 0xFF46BEE6;
    public static final int ACCENT_DIM = 0xFF2A6A8A;
    public static final int OMEGA = 0xFFC44CFF;
    public static final int HEAT = 0xFFFF6644;
    public static final int SLOT_IN = 0xFF243848;
    public static final int SLOT_OUT = 0xFF3A3420;
    public static final int SLOT_FRAME = 0xFF4A6A80;
    public static final int CHIP = 0xFF2A4A5C;
    public static final int CHIP_HOT = 0xFF3A5A70;
    public static final int CHIP_DANGER = 0xFF5C2A2A;

    private TowerChrome() {}

    public static void drawPanel(GuiGraphics g, int left, int top, int width, int height) {
        g.fill(left - 4, top - 4, left + width + 4, top + height + 4, BG_OUTER);
        g.fill(left, top, left + width, top + height, BG_INNER);
        g.fill(left + 1, top + 1, left + width - 1, top + 2, LINE);
        g.fill(left + 1, top + height - 2, left + width - 1, top + height - 1, LINE);
        g.fill(left + 1, top + 1, left + 2, top + height - 1, LINE);
        g.fill(left + width - 2, top + 1, left + width - 1, top + height - 1, LINE);
    }

    /** Standard 176×166 reactor shell: machine bay + player inventory chrome. */
    public static void drawReactorShell(GuiGraphics g, int left, int top) {
        drawPanel(g, left, top, AlchemyGui.WIDTH, AlchemyGui.HEIGHT);
        g.fill(left + 6, top + 16, left + AlchemyGui.WIDTH - 6, top + 78, BG_BAY);
        g.fill(left + 6, top + 80, left + AlchemyGui.WIDTH - 6, top + AlchemyGui.HEIGHT - 6, BG_INV);
        drawPlayerInvSlots(g, left, top, 8, 84);
    }

    public static void drawWideReactorShell(GuiGraphics g, int left, int top, int width) {
        drawPanel(g, left, top, width, AlchemyGui.HEIGHT);
        g.fill(left + 6, top + 16, left + width - 6, top + 78, BG_BAY);
        g.fill(left + 6, top + 80, left + width - 6, top + AlchemyGui.HEIGHT - 6, BG_INV);
    }

    public static void drawPlayerInvSlots(GuiGraphics g, int left, int top, int invX, int invY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(g, left, top, invX + col * 18, invY + row * 18, false);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(g, left, top, invX + col * 18, invY + 58, false);
        }
    }

    public static void drawSlot(GuiGraphics g, int left, int top, int slotX, int slotY, boolean output) {
        int x0 = left + slotX - 1;
        int y0 = top + slotY - 1;
        g.fill(x0, y0, x0 + 18, y0 + 18, SLOT_FRAME);
        g.fill(x0 + 1, y0 + 1, x0 + 17, y0 + 17, output ? SLOT_OUT : SLOT_IN);
        g.fill(x0 + 1, y0 + 1, x0 + 17, y0 + 2, 0x4420A0C0);
    }

    public static void drawGauge(GuiGraphics g, int left, int top, int x, int y, int w, int h, float fill, int color) {
        int x0 = left + x;
        int y0 = top + y;
        g.fill(x0, y0, x0 + w, y0 + h, BAR_BG);
        g.fill(x0, y0, x0 + w, y0 + 1, LINE);
        g.fill(x0, y0 + h - 1, x0 + w, y0 + h, LINE);
        float clamped = Math.max(0f, Math.min(1f, fill));
        int fw = Math.round(w * clamped);
        if (fw > 0) {
            g.fill(x0, y0 + 1, x0 + fw, y0 + h - 1, color);
        }
    }

    public static void drawVGauge(GuiGraphics g, int left, int top, int x, int y, int w, int h, float fill, int color) {
        int x0 = left + x;
        int y0 = top + y;
        g.fill(x0, y0, x0 + w, y0 + h, BAR_BG);
        int fh = Math.round(h * Math.max(0f, Math.min(1f, fill)));
        if (fh > 0) {
            g.fill(x0 + 1, y0 + h - fh, x0 + w - 1, y0 + h, color);
        }
    }

    public static void drawChip(GuiGraphics g, int left, int top, int x, int y, int w, int h, boolean hot, boolean danger) {
        int x0 = left + x;
        int y0 = top + y;
        int fill = danger ? CHIP_DANGER : hot ? CHIP_HOT : CHIP;
        g.fill(x0, y0, x0 + w, y0 + h, fill);
        g.fill(x0, y0, x0 + w, y0 + 1, hot ? ACCENT : LINE);
        g.fill(x0, y0 + h - 1, x0 + w, y0 + h, LINE);
    }

    public static void drawTitle(GuiGraphics g, Font font, Component title, int left, int top) {
        g.drawString(font, title, left + 8, top + 6, TITLE, false);
    }

    public static void drawStatus(GuiGraphics g, Font font, Component text, int left, int top, int color) {
        g.drawString(font, text, left + 8, top + 18, color & 0xFFFFFF, false);
    }

    public static void drawDivider(GuiGraphics g, int left, int top, int x, int y0, int y1) {
        g.fill(left + x, top + y0, left + x + 1, top + y1, LINE);
    }

    /** Hit-target only — chrome chip is painted by the screen. */
    public static Button invisible(int x, int y, int w, int h, Button.OnPress onPress) {
        return new Button(x, y, w, h, Component.empty(), onPress, supplier -> Component.empty()) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
        };
    }
}
