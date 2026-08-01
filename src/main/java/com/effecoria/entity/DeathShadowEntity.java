package com.effecoria.entity;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import com.effecoria.content.ModEntities;
import com.effecoria.content.ModParticleTypes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Semi-transparent player-skin shade that drifts from the living target toward their last death.
 */
public class DeathShadowEntity extends LivingEntity {
    private static final EntityDataAccessor<Optional<UUID>> DATA_SKIN =
            SynchedEntityData.defineId(DeathShadowEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> DATA_NAME =
            SynchedEntityData.defineId(DeathShadowEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_TARGET_X =
            SynchedEntityData.defineId(DeathShadowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Y =
            SynchedEntityData.defineId(DeathShadowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Z =
            SynchedEntityData.defineId(DeathShadowEntity.class, EntityDataSerializers.FLOAT);

    private static final float SPEED = 0.22f;
    private static final int ARRIVE_LINGER = 35;
    private static final int MAX_LIFE = 20 * 25;

    private int lifeTicks = MAX_LIFE;
    private int arriveTicks = -1;

    public DeathShadowEntity(EntityType<? extends DeathShadowEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public static DeathShadowEntity spawn(ServerLevel level, Player subject, Vec3 deathPos) {
        DeathShadowEntity shadow = ModEntities.DEATH_SHADOW.get().create(level);
        if (shadow == null) {
            return null;
        }
        shadow.moveTo(subject.getX(), subject.getY() + 0.35, subject.getZ(), subject.getYRot(), 0f);
        shadow.configure(subject, deathPos);
        level.addFreshEntity(shadow);
        level.playSound(
                null,
                subject.blockPosition(),
                SoundEvents.SOUL_ESCAPE.value(),
                SoundSource.PLAYERS,
                0.7f,
                0.6f);
        return shadow;
    }

    private void configure(Player subject, Vec3 deathPos) {
        entityData.set(DATA_SKIN, Optional.of(subject.getUUID()));
        entityData.set(DATA_NAME, subject.getGameProfile().getName());
        entityData.set(DATA_TARGET_X, (float) deathPos.x);
        entityData.set(DATA_TARGET_Y, (float) deathPos.y + 0.4f);
        entityData.set(DATA_TARGET_Z, (float) deathPos.z);
        lifeTicks = MAX_LIFE;
        arriveTicks = -1;
    }

    public Optional<UUID> skinUuid() {
        return entityData.get(DATA_SKIN);
    }

    public String skinName() {
        return entityData.get(DATA_NAME);
    }

    public Vec3 targetPos() {
        return new Vec3(entityData.get(DATA_TARGET_X), entityData.get(DATA_TARGET_Y), entityData.get(DATA_TARGET_Z));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SKIN, Optional.empty());
        builder.define(DATA_NAME, "");
        builder.define(DATA_TARGET_X, 0f);
        builder.define(DATA_TARGET_Y, 0f);
        builder.define(DATA_TARGET_Z, 0f);
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(getDeltaMovement().multiply(0, 0, 0));
        if (level().isClientSide()) {
            if (tickCount % 4 == 0) {
                level().addParticle(
                        ModParticleTypes.NECRO_SHADE.get(),
                        getX() + (random.nextDouble() - 0.5) * 0.4,
                        getY() + 0.6 + random.nextDouble() * 0.4,
                        getZ() + (random.nextDouble() - 0.5) * 0.4,
                        0,
                        0.02,
                        0);
            }
            return;
        }

        if (--lifeTicks <= 0) {
            discard();
            return;
        }

        if (arriveTicks >= 0) {
            arriveTicks++;
            if (arriveTicks >= ARRIVE_LINGER) {
                if (level() instanceof ServerLevel server) {
                    server.sendParticles(
                            ModParticleTypes.NECRO_SOUL.get(),
                            getX(),
                            getY() + 0.6,
                            getZ(),
                            18,
                            0.25,
                            0.35,
                            0.25,
                            0.02);
                    server.playSound(
                            null,
                            blockPosition(),
                            SoundEvents.SCULK_SHRIEKER_SHRIEK,
                            SoundSource.PLAYERS,
                            0.35f,
                            1.6f);
                }
                discard();
            }
            return;
        }

        Vec3 target = targetPos();
        Vec3 pos = position();
        Vec3 delta = target.subtract(pos);
        double dist = delta.length();
        if (dist < 0.55) {
            setPos(target.x, target.y, target.z);
            arriveTicks = 0;
            return;
        }

        Vec3 step = delta.normalize().scale(Math.min(SPEED, dist));
        double bob = Math.sin(tickCount * 0.18) * 0.015;
        setPos(pos.x + step.x, pos.y + step.y + bob, pos.z + step.z);

        float yaw = (float) (Mth.atan2(step.z, step.x) * (180F / Math.PI)) - 90f;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;

        if (level() instanceof ServerLevel server && tickCount % 5 == 0) {
            server.sendParticles(
                    ModParticleTypes.NECRO_SHADOW.get(),
                    getX(),
                    getY() + 0.5,
                    getZ(),
                    2,
                    0.12,
                    0.15,
                    0.12,
                    0.005);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {}

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Skin")) {
            entityData.set(DATA_SKIN, Optional.of(tag.getUUID("Skin")));
        }
        entityData.set(DATA_NAME, tag.getString("Name"));
        entityData.set(DATA_TARGET_X, tag.getFloat("Tx"));
        entityData.set(DATA_TARGET_Y, tag.getFloat("Ty"));
        entityData.set(DATA_TARGET_Z, tag.getFloat("Tz"));
        lifeTicks = tag.getInt("Life");
        arriveTicks = tag.getInt("Arrive");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        skinUuid().ifPresent(id -> tag.putUUID("Skin", id));
        tag.putString("Name", skinName());
        tag.putFloat("Tx", entityData.get(DATA_TARGET_X));
        tag.putFloat("Ty", entityData.get(DATA_TARGET_Y));
        tag.putFloat("Tz", entityData.get(DATA_TARGET_Z));
        tag.putInt("Life", lifeTicks);
        tag.putInt("Arrive", arriveTicks);
    }

    @Override
    public Component getName() {
        String name = skinName();
        if (name == null || name.isEmpty()) {
            return super.getName();
        }
        return Component.translatable("entity.effecoria.death_shadow", name);
    }
}
