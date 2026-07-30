package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.effect.organic.OrganicEffects;
import com.effecoria.effect.organic.OrganicTags;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class OrganicProjectileEvents {
    private OrganicProjectileEvents() {}

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof Projectile projectile)) {
            return;
        }
        if (!projectile.getPersistentData().getBoolean(OrganicTags.PROJECTILE)) {
            return;
        }
        if (projectile.level().isClientSide()) {
            return;
        }
        if (!(projectile.level() instanceof ServerLevel level)) {
            return;
        }
        HitResult hit = event.getRayTraceResult();
        float damage = projectile.getPersistentData().getFloat(OrganicTags.POWER);
        if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult entityHit) {
            if (entityHit.getEntity() instanceof LivingEntity target && target.isAlive()) {
                DamageSource source = level.damageSources().indirectMagic(projectile, projectile.getOwner());
                target.hurt(source, Math.max(0.5f, damage));
                target.hurtMarked = true;
                OrganicEffects.spawnOrganicParticles(level, target.position().add(0, 1, 0));
            }
        }
        if (projectile instanceof Snowball) {
            event.setCanceled(true);
            projectile.discard();
        }
    }
}
