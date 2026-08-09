package com.effecoria.core.progression;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Breathing technique progression — continuous mastery with no mortal 100% ceiling.
 * {@link BalanceConfig#BREATHING_MAX_MASTERY} is the 100% reference; players may ascend far past it.
 */
public final class BreathingService {
    private BreathingService() {}

    /** Reference mastery shown as 100% (unlock / baseline scaling). */
    public static float referenceMastery() {
        return BalanceConfig.BREATHING_MAX_MASTERY.get().floatValue();
    }

    /** Absolute ceiling; {@code 0} means uncapped. */
    public static float hardCap() {
        return BalanceConfig.BREATHING_HARD_CAP.get().floatValue();
    }

    /** Standing calm with full breath — meditation stance for exhaustion recovery and training XP. */
    public static boolean isMeditating(ServerPlayer player) {
        return player.onGround()
                && !player.isSprinting()
                && !player.isInWater()
                && !player.isPassenger()
                && player.getAirSupply() >= player.getMaxAirSupply() - 10;
    }

    /**
     * Gain scale: full rate up to 100%, then soft diminishing so ascension stays a long climb
     * without stopping growth entirely.
     */
    public static float gainScale(float currentMastery) {
        float ref = referenceMastery();
        if (ref <= 0f || currentMastery <= ref) {
            return 1f;
        }
        // At 200% → 0.5, at 500% → 0.2, asymptote toward 0 but never zero.
        return ref / currentMastery;
    }

    /** Adds mastery up to the hard cap (if any); returns the amount actually gained. */
    public static float addMastery(PlayerPsiData data, float amount) {
        if (amount <= 0f) {
            return 0f;
        }
        float before = data.breathingMastery();
        float hard = hardCap();
        if (hard > 0f && before >= hard) {
            return 0f;
        }
        float scaled = amount * gainScale(before);
        if (data.race().orElse(null) == PlayerRace.HUMAN) {
            scaled *= 1.05f;
        }
        float after = before + scaled;
        if (hard > 0f) {
            after = Math.min(hard, after);
        }
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

    /** Milestone toast every 25% of reference mastery, including post-100% ascension. */
    public static void notifyMilestones(ServerPlayer player, float before, float after) {
        float ref = referenceMastery();
        if (ref <= 0f) {
            return;
        }
        int beforeStep = masteryStep(before, ref);
        int afterStep = masteryStep(after, ref);
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
        float ref = referenceMastery();
        float ratio = ref > 0f ? masteryAmount / ref : masteryAmount;
        return String.format("%.0f", ratio * 100f);
    }

    public static String formatTotalPercent(float totalMastery) {
        return formatPercent(totalMastery);
    }

    /** Ratio vs reference (1.0 = 100%). Unclamped — used for power/regen scaling. */
    public static float referenceRatio(float mastery) {
        float ref = referenceMastery();
        if (ref <= 0f) {
            return 0f;
        }
        return Math.max(0f, mastery / ref);
    }

    private static int masteryStep(float mastery, float ref) {
        if (ref <= 0f) {
            return 0;
        }
        return Math.max(0, (int) Math.floor(mastery / ref * 4f));
    }
}
