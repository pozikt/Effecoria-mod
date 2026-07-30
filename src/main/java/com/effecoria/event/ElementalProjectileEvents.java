package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.effect.elemental.ElementalEffects;
import com.effecoria.effect.elemental.ElementalTags;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class ElementalProjectileEvents {
    private ElementalProjectileEvents() {}

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof Projectile projectile)) {
            return;
        }
        if (!projectile.getPersistentData().getBoolean(ElementalTags.PROJECTILE)) {
            return;
        }
        if (projectile.level().isClientSide()) {
            return;
        }

        HitResult hit = event.getRayTraceResult();
        String kind = projectile.getPersistentData().getString(ElementalTags.KIND);
        float damage = projectile.getPersistentData().getFloat(ElementalTags.POWER);
        ServerLevel level = (ServerLevel) projectile.level();

        if (ElementalTags.KIND_GREAT_FIRE.equals(kind)) {
            handleGreatFireImpact(event, projectile, hit, damage, level);
            return;
        }

        if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult entityHit) {
            if (entityHit.getEntity() instanceof LivingEntity target && target.isAlive()) {
                DamageSource source = level.damageSources().indirectMagic(projectile, projectile.getOwner());
                switch (kind) {
                    case ElementalTags.KIND_WEAK_FIRE -> {
                        target.hurt(source, Math.max(0.5f, damage));
                        if (damage >= 3f) {
                            target.igniteForSeconds(2);
                        }
                        ElementalEffects.spawnFireParticles(level, target.position());
                    }
                    case ElementalTags.KIND_ICE_SHARD -> {
                        target.hurt(source, damage);
                        int slowAmp = damage >= 6f ? 2 : 1;
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, slowAmp));
                        ElementalEffects.spawnIceParticles(level, target.position().add(0, 1, 0));
                    }
                    case ElementalTags.KIND_PLASMA -> {
                        target.hurt(source, damage);
                        target.hurt(level.damageSources().onFire(), damage * 0.4f);
                        target.igniteForSeconds(4);
                        level.sendParticles(
                                ModParticleTypes.PHI_FLAME.get(),
                                target.getX(),
                                target.getY() + 1,
                                target.getZ(),
                                10,
                                0.2,
                                0.3,
                                0.2,
                                0.04);
                    }
                    default -> {}
                }
                target.hurtMarked = true;
            }
        }

        if (projectile instanceof Snowball) {
            event.setCanceled(true);
            projectile.discard();
        }
    }

    private static void handleGreatFireImpact(
            ProjectileImpactEvent event,
            Projectile projectile,
            HitResult hit,
            float damage,
            ServerLevel level) {
        event.setCanceled(true);

        if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult entityHit) {
            if (entityHit.getEntity() instanceof LivingEntity target && target.isAlive()) {
                DamageSource source = level.damageSources().indirectMagic(projectile, projectile.getOwner());
                target.hurt(source, Math.max(4f, damage));
                target.igniteForSeconds(6);
                target.hurtMarked = true;
                ElementalEffects.spawnFireParticles(level, target.position());
                ElementalEffects.ignitePatch(level, target.blockPosition(), 1, 4);
            }
            projectile.discard();
            return;
        }

        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
            projectile.discard();
            return;
        }

        Direction face = blockHit.getDirection();
        Vec3 loc = blockHit.getLocation();
        boolean groundHit = face == Direction.UP;

        if (groundHit) {
            int count = Math.max(1, projectile.getPersistentData().getInt(ElementalTags.GROUND_IGNITE_COUNT));
            int radius = Math.max(1, projectile.getPersistentData().getInt(ElementalTags.IGNITE_RADIUS));
            ElementalEffects.ignitePatch(level, blockHit.getBlockPos().above(), radius, count);
            level.sendParticles(ParticleTypes.EXPLOSION, loc.x, loc.y + 0.2, loc.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.FLAME, loc.x, loc.y + 0.3, loc.z, 28, 0.9, 0.2, 0.9, 0.05);
            projectile.discard();
            return;
        }

        // Scrape a block: shed one fire mass unit and keep flying.
        int mass = projectile.getPersistentData().getInt(ElementalTags.FIRE_MASS);
        ElementalEffects.shedFireAt(level, blockHit.getBlockPos(), loc, face);
        mass--;
        projectile.getPersistentData().putInt(ElementalTags.FIRE_MASS, mass);
        if (mass <= 0) {
            projectile.discard();
            return;
        }

        Vec3 vel = projectile.getDeltaMovement();
        if (vel.lengthSqr() < 1.0e-4) {
            projectile.discard();
            return;
        }
        // Nudge past the scraped face so the same block is not hit every tick.
        projectile.setPos(projectile.position().add(vel.normalize().scale(0.45)));
        // Slight slowdown as mass is lost.
        projectile.setDeltaMovement(vel.scale(0.92));
    }

    @SubscribeEvent
    public static void onProjectileTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Snowball snowball)) {
            return;
        }
        if (!snowball.getPersistentData().getBoolean(ElementalTags.PROJECTILE)) {
            return;
        }
        if (snowball.level().isClientSide()) {
            return;
        }
        String kind = snowball.getPersistentData().getString(ElementalTags.KIND);
        if (!ElementalTags.KIND_GREAT_FIRE.equals(kind)) {
            return;
        }
        if (!(snowball.level() instanceof ServerLevel level)) {
            return;
        }
        int mass = Math.max(1, snowball.getPersistentData().getInt(ElementalTags.FIRE_MASS));
        level.sendParticles(
                ParticleTypes.FLAME,
                snowball.getX(),
                snowball.getY(),
                snowball.getZ(),
                2 + mass,
                0.2 + mass * 0.05,
                0.2 + mass * 0.05,
                0.2 + mass * 0.05,
                0.01);
        if (snowball.tickCount % 3 == 0) {
            level.sendParticles(
                    ModParticleTypes.PHI_FLAME.get(),
                    snowball.getX(),
                    snowball.getY(),
                    snowball.getZ(),
                    2,
                    0.12,
                    0.12,
                    0.12,
                    0.02);
        }
    }
}
