package com.effecoria.effect.elemental;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.magic.SpellEffectEntry;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Four aggregate states: liquid water, steam gas, ice solid, plasma. */
public final class ElementalEffects {
    private ElementalEffects() {}

    /** Starter fire — almost harmless at low concentration; scales toward real burns. */
    public static void weakFireball(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        Vec3 look = caster.getLookAngle().normalize();
        float speed = effect.params().has("speed") ? effect.params().get("speed").getAsFloat() : 1.2f;
        speed *= 0.8f + (power / 120f);

        float baseDamage = effect.params().has("damage") ? effect.params().get("damage").getAsFloat() : 1.5f;
        float scaledDamage = baseDamage * (power / 50f);

        if (power >= 55f && effect.params().has("plasma_threshold")
                && power >= effect.params().get("plasma_threshold").getAsFloat()) {
            plasmaBolt(caster, effect, power);
            return;
        }

        Snowball bolt = new Snowball(level, caster);
        bolt.setPos(caster.getX(), caster.getEyeY() - 0.1, caster.getZ());
        bolt.shoot(look.x, look.y, look.z, speed, 1f);
        tagProjectile(bolt, ElementalTags.KIND_WEAK_FIRE, scaledDamage);
        level.addFreshEntity(bolt);

        level.playSound(null, caster.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.6f, 1.3f);
        spawnFireParticles(level, caster.getEyePosition());
    }

    /** Liquid lash — a stinging slap at low power. */
    public static void waterLash(ServerPlayer caster, SpellEffectEntry effect, float power) {
        waterStream(caster, effect, power, false);
    }

    /** High-pressure cutter — shreds vegetation and stone at mastery. */
    public static void hydroSlice(ServerPlayer caster, SpellEffectEntry effect, float power) {
        waterStream(caster, effect, power, true);
    }

    /** Gaseous steam jet — scalding cone that leaves lingering fog. */
    public static void steamJet(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        double range = effect.params().has("range") ? effect.params().get("range").getAsDouble() : 8;
        float damage = effect.params().has("damage") ? effect.params().get("damage").getAsFloat() : 4f;
        float knockback = effect.params().has("knockback") ? effect.params().get("knockback").getAsFloat() : 1.6f;
        float cloudRadius = effect.params().has("cloud_radius") ? effect.params().get("cloud_radius").getAsFloat() : 2.5f;
        int cloudDuration = effect.params().has("cloud_duration_ticks")
                ? effect.params().get("cloud_duration_ticks").getAsInt()
                : 60;

        Vec3 look = caster.getLookAngle().normalize();
        Vec3 start = caster.getEyePosition();
        float scaledDamage = damage * (power / 50f);
        float scaledKnock = knockback * (power / 50f);
        float scaledCloudRadius = cloudRadius * (0.85f + power / 120f);
        int scaledCloudDuration = Math.round(cloudDuration * (0.85f + power / 100f));
        DamageSource source = caster.level().damageSources().magic();

        LivingEntity nearestHit = null;
        double nearestDist = Double.MAX_VALUE;

        AABB sweep = caster.getBoundingBox().expandTowards(look.scale(range)).inflate(1.5);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != caster && e.isAlive())) {
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(start);
            double along = toTarget.dot(look);
            if (along < 0 || along > range || toTarget.subtract(look.scale(along)).lengthSqr() > 2.5) {
                continue;
            }
            target.hurt(source, scaledDamage);
            target.push(look.x * scaledKnock, 0.2, look.z * scaledKnock);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            target.hurtMarked = true;
            spawnSteamBurst(level, target.position());
            if (along < nearestDist) {
                nearestDist = along;
                nearestHit = target;
            }
        }

        spawnSteamBeam(level, start, look, range);

        Vec3 trailCenter = start.add(look.scale(range * 0.55));
        SteamCloudService.spawn(
                level, trailCenter, scaledCloudRadius, scaledCloudDuration, caster.getUUID(), true);
        if (nearestHit != null) {
            SteamCloudService.spawn(
                    level,
                    nearestHit.position().add(0, 0.8, 0),
                    scaledCloudRadius * 0.85f,
                    Math.round(scaledCloudDuration * 0.75f),
                    caster.getUUID(),
                    true);
        }

        level.playSound(null, caster.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.9f, 0.7f);
    }

    /** Stationary steam fog volume around the cast point — does not follow the caster. */
    public static void steamVeil(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 100;
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 4f;
        duration = Math.round(duration * (0.75f + power / 100f));
        radius *= 0.85f + power / 120f;

        Vec3 center = caster.position().add(0, 1.0, 0);
        SteamCloudService.spawn(level, center, radius, duration, caster.getUUID(), false);

        level.playSound(null, caster.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7f, 1.1f);
    }

    /** Solid ice projectile — requires concentration; slows and chills. */
    public static void iceShard(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        Vec3 look = caster.getLookAngle().normalize();
        float speed = effect.params().has("speed") ? effect.params().get("speed").getAsFloat() : 1.5f;
        float baseDamage = effect.params().has("damage") ? effect.params().get("damage").getAsFloat() : 4f;
        float scaledDamage = baseDamage * (power / 50f);

        Snowball shard = new Snowball(level, caster);
        shard.setPos(caster.getX(), caster.getEyeY() - 0.1, caster.getZ());
        shard.shoot(look.x, look.y, look.z, speed, 0.5f);
        tagProjectile(shard, ElementalTags.KIND_ICE_SHARD, scaledDamage);
        level.addFreshEntity(shard);

        level.playSound(null, caster.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.4f);
        spawnIceParticles(level, caster.getEyePosition());
    }

    /** Ice rampart — rises gradually bottom-up along the aim line. */
    public static void frostBastion(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        int width = effect.params().has("width") ? effect.params().get("width").getAsInt() : 3;
        int height = effect.params().has("height") ? effect.params().get("height").getAsInt() : 3;
        int depth = effect.params().has("depth") ? effect.params().get("depth").getAsInt() : 1;
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 300;
        double range = effect.params().has("range") ? effect.params().get("range").getAsDouble() : 4;
        int riseDelay = effect.params().has("rise_delay_ticks") ? effect.params().get("rise_delay_ticks").getAsInt() : 2;

        HitResult hit = caster.pick(range + 2, 0f, false);
        BlockPos anchor = hit.getType() == HitResult.Type.BLOCK
                ? ((BlockHitResult) hit).getBlockPos().relative(((BlockHitResult) hit).getDirection())
                : BlockPos.containing(caster.getEyePosition().add(caster.getLookAngle().scale(range)));

        Vec3 look = caster.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0, look.z);
        if (horizontal.lengthSqr() < 1.0E-4) {
            horizontal = new Vec3(caster.getLookAngle().x, 0, caster.getLookAngle().z);
        }
        horizontal = horizontal.normalize();
        Vec3 right = new Vec3(-horizontal.z, 0, horizontal.x);

        BlockState ice = power >= 60f ? Blocks.BLUE_ICE.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
        List<BlockPos> targets = new ArrayList<>();
        for (int d = 0; d < depth; d++) {
            BlockPos rowBase = anchor.offset(
                    (int) Math.round(horizontal.x * d),
                    0,
                    (int) Math.round(horizontal.z * d));
            for (int w = -(width / 2); w <= width / 2; w++) {
                for (int h = 0; h < height; h++) {
                    BlockPos pos = rowBase.offset(
                            (int) Math.round(right.x * w),
                            h,
                            (int) Math.round(right.z * w));
                    BlockState current = level.getBlockState(pos);
                    if (current.canBeReplaced() || current.isAir()) {
                        targets.add(pos.immutable());
                    }
                }
            }
        }

        if (targets.isEmpty()) {
            return;
        }

        BlockPos center = anchor;
        List<BlockPos> ordered = ElementalBlockService.risingOrder(targets, center);
        long startTick = level.getGameTime();
        for (int i = 0; i < ordered.size(); i++) {
            BlockPos pos = ordered.get(i);
            long placeAt = startTick + (long) i * riseDelay;
            ElementalBlockService.scheduleTemporary(level, pos, ice, duration, placeAt);
        }

        level.playSound(null, anchor, SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 0.7f, 1.1f);
        spawnIceParticles(level, Vec3.atCenterOf(anchor));
    }

    /** Plasma bolt — high-energy projectile with fire and magic damage. */
    public static void plasmaBolt(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        Vec3 look = caster.getLookAngle().normalize();
        float speed = effect.params().has("speed") ? effect.params().get("speed").getAsFloat() : 2f;
        speed *= 0.9f + (power / 80f);
        float baseDamage = effect.params().has("damage") ? effect.params().get("damage").getAsFloat() : 10f;
        float scaledDamage = baseDamage * (power / 50f);

        Snowball plasma = new Snowball(level, caster);
        plasma.setPos(caster.getX(), caster.getEyeY() - 0.1, caster.getZ());
        plasma.shoot(look.x, look.y, look.z, speed, 0.5f);
        tagProjectile(plasma, ElementalTags.KIND_PLASMA, scaledDamage);
        level.addFreshEntity(plasma);

        level.playSound(null, caster.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.8f, 1.6f);
        level.sendParticles(ModParticleTypes.PHI_FLAME.get(), caster.getX(), caster.getEyeY(), caster.getZ(), 8, 0.1, 0.1, 0.1, 0.03);
    }

    public static void windCharge(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        Vec3 look = caster.getLookAngle().normalize();
        float speed = effect.params().has("speed") ? effect.params().get("speed").getAsFloat() : 1.25f;
        speed *= 0.9f + (power / 120f);

        WindCharge charge = new WindCharge(caster, level, caster.getX(), caster.getEyeY() - 0.1, caster.getZ());
        charge.shoot(look.x, look.y, look.z, speed, 0f);
        level.addFreshEntity(charge);
        level.playSound(null, caster.blockPosition(), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 1f, 1f);
        for (int i = 1; i <= 6; i++) {
            Vec3 p = caster.getEyePosition().add(look.scale(i * 0.35));
            level.sendParticles(
                    ModParticleTypes.PHI_GUST.get(),
                    p.x,
                    p.y,
                    p.z,
                    2,
                    look.x * 0.08,
                    look.y * 0.08,
                    look.z * 0.08,
                    0.02);
        }
    }

    private static void waterStream(ServerPlayer caster, SpellEffectEntry effect, float power, boolean cutter) {
        ServerLevel level = caster.serverLevel();
        JsonObject params = effect.params();
        double range = params.get("range").getAsDouble();
        float damage = params.get("damage").getAsFloat();
        float knockback = params.has("knockback") ? params.get("knockback").getAsFloat() : 1.2f;
        int slowTicks = params.has("slow_ticks") ? params.get("slow_ticks").getAsInt() : 40;
        boolean extinguish = !params.has("extinguish_fire") || params.get("extinguish_fire").getAsBoolean();

        Vec3 look = caster.getLookAngle().normalize();
        Vec3 start = caster.getEyePosition();
        // Hydro-slice matches diamond-axe strike damage; soft water lash still scales with power.
        float scaledDamage = cutter
                ? diamondAxeDamage() * Mth.clamp(0.85f + power / 200f, 0.85f, 1.25f)
                : damage * (power / 50f);
        float scaledKnock = knockback * (power / 50f);
        DamageSource source = cutter
                ? caster.level().damageSources().playerAttack(caster)
                : caster.level().damageSources().magic();

        AABB sweep = caster.getBoundingBox().expandTowards(look.scale(range)).inflate(1.5);
        Set<LivingEntity> hit = new HashSet<>();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != caster && e.isAlive())) {
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(start);
            double along = toTarget.dot(look);
            if (along < 0 || along > range || toTarget.subtract(look.scale(along)).lengthSqr() > 2.5) {
                continue;
            }
            hit.add(target);
        }

        for (LivingEntity target : hit) {
            target.hurt(source, scaledDamage);
            target.push(look.x * scaledKnock, 0.15, look.z * scaledKnock);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 0));
            if (extinguish) {
                target.clearFire();
            }
            target.hurtMarked = true;
            spawnWaterHit(level, target.position());
        }

        if (cutter) {
            cutBlocksAlongBeam(level, start, look, range, power);
        } else if (extinguish) {
            extinguishFireAlongBeam(level, start, look, range);
        }

        spawnWaterBeam(level, start, look, range);
        level.playSound(
                null,
                caster.blockPosition(),
                cutter ? SoundEvents.GENERIC_EXPLODE.value() : SoundEvents.PLAYER_SPLASH_HIGH_SPEED,
                SoundSource.PLAYERS,
                cutter ? 0.4f : 0.8f,
                cutter ? 1.6f : 1.1f);
    }

    private static void cutBlocksAlongBeam(ServerLevel level, Vec3 start, Vec3 look, double range, float power) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int steps = (int) (range * 4);
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(look.scale(i * 0.25));
            pos.set(point.x, point.y, point.z);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !canCut(state, power)) {
                continue;
            }
            level.destroyBlock(pos, true);
            level.sendParticles(ModParticleTypes.WATER_SPLASH.get(), point.x, point.y, point.z, 4, 0.1, 0.1, 0.1, 0.05);
        }
    }

    private static boolean canCut(BlockState state, float power) {
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS) || state.is(BlockTags.REPLACEABLE)) {
            return power >= 20f;
        }
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS)) {
            return power >= 40f;
        }
        float hardness = state.getDestroySpeed(null, null);
        if (hardness >= 0f && hardness <= 1.5f) {
            return power >= 55f;
        }
        return false;
    }

    private static void extinguishFireAlongBeam(ServerLevel level, Vec3 start, Vec3 look, double range) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int steps = (int) (range * 4);
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(look.scale(i * 0.25));
            pos.set(point.x, point.y, point.z);
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.FIRE)) {
                level.removeBlock(pos, false);
            }
        }
    }

    private static void tagProjectile(net.minecraft.world.entity.projectile.Projectile projectile, String kind, float damage) {
        projectile.getPersistentData().putBoolean(ElementalTags.PROJECTILE, true);
        projectile.getPersistentData().putString(ElementalTags.KIND, kind);
        projectile.getPersistentData().putFloat(ElementalTags.POWER, damage);
    }

    /** Vanilla diamond axe attack damage (1.21). */
    public static float diamondAxeDamage() {
        return 9f;
    }

    /**
     * Large fire orb that sheds mass (and fire) on each block scrape; floor impact ignites a wide patch.
     */
    public static void greatFireball(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        JsonObject params = effect.params();
        Vec3 look = caster.getLookAngle().normalize();
        float speed = params.has("speed") ? params.get("speed").getAsFloat() : 1.05f;
        speed *= 0.85f + power / 140f;
        float baseDamage = params.has("damage") ? params.get("damage").getAsFloat() : 10f;
        float scaledDamage = baseDamage * (power / 50f);
        int mass = params.has("fire_mass") ? params.get("fire_mass").getAsInt() : 4;
        int igniteRadius = params.has("ignite_radius") ? params.get("ignite_radius").getAsInt() : 2;
        int groundIgnite = params.has("ground_ignite_count") ? params.get("ground_ignite_count").getAsInt() : 9;

        Snowball orb = new Snowball(level, caster);
        orb.setPos(caster.getX(), caster.getEyeY() - 0.1, caster.getZ());
        orb.shoot(look.x, look.y, look.z, speed, 0.6f);
        tagProjectile(orb, ElementalTags.KIND_GREAT_FIRE, scaledDamage);
        orb.getPersistentData().putInt(ElementalTags.FIRE_MASS, Math.max(1, mass));
        orb.getPersistentData().putInt(ElementalTags.IGNITE_RADIUS, Math.max(1, igniteRadius));
        orb.getPersistentData().putInt(ElementalTags.GROUND_IGNITE_COUNT, Math.max(1, groundIgnite));
        level.addFreshEntity(orb);

        level.playSound(null, caster.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 0.85f, 0.7f);
        spawnFireParticles(level, caster.getEyePosition());
        level.sendParticles(
                ParticleTypes.FLAME,
                caster.getX() + look.x,
                caster.getEyeY() + look.y,
                caster.getZ() + look.z,
                18,
                0.25,
                0.25,
                0.25,
                0.04);
    }

    /** Start / boost steam-powered elytra-style flight; continuous drain is handled by {@link SteamFlightService}. */
    public static void steamFlight(ServerPlayer caster, SpellEffectEntry effect, float power) {
        JsonObject params = effect.params();
        float drainPerSecond = params.has("drain_per_second")
                ? params.get("drain_per_second").getAsFloat()
                : 4f;
        float drainPerTick = drainPerSecond / 20f * (0.9f + 10f / Math.max(20f, power));
        float boost = params.has("boost") ? params.get("boost").getAsFloat() : 1.5f;
        boost *= 0.9f + power / 160f;

        SteamFlightService.activate(caster, drainPerTick, boost);

        ServerLevel level = caster.serverLevel();
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 trail = caster.position().add(look.scale(-0.6));
        SteamCloudService.spawn(level, trail, 1.6f * (0.85f + power / 120f), 40, caster.getUUID(), false);
        spawnSteamBurst(level, caster.position());
        level.playSound(null, caster.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.9f, 1.15f);
    }

    public static void ignitePatch(ServerLevel level, BlockPos center, int radius, int maxFires) {
        int placed = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -1; dy <= 1 && placed < maxFires; dy++) {
            for (int dx = -radius; dx <= radius && placed < maxFires; dx++) {
                for (int dz = -radius; dz <= radius && placed < maxFires; dz++) {
                    if (dx * dx + dz * dz > radius * radius + 1) {
                        continue;
                    }
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (tryPlaceFire(level, cursor)) {
                        placed++;
                    }
                }
            }
        }
    }

    public static boolean tryPlaceFire(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) {
            return false;
        }
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (belowState.isAir()) {
            return false;
        }
        BlockState fire = Blocks.FIRE.defaultBlockState();
        if (!fire.canSurvive(level, pos)) {
            return false;
        }
        level.setBlock(pos, fire, 3);
        level.sendParticles(
                ParticleTypes.FLAME,
                pos.getX() + 0.5,
                pos.getY() + 0.2,
                pos.getZ() + 0.5,
                6,
                0.15,
                0.15,
                0.15,
                0.01);
        return true;
    }

    public static void shedFireAt(ServerLevel level, BlockPos hitPos, Vec3 hitLocation, net.minecraft.core.Direction face) {
        BlockPos firePos = hitPos.relative(face);
        if (!tryPlaceFire(level, firePos)) {
            tryPlaceFire(level, hitPos.above());
        }
        spawnFireParticles(level, hitLocation);
        level.sendParticles(ParticleTypes.LAVA, hitLocation.x, hitLocation.y, hitLocation.z, 4, 0.1, 0.1, 0.1, 0.02);
    }

    public static void spawnFireParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.PHI_FLAME.get(), pos.x, pos.y, pos.z, 10, 0.08, 0.08, 0.08, 0.015);
    }

    public static void spawnWaterBeam(ServerLevel level, Vec3 start, Vec3 look, double range) {
        int steps = (int) (range * 4);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = start.add(look.scale(i * 0.25));
            level.sendParticles(ModParticleTypes.WATER_DROP.get(), p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.02);
            if (i % 3 == 0) {
                level.sendParticles(ModParticleTypes.WATER_WAVE.get(), p.x, p.y, p.z, 1, look.x * 0.08, 0, look.z * 0.08, 0.01);
            }
        }
    }

    public static void spawnWaterHit(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.WATER_SPLASH.get(), pos.x, pos.y + 0.5, pos.z, 6, 0.2, 0.15, 0.2, 0.02);
    }

    public static void spawnSteamBeam(ServerLevel level, Vec3 start, Vec3 look, double range) {
        int steps = (int) (range * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = start.add(look.scale(i * 0.3));
            level.sendParticles(ModParticleTypes.STEAM_FOG.get(), p.x, p.y, p.z, 3, 0.12, 0.12, 0.12, 0.01);
        }
    }

    public static void spawnSteamBurst(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.STEAM_FOG.get(), pos.x, pos.y, pos.z, 12, 0.35, 0.4, 0.35, 0.008);
        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.5, pos.z, 4, 0.15, 0.2, 0.15, 0.01);
    }

    public static void spawnIceParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.ICE_CRYSTAL.get(), pos.x, pos.y, pos.z, 8, 0.2, 0.25, 0.2, 0.02);
    }

    public static float clampPowerScale(float power) {
        return Mth.clamp(power / 50f, 0.05f, 3f);
    }
}
