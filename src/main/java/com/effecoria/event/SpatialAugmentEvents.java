package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.effect.spatial.SpatialAugments;
import com.effecoria.effect.spatial.SpatialPocketData;
import com.effecoria.effect.spatial.SpatialVfx;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class SpatialAugmentEvents {
    private SpatialAugmentEvents() {}

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getProjectile().level().isClientSide()) {
            return;
        }
        HitResult hit = event.getRayTraceResult();
        if (!(hit instanceof EntityHitResult entityHit)) {
            return;
        }
        if (!(entityHit.getEntity() instanceof LivingEntity living)) {
            return;
        }
        long time = living.level().getGameTime();
        if (!SpatialAugments.hasLens(living, time) && !SpatialAugments.hasCocoon(living, time)) {
            return;
        }
        Projectile projectile = event.getProjectile();
        // Lens / cocoon: bend trajectory around the mage and warp space at the contact point.
        event.setCanceled(true);
        Vec3 impact = hit.getLocation();
        Vec3 away = projectile.position().subtract(living.position()).normalize();
        if (away.lengthSqr() < 1.0e-4) {
            away = living.getLookAngle();
        }
        projectile.setDeltaMovement(away.scale(Math.max(0.6, projectile.getDeltaMovement().length())));
        projectile.hasImpulse = true;
        projectile.hurtMarked = true;
        if (living.level() instanceof ServerLevel level) {
            SpatialVfx.playLensBend(level, impact);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        long time = victim.level().getGameTime();
        if (!SpatialAugments.hasCocoon(victim, time)) {
            return;
        }
        // Absolute cocoon — physical/projectile metric folds around the mage.
        // Magic without trajectory (wither/magic tags) still leaks a little.
        var source = event.getSource();
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO)
                || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_EFFECTS)) {
            event.setAmount(event.getAmount() * 0.35f);
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()) {
            return;
        }
        long time = mob.level().getGameTime();
        if (!SpatialAugments.hasTimeLoop(mob, time)) {
            return;
        }
        LivingEntity next = event.getNewAboutToBeSetTarget();
        if (next != null && next.getTags().contains(SpatialAugments.ECHO_TAG)) {
            return;
        }
        // Chronal lock — keep attacking the frozen echo point.
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof Projectile projectile) {
            redirectLoopedProjectile(projectile);
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        long time = living.level().getGameTime();
        SpatialAugments.tickTimeLoop(living, time);

        if (SpatialAugments.hasWallWalk(living, time) && living instanceof ServerPlayer player) {
            // Artificial gravity — climb when pressed into a wall / ceiling.
            if (player.horizontalCollision) {
                double up = player.isShiftKeyDown() ? -0.25 : 0.28;
                player.setDeltaMovement(player.getDeltaMovement().x * 0.7, up, player.getDeltaMovement().z * 0.7);
                player.fallDistance = 0f;
                player.hurtMarked = true;
            } else if (!player.onGround() && lookUp(player)) {
                player.setDeltaMovement(player.getDeltaMovement().x, 0.22, player.getDeltaMovement().z);
                player.fallDistance = 0f;
            }
            if (player.tickCount % 40 == 0) {
                com.effecoria.core.formula.BreathDebuffs.apply(
                        player,
                        new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.CONFUSION, 50, 0, true, false, true));
            }
        }
    }

    /** Projectiles from a looped shooter keep flying at the frozen aim point. */
    private static void redirectLoopedProjectile(Projectile projectile) {
        if (!(projectile.getOwner() instanceof LivingEntity owner)) {
            return;
        }
        long time = owner.level().getGameTime();
        if (!SpatialAugments.hasTimeLoop(owner, time)) {
            return;
        }
        if (projectile.tickCount > 8) {
            return;
        }
        Vec3 aim = SpatialAugments.getLoopAim(owner);
        if (aim == null) {
            return;
        }
        Vec3 dir = aim.subtract(projectile.position());
        if (dir.lengthSqr() < 1.0e-6) {
            return;
        }
        double speed = Math.max(0.85, projectile.getDeltaMovement().length());
        projectile.setDeltaMovement(dir.normalize().scale(speed));
        projectile.hasImpulse = true;
        projectile.hurtMarked = true;
    }

    private static boolean lookUp(ServerPlayer player) {
        return player.getXRot() < -35f;
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide()) {
            return;
        }
        SpatialAugments.clearTimeLoop(dead);

        if (!(dead instanceof ServerPlayer player)) {
            return;
        }
        SpatialPocketData pocket = player.getData(ModAttachments.SPATIAL_POCKET.get());
        if (pocket.isEmpty()) {
            return;
        }
        // Pocket collapses — dump contents near the corpse with scatter.
        for (ItemStack stack : pocket.items()) {
            if (stack.isEmpty()) {
                continue;
            }
            player.drop(stack.copy(), true, false);
        }
        pocket.clear();
        player.setData(ModAttachments.SPATIAL_POCKET.get(), pocket);
        player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.effecoria.spatial.pocket_collapse"),
                false);
    }
}
