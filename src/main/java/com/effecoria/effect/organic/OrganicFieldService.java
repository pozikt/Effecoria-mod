package com.effecoria.effect.organic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.DiceDamage;
import com.effecoria.core.phi.CreativeGodMode;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Regeneration aura — biological field (D&D level 5). */
public final class OrganicFieldService {
    private static final List<HealField> FIELDS = new CopyOnWriteArrayList<>();

    private static final class HealField {
        ServerLevel level;
        Vec3 center;
        float radius;
        long expireAt;
        UUID owner;
        float maintainDrainPerTick;
        float healPerSecond;
    }

    private OrganicFieldService() {}

    public static void spawn(
            ServerLevel level,
            Vec3 center,
            UUID owner,
            float radius,
            int durationTicks,
            float maintainDrainPerSecond,
            float healPerSecond) {
        HealField field = new HealField();
        field.level = level;
        field.center = center;
        field.radius = Math.max(1f, radius);
        field.expireAt = level.getGameTime() + Math.max(1, durationTicks);
        field.owner = owner;
        field.maintainDrainPerTick = Math.max(0f, maintainDrainPerSecond / 20f);
        field.healPerSecond = Math.max(0f, healPerSecond);
        FIELDS.add(field);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.1f);
    }

    public static void clearFor(UUID owner) {
        FIELDS.removeIf(f -> owner.equals(f.owner));
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        List<HealField> toRemove = new ArrayList<>();
        for (HealField field : FIELDS) {
            if (field.level != level) {
                continue;
            }
            if (now >= field.expireAt) {
                toRemove.add(field);
                continue;
            }
            if (field.maintainDrainPerTick > 0f && !drainOwner(field)) {
                toRemove.add(field);
                continue;
            }
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(field.owner);
            if (owner != null) {
                field.center = owner.position().add(0, 0.5, 0);
            }
            if (now % 20 == 0 && field.healPerSecond > 0f) {
                AABB box = new AABB(field.center, field.center).inflate(field.radius);
                for (ServerPlayer ally : level.getEntitiesOfClass(ServerPlayer.class, box, LivingEntity::isAlive)) {
                    if (ally.position().distanceToSqr(field.center) > (double) field.radius * field.radius) {
                        continue;
                    }
                    ally.heal(field.healPerSecond);
                    OrganicEffects.spawnOrganicParticles(level, ally.position().add(0, 1, 0));
                }
            }
            if (now % 6 == 0) {
                level.sendParticles(
                        ModParticleTypes.ORGANIC_FOG.get(),
                        field.center.x,
                        field.center.y + 0.5,
                        field.center.z,
                        6,
                        field.radius * 0.35,
                        0.4,
                        field.radius * 0.35,
                        0.01);
            }
        }
        FIELDS.removeAll(toRemove);
    }

    private static boolean drainOwner(HealField field) {
        ServerPlayer owner = field.level.getServer().getPlayerList().getPlayer(field.owner);
        if (owner == null) {
            return false;
        }
        if (CreativeGodMode.isActive(owner)) {
            return true;
        }
        PlayerPsiData data = PsiHelper.get(owner);
        if (data.currentPsi() < field.maintainDrainPerTick) {
            return false;
        }
        data.setCurrentPsi(data.currentPsi() - field.maintainDrainPerTick);
        PsiHelper.set(owner, data);
        owner.syncData(ModAttachments.PSI.get());
        return true;
    }
}
