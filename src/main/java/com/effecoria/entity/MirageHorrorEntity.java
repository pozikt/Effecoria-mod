package com.effecoria.entity;

import java.util.Optional;
import java.util.UUID;

import com.effecoria.content.ModEntities;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.effect.mental.MirageWorldService;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Horizontal serpentine mirage horror — long floor-bound tail, 2–3 rising front segments.
 * Limbs latch onto mirage ground/walls and haul the body; never flies.
 */
public class MirageHorrorEntity extends Mob implements GeoEntity {
    public static final int POSE_IDLE = 0;
    public static final int POSE_CRAWL = 1;
    public static final int POSE_REACH = 2;
    public static final int POSE_PULL = 3;
    public static final int POSE_LUNGE = 4;

    private static final EntityDataAccessor<Optional<UUID>> DATA_VICTIM =
            SynchedEntityData.defineId(MirageHorrorEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_POSE =
            SynchedEntityData.defineId(MirageHorrorEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("animation.mirage_horror.idle");
    private static final RawAnimation CRAWL =
            RawAnimation.begin().thenLoop("animation.mirage_horror.crawl");
    private static final RawAnimation REACH =
            RawAnimation.begin().thenPlay("animation.mirage_horror.reach");
    private static final RawAnimation PULL =
            RawAnimation.begin().thenPlay("animation.mirage_horror.pull");
    private static final RawAnimation LUNGE =
            RawAnimation.begin().thenPlay("animation.mirage_horror.lunge");

    private static final int REACH_TICKS = 12;
    private static final int PULL_TICKS = 12;
    /** Long limbs latch far ahead on the floor. */
    private static final double GRAB_REACH = 10.0;
    /**
     * Large body covers more ground per stride. Sustained chase a bit above walk
     * (~0.22) and near sprint (~0.28) — run to stay ahead.
     */
    private static final double CRAWL_NEAR = 0.27;
    private static final double CRAWL_FAR = 0.35;
    private static final double REACH_CREEP = 0.20;
    private static final double PULL_YANK = 0.90;
    private static final double PULL_CHASE = 0.40;
    private static final double LUNGE_STEP = 0.45;
    private static final double STRIKE_RANGE = 5.8;
    private static final int STUCK_CHECK_TICKS = 35;
    private static final double STUCK_MOVE_EPS = 0.45;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int lungeCooldown;
    private int lifeTicks = 20 * 40;
    private int gaitPhase;
    private int gaitTicks;
    private Vec3 grabTarget = Vec3.ZERO;
    private Vec3 pullVelocity = Vec3.ZERO;
    private Vec3 stuckAnchor = Vec3.ZERO;
    private int stuckTicks;

    public MirageHorrorEntity(EntityType<? extends MirageHorrorEntity> type, Level level) {
        super(type, level);
        // Mirage terrain is client-fake; real world has no floor. Stay grounded via snapToGround.
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setSilent(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.33)
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
        double standY = MirageWorldService.findStandY(victim, x, z).orElse(y);
        horror.moveTo(x, standY, z, victim.getYRot() + 180f, 0f);
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

    public int getPoseAnim() {
        return entityData.get(DATA_POSE);
    }

    private void setPoseAnim(int pose) {
        if (getPoseAnim() != pose) {
            entityData.set(DATA_POSE, pose);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VICTIM, Optional.empty());
        builder.define(DATA_POSE, POSE_IDLE);
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

        Vec3 toVictim = flat(victim.position().subtract(position()));
        double dist = toVictim.length();
        faceToward(toVictim);
        unstickIfNeeded(victim, toVictim, dist);

        if (lungeCooldown > 0) {
            lungeCooldown--;
            if (lungeCooldown == 12 && getPoseAnim() == POSE_LUNGE) {
                setPoseAnim(POSE_IDLE);
            }
        } else if (dist < STRIKE_RANGE && gaitPhase == 0) {
            performFearStrike(victim);
            lungeCooldown = 55;
            setPoseAnim(POSE_LUNGE);
            crawlBy(victim, toVictim.normalize().scale(LUNGE_STEP));
            snapToGround(victim);
            return;
        }

        tickGrabLocomotion(serverLevel, victim, toVictim, dist);
        snapToGround(victim);

        if (tickCount % 8 == 0) {
            serverLevel.sendParticles(
                    ModParticleTypes.MENTAL_FEAR.get(),
                    getX(),
                    getY() + 2.8,
                    getZ(),
                    5,
                    0.8,
                    0.9,
                    1.4,
                    0.01);
        }
    }

    private void tickGrabLocomotion(ServerLevel level, ServerPlayer victim, Vec3 toVictim, double dist) {
        if (gaitPhase == 1) {
            gaitTicks--;
            setPoseAnim(POSE_REACH);
            // Keep sliding toward the latch / victim so the wind-up does not freeze the chase.
            Vec3 reachDir = flat(grabTarget.subtract(position()));
            if (reachDir.lengthSqr() < 0.4) {
                reachDir = toVictim;
            }
            if (toVictim.lengthSqr() > 1.0e-4) {
                reachDir = reachDir.normalize().scale(0.65).add(toVictim.normalize().scale(0.35));
            }
            if (reachDir.lengthSqr() > 1.0e-6) {
                crawlBy(victim, reachDir.normalize().scale(REACH_CREEP));
            }
            if (gaitTicks <= 0) {
                gaitPhase = 2;
                gaitTicks = PULL_TICKS;
                Vec3 pullDir = flat(grabTarget.subtract(position()));
                Vec3 chase = toVictim.lengthSqr() > 1.0e-4 ? toVictim.normalize() : pullDir;
                if (pullDir.lengthSqr() < 1.0
                        || (pullDir.lengthSqr() > 1.0e-6 && pullDir.normalize().dot(chase) < 0.1)) {
                    pullDir = toVictim;
                }
                Vec3 yank = pullDir.normalize().scale(PULL_YANK);
                if (toVictim.lengthSqr() > 1.0e-4) {
                    yank = yank.add(toVictim.normalize().scale(PULL_CHASE));
                }
                pullVelocity = yank;
                spawnGrabBurst(level, grabTarget);
                level.playSound(
                        null,
                        blockPosition(),
                        SoundEvents.SPIDER_STEP,
                        SoundSource.HOSTILE,
                        0.7f,
                        0.45f + random.nextFloat() * 0.2f);
            }
            return;
        }

        if (gaitPhase == 2) {
            gaitTicks--;
            setPoseAnim(POSE_PULL);
            crawlBy(victim, pullVelocity);
            pullVelocity = pullVelocity.scale(0.86);
            if (gaitTicks <= 0) {
                gaitPhase = 0;
                gaitTicks = 2 + random.nextInt(3);
                setPoseAnim(dist > 2.5 ? POSE_CRAWL : POSE_IDLE);
                setDeltaMovement(Vec3.ZERO);
            }
            return;
        }

        if (gaitTicks > 0) {
            gaitTicks--;
            setPoseAnim(dist > 3.0 ? POSE_CRAWL : POSE_IDLE);
            if (dist > 0.4) {
                crawlBy(victim, toVictim.normalize().scale(dist > 12 ? CRAWL_FAR : CRAWL_NEAR));
            } else {
                setDeltaMovement(Vec3.ZERO);
            }
            return;
        }

        Optional<Vec3> grab =
                MirageWorldService.findGrabSurface(victim, position().add(0, 1.4, 0), toVictim, GRAB_REACH);
        if (grab.isEmpty() && dist > 1.0) {
            Vec3 ahead = position().add(toVictim.normalize().scale(5.5));
            double gy = MirageWorldService.findStandY(victim, ahead.x, ahead.z).orElse(getY());
            grab = Optional.of(new Vec3(ahead.x, gy + 0.05, ahead.z));
        }
        if (grab.isPresent()) {
            grabTarget = grab.get();
            gaitPhase = 1;
            gaitTicks = REACH_TICKS;
            setPoseAnim(POSE_REACH);
            level.playSound(
                    null,
                    blockPosition(),
                    SoundEvents.SLIME_SQUISH_SMALL,
                    SoundSource.HOSTILE,
                    0.45f,
                    0.55f);
        } else {
            setPoseAnim(dist > 3.0 ? POSE_CRAWL : POSE_IDLE);
            if (dist > 0.4 && toVictim.lengthSqr() > 1.0e-4) {
                crawlBy(victim, toVictim.normalize().scale(CRAWL_NEAR));
            } else {
                setDeltaMovement(Vec3.ZERO);
            }
            gaitTicks = 4;
        }
    }

    private void unstickIfNeeded(ServerPlayer victim, Vec3 toVictim, double dist) {
        if (stuckAnchor == Vec3.ZERO) {
            stuckAnchor = position();
            stuckTicks = 0;
            return;
        }
        if (position().distanceToSqr(stuckAnchor) > STUCK_MOVE_EPS * STUCK_MOVE_EPS) {
            stuckAnchor = position();
            stuckTicks = 0;
            return;
        }
        stuckTicks++;
        if (stuckTicks < STUCK_CHECK_TICKS) {
            return;
        }
        // Hard reset: drop arches, abort gait, shove toward the soul.
        stuckTicks = 0;
        stuckAnchor = position();
        gaitPhase = 0;
        gaitTicks = 6;
        pullVelocity = Vec3.ZERO;
        setPoseAnim(dist > 2.5 ? POSE_CRAWL : POSE_IDLE);
        if (toVictim.lengthSqr() > 1.0e-4) {
            crawlBy(victim, toVictim.normalize().scale(CRAWL_FAR));
        }
        MirageWorldService.findStandY(victim, getX(), getZ()).ifPresent(y -> setPos(getX(), y, getZ()));
    }

    /** Horizontal step only — Y is forced to mirage floor afterward. */
    private void crawlBy(ServerPlayer victim, Vec3 delta) {
        Vec3 flat = flat(delta);
        if (flat.lengthSqr() < 1.0e-8) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        setDeltaMovement(flat);
    }

    private void snapToGround(ServerPlayer victim) {
        MirageWorldService.findStandY(victim, getX(), getZ()).ifPresent(y -> {
            if (Math.abs(getY() - y) > 0.02) {
                setPos(getX(), y, getZ());
            }
        });
        // Kill any residual vertical velocity so the entity never drifts upward.
        Vec3 motion = getDeltaMovement();
        if (Math.abs(motion.y) > 1.0e-4) {
            setDeltaMovement(motion.x, 0, motion.z);
        }
        setXRot(0f);
    }

    private static Vec3 flat(Vec3 v) {
        return new Vec3(v.x, 0, v.z);
    }

    private void spawnGrabBurst(ServerLevel level, Vec3 at) {
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState()),
                at.x,
                at.y,
                at.z,
                10,
                0.25,
                0.15,
                0.25,
                0.05);
        level.sendParticles(
                ModParticleTypes.MENTAL_FEAR.get(),
                at.x,
                at.y,
                at.z,
                4,
                0.2,
                0.15,
                0.2,
                0.01);
    }

    private void faceToward(Vec3 to) {
        if (to.lengthSqr() < 1.0e-4) {
            return;
        }
        Vec3 dir = to.normalize();
        float yaw = (float) (Mth.atan2(dir.z, dir.x) * (180f / Math.PI)) - 90f;
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
        setXRot(0f);
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
        tag.putInt("Pose", getPoseAnim());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Victim")) {
            setVictim(tag.getUUID("Victim"));
        }
        lifeTicks = tag.getInt("Life");
        setPoseAnim(tag.getInt("Pose"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 2, state -> {
            return switch (getPoseAnim()) {
                case POSE_LUNGE -> {
                    state.setAnimation(LUNGE);
                    yield PlayState.CONTINUE;
                }
                case POSE_REACH -> {
                    state.setAnimation(REACH);
                    yield PlayState.CONTINUE;
                }
                case POSE_PULL -> {
                    state.setAnimation(PULL);
                    yield PlayState.CONTINUE;
                }
                case POSE_CRAWL -> {
                    state.setAnimation(CRAWL);
                    yield PlayState.CONTINUE;
                }
                default -> {
                    state.setAnimation(IDLE);
                    yield PlayState.CONTINUE;
                }
            };
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
