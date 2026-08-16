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
 * Deep Φ pocket — low-light, high-entropy underground caves with residual deep Φ.
 * Milder than {@link OmegaScarService}: entropy drip + rare Darkness, no gravity/time-loop.
 */
public final class DeepPhiPocketService {
    private DeepPhiPocketService() {}

    private static final Map<UUID, Integer> STAY_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> WARNED = new ConcurrentHashMap<>();
    /** Absolute {@link Level#getGameTime()} of last Darkness pulse — not cleared on brief biome flicker. */
    private static final Map<UUID, Long> LAST_DARKNESS_GAME_TIME = new ConcurrentHashMap<>();

    public static boolean isBiome(LevelReader level, BlockPos pos) {
        return level.getBiome(pos).is(ModBiomeTags.DEEP_PHI_POCKET);
    }

    public static boolean isIn(Level level, Vec3 position) {
        return isBiome(level, BlockPos.containing(position));
    }

    public static float phiEnvironmentBonus(Level level, BlockPos pos) {
        if (!isBiome(level, pos)) {
            return 0f;
        }
        return BalanceConfig.DEEP_PHI_POCKET_PHI_BONUS.get().floatValue();
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
                    Component.translatable("message.effecoria.deep_phi_pocket_enter"), true);
        }

        tickDarkness(player, id);
        tickEntropy(player, ticks);
    }

    private static void tickDarkness(Player player, UUID id) {
        int duration = BalanceConfig.DEEP_PHI_POCKET_DARKNESS_DURATION.get();
        if (duration <= 0) {
            return;
        }
        if (player.level().canSeeSky(player.blockPosition())) {
            return;
        }
        long now = player.level().getGameTime();
        int cooldown = BalanceConfig.DEEP_PHI_POCKET_DARKNESS_COOLDOWN.get();
        long last = LAST_DARKNESS_GAME_TIME.getOrDefault(id, Long.MIN_VALUE / 4);
        if (last != Long.MIN_VALUE / 4 && now - last < cooldown) {
            return;
        }
        // First pulse also waits a full cooldown after world join / never pulsed.
        if (last == Long.MIN_VALUE / 4) {
            LAST_DARKNESS_GAME_TIME.put(id, now);
            return;
        }
        LAST_DARKNESS_GAME_TIME.put(id, now);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, duration, 0, false, false, true));
    }

    private static void tickEntropy(Player player, int stayTicks) {
        if (stayTicks % 40 != 0 || stayTicks < 80) {
            return;
        }
        var data = PsiHelper.get(player);
        float pulse = BalanceConfig.DEEP_PHI_POCKET_ENTROPY_PULSE.get().floatValue();
        data.setEntropyB(data.entropyB() + pulse);
        PsiHelper.set(player, data);
    }

    public static void clearPlayer(UUID id) {
        STAY_TICKS.remove(id);
        WARNED.remove(id);
        LAST_DARKNESS_GAME_TIME.remove(id);
    }
}
