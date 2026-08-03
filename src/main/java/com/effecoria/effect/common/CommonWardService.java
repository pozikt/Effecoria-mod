package com.effecoria.effect.common;

import com.effecoria.core.psi.PsiHelper;
import com.effecoria.core.psi.PlayerPsiData;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Continuous Ψ-ward — buffers corruption curses and mental affliction while draining Ψ.
 */
public final class CommonWardService {
    public static final String WARD_UNTIL_TAG = "effecoria:psi_ward_until";
    /** Ψ drained every second while the ward is up. */
    public static final float DRAIN_PER_SECOND = 1.25f;

    private CommonWardService() {}

    public static void activate(LivingEntity entity, long untilGameTime) {
        entity.getPersistentData().putLong(WARD_UNTIL_TAG, untilGameTime);
    }

    public static boolean hasWard(LivingEntity entity, long gameTime) {
        return entity.getPersistentData().getLong(WARD_UNTIL_TAG) > gameTime;
    }

    public static void clear(LivingEntity entity) {
        entity.getPersistentData().remove(WARD_UNTIL_TAG);
    }

    /** Drain upkeep; drop the ward when Ψ runs dry. */
    public static void tickPlayer(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (!hasWard(player, now)) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        PlayerPsiData data = PsiHelper.get(player);
        float have = data.currentPsi();
        if (have < DRAIN_PER_SECOND) {
            clear(player);
            player.displayClientMessage(Component.translatable("message.effecoria.common.ward_drop"), true);
            return;
        }
        data.setCurrentPsi(have - DRAIN_PER_SECOND);
        PsiHelper.set(player, data);
    }
}
