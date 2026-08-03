package com.effecoria.core.formula;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Applies potion effects after a delay — for rush→crash spells (strength, then weakness).
 */
public final class DelayedEffectService {
    private static final List<Pending> PENDING = new ArrayList<>();

    private DelayedEffectService() {}

    private record Pending(UUID entityId, MobEffectInstance effect, long applyAtGameTime) {}

    /** Schedule {@code effect} to apply after {@code delayTicks}. Replaces any pending same effect type. */
    public static void schedule(LivingEntity entity, MobEffectInstance effect, int delayTicks) {
        if (entity == null || effect == null || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (delayTicks <= 0) {
            BreathDebuffs.applyExact(entity, effect);
            return;
        }
        Holder<MobEffect> type = effect.getEffect();
        UUID id = entity.getUUID();
        PENDING.removeIf(p -> p.entityId().equals(id) && p.effect().getEffect().equals(type));
        PENDING.add(new Pending(id, new MobEffectInstance(effect), level.getGameTime() + delayTicks));
    }

    public static void tick(ServerLevel level) {
        if (PENDING.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext()) {
            Pending pending = it.next();
            if (now < pending.applyAtGameTime()) {
                continue;
            }
            it.remove();
            Entity entity = level.getEntity(pending.entityId());
            if (entity instanceof LivingEntity living && living.isAlive()) {
                BreathDebuffs.applyExact(living, pending.effect());
            }
        }
    }

    public static void clearFor(UUID entityId) {
        PENDING.removeIf(p -> p.entityId().equals(entityId));
    }
}
