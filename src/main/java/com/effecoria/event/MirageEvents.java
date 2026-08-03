package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.effect.mental.MirageWorldService;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Keeps mirage blocks client-only: cancel dig/place that would desync the victim’s illusion. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class MirageEvents {
    private MirageEvents() {}

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!MirageWorldService.isMirageBlock(player, event.getPos())) {
            return;
        }
        event.setCanceled(true);
        MirageWorldService.resend(player, event.getPos());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!MirageWorldService.isMirageBlock(player, event.getPos())) {
            return;
        }
        event.setCanceled(true);
        MirageWorldService.resend(player, event.getPos());
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (!MirageWorldService.isMirageBlock(player, event.getPos())) {
            return;
        }
        event.setCanceled(true);
        MirageWorldService.resend(player, event.getPos());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MirageWorldService.onLogout(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MirageWorldService.onLogout(player);
        }
    }
}
