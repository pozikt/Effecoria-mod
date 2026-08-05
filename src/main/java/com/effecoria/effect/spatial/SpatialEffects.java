package com.effecoria.effect.spatial;

import com.effecoria.core.formula.SpellCombat;

import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.core.formula.DiceDamage;
import com.effecoria.core.magic.SpellEffectEntry;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.world.ModDimensions;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class SpatialEffects {
    private SpatialEffects() {}

    /** Φ-location — spatial sonar through connected cavities; fades every 5 path-blocks. */
    public static void warpBolt(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 52f;
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 140;
        int attenuateEvery =
                effect.params().has("attenuate_every") ? effect.params().get("attenuate_every").getAsInt() : 5;
        int maxRange = Math.min(64, Math.round(radius * (0.9f + power / 180f)));

        SpatialSenseService.ScanResult scan =
                SpatialSenseService.scan(level, caster.blockPosition(), maxRange, attenuateEvery);
        SpatialSenseService.sendTo(caster, scan, duration);

        int hidden = 0;
        // Only entities inside the sensed network (near a hit) get revealed.
        double revealR = Math.max(4.0, maxRange * 0.35);
        AABB box = caster.getBoundingBox().inflate(revealR);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (!(entity.isInvisible() || entity.hasEffect(MobEffects.INVISIBILITY))) {
                continue;
            }
            if (!nearSenseNetwork(entity.blockPosition(), scan)) {
                continue;
            }
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false, true));
            hidden++;
        }

        if (ModDimensions.isSubspace(level) || caster.getData(ModAttachments.SUBSPACE_VOYAGE.get()).active()) {
            BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false, true));
            PlayerPsiData data = PsiHelper.get(caster);
            data.setEntropyB(data.entropyB() + 0.04f);
            PsiHelper.set(caster, data);
            caster.displayClientMessage(Component.translatable("message.effecoria.spatial.sense_warped"), true);
        }

        caster.displayClientMessage(
                Component.translatable(
                        "message.effecoria.spatial.sense",
                        scan.cavities(),
                        scan.traps(),
                        hidden,
                        scan.maxReach()),
                true);
        SpatialVfx.playRipple(caster, SpatialSenseService.pingCenter(caster), power);
        level.playSound(
                null,
                caster.blockPosition(),
                SoundEvents.BELL_BLOCK,
                SoundSource.PLAYERS,
                0.35f,
                1.55f);
    }

    private static boolean nearSenseNetwork(BlockPos entityPos, SpatialSenseService.ScanResult scan) {
        BlockPos origin = scan.origin();
        for (SpatialSenseService.Hit hit : scan.hits()) {
            BlockPos mark = origin.offset(hit.dx(), hit.dy(), hit.dz());
            if (mark.closerThan(entityPos, 3.5)) {
                return true;
            }
        }
        return entityPos.closerThan(origin, 4.0);
    }

    /** Lens — bend projectile trajectories around the mage. */
    public static void spatialWard(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 160;
        SpatialAugments.setLens(caster, caster.level().getGameTime() + duration);
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true, true));
        caster.displayClientMessage(Component.translatable("message.effecoria.spatial.lens_on"), true);
        SpatialVfx.playRipple(caster, caster.position().add(0, 1, 0), power);
    }

    public static void foldRepulse(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 5f;
        float force = effect.params().has("force") ? effect.params().get("force").getAsFloat() : 2.4f;
        float damage = DiceDamage.fromParams(effect.params(), power, 2f);
        Vec3 center = caster.position().add(0, 0.5, 0);
        AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            Vec3 away = entity.position().subtract(center);
            if (away.lengthSqr() < 1.0e-4) {
                away = caster.getLookAngle();
            } else {
                away = away.normalize();
            }
            double strength = force * (power / 50f);
            entity.setDeltaMovement(entity.getDeltaMovement().add(away.scale(strength)).add(0, 0.35, 0));
            entity.hurt(SpellCombat.magic(caster), damage);
            entity.hurtMarked = true;
        }
        SpatialVfx.playGravityWave(level, center, radius, power);
        level.playSound(null, caster.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.8f, 0.7f);
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

    /** Artificial gravity — walk walls / ceilings for a while. */
    public static void gravitySnare(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 200;
        SpatialAugments.setWallWalk(caster, caster.level().getGameTime() + duration);
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.SLOW_FALLING, duration, 0, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.JUMP, duration, 1, false, false, true));
        caster.displayClientMessage(Component.translatable("message.effecoria.spatial.wall_walk"), true);
        SpatialVfx.playRipple(caster, caster.position().add(0, 1, 0), power);
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
                Math.max(dps, 1.5f),
                power);
        caster.displayClientMessage(Component.translatable("message.effecoria.spatial.gravity_well"), true);
    }

    /** Chronal anomaly — freeze target in a short time loop aimed at the caster's position. */
    public static void dimensionalAnchor(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 100;
        Vec3 aimPoint = caster.getEyePosition();
        SpatialAugments.beginTimeLoop(target, caster.level().getGameTime() + duration, aimPoint);
        // Root lightly — they must still be able to repeat attacks toward the frozen point.
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 2));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.GLOWING, duration, 0));
        SpatialVfx.playRipple(caster, target.position().add(0, 1, 0), power);
        SpatialVfx.playLensBend(caster.serverLevel(), aimPoint);
        // Mage may slip into their own loop (aiming at where they stood).
        if (caster.getRandom().nextFloat() < 0.12f) {
            SpatialAugments.beginTimeLoop(
                    caster, caster.level().getGameTime() + duration / 2, aimPoint);
            caster.displayClientMessage(Component.translatable("message.effecoria.spatial.loop_self"), true);
        }
        PlayerPsiData data = PsiHelper.get(caster);
        data.setEntropyB(data.entropyB() + 0.1f);
        PsiHelper.set(caster, data);
        caster.displayClientMessage(Component.translatable("message.effecoria.spatial.loop_cast"), true);
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
        boolean anchored = hasSpatialAnchor(caster);
        double miss = anchored ? 0.0 : 8.0 + caster.getRandom().nextDouble() * 24.0;
        if (!anchored) {
            caster.displayClientMessage(Component.translatable("message.effecoria.spatial.far_no_anchor"), true);
        }
        blinkAlongLook(caster, effect, power, 1.0, defaultMaxRange(effect, 200));
        if (miss > 0.5) {
            // Without beacon/lodestone — lateral miss.
            double ang = caster.getRandom().nextDouble() * Math.PI * 2;
            caster.teleportTo(
                    caster.getX() + Math.cos(ang) * miss,
                    caster.getY(),
                    caster.getZ() + Math.sin(ang) * miss);
            caster.fallDistance = 0f;
        }
    }

    private static boolean hasSpatialAnchor(ServerPlayer caster) {
        for (int i = 0; i < caster.getInventory().getContainerSize(); i++) {
            var stack = caster.getInventory().getItem(i);
            if (stack.is(Items.COMPASS) || stack.is(Items.RECOVERY_COMPASS)) {
                return true;
            }
        }
        BlockPos origin = caster.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -4, -8), origin.offset(8, 4, 8))) {
            if (caster.level().getBlockState(pos).is(Blocks.BEACON)
                    || caster.level().getBlockState(pos).is(Blocks.LODESTONE)
                    || caster.level().getBlockState(pos).is(Blocks.RESPAWN_ANCHOR)) {
                return true;
            }
        }
        return false;
    }

    public static void standardBlink(ServerPlayer caster, SpellEffectEntry effect, float power) {
        blinkAlongLook(caster, effect, power, 1.0, defaultMaxRange(effect, 24));
    }

    /**
     * Short blink that may pass through at most one solid block along the look ray
     * (thin walls / doors), then land in clear space.
     */
    public static void phaseSlip(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        double range = effect.params().has("range") ? effect.params().get("range").getAsDouble() : 10;
        double minRange = effect.params().has("min_range") ? effect.params().get("min_range").getAsDouble() : 2;
        int maxSolids = effect.params().has("max_solids") ? effect.params().get("max_solids").getAsInt() : 1;
        double maxCap = defaultMaxRange(effect, 18);
        range = Math.min(maxCap, range * (0.85 + power / 120f));

        Vec3 look = caster.getLookAngle().normalize();
        Vec3 origin = caster.position();
        Vec3 eye = caster.getEyePosition();
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
            Vec3 land = new Vec3(candidate.x, feet.getY(), candidate.z);
            if (solidBlocksAlong(level, eye, land.add(0, 1, 0)) > maxSolids) {
                continue;
            }
            best = land;
            break;
        }
        if (best == null) {
            caster.displayClientMessage(Component.translatable("message.effecoria.phase_slip.blocked"), true);
            return;
        }
        Vec3 from = origin.add(0, 1, 0);
        Vec3 to = best.add(0, 1, 0);
        SpatialVfx.playRipple(caster, from, power * 0.85f);
        SpatialVfx.playLensBend(level, from.add(to.subtract(from).scale(0.5)));
        caster.teleportTo(best.x, best.y, best.z);
        caster.fallDistance = 0f;
        SpatialVfx.playRipple(caster, to, power);
        level.playSound(null, caster.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.85f, 1.35f);
    }

    private static int solidBlocksAlong(ServerLevel level, Vec3 from, Vec3 to) {
        java.util.HashSet<BlockPos> solids = new java.util.HashSet<>();
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 0.2) {
            return 0;
        }
        Vec3 dir = delta.scale(1.0 / length);
        int steps = Math.max(1, (int) Math.ceil(length * 4.0));
        for (int i = 1; i < steps; i++) {
            Vec3 point = from.add(dir.scale(i * 0.25));
            BlockPos pos = BlockPos.containing(point);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                solids.add(pos);
            }
        }
        return solids.size();
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

    /** Mini-pulsar — soul-burn AoE; exhausts the caster. */
    public static void spatialSingularity(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 8f;
        float damage = DiceDamage.fromParams(effect.params(), power, 10f);
        Vec3 center = target != null ? target.position() : aim;
        AABB box = new AABB(center, center).inflate(radius);
        boolean cocooned = SpatialAugments.hasCocoon(caster, level.getGameTime());
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster && cocooned) {
                continue;
            }
            if (entity.position().distanceToSqr(center) > radius * radius) {
                continue;
            }
            entity.hurt(SpellCombat.magic(caster), damage);
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.WITHER, 100, 1));
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            entity.hurtMarked = true;
            spawnSpatialParticles(level, entity.position().add(0, 1, 0));
        }
        // Exhaustion — Stage 1-2 feel.
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.HUNGER, 100, 1));
        PlayerPsiData data = PsiHelper.get(caster);
        data.setCurrentPsi(Math.max(0f, data.currentPsi() * 0.25f));
        data.setEntropyB(data.entropyB() + 0.15f);
        PsiHelper.set(caster, data);
        caster.displayClientMessage(Component.translatable("message.effecoria.spatial.pulsar"), true);
        cutSphere(level, caster, center.add(0, 0.5, 0), Math.min(radius * 0.55f, 4f), power, 64);
        SpatialVfx.playAround(caster, center.add(0, 1, 0), power, 4);
        level.playSound(null, BlockPos.containing(center), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.7f, 0.6f);
    }

    /** Space cocoon — absolute short invulnerability fold. */
    public static void absoluteFold(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("veil_ticks") ? effect.params().get("veil_ticks").getAsInt() : 80;
        SpatialAugments.setCocoon(caster, caster.level().getGameTime() + duration);
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 3, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, false, true));
        // Sealed syndrome risk — brief self-lock after.
        if (caster.getRandom().nextFloat() < 0.1f) {
            BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4));
            BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 2));
            caster.displayClientMessage(Component.translatable("message.effecoria.spatial.cocoon_seal"), true);
        } else {
            caster.displayClientMessage(Component.translatable("message.effecoria.spatial.cocoon_on"), true);
        }
        PlayerPsiData data = PsiHelper.get(caster);
        data.setCurrentPsi(Math.max(0f, data.currentPsi() - 12f));
        PsiHelper.set(caster, data);
        SpatialVfx.playRipple(caster, caster.position().add(0, 1, 0), power);
    }

    /** Open the personal spatial pocket (9 slots). */
    public static void spatialPocket(ServerPlayer caster, SpellEffectEntry effect, float power) {
        SpatialPocketData pocket = caster.getData(ModAttachments.SPATIAL_POCKET.get());
        var container = pocket.asContainer();
        caster.openMenu(new SimpleMenuProvider(
                (id, inv, player) -> new ChestMenu(MenuType.GENERIC_9x1, id, inv, container, 1),
                Component.translatable("gui.effecoria.spatial_pocket")));
        // Persist on close via listener already writing into pocket list — flush attachment.
        caster.setData(ModAttachments.SPATIAL_POCKET.get(), pocket);
        SpatialVfx.playPocketOpen(caster, power);
        caster.serverLevel()
                .playSound(null, caster.blockPosition(), SoundEvents.ENDER_CHEST_OPEN, SoundSource.PLAYERS, 0.6f, 1.4f);
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
