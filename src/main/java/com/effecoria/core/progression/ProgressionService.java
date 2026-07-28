package com.effecoria.core.progression;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.psi.PlayerPsiData;

import net.minecraft.server.level.ServerPlayer;

/** Passive progression: calm breathing tiers and physical training. */
public final class ProgressionService {
    private static final int CALM_TICKS_PER_TIER = 200;
    private static final int MAX_BREATHING_TIER = 2;

    private ProgressionService() {}

    public static void tick(ServerPlayer player, PlayerPsiData data) {
        tickBreathing(player, data);
        tickTraining(player, data);
    }

    private static void tickBreathing(ServerPlayer player, PlayerPsiData data) {
        boolean calm = player.onGround()
                && !player.isSprinting()
                && !player.isInWater()
                && player.getAirSupply() >= player.getMaxAirSupply() - 10;

        if (calm && data.breathingTier() < MAX_BREATHING_TIER) {
            data.addCalmBreathTicks(1);
            if (data.calmBreathTicks() >= CALM_TICKS_PER_TIER) {
                data.setBreathingTier(data.breathingTier() + 1);
                data.resetCalmBreathTicks();
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
    }
}
