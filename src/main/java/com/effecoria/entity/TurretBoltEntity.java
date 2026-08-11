package com.effecoria.entity;

import com.effecoria.block.PhiTurretBlock;
import com.effecoria.block.TurretMountBlock;
import com.effecoria.content.ModEntities;
import com.effecoria.core.disease.DiseaseService;
import com.effecoria.core.disease.PhiDisease;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Fast kinetic / Ω turret bolt — hypervelocity, piercing, heavy knockback. */
public final class TurretBoltEntity extends Projectile {
    /** Vanilla arrows are ~3; Φ bolts are hypervelocity. */
    public static final float SPEED = 5.75f;
    public static final int MAX_PIERCE = 5;
    public static final double KNOCKBACK = 2.35;

    private static final EntityDataAccessor<Boolean> DATA_OMEGA =
            SynchedEntityData.defineId(TurretBoltEntity.class, EntityDataSerializers.BOOLEAN);

    private float damage = 10f;
    private int life;
    private double maxRange = 32.0;
    private Vec3 origin = Vec3.ZERO;
    private int pierceLeft = MAX_PIERCE;
    private final Set<UUID> pierced = new HashSet<>();

    public TurretBoltEntity(EntityType<? extends TurretBoltEntity> type, Level level) {
        super(type, level);
        this.life = 20;
    }

    public TurretBoltEntity(Level level, double x, double y, double z, Vec3 dir, float damage, double maxRange) {
        this(ModEntities.TURRET_BOLT.get(), level);
        setPos(x, y, z);
        this.origin = new Vec3(x, y, z);
        this.damage = damage;
        this.maxRange = Math.max(4.0, maxRange);
        Vec3 velocity = dir.normalize().scale(SPEED);
        setDeltaMovement(velocity);
        setYRot((float) (Mth.atan2(velocity.x, velocity.z) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(velocity.y, velocity.horizontalDistance()) * Mth.RAD_TO_DEG));
        yRotO = getYRot();
        xRotO = getXRot();
        this.life = Mth.ceil(this.maxRange / SPEED) + 4;
        this.pierceLeft = MAX_PIERCE;
    }

    public void setOmega(boolean value) {
        entityData.set(DATA_OMEGA, value);
    }

    public boolean isOmega() {
        return entityData.get(DATA_OMEGA);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OMEGA, false);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (!super.canHitEntity(target)) {
            return false;
        }
        return !pierced.contains(target.getUUID());
    }

    @Override
    public void tick() {
        yRotO = getYRot();
        xRotO = getXRot();
        super.tick();

        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-7) {
            setYRot((float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG));
            setXRot((float) (Mth.atan2(motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG));
        }

        if (!level().isClientSide()) {
            life--;
            if (life <= 0 || position().distanceToSqr(origin) > maxRange * maxRange) {
                discard();
                return;
            }
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                onHit(hit);
                if (isRemoved()) {
                    return;
                }
            }
        } else {
            boolean omega = isOmega();
            level().addParticle(
                    omega ? ParticleTypes.SCULK_SOUL : ParticleTypes.CRIT,
                    getX(),
                    getY(),
                    getZ(),
                    motion.x * 0.2,
                    motion.y * 0.2,
                    motion.z * 0.2);
            if (tickCount % 2 == 0) {
                level().addParticle(
                        omega ? ParticleTypes.PORTAL : ParticleTypes.END_ROD,
                        getX() - motion.x * 0.4,
                        getY() - motion.y * 0.4,
                        getZ() - motion.z * 0.4,
                        0,
                        0,
                        0);
            }
        }

        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();
        pierced.add(entity.getUUID());
        if (entity instanceof LivingEntity living && level() instanceof ServerLevel server) {
            living.hurt(server.damageSources().magic(), damage);
            applyKnockback(living);
            if (isOmega() && living instanceof ServerPlayer player) {
                DiseaseService.infect(player, PhiDisease.OMEGA_SICKNESS, 1);
            }
            server.sendParticles(
                    isOmega() ? ParticleTypes.SCULK_SOUL : ParticleTypes.CRIT,
                    living.getX(),
                    living.getY() + living.getBbHeight() * 0.5,
                    living.getZ(),
                    10,
                    0.25,
                    0.35,
                    0.25,
                    0.08);
        }
        pierceLeft--;
        // Soften slightly after each pierce, keep going through flesh.
        damage = Math.max(4f, damage * 0.85f);
        if (pierceLeft <= 0) {
            discard();
        }
        // Do not call discard for flesh hits while pierce remains — bolt continues.
    }

    private void applyKnockback(LivingEntity living) {
        Vec3 dir = getDeltaMovement();
        if (dir.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 n = dir.normalize();
        // Strong horizontal throw + lift — “gauss punch”.
        living.push(n.x * KNOCKBACK, 0.55 + Math.abs(n.y) * 0.35, n.z * KNOCKBACK);
        living.hurtMarked = true;
        living.fallDistance = 0f;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        BlockState state = level().getBlockState(result.getBlockPos());
        if (state.getBlock() instanceof PhiTurretBlock || state.getBlock() instanceof TurretMountBlock) {
            return;
        }
        discard();
    }

    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.BLOCK && result instanceof BlockHitResult blockHit) {
            BlockState state = level().getBlockState(blockHit.getBlockPos());
            if (state.getBlock() instanceof PhiTurretBlock || state.getBlock() instanceof TurretMountBlock) {
                return;
            }
            onHitBlock(blockHit);
            return;
        }
        if (result.getType() == HitResult.Type.ENTITY && result instanceof EntityHitResult entityHit) {
            onHitEntity(entityHit);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Damage", damage);
        tag.putBoolean("Omega", isOmega());
        tag.putInt("Life", life);
        tag.putDouble("MaxRange", maxRange);
        tag.putInt("Pierce", pierceLeft);
        tag.putDouble("Ox", origin.x);
        tag.putDouble("Oy", origin.y);
        tag.putDouble("Oz", origin.z);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        damage = tag.getFloat("Damage");
        setOmega(tag.getBoolean("Omega"));
        life = tag.getInt("Life");
        maxRange = tag.contains("MaxRange") ? tag.getDouble("MaxRange") : 32.0;
        pierceLeft = tag.contains("Pierce") ? tag.getInt("Pierce") : MAX_PIERCE;
        if (tag.contains("Ox")) {
            origin = new Vec3(tag.getDouble("Ox"), tag.getDouble("Oy"), tag.getDouble("Oz"));
        } else {
            origin = position();
        }
    }
}
