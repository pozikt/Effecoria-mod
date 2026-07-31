package com.effecoria.core.formula;

import com.effecoria.effect.mental.MentalCompulsionService;
import com.effecoria.effect.necromancy.NecroSummonService;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Spell damage attribution and aggro. Magic/wither hits name the caster so mobs fight back.
 * Necro thralls are will-less and never acquire their own targets from this path.
 */
public final class SpellCombat {
    private SpellCombat() {}

    public static DamageSource magic(LivingEntity caster) {
        if (caster == null) {
            throw new IllegalArgumentException("caster");
        }
        return caster.level().damageSources().source(DamageTypes.MAGIC, caster);
    }

    public static DamageSource wither(LivingEntity caster) {
        if (caster == null) {
            throw new IllegalArgumentException("caster");
        }
        return caster.level().damageSources().source(DamageTypes.WITHER, caster);
    }

    public static DamageSource magicOrAnonymous(LivingEntity caster) {
        if (caster == null) {
            return null;
        }
        return magic(caster);
    }

    /** Hurt with caster-attributed magic damage and provoke aggro. */
    public static boolean hurtMagic(LivingEntity caster, LivingEntity target, float amount) {
        if (target == null || amount <= 0f) {
            return false;
        }
        if (caster == null) {
            return target.hurt(target.level().damageSources().magic(), amount);
        }
        boolean hit = target.hurt(magic(caster), amount);
        alert(target, caster);
        return hit;
    }

    /** Hurt with caster-attributed wither damage and provoke aggro. */
    public static boolean hurtWither(LivingEntity caster, LivingEntity target, float amount) {
        if (target == null || amount <= 0f) {
            return false;
        }
        if (caster == null) {
            return target.hurt(target.level().damageSources().wither(), amount);
        }
        boolean hit = target.hurt(wither(caster), amount);
        alert(target, caster);
        return hit;
    }

    /**
     * Make {@code victim} treat {@code attacker} as a threat.
     * Skips necro thralls (no free will) and mobs under mental compulsion.
     */
    public static void alert(LivingEntity victim, LivingEntity attacker) {
        if (victim == null || attacker == null || victim == attacker || !victim.isAlive()) {
            return;
        }
        if (!(victim instanceof Mob mob)) {
            return;
        }
        if (NecroSummonService.isNecroThrall(mob)) {
            return;
        }
        if (MentalCompulsionService.hasActive(mob)) {
            return;
        }
        if (attacker instanceof ServerPlayer player && NecroSummonService.isOwnedBy(mob, player.getUUID())) {
            return;
        }
        mob.setLastHurtByMob(attacker);
        LivingEntity current = mob.getTarget();
        if (current == null || !current.isAlive()) {
            mob.setTarget(attacker);
        }
    }

    /** Alert using active spell cast caster when present. */
    public static void alertFromCast(LivingEntity victim) {
        ServerPlayer caster = BreathDebuffs.currentCaster();
        if (caster != null) {
            alert(victim, caster);
        }
    }
}
