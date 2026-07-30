package com.effecoria.core.progression;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.formula.FormulaEngine;
import com.effecoria.core.psi.PlayerPsiData;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Side-channel entropy (b): accumulates on cast, decays at rest, backlash when full.
 */
public final class EntropyService {
    private EntropyService() {}

    /** Called every player magic tick (every 10 server ticks). */
    public static void tick(ServerPlayer player, PlayerPsiData data) {
        float decay = BalanceConfig.ENTROPY_DECAY_PER_TICK.get().floatValue();
        if (BreathingService.isMeditating(player) && player.getAirSupply() >= player.getMaxAirSupply() - 10) {
            decay += BalanceConfig.ENTROPY_MEDITATION_DECAY_BONUS.get().floatValue();
        }
        if (decay > 0f && data.entropyB() > 0f) {
            data.setEntropyB(data.entropyB() - decay);
        }
        maybeWarnRising(player, data);
    }

    /** First time entropy crosses the warn band — teach the player before backlash. */
    public static void maybeWarnRising(ServerPlayer player, PlayerPsiData data) {
        if (data.seenEntropyWarn()) {
            return;
        }
        float threshold = BalanceConfig.ENTROPY_THRESHOLD.get().floatValue();
        float warnAt = threshold * BalanceConfig.ENTROPY_WARN_RATIO.get().floatValue();
        if (data.entropyB() < warnAt) {
            return;
        }
        data.setSeenEntropyWarn(true);
        player.displayClientMessage(Component.translatable("message.effecoria.entropy_warn"), true);
    }

    /** First backlash: longer chat explanation after the action-bar flash. */
    public static void onBacklash(ServerPlayer player, PlayerPsiData data) {
        if (data.seenEntropyTutorial()) {
            return;
        }
        data.setSeenEntropyTutorial(true);
        data.setSeenEntropyWarn(true);
        player.sendSystemMessage(Component.translatable("message.effecoria.entropy_tutorial"));
    }

    public static float fillRatio(float entropyB) {
        float threshold = BalanceConfig.ENTROPY_THRESHOLD.get().floatValue();
        if (threshold <= 0f) {
            return 0f;
        }
        return Math.clamp(entropyB / threshold, 0f, 1f);
    }

    public static boolean isCritical(float entropyB) {
        float ratio = fillRatio(entropyB);
        return ratio >= BalanceConfig.ENTROPY_WARN_RATIO.get().floatValue()
                && !FormulaEngine.isBacklashTriggered(entropyB);
    }
}
