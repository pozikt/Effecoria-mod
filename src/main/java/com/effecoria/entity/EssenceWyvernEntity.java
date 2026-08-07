package com.effecoria.entity;

import com.effecoria.content.ModParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.AirRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

import javax.annotation.Nullable;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Φ-Wyvern — classical wyvern (no front legs). Grounded by default; flies to hunt/patrol.
 * Sitting plants wing knuckles as fore-props.
 */
public class EssenceWyvernEntity extends Monster implements GeoEntity {
    public static final byte ANIM_IDLE = 0;
    public static final byte ANIM_ATTACK = 1;
    public static final byte ANIM_BREATH = 2;
    public static final byte ANIM_HURT = 3;
    public static final byte ANIM_DEATH = 4;
    public static final byte ANIM_SIT = 5;

    private static final EntityDataAccessor<Byte> ANIM_STATE =
            SynchedEntityData.defineId(EssenceWyvernEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> FLYING =
            SynchedEntityData.defineId(EssenceWyvernEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SITTING =
            SynchedEntityData.defineId(EssenceWyvernEntity.class, EntityDataSerializers.BOOLEAN);
    /** Server-synced ground locomotion — client delta/limbSwing is unreliable for pathfinding mobs. */
    private static final EntityDataAccessor<Boolean> WALKING =
            SynchedEntityData.defineId(EssenceWyvernEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.essence_wyvern.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.essence_wyvern.walk");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.essence_wyvern.sit");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.essence_wyvern.fly");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.essence_wyvern.attack");
    private static final RawAnimation BREATH = RawAnimation.begin().thenPlay("animation.essence_wyvern.breath");
    private static final RawAnimation HURT = RawAnimation.begin().thenPlay("animation.essence_wyvern.hurt");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlay("animation.essence_wyvern.death");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private GroundPathNavigation groundNavigation;
    private FlyingPathNavigation flyingNavigation;

    private int animTicks;
    private int breathCooldown;
    private int flightTicks;
    private int groundedTicks;
    private int soarCooldown;

    public EssenceWyvernEntity(EntityType<? extends EssenceWyvernEntity> type, Level level) {
        super(type, level);
        this.xpReward = 25;
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0f);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0f);
        // Start grounded — do not hover forever.
        this.moveControl = new MoveControl(this);
        this.setNoGravity(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FLYING_SPEED, 0.55)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        GroundPathNavigation ground = new GroundPathNavigation(this, level);
        ground.setCanOpenDoors(false);
        ground.setCanFloat(true);
        ground.setCanPassDoors(true);
        this.groundNavigation = ground;

        FlyingPathNavigation flying = new FlyingPathNavigation(this, level);
        flying.setCanOpenDoors(false);
        flying.setCanFloat(true);
        flying.setCanPassDoors(true);
        this.flyingNavigation = flying;

        return ground;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANIM_STATE, ANIM_IDLE);
        builder.define(FLYING, false);
        builder.define(SITTING, false);
        builder.define(WALKING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new StandFromSitGoal());
        this.goalSelector.addGoal(2, new PlasmaBreathGoal());
        this.goalSelector.addGoal(3, new DiveMeleeGoal());
        this.goalSelector.addGoal(4, new TakeOffToHuntGoal());
        this.goalSelector.addGoal(5, new LandWhenCalmGoal());
        this.goalSelector.addGoal(6, new SoarPatrolGoal());
        this.goalSelector.addGoal(7, new SitRestGoal());
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.9) {
            @Override
            public boolean canUse() {
                return !isFlyingMode() && !isSitting() && super.canUse();
            }
        });
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 16.0f) {
            @Override
            public boolean canUse() {
                return !isSitting() && super.canUse();
            }
        });
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !isSitting() && super.canUse();
            }
        });
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public void setFlyingMode(boolean flying) {
        if (isFlyingMode() == flying) {
            return;
        }
        this.entityData.set(FLYING, flying);
        this.setNoGravity(flying);
        getNavigation().stop();
        if (flying) {
            setSitting(false);
            this.entityData.set(WALKING, false);
            this.moveControl = new FlyingMoveControl(this, 14, true);
            if (this.flyingNavigation != null) {
                this.navigation = this.flyingNavigation;
            }
            this.flightTicks = 0;
            this.soarCooldown = 80 + random.nextInt(80);
        } else {
            this.moveControl = new MoveControl(this);
            if (this.groundNavigation != null) {
                this.navigation = this.groundNavigation;
            }
            setDeltaMovement(getDeltaMovement().multiply(1.0, 0.2, 1.0));
            this.groundedTicks = 0;
            this.soarCooldown = 100 + random.nextInt(120);
        }
    }

    public boolean isFlyingMode() {
        return this.entityData.get(FLYING);
    }

    public void setSitting(boolean sitting) {
        this.entityData.set(SITTING, sitting);
        if (sitting) {
            getNavigation().stop();
            this.entityData.set(WALKING, false);
            playAnim(ANIM_SIT, -1);
        } else if (animState() == ANIM_SIT) {
            this.entityData.set(ANIM_STATE, ANIM_IDLE);
            this.animTicks = 0;
        }
    }

    public boolean isSitting() {
        return this.entityData.get(SITTING);
    }

    public boolean isWalkingAnim() {
        return this.entityData.get(WALKING);
    }

    private void playAnim(byte state, int ticks) {
        this.entityData.set(ANIM_STATE, state);
        this.animTicks = ticks;
    }

    public byte animState() {
        return this.entityData.get(ANIM_STATE);
    }

    /** True visual flight (flapping), not grounded hover. */
    public boolean isFlyingAnim() {
        return isFlyingMode() && !onGround();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }

        // Position delta this tick (xo/zo) — same trick as VitrifiedGolem.
        double dx = getX() - xo;
        double dz = getZ() - zo;
        boolean navigating = !isFlyingMode() && !isSitting() && getNavigation().isInProgress();
        boolean walking = !isFlyingMode()
                && !isSitting()
                && (navigating || (onGround() && (dx * dx + dz * dz) > 2.5E-5));
        if (this.entityData.get(WALKING) != walking) {
            this.entityData.set(WALKING, walking);
        }

        if (isFlyingMode()) {
            flightTicks++;
            groundedTicks = 0;
            // Soft ceiling: prefer altitude 6–18 above ground while cruising
            if (getTarget() == null && flightTicks > 40 && random.nextInt(40) == 0) {
                BlockPos below = blockPosition();
                int groundY = level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, below.getX(), below.getZ());
                double prefer = groundY + 10.0;
                if (getY() < prefer - 4.0) {
                    setDeltaMovement(getDeltaMovement().add(0, 0.08, 0));
                } else if (getY() > prefer + 8.0) {
                    setDeltaMovement(getDeltaMovement().add(0, -0.06, 0));
                }
            }
        } else {
            groundedTicks++;
            flightTicks = 0;
            if (soarCooldown > 0) {
                soarCooldown--;
            }
        }

        if (breathCooldown > 0) {
            breathCooldown--;
        }
        if (animTicks > 0) {
            animTicks--;
            if (animTicks == 0 && !isDeadOrDying() && !isSitting()) {
                this.entityData.set(ANIM_STATE, ANIM_IDLE);
            }
        }
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (!isControlledByLocalInstance()) {
            return;
        }
        if (isSitting()) {
            setDeltaMovement(Vec3.ZERO);
            calculateEntityAnimation(true);
            return;
        }
        if (isFlyingMode()) {
            if (isInWater()) {
                moveRelative(0.02f, travelVector);
                move(MoverType.SELF, getDeltaMovement());
                setDeltaMovement(getDeltaMovement().scale(0.8));
            } else {
                moveRelative(getSpeed() * 0.18f, travelVector);
                move(MoverType.SELF, getDeltaMovement());
                setDeltaMovement(getDeltaMovement().scale(0.91));
            }
            calculateEntityAnimation(false);
        } else {
            // Real gravity + ground locomotion (fixes perpetual hover)
            super.travel(travelVector);
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        setSitting(false);
        playAnim(ANIM_ATTACK, 20);
        playSound(SoundEvents.PHANTOM_BITE, 1.0f, 0.7f);
        return super.doHurtTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isSitting()) {
            setSitting(false);
        }
        if (!isFlyingMode() && getTarget() != null && random.nextFloat() < 0.35f) {
            setFlyingMode(true);
            setDeltaMovement(getDeltaMovement().add(0, 0.55, 0));
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !isDeadOrDying() && animState() != ANIM_BREATH) {
            playAnim(ANIM_HURT, 8);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource damageSource) {
        setSitting(false);
        playAnim(ANIM_DEATH, 36);
        super.die(damageSource);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PHANTOM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!isFlyingMode()) {
            playSound(SoundEvents.IRON_GOLEM_STEP, 0.45f, 0.55f);
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, state -> {
            if (isDeadOrDying() || animState() == ANIM_DEATH) {
                state.setAnimation(DEATH);
                return PlayState.CONTINUE;
            }
            return switch (animState()) {
                case ANIM_ATTACK -> {
                    state.setAnimation(ATTACK);
                    yield PlayState.CONTINUE;
                }
                case ANIM_BREATH -> {
                    state.setAnimation(BREATH);
                    yield PlayState.CONTINUE;
                }
                case ANIM_HURT -> {
                    state.setAnimation(HURT);
                    yield PlayState.CONTINUE;
                }
                case ANIM_SIT -> {
                    state.setAnimation(SIT);
                    yield PlayState.CONTINUE;
                }
                default -> {
                    if (isSitting()) {
                        state.setAnimation(SIT);
                    } else if (isFlyingAnim()) {
                        state.setAnimation(FLY);
                    } else if (isWalkingAnim() || state.getLimbSwingAmount() > 0.02f || state.isMoving()) {
                        // Prefer synced WALKING; limbSwing/isMoving are flaky client fallbacks.
                        state.setAnimation(WALK);
                    } else {
                        state.setAnimation(IDLE);
                    }
                    yield PlayState.CONTINUE;
                }
            };
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // --- Goals ---

    private final class StandFromSitGoal extends Goal {
        StandFromSitGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return isSitting() && (getTarget() != null || hurtTime > 0);
        }

        @Override
        public void start() {
            setSitting(false);
        }
    }

    private final class SitRestGoal extends Goal {
        private int sitTicks;

        SitRestGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (isFlyingMode() || isSitting() || getTarget() != null || !onGround()) {
                return false;
            }
            if (groundedTicks < 80) {
                return false;
            }
            return random.nextInt(240) == 0;
        }

        @Override
        public void start() {
            sitTicks = 100 + random.nextInt(160);
            setSitting(true);
            playSound(SoundEvents.PHANTOM_AMBIENT, 0.6f, 0.5f);
        }

        @Override
        public boolean canContinueToUse() {
            return isSitting() && sitTicks > 0 && getTarget() == null;
        }

        @Override
        public void tick() {
            sitTicks--;
            setDeltaMovement(Vec3.ZERO);
        }

        @Override
        public void stop() {
            setSitting(false);
        }
    }

    private final class TakeOffToHuntGoal extends Goal {
        TakeOffToHuntGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (isFlyingMode() || isSitting()) {
                return false;
            }
            LivingEntity target = getTarget();
            if (target == null) {
                return false;
            }
            double d = distanceToSqr(target);
            // Close → stay on foot; mid/far → take wing
            return d > 36.0 && (d > 100.0 || random.nextInt(20) == 0);
        }

        @Override
        public void start() {
            setFlyingMode(true);
            setDeltaMovement(getDeltaMovement().add(0, 0.65, 0));
            playSound(SoundEvents.PHANTOM_FLAP, 1.0f, 0.7f);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }

    private final class LandWhenCalmGoal extends Goal {
        private int landWarmup;

        LandWhenCalmGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!isFlyingMode() || getTarget() != null) {
                return false;
            }
            // After a while of idle flight, or when very low
            return flightTicks > 160 + random.nextInt(80) || (onGround() && flightTicks > 20);
        }

        @Override
        public void start() {
            landWarmup = 40;
            getNavigation().stop();
        }

        @Override
        public boolean canContinueToUse() {
            return isFlyingMode() && getTarget() == null && landWarmup > 0;
        }

        @Override
        public void tick() {
            landWarmup--;
            setDeltaMovement(getDeltaMovement().multiply(0.9, 1.0, 0.9).add(0, -0.12, 0));
            if (onGround() || landWarmup <= 0) {
                setFlyingMode(false);
            }
        }
    }

    private final class SoarPatrolGoal extends Goal {
        private Vec3 want;
        private int ticks;

        SoarPatrolGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (isSitting()) {
                return false;
            }
            // Occasional leisure flight from ground, or keep moving while already aloft
            if (!isFlyingMode()) {
                if (getTarget() != null || soarCooldown > 0 || groundedTicks < 60) {
                    return false;
                }
                return random.nextInt(300) == 0;
            }
            return getTarget() == null && getNavigation().isDone() && random.nextInt(30) == 0;
        }

        @Override
        public void start() {
            if (!isFlyingMode()) {
                setFlyingMode(true);
                setDeltaMovement(getDeltaMovement().add(0, 0.55, 0));
            }
            want = findAirPos();
            ticks = 80 + random.nextInt(60);
            if (want != null) {
                getNavigation().moveTo(want.x, want.y, want.z, 1.1);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return isFlyingMode() && getTarget() == null && ticks > 0 && want != null;
        }

        @Override
        public void tick() {
            ticks--;
            if (want != null && distanceToSqr(want) < 6.0) {
                want = findAirPos();
                if (want != null) {
                    getNavigation().moveTo(want.x, want.y, want.z, 1.1);
                }
            }
        }

        @Nullable
        private Vec3 findAirPos() {
            Vec3 view = getViewVector(0.0f);
            Vec3 hover = HoverRandomPos.getPos(EssenceWyvernEntity.this, 12, 6, view.x, view.z, Mth.PI / 2f, 4, 2);
            if (hover != null) {
                return hover;
            }
            return AirRandomPos.getPosTowards(
                    EssenceWyvernEntity.this, 14, 6, -2, position().add(view.scale(8)), Mth.HALF_PI);
        }
    }

    private final class DiveMeleeGoal extends MeleeAttackGoal {
        DiveMeleeGoal() {
            super(EssenceWyvernEntity.this, 1.2, true);
        }

        @Override
        public boolean canUse() {
            return !isSitting() && super.canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target != null && isFlyingMode()) {
                // Dive onto prey
                double dy = target.getY() - getY();
                if (dy < -1.5 && distanceToSqr(target) < 120.0) {
                    setDeltaMovement(getDeltaMovement().add(0, -0.08, 0));
                }
                if (distanceToSqr(target) < 12.0 && onGround()) {
                    setFlyingMode(false);
                }
            }
            super.tick();
        }
    }

    private final class PlasmaBreathGoal extends Goal {
        private int windup;

        PlasmaBreathGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (target == null || breathCooldown > 0 || isSitting()) {
                return false;
            }
            double d = distanceToSqr(target);
            boolean rangeOk = d > 20.0 && d < 280.0;
            // Prefer breath while airborne or when target is elevated
            return rangeOk && (isFlyingMode() || d > 64.0) && random.nextInt(36) == 0;
        }

        @Override
        public void start() {
            windup = 24;
            setSitting(false);
            if (!isFlyingMode() && random.nextBoolean()) {
                setFlyingMode(true);
                setDeltaMovement(getDeltaMovement().add(0, 0.4, 0));
            }
            playAnim(ANIM_BREATH, 32);
            getNavigation().stop();
            playSound(SoundEvents.BLAZE_AMBIENT, 1.0f, 0.55f);
        }

        @Override
        public boolean canContinueToUse() {
            return windup > 0 && getTarget() != null;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 40.0f, 40.0f);
            windup--;
            if (windup == 8) {
                firePlasma(target);
            }
        }

        @Override
        public void stop() {
            breathCooldown = 100 + random.nextInt(80);
            if (animState() == ANIM_BREATH) {
                entityData.set(ANIM_STATE, ANIM_IDLE);
            }
        }

        private void firePlasma(LivingEntity target) {
            playSound(SoundEvents.BLAZE_SHOOT, 1.2f, 0.75f);
            Vec3 from = getEyePosition().add(getLookAngle().scale(1.2));
            Vec3 to = target.getEyePosition();
            Vec3 dir = to.subtract(from).normalize();
            if (level() instanceof ServerLevel server) {
                for (int i = 0; i < 18; i++) {
                    Vec3 p = from.add(dir.scale(i * 0.55));
                    server.sendParticles(
                            ModParticleTypes.ELEMENTAL_PLASMA.get(),
                            p.x,
                            p.y,
                            p.z,
                            2,
                            0.08,
                            0.08,
                            0.08,
                            0.01);
                    if (i % 3 == 0) {
                        server.sendParticles(
                                ModParticleTypes.PHI_SPARK.get(), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.02);
                    }
                }
            }
            for (LivingEntity living : level().getEntitiesOfClass(
                    LivingEntity.class, getBoundingBox().inflate(12.0), e -> e != EssenceWyvernEntity.this)) {
                Vec3 toLiving = living.getEyePosition().subtract(from);
                double dist = toLiving.length();
                if (dist > 14.0 || dist < 0.2) {
                    continue;
                }
                if (dir.dot(toLiving.normalize()) < 0.88) {
                    continue;
                }
                float dmg = 8.0f + (float) (1.0 - dist / 14.0) * 6.0f;
                living.hurt(damageSources().mobAttack(EssenceWyvernEntity.this), dmg);
                living.igniteForSeconds(4);
            }
        }
    }
}
