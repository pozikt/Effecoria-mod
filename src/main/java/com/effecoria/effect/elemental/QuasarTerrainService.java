package com.effecoria.effect.elemental;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Quasar terrain: early life accretes and spins blocks; late life sheds mass outward.
 * Also seeds fire in the steam mantle.
 */
public final class QuasarTerrainService {
    private static final int MAX_ORBITING = 56;
    private static final float MAX_HARDNESS = 35f;
    /** Collect/spin until this life fraction, then begin shedding. */
    public static final float SHED_START = 0.55f;

    private QuasarTerrainService() {}

    public static boolean canLift(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.liquid() || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            return false;
        }
        if (state.is(Blocks.BEDROCK)
                || state.is(Blocks.BARRIER)
                || state.is(Blocks.COMMAND_BLOCK)
                || state.is(Blocks.CHAIN_COMMAND_BLOCK)
                || state.is(Blocks.REPEATING_COMMAND_BLOCK)
                || state.is(Blocks.STRUCTURE_BLOCK)
                || state.is(Blocks.STRUCTURE_VOID)
                || state.is(Blocks.END_PORTAL)
                || state.is(Blocks.END_PORTAL_FRAME)
                || state.is(Blocks.END_GATEWAY)
                || state.is(Blocks.MOVING_PISTON)) {
            return false;
        }
        float hardness = state.getDestroySpeed(level, pos);
        return hardness >= 0f && hardness <= MAX_HARDNESS;
    }

    /**
     * @param lifeProgress 0 at spawn → 1 at expiry
     */
    public static void tickTerrain(
            ServerLevel level, Vec3 center, float radius, long now, float lifeProgress, RandomSource random) {
        if (lifeProgress < SHED_START) {
            // Accretion phase — pull mass in and keep it spinning.
            float accrete = 1f - (lifeProgress / SHED_START);
            int budget = 2 + Math.round(3f * accrete);
            if (now % 2 == 0) {
                tear(level, center, radius, random, budget);
            }
            orbitDebris(level, center, radius, now, false);
        } else {
            // Mass-loss phase — stop collecting; gradually release orbiting blocks.
            float shed = (lifeProgress - SHED_START) / Math.max(0.001f, 1f - SHED_START);
            orbitDebris(level, center, radius, now, true);
            int releaseBudget = 1 + Math.round(shed * shed * 6f);
            if (now % 3 == 0) {
                shedMass(level, center, radius, random, releaseBudget, shed);
            }
            // Soft ground still scorches as the eye thins out.
            if (now % 5 == 0 && shed < 0.85f) {
                shredSoft(level, center, radius * (1f - shed * 0.35f), random, 2);
            }
        }

        if (now % 4 == 0) {
            igniteMantle(level, center, radius, lifeProgress, random);
        }
    }

    /** Rip blocks into orbiting falling entities. */
    public static void tear(ServerLevel level, Vec3 center, float radius, RandomSource random, int budget) {
        int orbiting = countOrbiting(level, center, radius);
        int remaining = Math.min(budget, MAX_ORBITING - orbiting);
        if (remaining <= 0) {
            return;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int attempts = remaining * 8;
        int lifted = 0;
        for (int i = 0; i < attempts && lifted < remaining; i++) {
            double ang = random.nextDouble() * Mth.TWO_PI;
            double dist = radius * (0.3 + random.nextDouble() * 0.75);
            int yOff = Mth.floor((random.nextDouble() - 0.45) * radius * 0.85);
            cursor.set(
                    Mth.floor(center.x + Math.cos(ang) * dist),
                    Mth.floor(center.y + yOff),
                    Mth.floor(center.z + Math.sin(ang) * dist));
            BlockState state = level.getBlockState(cursor);
            if (!canLift(level, cursor, state)) {
                continue;
            }
            if (state.is(BlockTags.LEAVES)
                    || state.is(BlockTags.FLOWERS)
                    || state.is(BlockTags.REPLACEABLE)
                    || state.is(BlockTags.CROPS)) {
                level.destroyBlock(cursor, true);
                lifted++;
                continue;
            }
            FallingBlockEntity falling = FallingBlockEntity.fall(level, cursor.immutable(), state);
            falling.setNoGravity(true);
            falling.setHurtsEntities(3.5f, 40);
            falling.dropItem = true;
            double orbitR = radius * (0.4 + random.nextDouble() * 0.4);
            Vec3 rel = falling.position().subtract(center);
            if (rel.horizontalDistanceSqr() < 1.0e-4) {
                rel = new Vec3(Math.cos(ang), 0, Math.sin(ang));
            }
            Vec3 tang = new Vec3(-rel.z, 0.05, rel.x).normalize().scale(0.42);
            Vec3 radial = rel.multiply(1, 0, 1).normalize().scale(0.1 * (orbitR - rel.horizontalDistance()));
            falling.setDeltaMovement(tang.add(radial));
            falling.hurtMarked = true;
            lifted++;
        }
    }

    public static void orbitDebris(ServerLevel level, Vec3 center, float radius, long now, boolean shedding) {
        AABB box = debrisBox(center, radius, 2);
        float spin = shedding ? 0.28f : 0.42f + 0.1f * Mth.sin(now * 0.11f);
        for (FallingBlockEntity falling : level.getEntitiesOfClass(FallingBlockEntity.class, box)) {
            if (!falling.isAlive() || !falling.isNoGravity()) {
                continue;
            }
            Vec3 pos = falling.position();
            Vec3 rel = pos.subtract(center);
            double horiz = Math.sqrt(rel.x * rel.x + rel.z * rel.z);
            if (horiz < 0.15) {
                continue;
            }
            double targetR = shedding
                    ? Mth.clamp(horiz + 0.08, radius * 0.45, radius * 1.15)
                    : Mth.clamp(horiz, radius * 0.32, radius * 0.88);
            Vec3 tang = new Vec3(-rel.z, 0, rel.x).normalize().scale(spin);
            double radialErr = targetR - horiz;
            Vec3 radial = new Vec3(rel.x, 0, rel.z).normalize().scale(radialErr * 0.09);
            double yTarget = center.y + Mth.sin(now * 0.15f + (float) horiz) * (radius * 0.18);
            double yPull = (yTarget - pos.y) * 0.08;
            falling.setDeltaMovement(tang.x + radial.x, yPull, tang.z + radial.z);
            falling.hurtMarked = true;
            falling.time = Math.min(falling.time, shedding ? 400 : 120);
        }
    }

    /** Gradually fling orbiting blocks away — loses mass over the shed phase. */
    public static void shedMass(
            ServerLevel level, Vec3 center, float radius, RandomSource random, int budget, float shedStrength) {
        AABB box = debrisBox(center, radius, 2);
        var list = level.getEntitiesOfClass(FallingBlockEntity.class, box, FallingBlockEntity::isNoGravity);
        if (list.isEmpty()) {
            return;
        }
        int toRelease = Math.min(budget, list.size());
        // Prefer outer debris first so the eye thins from the rim inward.
        list.sort((a, b) -> Double.compare(
                b.distanceToSqr(center.x, center.y, center.z),
                a.distanceToSqr(center.x, center.y, center.z)));
        float fling = 0.35f + shedStrength * 0.9f;
        for (int i = 0; i < toRelease; i++) {
            FallingBlockEntity falling = list.get(i);
            falling.setNoGravity(false);
            Vec3 away = falling.position().subtract(center);
            if (away.lengthSqr() < 1.0e-4) {
                away = new Vec3(random.nextGaussian(), 0.2, random.nextGaussian());
            }
            away = away.normalize().scale(fling).add(0, 0.15 + shedStrength * 0.35, 0);
            falling.setDeltaMovement(falling.getDeltaMovement().add(away));
            falling.hurtMarked = true;
        }
    }

    /** Seed fire around the steam mantle — stronger mid-life, fades while shedding. */
    public static void igniteMantle(
            ServerLevel level, Vec3 center, float radius, float lifeProgress, RandomSource random) {
        float firePower = lifeProgress < SHED_START
                ? 0.45f + 0.55f * (lifeProgress / SHED_START)
                : Math.max(0.15f, 1f - (lifeProgress - SHED_START) / (1f - SHED_START));
        int attempts = 2 + Math.round(5f * firePower);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < attempts; i++) {
            double ang = random.nextDouble() * Mth.TWO_PI;
            double dist = radius * (0.55 + random.nextDouble() * 0.55);
            int yOff = Mth.floor((random.nextDouble() - 0.35) * radius * 0.7);
            cursor.set(
                    Mth.floor(center.x + Math.cos(ang) * dist),
                    Mth.floor(center.y + yOff),
                    Mth.floor(center.z + Math.sin(ang) * dist));
            // Prefer surfaces: try the sampled air cell, then one above solid.
            if (!ElementalEffects.tryPlaceFire(level, cursor)) {
                BlockPos above = cursor.above();
                if (level.getBlockState(cursor).isSolidRender(level, cursor)) {
                    ElementalEffects.tryPlaceFire(level, above);
                } else {
                    ElementalEffects.tryPlaceFire(level, cursor.below().above());
                }
            }
        }
    }

    public static void release(ServerLevel level, Vec3 center, float radius) {
        AABB box = debrisBox(center, radius, 3);
        for (FallingBlockEntity falling : level.getEntitiesOfClass(FallingBlockEntity.class, box)) {
            falling.setNoGravity(false);
            Vec3 fling = falling.position().subtract(center);
            if (fling.lengthSqr() > 1.0e-4) {
                fling = fling.normalize().scale(0.7).add(0, 0.3, 0);
                falling.setDeltaMovement(falling.getDeltaMovement().add(fling));
            }
            falling.hurtMarked = true;
        }
    }

    private static AABB debrisBox(Vec3 center, float radius, double pad) {
        return new AABB(
                center.x - radius - pad,
                center.y - radius - pad,
                center.z - radius - pad,
                center.x + radius + pad,
                center.y + radius + pad,
                center.z + radius + pad);
    }

    private static int countOrbiting(ServerLevel level, Vec3 center, float radius) {
        return level.getEntitiesOfClass(FallingBlockEntity.class, debrisBox(center, radius, 1), FallingBlockEntity::isNoGravity)
                .size();
    }

    private static void shredSoft(ServerLevel level, Vec3 center, float radius, RandomSource random, int budget) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int done = 0;
        for (int i = 0; i < budget * 6 && done < budget; i++) {
            double ang = random.nextDouble() * Mth.TWO_PI;
            double dist = radius * (0.5 + random.nextDouble() * 0.55);
            cursor.set(
                    Mth.floor(center.x + Math.cos(ang) * dist),
                    Mth.floor(center.y + (random.nextDouble() - 0.4) * radius),
                    Mth.floor(center.z + Math.sin(ang) * dist));
            BlockState state = level.getBlockState(cursor);
            if (!canLift(level, cursor, state)) {
                continue;
            }
            if (state.getDestroySpeed(level, cursor) <= 1.6f
                    || state.is(BlockTags.LEAVES)
                    || state.is(BlockTags.DIRT)) {
                level.destroyBlock(cursor, true);
                done++;
            }
        }
    }
}
