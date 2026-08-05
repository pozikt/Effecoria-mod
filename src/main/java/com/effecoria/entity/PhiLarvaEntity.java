package com.effecoria.entity;

import com.effecoria.content.ModEntities;
import com.effecoria.content.ModItems;
import com.effecoria.content.PhiHarnessItems;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Φ-larva — glowing worm that crawls near essonite. Breedable “magic battery”:
 * adults near a player slowly restore Ψ / charge Φ-cells.
 */
public class PhiLarvaEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.phi_larva.idle");
    private static final RawAnimation CRAWL = RawAnimation.begin().thenLoop("animation.phi_larva.crawl");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PhiLarvaEntity(EntityType<? extends PhiLarvaEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.FOLLOW_RANGE, 12.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.35));
        this.goalSelector.addGoal(2, new BreedGoal(this, 0.9));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.05, Ingredient.of(ModItems.ESSENITE_DUST.get()), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.0));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.85));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || isBaby() || tickCount % 40 != 0) {
            return;
        }
        Player nearest = level().getNearestPlayer(this, 4.0);
        if (nearest == null || !nearest.isAlive()) {
            return;
        }
        var data = PsiHelper.get(nearest);
        if (data.initiated()) {
            data.setCurrentPsi(data.currentPsi() + 2.5f);
            PsiHelper.set(nearest, data);
        }
        var cell = PhiHarnessItems.findPhiCell(nearest);
        if (!cell.isEmpty()) {
            float charge = PhiHarnessItems.cellCharge(cell);
            if (charge < 1f) {
                PhiHarnessItems.setCellCharge(cell, Math.min(1f, charge + 0.04f));
            }
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModItems.ESSENITE_DUST.get());
    }

    @Override
    public PhiLarvaEntity getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.PHI_LARVA.get().create(level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            if (state.isMoving()) {
                state.setAnimation(CRAWL);
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
