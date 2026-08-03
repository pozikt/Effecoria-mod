package com.effecoria.entity;

import java.util.Optional;
import java.util.UUID;

import com.effecoria.content.ModEntities;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.effect.mental.MirageWorldService;

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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Serpentine mirage horror — real entity, rendered only for the bound victim.
 * Deals illusory moral damage, never real HP.
 */
public class MirageHorrorEntity extends Mob implements GeoEntity {
    private static final EntityDataAccessor<Optional<UUID>> DATA_VICTIM =
            SynchedEntityData.defineId(MirageHorrorEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_LUNGING =
            SynchedEntityData.defineId(MirageHorrorEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("animation.mirage_horror.idle");
    private static final RawAnimation LUNGE =
            RawAnimation.begin().thenPlay("animation.mirage_horror.lunge");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int lungeCooldown;
    private int lifeTicks = 20 * 40;

    public MirageHorrorEntity(EntityType<? extends MirageHorrorEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setSilent(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public static MirageHorrorEntity spawnFor(ServerPlayer victim, double x, double y, double z) {
        ServerLevel level = victim.serverLevel();
        MirageHorrorEntity horror = ModEntities.MIRAGE_HORROR.get().create(level);
        if (horror == null) {
            return null;
        }
        horror.moveTo(x, y, z, victim.getYRot() + 180f, 0f);
        horror.setVictim(victim.getUUID());
        level.addFreshEntity(horror);
        level.playSound(
                null,
                victim.blockPosition(),
                SoundEvents.WARDEN_AGITATED,
                SoundSource.HOSTILE,
                0.55f,
                0.55f);
        victim.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.effecoria.mental.mirage_horror_seen"),
                true);
        return horror;
    }

    public void setVictim(UUID uuid) {
        entityData.set(DATA_VICTIM, Optional.ofNullable(uuid));
    }

    public Optional<UUID> getVictimId() {
        return entityData.get(DATA_VICTIM);
    }

    public boolean isBoundTo(UUID playerId) {
        return getVictimId().filter(playerId::equals).isPresent();
    }

    public boolean isLunging() {
        return entityData.get(DATA_LUNGING);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VICTIM, Optional.empty());
        builder.define(DATA_LUNGING, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        lifeTicks--;
        if (lifeTicks <= 0) {
            discard();
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        UUID victimId = getVictimId().orElse(null);
        if (victimId == null) {
            discard();
            return;
        }
        ServerPlayer victim = serverLevel.getServer().getPlayerList().getPlayer(victimId);
        if (victim == null || !victim.isAlive() || !MirageWorldService.isActive(victim)) {
            discard();
            return;
        }

        // Drift toward the soul.
        Vec3 to = victim.position().subtract(position());
        double dist = to.length();
        if (dist > 0.05) {
            Vec3 dir = to.normalize();
            double speed = dist > 10 ? 0.18 : 0.09;
            setDeltaMovement(dir.scale(speed));
            float yaw = (float) (Mth.atan2(dir.z, dir.x) * (180f / Math.PI)) - 90f;
            setYRot(yaw);
            setYBodyRot(yaw);
            setYHeadRot(yaw);
        }

        if (lungeCooldown > 0) {
            lungeCooldown--;
            if (lungeCooldown == 12) {
                entityData.set(DATA_LUNGING, false);
            }
        } else if (dist < 4.5) {
            performFearStrike(victim);
            lungeCooldown = 55;
            entityData.set(DATA_LUNGING, true);
        }

        if (tickCount % 8 == 0) {
            serverLevel.sendParticles(
                    ModParticleTypes.MENTAL_FEAR.get(),
                    getX(),
                    getY() + 1.6,
                    getZ(),
                    4,
                    0.4,
                    0.8,
                    0.4,
                    0.01);
        }
    }

    private void performFearStrike(ServerPlayer victim) {
        BreathDebuffs.apply(victim, new MobEffectInstance(MobEffects.CONFUSION, 80, 2, false, false, true));
        MirageWorldService.applyMoralDamage(victim, 5.5f);
        victim.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.effecoria.mental.mirage_horror_strike"),
                true);
        level().playSound(
                null,
                victim.blockPosition(),
                SoundEvents.WARDEN_ATTACK_IMPACT,
                SoundSource.HOSTILE,
                0.7f,
                0.7f);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {}

    @Override
    public void push(net.minecraft.world.entity.Entity entity) {}

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        getVictimId().ifPresent(id -> tag.putUUID("Victim", id));
        tag.putInt("Life", lifeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Victim")) {
            setVictim(tag.getUUID("Victim"));
        }
        lifeTicks = tag.getInt("Life");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, state -> {
            if (isLunging()) {
                state.setAnimation(LUNGE);
                return PlayState.CONTINUE;
            }
            state.setAnimation(IDLE);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
