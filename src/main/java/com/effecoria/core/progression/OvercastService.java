package com.effecoria.core.progression;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.psi.PlayerPsiData;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Overcast trauma — paying for a spell with more Ψ than you have.
 * Comfortable casts (cost fits in usable Ψ) apply no cast penalties.
 */
public final class OvercastService {
    private OvercastService() {}

    /**
     * @param deficit {@code cost - usablePsi} (must be &gt; 0)
     * @param cost full spell cost
     */
    public static void apply(ServerPlayer player, PlayerPsiData data, float deficit, float cost) {
        float severity = Mth.clamp(deficit / Math.max(0.01f, cost), 0.2f, 1f);
        int duration = Math.round(
                (float) (BalanceConfig.OVERCAST_DURATION_BASE.get()
                        + severity * BalanceConfig.OVERCAST_DURATION_PER_SEVERITY.get()));
        long now = player.level().getGameTime();
        data.applyOvercastTrauma(now, severity, duration);

        float exhaustSpike = BalanceConfig.OVERCAST_EXHAUSTION_BASE.get().floatValue()
                + severity * BalanceConfig.OVERCAST_EXHAUSTION_PER_SEVERITY.get().floatValue();
        ExhaustionService.addExhaustion(data, exhaustSpike);

        // Immediate horror — refreshed by ExhaustionService while trauma lasts.
        int burst = Math.min(duration, 200);
        int amp = severity >= 0.7f ? 2 : 1;
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, burst, amp, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, burst, amp, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, burst, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, Math.min(burst, 120), 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, burst, 1, false, false, true));

        // No HP damage on cast — trauma is channel collapse (regen/breathing/exhaustion), not flesh wounds.
        float entropyBump = BalanceConfig.OVERCAST_ENTROPY_BUMP.get().floatValue() * severity;
        data.setEntropyB(data.entropyB() + entropyBump);

        player.displayClientMessage(
                Component.translatable(
                        "message.effecoria.overcast",
                        String.format("%.0f", severity * 100f),
                        String.format("%.0f", duration / 20f)),
                true);
    }
}
