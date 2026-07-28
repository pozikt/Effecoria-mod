package com.effecoria.client.hud;

import com.effecoria.core.formula.PhiSample;
import com.effecoria.core.phi.PhiFieldService;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class PsiHudOverlay {
    private PsiHudOverlay() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        PlayerPsiData data = minecraft.player.getData(ModAttachments.PSI.get());
        if (!data.initiated()) {
            return;
        }

        PhiSample phi = PhiFieldService.sample(minecraft.level, minecraft.player.position(), minecraft.player);
        int x = 10;
        int y = minecraft.getWindow().getGuiScaledHeight() - 55;

        drawBar(graphics, x, y, 80, 8, data.currentPsi() / data.maxPsi(), 0xFF6A0DAD, 0xFF2E0845);
        graphics.drawString(minecraft.font, Component.translatable("hud.effecoria.psi", (int) data.currentPsi(), (int) data.maxPsi()), x, y - 10, 0xE0A8FF);

        float phiFill = phi.isInfinite() ? 1f : Math.min(1f, phi.effectiveValue());
        drawBar(graphics, x, y + 16, 80, 6, phiFill, 0xFF3D85C6, 0xFF1B3A59);
        String phiLabel = phi.isInfinite()
                ? Component.translatable("hud.effecoria.phi_infinite").getString()
                : phi.zeroFlux()
                        ? Component.translatable("hud.effecoria.phi_zero").getString()
                        : Component.translatable("hud.effecoria.phi", String.format("%.2f", phi.effectiveValue())).getString();
        graphics.drawString(minecraft.font, phiLabel, x, y + 28, phi.isInfinite() ? 0xFFD4AF37 : phi.zeroFlux() ? 0xFF5555 : 0xAAD4FF);

        ResourceLocation selected = data.selectedSpell();
        if (selected != null) {
            graphics.drawString(
                    minecraft.font,
                    Component.translatable("hud.effecoria.spell", Component.translatable("spell.effecoria." + selected.getPath())),
                    x,
                    y + 40,
                    0xFFFFFF);
        }

        if (data.isPhiSenseActive(minecraft.level.getGameTime())) {
            graphics.drawString(minecraft.font, Component.translatable("hud.effecoria.phi_sense"), x, y - 22, 0x55FFFF);
        }

        if (data.breathingTier() > 0) {
            graphics.drawString(
                    minecraft.font,
                    Component.translatable("hud.effecoria.breathing", data.breathingTier()),
                    x,
                    y + 52,
                    0x88FFCC);
        }
    }

    private static void drawBar(GuiGraphics graphics, int x, int y, int width, int height, float fill, int fillColor, int bgColor) {
        graphics.fill(x, y, x + width, y + height, bgColor);
        graphics.fill(x, y, x + Math.round(width * Math.clamp(fill, 0f, 1f)), y + height, fillColor);
    }
}
