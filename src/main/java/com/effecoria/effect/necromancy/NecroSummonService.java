package com.effecoria.effect.necromancy;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Permanent thralls bound to a necromancer. Stay near the owner and only engage
 * enemies the owner can see (no cave-pushing through walls).
 * Army size/strength is gated by breathing mastery (control budget).
 */
public final class NecroSummonService {
    public static final String OWNER_TAG = "effecoria:necro_owner";
    public static final String TARGET_TAG = "effecoria:necro_target";
    public static final String RESERVE_TAG = "effecoria:psi_reserve";

    /** Soft leash — thralls try to stay within this of the owner. */
    private static final double LEASH = 5.5;
    /** Hard leash — teleport back if farther. */
    private static final double TELEPORT = 100.0;
    /** Only engage threats within this of the owner, with line of sight. */
    private static final double ENGAGE_RANGE = 14.0;
    /** Hostile aggro radius toward thralls. */
    private static final double PROVOKE_RANGE = 16.0;

    public enum ControlBlock {
        TOO_STRONG,
        TOO_MANY,
        BUDGET,
        PSI
    }

    private NecroSummonService() {}

    /** @return false if the caster cannot afford the given Ψ reserve / control budget */
    public static boolean register(Mob mob, ServerPlayer owner, LivingEntity preferredTarget, float reservePsi) {
        float reserve = Math.max(1f, reservePsi);
        EntityType<?> type = mob.getType();
        boolean spiderTribute = isSpiderTribute(type);
        boolean beyondCaps = spiderTribute && wouldExceedCaps(owner, reserve);

        Optional<ControlBlock> block = diagnoseControl(owner, reserve, type);
        if (block.isPresent()) {
            mob.discard();
            owner.displayClientMessage(controlMessage(block.get(), owner, reserve), true);
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
        PlayerPsiData data = PsiHelper.get(owner);
        data.trackThrall(mob.getUUID());
        PsiHelper.set(owner, data);
        DeathMarkService.syncReservedPsi(owner);
        if (beyondCaps) {
            owner.displayClientMessage(Component.translatable("message.effecoria.necro.spider_tribute"), true);
        }
        return true;
    }

    public static boolean canAfford(ServerPlayer owner, float reserveCost) {
        return diagnoseControl(owner, reserveCost).isEmpty();
    }

    public static boolean canAfford(ServerPlayer owner, float reserveCost, EntityType<?> type) {
        return diagnoseControl(owner, reserveCost, type).isEmpty();
    }

    public static Optional<ControlBlock> diagnoseControl(ServerPlayer owner, float reserveCost) {
        return diagnoseControl(owner, reserveCost, null);
    }

    public static Optional<ControlBlock> diagnoseControl(ServerPlayer owner, float reserveCost, EntityType<?> type) {
        PlayerPsiData data = PsiHelper.get(owner);
        float cost = Math.max(0f, reserveCost);
        boolean spiderTribute = isSpiderTribute(type);

        if (cost > maxSingleHp(data) + 0.05f) {
            return Optional.of(ControlBlock.TOO_STRONG);
        }
        if (!spiderTribute && countOwned(owner) >= maxThralls(data)) {
            return Optional.of(ControlBlock.TOO_MANY);
        }
        float next = reservedPsi(owner) + cost;
        if (!spiderTribute && next > controlBudget(data) + 0.05f) {
            return Optional.of(ControlBlock.BUDGET);
        }
        if (cost > usablePsi(owner, data) + 0.05f || next > data.maxPsi() - 1f) {
            return Optional.of(ControlBlock.PSI);
        }
        return Optional.empty();
    }

    private static boolean wouldExceedCaps(ServerPlayer owner, float reserveCost) {
        PlayerPsiData data = PsiHelper.get(owner);
        float cost = Math.max(0f, reserveCost);
        if (countOwned(owner) >= maxThralls(data)) {
            return true;
        }
        return reservedPsi(owner) + cost > controlBudget(data) + 0.05f;
    }

    private static boolean isSpiderTribute(EntityType<?> type) {
        return type == EntityType.SPIDER || type == EntityType.CAVE_SPIDER;
    }

    public static Component controlMessage(ControlBlock block, ServerPlayer owner, float reserveCost) {
        PlayerPsiData data = PsiHelper.get(owner);
        return switch (block) {
            case TOO_STRONG -> Component.translatable(
                    "message.effecoria.necro.control_too_strong",
                    (int) Math.ceil(reserveCost),
                    (int) Math.floor(maxSingleHp(data)));
            case TOO_MANY -> Component.translatable(
                    "message.effecoria.necro.control_too_many", maxThralls(data));
            case BUDGET -> Component.translatable(
                    "message.effecoria.necro.control_budget",
                    (int) Math.ceil(reservedPsi(owner)),
                    (int) Math.floor(controlBudget(data)),
                    (int) Math.ceil(reserveCost));
            case PSI -> Component.translatable(
                    "message.effecoria.necro.summon_psi_reserve", (int) Math.ceil(reserveCost));
        };
    }

    public static float controlBudget(PlayerPsiData data) {
        float mastery = Math.max(0f, data.breathingMastery());
        return BalanceConfig.NECRO_CONTROL_BUDGET_BASE.get().floatValue()
                + mastery * BalanceConfig.NECRO_CONTROL_BUDGET_PER_MASTERY.get().floatValue();
    }

    public static float maxSingleHp(PlayerPsiData data) {
        float mastery = Math.max(0f, data.breathingMastery());
        return BalanceConfig.NECRO_MAX_SINGLE_HP_BASE.get().floatValue()
                + mastery * BalanceConfig.NECRO_MAX_SINGLE_HP_PER_MASTERY.get().floatValue();
    }

    public static int maxThralls(PlayerPsiData data) {
        float mastery = Math.max(0f, data.breathingMastery());
        int base = BalanceConfig.NECRO_MAX_THRALLS_BASE.get();
        int extra = (int) Math.floor(mastery * BalanceConfig.NECRO_MAX_THRALLS_PER_MASTERY.get().floatValue());
        return Math.max(1, base + extra);
    }

    public static float reservedPsi(Player owner) {
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

    public static float usablePsi(Player owner, PlayerPsiData data) {
        return Math.max(0f, data.currentPsi() - reservedPsi(owner));
    }

    public static int countOwned(Player owner) {
        return listOwned(owner).size();
    }

    /**
     * Resolves thralls from the owner's server-side UUID ledger across all dimensions.
     * Prunes missing/dead IDs and resyncs reserved Ψ when the ledger changes.
     */
    public static List<Mob> listOwned(Player owner) {
        if (!(owner instanceof ServerPlayer serverOwner)) {
            return List.of();
        }
        MinecraftServer server = serverOwner.getServer();
        if (server == null) {
            return List.of();
        }

        PlayerPsiData data = PsiHelper.get(serverOwner);
        List<UUID> ledger = data.necroThrallIds();
        List<Mob> owned = new ArrayList<>();
        List<UUID> stale = new ArrayList<>();

        for (UUID id : new ArrayList<>(ledger)) {
            Mob found = findThrall(server, id);
            if (found == null || !found.isAlive() || !isOwnedBy(found, serverOwner.getUUID())) {
                stale.add(id);
                continue;
            }
            owned.add(found);
        }

        if (!stale.isEmpty()) {
            for (UUID id : stale) {
                data.untrackThrall(id);
            }
            float total = 0f;
            for (Mob mob : owned) {
                total += reserveOf(mob);
            }
            data.setNecroReservedPsi(total);
            PsiHelper.set(serverOwner, data);
        }
        return owned;
    }

    private static Mob findThrall(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity instanceof Mob mob) {
                return mob;
            }
        }
        return null;
    }

    /** Backfill ledger entries for thralls that predate the UUID list. */
    private static void seedNearbyThralls(ServerPlayer owner) {
        PlayerPsiData data = PsiHelper.get(owner);
        boolean changed = false;
        AABB box = owner.getBoundingBox().inflate(48);
        for (Mob mob : owner.level().getEntitiesOfClass(Mob.class, box, LivingEntity::isAlive)) {
            if (isOwnedBy(mob, owner.getUUID()) && !data.necroThrallIds().contains(mob.getUUID())) {
                data.trackThrall(mob.getUUID());
                changed = true;
            }
        }
        if (changed) {
            PsiHelper.set(owner, data);
        }
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
        if (owner.tickCount % 40 == 0) {
            seedNearbyThralls(owner);
        }
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

            provokeNearbyHostiles(mob);
        }
        if (owner.tickCount % 10 == 0) {
            DeathMarkService.syncReservedPsi(owner);
        }
    }

    /**
     * Nearby hostiles with line of sight aggro onto the thrall if idle or not already
     * fighting a player. Skips other thralls of the same owner.
     */
    private static void provokeNearbyHostiles(Mob thrall) {
        if (!thrall.getPersistentData().hasUUID(OWNER_TAG)) {
            return;
        }
        UUID ownerId = thrall.getPersistentData().getUUID(OWNER_TAG);
        AABB box = thrall.getBoundingBox().inflate(PROVOKE_RANGE);
        for (Monster monster : thrall.level().getEntitiesOfClass(Monster.class, box, LivingEntity::isAlive)) {
            if (monster == thrall || isOwnedBy(monster, ownerId)) {
                continue;
            }
            LivingEntity target = monster.getTarget();
            if (target instanceof Player) {
                continue;
            }
            if (!monster.hasLineOfSight(thrall)) {
                continue;
            }
            monster.setTarget(thrall);
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
