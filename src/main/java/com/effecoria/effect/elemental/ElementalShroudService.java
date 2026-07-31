package com.effecoria.effect.elemental;

import com.effecoria.core.formula.BreathDebuffs;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.phi.CreativeGodMode;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/** Concentration-style elemental shrouds / air form attached to the caster. */
public final class ElementalShroudService {
    public enum Kind {
        WATER,
        AIR,
        AIR_FORM
    }

    private static final Map<UUID, ActiveShroud> ACTIVE = new HashMap<>();

    private static final class ActiveShroud {
        Kind kind;
        long expireAt;
        float drainPerTick;
    }

    private ElementalShroudService() {}

    public static void activate(
            ServerPlayer player, Kind kind, int durationTicks, float maintainDrainPerSecond) {
        ActiveShroud shroud = new ActiveShroud();
        shroud.kind = kind;
        shroud.expireAt = player.level().getGameTime() + Math.max(1, durationTicks);
        shroud.drainPerTick = Math.max(0f, maintainDrainPerSecond / 20f);
        ACTIVE.put(player.getUUID(), shroud);

        ServerLevel level = player.serverLevel();
        switch (kind) {
            case WATER -> level.playSound(
                    null, player.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.7f, 0.8f);
            case AIR, AIR_FORM -> level.playSound(
                    null, player.blockPosition(), SoundEvents.BREEZE_INHALE, SoundSource.PLAYERS, 0.8f, 1.1f);
        }
    }

    public static void clearFor(UUID playerId) {
        ACTIVE.remove(playerId);
    }

    public static void tick(ServerPlayer player) {
        ActiveShroud shroud = ACTIVE.get(player.getUUID());
        if (shroud == null) {
            return;
        }
        long now = player.level().getGameTime();
        if (now >= shroud.expireAt || !player.isAlive()) {
            stop(player);
            return;
        }
        if (shroud.drainPerTick > 0f && !CreativeGodMode.isActive(player)) {
            PlayerPsiData data = PsiHelper.get(player);
            if (data.currentPsi() < shroud.drainPerTick) {
                stop(player);
                return;
            }
            data.setCurrentPsi(data.currentPsi() - shroud.drainPerTick);
            PsiHelper.set(player, data);
            player.syncData(ModAttachments.PSI.get());
        }

        applyBuffs(player, shroud.kind);
        if (shroud.kind == Kind.WATER && now % 10 == 0) {
            applyWaterAura(player);
        }
        if (now % 4 == 0) {
            spawnParticles(player, shroud.kind);
        }
    }

    public static void purgeStale(ServerLevel level) {
        Iterator<Map.Entry<UUID, ActiveShroud>> it = ACTIVE.entrySet().iterator();
        long now = level.getGameTime();
        while (it.hasNext()) {
            Map.Entry<UUID, ActiveShroud> entry = it.next();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null || now >= entry.getValue().expireAt) {
                it.remove();
            }
        }
    }

    private static void stop(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = false;
            player.getAbilities().mayfly = false;
            player.onUpdateAbilities();
        }
    }

    private static void applyBuffs(ServerPlayer player, Kind kind) {
        switch (kind) {
            case WATER -> {
                BreathDebuffs.apply(player, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 25, 0, false, false, true));
                BreathDebuffs.apply(player, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 25, 0, false, false, true));
                BreathDebuffs.apply(player, new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 25, 0, false, false, true));
            }
            case AIR -> {
                BreathDebuffs.apply(player, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, 1, false, false, true));
                BreathDebuffs.apply(player, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 25, 0, false, false, true));
                BreathDebuffs.apply(player, new MobEffectInstance(MobEffects.JUMP, 25, 1, false, false, true));
            }
            case AIR_FORM -> {
                BreathDebuffs.apply(player, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 25, 3, false, false, true));
                BreathDebuffs.apply(player, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, 2, false, false, true));
                BreathDebuffs.apply(player, new MobEffectInstance(MobEffects.SLOW_FALLING, 25, 0, false, false, true));
                if (!player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = true;
                    player.onUpdateAbilities();
                }
                if (!player.onGround()) {
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                }
            }
        }
    }

    private static void applyWaterAura(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        AABB box = player.getBoundingBox().inflate(3.5);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
            BreathDebuffs.apply(player, entity, new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, false, false, true));
            BreathDebuffs.apply(player, entity, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, false, true));
        }
    }

    private static void spawnParticles(ServerPlayer player, Kind kind) {
        ServerLevel level = player.serverLevel();
        switch (kind) {
            case WATER -> level.sendParticles(
                    ModParticleTypes.WATER_SPLASH.get(),
                    player.getX(),
                    player.getY() + 1,
                    player.getZ(),
                    4,
                    0.35,
                    0.4,
                    0.35,
                    0.02);
            case AIR -> level.sendParticles(
                    ModParticleTypes.PHI_GUST.get(),
                    player.getX(),
                    player.getY() + 1,
                    player.getZ(),
                    3,
                    0.3,
                    0.3,
                    0.3,
                    0.02);
            case AIR_FORM -> level.sendParticles(
                    ParticleTypes.CLOUD,
                    player.getX(),
                    player.getY() + 1,
                    player.getZ(),
                    5,
                    0.4,
                    0.5,
                    0.4,
                    0.01);
        }
    }
}
