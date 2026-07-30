package com.effecoria.client.hud;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.formula.FormulaEngine;
import com.effecoria.core.formula.PhiSample;
import com.effecoria.core.phi.CreativeGodMode;
import com.effecoria.core.phi.PhiFieldService;
import com.effecoria.core.progression.BreathingService;
import com.effecoria.core.progression.EntropyService;
import com.effecoria.core.progression.ExhaustionService;
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
        int y = minecraft.getWindow().getGuiScaledHeight() - 80;

        float psiFill = data.maxPsi() > 0f ? data.currentPsi() / data.maxPsi() : 0f;
        drawBar(graphics, x, y, 90, 8, psiFill, 0xFF6A0DAD, 0xFF2E0845);

        String regenLabel = formatPsiRegen(minecraft.player, data, phi, godMode);
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
        if (data.isLichAscensionActive(minecraft.level.getGameTime())) {
            graphics.drawString(
                    minecraft.font,
                    Component.translatable("hud.effecoria.lich_ascension", (int) (data.phylacteryEfficiency() * 100)),
                    x,
                    y - 34,
                    0xAA88FF);
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

        if (data.exhaustion() >= BalanceConfig.EXHAUSTION_WARM.get().floatValue()) {
            float exFill = data.exhaustion() / ExhaustionService.MAX;
            drawBar(graphics, x, y + 64, 90, 5, exFill, 0xFFAA4444, 0xFF331111);
            graphics.drawString(
                    minecraft.font,
                    Component.translatable(
                            "hud.effecoria.exhaustion",
                            formatExhaustionBand(data.exhaustion()),
                            (int) data.exhaustion()),
                    x,
                    y + 72,
                    0xFFCC8888);
        }

        float entropyFill = EntropyService.fillRatio(data.entropyB());
        if (entropyFill > 0.02f) {
            boolean critical = EntropyService.isCritical(data.entropyB());
            int barColor = critical ? 0xFFE07030 : 0xFF8866AA;
            int textColor = critical ? 0xFFFFAA66 : 0xCCBBAADD;
            drawBar(graphics, x, y + 84, 90, 5, entropyFill, barColor, 0xFF221828);
            graphics.drawString(
                    minecraft.font,
                    Component.translatable(
                            critical ? "hud.effecoria.entropy_warn" : "hud.effecoria.entropy",
                            (int) (entropyFill * 100)),
                    x,
                    y + 92,
                    textColor);
        }
    }

    private static Component formatExhaustionBand(float exhaustion) {
        return switch (ExhaustionService.band(exhaustion)) {
            case TIRED -> Component.translatable("hud.effecoria.exhaustion.tired");
            case STRAINED -> Component.translatable("hud.effecoria.exhaustion.strained");
            case COLLAPSING -> Component.translatable("hud.effecoria.exhaustion.collapsing");
            default -> Component.translatable("hud.effecoria.exhaustion.warm");
        };
    }

    private static String formatPsiRegen(
            net.minecraft.world.entity.player.Player player, PlayerPsiData data, PhiSample phi, boolean godMode) {
        if (godMode) {
            return "∞";
        }
        if (phi.zeroFlux()) {
            return "0";
        }
        long gameTime = player.level().getGameTime();
        float perSecond;
        if (data.isLichAscensionActive(gameTime)) {
            perSecond = FormulaEngine.regenPsiLich(
                            PsiHelper.toContext(player, data), phi, data.phylacteryEfficiency(), 10f)
                    * 2f;
        } else {
            perSecond = FormulaEngine.regenPsi(PsiHelper.toContext(player, data), phi, 10f) * 2f;
        }
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
