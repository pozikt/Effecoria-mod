package com.effecoria.core.magic;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;

import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.core.formula.SpellCombat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

/**
 * Lightweight initiation for vanilla/mod mobs — store a school and let them fire demo casts in combat.
 * Natural spawns roll {@link com.effecoria.config.BalanceConfig#MOB_MAGIC_SPAWN_CHANCE}.
 * For QA: {@code /effecoria initiateMob}.
 */
public final class MobMagicService {
    public static final String TAG = "effecoria:mob_magic";
    public static final String GOAL_TAG = "effecoria:mob_magic_goal";
    /** Set after the natural-spawn magic roll so we never re-roll the same mob. */
    public static final String ROLLED_TAG = "effecoria:mob_magic_rolled";

    private static final List<MagicSchool> MOB_SCHOOLS = Arrays.stream(MagicSchool.values())
            .filter(s -> s.isPlayable() && s != MagicSchool.SEALS)
            .toList();

    private MobMagicService() {}

    public static boolean isInitiated(LivingEntity entity) {
        return entity.getPersistentData().contains(TAG, Tag.TAG_COMPOUND);
    }

    public static MagicSchool schoolOf(LivingEntity entity) {
        if (!isInitiated(entity)) {
            return MagicSchool.NONE;
        }
        return MagicSchool.fromSerializedName(entity.getPersistentData().getCompound(TAG).getString("School"));
    }

    public static MagicSchool randomSchool(RandomSource random) {
        return MOB_SCHOOLS.get(random.nextInt(MOB_SCHOOLS.size()));
    }

    /**
     * One-shot roll for natural hostile spawns. Marks the mob as rolled whether or not it initiates.
     *
     * @return true if the mob became a mage
     */
    public static boolean tryNaturalInitiate(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (data.getBoolean(ROLLED_TAG) || isInitiated(mob)) {
            return false;
        }
        data.putBoolean(ROLLED_TAG, true);
        if (!(mob instanceof Enemy)) {
            return false;
        }
        float chance = BalanceConfig.MOB_MAGIC_SPAWN_CHANCE.get().floatValue();
        if (chance <= 0f || mob.getRandom().nextFloat() >= chance) {
            return false;
        }
        initiate(mob, randomSchool(mob.getRandom()), false);
        return true;
    }

    public static void initiate(Mob mob, MagicSchool school) {
        initiate(mob, school, true);
    }

    public static void initiate(Mob mob, MagicSchool school, boolean dramatic) {
        if (school == null || school == MagicSchool.NONE || school == MagicSchool.COMMON) {
            school = randomSchool(mob.getRandom());
        }
        if (school == MagicSchool.SEALS) {
            school = MagicSchool.MENTAL;
        }

        CompoundTag tag = new CompoundTag();
        tag.putString("School", school.getSerializedName());
        tag.putLong("InitiatedAt", mob.level().getGameTime());
        mob.getPersistentData().put(TAG, tag);
        mob.getPersistentData().putBoolean(ROLLED_TAG, true);

        if (dramatic) {
            BreathDebuffs.apply(mob, new MobEffectInstance(MobEffects.GLOWING, 20 * 60 * 10, 0, false, true, true));
        }
        // No nametag — school is internal; HUD/combat FX carry the tell.

        ensureCastGoal(mob);
    }

    public static void ensureCastGoal(LivingEntity entity) {
        if (!(entity instanceof PathfinderMob pathMob)) {
            return;
        }
        // Strip leftover school nametags from older builds.
        if (isInitiated(pathMob) && pathMob.hasCustomName()) {
            pathMob.setCustomName(null);
            pathMob.setCustomNameVisible(false);
        }
        CompoundTag root = pathMob.getPersistentData();
        if (root.getBoolean(GOAL_TAG)) {
            return;
        }
        pathMob.goalSelector.addGoal(3, new MobSpellCastGoal(pathMob));
        root.putBoolean(GOAL_TAG, true);
    }

    /** Fire one school-flavored bolt at {@code target}. Returns false if not initiated / invalid. */
    public static boolean castAt(Mob caster, @Nullable LivingEntity target) {
        if (!isInitiated(caster) || target == null || !target.isAlive() || target == caster) {
            return false;
        }
        if (!(caster.level() instanceof ServerLevel level)) {
            return false;
        }

        MagicSchool school = schoolOf(caster);
        Vec3 from = caster.getEyePosition();
        Vec3 to = target.getEyePosition();
        Vec3 mid = from.add(to.subtract(from).scale(0.5));

        switch (school) {
            case ELEMENTAL -> {
                SpellCombat.hurtMagic(caster, target, 5f);
                BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true, true));
                level.sendParticles(ModParticleTypes.ELEMENTAL_EMBER.get(), mid.x, mid.y, mid.z, 18, 0.25, 0.25, 0.25, 0.04);
                level.playSound(null, caster.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.7f, 1.1f);
            }
            case MENTAL -> {
                SpellCombat.hurtMagic(caster, target, 4f);
                BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, true, true));
                level.sendParticles(ModParticleTypes.MENTAL_SYNAPSE.get(), mid.x, mid.y, mid.z, 14, 0.2, 0.25, 0.2, 0.02);
                level.playSound(null, caster.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.8f, 0.7f);
            }
            case ORGANIC -> {
                SpellCombat.hurtMagic(caster, target, 3.5f);
                BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.POISON, 80, 0, false, true, true));
                level.sendParticles(ModParticleTypes.ORGANIC_SPORE.get(), mid.x, mid.y, mid.z, 16, 0.3, 0.25, 0.3, 0.02);
                level.playSound(null, caster.blockPosition(), SoundEvents.MOSS_PLACE, SoundSource.HOSTILE, 0.9f, 0.9f);
            }
            case NECROMANCY -> {
                SpellCombat.hurtWither(caster, target, 4f);
                BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WITHER, 60, 0, false, true, true));
                level.sendParticles(ModParticleTypes.NECRO_WITHER.get(), mid.x, mid.y, mid.z, 16, 0.25, 0.3, 0.25, 0.03);
                level.playSound(null, caster.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 0.45f, 1.4f);
            }
            case SPATIAL -> {
                SpellCombat.hurtMagic(caster, target, 3f);
                Vec3 away = target.position().subtract(caster.position()).normalize();
                if (away.lengthSqr() < 1.0e-4) {
                    away = caster.getLookAngle();
                }
                target.setDeltaMovement(target.getDeltaMovement().add(away.scale(0.85)).add(0, 0.25, 0));
                target.hurtMarked = true;
                level.sendParticles(ModParticleTypes.PHI_SPARK.get(), mid.x, mid.y, mid.z, 20, 0.2, 0.2, 0.2, 0.05);
                level.playSound(null, caster.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.6f, 1.3f);
            }
            case CORRUPTION -> {
                SpellCombat.hurtMagic(caster, target, 4.5f);
                BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true, true));
                BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WITHER, 40, 0, false, true, true));
                level.sendParticles(ModParticleTypes.CORRUPTION_MIASMA.get(), mid.x, mid.y, mid.z, 18, 0.3, 0.3, 0.3, 0.03);
                level.playSound(null, caster.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 0.35f, 1.5f);
            }
            default -> {
                SpellCombat.hurtMagic(caster, target, 3f);
                level.sendParticles(ModParticleTypes.PHI_SPARK.get(), mid.x, mid.y, mid.z, 12, 0.2, 0.2, 0.2, 0.02);
                level.playSound(null, caster.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE, 0.7f, 1.2f);
            }
        }

        double dx = target.getX() - caster.getX();
        double dz = target.getZ() - caster.getZ();
        caster.setYRot((float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90f);
        caster.yBodyRot = caster.getYRot();
        caster.yHeadRot = caster.getYRot();
        return true;
    }

    /** Combat goal: initiated pathfinder mobs periodically cast at their AI target. */
    public static final class MobSpellCastGoal extends Goal {
        private static final double RANGE = 14.0;
        private static final int COOLDOWN = 45;

        private final PathfinderMob mob;
        private int cooldown;

        public MobSpellCastGoal(PathfinderMob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
            this.cooldown = 10;
        }

        @Override
        public boolean canUse() {
            if (!isInitiated(mob) || --cooldown > 0) {
                return false;
            }
            LivingEntity target = mob.getTarget();
            return target != null
                    && target.isAlive()
                    && mob.distanceToSqr(target) <= RANGE * RANGE
                    && mob.hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = mob.getTarget();
            if (castAt(mob, target)) {
                cooldown = COOLDOWN + mob.getRandom().nextInt(20);
            } else {
                cooldown = 20;
            }
        }
    }
}
