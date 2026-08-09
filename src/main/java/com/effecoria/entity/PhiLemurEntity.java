package com.effecoria.entity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

/** Shy canopy lemur — panics easily and fades when stared at. */
public class PhiLemurEntity extends Fox {
    private static final double LOOK_DOT_THRESHOLD = 0.96;

    public PhiLemurEntity(EntityType<? extends PhiLemurEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType spawnType) {
        return true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Fox.createAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.42)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.65));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() || tickCount % 4 != 0) {
            return;
        }
        for (Player player : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(14.0))) {
            if (player.hasLineOfSight(this) && isPlayerLookingAt(player)) {
                addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 50, 0, false, false, false));
                break;
            }
        }
    }

    private boolean isPlayerLookingAt(Player player) {
        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 toMob = new Vec3(
                        getX() - player.getX(),
                        getEyeY() - player.getEyeY(),
                        getZ() - player.getZ())
                .normalize();
        return view.dot(toMob) >= LOOK_DOT_THRESHOLD;
    }
}
