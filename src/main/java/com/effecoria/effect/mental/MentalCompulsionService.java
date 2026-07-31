package com.effecoria.effect.mental;

import com.effecoria.content.ModParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Timed mental compulsions: flee a fear source, seek a lethal drop, frenzy, or freeze in despair.
 */
public final class MentalCompulsionService {
    public static final String TYPE_TAG = "effecoria:mental_compel_type";
    public static final String UNTIL_TAG = "effecoria:mental_compel_until";
    public static final String OWNER_TAG = "effecoria:mental_compel_owner";
    public static final String FEAR_SOURCE_TAG = "effecoria:mental_fear_source";

    public enum Type {
        TERROR,
        CLIFF,
        FRENZY,
        DEPRESS;

        static Type fromId(String id) {
            return switch (id) {
                case "cliff" -> CLIFF;
                case "frenzy" -> FRENZY;
                case "depress" -> DEPRESS;
                default -> TERROR;
            };
        }

        String id() {
            return switch (this) {
                case CLIFF -> "cliff";
                case FRENZY -> "frenzy";
                case DEPRESS -> "depress";
                case TERROR -> "terror";
            };
        }
    }

    private MentalCompulsionService() {}

    public static boolean apply(ServerPlayer caster, LivingEntity target, Type type, int durationTicks) {
        return apply(caster, target, type, durationTicks, null);
    }

    public static boolean apply(
            ServerPlayer caster, LivingEntity target, Type type, int durationTicks, LivingEntity fearSource) {
        if (!(target instanceof Mob mob) || target instanceof Player) {
            return false;
        }
        if (durationTicks <= 0) {
            return false;
        }
        CompoundTag data = mob.getPersistentData();
        data.putString(TYPE_TAG, type.id());
        data.putLong(UNTIL_TAG, caster.level().getGameTime() + durationTicks);
        data.putUUID(OWNER_TAG, caster.getUUID());
        if (type == Type.TERROR && fearSource != null && fearSource.isAlive()) {
            data.putUUID(FEAR_SOURCE_TAG, fearSource.getUUID());
        } else {
            data.remove(FEAR_SOURCE_TAG);
        }
        mob.setTarget(null);
        mob.getNavigation().stop();
        if (type == Type.DEPRESS) {
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 4));
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, 2));
            mob.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, durationTicks, 2));
            mob.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Math.min(60, durationTicks), 0));
        }
        return true;
    }

    public static boolean hasActive(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(TYPE_TAG) || !data.contains(UNTIL_TAG)) {
            return false;
        }
        return entity.level().getGameTime() <= data.getLong(UNTIL_TAG);
    }

    public static Type typeOf(LivingEntity entity) {
        return Type.fromId(entity.getPersistentData().getString(TYPE_TAG));
    }

    public static void clear(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        data.remove(TYPE_TAG);
        data.remove(UNTIL_TAG);
        data.remove(OWNER_TAG);
        data.remove(FEAR_SOURCE_TAG);
    }

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 2 != 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            AABB box = player.getBoundingBox().inflate(48);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, box, MentalCompulsionService::hasActive)) {
                tickMob(level, mob);
            }
        }
    }

    private static void tickMob(ServerLevel level, Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (level.getGameTime() > data.getLong(UNTIL_TAG)) {
            clear(mob);
            mob.setTarget(null);
            return;
        }
        Type type = Type.fromId(data.getString(TYPE_TAG));
        UUID ownerId = data.hasUUID(OWNER_TAG) ? data.getUUID(OWNER_TAG) : null;
        LivingEntity owner = ownerId != null && level.getPlayerByUUID(ownerId) instanceof LivingEntity living
                ? living
                : null;

        level.sendParticles(
                ModParticleTypes.MENTAL_FOG.get(),
                mob.getX(),
                mob.getY() + mob.getBbHeight() * 0.7,
                mob.getZ(),
                1,
                0.15,
                0.2,
                0.15,
                0.002);

        switch (type) {
            case TERROR -> tickTerror(level, mob, data, owner);
            case CLIFF -> tickCliff(level, mob);
            case FRENZY -> tickFrenzy(level, mob);
            case DEPRESS -> tickDepress(mob);
        }
    }

    private static void tickTerror(ServerLevel level, Mob mob, CompoundTag data, LivingEntity owner) {
        LivingEntity fear = resolveFearSource(level, data, owner);
        if (fear == null || !fear.isAlive()) {
            return;
        }
        double distSq = mob.distanceToSqr(fear);
        if (distSq < 2.5 * 2.5) {
            Vec3 push = mob.position().subtract(fear.position()).normalize().scale(0.55);
            mob.setDeltaMovement(mob.getDeltaMovement().add(push.x, 0.05, push.z));
            mob.hurtMarked = true;
        }
        Vec3 away = mob.position().subtract(fear.position());
        if (away.lengthSqr() < 0.01) {
            away = new Vec3(1, 0, 0);
        }
        Vec3 dest = mob.position().add(away.normalize().scale(10));
        mob.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.4);
        mob.setTarget(null);
    }

    private static LivingEntity resolveFearSource(ServerLevel level, CompoundTag data, LivingEntity owner) {
        if (data.hasUUID(FEAR_SOURCE_TAG)) {
            var entity = level.getEntity(data.getUUID(FEAR_SOURCE_TAG));
            if (entity instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
        }
        return owner;
    }

    private static void tickDepress(Mob mob) {
        mob.setTarget(null);
        mob.getNavigation().stop();
        mob.setDeltaMovement(mob.getDeltaMovement().multiply(0.2, 1.0, 0.2));
        mob.hurtMarked = true;
    }

    private static void tickCliff(ServerLevel level, Mob mob) {
        mob.setTarget(null);
        DropSite site = findDropSite(level, mob, 14, 4);
        if (site == null) {
            // No ledge in range — wander calmly while looking for one (no shove).
            if (mob.getNavigation().isDone() && level.getGameTime() % 20 == 0) {
                double ox = mob.getX() + (mob.getRandom().nextDouble() - 0.5) * 8;
                double oz = mob.getZ() + (mob.getRandom().nextDouble() - 0.5) * 8;
                mob.getNavigation().moveTo(ox, mob.getY(), oz, 1.05);
            }
            return;
        }

        double standX = site.stand().getX() + 0.5;
        double standY = site.stand().getY();
        double standZ = site.stand().getZ() + 0.5;
        double distToStand = mob.distanceToSqr(standX, standY, standZ);

        if (distToStand > 1.35 * 1.35) {
            // Walk to the ledge under own pathfinding.
            if (mob.getNavigation().isDone() || level.getGameTime() % 10 == 0) {
                mob.getNavigation().moveTo(standX, standY, standZ, 1.15);
            }
            return;
        }

        // On the ledge — keep walking into the open air cell (MoveControl, not impulse).
        mob.getNavigation().stop();
        double airX = site.air().getX() + 0.5;
        double airY = site.air().getY();
        double airZ = site.air().getZ() + 0.5;
        mob.getLookControl().setLookAt(airX, airY + 0.5, airZ);
        mob.getMoveControl().setWantedPosition(airX, airY, airZ, 1.1);
    }

    private static void tickFrenzy(ServerLevel level, Mob mob) {
        LivingEntity current = mob.getTarget();
        if (current != null && current.isAlive() && current != mob && mob.distanceToSqr(current) < 20 * 20) {
            return;
        }
        LivingEntity next = findFrenzyTarget(level, mob);
        mob.setTarget(next);
    }

    private record DropSite(BlockPos stand, BlockPos air) {}

    private static DropSite findDropSite(ServerLevel level, Mob mob, int radius, int minFall) {
        BlockPos origin = mob.blockPosition();
        DropSite best = null;
        double bestScore = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos column = origin.offset(dx, 0, dz);
                BlockPos surface = findStandable(level, column, 8);
                if (surface == null) {
                    continue;
                }
                BlockPos air = adjacentFallAir(level, surface, minFall);
                if (air == null) {
                    continue;
                }
                // Prefer edges the mob can actually path to.
                if (mob.getNavigation().createPath(surface, 0) == null && mob.distanceToSqr(
                        surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5) > 4) {
                    continue;
                }
                int fall = columnFallDepth(level, air);
                double dist = mob.distanceToSqr(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5);
                double score = dist - fall * 4.0;
                if (score < bestScore) {
                    bestScore = score;
                    best = new DropSite(surface, air);
                }
            }
        }
        return best;
    }

    private static LivingEntity findFrenzyTarget(ServerLevel level, Mob mob) {
        AABB box = mob.getBoundingBox().inflate(16);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (other == mob) {
                continue;
            }
            double dist = mob.distanceToSqr(other);
            if (dist < bestDist) {
                bestDist = dist;
                best = other;
            }
        }
        return best;
    }

    private static BlockPos findStandable(ServerLevel level, BlockPos column, int verticalScan) {
        BlockPos.MutableBlockPos cursor = column.mutable();
        for (int dy = verticalScan; dy >= -verticalScan; dy--) {
            cursor.set(column.getX(), column.getY() + dy, column.getZ());
            BlockState below = level.getBlockState(cursor.below());
            BlockState here = level.getBlockState(cursor);
            if (!below.getCollisionShape(level, cursor.below()).isEmpty()
                    && here.getCollisionShape(level, cursor).isEmpty()) {
                return cursor.immutable();
            }
        }
        return null;
    }

    /** Air cell next to a standable ledge with enough empty space below. */
    private static BlockPos adjacentFallAir(ServerLevel level, BlockPos standPos, int minFall) {
        BlockPos best = null;
        int deepest = minFall - 1;
        for (Direction dir : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            BlockPos air = standPos.relative(dir);
            if (!level.getBlockState(air).getCollisionShape(level, air).isEmpty()) {
                continue;
            }
            int depth = columnFallDepth(level, air);
            if (depth > deepest) {
                deepest = depth;
                best = air;
            }
        }
        return best;
    }

    private static int columnFallDepth(ServerLevel level, BlockPos airStart) {
        int depth = 0;
        for (int y = 1; y <= 32; y++) {
            BlockPos below = airStart.below(y);
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                break;
            }
            depth = y;
        }
        return depth;
    }
}
