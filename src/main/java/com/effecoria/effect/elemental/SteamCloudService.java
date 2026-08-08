package com.effecoria.effect.elemental;

import com.effecoria.core.formula.BreathDebuffs;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.network.ModNetworking;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** World-space steam fog volumes — linger after cast, not attached to the caster. */
public final class SteamCloudService {
    private static final List<SteamCloud> CLOUDS = new CopyOnWriteArrayList<>();
    private static final int PARTICLE_INTERVAL = 2;
    private static final int DAMAGE_INTERVAL = 20;
    /** Extra reach beyond cloud radius for form mutate. */
    public static final float MUTATE_EXTRA_REACH = 4f;

    public enum Mode {
        VEIL(0),
        SCALDING(1),
        FROST(2);

        private final int id;

        Mode(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static Mode fromId(int id) {
            return switch (id) {
                case 1 -> SCALDING;
                case 2 -> FROST;
                default -> VEIL;
            };
        }
    }

    private SteamCloudService() {}

    public record SteamCloud(
            ServerLevel level,
            Vec3 center,
            float radius,
            long expireAt,
            UUID owner,
            Mode mode) {
        public boolean contains(Vec3 pos) {
            return pos.distanceToSqr(center) <= (double) radius * radius;
        }

        public AABB bounds() {
            return new AABB(
                    center.x - radius,
                    center.y - radius * 0.6,
                    center.z - radius,
                    center.x + radius,
                    center.y + radius * 0.9,
                    center.z + radius);
        }

        public boolean scalding() {
            return mode == Mode.SCALDING;
        }

        public boolean frost() {
            return mode == Mode.FROST;
        }
    }

    /** Client-safe snapshot for fog rendering. */
    public record CloudSnapshot(double x, double y, double z, float radius, long expireAt, int modeId) {
        public Mode mode() {
            return Mode.fromId(modeId);
        }
    }

    public static void spawn(
            ServerLevel level, Vec3 center, float radius, int durationTicks, UUID owner, boolean scalding) {
        spawn(level, center, radius, durationTicks, owner, scalding ? Mode.SCALDING : Mode.VEIL);
    }

    public static void spawn(
            ServerLevel level, Vec3 center, float radius, int durationTicks, UUID owner, Mode mode) {
        long expireAt = level.getGameTime() + Math.max(1, durationTicks);
        CLOUDS.add(new SteamCloud(level, center, Math.max(0.5f, radius), expireAt, owner, mode));
        syncToTracking(level);
        spawnBurst(level, center, radius, mode);
    }

    /** True if owner has any live cloud in this level. */
    public static boolean hasOwned(ServerLevel level, UUID owner) {
        long now = level.getGameTime();
        for (SteamCloud cloud : CLOUDS) {
            if (cloud.level() == level && now < cloud.expireAt() && owner.equals(cloud.owner())) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static SteamCloud findNearestOwned(ServerLevel level, UUID owner, Vec3 from) {
        long now = level.getGameTime();
        SteamCloud best = null;
        double bestDist = Double.MAX_VALUE;
        for (SteamCloud cloud : CLOUDS) {
            if (cloud.level() != level || now >= cloud.expireAt() || !owner.equals(cloud.owner())) {
                continue;
            }
            double d = from.distanceToSqr(cloud.center());
            if (d < bestDist) {
                bestDist = d;
                best = cloud;
            }
        }
        return best;
    }

    public static boolean isInMutateRange(SteamCloud cloud, Vec3 from) {
        double reach = cloud.radius() + MUTATE_EXTRA_REACH;
        return from.distanceToSqr(cloud.center()) <= reach * reach;
    }

    /**
     * Mutate nearest owned cloud to {@code mode}, optionally extending lifetime.
     *
     * @return true if a cloud was mutated
     */
    public static boolean mutateOwned(ServerLevel level, UUID owner, Vec3 from, Mode mode, int refreshTicks) {
        SteamCloud cloud = findNearestOwned(level, owner, from);
        if (cloud == null || !isInMutateRange(cloud, from)) {
            return false;
        }
        long newExpire = Math.max(cloud.expireAt(), level.getGameTime() + Math.max(1, refreshTicks));
        CLOUDS.remove(cloud);
        CLOUDS.add(new SteamCloud(level, cloud.center(), cloud.radius(), newExpire, owner, mode));
        syncToTracking(level);
        spawnBurst(level, cloud.center(), cloud.radius(), mode);
        return true;
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        boolean changed = CLOUDS.removeIf(cloud -> cloud.level() == level && now >= cloud.expireAt());

        for (SteamCloud cloud : CLOUDS) {
            if (cloud.level() != level) {
                continue;
            }
            applyZoneEffects(level, cloud, now);
            if (now % PARTICLE_INTERVAL == 0) {
                spawnVolumeParticles(level, cloud);
            }
        }

        if (now % 5 == 0) {
            clearObscuredTargets(level);
        }

        if (changed) {
            syncToTracking(level);
        }
    }

    public static List<CloudSnapshot> snapshotsFor(ServerLevel level) {
        long now = level.getGameTime();
        List<CloudSnapshot> out = new ArrayList<>();
        for (SteamCloud cloud : CLOUDS) {
            if (cloud.level() != level || now >= cloud.expireAt()) {
                continue;
            }
            out.add(new CloudSnapshot(
                    cloud.center().x,
                    cloud.center().y,
                    cloud.center().z,
                    cloud.radius(),
                    cloud.expireAt(),
                    cloud.mode().id()));
        }
        return out;
    }

    public static void syncToTracking(ServerLevel level) {
        List<CloudSnapshot> snaps = snapshotsFor(level);
        PacketDistributor.sendToPlayersInDimension(level, new ModNetworking.SteamCloudsPayload(snaps));
    }

    public static void syncToPlayer(ServerPlayer player, ServerLevel level) {
        PacketDistributor.sendToPlayer(player, new ModNetworking.SteamCloudsPayload(snapshotsFor(level)));
    }

    /** True if this mob cannot see the target because steam fog conceals or blocks the view. */
    public static boolean obscuresVision(LivingEntity viewer, LivingEntity target) {
        if (viewer == null || target == null || viewer.level().isClientSide()) {
            return false;
        }
        if (!(viewer.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!hasClouds(level)) {
            return false;
        }
        Vec3 eyeTarget = target.getEyePosition();
        if (isInsideCloud(level, eyeTarget) || isInsideCloud(level, target.position().add(0, target.getBbHeight() * 0.5, 0))) {
            return true;
        }
        return blocksLineOfSight(level, viewer.getEyePosition(), eyeTarget);
    }

    public static boolean hasClouds(ServerLevel level) {
        long now = level.getGameTime();
        for (SteamCloud cloud : CLOUDS) {
            if (cloud.level() == level && now < cloud.expireAt()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInsideCloud(ServerLevel level, Vec3 pos) {
        long now = level.getGameTime();
        for (SteamCloud cloud : CLOUDS) {
            if (cloud.level() != level || now >= cloud.expireAt()) {
                continue;
            }
            if (cloud.contains(pos)) {
                return true;
            }
        }
        return false;
    }

    /** Steam acts as a visual wall: any active cloud intersecting the eye-line blocks sight. */
    public static boolean blocksLineOfSight(ServerLevel level, Vec3 from, Vec3 to) {
        long now = level.getGameTime();
        for (SteamCloud cloud : CLOUDS) {
            if (cloud.level() != level || now >= cloud.expireAt()) {
                continue;
            }
            if (segmentIntersectsSphere(from, to, cloud.center(), cloud.radius())) {
                return true;
            }
        }
        return false;
    }

    private static boolean segmentIntersectsSphere(Vec3 a, Vec3 b, Vec3 center, float radius) {
        double r2 = (double) radius * radius;
        if (a.distanceToSqr(center) <= r2 || b.distanceToSqr(center) <= r2) {
            return true;
        }
        Vec3 ab = b.subtract(a);
        double abLenSq = ab.lengthSqr();
        if (abLenSq < 1.0E-8) {
            return false;
        }
        double t = Mth.clamp(center.subtract(a).dot(ab) / abLenSq, 0.0, 1.0);
        Vec3 closest = a.add(ab.scale(t));
        return closest.distanceToSqr(center) <= r2;
    }

    /** Drop mob aggression when their current target is hidden by steam. */
    public static void clearObscuredTargets(ServerLevel level) {
        if (!hasClouds(level)) {
            return;
        }
        double pad = 24.0;
        for (SteamCloud cloud : CLOUDS) {
            if (cloud.level() != level || level.getGameTime() >= cloud.expireAt()) {
                continue;
            }
            AABB search = cloud.bounds().inflate(pad);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, search, LivingEntity::isAlive)) {
                LivingEntity target = mob.getTarget();
                if (target != null && obscuresVision(mob, target)) {
                    mob.setTarget(null);
                }
            }
        }
    }

    private static void applyZoneEffects(ServerLevel level, SteamCloud cloud, long now) {
        AABB box = cloud.bounds();
        DamageSource source = level.damageSources().magic();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (!cloud.contains(entity.position().add(0, entity.getBbHeight() * 0.5, 0))) {
                continue;
            }
            boolean isOwner = cloud.owner() != null && cloud.owner().equals(entity.getUUID());
            if (isOwner) {
                BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, 0, false, false, true));
                continue;
            }
            int slowAmp = cloud.frost() ? 1 : 0;
            BreathDebuffs.apply(
                    level,
                    cloud.owner(),
                    entity,
                    new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, slowAmp, false, false, true));
            if (cloud.scalding()) {
                BreathDebuffs.apply(
                        level,
                        cloud.owner(),
                        entity,
                        new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false, true));
                if (now % DAMAGE_INTERVAL == 0) {
                    entity.hurt(source, 1.0f);
                }
            } else if (cloud.frost()) {
                if (now % DAMAGE_INTERVAL == 0) {
                    entity.hurt(source, 1.0f);
                    entity.setTicksFrozen(Math.min(entity.getTicksRequiredToFreeze() + 40, entity.getTicksFrozen() + 30));
                }
            }
        }
    }

    private static void spawnVolumeParticles(ServerLevel level, SteamCloud cloud) {
        float r = cloud.radius();
        int count = Math.max(6, Math.round(r * r * 1.8f));
        if (cloud.frost()) {
            level.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    cloud.center().x,
                    cloud.center().y + 0.4,
                    cloud.center().z,
                    Math.max(4, count / 2),
                    r * 0.5,
                    r * 0.3,
                    r * 0.5,
                    0.01);
            level.sendParticles(
                    ModParticleTypes.STEAM_FOG.get(),
                    cloud.center().x,
                    cloud.center().y + 0.3,
                    cloud.center().z,
                    count / 2,
                    r * 0.55,
                    r * 0.3,
                    r * 0.55,
                    0.008);
            return;
        }
        if (cloud.scalding()) {
            level.sendParticles(
                    ParticleTypes.CLOUD,
                    cloud.center().x,
                    cloud.center().y + 0.5,
                    cloud.center().z,
                    Math.max(3, count / 3),
                    r * 0.4,
                    r * 0.25,
                    r * 0.4,
                    0.02);
        }
        level.sendParticles(
                ModParticleTypes.STEAM_FOG.get(),
                cloud.center().x,
                cloud.center().y + 0.4,
                cloud.center().z,
                count,
                r * 0.55,
                r * 0.35,
                r * 0.55,
                0.012);
    }

    private static void spawnBurst(ServerLevel level, Vec3 center, float radius, Mode mode) {
        if (mode == Mode.FROST) {
            level.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    center.x,
                    center.y + 0.5,
                    center.z,
                    Math.max(16, Math.round(radius * 10)),
                    radius * 0.45,
                    radius * 0.3,
                    radius * 0.45,
                    0.02);
        }
        level.sendParticles(
                ModParticleTypes.STEAM_FOG.get(),
                center.x,
                center.y + 0.5,
                center.z,
                Math.max(20, Math.round(radius * 12)),
                radius * 0.5,
                radius * 0.35,
                radius * 0.5,
                0.02);
    }
}
