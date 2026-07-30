package com.effecoria.core.progression;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.magic.SpellDefinition;
import com.effecoria.core.psi.PlayerPsiData;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Magical exhaustion from overcasting — soft penalty layer below entropy backlash.
 */
public final class ExhaustionService {
    public static final float MAX = 100f;

    public enum Band {
        NONE,
        TIRED,
        STRAINED,
        COLLAPSING
    }

    private ExhaustionService() {}

    public static Band band(float exhaustion) {
        if (exhaustion >= BalanceConfig.EXHAUSTION_STRAINED.get().floatValue()) {
            return Band.COLLAPSING;
        }
        if (exhaustion >= BalanceConfig.EXHAUSTION_TIRED.get().floatValue()) {
            return Band.STRAINED;
        }
        if (exhaustion >= BalanceConfig.EXHAUSTION_WARM.get().floatValue()) {
            return Band.TIRED;
        }
        return Band.NONE;
    }

    public static void onSuccessfulCast(
            ServerPlayer player, PlayerPsiData data, SpellDefinition spell, float actualCost) {
        float gain = actualCost * BalanceConfig.EXHAUSTION_GAIN_PER_COST.get().floatValue()
                + spell.sideEntropyRatio() * BalanceConfig.EXHAUSTION_GAIN_PER_ENTROPY.get().floatValue();
        float psiRatio = data.maxPsi() > 0f ? data.currentPsi() / data.maxPsi() : 1f;
        if (psiRatio < 0.2f) {
            gain += BalanceConfig.EXHAUSTION_LOW_PSI_BONUS.get().floatValue();
        }
        addExhaustion(data, gain);
        applyCollapseCastDamage(player, data);
    }

    public static void onBacklash(ServerPlayer player, PlayerPsiData data) {
        addExhaustion(data, BalanceConfig.EXHAUSTION_BACKLASH_SPIKE.get().floatValue());
    }

    /** Clears overcast penalties that should not survive death. */
    public static void clearOnDeath(PlayerPsiData data) {
        data.setExhaustion(0f);
        data.setEntropyB(0f);
        data.setSteamFlightActive(false);
        data.setSteamFlightDrainPerTick(0f);
    }

    /** Strip exhaustion-driven status effects from a living player. */
    public static void stripExhaustionEffects(ServerPlayer player) {
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.CONFUSION);
    }

    public static void tick(ServerPlayer player, PlayerPsiData data) {
        float decay = BalanceConfig.EXHAUSTION_DECAY_PER_TICK.get().floatValue();
        if (BreathingService.isMeditating(player) && player.getAirSupply() >= player.getMaxAirSupply() - 10) {
            decay += BalanceConfig.EXHAUSTION_MEDITATION_DECAY_BONUS.get().floatValue();
        }
        if (decay > 0f && data.exhaustion() > 0f) {
            data.setExhaustion(Math.max(0f, data.exhaustion() - decay));
        }
        applyMobEffects(player, data);
    }

    public static float regenMultiplier(float exhaustion) {
        return switch (band(exhaustion)) {
            case TIRED -> BalanceConfig.EXHAUSTION_REGEN_TIRED.get().floatValue();
            case STRAINED -> BalanceConfig.EXHAUSTION_REGEN_STRAINED.get().floatValue();
            case COLLAPSING -> BalanceConfig.EXHAUSTION_REGEN_COLLAPSING.get().floatValue();
            default -> 1f;
        };
    }

    public static float costMultiplier(float exhaustion) {
        return switch (band(exhaustion)) {
            case TIRED -> BalanceConfig.EXHAUSTION_COST_TIRED.get().floatValue();
            case STRAINED -> BalanceConfig.EXHAUSTION_COST_STRAINED.get().floatValue();
            case COLLAPSING -> BalanceConfig.EXHAUSTION_COST_COLLAPSING.get().floatValue();
            default -> 1f;
        };
    }

    private static void addExhaustion(PlayerPsiData data, float amount) {
        data.setExhaustion(Math.min(MAX, data.exhaustion() + amount));
    }

    private static void applyCollapseCastDamage(ServerPlayer player, PlayerPsiData data) {
        if (band(data.exhaustion()) != Band.COLLAPSING) {
            return;
        }
        float damage = BalanceConfig.EXHAUSTION_COLLAPSE_CAST_DAMAGE.get().floatValue();
        if (damage > 0f) {
            player.hurt(player.level().damageSources().magic(), damage);
        }
    }

    private static void applyMobEffects(ServerPlayer player, PlayerPsiData data) {
        switch (band(data.exhaustion())) {
            case TIRED -> player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, 0, false, false, true));
            case STRAINED -> {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, 0, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 0, false, false, true));
            }
            case COLLAPSING -> {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, 1, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 1, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 25, 0, false, false, true));
            }
            default -> {}
        }
    }
}
