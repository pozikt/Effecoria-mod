package com.effecoria.client.gui;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.network.ModNetworking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Breathing technique timing drill — click when the marker is in the green zone.
 * Every click shrinks the green zone; successes grant regen bonus; three misses apply fatigue.
 */
public class BreathingTrainScreen extends Screen {
    private static final int BAR_W = 140;
    private static final int BAR_H = 18;

    private float marker;
    private float direction = 1f;
    private float greenCenter = 0.5f;
    private float greenHalf;
    private int hits;
    private int misses;
    private int missLimit;
    /** Lifetime clicks this session (hits + misses) — drives green shrink. */
    private int clicks;
    private boolean fatigued;
    private long fatigueUntilMs;
    private String flash = "";
    private int flashTicks;
    private boolean lastHitSuccess;

    public BreathingTrainScreen() {
        super(Component.translatable("gui.effecoria.breath_train"));
    }

    @Override
    protected void init() {
        refreshFromPlayer();
        randomizeGreen();
        marker = 0.1f;
        direction = 1f;
    }

    private void refreshFromPlayer() {
        missLimit = Math.max(1, BalanceConfig.BREATHING_TRAIN_MISS_LIMIT.get());
        if (minecraft == null || minecraft.player == null) {
            hits = 0;
            misses = 0;
            fatigued = false;
            greenHalf = greenHalfWidth(clicks);
            return;
        }
        PlayerPsiData data = minecraft.player.getData(ModAttachments.PSI.get());
        hits = Math.max(hits, data.breathTrainHits());
        // Keep optimistic local miss count until the server catches up (or fatigue clears the streak).
        int syncedMisses = data.breathTrainSessionMisses();
        if (data.isBreathTrainFatigued()) {
            misses = 0;
            fatigued = true;
            fatigueUntilMs = System.currentTimeMillis() + data.breathTrainFatigueRemainingMs();
        } else {
            misses = Math.max(misses, syncedMisses);
            fatigued = false;
        }
        greenHalf = greenHalfWidth(clicks);
    }

    /** Green half-width shrinks with every click (success or miss). */
    public static float greenHalfWidth(int clicks) {
        float full = 0.28f;
        float min = 0.055f;
        float shrunk = full - clicks * 0.016f;
        return Mth.clamp(shrunk, min, full) * 0.5f;
    }

    private void randomizeGreen() {
        float half = greenHalf;
        float roll = 0.5f;
        if (this.minecraft != null && this.minecraft.level != null) {
            roll = this.minecraft.level.random.nextFloat();
        }
        greenCenter = Mth.clamp(0.15f + roll * 0.7f, half + 0.02f, 1f - half - 0.02f);
    }

    private float markerSpeed() {
        // Constant on-screen speed (px/tick); shorter bar → less time to cross.
        float pxPerTick = 3.6f + Math.min(5.5f, hits * 0.22f);
        return pxPerTick / BAR_W;
    }

    @Override
    public void tick() {
        refreshFromPlayer();
        if (flashTicks > 0) {
            flashTicks--;
        }
        if (fatigued) {
            return;
        }
        marker += direction * markerSpeed();
        if (marker >= 1f) {
            marker = 1f;
            direction = -1f;
        } else if (marker <= 0f) {
            marker = 0f;
            direction = 1f;
        }
    }

    private boolean inGreen() {
        return Math.abs(marker - greenCenter) <= greenHalf;
    }

    private void attemptHit() {
        if (fatigued) {
            flash = Component.translatable("gui.effecoria.breath_train.fatigued").getString();
            flashTicks = 40;
            lastHitSuccess = false;
            return;
        }
        boolean success = inGreen();
        lastHitSuccess = success;
        clicks++;
        greenHalf = greenHalfWidth(clicks);
        if (success) {
            PacketDistributor.sendToServer(new ModNetworking.BreathTrainHitPayload());
            flash = Component.translatable("gui.effecoria.breath_train.success").getString();
            flashTicks = 30;
            hits++;
            randomizeGreen();
        } else {
            PacketDistributor.sendToServer(new ModNetworking.BreathTrainMissPayload());
            misses++;
            randomizeGreen();
            if (misses >= missLimit) {
                flash = Component.translatable("gui.effecoria.breath_train.miss_fatigue").getString();
                flashTicks = 40;
                fatigued = true;
                fatigueUntilMs = System.currentTimeMillis()
                        + BalanceConfig.BREATHING_TRAIN_FATIGUE_MS.get();
                misses = 0;
            } else {
                flash = Component.translatable(
                                "gui.effecoria.breath_train.miss",
                                misses,
                                missLimit)
                        .getString();
                flashTicks = 25;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            attemptHit();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 32 || keyCode == 257) { // space / enter
            attemptHit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xAA000810);

        int cx = this.width / 2;
        int cy = this.height / 2;
        graphics.drawCenteredString(this.font, this.title, cx, cy - 70, 0xE8F0FF);
        graphics.drawCenteredString(
                this.font,
                fatigued
                        ? Component.translatable("gui.effecoria.breath_train.hint_fatigued")
                        : Component.translatable("gui.effecoria.breath_train.hint"),
                cx,
                cy - 56,
                fatigued ? 0xFFAA8888 : 0x99AABBCC);

        int barLeft = cx - BAR_W / 2;
        int barTop = cy - BAR_H / 2;

        // Red track
        graphics.fill(barLeft - 1, barTop - 1, barLeft + BAR_W + 1, barTop + BAR_H + 1, 0xFF705050);
        graphics.fill(barLeft, barTop, barLeft + BAR_W, barTop + BAR_H, 0xFF8B2020);

        // Green zone
        int gLeft = barLeft + Math.round((greenCenter - greenHalf) * BAR_W);
        int gRight = barLeft + Math.round((greenCenter + greenHalf) * BAR_W);
        graphics.fill(gLeft, barTop, gRight, barTop + BAR_H, 0xFF2EAA4A);

        // Marker stick
        int mx = barLeft + Math.round(marker * BAR_W);
        graphics.fill(mx - 2, barTop - 4, mx + 2, barTop + BAR_H + 4, 0xFFF5F5FF);

        if (fatigued) {
            graphics.fill(barLeft, barTop, barLeft + BAR_W, barTop + BAR_H, 0x88000000);
        }

        float bonusPct = hits * BalanceConfig.BREATHING_TRAIN_REGEN_BONUS.get().floatValue() * 100f;
        graphics.drawCenteredString(
                this.font,
                Component.translatable(
                        "gui.effecoria.breath_train.stats",
                        hits,
                        String.format("%.1f", bonusPct),
                        misses,
                        missLimit),
                cx,
                cy + 28,
                0xCCE8F0FF);

        if (fatigued) {
            long rem = Math.max(0L, fatigueUntilMs - System.currentTimeMillis());
            int sec = (int) Math.ceil(rem / 1000.0);
            int regenPct = (int) (BalanceConfig.BREATHING_TRAIN_FATIGUE_REGEN_MULT.get().floatValue() * 100f);
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("gui.effecoria.breath_train.cooldown_detail", sec, regenPct),
                    cx,
                    cy + 42,
                    0xFFCC8888);
        }

        if (flashTicks > 0 && !flash.isEmpty()) {
            graphics.drawCenteredString(this.font, flash, cx, cy + 58, lastHitSuccess ? 0xFF88FFAA : 0xFFFF8888);
        }

        graphics.drawCenteredString(
                this.font,
                Component.translatable("gui.effecoria.breath_train.close"),
                cx,
                this.height - 28,
                0x889999AA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
