package com.effecoria.client.gui;

import com.effecoria.client.hud.ShellWakeHud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * Replaces vanilla {@code DeathScreen}: eclipse, flattening shell-pulse, protocol lines,
 * then auto-respawn into {@link ShellWakeHud} eyelid fade.
 */
public final class ShellDeathScreen extends Screen {
    private static final int FADE_IN = 12;
    private static final int FLATLINE_AT = 38;
    private static final int FIRST_LINE_AT = 42;
    private static final int LINE_STAGGER = 18;
    private static final int HOLD_AFTER = 28;
    private static final int LINE_COUNT = 4;
    private static final int PULSE_COLOR = 0xFF7EC8E3;
    private static final int LINE_COLOR = 0xFFB8E0FF;

    private int ticks;
    private boolean respawnSent;

    public ShellDeathScreen() {
        super(Component.translatable("death.effecoria.shell.title"));
    }

    @Override
    protected void init() {
        super.init();
        ticks = 0;
        respawnSent = false;
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.WARDEN_HEARTBEAT, 0.55f, 0.45f);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void tick() {
        ticks++;
        if (ticks == 8 || ticks == 20 || ticks == 32) {
            if (minecraft != null && minecraft.player != null && ticks < FLATLINE_AT) {
                float pitch = 0.55f - ticks * 0.006f;
                minecraft.player.playSound(SoundEvents.WARDEN_HEARTBEAT, 0.4f, pitch);
            }
        }
        if (ticks == FLATLINE_AT && minecraft != null && minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.BEACON_DEACTIVATE, 0.35f, 0.6f);
        }
        if (!respawnSent && ticks >= respawnAt()) {
            sendRespawn();
        }
    }

    private static int respawnAt() {
        return FIRST_LINE_AT + LINE_COUNT * LINE_STAGGER + HOLD_AFTER;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (ticks >= FIRST_LINE_AT + LINE_COUNT * LINE_STAGGER) {
            sendRespawn();
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ticks >= FIRST_LINE_AT + LINE_COUNT * LINE_STAGGER) {
            sendRespawn();
            return true;
        }
        return true;
    }

    private void sendRespawn() {
        if (respawnSent || minecraft == null || minecraft.player == null) {
            return;
        }
        respawnSent = true;
        ShellWakeHud.begin();
        minecraft.player.connection.send(
                new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        minecraft.setScreen(null);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF02050A);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        float t = ticks + partialTick;
        float fade = Mth.clamp(t / FADE_IN, 0f, 1f);
        int veil = Math.round(fade * 230f) & 0xFF;
        graphics.fill(0, 0, this.width, this.height, (veil << 24));

        float amp = t < FLATLINE_AT
                ? Mth.clamp(1f - (t / FLATLINE_AT) * (t / FLATLINE_AT), 0f, 1f)
                : 0f;
        drawPulse(graphics, t, amp, fade);

        int y = this.height / 2 + 18;
        for (int i = 0; i < LINE_COUNT; i++) {
            int start = FIRST_LINE_AT + i * LINE_STAGGER;
            if (ticks < start) {
                break;
            }
            String full = Component.translatable("death.effecoria.shell.line" + (i + 1)).getString();
            int shown = Mth.clamp((ticks - start) * 2, 0, full.length());
            String vis = full.substring(0, shown);
            int color = LINE_COLOR;
            int w = this.font.width(vis);
            graphics.drawString(this.font, vis, this.width / 2 - w / 2, y + i * 14, color, false);
        }
    }

    private void drawPulse(GuiGraphics graphics, float time, float amp, float fade) {
        int mid = this.height / 2 - 28;
        int left = 24;
        int right = this.width - 24;
        int a = Math.round(fade * (90 + amp * 140)) & 0xFF;
        int color = (a << 24) | (PULSE_COLOR & 0x00FFFFFF);
        int prevX = left;
        int prevY = mid;
        for (int x = left; x <= right; x += 2) {
            float u = (x - left) / 18f - time * 2.1f;
            float beat = (float) Math.pow(Math.max(0.0, Math.sin(u)), 18.0);
            int y = mid - Math.round(beat * amp * 36f);
            int y0 = Math.min(prevY, y);
            int y1 = Math.max(prevY, y);
            graphics.fill(prevX, prevY, x + 1, prevY + 2, color);
            if (y1 > y0) {
                graphics.fill(x, y0, x + 2, y1 + 2, color);
            }
            prevX = x;
            prevY = y;
        }
        if (amp < 0.08f) {
            int flatA = Math.round(fade * 70) & 0xFF;
            graphics.fill(left, mid, right, mid + 1, (flatA << 24) | 0x003A4A55);
        }
    }
}
