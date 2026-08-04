package com.effecoria.core.progression;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.magic.CastDelivery;

import net.minecraft.server.level.ServerPlayer;

/**
 * Passive progression: movement fills training toward +max Ψ (internal cast energy).
 * Casts, meditation, and the breathing drill also feed the same bar. Soul strength still
 * ticks up on milestones until its cap. Orkanum is not trained here.
 */
public final class ProgressionService {
    private ProgressionService() {}

    public static void tick(ServerPlayer player, PlayerPsiData data) {
        tickPassiveXp(player, data);
        applyMilestones(player, data);
    }

    /** Grant XP from any source and convert completed thresholds into max Ψ / soul / essence. */
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

    /** Effective max Ψ cap from training — never below default + bonus (fixes legacy config cap = 150). */
    public static float trainingMaxPsiCap() {
        float configured = BalanceConfig.TRAINING_MAX_PSI_CAP.get().floatValue();
        float floor = BalanceConfig.DEFAULT_MAX_PSI.get().floatValue()
                + BalanceConfig.TRAINING_MAX_PSI_BONUS.get().floatValue();
        return Math.max(configured, floor);
    }

    private static void tickPassiveXp(ServerPlayer player, PlayerPsiData data) {
        if (isStillMeditating(player)) {
            data.addTrainingXp(BalanceConfig.TRAINING_XP_MEDITATE.get().floatValue());
            return;
        }

        float blocks = data.consumeMovementSample(player);
        if (blocks <= 0.001f) {
            return;
        }

        float rate;
        if (player.isSwimming()) {
            rate = BalanceConfig.TRAINING_XP_SWIM_PER_BLOCK.get().floatValue();
        } else if (player.onGround() && player.isSprinting()) {
            rate = BalanceConfig.TRAINING_XP_SPRINT_PER_BLOCK.get().floatValue();
        } else if (player.onGround() && !player.isShiftKeyDown()) {
            rate = BalanceConfig.TRAINING_XP_WALK_PER_BLOCK.get().floatValue();
        } else {
            rate = BalanceConfig.TRAINING_XP_WALK_PER_BLOCK.get().floatValue() * 0.35f;
        }
        data.addTrainingXp(blocks * rate);
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
        float maxPsiBefore = data.maxPsi();
        float maxPsiCap = trainingMaxPsiCap();
        float psiGain = BalanceConfig.TRAINING_MAX_PSI_GAIN.get().floatValue();
        int safety = 64;
        while (data.trainingXp() >= threshold && safety-- > 0) {
            float soulGain = BalanceConfig.TRAINING_SOUL_GAIN.get().floatValue();
            float maxSoul = BalanceConfig.TRAINING_MAX_SOUL.get().floatValue();
            int essenceGain = BalanceConfig.ESSENCE_PER_TRAINING_MILESTONE.get();

            boolean gainPsi = data.maxPsi() + 0.001f < maxPsiCap && psiGain > 0f;
            boolean gainSoul = data.soulStrength() + 0.001f < maxSoul && soulGain > 0f;
            boolean gainEssence = essenceGain > 0;

            if (!gainPsi && !gainSoul && !gainEssence) {
                break;
            }

            data.addTrainingXp(-threshold);

            if (gainSoul) {
                data.setSoulStrength(Math.min(maxSoul, data.soulStrength() + soulGain));
            }
            if (gainPsi) {
                float next = Math.min(maxPsiCap, data.maxPsi() + psiGain);
                float delta = next - data.maxPsi();
                data.setMaxPsi(next);
                if (delta > 0f) {
                    data.setCurrentPsi(data.currentPsi() + delta);
                }
            }
            if (gainEssence) {
                data.addEssence(essenceGain);
            }
        }

        if (data.maxPsi() > maxPsiBefore) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.effecoria.training_psi_up", (int) data.maxPsi()),
                    true);
        }
    }

    public static boolean isTrainingMaxPsiCapped(PlayerPsiData data) {
        return data.maxPsi() >= trainingMaxPsiCap() - 0.001f;
    }
}
