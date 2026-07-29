package com.effecoria.event;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.effect.elemental.ElementalEffects;
import com.effecoria.effect.elemental.ElementalTags;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

import com.effecoria.EffecoriaMod;

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
}
