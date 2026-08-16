package com.effecoria.entity;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Canopy wailer — harmless bat that unsettles trespassers with cave-wails and gloom. */
public class WailerBatEntity extends Bat implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.wailer_bat.idle");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.wailer_bat.fly");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public WailerBatEntity(EntityType<? extends WailerBatEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType spawnType) {
        return true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() || tickCount % 160 != 0 || random.nextFloat() > 0.28f) {
            return;
        }
        var players = level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(10.0));
        if (players.isEmpty()) {
            return;
        }
        Player target = players.get(random.nextInt(players.size()));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 90, 0, true, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 70, 0, true, false, true));
        level().playSound(null, blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.9f, 0.55f);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            if (state.isMoving() || state.getLimbSwingAmount() > 0.02f) {
                state.setAnimation(FLY);
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
