package com.effecoria.core.progression;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.magic.CastDelivery;

import net.minecraft.server.level.ServerPlayer;

/**
 * Passive progression: body training, meditation, successful casts, and the breathing drill.
 * Breathing mastery itself is trained via the hub mini-game / scrolls.
 */
public final class ProgressionService {
    private ProgressionService() {}

    public static void tick(ServerPlayer player, PlayerPsiData data) {
        tickPassiveXp(player, data);
        applyMilestones(player, data);
    }

    /** Grant XP from any source and convert completed thresholds into soul / max Ψ / essence. */
    public static void grant(ServerPlayer player, PlayerPsiData data, float amount) {
        if (amount <= 0f) {
            return;
        }
        data.addTrainingXp(amount);
        applyMilestones(player, data);
    }

    public static void onCastResolved(ServerPlayer player, PlayerPsiData data, CastDelivery delivery) {
        if (delivery == CastDelivery.FULL) {
            int streak = data.castSuccessStreak() + 1;
            data.setCastSuccessStreak(streak);
            float base = BalanceConfig.TRAINING_XP_CAST.get().floatValue();
            int cap = BalanceConfig.TRAINING_XP_CAST_STREAK_CAP.get();
            float perStep = BalanceConfig.TRAINING_XP_CAST_STREAK.get().floatValue();
            int bonusSteps = Math.min(Math.max(0, streak - 1), cap);
            grant(player, data, base + bonusSteps * perStep);
            return;
        }
        data.setCastSuccessStreak(0);
    }

    public static void onBreathTrainHit(ServerPlayer player, PlayerPsiData data) {
        grant(player, data, BalanceConfig.TRAINING_XP_BREATH_TRAIN.get().floatValue());
    }

    private static void tickPassiveXp(ServerPlayer player, PlayerPsiData data) {
        if (isStillMeditating(player)) {
            data.addTrainingXp(BalanceConfig.TRAINING_XP_MEDITATE.get().floatValue());
        } else if (player.isSprinting() && player.onGround()) {
            data.addTrainingXp(BalanceConfig.TRAINING_XP_SPRINT.get().floatValue());
        } else if (player.isSwimming()) {
            data.addTrainingXp(BalanceConfig.TRAINING_XP_SWIM.get().floatValue());
        }
    }

    private static boolean isStillMeditating(ServerPlayer player) {
        if (!BreathingService.isMeditating(player)) {
            return false;
        }
        return player.getDeltaMovement().horizontalDistanceSqr() < 0.0004;
    }

    private static void applyMilestones(ServerPlayer player, PlayerPsiData data) {
        float threshold = BalanceConfig.TRAINING_XP_THRESHOLD.get().floatValue();
        if (threshold <= 0f) {
            return;
        }
        float soulBefore = data.soulStrength();
        float maxPsiBefore = data.maxPsi();
        // Cap iterations so dumping huge XP (e.g. 70k) cannot stall the server.
        int safety = 64;
        while (data.trainingXp() >= threshold && safety-- > 0) {
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
