package com.effecoria.client.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * After shell-death respawn: lids part and the veil lifts, like opening eyes.
 */
public final class ShellWakeHud {
    private static final int OPEN_MS = 2200;

    private static boolean waitingForAlive;
    private static boolean opening;
    private static long openStartMs;

    private ShellWakeHud() {}

    public static void begin() {
        waitingForAlive = true;
        opening = false;
        openStartMs = 0L;
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!waitingForAlive && !opening) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (waitingForAlive) {
            if (minecraft.player.isDeadOrDying()) {
                fillClosed(graphics);
                return;
            }
            waitingForAlive = false;
            opening = true;
            openStartMs = System.currentTimeMillis();
        }
        long elapsed = System.currentTimeMillis() - openStartMs;
        if (elapsed >= OPEN_MS) {
            opening = false;
            return;
        }
        float raw = Mth.clamp(elapsed / (float) OPEN_MS, 0f, 1f);
        // Slow start, faster at the end — lids stick then break.
        float open = raw * raw * (3f - 2f * raw);
        open = open * open;
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        int gap = Math.round(h * open);
        int lid = Math.max(0, (h - gap) / 2);
        int veil = Math.round((1f - open) * 200f) & 0xFF;
        graphics.fill(0, 0, w, h, (veil << 24) | 0x0002050A);
        if (lid > 0) {
            graphics.fill(0, 0, w, lid, 0xFF010308);
            graphics.fill(0, h - lid, w, h, 0xFF010308);
        }
    }

    private static void fillClosed(GuiGraphics graphics) {
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0xFF02050A);
    }
}
