package com.effecoria.client;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Client feedback for mirage — no HUD overlay; blocks arrive via vanilla block-update packets. */
public final class MirageClient {
    private MirageClient() {}

    public static void onStart(int durationTicks, float maxHp, float intensity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            mc.level.playLocalSound(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    SoundEvents.ILLUSIONER_MIRROR_MOVE,
                    SoundSource.PLAYERS,
                    0.9f,
                    0.7f,
                    false);
        }
    }

    public static void onHurt(float amount, float remainingHp, float maxHp) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        mc.player.hurtTime = 10;
        mc.player.hurtDuration = 10;
        mc.level.playLocalSound(
                mc.player.getX(),
                mc.player.getY(),
                mc.player.getZ(),
                SoundEvents.PLAYER_HURT,
                SoundSource.PLAYERS,
                0.75f,
                1.0f,
                false);
    }

    public static void onEnd(boolean collapsed) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            mc.level.playLocalSound(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    collapsed ? SoundEvents.ILLUSIONER_CAST_SPELL : SoundEvents.ILLUSIONER_PREPARE_MIRROR,
                    SoundSource.PLAYERS,
                    0.75f,
                    collapsed ? 0.55f : 1.15f,
                    false);
        }
    }
}
