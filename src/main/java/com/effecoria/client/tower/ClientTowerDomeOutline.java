package com.effecoria.client.tower;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import javax.annotation.Nullable;

/** Ultramarine combat Φ-dome wireframe synced from the tower anchor. */
@EventBusSubscriber(modid = com.effecoria.EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientTowerDomeOutline {
    @Nullable
    private static BlockPos anchorPos;
    @Nullable
    private static AABB domeBox;
    private static boolean combat;

    private ClientTowerDomeOutline() {}

    public static void apply(BlockPos anchor, boolean activeCombat, @Nullable AABB box) {
        if (!activeCombat || box == null) {
            if (anchorPos != null && anchorPos.equals(anchor)) {
                clear();
            } else if (anchorPos == null) {
                clear();
            }
            return;
        }
        anchorPos = anchor.immutable();
        domeBox = box;
        combat = true;
    }

    public static void clear() {
        anchorPos = null;
        domeBox = null;
        combat = false;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        if (!combat || domeBox == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        VertexConsumer lines = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(pose, lines, domeBox.inflate(0.01), 0.25f, 0.4f, 1.0f, 0.95f);
        LevelRenderer.renderLineBox(pose, lines, domeBox.deflate(0.08), 0.1f, 0.2f, 0.55f, 0.45f);
        pose.popPose();
    }
}
