package com.effecoria.core.formula;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.progression.BreathingService;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/**
 * Scales harmful/neutral potion effects (duration + amplifier) by the caster's breathing mastery.
 * Bind a caster with {@link #beginCast}/{@link #endCast} during spell resolve, or pass the caster explicitly.
 */
public final class BreathDebuffs {
    private static final ThreadLocal<ServerPlayer> CAST_CASTER = new ThreadLocal<>();

    private BreathDebuffs() {}

    public static void beginCast(ServerPlayer caster) {
        CAST_CASTER.set(caster);
    }

    public static void endCast() {
        CAST_CASTER.remove();
    }

    public static ServerPlayer currentCaster() {
        return CAST_CASTER.get();
    }

    public static float durationMultiplier(float breathingMastery) {
        float ratio = BreathingService.referenceRatio(breathingMastery);
        float mult = 1f + ratio * BalanceConfig.DEBUFF_DURATION_PER_REFERENCE.get().floatValue();
        return Math.min(mult, 2.25f);
    }

    public static int scaleDuration(float breathingMastery, int baseTicks) {
        if (baseTicks <= 0) {
            return baseTicks;
        }
        return Math.max(1, Math.round(baseTicks * durationMultiplier(breathingMastery)));
    }

    public static int scaleDuration(ServerPlayer caster, int baseTicks) {
        if (caster == null) {
            return baseTicks;
        }
        return scaleDuration(PsiHelper.get(caster).breathingMastery(), baseTicks);
    }

    public static int scaleAmplifier(float breathingMastery, int baseAmplifier) {
        float ratio = BreathingService.referenceRatio(breathingMastery);
        int bonus = (int) Math.floor(ratio * BalanceConfig.DEBUFF_AMPLIFIER_PER_REFERENCE.get().floatValue());
        bonus = Math.min(bonus, BalanceConfig.DEBUFF_AMPLIFIER_MAX_BONUS.get());
        return Math.min(255, Math.max(0, baseAmplifier + bonus));
    }

    public static int scaleAmplifier(ServerPlayer caster, int baseAmplifier) {
        if (caster == null) {
            return baseAmplifier;
        }
        return scaleAmplifier(PsiHelper.get(caster).breathingMastery(), baseAmplifier);
    }

    public static boolean isScalableDebuff(MobEffectInstance instance) {
        MobEffectCategory category = instance.getEffect().value().getCategory();
        return category == MobEffectCategory.HARMFUL || category == MobEffectCategory.NEUTRAL;
    }

    public static MobEffectInstance scale(ServerPlayer caster, MobEffectInstance source) {
        if (caster == null || source == null || !isScalableDebuff(source)) {
            return source;
        }
        float mastery = PsiHelper.get(caster).breathingMastery();
        return new MobEffectInstance(
                source.getEffect(),
                scaleDuration(mastery, source.getDuration()),
                scaleAmplifier(mastery, source.getAmplifier()),
                source.isAmbient(),
                source.isVisible(),
                source.showIcon());
    }

    /**
     * Applies an effect, scaling harmful/neutral debuffs when {@code target} is not the caster.
     * Uses the active cast caster if {@code caster} is null.
     */
    public static boolean apply(ServerPlayer caster, LivingEntity target, MobEffectInstance instance) {
        if (target == null || instance == null) {
            return false;
        }
        ServerPlayer source = caster != null ? caster : CAST_CASTER.get();
        MobEffectInstance toApply = instance;
        if (source != null && target != source) {
            toApply = scale(source, instance);
            boolean applied = target.addEffect(toApply);
            if (applied && isScalableDebuff(instance)) {
                SpellCombat.alert(target, source);
            }
            return applied;
        }
        return target.addEffect(toApply);
    }

    /** Prefer during spell resolve (cast context bound). */
    public static boolean apply(LivingEntity target, MobEffectInstance instance) {
        return apply(CAST_CASTER.get(), target, instance);
    }

    /** Apply without breathing scale (duration/amp already finalized). */
    public static boolean applyExact(LivingEntity target, MobEffectInstance instance) {
        if (target == null || instance == null) {
            return false;
        }
        return target.addEffect(instance);
    }

    public static boolean apply(ServerLevel level, UUID ownerId, LivingEntity target, MobEffectInstance instance) {
        if (level == null || ownerId == null) {
            return apply((ServerPlayer) null, target, instance);
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        return apply(owner, target, instance);
    }
}
