package com.effecoria.client.hud;

import com.effecoria.client.ClientInputEvents;
import com.effecoria.config.BalanceConfig;
import com.effecoria.core.formula.FormulaEngine;
import com.effecoria.core.formula.PhiSample;
import com.effecoria.core.phi.CreativeGodMode;
import com.effecoria.core.phi.PhiFieldService;
import com.effecoria.core.progression.BiologyService;
import com.effecoria.core.progression.BreathingService;
import com.effecoria.core.progression.EntropyService;
import com.effecoria.core.progression.ExhaustionService;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.effect.necromancy.NecroSummonService;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class PsiHudOverlay {
    /** Game-time when the primer HUD nudge first became eligible; -1 = inactive. */
    private static long primerNudgeSince = -1L;
    private static final int PRIMER_NUDGE_TICKS = 20 * 30; // half a minute

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

        float max = data.maxPsi();
        float current = data.currentPsi();
        float reserved = Math.min(max, Math.max(0f, data.necroReservedPsi()));
        float usable = Math.max(0f, current - reserved);
        float usableFill = max > 0f ? usable / max : 0f;
        float reservedFill = max > 0f ? reserved / max : 0f;

        // Background → reserved band (right) → usable fill (left).
        graphics.fill(x, y, x + 90, y + 8, 0xFF2E0845);
        if (reservedFill > 0.001f) {
            int reservedWidth = Math.round(90 * reservedFill);
            graphics.fill(x + 90 - reservedWidth, y, x + 90, y + 8, 0xFF5A3068);
        }
        if (usableFill > 0.001f) {
            graphics.fill(x, y, x + Math.round(90 * Math.clamp(usableFill, 0f, 1f)), y + 8, 0xFF6A0DAD);
        }

        String regenLabel = formatPsiRegen(minecraft.player, data, phi, godMode);
        graphics.drawString(
                minecraft.font,
                Component.translatable("hud.effecoria.psi", (int) current, (int) max, regenLabel),
                x,
                y - 10,
                0xE0A8FF);

        if (reserved > 0.5f) {
            graphics.drawString(
                    minecraft.font,
                    Component.translatable(
                            "hud.effecoria.psi_reserved",
                            (int) reserved,
                            (int) NecroSummonService.usablePsi(minecraft.player, data)),
                    x,
                    y - 20,
                    0xCCAA88CC);
        }

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
        } else if ((data.primerTipsMask() & com.effecoria.core.progression.FirstHourTips.Tip.FIRST_CAST.mask())
                == 0
                && data.initiated()) {
            long now = minecraft.level.getGameTime();
            if (primerNudgeSince < 0L) {
                primerNudgeSince = now;
            }
            if (now - primerNudgeSince < PRIMER_NUDGE_TICKS) {
                graphics.drawString(
                        minecraft.font,
                        Component.translatable("hud.effecoria.primer_nudge"),
                        x,
                        y - 34,
                        0xCCD4A060);
            }
        } else {
            primerNudgeSince = -1L;
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

        int extraY = 0;
        if (!godMode && !data.isLichAscensionActive(minecraft.level.getGameTime())) {
            float body = BiologyService.bodyFactor(minecraft.player);
            if (body < 0.995f) {
                int hintY = data.breathingMastery() > 0f ? y + 62 : y + 52;
                graphics.drawString(
                        minecraft.font,
                        Component.translatable("hud.effecoria.body_low", (int) (body * 100f)),
                        x,
                        hintY,
                        0xFFCCAA66);
                extraY = 10;
            }
        }

        if (ClientInputEvents.isCastCharging()) {
            float charge = ClientInputEvents.castChargeFraction();
            float min = BalanceConfig.CAST_CHARGE_MIN_POWER.get().floatValue();
            float powerPct = min + (1f - min) * charge;
            int barY = y + 52 + (data.breathingMastery() > 0f ? 10 : 0) + extraY;
            drawBar(graphics, x, barY, 90, 4, charge, 0xFFE8C060, 0xFF3A3018);
            graphics.drawString(
                    minecraft.font,
                    Component.translatable("hud.effecoria.cast_charge", (int) (powerPct * 100f)),
                    x,
                    barY + 6,
                    0xFFE8C060);
            graphics.drawString(
                    minecraft.font,
                    Component.translatable("hud.effecoria.cast_charge_hint"),
                    x,
                    barY + 16,
                    0xCCBBA070);
            extraY += 26;
        }

        if (data.exhaustion() >= BalanceConfig.EXHAUSTION_WARM.get().floatValue()) {
            float exFill = data.exhaustion() / ExhaustionService.MAX;
            drawBar(graphics, x, y + 64 + extraY, 90, 5, exFill, 0xFFAA4444, 0xFF331111);
            graphics.drawString(
                    minecraft.font,
                    Component.translatable(
                            "hud.effecoria.exhaustion",
                            formatExhaustionBand(data.exhaustion()),
                            (int) data.exhaustion()),
                    x,
                    y + 72 + extraY,
                    0xFFCC8888);
        }

        float entropyFill = EntropyService.fillRatio(data.entropyB());
        if (entropyFill > 0.02f) {
            boolean critical = EntropyService.isCritical(data.entropyB());
            int barColor = critical ? 0xFFE07030 : 0xFF8866AA;
            int textColor = critical ? 0xFFFFAA66 : 0xCCBBAADD;
            drawBar(graphics, x, y + 84 + extraY, 90, 5, entropyFill, barColor, 0xFF221828);
            graphics.drawString(
                    minecraft.font,
                    Component.translatable(
                            critical ? "hud.effecoria.entropy_warn" : "hud.effecoria.entropy",
                            (int) (entropyFill * 100)),
                    x,
                    y + 92 + extraY,
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
        Minecraft minecraft = Minecraft.getInstance();
        boolean underwater = minecraft.player != null && minecraft.player.isUnderWater();
        boolean inWater = minecraft.player != null && minecraft.player.isInWaterOrBubble();
        String phaseKey = phi.solarDay() ? "hud.effecoria.phi_day" : "hud.effecoria.phi_night";
        Component base = Component.translatable(phaseKey, String.format("%.2f", phi.effectiveValue()));
        if (underwater) {
            return base.copy().append(Component.translatable("hud.effecoria.phi_underwater_suffix"));
        }
        if (inWater) {
            return base.copy().append(Component.translatable("hud.effecoria.phi_water_suffix"));
        }
        return base;
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
