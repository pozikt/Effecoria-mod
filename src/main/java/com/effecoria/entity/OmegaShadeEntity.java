package com.effecoria.entity;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Ω-Shade — Scar parasite built on vex flight; drains Ψ when latched. */
public class OmegaShadeEntity extends Vex {
    public OmegaShadeEntity(EntityType<? extends OmegaShadeEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType spawnType) {
        return true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Vex.createAttributes()
                .add(Attributes.MAX_HEALTH, 18.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new DrainAttachGoal());
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() && tickCount % 3 == 0) {
            level().addParticle(
                    ModParticleTypes.CORRUPTION_MIASMA.get(),
                    getX() + (random.nextDouble() - 0.5) * 0.5,
                    getY() + 0.4,
                    getZ() + (random.nextDouble() - 0.5) * 0.5,
                    0,
                    0.02,
                    0);
        }
    }

    private final class DrainAttachGoal extends Goal {
        private LivingEntity target;
        private int drainTicks;

        DrainAttachGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public void start() {
            target = getTarget();
            drainTicks = 0;
        }

        @Override
        public void stop() {
            target = null;
        }

        @Override
        public void tick() {
            if (target == null || !target.isAlive()) {
                return;
            }
            getLookControl().setLookAt(target, 30f, 30f);
            double dist = distanceToSqr(target);
            if (dist > 2.25) {
                Vec3 toward = target.position().add(0, target.getBbHeight() * 0.55, 0).subtract(position());
                setDeltaMovement(getDeltaMovement().scale(0.55).add(toward.normalize().scale(0.2)));
                hasImpulse = true;
            } else {
                setDeltaMovement(getDeltaMovement().scale(0.35));
                drainTicks++;
                if (drainTicks % 20 == 0 && target instanceof Player player && !level().isClientSide()) {
                    var data = PsiHelper.get(player);
                    if (data.initiated()) {
                        data.setCurrentPsi(Math.max(0f, data.currentPsi() - 4.5f));
                        data.setEntropyB(data.entropyB() + 0.03f);
                        PsiHelper.set(player, data);
                    }
                    target.hurt(damageSources().magic(), 1.5f);
                }
            }
        }
    }
}
