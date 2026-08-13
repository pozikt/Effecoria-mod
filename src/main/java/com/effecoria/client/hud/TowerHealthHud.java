package com.effecoria.client.hud;

import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Compressed dual health HUD for soulbound mages:
 * 3 tinted player hearts + digit | 3 ultramarine tower hearts + digit.
 */
public final class TowerHealthHud {
    private static final ResourceLocation HEART_CONTAINER =
            ResourceLocation.withDefaultNamespace("hud/heart/container");
    private static final ResourceLocation HEART_FULL =
            ResourceLocation.withDefaultNamespace("hud/heart/full");

    private TowerHealthHud() {}

    public static boolean shouldReplaceVanillaHearts() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return false;
        }
        PlayerPsiData data = PsiHelper.get(mc.player);
        return data.towerBound();
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.gui.getBossOverlay().shouldCreateWorldFog()) {
            return;
        }
        Player player = mc.player;
        PlayerPsiData data = PsiHelper.get(player);
        if (!data.towerBound()) {
            return;
        }

        int left = graphics.guiWidth() / 2 - 91;
        int top = graphics.guiHeight() - 39;
        if (player.getArmorValue() > 0) {
            top -= 10;
        }

        float maxHp = Math.max(1f, player.getMaxHealth());
        float hp = Math.max(0f, player.getHealth());
        float frac = Math.min(1f, hp / maxHp);

        // Player: 3 hearts with health-dependent tint + digit
        float[] tint = playerTint(frac);
        for (int i = 0; i < 3; i++) {
            int x = left + i * 8;
            float fill = Math.min(1f, Math.max(0f, frac * 3f - i));
            drawHeart(graphics, x, top, tint[0], tint[1], tint[2], fill);
        }
        String playerNum = String.valueOf(Math.round(hp));
        graphics.drawString(mc.font, playerNum, left + 28, top + 1, 0xFFFFFF, true);

        // Tower: ultramarine hearts + digit
        int towerMax = Math.max(0, data.towerMaxHp());
        int towerHp = Math.max(0, data.towerHp());
        float tFrac = towerMax <= 0 ? 0f : Math.min(1f, towerHp / (float) towerMax);
        int towerLeft = left + 52;
        for (int i = 0; i < 3; i++) {
            int x = towerLeft + i * 8;
            float fill = Math.min(1f, Math.max(0f, tFrac * 3f - i));
            drawHeart(graphics, x, top, 0.25f, 0.45f, 0.95f, fill);
        }
        String towerNum = towerMax <= 0 ? "0" : (towerHp + "/" + towerMax);
        graphics.drawString(mc.font, towerNum, towerLeft + 28, top + 1, 0x88AAFF, true);
    }

    private static float[] playerTint(float frac) {
        if (frac > 0.66f) {
            return new float[] {0.35f, 0.95f, 0.45f};
        }
        if (frac > 0.33f) {
            return new float[] {0.95f, 0.85f, 0.25f};
        }
        return new float[] {0.95f, 0.30f, 0.30f};
    }

    private static void drawHeart(GuiGraphics graphics, int x, int y, float r, float g, float b, float fill) {
        graphics.blitSprite(HEART_CONTAINER, x, y, 9, 9);
        if (fill <= 0.01f) {
            return;
        }
        graphics.setColor(r, g, b, 1f);
        if (fill >= 0.99f) {
            graphics.blitSprite(HEART_FULL, x, y, 9, 9);
        } else {
            // Half-ish: clip vertically via scissor
            int h = Math.max(1, Math.round(9 * fill));
            graphics.enableScissor(x, y + (9 - h), x + 9, y + 9);
            graphics.blitSprite(HEART_FULL, x, y, 9, 9);
            graphics.disableScissor();
        }
        graphics.setColor(1f, 1f, 1f, 1f);
    }
}
