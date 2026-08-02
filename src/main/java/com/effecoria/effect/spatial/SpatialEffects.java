package com.effecoria.effect.spatial;

import com.effecoria.core.formula.SpellCombat;

import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.core.formula.DiceDamage;
import com.effecoria.core.magic.SpellEffectEntry;
import com.effecoria.world.ModDimensions;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class SpatialEffects {
    private SpatialEffects() {}

    public static void warpBolt(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        ServerLevel level = caster.serverLevel();
        Vec3 hit = target != null ? target.position().add(0, 1.0, 0) : aim;
        if (target != null) {
            float damage = DiceDamage.fromParams(effect.params(), power, 5f);
            target.hurt(SpellCombat.magic(caster), damage);
            target.hurtMarked = true;
        }
        cutAlongCasterLook(caster, hit, power);
        SpatialVfx.playLineFromCaster(caster, hit, power, 2);
        finishHit(level, target != null ? target.position() : aim);
    }

    public static void spatialWard(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 160;
        int absorb = effect.params().has("absorption_amplifier") ? effect.params().get("absorption_amplifier").getAsInt() : 1;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.ABSORPTION, duration, absorb, false, false, true));
        spawnSpatialParticles(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void foldRepulse(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        ServerLevel level = caster.serverLevel();
        Vec3 hit = target != null ? target.position().add(0, 1.0, 0) : aim;
        if (target != null) {
            float force = effect.params().has("force") ? effect.params().get("force").getAsFloat() : 2.2f;
            Vec3 away = target.position().subtract(caster.position()).normalize();
            double strength = force * (power / 50f);
            target.setDeltaMovement(target.getDeltaMovement().add(away.scale(strength)));
            target.hurtMarked = true;
            float damage = DiceDamage.fromParams(effect.params(), power, 2f);
            target.hurt(SpellCombat.magic(caster), damage);
        }
        cutAlongCasterLook(caster, hit, power);
        SpatialVfx.playLineFromCaster(caster, hit, power, 2);
        finishHit(level, target != null ? target.position() : aim);
    }

    public static void riftSlash(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        ServerLevel level = caster.serverLevel();
        Vec3 center = target != null ? target.position().add(0, 1.0, 0) : aim;
        if (target != null) {
            float damage = DiceDamage.fromParams(effect.params(), power, 6f);
            int slowTicks = effect.params().has("slow_ticks") ? effect.params().get("slow_ticks").getAsInt() : 60;
            target.hurt(SpellCombat.magic(caster), damage);
            BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 1));
            target.hurtMarked = true;
        }
        cutAlongCasterLook(caster, center, power);
        // Slash also nicks a short transverse ribbon through the hit point.
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 side = look.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0e-4) {
            side = look.cross(new Vec3(1, 0, 0));
        }
        side = side.normalize().scale(1.4);
        cutAlongSegment(level, caster, center.subtract(side), center.add(side), power, 0);
        SpatialVfx.playAround(caster, center, power, 2);
        finishHit(level, target != null ? target.position() : aim);
    }

    public static void gravitySnare(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 6f;
        int slowTicks = effect.params().has("slow_ticks") ? effect.params().get("slow_ticks").getAsInt() : 80;
        float pull = effect.params().has("pull_strength") ? effect.params().get("pull_strength").getAsFloat() : 0.35f;
        Vec3 center = caster.position();
        AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 2));
            Vec3 toward = center.subtract(entity.position()).normalize().scale(pull);
            entity.setDeltaMovement(entity.getDeltaMovement().add(toward));
            entity.hurtMarked = true;
            spawnSpatialParticles(level, entity.position().add(0, 1, 0));
        }
        spawnSpatialParticles(level, center.add(0, 1, 0));
    }

    public static void gravityField(ServerPlayer caster, SpellEffectEntry effect, float power) {
        JsonObject params = effect.params();
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 8f;
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 180;
        float pull = params.has("pull_strength") ? params.get("pull_strength").getAsFloat() : 0.25f;
        float dps = params.has("damage_dice_per_round")
                ? SpatialFieldService.dpsFromParams(params, power)
                : 0f;
        SpatialFieldService.spawnGravityWell(
                caster.serverLevel(),
                caster.position().add(0, 0.5, 0),
                caster.getUUID(),
                radius,
                duration,
                pull,
                dps);
    }

    public static void dimensionalAnchor(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 100;
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 5));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.GLOWING, duration, 0));
        finishHit(caster.serverLevel(), target.position());
    }

    public static void voidLance(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        ServerLevel level = caster.serverLevel();
        Vec3 hit = target != null ? target.position().add(0, 1.0, 0) : aim;
        if (target != null) {
            float damage = DiceDamage.fromParams(effect.params(), power, 9f);
            target.hurt(SpellCombat.magic(caster), damage);
            BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            target.hurtMarked = true;
        }
        cutAlongCasterLook(caster, hit, power);
        SpatialVfx.playLineFromCaster(caster, hit, power, 3);
        finishHit(level, target != null ? target.position() : aim);
        level.playSound(null, BlockPos.containing(hit), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    public static void warpExchange(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        Vec3 casterPos = caster.position();
        Vec3 targetPos = target.position();
        SpatialVfx.playRipple(caster, casterPos.add(0, 1, 0), power);
        SpatialVfx.playRipple(caster, targetPos.add(0, 1, 0), power);
        caster.teleportTo(targetPos.x, targetPos.y, targetPos.z);
        caster.fallDistance = 0f;
        target.teleportTo(casterPos.x, casterPos.y, casterPos.z);
        target.hurtMarked = true;
        float damage = DiceDamage.fromParams(effect.params(), power, 3f);
        target.hurt(SpellCombat.magic(caster), damage);
        level.playSound(null, caster.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1f, 0.9f);
    }

    public static void spatialSurge(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 5f;
        float damage = DiceDamage.fromParams(effect.params(), power, 6f);
        AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            entity.hurt(SpellCombat.magic(caster), damage);
            entity.hurtMarked = true;
            spawnSpatialParticles(level, entity.position().add(0, 1, 0));
        }
        Vec3 tip = caster.position().add(0, 1.0, 0).add(caster.getLookAngle().normalize().scale(Math.max(3.0, radius * 0.8)));
        cutAlongCasterLook(caster, tip, power);
        cutSphere(level, caster, caster.position().add(0, 1.0, 0), Math.min(radius, 3.5f), power, 36);
        SpatialVfx.playLineFromCaster(caster, tip, power, 3);
        spawnSpatialParticles(level, caster.position().add(0, 1, 0));
    }

    public static void farBlink(ServerPlayer caster, SpellEffectEntry effect, float power) {
        blinkAlongLook(caster, effect, power, 1.0, defaultMaxRange(effect, 200));
    }

    public static void standardBlink(ServerPlayer caster, SpellEffectEntry effect, float power) {
        blinkAlongLook(caster, effect, power, 1.0, defaultMaxRange(effect, 24));
    }

    public static void riftBurst(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 4f;
        float damage = DiceDamage.fromParams(effect.params(), power, 7f);
        Vec3 center = target != null ? target.position() : aim;
        hurtRadius(level, center, radius, damage, caster);
        cutSphere(level, caster, center.add(0, 0.5, 0), Math.min(radius, 3.5f), power, 48);
        SpatialVfx.playAround(caster, center.add(0, 1.0, 0), power, 2);
        spawnSpatialParticles(level, center.add(0, 1, 0));
        level.playSound(null, BlockPos.containing(center), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 0.5f);
    }

    public static void spatialSingularity(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 7f;
        float pull = effect.params().has("pull_strength") ? effect.params().get("pull_strength").getAsFloat() : 0.9f;
        float damage = DiceDamage.fromParams(effect.params(), power, 8f);
        Vec3 center = target != null ? target.position() : aim;
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.position().distanceToSqr(center) > radius * radius) {
                continue;
            }
            Vec3 toward = center.subtract(entity.position()).normalize().scale(pull);
            entity.setDeltaMovement(entity.getDeltaMovement().add(toward));
            entity.hurt(SpellCombat.magic(caster), damage);
            entity.hurtMarked = true;
            spawnSpatialParticles(level, entity.position().add(0, 1, 0));
        }
        cutSphere(level, caster, center.add(0, 0.5, 0), Math.min(radius * 0.55f, 4f), power, 64);
        spawnSpatialParticles(level, center.add(0, 1, 0));
    }

    public static void absoluteFold(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int veilTicks = effect.params().has("veil_ticks") ? effect.params().get("veil_ticks").getAsInt() : 100;
        blinkAlongLook(caster, effect, power, 1.05, defaultMaxRange(effect, 220));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.INVISIBILITY, veilTicks, 0, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, veilTicks, 1, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, veilTicks, 1, false, false, true));
    }

    /** Open / advance a subspace voyage gate. */
    public static void subspaceVoyage(ServerPlayer caster, SpellEffectEntry effect, float power) {
        SubspaceVoyageService.cast(caster);
        spawnSpatialParticles(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    /**
     * Exile a small cube of realspace blocks into hyperspace (no loot).
     * Matter is classified/queued for future Chaos Reefs — see docs/SUBSPACE.md.
     */
    public static void riftExcise(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        if (ModDimensions.isSubspace(level)) {
            caster.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.effecoria.rift_excise.in_subspace"),
                    true);
            return;
        }

        JsonObject params = effect.params();
        double range = params.has("range") ? params.get("range").getAsDouble() : 8;
        int radius = params.has("radius") ? params.get("radius").getAsInt() : 1;
        radius = Math.max(0, Math.min(4, radius + (power >= 70f ? 1 : 0)));

        HitResult hit = caster.pick(range, 0f, false);
        BlockPos center;
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
            center = blockHit.getBlockPos();
        } else {
            Vec3 aim = caster.getEyePosition().add(caster.getLookAngle().normalize().scale(Math.min(range, 5)));
            center = BlockPos.containing(aim);
        }

        java.util.ArrayList<BlockPos> targets = new java.util.ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > (radius + 0.5f) * (radius + 0.5f)) {
                        continue;
                    }
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    targets.add(cursor.immutable());
                }
            }
        }

        SubspaceMatterService.ExileResult result = SubspaceMatterService.exileVolume(level, caster, targets);

        spawnSpatialParticles(level, Vec3.atCenterOf(center));
        level.playSound(
                null,
                center,
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                0.9f,
                0.45f);
        if (result.removed() > 0) {
            if (result.placedInSubspace() && result.dumpCorner() != null) {
                BlockPos dump = result.dumpCorner();
                caster.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "message.effecoria.rift_excise.done_at",
                                result.removed(),
                                dump.getX(),
                                dump.getY(),
                                dump.getZ()),
                        true);
            } else {
                caster.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "message.effecoria.rift_excise.done_queued", result.removed()),
                        true);
            }
        } else {
            caster.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.effecoria.rift_excise.none"),
                    true);
        }
    }

    private static double defaultMaxRange(SpellEffectEntry effect, double fallback) {
        return effect.params().has("max_range") ? effect.params().get("max_range").getAsDouble() : fallback;
    }

    private static void blinkAlongLook(
            ServerPlayer caster, SpellEffectEntry effect, float power, double rangeScale, double maxCap) {
        ServerLevel level = caster.serverLevel();
        double range = effect.params().has("range") ? effect.params().get("range").getAsDouble() : 10;
        double minRange = effect.params().has("min_range") ? effect.params().get("min_range").getAsDouble() : 2;
        range = Math.min(maxCap, range * rangeScale * (0.85 + power / 120f));

        Vec3 look = caster.getLookAngle().normalize();
        Vec3 origin = caster.position();
        Vec3 best = null;
        for (double dist = range; dist >= minRange; dist -= 0.5) {
            Vec3 candidate = origin.add(look.scale(dist));
            BlockPos feet = BlockPos.containing(candidate.x, candidate.y, candidate.z);
            BlockPos head = feet.above();
            if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()) {
                continue;
            }
            if (!level.getBlockState(head).getCollisionShape(level, head).isEmpty()) {
                continue;
            }
            best = new Vec3(candidate.x, feet.getY(), candidate.z);
            break;
        }
        if (best == null) {
            return;
        }
        Vec3 from = origin.add(0, 1, 0);
        Vec3 to = best.add(0, 1, 0);
        SpatialVfx.playRipple(caster, from, power * 0.85f);
        caster.teleportTo(best.x, best.y, best.z);
        caster.fallDistance = 0f;
        SpatialVfx.playRipple(caster, to, power);
        level.playSound(null, caster.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9f, 1.1f);
    }

    private static void spawnBlinkTrail(ServerLevel level, Vec3 from, Vec3 to) {
        // Spatial VFX is distortion-only; trail particles removed.
    }

    private static void hurtRadius(ServerLevel level, Vec3 center, float radius, float damage, ServerPlayer skip) {
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == skip) {
                continue;
            }
            if (entity.position().distanceToSqr(center) > radius * radius) {
                continue;
            }
            entity.hurt(SpellCombat.magic(skip), damage);
            entity.hurtMarked = true;
        }
    }

    /** Rift-cut blocks from the caster's eyes to the hit point (skips the first metre). */
    private static void cutAlongCasterLook(ServerPlayer caster, Vec3 to, float power) {
        if (to == null) {
            return;
        }
        cutAlongSegment(caster.serverLevel(), caster, caster.getEyePosition(), to, power, 1.0);
    }

    private static void cutAlongSegment(
            ServerLevel level, ServerPlayer caster, Vec3 from, Vec3 to, float power, double skipMeters) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 0.2) {
            return;
        }
        Vec3 dir = delta.scale(1.0 / length);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        java.util.HashSet<BlockPos> seen = new java.util.HashSet<>();
        int steps = Math.max(1, (int) Math.ceil(length * 4.0));
        int startStep = Math.max(0, (int) Math.ceil(skipMeters * 4.0));
        for (int i = startStep; i <= steps; i++) {
            Vec3 point = from.add(dir.scale(i * 0.25));
            cursor.set(point.x, point.y, point.z);
            BlockPos immutable = cursor.immutable();
            if (!seen.add(immutable)) {
                continue;
            }
            if (isNearCasterFeet(caster, immutable)) {
                continue;
            }
            tryRiftCut(level, caster, immutable, power);
        }
    }

    private static void cutSphere(
            ServerLevel level, ServerPlayer caster, Vec3 center, float radius, float power, int budget) {
        int broken = 0;
        int r = Math.max(1, (int) Math.ceil(radius));
        BlockPos origin = BlockPos.containing(center);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        float r2 = radius * radius;
        for (int dx = -r; dx <= r && broken < budget; dx++) {
            for (int dy = -r; dy <= r && broken < budget; dy++) {
                for (int dz = -r; dz <= r && broken < budget; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r2 + 0.01f) {
                        continue;
                    }
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (isNearCasterFeet(caster, cursor)) {
                        continue;
                    }
                    if (tryRiftCut(level, caster, cursor.immutable(), power)) {
                        broken++;
                    }
                }
            }
        }
    }

    private static boolean isNearCasterFeet(ServerPlayer caster, BlockPos pos) {
        BlockPos feet = caster.blockPosition();
        return pos.getX() == feet.getX()
                && pos.getZ() == feet.getZ()
                && pos.getY() >= feet.getY() - 1
                && pos.getY() <= feet.getY() + 1;
    }

    /**
     * Simple break with drops — spatial rifts shear matter that isn't indestructible.
     * Hardness gate scales with cast power; bedrock / portals / barriers stay forbidden.
     */
    private static boolean tryRiftCut(ServerLevel level, ServerPlayer caster, BlockPos pos, float power) {
        var state = level.getBlockState(pos);
        if (!canRiftCut(state, level, pos, power)) {
            return false;
        }
        return level.destroyBlock(pos, true, caster);
    }

    private static boolean canRiftCut(
            net.minecraft.world.level.block.state.BlockState state, ServerLevel level, BlockPos pos, float power) {
        if (!SubspaceMatterService.canExile(state, level, pos)) {
            return false;
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0f) {
            return false;
        }
        return hardness <= maxRiftCutHardness(power);
    }

    private static float maxRiftCutHardness(float power) {
        // Dirt/sand early → stone mid → deepslate-ish at high mastery. Never obsidian/bedrock.
        return Math.min(5f, 1.2f + power / 22f);
    }

    private static void finishHit(ServerLevel level, Vec3 pos) {
        level.playSound(null, BlockPos.containing(pos), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.6f, 1.3f);
    }

    /** No-op: Spatial school uses Veil distortion instead of particles. */
    public static void spawnSpatialParticles(ServerLevel level, Vec3 pos) {
        // intentionally empty
    }
}
