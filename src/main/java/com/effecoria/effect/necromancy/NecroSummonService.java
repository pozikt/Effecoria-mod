package com.effecoria.effect.necromancy;

import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Permanent thralls bound to a necromancer. Stay near the owner and only engage
 * enemies the owner can see (no cave-pushing through walls).
 */
public final class NecroSummonService {
    public static final String OWNER_TAG = "effecoria:necro_owner";
    public static final String TARGET_TAG = "effecoria:necro_target";
    public static final String RESERVE_TAG = "effecoria:psi_reserve";

    /** Soft leash — thralls try to stay within this of the owner. */
    private static final double LEASH = 5.5;
    /** Hard leash — teleport back if farther. */
    private static final double TELEPORT = 10.0;
    /** Only engage threats within this of the owner, with line of sight. */
    private static final double ENGAGE_RANGE = 14.0;

    private NecroSummonService() {}

    /** @return false if the caster cannot afford the given Ψ reserve */
    public static boolean register(Mob mob, ServerPlayer owner, LivingEntity preferredTarget, float reservePsi) {
        float reserve = Math.max(1f, reservePsi);
        if (!canAfford(owner, reserve)) {
            mob.discard();
            owner.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.effecoria.necro.summon_psi_reserve", (int) Math.ceil(reserve)),
                    true);
            return false;
        }
        mob.getPersistentData().putUUID(OWNER_TAG, owner.getUUID());
        mob.getPersistentData().putFloat(RESERVE_TAG, reserve);
        if (preferredTarget != null) {
            mob.getPersistentData().putUUID(TARGET_TAG, preferredTarget.getUUID());
        }
        mob.setPersistenceRequired();
        lightlyBuff(mob);
        stripAutonomousAi(mob);
        if (preferredTarget != null && canEngage(owner, preferredTarget)) {
            assignFocus(mob, preferredTarget);
        } else {
            mob.setTarget(null);
        }
        DeathMarkService.syncReservedPsi(owner);
        return true;
    }

    public static boolean canAfford(ServerPlayer owner, float reserveCost) {
        PlayerPsiData data = PsiHelper.get(owner);
        float nextReserve = reservedPsi(owner) + Math.max(0f, reserveCost);
        return nextReserve <= data.maxPsi() - 1f;
    }

    public static float reservedPsi(net.minecraft.world.entity.player.Player owner) {
        if (owner.level().isClientSide()) {
            return PsiHelper.get(owner).necroReservedPsi();
        }
        float total = 0f;
        for (Mob mob : listOwned(owner)) {
            total += reserveOf(mob);
        }
        return total;
    }

    public static float reserveOf(Mob mob) {
        if (mob.getPersistentData().contains(RESERVE_TAG)) {
            return Math.max(1f, mob.getPersistentData().getFloat(RESERVE_TAG));
        }
        return Math.max(1f, mob.getMaxHealth());
    }

    public static float usablePsi(net.minecraft.world.entity.player.Player owner, PlayerPsiData data) {
        return Math.max(0f, data.currentPsi() - reservedPsi(owner));
    }

    public static int countOwned(net.minecraft.world.entity.player.Player owner) {
        return listOwned(owner).size();
    }

    public static List<Mob> listOwned(net.minecraft.world.entity.player.Player owner) {
        AABB box = owner.getBoundingBox().inflate(48);
        List<Mob> owned = new ArrayList<>();
        for (Mob mob : owner.level().getEntitiesOfClass(Mob.class, box, Mob::isAlive)) {
            if (isOwnedBy(mob, owner.getUUID())) {
                owned.add(mob);
            }
        }
        return owned;
    }

    public static boolean isOwnedBy(Mob mob, UUID ownerId) {
        return mob.getPersistentData().hasUUID(OWNER_TAG)
                && ownerId.equals(mob.getPersistentData().getUUID(OWNER_TAG));
    }

    public static boolean isNecroThrall(LivingEntity entity) {
        return entity instanceof Mob mob && mob.getPersistentData().hasUUID(OWNER_TAG);
    }

    public static boolean sameNecromancer(LivingEntity a, LivingEntity b) {
        if (!(a instanceof Mob ma) || !(b instanceof Mob mb)) {
            return false;
        }
        if (!ma.getPersistentData().hasUUID(OWNER_TAG) || !mb.getPersistentData().hasUUID(OWNER_TAG)) {
            return false;
        }
        return ma.getPersistentData().getUUID(OWNER_TAG).equals(mb.getPersistentData().getUUID(OWNER_TAG));
    }

    /**
     * Whether a thrall may target this living entity: near the owner and visible to the owner
     * (walls / cave ceilings block engagement).
     */
    public static boolean canEngage(ServerPlayer owner, LivingEntity candidate) {
        if (!isValidFocus(owner, candidate)) {
            return false;
        }
        if (owner.distanceToSqr(candidate) > ENGAGE_RANGE * ENGAGE_RANGE) {
            return false;
        }
        return owner.hasLineOfSight(candidate);
    }

    public static void tick(ServerPlayer owner) {
        ServerLevel level = owner.serverLevel();
        LivingEntity combatFocus = resolveOwnerCombatFocus(owner);
        for (Mob mob : listOwned(owner)) {
            if (mob.isOnFire() && level.isDay() && level.canSeeSky(mob.blockPosition())) {
                mob.clearFire();
            }

            // Re-strip in case goals were re-added (rare) or entity reloaded mid-session.
            if (owner.tickCount % 40 == 0) {
                stripAutonomousAi(mob);
            }

            LivingEntity current = mob.getTarget();
            if (current != null && !canEngage(owner, current)) {
                mob.setTarget(null);
                mob.setLastHurtByMob(null);
                current = null;
            }

            LivingEntity preferred = combatFocus;
            if (preferred == null && mob.getPersistentData().hasUUID(TARGET_TAG)) {
                UUID targetId = mob.getPersistentData().getUUID(TARGET_TAG);
                if (level.getEntity(targetId) instanceof LivingEntity living && canEngage(owner, living)) {
                    preferred = living;
                }
            }

            LivingEntity next = preferred != null ? preferred : findVisibleHostile(owner);
            if (next != null && canEngage(owner, next)) {
                assignFocus(mob, next);
                if (mob instanceof Vex vex) {
                    vex.setAggressive(true);
                }
                // Don't chase so far that they leave the owner's side.
                if (mob.distanceToSqr(owner) > LEASH * LEASH * 1.8) {
                    followOwner(mob, owner);
                }
            } else {
                mob.setTarget(null);
                mob.getPersistentData().remove(TARGET_TAG);
                mob.setLastHurtByMob(null);
                followOwner(mob, owner);
            }
        }
        if (owner.tickCount % 10 == 0) {
            DeathMarkService.syncReservedPsi(owner);
        }
    }

    private static LivingEntity resolveOwnerCombatFocus(ServerPlayer owner) {
        LivingEntity attacker = owner.getLastHurtByMob();
        if (canEngage(owner, attacker)) {
            return attacker;
        }
        LivingEntity struck = owner.getLastHurtMob();
        if (canEngage(owner, struck)) {
            return struck;
        }
        return null;
    }

    private static boolean isValidFocus(ServerPlayer owner, LivingEntity candidate) {
        if (candidate == null || !candidate.isAlive() || candidate == owner) {
            return false;
        }
        if (candidate instanceof Mob other && isOwnedBy(other, owner.getUUID())) {
            return false;
        }
        return true;
    }

    /** Push the necromancer's current fight onto all thralls (only if visible). */
    public static void syncCombatFocus(ServerPlayer owner, LivingEntity focus) {
        if (!canEngage(owner, focus)) {
            return;
        }
        for (Mob mob : listOwned(owner)) {
            assignFocus(mob, focus);
        }
    }

    private static void assignFocus(Mob mob, LivingEntity focus) {
        mob.getPersistentData().putUUID(TARGET_TAG, focus.getUUID());
        mob.setTarget(focus);
    }

    /** Hostiles near the owner that the owner can see — never through walls. */
    private static LivingEntity findVisibleHostile(ServerPlayer owner) {
        AABB box = owner.getBoundingBox().inflate(ENGAGE_RANGE);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Monster monster : owner.level().getEntitiesOfClass(Monster.class, box, LivingEntity::isAlive)) {
            if (!canEngage(owner, monster)) {
                continue;
            }
            if (isOwnedBy(monster, owner.getUUID())) {
                continue;
            }
            double dist = monster.distanceToSqr(owner);
            if (monster.getTarget() == owner) {
                dist *= 0.35;
            }
            if (dist < bestDist) {
                bestDist = dist;
                best = monster;
            }
        }
        return best;
    }

    private static void followOwner(Mob mob, ServerPlayer owner) {
        double dist = mob.distanceToSqr(owner);
        if (dist > TELEPORT * TELEPORT) {
            double yaw = Math.toRadians(owner.getYRot());
            double ox = -Math.sin(yaw) * 1.4;
            double oz = Math.cos(yaw) * 1.4;
            mob.teleportTo(owner.getX() + ox, owner.getY(), owner.getZ() + oz);
            mob.getNavigation().stop();
            return;
        }
        if (dist > LEASH * LEASH) {
            mob.getNavigation().moveTo(owner, 1.2);
        } else if (mob.getTarget() == null) {
            mob.getNavigation().stop();
        }
    }

    /** Kill wander / sun-flee / autonomous targeting so thralls only obey our tick. */
    private static void stripAutonomousAi(Mob mob) {
        mob.targetSelector.removeAllGoals(goal -> true);
        List<WrappedGoal> toRemove = new ArrayList<>();
        for (WrappedGoal wrapped : mob.goalSelector.getAvailableGoals()) {
            var goal = wrapped.getGoal();
            if (goal instanceof WaterAvoidingRandomStrollGoal
                    || goal instanceof FleeSunGoal
                    || goal instanceof RestrictSunGoal
                    || goal.getClass().getSimpleName().contains("RandomStroll")
                    || goal.getClass().getSimpleName().contains("Wander")
                    || goal.getClass().getSimpleName().contains("MoveThroughVillage")
                    || goal.getClass().getSimpleName().contains("Patrol")) {
                toRemove.add(wrapped);
            }
        }
        for (WrappedGoal wrapped : toRemove) {
            mob.goalSelector.removeGoal(wrapped.getGoal());
        }
        if (mob.getAttribute(Attributes.FOLLOW_RANGE) != null) {
            mob.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(Math.min(
                    mob.getAttribute(Attributes.FOLLOW_RANGE).getBaseValue(), ENGAGE_RANGE));
        }
    }

    private static void lightlyBuff(Mob mob) {
        if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            double damage = mob.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
            mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.max(damage, damage * 1.1));
        }
        if (mob.getAttribute(Attributes.FOLLOW_RANGE) != null) {
            mob.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(ENGAGE_RANGE);
        }
    }
}
