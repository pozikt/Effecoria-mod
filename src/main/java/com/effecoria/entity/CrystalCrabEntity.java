package com.effecoria.entity;

import com.effecoria.content.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Crystal crab — neutral golem-crab; angered when nearby essonite crystals are mined. */
public class CrystalCrabEntity extends Monster implements GeoEntity, NeutralMob {
    private static final EntityDataAccessor<Boolean> WALKING =
            SynchedEntityData.defineId(CrystalCrabEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ATTACKING =
            SynchedEntityData.defineId(CrystalCrabEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.crystal_crab.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.crystal_crab.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.crystal_crab.attack");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int remainingPersistentAngerTime;
    private UUID persistentAngerTarget;
    private int attackAnimTicks;

    public CrystalCrabEntity(EntityType<? extends CrystalCrabEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 28.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.FOLLOW_RANGE, 18.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WALKING, false);
        builder.define(ATTACKING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.85));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(3, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    /** Called when a player breaks an essonite crystal near this crab. */
    public void onCrystalDisturbed(Player player, BlockPos pos) {
        if (level().isClientSide || player == null || player.isCreative()) {
            return;
        }
        if (distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 12.0 * 12.0) {
            return;
        }
        setPersistentAngerTarget(player.getUUID());
        startPersistentAngerTimer();
        setTarget(player);
    }

    public static boolean isProtectedCrystal(BlockState state) {
        return state.is(ModBlocks.ESSONITE_CRYSTAL.get())
                || state.is(ModBlocks.ESSONITE_CRYSTAL_BUD_SMALL.get())
                || state.is(ModBlocks.ESSONITE_CRYSTAL_BUD_MEDIUM.get())
                || state.is(ModBlocks.ESSONITE_CRYSTAL_BUD_LARGE.get())
                || state.is(ModBlocks.ESSONITE_POINTED.get())
                || state.is(ModBlocks.ESSONITE_BLOCK.get());
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        attackAnimTicks = 12;
        this.entityData.set(ATTACKING, true);
        return super.doHurtTarget(target);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        updatePersistentAnger((net.minecraft.server.level.ServerLevel) level(), true);

        double dx = getX() - xo;
        double dz = getZ() - zo;
        boolean navigating = getNavigation().isInProgress();
        boolean walking = navigating || (onGround() && (dx * dx + dz * dz) > 2.5E-5);
        if (this.entityData.get(WALKING) != walking) {
            this.entityData.set(WALKING, walking);
        }

        if (attackAnimTicks > 0) {
            attackAnimTicks--;
            if (attackAnimTicks == 0) {
                this.entityData.set(ATTACKING, false);
            }
        }
    }

    public boolean isWalkingAnim() {
        return this.entityData.get(WALKING);
    }

    public boolean isAttackingAnim() {
        return this.entityData.get(ATTACKING);
    }

    @Override
    public int getRemainingPersistentAngerTime() {
        return remainingPersistentAngerTime;
    }

    @Override
    public void setRemainingPersistentAngerTime(int time) {
        remainingPersistentAngerTime = time;
    }

    @Override
    public UUID getPersistentAngerTarget() {
        return persistentAngerTarget;
    }

    @Override
    public void setPersistentAngerTarget(UUID target) {
        persistentAngerTarget = target;
    }

    @Override
    public void startPersistentAngerTimer() {
        setRemainingPersistentAngerTime(20 * (20 + random.nextInt(20)));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.TURTLE_AMBIENT_LAND;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.TURTLE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TURTLE_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            if (isAttackingAnim()) {
                state.setAnimation(ATTACK);
            } else if (isWalkingAnim() || state.getLimbSwingAmount() > 0.02f || state.isMoving()) {
                state.setAnimation(WALK);
            } else {
                state.setAnimation(IDLE);
            }
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
