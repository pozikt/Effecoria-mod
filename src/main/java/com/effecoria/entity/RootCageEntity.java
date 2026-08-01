package com.effecoria.entity;

import java.util.UUID;

import javax.annotation.Nullable;

import com.effecoria.content.ModEntities;
import com.effecoria.core.formula.BreathDebuffs;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Static mangrove-root prison that stuns a captive until duration ends or they struggle free.
 */
public class RootCageEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_WIDTH =
            SynchedEntityData.defineId(RootCageEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT =
            SynchedEntityData.defineId(RootCageEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_INTEGRITY =
            SynchedEntityData.defineId(RootCageEntity.class, EntityDataSerializers.FLOAT);

    private UUID captiveId;
    private UUID ownerId;
    private int lifeTicks;
    private float integrity;
    private float maxIntegrity = 20f;
    private boolean savedNoAi;
    private boolean hadNoAi;

    public RootCageEntity(EntityType<? extends RootCageEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static RootCageEntity bind(
            ServerLevel level, LivingEntity target, @Nullable ServerPlayer owner, int durationTicks, float power) {
        RootCageEntity cage = ModEntities.ROOT_CAGE.get().create(level);
        if (cage == null) {
            return null;
        }
        cage.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), 0f);
        cage.configure(target, owner, durationTicks, power);
        level.addFreshEntity(cage);
        level.playSound(null, target.blockPosition(), SoundEvents.ROOTED_DIRT_PLACE, SoundSource.PLAYERS, 1.0f, 0.75f);
        level.playSound(null, target.blockPosition(), SoundEvents.AZALEA_PLACE, SoundSource.PLAYERS, 0.8f, 0.65f);
        return cage;
    }

    /** Remove other cages holding the same captive near the target. */
    private static void clearExistingNear(ServerLevel level, LivingEntity target) {
        var box = target.getBoundingBox().inflate(4.0);
        for (RootCageEntity cage : level.getEntitiesOfClass(RootCageEntity.class, box)) {
            if (cage.captiveId != null && cage.captiveId.equals(target.getUUID()) && !cage.isRemoved()) {
                cage.release(false);
            }
        }
    }

    private void configure(LivingEntity target, @Nullable ServerPlayer owner, int durationTicks, float power) {
        if (level() instanceof ServerLevel server) {
            clearExistingNear(server, target);
        }
        this.captiveId = target.getUUID();
        this.ownerId = owner != null ? owner.getUUID() : null;
        this.lifeTicks = Math.max(20, durationTicks);

        float width = Math.max(0.6f, target.getBbWidth() + 0.35f);
        float height = Math.max(1.0f, target.getBbHeight() + 0.25f);
        entityData.set(DATA_WIDTH, width);
        entityData.set(DATA_HEIGHT, height);
        refreshDimensions();

        this.maxIntegrity = Mth.clamp(8f + target.getMaxHealth() * 0.2f + power * 0.08f, 8f, 48f);
        this.integrity = this.maxIntegrity;
        entityData.set(DATA_INTEGRITY, 1.0f);

        if (target instanceof Mob mob) {
            this.hadNoAi = mob.isNoAi();
            mob.setNoAi(true);
            this.savedNoAi = true;
        }

        pinEffects(target, this.lifeTicks);
        snapCaptive(target);
    }

    private void pinEffects(LivingEntity target, int ticks) {
        ServerPlayer caster = resolveOwner();
        BreathDebuffs.applyExact(
                target,
                new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        ticks,
                        BreathDebuffs.scaleAmplifier(caster, 3),
                        false,
                        false,
                        true));
        BreathDebuffs.applyExact(
                target,
                new MobEffectInstance(
                        MobEffects.DIG_SLOWDOWN,
                        ticks,
                        BreathDebuffs.scaleAmplifier(caster, 2),
                        false,
                        false,
                        true));
        BreathDebuffs.applyExact(
                target,
                new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0, false, false, true));
    }

    @Nullable
    private ServerPlayer resolveOwner() {
        if (ownerId == null || !(level() instanceof ServerLevel server)) {
            return null;
        }
        return server.getServer().getPlayerList().getPlayer(ownerId);
    }

    @Nullable
    private LivingEntity resolveCaptive() {
        if (captiveId == null || !(level() instanceof ServerLevel server)) {
            return null;
        }
        Entity entity = server.getEntity(captiveId);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    public float getCageWidth() {
        return entityData.get(DATA_WIDTH);
    }

    public float getCageHeight() {
        return entityData.get(DATA_HEIGHT);
    }

    /** 1 = intact, 0 = about to shatter. */
    public float getIntegrityRatio() {
        return entityData.get(DATA_INTEGRITY);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_WIDTH, 1.0f);
        builder.define(DATA_HEIGHT, 1.8f);
        builder.define(DATA_INTEGRITY, 1.0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }

        lifeTicks--;
        LivingEntity captive = resolveCaptive();
        if (captive == null) {
            discard();
            return;
        }

        snapCaptive(captive);
        tickStruggle(captive);

        if (lifeTicks <= 0) {
            release(true);
            return;
        }

        // Refresh short stun so it cannot be milked off easily mid-bind.
        if (tickCount % 20 == 0) {
            pinEffects(captive, Math.max(25, lifeTicks));
        }

        entityData.set(DATA_INTEGRITY, Mth.clamp(integrity / maxIntegrity, 0f, 1f));
        if (integrity <= 0f) {
            breakFree(captive);
        }
    }

    private void snapCaptive(LivingEntity captive) {
        captive.teleportTo(getX(), getY(), getZ());
        captive.setDeltaMovement(Vec3.ZERO);
        captive.setYRot(getYRot());
        captive.setXRot(0f);
        captive.setYHeadRot(getYRot());
        captive.hurtMarked = true;
        captive.fallDistance = 0f;
        captive.setOnGround(true);
    }

    private void tickStruggle(LivingEntity captive) {
        if (captive instanceof Player player) {
            boolean trying = Math.abs(player.xxa) > 0.01f
                    || Math.abs(player.zza) > 0.01f
                    || player.isSprinting()
                    || player.isShiftKeyDown();
            if (trying && tickCount % 8 == 0) {
                weaken(2.2f);
                level().playSound(
                        null,
                        blockPosition(),
                        SoundEvents.ROOTED_DIRT_HIT,
                        SoundSource.PLAYERS,
                        0.35f,
                        0.9f + random.nextFloat() * 0.2f);
            }
            return;
        }

        if (tickCount % 15 == 0) {
            float attack = 2f;
            if (captive.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                attack = (float) captive.getAttributeValue(Attributes.ATTACK_DAMAGE);
            }
            float struggle = Mth.clamp(1.1f + attack * 0.65f + captive.getBbWidth() * 0.8f, 1.2f, 10f);
            // Bigger / stronger mobs tear free faster.
            weaken(struggle);
            level().playSound(
                    null,
                    blockPosition(),
                    SoundEvents.ROOTED_DIRT_HIT,
                    SoundSource.NEUTRAL,
                    0.4f,
                    0.7f + random.nextFloat() * 0.25f);
        }
    }

    private void weaken(float amount) {
        integrity = Math.max(0f, integrity - amount);
        entityData.set(DATA_INTEGRITY, Mth.clamp(integrity / maxIntegrity, 0f, 1f));
    }

    private void breakFree(LivingEntity captive) {
        level().playSound(null, blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.0f, 0.85f);
        level().playSound(null, blockPosition(), SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.PLAYERS, 0.7f, 0.9f);
        restoreCaptiveAi(captive);
        captive.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        captive.removeEffect(MobEffects.DIG_SLOWDOWN);
        captive.removeEffect(MobEffects.WEAKNESS);
        discard();
    }

    private void release(boolean naturalExpire) {
        LivingEntity captive = resolveCaptive();
        if (captive != null) {
            restoreCaptiveAi(captive);
            if (naturalExpire) {
                level().playSound(null, blockPosition(), SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.PLAYERS, 0.6f, 0.7f);
            }
        }
        discard();
    }

    private void restoreCaptiveAi(LivingEntity captive) {
        if (savedNoAi && captive instanceof Mob mob) {
            mob.setNoAi(hadNoAi);
            savedNoAi = false;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved() || amount <= 0f) {
            return false;
        }
        Entity attacker = source.getEntity();
        if (ownerId != null && attacker != null && ownerId.equals(attacker.getUUID())) {
            // Owner can chop their own roots open quickly.
            weaken(amount * 1.8f);
        } else {
            weaken(amount * 1.15f);
        }
        level().playSound(null, blockPosition(), SoundEvents.ROOTED_DIRT_HIT, SoundSource.PLAYERS, 0.5f, 1.0f);
        if (integrity <= 0f) {
            LivingEntity captive = resolveCaptive();
            if (captive != null) {
                breakFree(captive);
            } else {
                discard();
            }
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(getCageWidth(), getCageHeight());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Captive")) {
            captiveId = tag.getUUID("Captive");
        }
        if (tag.hasUUID("Owner")) {
            ownerId = tag.getUUID("Owner");
        }
        lifeTicks = tag.getInt("Life");
        integrity = tag.getFloat("Integrity");
        maxIntegrity = Math.max(1f, tag.getFloat("MaxIntegrity"));
        entityData.set(DATA_WIDTH, Math.max(0.5f, tag.getFloat("CageW")));
        entityData.set(DATA_HEIGHT, Math.max(0.8f, tag.getFloat("CageH")));
        entityData.set(DATA_INTEGRITY, Mth.clamp(integrity / maxIntegrity, 0f, 1f));
        savedNoAi = tag.getBoolean("SavedNoAi");
        hadNoAi = tag.getBoolean("HadNoAi");
        refreshDimensions();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (captiveId != null) {
            tag.putUUID("Captive", captiveId);
        }
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putInt("Life", lifeTicks);
        tag.putFloat("Integrity", integrity);
        tag.putFloat("MaxIntegrity", maxIntegrity);
        tag.putFloat("CageW", getCageWidth());
        tag.putFloat("CageH", getCageHeight());
        tag.putBoolean("SavedNoAi", savedNoAi);
        tag.putBoolean("HadNoAi", hadNoAi);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            LivingEntity captive = resolveCaptive();
            if (captive != null) {
                restoreCaptiveAi(captive);
            }
        }
        super.remove(reason);
    }
}
