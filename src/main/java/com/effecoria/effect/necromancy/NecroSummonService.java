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
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
    /** Drunk skeleton — chaotic targeting. */
    public static final String CHAOS_TAG = "effecoria:necro_chaos";
    /** Spirit contract — may break and attack the mage. */
    public static final String CONTRACT_TAG = "effecoria:necro_contract";
    /** Stationary eternal guard. */
    public static final String GUARD_TAG = "effecoria:necro_guard";
    public static final String GUARD_X = "effecoria:necro_guard_x";
    public static final String GUARD_Y = "effecoria:necro_guard_y";
    public static final String GUARD_Z = "effecoria:necro_guard_z";
    /** Multi-soul doll — stronger, unstable. */
    public static final String DOLL_TAG = "effecoria:necro_doll";
    public static final String LOOSE_TAG = "effecoria:necro_loose";

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
        PlayerPsiData data = PsiHelper.get(owner);
        float usable = usablePsi(owner, data);

        for (Mob mob : listOwned(owner)) {
            if (mob.isOnFire() && level.isDay() && level.canSeeSky(mob.blockPosition())) {
                mob.clearFire();
            }

            if (owner.tickCount % 40 == 0) {
                stripAutonomousAi(mob);
            }

            // Eternal guard: hold post; starve → go loose and attack everyone.
            if (mob.getPersistentData().getBoolean(GUARD_TAG)) {
                tickGuard(owner, mob, usable);
                continue;
            }

            // Lost army / broken contract.
            if (mob.getPersistentData().getBoolean(LOOSE_TAG)) {
                tickLoose(mob);
                continue;
            }

            if (mob.getPersistentData().getBoolean(CHAOS_TAG) && owner.tickCount % 35 == 0) {
                tickChaos(owner, mob);
                continue;
            }

            if (mob.getPersistentData().getBoolean(CONTRACT_TAG) && owner.tickCount % 100 == 0) {
                long until = mob.getPersistentData().getLong("effecoria:necro_pact_until");
                if (until > 0 && level.getGameTime() >= until) {
                    breakContract(owner, mob);
                    continue;
                }
                if (owner.getRandom().nextFloat() < 0.08f || usable < 4f) {
                    breakContract(owner, mob);
                    continue;
                }
            }

            if (mob.getPersistentData().getBoolean(DOLL_TAG) && owner.tickCount % 80 == 0) {
                if (owner.getRandom().nextFloat() < 0.2f) {
                    BreathDebuffsProxy.confuse(mob);
                }
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

        // Army control loss when Ψ is drained.
        if (usable < 2f && owner.tickCount % 60 == 0) {
            for (Mob mob : listOwned(owner)) {
                if (mob.getPersistentData().getBoolean(GUARD_TAG)) {
                    continue;
                }
                if (owner.getRandom().nextFloat() < 0.25f) {
                    releaseControl(owner, mob);
                }
            }
        }

        if (owner.tickCount % 10 == 0) {
            DeathMarkService.syncReservedPsi(owner);
        }
    }

    private static void tickGuard(ServerPlayer owner, Mob mob, float usablePsi) {
        double gx = mob.getPersistentData().getDouble(GUARD_X);
        double gy = mob.getPersistentData().getDouble(GUARD_Y);
        double gz = mob.getPersistentData().getDouble(GUARD_Z);
        if (mob.distanceToSqr(gx, gy, gz) > 9.0) {
            mob.getNavigation().moveTo(gx, gy, gz, 1.0);
        }
        // Upkeep — without Ψ the seal dims.
        if (usablePsi < 1.5f) {
            if (owner.getRandom().nextFloat() < 0.15f) {
                mob.getPersistentData().putBoolean(LOOSE_TAG, true);
                mob.getPersistentData().remove(OWNER_TAG);
                PlayerPsiData data = PsiHelper.get(owner);
                data.untrackThrall(mob.getUUID());
                PsiHelper.set(owner, data);
                owner.displayClientMessage(Component.translatable("message.effecoria.necro.guard_loose"), true);
            } else if (owner.tickCount % 40 == 0) {
                // Sleep: freeze in place.
                mob.setTarget(null);
                mob.getNavigation().stop();
                BreathDebuffsProxy.slowness(mob, 50, 6);
            }
            return;
        }
        LivingEntity focus = resolveOwnerCombatFocus(owner);
        if (focus != null && mob.distanceToSqr(focus) < 100) {
            mob.setTarget(focus);
        } else {
            LivingEntity near = findGuardHostile(mob);
            mob.setTarget(near);
        }
        // Soft Ψ drain via reserve already; nudge entropy feel with tiny reserve bump ignored.
    }

    private static void tickLoose(Mob mob) {
        AABB box = mob.getBoundingBox().inflate(12);
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity e : mob.level().getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (e == mob) {
                continue;
            }
            double d = mob.distanceToSqr(e);
            if (d < best) {
                best = d;
                nearest = e;
            }
        }
        mob.setTarget(nearest);
    }

    private static void tickChaos(ServerPlayer owner, Mob mob) {
        AABB box = mob.getBoundingBox().inflate(10);
        var list = mob.level().getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != mob);
        if (list.isEmpty()) {
            followOwner(mob, owner);
            return;
        }
        LivingEntity pick = list.get(owner.getRandom().nextInt(list.size()));
        mob.setTarget(pick);
    }

    private static void breakContract(ServerPlayer owner, Mob mob) {
        mob.getPersistentData().putBoolean(LOOSE_TAG, true);
        mob.getPersistentData().remove(CONTRACT_TAG);
        mob.setTarget(owner);
        owner.displayClientMessage(Component.translatable("message.effecoria.necro.contract_break"), true);
        PlayerPsiData data = PsiHelper.get(owner);
        data.setEntropyB(data.entropyB() + 0.12f);
        PsiHelper.set(owner, data);
    }

    public static void releaseControl(ServerPlayer owner, Mob mob) {
        mob.getPersistentData().putBoolean(LOOSE_TAG, true);
        mob.getPersistentData().remove(OWNER_TAG);
        PlayerPsiData data = PsiHelper.get(owner);
        data.untrackThrall(mob.getUUID());
        PsiHelper.set(owner, data);
        owner.displayClientMessage(Component.translatable("message.effecoria.necro.army_loose"), true);
    }

    private static LivingEntity findGuardHostile(Mob guard) {
        AABB box = guard.getBoundingBox().inflate(10);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Monster monster : guard.level().getEntitiesOfClass(Monster.class, box, LivingEntity::isAlive)) {
            if (monster == guard) {
                continue;
            }
            if (monster.getPersistentData().hasUUID(OWNER_TAG)) {
                continue;
            }
            double d = monster.distanceToSqr(guard);
            if (d < bestDist) {
                bestDist = d;
                best = monster;
            }
        }
        return best;
    }

    /** Raise a bone servant at the caster's look-hit / feet. */
    public static boolean spawnBoneServant(ServerPlayer owner, boolean overcharge) {
        ServerLevel level = owner.serverLevel();
        float reserve = 12f;
        Optional<ControlBlock> block = diagnoseControl(owner, reserve, EntityType.SKELETON);
        if (block.isPresent()) {
            owner.displayClientMessage(controlMessage(block.get(), owner, reserve), true);
            return false;
        }
        Skeleton skeleton = EntityType.SKELETON.create(level);
        if (skeleton == null) {
            return false;
        }
        Vec3 at = owner.position().add(owner.getLookAngle().scale(1.5)).add(0, 0.1, 0);
        skeleton.moveTo(at.x, at.y, at.z, owner.getYRot(), 0f);
        level.addFreshEntity(skeleton);
        if (!register(skeleton, owner, null, reserve)) {
            return false;
        }
        if (overcharge) {
            skeleton.getPersistentData().putBoolean(CHAOS_TAG, true);
            owner.displayClientMessage(Component.translatable("message.effecoria.necro.drunk_skeleton"), true);
        }
        return true;
    }

    /** Spirit contract — vex thrall that may betray. */
    public static boolean spawnSpiritContract(ServerPlayer owner, LivingEntity preferred) {
        ServerLevel level = owner.serverLevel();
        float reserve = 14f;
        Optional<ControlBlock> block = diagnoseControl(owner, reserve, EntityType.VEX);
        if (block.isPresent()) {
            owner.displayClientMessage(controlMessage(block.get(), owner, reserve), true);
            return false;
        }
        Vex vex = EntityType.VEX.create(level);
        if (vex == null) {
            return false;
        }
        Vec3 at = owner.position().add(0, 1.2, 0);
        vex.moveTo(at.x, at.y, at.z, owner.getYRot(), 0f);
        level.addFreshEntity(vex);
        if (!register(vex, owner, preferred, reserve)) {
            return false;
        }
        vex.getPersistentData().putBoolean(CONTRACT_TAG, true);
        return true;
    }

    /** Long-lived husk guard at a block position. */
    public static boolean spawnEternalGuard(ServerPlayer owner, Vec3 post) {
        ServerLevel level = owner.serverLevel();
        float reserve = 18f;
        Optional<ControlBlock> block = diagnoseControl(owner, reserve, EntityType.HUSK);
        if (block.isPresent()) {
            owner.displayClientMessage(controlMessage(block.get(), owner, reserve), true);
            return false;
        }
        Husk husk = EntityType.HUSK.create(level);
        if (husk == null) {
            return false;
        }
        husk.moveTo(post.x, post.y, post.z, owner.getYRot(), 0f);
        level.addFreshEntity(husk);
        if (!register(husk, owner, null, reserve)) {
            return false;
        }
        husk.getPersistentData().putBoolean(GUARD_TAG, true);
        husk.getPersistentData().putDouble(GUARD_X, post.x);
        husk.getPersistentData().putDouble(GUARD_Y, post.y);
        husk.getPersistentData().putDouble(GUARD_Z, post.z);
        return true;
    }

    /** Merge thralls into one stronger doll (wither skeleton). */
    public static boolean fuseIntoDoll(ServerPlayer owner) {
        List<Mob> owned = listOwned(owner);
        if (owned.size() < 2) {
            owner.displayClientMessage(Component.translatable("message.effecoria.necro.doll_need_thralls"), true);
            return false;
        }
        ServerLevel level = owner.serverLevel();
        float totalReserve = 0f;
        int take = Math.min(3, owned.size());
        List<Mob> sacrifice = owned.subList(0, take);
        Vec3 at = sacrifice.get(0).position();
        for (Mob mob : new ArrayList<>(sacrifice)) {
            totalReserve += reserveOf(mob);
            mob.discard();
            PlayerPsiData data = PsiHelper.get(owner);
            data.untrackThrall(mob.getUUID());
            PsiHelper.set(owner, data);
        }
        float reserve = Math.min(maxSingleHp(PsiHelper.get(owner)), Math.max(20f, totalReserve * 0.85f));
        Optional<ControlBlock> block = diagnoseControl(owner, reserve, EntityType.WITHER_SKELETON);
        if (block.isPresent()) {
            owner.displayClientMessage(controlMessage(block.get(), owner, reserve), true);
            return false;
        }
        WitherSkeleton doll = EntityType.WITHER_SKELETON.create(level);
        if (doll == null) {
            return false;
        }
        doll.moveTo(at.x, at.y, at.z, owner.getYRot(), 0f);
        if (doll.getAttribute(Attributes.MAX_HEALTH) != null) {
            doll.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.min(60, 20 + take * 12));
            doll.setHealth(doll.getMaxHealth());
        }
        level.addFreshEntity(doll);
        if (!register(doll, owner, null, reserve)) {
            return false;
        }
        doll.getPersistentData().putBoolean(DOLL_TAG, true);
        PlayerPsiData data = PsiHelper.get(owner);
        data.setEntropyB(data.entropyB() + 0.08f * take);
        PsiHelper.set(owner, data);
        return true;
    }

    /** Mass raise skeletons near the caster. */
    public static int spawnArmy(ServerPlayer owner, int count) {
        int raised = 0;
        for (int i = 0; i < count; i++) {
            float reserve = 10f;
            if (diagnoseControl(owner, reserve, EntityType.SKELETON).isPresent()) {
                break;
            }
            ServerLevel level = owner.serverLevel();
            Skeleton skeleton = EntityType.SKELETON.create(level);
            if (skeleton == null) {
                break;
            }
            double ang = (Math.PI * 2 * i) / Math.max(1, count);
            Vec3 at = owner.position().add(Math.cos(ang) * 2.2, 0.1, Math.sin(ang) * 2.2);
            skeleton.moveTo(at.x, at.y, at.z, owner.getYRot(), 0f);
            level.addFreshEntity(skeleton);
            if (register(skeleton, owner, null, reserve)) {
                raised++;
            }
        }
        return raised;
    }

    /** Temporary eldritch ally — wither skeleton that turns on everyone when the pact ends. */
    public static boolean spawnEldritchAlly(ServerPlayer owner, int lifetimeTicks) {
        ServerLevel level = owner.serverLevel();
        float reserve = 28f;
        Optional<ControlBlock> block = diagnoseControl(owner, reserve, EntityType.WITHER_SKELETON);
        if (block.isPresent()) {
            owner.displayClientMessage(controlMessage(block.get(), owner, reserve), true);
            return false;
        }
        WitherSkeleton demon = EntityType.WITHER_SKELETON.create(level);
        if (demon == null) {
            return false;
        }
        Vec3 at = owner.position().add(owner.getLookAngle().scale(2));
        demon.moveTo(at.x, at.y, at.z, owner.getYRot(), 0f);
        if (demon.getAttribute(Attributes.MAX_HEALTH) != null) {
            demon.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);
            demon.setHealth(40);
        }
        level.addFreshEntity(demon);
        if (!register(demon, owner, null, reserve)) {
            return false;
        }
        demon.getPersistentData().putBoolean(CONTRACT_TAG, true);
        demon.getPersistentData().putLong("effecoria:necro_pact_until", level.getGameTime() + lifetimeTicks);
        PlayerPsiData data = PsiHelper.get(owner);
        data.setEntropyB(data.entropyB() + 0.2f);
        PsiHelper.set(owner, data);
        return true;
    }

    /** Tiny indirection so NecroSummonService need not import BreathDebuffs at top for every call site. */
    private static final class BreathDebuffsProxy {
        static void confuse(Mob mob) {
            com.effecoria.core.formula.BreathDebuffs.apply(
                    mob, new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.CONFUSION, 60, 0, true, false, true));
        }

        static void slowness(Mob mob, int ticks, int amp) {
            com.effecoria.core.formula.BreathDebuffs.apply(
                    mob, new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, ticks, amp, true, false, true));
        }
    }
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
