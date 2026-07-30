package com.effecoria.effect.elemental;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.DiceDamage;
import com.effecoria.core.magic.SpellEffectEntry;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
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

        float scaledDamage = DiceDamage.fromParams(effect.params(), power, 1.5f);

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
        float knockback = effect.params().has("knockback") ? effect.params().get("knockback").getAsFloat() : 1.6f;
        float cloudRadius = effect.params().has("cloud_radius") ? effect.params().get("cloud_radius").getAsFloat() : 2.5f;
        int cloudDuration = effect.params().has("cloud_duration_ticks")
                ? effect.params().get("cloud_duration_ticks").getAsInt()
                : 60;
        int blindTicks = effect.params().has("blind_ticks") ? effect.params().get("blind_ticks").getAsInt() : 20;

        Vec3 look = caster.getLookAngle().normalize();
        Vec3 start = caster.getEyePosition();
        float scaledDamage = DiceDamage.fromParams(effect.params(), power, 4f);
        float scaledKnock = knockback * (0.75f + power / 100f);
        float scaledCloudRadius = cloudRadius * (0.85f + power / 120f);
        int scaledCloudDuration = Math.round(cloudDuration * (0.85f + power / 100f));
        DamageSource source = caster.level().damageSources().onFire();

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
            if (blindTicks > 0) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blindTicks, 0));
            }
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
        float scaledDamage = DiceDamage.fromParams(effect.params(), power, 4f);

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
        float scaledDamage = DiceDamage.fromParams(effect.params(), power, 10f);
        PlayerPsiData psi = PsiHelper.get(caster);
        scaledDamage += psi.takeIonChargeBonus();
        PsiHelper.set(caster, psi);
        caster.syncData(ModAttachments.PSI.get());

        Snowball plasma = new Snowball(level, caster);
        plasma.setPos(caster.getX(), caster.getEyeY() - 0.1, caster.getZ());
        plasma.shoot(look.x, look.y, look.z, speed, 0.5f);
        tagProjectile(plasma, ElementalTags.KIND_PLASMA, scaledDamage);
        level.addFreshEntity(plasma);

        level.playSound(null, caster.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.8f, 1.6f);
        level.sendParticles(ModParticleTypes.PHI_FLAME.get(), caster.getX(), caster.getEyeY(), caster.getZ(), 8, 0.1, 0.1, 0.1, 0.03);
    }

    /** Soft breeze cantrip — light push along the look direction, no real damage. */
    public static void weakBreeze(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        JsonObject params = effect.params();
        double range = params.has("range") ? params.get("range").getAsDouble() : 5;
        float knock = params.has("knockback") ? params.get("knockback").getAsFloat() : 0.55f;
        knock *= 0.85f + power / 140f;

        Vec3 look = caster.getLookAngle().normalize();
        Vec3 start = caster.getEyePosition();
        AABB sweep = caster.getBoundingBox().expandTowards(look.scale(range)).inflate(1.1);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != caster && e.isAlive())) {
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(start);
            double along = toTarget.dot(look);
            if (along < 0 || along > range || toTarget.subtract(look.scale(along)).lengthSqr() > 1.8) {
                continue;
            }
            target.push(look.x * knock, 0.08, look.z * knock);
            target.hurtMarked = true;
        }

        for (int i = 1; i <= 5; i++) {
            Vec3 p = start.add(look.scale(i * 0.4));
            level.sendParticles(
                    ModParticleTypes.PHI_GUST.get(),
                    p.x,
                    p.y,
                    p.z,
                    1,
                    0.05,
                    0.05,
                    0.05,
                    0.01);
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.BREEZE_IDLE_GROUND, SoundSource.PLAYERS, 0.45f, 1.4f);
    }

    /** Hyper-cooling — cold burst that slows foes and seeds a temporary ice patch. */
    public static void hyperCooling(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        JsonObject params = effect.params();
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 4f;
        int slowTicks = params.has("slow_ticks") ? params.get("slow_ticks").getAsInt() : 80;
        int iceHalf = params.has("ice_half_size") ? params.get("ice_half_size").getAsInt() : 2;
        int iceDuration = params.has("ice_duration_ticks") ? params.get("ice_duration_ticks").getAsInt() : 100;
        iceDuration = Math.round(iceDuration * (0.85f + power / 120f));
        radius *= 0.9f + power / 160f;

        float damage = DiceDamage.fromParams(params, power, 5f);
        Vec3 center = caster.position().add(0, 0.2, 0);
        HitResult hit = caster.pick(10, 0f, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            center = hit.getLocation();
        } else if (hit.getType() == HitResult.Type.ENTITY) {
            center = hit.getLocation();
        }

        AABB box = new AABB(center, center).inflate(radius);
        DamageSource source = level.damageSources().magic();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
            if (target.position().distanceToSqr(center) > (double) radius * radius) {
                continue;
            }
            target.hurt(source, damage);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 1));
            target.setTicksFrozen(Math.min(140, target.getTicksFrozen() + 80));
            target.hurtMarked = true;
            spawnIceParticles(level, target.position().add(0, 1, 0));
        }

        BlockPos groundCenter = BlockPos.containing(center);
        BlockState ice = Blocks.PACKED_ICE.defaultBlockState();
        List<BlockPos> iceTargets = new ArrayList<>();
        for (int dx = -iceHalf; dx <= iceHalf; dx++) {
            for (int dz = -iceHalf; dz <= iceHalf; dz++) {
                if (dx * dx + dz * dz > iceHalf * iceHalf + 1) {
                    continue;
                }
                BlockPos ground = findGround(level, groundCenter.offset(dx, 0, dz));
                if (ground == null) {
                    continue;
                }
                BlockPos above = ground.above();
                BlockState current = level.getBlockState(above);
                if (current.canBeReplaced() || current.isAir()) {
                    iceTargets.add(above.immutable());
                }
            }
        }
        if (!iceTargets.isEmpty()) {
            List<BlockPos> ordered = ElementalBlockService.risingOrder(iceTargets, groundCenter);
            long startTick = level.getGameTime();
            for (int i = 0; i < ordered.size(); i++) {
                ElementalBlockService.scheduleTemporary(level, ordered.get(i), ice, iceDuration, startTick + i);
            }
        }

        level.playSound(null, BlockPos.containing(center), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.9f, 0.7f);
        level.sendParticles(
                ParticleTypes.SNOWFLAKE,
                center.x,
                center.y + 0.5,
                center.z,
                28,
                radius * 0.35,
                0.4,
                radius * 0.35,
                0.02);
        spawnIceParticles(level, center);
    }

    /** Squall — fires a wind charge projectile (D&D level 2). */
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
        double range = params.has("range") ? params.get("range").getAsDouble() : 8;
        float knockback = params.has("knockback") ? params.get("knockback").getAsFloat() : 1.2f;
        int slowTicks = params.has("slow_ticks") ? params.get("slow_ticks").getAsInt() : 40;
        boolean extinguish = !params.has("extinguish_fire") || params.get("extinguish_fire").getAsBoolean();

        Vec3 look = caster.getLookAngle().normalize();
        Vec3 start = caster.getEyePosition();
        // Hydro-slice: 3d8 slash ≈ diamond-axe class; soft lash uses dice/legacy params.
        float scaledDamage = cutter
                ? DiceDamage.fromParams(params, power, diamondAxeDamage())
                : DiceDamage.fromParams(params, power, 2f);
        float scaledKnock = knockback * (0.75f + power / 100f);
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
        float scaledDamage = DiceDamage.fromParams(params, power, 10f);
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

    /**
     * Grab a living target with a wind-formed hand and drag it with the caster's look.
     * Cast again to release. Returns false if no target and not already holding.
     */
    public static boolean airHand(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        JsonObject params = effect.params();
        float holdDistance = params.has("hold_distance") ? params.get("hold_distance").getAsFloat() : 3.5f;
        holdDistance *= 0.9f + power / 200f;
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 160;
        duration = Math.round(duration * (0.85f + power / 120f));
        float drainPerSecond = params.has("drain_per_second") ? params.get("drain_per_second").getAsFloat() : 2.5f;
        return AirHandService.toggleOrGrab(caster, target, holdDistance, duration, drainPerSecond);
    }

    /** Submerge and root a target in a temporary water shell. */
    public static void waterPrison(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        JsonObject params = effect.params();
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 2f;
        radius *= 0.9f + power / 160f;
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 100;
        duration = Math.round(duration * (0.85f + power / 120f));
        float dps = DiceDamage.perSecondFromParams(params, power, 2f);
        ElementalCageService.imprisonWater(
                caster.serverLevel(), target, caster.getUUID(), radius, duration, dps);
    }

    /** Absolute vacuum sphere around the looked-at target (or aim point). */
    public static void vacuumCage(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        JsonObject params = effect.params();
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 3f;
        radius *= 0.9f + power / 160f;
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 100;
        duration = Math.round(duration * (0.85f + power / 120f));
        float dps = DiceDamage.perSecondFromParams(params, power, 2.5f);
        Vec3 center = target != null
                ? target.position().add(0, target.getBbHeight() * 0.5, 0)
                : caster.getEyePosition().add(caster.getLookAngle().normalize().scale(4));
        ElementalCageService.imprisonVacuumAoE(
                caster.serverLevel(), center, caster.getUUID(), radius, duration, dps);
    }

    /** Ice cocoon prison — packed-ice shell + cold DoT (D&D level 6). */
    public static void icePrison(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        JsonObject params = effect.params();
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 2f;
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 120;
        duration = Math.round(duration * (0.85f + power / 120f));
        float dps = DiceDamage.perSecondFromParams(params, power, 2f);
        ElementalCageService.imprisonIce(
                caster.serverLevel(), target, caster.getUUID(), radius, duration, dps);
    }

    /** Shockwave — compressed air burst around the caster (D&D level 3). */
    public static void shockwave(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        JsonObject params = effect.params();
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 3f;
        float knock = params.has("knockback") ? params.get("knockback").getAsFloat() : 1.4f;
        int stunTicks = params.has("stun_ticks") ? params.get("stun_ticks").getAsInt() : 20;
        float damage = DiceDamage.fromParams(params, power, 7f);
        AABB box = caster.getBoundingBox().inflate(radius);
        DamageSource source = level.damageSources().magic();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
            target.hurt(source, damage);
            Vec3 away = target.position().subtract(caster.position()).normalize();
            target.push(away.x * knock, 0.35, away.z * knock);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stunTicks, 2));
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, stunTicks, 0));
            target.hurtMarked = true;
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.4f);
        level.sendParticles(
                ModParticleTypes.PHI_GUST.get(),
                caster.getX(),
                caster.getY() + 1,
                caster.getZ(),
                28,
                radius * 0.4,
                0.4,
                radius * 0.4,
                0.08);
    }

    /** Ionize air — primes the next plasma (electric) strike with bonus damage. */
    public static void airIonization(ServerPlayer caster, SpellEffectEntry effect, float power) {
        JsonObject params = effect.params();
        int chargeTicks = params.has("charge_ticks") ? params.get("charge_ticks").getAsInt() : 600;
        chargeTicks = Math.round(chargeTicks * (0.85f + power / 120f));
        JsonObject bonusParams = new JsonObject();
        if (params.has("bonus_dice")) {
            bonusParams.addProperty("damage_dice", params.get("bonus_dice").getAsString());
        } else {
            bonusParams.addProperty("damage_dice", "2d6");
        }
        float bonus = DiceDamage.fromParams(bonusParams, power, 3f);

        PlayerPsiData data = PsiHelper.get(caster);
        data.activateIonCharge(chargeTicks, bonus);
        PsiHelper.set(caster, data);
        caster.syncData(ModAttachments.PSI.get());

        ServerLevel level = caster.serverLevel();
        caster.addEffect(new MobEffectInstance(MobEffects.GLOWING, Math.min(chargeTicks, 200), 0, false, false, true));
        level.playSound(null, caster.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5f, 1.7f);
        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                caster.getX(),
                caster.getEyeY(),
                caster.getZ(),
                16,
                0.35,
                0.35,
                0.35,
                0.12);
    }

    /** Mirage — caster shimmers; nearby foes struggle to aim (blindness + glow). */
    public static void mirage(ServerPlayer caster, SpellEffectEntry effect, float power) {
        JsonObject params = effect.params();
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 200;
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 4f;
        float drainPerSecond = params.has("maintain_drain_per_second")
                ? params.get("maintain_drain_per_second").getAsFloat()
                : 2f;
        duration = Math.round(duration * (0.8f + power / 100f));
        radius *= 0.85f + power / 120f;

        ElementalFieldService.spawnMirage(caster.serverLevel(), caster, radius, duration, drainPerSecond);
    }

    /** Tornado — moving wind column that lifts small foes and shreds them. */
    public static void tornado(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        JsonObject params = effect.params();
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 2.5f;
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 80;
        float drainPerSecond = params.has("maintain_drain_per_second")
                ? params.get("maintain_drain_per_second").getAsFloat()
                : 2f;
        float knock = params.has("knockback") ? params.get("knockback").getAsFloat() : 1.2f;
        float liftMaxHealth = params.has("lift_max_health") ? params.get("lift_max_health").getAsFloat() : 30f;
        float moveSpeed = params.has("move_speed") ? params.get("move_speed").getAsFloat() : 0.15f;
        moveSpeed *= 0.9f + power / 160f;

        float dps = DiceDamage.perSecondFromParams(params, power, 3f);

        Vec3 look = caster.getLookAngle().normalize();
        Vec3 start = caster.position().add(look.scale(3)).add(0, 0.5, 0);
        HitResult hit = caster.pick(12, 0f, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            start = hit.getLocation();
        }

        ElementalFieldService.spawnTornado(
                level,
                start,
                look,
                caster.getUUID(),
                radius,
                duration,
                drainPerSecond,
                dps,
                knock,
                liftMaxHealth,
                moveSpeed);
    }

    /** Ion storm — stationary electric zone (DoT + concentration drain). */
    public static void ionStorm(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        JsonObject params = effect.params();
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 4.5f;
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 160;
        float drainPerSecond = params.has("maintain_drain_per_second")
                ? params.get("maintain_drain_per_second").getAsFloat()
                : 3f;
        duration = Math.round(duration * (0.85f + power / 120f));
        radius *= 0.85f + power / 120f;

        float dps = DiceDamage.perSecondFromParams(params, power, 4f);

        Vec3 center = caster.position().add(0, 0.5, 0);
        HitResult hit = caster.pick(10, 0f, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            center = hit.getLocation();
        }

        ElementalFieldService.spawnIonStorm(
                level, center, caster.getUUID(), radius, duration, drainPerSecond, dps);
    }

    /** Ice sheet — slick packed ice spreads outward from the aim point (D&D level 3). */
    public static void iceSheet(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        JsonObject params = effect.params();
        int half = params.has("half_size") ? params.get("half_size").getAsInt() : 3;
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 160;
        duration = Math.round(duration * (0.85f + power / 120f));
        int riseDelay = params.has("rise_delay_ticks") ? params.get("rise_delay_ticks").getAsInt() : 1;

        BlockPos center = caster.blockPosition();
        HitResult hit = caster.pick(8, 0f, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            center = ((BlockHitResult) hit).getBlockPos();
        }

        BlockState ice = Blocks.PACKED_ICE.defaultBlockState();
        List<BlockPos> targets = new ArrayList<>();
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                BlockPos ground = findGround(level, center.offset(dx, 0, dz));
                if (ground == null) {
                    continue;
                }
                BlockPos above = ground.above();
                BlockState current = level.getBlockState(above);
                if (current.canBeReplaced() || current.isAir()) {
                    targets.add(above.immutable());
                }
            }
        }
        if (targets.isEmpty()) {
            return;
        }

        // Same staged placement as frost bastion: center-out over successive ticks.
        BlockPos orderCenter = targets.get(0);
        double best = Double.MAX_VALUE;
        for (BlockPos pos : targets) {
            double d = pos.distSqr(center);
            if (d < best) {
                best = d;
                orderCenter = pos;
            }
        }
        List<BlockPos> ordered = ElementalBlockService.risingOrder(targets, orderCenter);
        long startTick = level.getGameTime();
        for (int i = 0; i < ordered.size(); i++) {
            long placeAt = startTick + (long) i * riseDelay;
            ElementalBlockService.scheduleTemporary(level, ordered.get(i), ice, duration, placeAt);
        }

        level.playSound(null, center, SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 0.8f, 1.2f);
        spawnIceParticles(level, Vec3.atCenterOf(center));
    }

    private static BlockPos findGround(ServerLevel level, BlockPos start) {
        BlockPos.MutableBlockPos cursor = start.mutable();
        for (int i = 0; i < 6; i++) {
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced()) {
                return cursor.immutable();
            }
            cursor.move(0, -1, 0);
        }
        return null;
    }

    /** Breath bubble — water breathing for the target (D&D level 3). */
    public static void breathBubble(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        LivingEntity subject = target != null ? target : caster;
        JsonObject params = effect.params();
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 12000;
        duration = Math.round(duration * (0.85f + power / 120f));
        subject.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, duration, 0, false, true, true));
        subject.clearFire();
        ServerLevel level = caster.serverLevel();
        level.playSound(null, subject.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.PLAYERS, 0.9f, 1.2f);
        level.sendParticles(
                ParticleTypes.BUBBLE,
                subject.getX(),
                subject.getY() + subject.getBbHeight(),
                subject.getZ(),
                16,
                0.3,
                0.3,
                0.3,
                0.02);
    }

    /** Water shield — brief absorption/resistance (D&D level 1, simplified concentration). */
    public static void waterShield(ServerPlayer caster, SpellEffectEntry effect, float power) {
        JsonObject params = effect.params();
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 200;
        duration = Math.round(duration * (0.85f + power / 120f));
        int amp = power >= 50f ? 1 : 0;
        caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, amp, false, true, true));
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true, true));
        float maintain = params.has("maintain_drain") ? params.get("maintain_drain").getAsFloat() : 0.05f;
        if (maintain > 0f) {
            PlayerPsiData data = PsiHelper.get(caster);
            data.setCurrentPsi(Math.max(0f, data.currentPsi() - maintain * 20f));
            PsiHelper.set(caster, data);
            caster.syncData(ModAttachments.PSI.get());
        }
        ServerLevel level = caster.serverLevel();
        level.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.7f, 1.3f);
        spawnWaterHit(level, caster.position().add(0, 1, 0));
    }

    /** Sonic lance — focused compressed-air beam (D&D level 4). */
    public static void sonicLance(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        JsonObject params = effect.params();
        double range = params.has("range") ? params.get("range").getAsDouble() : 48;
        float damage = DiceDamage.fromParams(params, power, 7f);
        float knock = params.has("knockback") ? params.get("knockback").getAsFloat() : 0.8f;
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 start = caster.getEyePosition();
        DamageSource source = level.damageSources().magic();
        AABB sweep = caster.getBoundingBox().expandTowards(look.scale(range)).inflate(1.2);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != caster && e.isAlive())) {
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(start);
            double along = toTarget.dot(look);
            if (along < 0 || along > range || toTarget.subtract(look.scale(along)).lengthSqr() > 2.0) {
                continue;
            }
            target.hurt(source, damage);
            target.push(look.x * knock, 0.12, look.z * knock);
            target.hurtMarked = true;
        }
        int steps = (int) (range * 2);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = start.add(look.scale(i * 0.5));
            level.sendParticles(ModParticleTypes.PHI_GUST.get(), p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.01);
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 1f, 0.7f);
    }

    /** Lightning spear — strike an aim point; nearby foes take fire/stun (D&D level 5). */
    public static void lightningSpear(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        JsonObject params = effect.params();
        double range = params.has("range") ? params.get("range").getAsDouble() : 40;
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 7.5f;
        int stunTicks = params.has("stun_ticks") ? params.get("stun_ticks").getAsInt() : 40;
        float executeBelow = params.has("execute_below_health") ? params.get("execute_below_health").getAsFloat() : 8f;
        float damage = DiceDamage.fromParams(params, power, 12f);

        Vec3 look = caster.getLookAngle().normalize();
        Vec3 center = caster.getEyePosition().add(look.scale(Math.min(range, 12)));
        HitResult hit = caster.pick(range, 0f, false);
        if (hit.getType() != HitResult.Type.MISS) {
            center = hit.getLocation();
        }

        DamageSource source = level.damageSources().magic();
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
            if (target.position().distanceToSqr(center) > (double) radius * radius) {
                continue;
            }
            target.hurt(source, damage);
            target.hurt(level.damageSources().onFire(), damage * 0.35f);
            target.igniteForSeconds(3);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stunTicks, 3));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, stunTicks, 1));
            // Soft execute: fragile non-boss leftovers drop from residual voltage.
            if (target.getHealth() > 0f
                    && target.getHealth() <= executeBelow
                    && target.getMaxHealth() <= 80f) {
                target.hurt(source, target.getHealth() + 1f);
            }
            target.hurtMarked = true;
        }

        level.playSound(
                null,
                BlockPos.containing(center),
                SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.PLAYERS,
                0.8f,
                1.2f);
        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                center.x,
                center.y + 0.5,
                center.z,
                48,
                radius * 0.3,
                0.8,
                radius * 0.3,
                0.2);
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 0.5, center.z, 1, 0, 0, 0, 0);
    }

    public static void waterShroud(ServerPlayer caster, SpellEffectEntry effect, float power) {
        JsonObject params = effect.params();
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 200;
        float drain = params.has("maintain_drain_per_second")
                ? params.get("maintain_drain_per_second").getAsFloat()
                : 5f;
        duration = Math.round(duration * (0.85f + power / 120f));
        ElementalShroudService.activate(caster, ElementalShroudService.Kind.WATER, duration, drain);
    }

    public static void airShroud(ServerPlayer caster, SpellEffectEntry effect, float power) {
        JsonObject params = effect.params();
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 200;
        float drain = params.has("maintain_drain_per_second")
                ? params.get("maintain_drain_per_second").getAsFloat()
                : 5f;
        duration = Math.round(duration * (0.85f + power / 120f));
        ElementalShroudService.activate(caster, ElementalShroudService.Kind.AIR, duration, drain);
    }

    public static void atmosphericPressure(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        JsonObject params = effect.params();
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 6f;
        int stunTicks = params.has("stun_ticks") ? params.get("stun_ticks").getAsInt() : 30;
        float damage = DiceDamage.fromParams(params, power, 10f);
        radius *= 0.9f + power / 160f;

        Vec3 center = aimPoint(caster, 12);
        DamageSource source = level.damageSources().magic();
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
            if (target.position().distanceToSqr(center) > (double) radius * radius) {
                continue;
            }
            target.hurt(source, damage);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stunTicks, 4));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, stunTicks, 2));
            target.push(0, -0.35, 0);
            target.hurtMarked = true;
        }
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 0.5f);
        level.sendParticles(
                ParticleTypes.CLOUD,
                center.x,
                center.y + 0.5,
                center.z,
                30,
                radius * 0.35,
                0.3,
                radius * 0.35,
                0.04);
    }

    public static void cryoWave(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        JsonObject params = effect.params();
        double range = params.has("range") ? params.get("range").getAsDouble() : 9;
        float coneHalf = params.has("cone_half_width") ? params.get("cone_half_width").getAsFloat() : 2.8f;
        int slowTicks = params.has("slow_ticks") ? params.get("slow_ticks").getAsInt() : 40;
        float damage = DiceDamage.fromParams(params, power, 14f);

        Vec3 look = caster.getLookAngle().normalize();
        Vec3 start = caster.getEyePosition();
        DamageSource source = level.damageSources().magic();
        AABB sweep = caster.getBoundingBox().expandTowards(look.scale(range)).inflate(coneHalf);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != caster && e.isAlive())) {
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(start);
            double along = toTarget.dot(look);
            if (along < 0 || along > range) {
                continue;
            }
            double radial = toTarget.subtract(look.scale(along)).length();
            if (radial > coneHalf * (0.35 + along / range)) {
                continue;
            }
            target.hurt(source, damage);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 2));
            target.setTicksFrozen(Math.min(200, target.getTicksFrozen() + 100));
            target.hurtMarked = true;
            spawnIceParticles(level, target.position().add(0, 1, 0));
        }

        // Freeze moisture along the cone into short-lived ice.
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 1; i <= (int) range; i++) {
            Vec3 p = start.add(look.scale(i));
            cursor.set(p.x, p.y - 1, p.z);
            BlockPos ground = findGround(level, cursor);
            if (ground == null) {
                continue;
            }
            BlockPos above = ground.above();
            if (level.getBlockState(above).canBeReplaced() || level.getBlockState(above).isAir()) {
                ElementalBlockService.scheduleTemporary(
                        level, above, Blocks.PACKED_ICE.defaultBlockState(), 80, level.getGameTime() + i);
            }
        }

        for (int i = 0; i <= (int) (range * 2); i++) {
            Vec3 p = start.add(look.scale(i * 0.5));
            level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 2, 0.15, 0.1, 0.15, 0.01);
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1f, 0.6f);
    }

    public static void airForm(ServerPlayer caster, SpellEffectEntry effect, float power) {
        JsonObject params = effect.params();
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 12000;
        duration = Math.round(duration * (0.85f + power / 140f));
        ElementalShroudService.activate(caster, ElementalShroudService.Kind.AIR_FORM, duration, 0f);
    }

    public static void hurricaneStorm(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        JsonObject params = effect.params();
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 7.5f;
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 160;
        float drain = params.has("maintain_drain_per_second")
                ? params.get("maintain_drain_per_second").getAsFloat()
                : 4f;
        float knock = params.has("knockback") ? params.get("knockback").getAsFloat() : 1.8f;
        duration = Math.round(duration * (0.85f + power / 120f));
        float dps = DiceDamage.perSecondFromParams(params, power, 5f);
        Vec3 center = aimPoint(caster, 14);
        ElementalFieldService.spawnHurricane(
                level, center, caster.getUUID(), radius, duration, drain, dps, knock);
    }

    public static void elementalSupremacy(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        JsonObject params = effect.params();
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 15f;
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 200;
        float drain = params.has("maintain_drain_per_second")
                ? params.get("maintain_drain_per_second").getAsFloat()
                : 6f;
        duration = Math.round(duration * (0.85f + power / 120f));
        float dps = DiceDamage.perSecondFromParams(params, power, 4f);
        ElementalFieldService.spawnSupremacy(
                level, caster.position().add(0, 0.5, 0), caster.getUUID(), radius, duration, drain, dps);
    }

    public static void thermonuclearPulse(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        JsonObject params = effect.params();
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 9f;
        int blindTicks = params.has("blind_ticks") ? params.get("blind_ticks").getAsInt() : 40;
        float damage = DiceDamage.fromParams(params, power, 22f);
        Vec3 center = aimPoint(caster, 16);
        DamageSource source = level.damageSources().magic();
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
            if (target.position().distanceToSqr(center) > (double) radius * radius) {
                continue;
            }
            target.hurt(source, damage);
            target.hurt(level.damageSources().onFire(), damage * 0.5f);
            target.igniteForSeconds(8);
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blindTicks, 0));
            target.hurtMarked = true;
        }
        ignitePatch(level, BlockPos.containing(center), Math.max(2, Math.round(radius * 0.4f)), 18);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 0.55f);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 0.5, center.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.5, center.z, 60, radius * 0.35, 0.5, radius * 0.35, 0.08);
    }

    public static void absoluteZero(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        JsonObject params = effect.params();
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 6f;
        int freezeTicks = params.has("freeze_ticks") ? params.get("freeze_ticks").getAsInt() : 30;
        float damage = DiceDamage.fromParams(params, power, 20f);
        Vec3 center = aimPoint(caster, 12);
        DamageSource source = level.damageSources().magic();
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
            if (target.position().distanceToSqr(center) > (double) radius * radius) {
                continue;
            }
            target.hurt(source, damage);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, freezeTicks, 6));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, freezeTicks, 4));
            target.setTicksFrozen(Math.min(300, target.getTicksFrozen() + 200));
            target.setDeltaMovement(Vec3.ZERO);
            target.hurtMarked = true;
            spawnIceParticles(level, target.position().add(0, 1, 0));
        }
        BlockPos ground = BlockPos.containing(center);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos g = findGround(level, ground.offset(dx, 0, dz));
                if (g == null) {
                    continue;
                }
                BlockPos above = g.above();
                if (level.getBlockState(above).canBeReplaced() || level.getBlockState(above).isAir()) {
                    ElementalBlockService.scheduleTemporary(
                            level, above, Blocks.BLUE_ICE.defaultBlockState(), 120, level.getGameTime());
                }
            }
        }
        level.playSound(null, ground, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1f, 0.4f);
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.5, center.z, 50, radius * 0.3, 0.5, radius * 0.3, 0.02);
    }

    public static void meteorologicalCataclysm(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        JsonObject params = effect.params();
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 28f;
        float damage = DiceDamage.fromParams(params, power, 28f);
        Vec3 center = caster.position();
        DamageSource source = level.damageSources().magic();
        AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
            if (target.position().distanceToSqr(center) > (double) radius * radius) {
                continue;
            }
            target.hurt(source, damage * 0.45f);
            target.hurt(level.damageSources().onFire(), damage * 0.3f);
            target.setTicksFrozen(Math.min(200, target.getTicksFrozen() + 60));
            Vec3 away = target.position().subtract(center);
            if (away.lengthSqr() > 1.0e-4) {
                away = away.normalize();
                target.push(away.x * 1.2, 0.45, away.z * 1.2);
            }
            target.hurtMarked = true;
        }
        ElementalFieldService.spawnTornado(
                level,
                center.add(3, 0.5, 0),
                new Vec3(1, 0, 0.2),
                caster.getUUID(),
                3.5f,
                60,
                0f,
                damage / 8f,
                1.5f,
                40f,
                0.2f);
        level.playSound(null, caster.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.2f, 0.6f);
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 3, center.z, 80, radius * 0.25, 2.0, radius * 0.25, 0.05);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 2, center.z, 40, radius * 0.2, 1.5, radius * 0.2, 0.1);
    }

    public static void quasar(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        JsonObject params = effect.params();
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 7.5f;
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 1200;
        float dps = DiceDamage.perSecondFromParams(params, power, 12f);
        Vec3 center = aimPoint(caster, 16);
        ElementalFieldService.spawnQuasar(level, center, caster.getUUID(), radius, duration, dps);
    }

    public static void plasmaBarrage(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        JsonObject params = effect.params();
        int count = params.has("count") ? params.get("count").getAsInt() : 10;
        float speed = params.has("speed") ? params.get("speed").getAsFloat() : 2.2f;
        float spread = params.has("spread") ? params.get("spread").getAsFloat() : 4.5f;
        float damage = DiceDamage.fromParams(params, power, 18f);
        Vec3 look = caster.getLookAngle().normalize();
        for (int i = 0; i < count; i++) {
            Snowball plasma = new Snowball(level, caster);
            plasma.setPos(caster.getX(), caster.getEyeY() - 0.1, caster.getZ());
            float inaccuracy = spread * (0.35f + (i % 5) * 0.15f);
            plasma.shoot(look.x, look.y, look.z, speed, inaccuracy);
            tagProjectile(plasma, ElementalTags.KIND_PLASMA, damage);
            level.addFreshEntity(plasma);
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1f, 1.4f);
        level.sendParticles(
                ModParticleTypes.PHI_FLAME.get(),
                caster.getX(),
                caster.getEyeY(),
                caster.getZ(),
                20,
                0.3,
                0.3,
                0.3,
                0.05);
    }

    private static Vec3 aimPoint(ServerPlayer caster, double range) {
        HitResult hit = caster.pick(range, 0f, false);
        if (hit.getType() != HitResult.Type.MISS) {
            return hit.getLocation();
        }
        return caster.getEyePosition().add(caster.getLookAngle().normalize().scale(range * 0.5));
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
