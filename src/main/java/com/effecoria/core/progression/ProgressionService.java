package com.effecoria.core.progression;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.psi.PlayerPsiData;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Passive progression: calm breathing tiers and physical training. */
public final class ProgressionService {
    private ProgressionService() {}

    public static void tick(ServerPlayer player, PlayerPsiData data) {
        tickBreathing(player, data);
        tickTraining(player, data);
    }

    private static void tickBreathing(ServerPlayer player, PlayerPsiData data) {
        int maxTier = BalanceConfig.BREATHING_MAX_TIER.get();
        int ticksPerTier = BalanceConfig.BREATHING_CALM_TICKS_PER_TIER.get();

        boolean calm = player.onGround()
                && !player.isSprinting()
                && !player.isInWater()
                && player.getAirSupply() >= player.getMaxAirSupply() - 10;

        if (calm && data.breathingTier() < maxTier) {
            data.addCalmBreathTicks(1);
            if (data.calmBreathTicks() >= ticksPerTier) {
                int newTier = data.breathingTier() + 1;
                data.setBreathingTier(newTier);
                data.resetCalmBreathTicks();
                player.displayClientMessage(
                        Component.translatable("message.effecoria.breathing_tier_up", newTier),
                        true);
            }
        } else if (!calm) {
            data.resetCalmBreathTicks();
        }
    }

    private static void tickTraining(ServerPlayer player, PlayerPsiData data) {
        if (player.isSprinting() && player.onGround()) {
            data.addTrainingXp(BalanceConfig.TRAINING_XP_SPRINT.get().floatValue());
        } else if (player.isSwimming()) {
            data.addTrainingXp(BalanceConfig.TRAINING_XP_SWIM.get().floatValue());
        }

        float threshold = BalanceConfig.TRAINING_XP_THRESHOLD.get().floatValue();
        float soulBefore = data.soulStrength();
        float maxPsiBefore = data.maxPsi();
        while (data.trainingXp() >= threshold) {
            data.addTrainingXp(-threshold);

            float soulGain = BalanceConfig.TRAINING_SOUL_GAIN.get().floatValue();
            float maxSoul = BalanceConfig.TRAINING_MAX_SOUL.get().floatValue();
            float psiGain = BalanceConfig.TRAINING_MAX_PSI_GAIN.get().floatValue();
            float maxPsi = BalanceConfig.TRAINING_MAX_PSI_CAP.get().floatValue();

            if (data.soulStrength() < maxSoul) {
                data.setSoulStrength(Math.min(maxSoul, data.soulStrength() + soulGain));
            }
            if (data.maxPsi() < maxPsi) {
                data.setMaxPsi(Math.min(maxPsi, data.maxPsi() + psiGain));
            }
        }

        if (data.soulStrength() > soulBefore || data.maxPsi() > maxPsiBefore) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.effecoria.training_milestone",
                            String.format("%.2f", data.soulStrength()),
                            (int) data.maxPsi()),
                    true);
        }
    }
}
