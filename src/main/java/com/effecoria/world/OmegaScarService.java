package com.effecoria.world;

import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModBiomeTags;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
 * Ω-Scar — causality rupture. Ambient Ω fog/rain via {@link com.effecoria.world.weather.PhiWeatherService}.
 */
public final class OmegaScarService {
    private OmegaScarService() {}

    private static final Map<UUID, Integer> STAY_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> WARNED = new ConcurrentHashMap<>();

    public static boolean isBiome(LevelReader level, BlockPos pos) {
        return level.getBiome(pos).is(ModBiomeTags.OMEGA_SCAR);
    }

    public static boolean isIn(Level level, Vec3 position) {
        return isBiome(level, BlockPos.containing(position));
    }

    public static float phiEnvironmentBonus(Level level, BlockPos pos) {
        if (!isBiome(level, pos)) {
            return 0f;
        }
        return BalanceConfig.OMEGA_SCAR_PHI_BONUS.get().floatValue();
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
                    Component.translatable("message.effecoria.omega_scar_enter"), true);
        }

        if (ticks % 40 != 0) {
            return;
        }

        var data = PsiHelper.get(player);
        if (!data.initiated()) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, true));
            if (ticks >= 200) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false, true));
            }
            return;
        }

        if (ticks >= 300 && player.tickCount % 100 == 0) {
            data.setEntropyB(
                    data.entropyB() + BalanceConfig.OMEGA_SCAR_ENTROPY_PULSE.get().floatValue());
            PsiHelper.set(player, data);
        }
    }

    public static void clearPlayer(UUID id) {
        STAY_TICKS.remove(id);
        WARNED.remove(id);
    }
}
