package com.effecoria.core.progression;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Breathing technique progression — continuous mastery, not discrete tiers. */
public final class BreathingService {
    private BreathingService() {}

    public static float maxMastery() {
        return BalanceConfig.BREATHING_MAX_MASTERY.get().floatValue();
    }

    /** Standing calm with full breath — meditation stance for breathing gains and exhaustion recovery. */
    public static boolean isMeditating(ServerPlayer player) {
        return player.onGround()
                && !player.isSprinting()
                && !player.isInWater()
                && !player.isPassenger()
                && player.getAirSupply() >= player.getMaxAirSupply() - 10;
    }

    /** Adds mastery up to the cap; returns the amount actually gained. */
    public static float addMastery(PlayerPsiData data, float amount) {
        if (amount <= 0f) {
            return 0f;
        }
        float cap = maxMastery();
        float before = data.breathingMastery();
        if (before >= cap) {
            return 0f;
        }
        float after = Math.min(cap, before + amount);
        data.setBreathingMastery(after);
        return after - before;
    }

    public static void notifyGain(ServerPlayer player, float gained, String messageKey) {
        if (gained <= 0f) {
            return;
        }
        player.displayClientMessage(
                Component.translatable(messageKey, formatPercent(gained)),
                true);
    }

    /** Milestone toast when crossing 25/50/75/100% of max mastery. */
    public static void notifyMilestones(ServerPlayer player, float before, float after) {
        float cap = maxMastery();
        if (cap <= 0f) {
            return;
        }
        int beforeStep = masteryStep(before, cap);
        int afterStep = masteryStep(after, cap);
        if (afterStep > beforeStep) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.effecoria.breathing_milestone",
                            formatPercent(after)),
                    true);
        }
    }

    public static void sync(ServerPlayer player, PlayerPsiData data) {
        player.setData(ModAttachments.PSI.get(), data);
        player.syncData(ModAttachments.PSI.get());
    }

    public static String formatPercent(float masteryAmount) {
        float cap = maxMastery();
        float ratio = cap > 0f ? masteryAmount / cap : masteryAmount;
        return String.format("%.0f", ratio * 100f);
    }

    public static String formatTotalPercent(float totalMastery) {
        float cap = maxMastery();
        float ratio = cap > 0f ? totalMastery / cap : totalMastery;
        return String.format("%.0f", ratio * 100f);
    }

    private static int masteryStep(float mastery, float cap) {
        if (cap <= 0f) {
            return 0;
        }
        float ratio = mastery / cap;
        if (ratio >= 1f) {
            return 4;
        }
        if (ratio >= 0.75f) {
            return 3;
        }
        if (ratio >= 0.5f) {
            return 2;
        }
        if (ratio >= 0.25f) {
            return 1;
        }
        return 0;
    }
}
