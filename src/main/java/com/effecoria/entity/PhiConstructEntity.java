package com.effecoria.entity;

import java.util.Optional;
import java.util.UUID;

import com.effecoria.content.ModEntities;
import com.effecoria.content.ModItems;
import com.effecoria.core.technomagic.ConstructBindingService;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Friendly Φ-construct — reuses vitrified golem geo; follow/sit/defend via imprint binding.
 * Extends {@link TamableAnimal} for vanilla owner goals while binding uses ConstructBindingService ledger.
 */
public class PhiConstructEntity extends TamableAnimal implements GeoEntity {
    private static final EntityDataAccessor<Boolean> WALKING =
            SynchedEntityData.defineId(PhiConstructEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> POWERED =
            SynchedEntityData.defineId(PhiConstructEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.vitrified_golem.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.vitrified_golem.walk");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int chargeTick;

    public PhiConstructEntity(EntityType<? extends PhiConstructEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 36.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WALKING, false);
        builder.define(POWERED, true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.FollowOwnerGoal(this, 1.15, 6.0f, 2.0f));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    public void setPowered(boolean powered) {
        this.entityData.set(POWERED, powered);
    }

    public boolean isPowered() {
        return this.entityData.get(POWERED);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            return;
        }
        double dx = getX() - xo;
        double dz = getZ() - zo;
        boolean walking = onGround() && (dx * dx + dz * dz) > 1.0E-5;
        if (this.entityData.get(WALKING) != walking) {
            this.entityData.set(WALKING, walking);
        }
        if (++chargeTick >= 20) {
            chargeTick = 0;
            if (getOwner() instanceof ServerPlayer owner) {
                setPowered(ConstructBindingService.consumeChargeTick(owner));
            }
        }
        if (!isPowered() || isOrderedToSit()) {
            setTarget(null);
            getNavigation().stop();
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (!isOwnedBy(player)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            setOrderedToSit(!isOrderedToSit());
            getNavigation().stop();
            setTarget(null);
            player.displayClientMessage(
                    Component.translatable(
                            isOrderedToSit()
                                    ? "message.effecoria.construct_sit"
                                    : "message.effecoria.construct_follow"),
                    true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        if (!isPowered() || isOrderedToSit()) {
            return false;
        }
        return super.doHurtTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!isPowered() && source.getEntity() instanceof LivingEntity) {
            // Still take damage when unpowered
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public PhiConstructEntity getBreedOffspring(ServerLevel level, AgeableMob other) {
        return ModEntities.PHI_CONSTRUCT.get().create(level);
    }

    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.IRON_GOLEM_STEP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Powered", isPowered());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setPowered(!tag.contains("Powered") || tag.getBoolean("Powered"));
        Optional.ofNullable(getOwnerUUID()).ifPresent(id -> getPersistentData().putUUID(ConstructBindingService.OWNER_TAG, id));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (this.entityData.get(WALKING) && isPowered() && !isOrderedToSit()) {
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

    /** Drop imprintable chassis scrap on death. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        ItemStack chassis = new ItemStack(ModItems.GOLEM_CHASSIS.get());
        // blank again — imprint burned out
        spawnAtLocation(chassis);
    }
}
