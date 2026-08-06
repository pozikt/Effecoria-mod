package com.effecoria.entity;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Hostile glass guardian of the Vitrified Wastes — claw melee, rush, and Φ-flash special.
 */
public class VitrifiedGolemEntity extends Monster implements GeoEntity {
    public static final byte ANIM_IDLE = 0;
    public static final byte ANIM_DETECT = 1;
    public static final byte ANIM_ATTACK_1 = 2;
    public static final byte ANIM_ATTACK_2 = 3;
    public static final byte ANIM_RUSH = 4;
    public static final byte ANIM_SPECIAL = 5;
    public static final byte ANIM_HURT = 6;
    public static final byte ANIM_DEATH = 7;

    private static final EntityDataAccessor<Byte> ANIM_STATE =
            SynchedEntityData.defineId(VitrifiedGolemEntity.class, EntityDataSerializers.BYTE);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.vitrified_golem.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.vitrified_golem.walk");
    private static final RawAnimation DETECT = RawAnimation.begin().thenPlay("animation.vitrified_golem.detect");
    private static final RawAnimation ATTACK_1 = RawAnimation.begin().thenPlay("animation.vitrified_golem.attack_1");
    private static final RawAnimation ATTACK_2 = RawAnimation.begin().thenPlay("animation.vitrified_golem.attack_2");
    private static final RawAnimation RUSH = RawAnimation.begin().thenPlay("animation.vitrified_golem.rush");
    private static final RawAnimation SPECIAL = RawAnimation.begin().thenPlay("animation.vitrified_golem.special");
    private static final RawAnimation HURT = RawAnimation.begin().thenPlay("animation.vitrified_golem.hurt");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlay("animation.vitrified_golem.death");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int animTicks;
    private int rushCooldown;
    private int specialCooldown;
    private int nextAttackHand;
    private boolean detectedTarget;

    public VitrifiedGolemEntity(EntityType<? extends VitrifiedGolemEntity> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.55);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANIM_STATE, ANIM_IDLE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SpecialFlashGoal());
        this.goalSelector.addGoal(2, new RushGoal());
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.05, false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 10.0f));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public byte animState() {
        return this.entityData.get(ANIM_STATE);
    }

    private void playAnim(byte state, int ticks) {
        this.entityData.set(ANIM_STATE, state);
        this.animTicks = ticks;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        if (rushCooldown > 0) {
            rushCooldown--;
        }
        if (specialCooldown > 0) {
            specialCooldown--;
        }
        if (animTicks > 0) {
            animTicks--;
            if (animTicks == 0 && !isDeadOrDying()) {
                this.entityData.set(ANIM_STATE, ANIM_IDLE);
            }
        }

        LivingEntity target = getTarget();
        if (target != null && !detectedTarget) {
            detectedTarget = true;
            playAnim(ANIM_DETECT, 20);
            playSound(SoundEvents.GLASS_BREAK, 0.7f, 0.55f);
            playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.9f, 0.7f);
        } else if (target == null) {
            detectedTarget = false;
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean left = nextAttackHand++ % 2 == 1;
        playAnim(left ? ANIM_ATTACK_2 : ANIM_ATTACK_1, 30);
        playSound(SoundEvents.GLASS_BREAK, 1.0f, 0.85f + random.nextFloat() * 0.2f);
        boolean hit = super.doHurtTarget(target);
        if (hit && level() instanceof ServerLevel server) {
            server.sendParticles(
                    ModParticleTypes.PHI_SPARK.get(),
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.5,
                    target.getZ(),
                    8,
                    0.2,
                    0.3,
                    0.2,
                    0.02);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !isDeadOrDying() && animState() != ANIM_SPECIAL && animState() != ANIM_RUSH) {
            playAnim(ANIM_HURT, 10);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(
                        ModParticleTypes.PHI_SPARK.get(),
                        getX(),
                        getY() + 1.4,
                        getZ(),
                        12,
                        0.35,
                        0.5,
                        0.35,
                        0.03);
            }
        }
        return hurt;
    }

    @Override
    public void die(DamageSource damageSource) {
        playAnim(ANIM_DEATH, 40);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(
                    ModParticleTypes.PHI_SPARK.get(),
                    getX(),
                    getY() + 1.2,
                    getZ(),
                    40,
                    0.5,
                    1.0,
                    0.5,
                    0.05);
        }
        super.die(damageSource);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GLASS_PLACE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GLASS_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GLASS_BREAK;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.GLASS_STEP, 0.25f, 0.9f + random.nextFloat() * 0.2f);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 2, state -> {
            if (isDeadOrDying() || animState() == ANIM_DEATH) {
                state.setAnimation(DEATH);
                return PlayState.CONTINUE;
            }
            return switch (animState()) {
                case ANIM_DETECT -> {
                    state.setAnimation(DETECT);
                    yield PlayState.CONTINUE;
                }
                case ANIM_ATTACK_1 -> {
                    state.setAnimation(ATTACK_1);
                    yield PlayState.CONTINUE;
                }
                case ANIM_ATTACK_2 -> {
                    state.setAnimation(ATTACK_2);
                    yield PlayState.CONTINUE;
                }
                case ANIM_RUSH -> {
                    state.setAnimation(RUSH);
                    yield PlayState.CONTINUE;
                }
                case ANIM_SPECIAL -> {
                    state.setAnimation(SPECIAL);
                    yield PlayState.CONTINUE;
                }
                case ANIM_HURT -> {
                    state.setAnimation(HURT);
                    yield PlayState.CONTINUE;
                }
                default -> {
                    if (state.isMoving()) {
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

    private final class RushGoal extends Goal {
        private int windup;

        RushGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (target == null || rushCooldown > 0 || animState() == ANIM_SPECIAL) {
                return false;
            }
            double dist = distanceToSqr(target);
            return dist > 9.0 && dist < 100.0 && random.nextInt(30) == 0;
        }

        @Override
        public void start() {
            windup = 8;
            playAnim(ANIM_RUSH, 40);
            playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 0.5f);
            getNavigation().stop();
        }

        @Override
        public boolean canContinueToUse() {
            return windup > -12 && getTarget() != null;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0f, 30.0f);
            if (windup-- > 0) {
                return;
            }
            if (windup == -1) {
                Vec3 dir = target.position().subtract(position()).normalize();
                setDeltaMovement(dir.x * 1.55, 0.12, dir.z * 1.55);
                hurtMarked = true;
            }
            if (windup == -8 && distanceToSqr(target) < 6.25) {
                float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.0f;
                // Wall-pin bonus: if target has solid behind them
                if (isPinnedToWall(target)) {
                    dmg *= 1.0f; // already double; keep as double per TZ
                }
                target.hurt(damageSources().mobAttack(VitrifiedGolemEntity.this), dmg);
                playSound(SoundEvents.GLASS_BREAK, 1.1f, 0.7f);
            }
        }

        @Override
        public void stop() {
            rushCooldown = 80 + random.nextInt(60);
            if (animState() == ANIM_RUSH) {
                entityData.set(ANIM_STATE, ANIM_IDLE);
            }
        }

        private boolean isPinnedToWall(LivingEntity target) {
            Vec3 away = target.position().subtract(position()).normalize();
            BlockPos behind = BlockPos.containing(target.position().add(away.scale(0.8)));
            return !level().getBlockState(behind).getCollisionShape(level(), behind).isEmpty();
        }
    }

    private final class SpecialFlashGoal extends Goal {
        private int ticks;

        SpecialFlashGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (specialCooldown > 0 || getHealth() > getMaxHealth() * 0.5f) {
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && distanceToSqr(target) < 64.0 && random.nextInt(40) == 0;
        }

        @Override
        public void start() {
            ticks = 50;
            playAnim(ANIM_SPECIAL, 50);
            getNavigation().stop();
            playSound(SoundEvents.WARDEN_SONIC_CHARGE, 0.55f, 1.4f);
        }

        @Override
        public boolean canContinueToUse() {
            return ticks > 0;
        }

        @Override
        public void tick() {
            ticks--;
            if (ticks == 24) {
                doPhiFlash();
            }
        }

        @Override
        public void stop() {
            specialCooldown = 400 + random.nextInt(200); // 20–30s
            if (animState() == ANIM_SPECIAL) {
                entityData.set(ANIM_STATE, ANIM_IDLE);
            }
        }

        private void doPhiFlash() {
            playSound(SoundEvents.WARDEN_SONIC_BOOM, 0.7f, 1.6f);
            playSound(SoundEvents.GLASS_BREAK, 1.0f, 0.6f);
            if (!(level() instanceof ServerLevel server)) {
                return;
            }
            double cx = getX();
            double cy = getY() + 1.2;
            double cz = getZ();
            server.sendParticles(ModParticleTypes.PHI_SPARK.get(), cx, cy, cz, 50, 1.5, 0.8, 1.5, 0.08);
            server.sendParticles(ModParticleTypes.ELEMENTAL_PLASMA.get(), cx, cy, cz, 20, 1.2, 0.6, 1.2, 0.04);

            AABB box = getBoundingBox().inflate(3.5);
            for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class, box, e -> e != VitrifiedGolemEntity.this)) {
                living.hurt(damageSources().magic(), 4.0f);
                if (living instanceof Player player) {
                    PlayerPsiData data = PsiHelper.get(player);
                    if (data.initiated() && data.currentPsi() > 0f) {
                        float drain = Math.min(data.currentPsi(), 8.0f + random.nextFloat() * 6.0f);
                        data.setCurrentPsi(data.currentPsi() - drain);
                        PsiHelper.set(player, data);
                        player.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable(
                                        "message.effecoria.vitrified_golem_psi_drain"),
                                true);
                    }
                }
            }
            // Mild knockback ring
            for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class, box, e -> e != VitrifiedGolemEntity.this)) {
                Vec3 push = living.position().subtract(position()).normalize().scale(0.55);
                living.push(push.x, 0.2, push.z);
            }
        }
    }
}
