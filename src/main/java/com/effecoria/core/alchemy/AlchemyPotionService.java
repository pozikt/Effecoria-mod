package com.effecoria.core.alchemy;

import com.effecoria.content.ModMobEffects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Reads active Φ-alchemy buffs / crash for Ψ regen and Φ sample multipliers. */
public final class AlchemyPotionService {
    private AlchemyPotionService() {}

    public static float regenMultiplier(LivingEntity entity) {
        float mult = 1f;
        if (entity.hasEffect(ModMobEffects.PHI_TONIC)) {
            mult *= 1.35f;
        }
        if (entity.hasEffect(ModMobEffects.PHI_STIMULANT)) {
            mult *= 1.5f;
        }
        MobEffectInstance crash = entity.getEffect(ModMobEffects.ALCHEMY_CRASH);
        if (crash != null) {
            mult *= AlchemyCrashKind.fromAmplifier(crash.getAmplifier()).regenMultiplier();
        }
        return mult;
    }

    public static float phiMultiplier(LivingEntity entity) {
        float mult = 1f;
        if (entity.hasEffect(ModMobEffects.PHI_RESONANCE)) {
            mult *= 1.25f;
        }
        if (entity.hasEffect(ModMobEffects.PHI_STIMULANT)) {
            mult *= 1.35f;
        }
        MobEffectInstance crash = entity.getEffect(ModMobEffects.ALCHEMY_CRASH);
        if (crash != null) {
            mult *= AlchemyCrashKind.fromAmplifier(crash.getAmplifier()).phiMultiplier();
        }
        return mult;
    }

    public static void applyCrash(Player player, AlchemyCrashKind kind) {
        player.addEffect(new MobEffectInstance(
                ModMobEffects.ALCHEMY_CRASH, kind.durationTicks(), kind.amplifier(), false, true, true));
    }
}
