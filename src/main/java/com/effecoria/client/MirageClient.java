package com.effecoria.client;

import com.effecoria.EffecoriaMod;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Red-sky mirage atmosphere for the soul; blocks arrive via vanilla block-update packets. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class MirageClient {
    private static boolean active;
    private static long expireAtMs;

    private MirageClient() {}

    public static void onStart(int durationTicks, float maxHp, float intensity) {
        active = true;
        expireAtMs = System.currentTimeMillis() + Math.max(80, durationTicks) * 50L;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            mc.level.playLocalSound(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    SoundEvents.ILLUSIONER_MIRROR_MOVE,
                    SoundSource.PLAYERS,
                    0.95f,
                    0.55f,
                    false);
        }
    }

    public static void onHurt(float amount, float remainingHp, float maxHp) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        int flash = amount >= 6f ? 14 : 10;
        mc.player.hurtTime = flash;
        mc.player.hurtDuration = flash;
        mc.level.playLocalSound(
                mc.player.getX(),
                mc.player.getY(),
                mc.player.getZ(),
                amount >= 6f ? SoundEvents.WARDEN_HURT : SoundEvents.PLAYER_HURT,
                SoundSource.PLAYERS,
                0.8f,
                amount >= 6f ? 0.7f : 1.0f,
                false);
    }

    public static void onEnd(boolean collapsed) {
        active = false;
        expireAtMs = 0L;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            mc.level.playLocalSound(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    collapsed ? SoundEvents.ILLUSIONER_CAST_SPELL : SoundEvents.ILLUSIONER_PREPARE_MIRROR,
                    SoundSource.PLAYERS,
                    0.8f,
                    collapsed ? 0.5f : 1.1f,
                    false);
        }
    }

    public static boolean isActive() {
        if (active && System.currentTimeMillis() >= expireAtMs) {
            active = false;
        }
        return active;
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!isActive()) {
            return;
        }
        event.setNearPlaneDistance(0.5f);
        event.setFarPlaneDistance(18f);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!isActive()) {
            return;
        }
        float pulse = 0.5f + 0.5f * Mth.sin(System.currentTimeMillis() / 900f);
        event.setRed(0.55f + 0.2f * pulse);
        event.setGreen(0.04f + 0.03f * pulse);
        event.setBlue(0.05f);
    }
}
