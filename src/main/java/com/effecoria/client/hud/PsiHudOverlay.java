package com.effecoria.client.hud;

import com.effecoria.core.formula.FormulaEngine;
import com.effecoria.core.formula.PhiSample;
import com.effecoria.core.phi.CreativeGodMode;
import com.effecoria.core.phi.PhiFieldService;
import com.effecoria.core.progression.BreathingService;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class PsiHudOverlay {
    private PsiHudOverlay() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.level == null) {
            return;
        }

        PlayerPsiData data = minecraft.player.getData(ModAttachments.PSI.get());
        if (!data.initiated()) {
            return;
        }

        PhiSample phi = PhiFieldService.sample(minecraft.level, minecraft.player.position(), minecraft.player);
        boolean godMode = CreativeGodMode.isActive(minecraft.player);
        int x = 10;
        int y = minecraft.getWindow().getGuiScaledHeight() - 68;

        float psiFill = data.maxPsi() > 0f ? data.currentPsi() / data.maxPsi() : 0f;
        drawBar(graphics, x, y, 90, 8, psiFill, 0xFF6A0DAD, 0xFF2E0845);

        String regenLabel = formatPsiRegen(data, phi, godMode);
        graphics.drawString(
                minecraft.font,
                Component.translatable("hud.effecoria.psi", (int) data.currentPsi(), (int) data.maxPsi(), regenLabel),
                x,
                y - 10,
                0xE0A8FF);

        float phiFill = phi.isInfinite() ? 1f : Math.min(1f, phi.effectiveValue() / 1.2f);
        drawBar(graphics, x, y + 16, 90, 6, phiFill, phiBarColor(phi), 0xFF1B3A59);
        graphics.drawString(minecraft.font, formatPhiLabel(phi), x, y + 28, phiTextColor(phi));

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

        if (data.breathingMastery() > 0f) {
            graphics.drawString(
                    minecraft.font,
                    Component.translatable(
                            "hud.effecoria.breathing",
                            BreathingService.formatTotalPercent(data.breathingMastery())),
                    x,
                    y + 52,
                    0x88FFCC);
        }
    }

    /** Ψ regen per second — server applies regen every 10 ticks with Δt=10. */
    private static String formatPsiRegen(PlayerPsiData data, PhiSample phi, boolean godMode) {
        if (godMode) {
            return "∞";
        }
        if (phi.zeroFlux()) {
            return "0";
        }
        float perSecond = FormulaEngine.regenPsi(PsiHelper.toContext(data), phi, 10f) * 2f;
        return String.format("%.1f", perSecond);
    }

    private static Component formatPhiLabel(PhiSample phi) {
        if (phi.isInfinite()) {
            return Component.translatable("hud.effecoria.phi_infinite");
        }
        if (phi.zeroFlux()) {
            return Component.translatable("hud.effecoria.phi_zero");
        }
        String phaseKey = phi.solarDay() ? "hud.effecoria.phi_day" : "hud.effecoria.phi_night";
        return Component.translatable(phaseKey, String.format("%.2f", phi.effectiveValue()));
    }

    private static int phiBarColor(PhiSample phi) {
        if (phi.zeroFlux()) {
            return 0xFF553355;
        }
        return phi.solarDay() ? 0xFF3D85C6 : 0xFF2A4A7A;
    }

    private static int phiTextColor(PhiSample phi) {
        if (phi.isInfinite()) {
            return 0xFFD4AF37;
        }
        if (phi.zeroFlux()) {
            return 0xFF5555;
        }
        return 0xAAD4FF;
    }

    private static void drawBar(GuiGraphics graphics, int x, int y, int width, int height, float fill, int fillColor, int bgColor) {
        graphics.fill(x, y, x + width, y + height, bgColor);
        graphics.fill(x, y, x + Math.round(width * Math.clamp(fill, 0f, 1f)), y + height, fillColor);
    }
}
