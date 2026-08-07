package com.effecoria.entity;

import com.effecoria.content.ModItems;
import com.effecoria.content.ModParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
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
 * Eidos — rare pure Φ-field being. Passive; offering an essonite crystal yields a buff or a short portal hop.
 */
public class EidosEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Boolean> GIFTING =
            SynchedEntityData.defineId(EidosEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.eidos.idle");
    private static final RawAnimation GIFT = RawAnimation.begin().thenPlay("animation.eidos.gift");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int giftAnimTicks;
    private int giftCooldown;

    public EidosEntity(EntityType<? extends EidosEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 12, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FLYING_SPEED, 0.35)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GIFTING, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new DriftGoal());
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 10.0f));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ModItems.ESSONITE_CRYSTAL.get()) && !stack.is(ModItems.PURE_ESSONITE.get())) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (giftCooldown > 0) {
            player.displayClientMessage(Component.translatable("message.effecoria.eidos_busy"), true);
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        giftAnimTicks = 20;
        this.entityData.set(GIFTING, true);
        giftCooldown = 20 * 30;
        playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.4f);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(
                    ModParticleTypes.PHI_SPARK.get(),
                    getX(),
                    getY() + 1.0,
                    getZ(),
                    40,
                    0.6,
                    0.8,
                    0.6,
                    0.04);
        }
        if (random.nextBoolean()) {
            grantBuff(player);
        } else {
            openHop(player);
        }
        return InteractionResult.CONSUME;
    }

    private void grantBuff(Player player) {
        int pick = random.nextInt(4);
        MobEffectInstance effect = switch (pick) {
            case 0 -> new MobEffectInstance(MobEffects.REGENERATION, 20 * 30, 1, false, true, true);
            case 1 -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 40, 0, false, true, true);
            case 2 -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 45, 1, false, true, true);
            default -> new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 90, 0, false, true, true);
        };
        player.addEffect(effect);
        player.displayClientMessage(Component.translatable("message.effecoria.eidos_gift"), true);
    }

    private void openHop(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(level() instanceof ServerLevel server)) {
            return;
        }
        BlockPos origin = blockPosition();
        for (int attempt = 0; attempt < 12; attempt++) {
            int dx = random.nextInt(97) - 48;
            int dz = random.nextInt(97) - 48;
            int x = origin.getX() + dx;
            int z = origin.getZ() + dz;
            int y = server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos dest = new BlockPos(x, y, z);
            if (!server.getBlockState(dest).isAir() && server.getBlockState(dest.above()).isAir()) {
                serverPlayer.teleportTo(server, x + 0.5, y + 1.0, z + 0.5, serverPlayer.getYRot(), serverPlayer.getXRot());
                server.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.PORTAL,
                        x + 0.5,
                        y + 1.5,
                        z + 0.5,
                        40,
                        0.4,
                        0.6,
                        0.4,
                        0.2);
                serverPlayer.displayClientMessage(Component.translatable("message.effecoria.eidos_portal"), true);
                playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.2f);
                return;
            }
        }
        grantBuff(player);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            if (giftAnimTicks > 0) {
                giftAnimTicks--;
                if (giftAnimTicks == 0) {
                    this.entityData.set(GIFTING, false);
                }
            }
            if (giftCooldown > 0) {
                giftCooldown--;
            }
        }
        if (level().isClientSide && tickCount % 8 == 0) {
            level().addParticle(
                    ModParticleTypes.PHI_SPARK.get(),
                    getX() + (random.nextDouble() - 0.5) * 0.8,
                    getY() + 0.8 + random.nextDouble(),
                    getZ() + (random.nextDouble() - 0.5) * 0.8,
                    0,
                    0.02,
                    0);
        }
    }

    public boolean isGiftingAnim() {
        return this.entityData.get(GIFTING);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount * 0.5f);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {}

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ALLAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ALLAY_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            if (isGiftingAnim()) {
                state.setAnimation(GIFT);
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

    private class DriftGoal extends Goal {
        private int cooldown;

        DriftGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return --cooldown <= 0;
        }

        @Override
        public void start() {
            cooldown = 50 + random.nextInt(70);
            Vec3 pos = position();
            double x = pos.x + (random.nextDouble() - 0.5) * 10;
            double z = pos.z + (random.nextDouble() - 0.5) * 10;
            int ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
            double y = Math.max(ground + 2.0, pos.y + (random.nextDouble() - 0.5) * 3);
            getMoveControl().setWantedPosition(x, y, z, 0.65);
        }
    }
}
