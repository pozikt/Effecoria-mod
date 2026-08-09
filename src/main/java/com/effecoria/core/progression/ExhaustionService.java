package com.effecoria.core.progression;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.psi.PlayerPsiData;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Magical exhaustion — filled by overcast trauma and entropy backlash, not by healthy casts.
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

    /**
     * Healthy casts (cost fits in usable Ψ) apply no exhaustion.
     * Overcasts are handled by {@link OvercastService} before this is called.
     */
    public static void onHealthyCast(ServerPlayer player, PlayerPsiData data) {
        // No cast tax while the operator still has the energy to pay.
    }

    public static void onBacklash(ServerPlayer player, PlayerPsiData data) {
        addExhaustion(data, BalanceConfig.EXHAUSTION_BACKLASH_SPIKE.get().floatValue());
    }

    /** Clears overcast penalties that should not survive death. */
    public static void clearOnDeath(PlayerPsiData data) {
        data.setExhaustion(0f);
        data.setEntropyB(0f);
        data.clearOvercastTrauma();
        data.setCastSuccessStreak(0);
        data.setSteamFlightActive(false);
        data.setSteamFlightDrainPerTick(0f);
    }

    /** Strip exhaustion-driven status effects from a living player. */
    public static void stripExhaustionEffects(ServerPlayer player) {
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.DARKNESS);
        player.removeEffect(MobEffects.HUNGER);
    }

    public static void tick(ServerPlayer player, PlayerPsiData data) {
        long now = player.level().getGameTime();
        if (!data.hasOvercastTrauma(now) && data.exhaustion() <= 0f) {
            return;
        }

        float decay = BalanceConfig.EXHAUSTION_DECAY_PER_TICK.get().floatValue();
        if (BreathingService.isMeditating(player) && player.getAirSupply() >= player.getMaxAirSupply() - 10) {
            decay += BalanceConfig.EXHAUSTION_MEDITATION_DECAY_BONUS.get().floatValue();
        }
        // Trauma decays slower while active.
        if (data.hasOvercastTrauma(now)) {
            decay *= 0.45f;
        }
        if (decay > 0f && data.exhaustion() > 0f) {
            decay *= RaceTraitsService.exhaustionDecayMultiplier(player);
            data.setExhaustion(Math.max(0f, data.exhaustion() - decay));
        }
        applyMobEffects(player, data, now);
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

    public static void addExhaustion(PlayerPsiData data, float amount) {
        if (amount <= 0f) {
            return;
        }
        float scaled = amount;
        if (data.race().orElse(null) == PlayerRace.ORC) {
            scaled *= 0.90f;
        }
        data.setExhaustion(Math.min(MAX, data.exhaustion() + scaled));
    }

    public static void applyCollapseCastDamage(ServerPlayer player, PlayerPsiData data) {
        if (band(data.exhaustion()) != Band.COLLAPSING) {
            return;
        }
        float damage = BalanceConfig.EXHAUSTION_COLLAPSE_CAST_DAMAGE.get().floatValue();
        if (damage > 0f) {
            player.hurt(player.level().damageSources().magic(), damage);
        }
    }

    private static void applyMobEffects(ServerPlayer player, PlayerPsiData data, long gameTime) {
        boolean trauma = data.hasOvercastTrauma(gameTime);
        Band b = band(data.exhaustion());
        if (!trauma && b == Band.NONE) {
            return;
        }
        // Overcast trauma forces at least Strained visuals.
        if (trauma && b == Band.NONE) {
            b = Band.STRAINED;
        } else if (trauma && b == Band.TIRED) {
            b = Band.STRAINED;
        }

        switch (b) {
            case TIRED -> player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, 0, false, false, true));
            case STRAINED -> {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, 1, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 0, false, false, true));
                if (trauma) {
                    player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 25, 0, false, false, true));
                }
            }
            case COLLAPSING -> {
                int amp = trauma ? 2 : 1;
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, amp, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, amp, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 25, 0, false, false, true));
                if (trauma) {
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 25, 0, false, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 25, 1, false, false, true));
                }
            }
            default -> {}
        }
    }
}
