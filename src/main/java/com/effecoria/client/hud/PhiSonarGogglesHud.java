package com.effecoria.client.hud;

import com.effecoria.client.ClientPhiSonarMap;
import com.effecoria.content.ModItems;
import com.effecoria.core.artifact.CuriosAccess;
import com.effecoria.core.tower.PhiSonarService;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Corner HUD while Φ-sonar goggles are worn — feed status + terrain legend. */
public final class PhiSonarGogglesHud {
    private PhiSonarGogglesHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        if (!CuriosAccess.hasEquipped(mc.player, ModItems.PHI_SONAR_GOGGLES.get())) {
            return;
        }

        int x = 8;
        int y = 8;
        graphics.drawString(
                mc.font,
                Component.translatable("gui.effecoria.phi_sonar_goggles.title"),
                x,
                y,
                0xB8E0FF,
                false);
        y += 10;

        if (!ClientPhiSonarMap.hasMap()) {
            graphics.drawString(
                    mc.font,
                    Component.translatable("gui.effecoria.phi_sonar_goggles.no_feed"),
                    x,
                    y,
                    0x8899AA,
                    false);
            return;
        }

        PhiSonarService.Mode mode = PhiSonarService.Mode.fromId(ClientPhiSonarMap.modeId());
        graphics.drawString(
                mc.font,
                Component.translatable(
                        "gui.effecoria.phi_sonar_goggles.feed",
                        Component.translatable("gui.effecoria.phi_sonar.mode." + mode.name().toLowerCase()),
                        ClientPhiSonarMap.originX(),
                        ClientPhiSonarMap.originZ()),
                x,
                y,
                0xD0D8E0,
                false);
        y += 12;

        drawLegend(graphics, mc, x, y, 0xFF3AD0E8, "gui.effecoria.phi_sonar.terrain.essonite");
        y += 9;
        drawLegend(graphics, mc, x, y, 0xFF7EC8FF, "gui.effecoria.phi_sonar.terrain.mithril");
        y += 9;
        drawLegend(graphics, mc, x, y, 0xFFAA40FF, "gui.effecoria.phi_sonar.terrain.omega");
        y += 9;
        drawLegend(graphics, mc, x, y, 0xFF40E8FF, "gui.effecoria.phi_sonar.terrain.geyser");
        y += 9;
        drawLegend(graphics, mc, x, y, 0xFFD0B040, "gui.effecoria.phi_sonar.terrain.shield");
    }

    private static void drawLegend(GuiGraphics graphics, Minecraft mc, int x, int y, int color, String key) {
        graphics.fill(x, y, x + 6, y + 6, color);
        graphics.drawString(mc.font, Component.translatable(key), x + 9, y - 1, 0xC8D0D8, false);
    }
}
