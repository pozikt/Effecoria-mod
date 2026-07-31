package com.effecoria.effect.necromancy;

import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Permanent thralls bound to a necromancer. Each alive thrall reserves Ψ equal to its max health
 * (or a stored reserve amount from Death Mark raise).
 */
public final class NecroSummonService {
    public static final String OWNER_TAG = "effecoria:necro_owner";
    public static final String TARGET_TAG = "effecoria:necro_target";
    public static final String RESERVE_TAG = "effecoria:psi_reserve";

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
        if (preferredTarget != null && preferredTarget.isAlive() && preferredTarget != owner) {
            mob.setTarget(preferredTarget);
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
        // Prefer networked value on client (PersistentData on thralls is not synced).
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
        AABB box = owner.getBoundingBox().inflate(96);
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

    public static void tick(ServerPlayer owner) {
        ServerLevel level = owner.serverLevel();
        LivingEntity combatFocus = resolveOwnerCombatFocus(owner);
        for (Mob mob : listOwned(owner)) {
            // Undead thralls should not burn in daylight under necro control.
            if (mob.isOnFire() && level.isDay() && level.canSeeSky(mob.blockPosition())) {
                mob.clearFire();
            }

            LivingEntity current = mob.getTarget();
            if (current == owner || (current instanceof Mob other && isOwnedBy(other, owner.getUUID()))) {
                mob.setTarget(null);
                current = null;
            }

            LivingEntity preferred = combatFocus;
            if (preferred == null && mob.getPersistentData().hasUUID(TARGET_TAG)) {
                UUID targetId = mob.getPersistentData().getUUID(TARGET_TAG);
                if (level.getEntity(targetId) instanceof LivingEntity living
                        && isValidFocus(owner, living)) {
                    preferred = living;
                }
            }

            LivingEntity next = preferred != null ? preferred : findHostile(owner, mob);
            if (next != null) {
                assignFocus(mob, next);
                if (mob instanceof Vex vex) {
                    vex.setAggressive(true);
                }
            } else {
                mob.setTarget(null);
                followOwner(mob, owner);
            }
        }
        DeathMarkService.syncReservedPsi(owner);
    }

    /** Prefer retaliating against whoever hurt the necromancer, else whoever they last struck. */
    private static LivingEntity resolveOwnerCombatFocus(ServerPlayer owner) {
        LivingEntity attacker = owner.getLastHurtByMob();
        if (isValidFocus(owner, attacker)) {
            return attacker;
        }
        LivingEntity struck = owner.getLastHurtMob();
        if (isValidFocus(owner, struck)) {
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

    /** Push the necromancer's current fight onto all thralls. */
    public static void syncCombatFocus(ServerPlayer owner, LivingEntity focus) {
        if (!isValidFocus(owner, focus)) {
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

    private static LivingEntity findHostile(ServerPlayer owner, Mob thrall) {
        AABB box = thrall.getBoundingBox().inflate(24);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Monster monster : thrall.level().getEntitiesOfClass(Monster.class, box, LivingEntity::isAlive)) {
            if (monster == thrall) {
                continue;
            }
            if (isOwnedBy(monster, owner.getUUID())) {
                continue;
            }
            LivingEntity theirTarget = monster.getTarget();
            boolean prioritizesOwner = theirTarget == owner;
            double dist = monster.distanceToSqr(thrall);
            if (prioritizesOwner) {
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
        if (dist > 12 * 12) {
            mob.teleportTo(owner.getX() + 0.5, owner.getY(), owner.getZ() + 0.5);
        } else if (dist > 4 * 4) {
            mob.getNavigation().moveTo(owner, 1.15);
        }
    }

    /** Mild combat buff without rewriting max health (reserve is tied to HP). */
    private static void lightlyBuff(Mob mob) {
        if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            double damage = mob.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
            mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.max(damage, damage * 1.1));
        }
        if (mob.getAttribute(Attributes.FOLLOW_RANGE) != null) {
            double range = mob.getAttribute(Attributes.FOLLOW_RANGE).getBaseValue();
            mob.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(Math.max(range, 28.0));
        }
    }
}
