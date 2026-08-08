package com.effecoria.world;

import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModBiomeTags;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vitrified Wastes — residual Φ radiation after a flash-vitrification event.
 * Magic still flows (elevated Φ), but the ground burns the unprotected.
 */
public final class VitrifiedWastesService {
    private VitrifiedWastesService() {}

    private static final Map<UUID, Integer> STAY_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> WARNED = new ConcurrentHashMap<>();
    /** World-day keyed storm end tick (game time). */
    private static long stormEndsAt = -1L;

    public static boolean isBiome(LevelReader level, BlockPos pos) {
        return level.getBiome(pos).is(ModBiomeTags.VITRIFIED_WASTES);
    }

    public static boolean isIn(Level level, Vec3 position) {
        return isBiome(level, BlockPos.containing(position));
    }

    public static float phiEnvironmentBonus(Level level, BlockPos pos) {
        if (!isBiome(level, pos)) {
            return 0f;
        }
        float bonus = BalanceConfig.VITRIFIED_PHI_BONUS.get().floatValue();
        if (isStormActive(level)) {
            bonus += BalanceConfig.VITRIFIED_STORM_PHI_BONUS.get().floatValue();
        }
        return bonus;
    }

    public static boolean isStormActive(Level level) {
        return stormEndsAt > level.getGameTime();
    }

    public static void tickPlayer(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        UUID id = player.getUUID();
        if (!isIn(player.level(), player.position())) {
            STAY_TICKS.remove(id);
            WARNED.remove(id);
            return;
        }

        int ticks = STAY_TICKS.getOrDefault(id, 0) + 1;
        STAY_TICKS.put(id, ticks);

        if (player instanceof ServerPlayer serverPlayer && WARNED.putIfAbsent(id, Boolean.TRUE) == null) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.effecoria.vitrified_wastes_enter"), true);
        }

        maybeStartStorm(player.level());
        tickRadiation(player, ticks);
        if (isStormActive(player.level())) {
            tickStorm(player);
        }
    }

    private static void maybeStartStorm(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (isStormActive(level)) {
            return;
        }
        // ~ once per several hours of play, checked each second of player presence
        if (serverLevel.getGameTime() % 20 != 0) {
            return;
        }
        float chance = BalanceConfig.VITRIFIED_STORM_CHANCE_PER_SECOND.get().floatValue();
        if (serverLevel.random.nextFloat() < chance) {
            int duration = BalanceConfig.VITRIFIED_STORM_DURATION_TICKS.get();
            stormEndsAt = serverLevel.getGameTime() + duration;
            for (ServerPlayer p : serverLevel.players()) {
                if (isIn(serverLevel, p.position())) {
                    p.displayClientMessage(
                            Component.translatable("message.effecoria.vitrified_storm_start"), true);
                }
            }
        }
    }

    private static void tickRadiation(Player player, int stayTicks) {
        if (stayTicks % 20 != 0) {
            return;
        }
        PhiRadiationService.Shield shield = PhiRadiationService.evaluate(player);
        float remain = shield.remaining();
        if (remain <= 0.001f) {
            var data = PsiHelper.get(player);
            if (data.initiated() && data.currentPsi() < data.maxPsi()) {
                float regen = BalanceConfig.VITRIFIED_PROTECTED_PSI_REGEN.get().floatValue();
                data.setCurrentPsi(Math.min(data.maxPsi(), data.currentPsi() + regen));
                PsiHelper.set(player, data);
            }
            return;
        }
        if (shield.adequate()) {
            var data = PsiHelper.get(player);
            if (data.initiated() && data.currentPsi() < data.maxPsi()) {
                float regen = BalanceConfig.VITRIFIED_PROTECTED_PSI_REGEN.get().floatValue() * (1f - remain);
                data.setCurrentPsi(Math.min(data.maxPsi(), data.currentPsi() + regen));
                PsiHelper.set(player, data);
            }
        }
        float dmg = BalanceConfig.VITRIFIED_RADIATION_DAMAGE.get().floatValue() * remain;
        if (dmg > 0f) {
            player.hurt(player.damageSources().magic(), dmg);
        }
        if (remain > 0.4f) {
            BreathDebuffs.apply(
                    player, new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true));
            if (!PsiHelper.get(player).initiated()) {
                BreathDebuffs.apply(
                        player, new MobEffectInstance(MobEffects.HUNGER, 40, 0, false, false, true));
            }
        }
    }

    private static void tickStorm(Player player) {
        if (player.tickCount % 20 != 0) {
            return;
        }
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ModParticleTypes.PHI_SPARK.get(),
                    player.getX(),
                    player.getY() + 1.0,
                    player.getZ(),
                    6,
                    0.8,
                    0.6,
                    0.8,
                    0.02);
        }
        float remain = PhiRadiationService.evaluate(player).remaining();
        if (remain <= 0.001f) {
            return;
        }
        float stormDmg = BalanceConfig.VITRIFIED_STORM_DAMAGE.get().floatValue() * remain;
        if (stormDmg > 0f) {
            player.hurt(player.damageSources().magic(), stormDmg);
        }
        if (remain > 0.35f) {
            BreathDebuffs.apply(
                    player, new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, true));
        }
    }

    /** Delegates to catalogued Φ-radiation shield. */
    public static boolean hasPhiProtection(Player player) {
        return PhiRadiationService.hasPhiProtection(player);
    }
}
