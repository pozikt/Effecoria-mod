package com.effecoria.world;

import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModBiomeTags;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Crystal Forest — humid Φ woodland: dense mist, frequent essence rain, rare storms.
 */
public final class CrystalForestService {
    private CrystalForestService() {}

    private static final Map<UUID, Boolean> WARNED = new ConcurrentHashMap<>();

    public static boolean isBiome(LevelReader level, BlockPos pos) {
        return level.getBiome(pos).is(ModBiomeTags.CRYSTAL_FOREST);
    }

    public static boolean isIn(Level level, Vec3 position) {
        return isBiome(level, BlockPos.containing(position));
    }

    public static float phiEnvironmentBonus(Level level, BlockPos pos) {
        if (!isBiome(level, pos)) {
            return 0f;
        }
        return BalanceConfig.CRYSTAL_FOREST_PHI_BONUS.get().floatValue();
    }

    public static void tickPlayer(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        UUID id = player.getUUID();
        if (!isIn(player.level(), player.position())) {
            WARNED.remove(id);
            return;
        }
        if (player instanceof ServerPlayer serverPlayer && WARNED.putIfAbsent(id, Boolean.TRUE) == null) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.effecoria.crystal_forest_enter"), true);
        }
        PhiFogService.tickPlayer(player);
    }

    public static void clearPlayer(UUID id) {
        WARNED.remove(id);
    }
}
