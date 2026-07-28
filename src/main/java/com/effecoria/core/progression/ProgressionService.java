package com.effecoria.core.progression;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.psi.PlayerPsiData;

import net.minecraft.server.level.ServerPlayer;

/** Passive progression: breathing meditation and physical training. */
public final class ProgressionService {
    private ProgressionService() {}

    public static void tick(ServerPlayer player, PlayerPsiData data) {
        tickBreathingMeditation(player, data);
        tickTraining(player, data);
    }

    /** Standing calm — gradual breathing mastery (meditation). */
    private static void tickBreathingMeditation(ServerPlayer player, PlayerPsiData data) {
        if (!data.initiated()) {
            return;
        }

        boolean meditating = player.onGround()
                && !player.isSprinting()
                && !player.isInWater()
                && !player.isPassenger()
                && player.getAirSupply() >= player.getMaxAirSupply() - 10;

        if (!meditating || data.breathingMastery() >= BreathingService.maxMastery()) {
            return;
        }

        float before = data.breathingMastery();
        float gained = BreathingService.addMastery(
                data,
                BalanceConfig.BREATHING_MEDITATION_GAIN.get().floatValue());
        if (gained > 0f) {
            BreathingService.notifyMilestones(player, before, data.breathingMastery());
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
            int essenceGain = BalanceConfig.ESSENCE_PER_TRAINING_MILESTONE.get();
            if (essenceGain > 0) {
                data.addEssence(essenceGain);
            }
        }

        if (data.soulStrength() > soulBefore || data.maxPsi() > maxPsiBefore) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.effecoria.training_milestone",
                            String.format("%.2f", data.soulStrength()),
                            (int) data.maxPsi()),
                    true);
        }
    }
}
