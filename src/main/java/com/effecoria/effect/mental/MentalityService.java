package com.effecoria.effect.mental;

import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.core.progression.BreathingService;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.effect.common.CommonWardService;
import com.effecoria.effect.necromancy.NecroSummonService;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.phys.AABB;

/**
 * Mentality tiers for mental-school balance.
 *
 * <ul>
 *   <li>{@link Kind#BEAST} — animals / vermin / wild fauna: always afflicted, never break free</li>
 *   <li>{@link Kind#HUMANOID} — undead, villagers, piglins, hoglins, etc.: can resist and break out
 *       based on max HP will vs caster breathing mastery</li>
 *   <li>{@link Kind#CONSTRUCT} — golems, summons, bosses without a malleable mind: full immunity</li>
 * </ul>
 */
public final class MentalityService {
    public static final String AFFLICT_UNTIL_TAG = "effecoria:mental_afflict_until";
    public static final String AFFLICT_MASTERY_TAG = "effecoria:mental_afflict_mastery";
    public static final String AFFLICT_OWNER_TAG = "effecoria:mental_afflict_owner";
    /** Mental shield — blocks empathic scan / deep probe while active. */
    public static final String SHIELD_UNTIL_TAG = "effecoria:mental_shield_until";
    /** Mind blank — keep mob target cleared for a short window. */
    public static final String BLANK_UNTIL_TAG = "effecoria:mental_blank_until";

    public enum Kind {
        BEAST,
        HUMANOID,
        CONSTRUCT
    }

    private MentalityService() {}

    public static void setShield(LivingEntity entity, long untilGameTime) {
        entity.getPersistentData().putLong(SHIELD_UNTIL_TAG, untilGameTime);
    }

    public static boolean hasShield(LivingEntity entity, long gameTime) {
        return entity.getPersistentData().getLong(SHIELD_UNTIL_TAG) > gameTime;
    }

    public static void applyBlank(Mob mob, int durationTicks) {
        long until = mob.level().getGameTime() + Math.max(1, durationTicks);
        mob.getPersistentData().putLong(BLANK_UNTIL_TAG, until);
        mob.setTarget(null);
        mob.getNavigation().stop();
    }

    public static boolean hasBlank(LivingEntity entity, long gameTime) {
        return entity.getPersistentData().getLong(BLANK_UNTIL_TAG) > gameTime;
    }

    public static void clearBlank(LivingEntity entity) {
        entity.getPersistentData().remove(BLANK_UNTIL_TAG);
    }

    public static void tickBlank(Mob mob, long gameTime) {
        long until = mob.getPersistentData().getLong(BLANK_UNTIL_TAG);
        if (until <= 0L) {
            return;
        }
        if (gameTime > until) {
            clearBlank(mob);
            return;
        }
        if (mob.getTarget() != null) {
            mob.setTarget(null);
        }
        mob.getNavigation().stop();
    }

    public static Kind of(LivingEntity entity) {
        if (entity instanceof Player) {
            return Kind.HUMANOID;
        }

        // Raised thralls / bound summons — no living mind to seize
        if (entity.getPersistentData().hasUUID(NecroSummonService.OWNER_TAG)) {
            return Kind.CONSTRUCT;
        }

        if (entity instanceof IronGolem
                || entity instanceof SnowGolem
                || entity instanceof Shulker
                || entity instanceof Vex
                || entity instanceof Allay
                || entity instanceof Blaze
                || entity instanceof Warden
                || entity instanceof WitherBoss
                || entity instanceof EnderDragon) {
            return Kind.CONSTRUCT;
        }

        // Minded humanoids / near-humanoids (can resist)
        if (entity.getType().is(EntityTypeTags.UNDEAD)
                || entity instanceof AbstractVillager
                || entity instanceof Raider
                || entity instanceof AbstractPiglin
                || entity instanceof Hoglin
                || entity instanceof Witch
                || entity instanceof EnderMan
                || entity instanceof SkeletonHorse
                || entity instanceof ZombieHorse) {
            return Kind.HUMANOID;
        }

        // Beasts, vermin, simple monsters — open minds
        if (entity instanceof Animal
                || entity instanceof WaterAnimal
                || entity instanceof Spider
                || entity instanceof Silverfish
                || entity instanceof Endermite
                || entity instanceof Slime
                || entity instanceof MagmaCube
                || entity instanceof Creeper
                || entity instanceof Ghast
                || entity instanceof Guardian) {
            return Kind.BEAST;
        }

        MobCategory category = entity.getType().getCategory();
        if (category == MobCategory.CREATURE
                || category == MobCategory.AMBIENT
                || category == MobCategory.WATER_CREATURE
                || category == MobCategory.WATER_AMBIENT
                || category == MobCategory.UNDERGROUND_WATER_CREATURE) {
            return Kind.BEAST;
        }

        // Remaining monsters default to willful minds
        if (category == MobCategory.MONSTER) {
            return Kind.HUMANOID;
        }

        return Kind.BEAST;
    }

    public static boolean isImmune(LivingEntity entity) {
        return of(entity) == Kind.CONSTRUCT;
    }

    /** Willpower proxy from max HP (20 HP ≈ 1.0). */
    public static float willStrength(LivingEntity entity) {
        return Mth.clamp(entity.getMaxHealth() / 20f, 0.35f, 5f);
    }

    /**
     * Chance the target shrugs off a new mental affliction (0 for beasts / immune handled apart).
     */
    public static float resistChance(LivingEntity target, float casterMastery) {
        if (of(target) != Kind.HUMANOID) {
            return 0f;
        }
        float will = willStrength(target);
        float mastery = BreathingService.referenceRatio(casterMastery);
        float armor = com.effecoria.armor.EssoniteArmorService.mentalResistBonus(target);
        return Mth.clamp(0.08f + will * 0.10f - mastery * 0.45f + armor, 0.02f, 0.85f);
    }

    /** Per-second-ish breakout chance while afflicted (checked every 20 ticks). */
    public static float breakoutChance(LivingEntity target, float casterMastery) {
        if (of(target) != Kind.HUMANOID) {
            return 0f;
        }
        float will = willStrength(target);
        float mastery = BreathingService.referenceRatio(casterMastery);
        return Mth.clamp(0.018f + will * 0.014f - mastery * 0.055f, 0.004f, 0.14f);
    }

    /**
     * Gate for applying mental control / debuffs to another creature.
     *
     * @return false if immune or resisted
     */
    public static boolean tryAfflict(ServerPlayer caster, LivingEntity target) {
        return tryAfflict(caster, target, -1);
    }

    /**
     * @param durationHint ticks to track for breakout sweeps; {@code <= 0} uses a short default window
     */
    public static boolean tryAfflict(ServerPlayer caster, LivingEntity target, int durationHint) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (target == caster && !BreathDebuffs.allowSelfTarget()) {
            return false;
        }
        if (CommonWardService.hasWard(target, target.level().getGameTime())
                || hasShield(target, target.level().getGameTime())) {
            return false;
        }
        Kind kind = of(target);
        if (kind == Kind.CONSTRUCT) {
            return false;
        }
        float mastery = PsiHelper.get(caster).breathingMastery();
        if (kind == Kind.HUMANOID) {
            float resist = resistChance(target, mastery);
            if (target.getRandom().nextFloat() < resist) {
                return false;
            }
        }
        int window = durationHint > 0 ? durationHint : 100;
        markAfflicted(target, caster, mastery, window);
        return true;
    }

    public static void markAfflicted(
            LivingEntity target, ServerPlayer caster, float casterMastery, int durationTicks) {
        var data = target.getPersistentData();
        long until = target.level().getGameTime() + Math.max(20, durationTicks);
        if (data.contains(AFFLICT_UNTIL_TAG)) {
            until = Math.max(until, data.getLong(AFFLICT_UNTIL_TAG));
        }
        data.putLong(AFFLICT_UNTIL_TAG, until);
        data.putFloat(AFFLICT_MASTERY_TAG, casterMastery);
        data.putUUID(AFFLICT_OWNER_TAG, caster.getUUID());
    }

    public static boolean isAfflicted(LivingEntity entity) {
        var data = entity.getPersistentData();
        if (!data.contains(AFFLICT_UNTIL_TAG)) {
            return false;
        }
        if (entity.level().getGameTime() > data.getLong(AFFLICT_UNTIL_TAG)) {
            clearAfflict(entity);
            return false;
        }
        return true;
    }

    public static void clearAfflict(LivingEntity entity) {
        var data = entity.getPersistentData();
        data.remove(AFFLICT_UNTIL_TAG);
        data.remove(AFFLICT_MASTERY_TAG);
        data.remove(AFFLICT_OWNER_TAG);
    }

    /** Strip mental control + typical mental potion clutter. */
    public static void purgeMentalEffects(LivingEntity entity) {
        MentalCompulsionService.clear(entity);
        clearAfflict(entity);
        clearBlank(entity);
        entity.removeEffect(MobEffects.CONFUSION);
        entity.removeEffect(MobEffects.BLINDNESS);
        entity.removeEffect(MobEffects.WEAKNESS);
        entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        entity.removeEffect(MobEffects.DIG_SLOWDOWN);
        entity.removeEffect(MobEffects.LEVITATION);
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }

    public static boolean tryBreakout(LivingEntity entity) {
        if (!isAfflicted(entity) && !MentalCompulsionService.hasActive(entity)) {
            return false;
        }
        if (of(entity) != Kind.HUMANOID) {
            return false;
        }
        float mastery = entity.getPersistentData().contains(AFFLICT_MASTERY_TAG)
                ? entity.getPersistentData().getFloat(AFFLICT_MASTERY_TAG)
                : 0f;
        if (entity.getRandom().nextFloat() >= breakoutChance(entity, mastery)) {
            return false;
        }
        java.util.UUID ownerId = entity.getPersistentData().hasUUID(AFFLICT_OWNER_TAG)
                ? entity.getPersistentData().getUUID(AFFLICT_OWNER_TAG)
                : (entity.getPersistentData().hasUUID(MentalCompulsionService.OWNER_TAG)
                        ? entity.getPersistentData().getUUID(MentalCompulsionService.OWNER_TAG)
                        : null);
        purgeMentalEffects(entity);
        if (ownerId != null && entity.level() instanceof ServerLevel level) {
            if (level.getPlayerByUUID(ownerId) instanceof ServerPlayer caster) {
                caster.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "message.effecoria.mental.broke_free", entity.getDisplayName()),
                        true);
            }
        }
        return true;
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        if (now % 2 == 0) {
            for (ServerPlayer player : level.players()) {
                AABB box = player.getBoundingBox().inflate(48);
                for (Mob mob : level.getEntitiesOfClass(Mob.class, box, m -> hasBlank(m, now))) {
                    tickBlank(mob, now);
                }
            }
        }
        if (now % 20 == 0) {
            for (ServerPlayer player : level.players()) {
                AABB box = player.getBoundingBox().inflate(48);
                for (Mob mob : level.getEntitiesOfClass(Mob.class, box, LivingEntity::isAlive)) {
                    if (!isAfflicted(mob) && !MentalCompulsionService.hasActive(mob) && !hasBlank(mob, now)) {
                        continue;
                    }
                    tryBreakout(mob);
                }
            }
        }
        if (now % 10 == 0) {
            for (ServerPlayer player : level.players()) {
                AABB box = player.getBoundingBox().inflate(32);
                for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, box, e -> true)) {
                    var data = stand.getPersistentData();
                    if (!data.contains("effecoria:psi_echo_until")) {
                        continue;
                    }
                    if (now >= data.getLong("effecoria:psi_echo_until")) {
                        stand.discard();
                    }
                }
            }
        }
    }

    public static void notifyFail(ServerPlayer caster, LivingEntity target) {
        if (isImmune(target)) {
            caster.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.effecoria.mental.immune", target.getDisplayName()),
                    true);
        } else {
            caster.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.effecoria.mental.resisted", target.getDisplayName()),
                    true);
        }
    }
}
