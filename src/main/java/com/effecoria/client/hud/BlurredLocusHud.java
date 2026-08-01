package com.effecoria.client.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Center-screen blurred structure locus from mental Locus Echo. */
public final class BlurredLocusHud {
    private static final int FADE_TICKS = 30;

    private static int x;
    private static int y;
    private static int z;
    private static long expireAtMs;
    private static long startAtMs;
    private static int durationTicks;
    private static boolean active;

    private BlurredLocusHud() {}

    public static void show(int blurX, int blurY, int blurZ, int displayTicks) {
        x = blurX;
        y = blurY;
        z = blurZ;
        durationTicks = Math.max(20, displayTicks);
        startAtMs = System.currentTimeMillis();
        expireAtMs = startAtMs + durationTicks * 50L;
        active = true;
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!active) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now >= expireAtMs) {
            active = false;
            return;
        }

        long totalMs = Math.max(1L, expireAtMs - startAtMs);
        long remainingMs = expireAtMs - now;
        float fadeWindowMs = FADE_TICKS * 50f;
        float alpha;
        if (remainingMs <= fadeWindowMs) {
            alpha = Mth.clamp(remainingMs / fadeWindowMs, 0f, 1f);
        } else {
            alpha = 1f;
        }
        if (alpha <= 0.02f) {
            return;
        }

        int a = Math.round(alpha * 255f) & 0xFF;
        int titleColor = (a << 24) | 0x00C8B0FF;
        int coordsColor = (a << 24) | 0x00E8E0FF;
        int shadowColor = (Math.max(1, a / 2) << 24);

        Component title = Component.translatable("hud.effecoria.locus_echo");
        Component coords = Component.translatable("hud.effecoria.locus_echo.coords", x, y, z);

        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();
        int titleW = minecraft.font.width(title);
        int coordsW = minecraft.font.width(coords);
        int cx = screenW / 2;
        int cy = screenH / 2 - 12;

        graphics.drawString(minecraft.font, title, cx - titleW / 2 + 1, cy + 1, shadowColor, false);
        graphics.drawString(minecraft.font, title, cx - titleW / 2, cy, titleColor, false);
        graphics.drawString(minecraft.font, coords, cx - coordsW / 2 + 1, cy + 12, shadowColor, false);
        graphics.drawString(minecraft.font, coords, cx - coordsW / 2, cy + 11, coordsColor, false);
    }
}
