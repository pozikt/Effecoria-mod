package com.effecoria.client.gui.alchemy;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Procedural chrome for the Artifact Assembler — Φ-bonding bay, no vanilla stonecutter frame. */
public final class AssemblerGui {
    public static final int WIDTH = AlchemyGui.WIDTH;
    public static final int HEIGHT = AlchemyGui.HEIGHT;

    public static final int TEMPLATE_COUNT = 4;
    public static final int CHIP_Y = 22;
    public static final int CHIP_H = 14;
    public static final int CHIP_W = 40;
    public static final int CHIP_GAP = 2;
    public static final int CHIP_X0 = 6;

    /** Must match {@link com.effecoria.alchemy.menu.AssemblerMenu} slot origins. */
    public static final int SLOT_A_X = 44;
    public static final int SLOT_A_Y = 35;
    public static final int SLOT_B_X = 80;
    public static final int SLOT_B_Y = 35;
    public static final int SLOT_OUT_X = 134;
    public static final int SLOT_OUT_Y = 35;

    private static final int BOND_CX = 107;
    private static final int BOND_CY = 43;
    private static final int BOND_R = 13;

    private AssemblerGui() {}

    public static void drawShell(GuiGraphics graphics, int left, int top) {
        TowerChrome.drawReactorShell(graphics, left, top);
        int bayLeft = left + 6;
        int bayTop = top + 28;
        int bayRight = left + WIDTH - 6;
        int bayBottom = top + 76;
        graphics.fill(bayLeft, bayTop, bayRight, bayBottom, TowerChrome.BG_BAY);
        graphics.fill(bayLeft, bayTop, bayRight, bayTop + 1, TowerChrome.LINE);
        graphics.fill(bayLeft, bayBottom - 1, bayRight, bayBottom, TowerChrome.LINE);
        drawBondChamber(graphics, left, top);
        TowerChrome.drawSlot(graphics, left, top, SLOT_A_X, SLOT_A_Y, false);
        TowerChrome.drawSlot(graphics, left, top, SLOT_B_X, SLOT_B_Y, false);
        TowerChrome.drawSlot(graphics, left, top, SLOT_OUT_X, SLOT_OUT_Y, true);
    }

    public static void drawTemplateTabs(
            GuiGraphics graphics, Font font, int left, int top, int selected, int mouseX, int mouseY) {
        for (int i = 0; i < TEMPLATE_COUNT; i++) {
            int x = chipX(i);
            boolean hot = templateHit(left, top, i, mouseX, mouseY);
            boolean sel = i == selected;
            TowerChrome.drawChip(graphics, left, top, x, CHIP_Y, CHIP_W, CHIP_H, sel || hot, false);
            if (sel) {
                int x0 = left + x;
                int y0 = top + CHIP_Y;
                graphics.fill(x0, y0 + CHIP_H - 2, x0 + CHIP_W, y0 + CHIP_H, TowerChrome.ACCENT);
            }
            String label = switch (i) {
                case 0 -> "S";
                case 1 -> "R";
                case 2 -> "A";
                default -> "C";
            };
            graphics.drawString(font, label, left + x + 4, top + CHIP_Y + 3, sel ? TowerChrome.TITLE : TowerChrome.MUTED, false);
            int iconX = left + x + CHIP_W - 12;
            int iconY = top + CHIP_Y + 1;
            graphics.fill(iconX, iconY, iconX + 10, iconY + 10, sel ? TowerChrome.ACCENT_DIM : TowerChrome.SLOT_IN);
            graphics.fill(iconX, iconY, iconX + 10, iconY + 1, TowerChrome.LINE);
        }
    }

    public static void drawBondProgress(GuiGraphics graphics, int left, int top, float ratio, long gameTime) {
        float clamped = Mth.clamp(ratio, 0f, 1f);
        int cx = left + BOND_CX;
        int cy = top + BOND_CY;
        float pulse = 0.65f + 0.35f * Mth.sin(gameTime * 0.18f);
        int ring = blend(TowerChrome.ACCENT_DIM, TowerChrome.ACCENT, clamped * pulse);
        drawRing(graphics, cx, cy, BOND_R, ring);
        if (clamped > 0.01f) {
            drawArc(graphics, cx, cy, BOND_R - 2, -90f, -90f + 360f * clamped, TowerChrome.ACCENT);
        }
        drawFeedLine(graphics, left + SLOT_A_X + 8, top + SLOT_A_Y + 8, cx - BOND_R, cy, clamped, false);
        drawFeedLine(graphics, left + SLOT_B_X + 8, top + SLOT_B_Y + 8, cx - 2, cy + BOND_R - 2, clamped, false);
        drawFeedLine(graphics, cx + BOND_R, cy, left + SLOT_OUT_X + 2, top + SLOT_OUT_Y + 8, clamped, true);
        if (clamped > 0f) {
            TowerChrome.drawGauge(graphics, left, top, 52, 62, 72, 5, clamped, TowerChrome.ACCENT);
        }
    }

    public static boolean templateHit(int left, int top, int index, double mouseX, double mouseY) {
        int x = left + chipX(index);
        int y = top + CHIP_Y;
        return mouseX >= x && mouseX < x + CHIP_W && mouseY >= y && mouseY < y + CHIP_H;
    }

    public static int chipX(int index) {
        return CHIP_X0 + index * (CHIP_W + CHIP_GAP);
    }

    private static void drawBondChamber(GuiGraphics graphics, int left, int top) {
        int cx = left + BOND_CX;
        int cy = top + BOND_CY;
        drawRing(graphics, cx, cy, BOND_R + 4, TowerChrome.LINE);
        graphics.fill(cx - 1, cy - BOND_R - 6, cx + 1, cy - BOND_R - 2, TowerChrome.ACCENT_DIM);
        graphics.fill(cx - 1, cy + BOND_R + 2, cx + 1, cy + BOND_R + 6, TowerChrome.ACCENT_DIM);
    }

    private static void drawFeedLine(
            GuiGraphics graphics, int x0, int y0, int x1, int y1, float fill, boolean toOutput) {
        int color = toOutput ? TowerChrome.WARN : TowerChrome.ACCENT_DIM;
        if (fill <= 0f) {
            graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
            return;
        }
        int mx = Mth.floor(Mth.lerp(fill, x0, x1));
        int my = Mth.floor(Mth.lerp(fill, y0, y1));
        graphics.fill(Math.min(x0, mx), Math.min(y0, my), Math.max(x0, mx) + 1, Math.max(y0, my) + 1, color);
    }

    private static void drawRing(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int d2 = dx * dx + dy * dy;
                if (d2 >= (radius - 1) * (radius - 1) && d2 <= radius * radius) {
                    graphics.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
                }
            }
        }
    }

    private static void drawArc(
            GuiGraphics graphics, int cx, int cy, int radius, float startDeg, float endDeg, int color) {
        float step = 6f;
        for (float a = startDeg; a <= endDeg; a += step) {
            float rad = (float) Math.toRadians(a);
            int x = cx + Math.round(Mth.cos(rad) * radius);
            int y = cy + Math.round(Mth.sin(rad) * radius);
            graphics.fill(x, y, x + 2, y + 2, color);
        }
    }

    private static int blend(int from, int to, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int fa = (from >> 24) & 0xFF;
        int fr = (from >> 16) & 0xFF;
        int fg = (from >> 8) & 0xFF;
        int fb = from & 0xFF;
        int ta = (to >> 24) & 0xFF;
        int tr = (to >> 16) & 0xFF;
        int tg = (to >> 8) & 0xFF;
        int tb = to & 0xFF;
        int a = (int) Mth.lerp(t, fa, ta);
        int r = (int) Mth.lerp(t, fr, tr);
        int g = (int) Mth.lerp(t, fg, tg);
        int b = (int) Mth.lerp(t, fb, tb);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static Component templateName(int template) {
        return switch (template) {
            case 0 -> Component.translatable("gui.effecoria.assembler.staff");
            case 1 -> Component.translatable("gui.effecoria.assembler.ring");
            case 2 -> Component.translatable("gui.effecoria.assembler.amulet");
            default -> Component.translatable("gui.effecoria.assembler.charm");
        };
    }
}
