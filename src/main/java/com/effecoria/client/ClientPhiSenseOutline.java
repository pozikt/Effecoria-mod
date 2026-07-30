package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.formula.PhiSample;
import com.effecoria.core.phi.PhiFieldService;
import com.effecoria.core.psi.PsiHelper;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Colored hitbox outlines for living entities while phi-sense is active. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientPhiSenseOutline {
    private static final double ENTITY_RADIUS = 32;

    private ClientPhiSenseOutline() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (!PsiHelper.get(minecraft.player).isPhiSenseActive(minecraft.level.getGameTime())) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        AABB scan = minecraft.player.getBoundingBox().inflate(ENTITY_RADIUS);
        for (LivingEntity entity : minecraft.level.getEntitiesOfClass(LivingEntity.class, scan, LivingEntity::isAlive)) {
            if (entity == minecraft.player) {
                continue;
            }
            if (entity.distanceToSqr(minecraft.player) > ENTITY_RADIUS * ENTITY_RADIUS) {
                continue;
            }
            PhiSample sample = PhiFieldService.sample(minecraft.level, entity.getEyePosition(), minecraft.player);
            float[] rgb = outlineColor(sample);
            if (rgb == null) {
                continue;
            }
            AABB box = entity.getBoundingBox().inflate(0.05);
            LevelRenderer.renderLineBox(poseStack, lines, box, rgb[0], rgb[1], rgb[2], 1.0f);
        }

        poseStack.popPose();
        buffers.endBatch(RenderType.lines());
    }

  /** @return RGB in 0–1, or null if no outline (neutral Φ — particles only). */
    private static float[] outlineColor(PhiSample sample) {
        if (sample.zeroFlux()) {
            return new float[] {0.75f, 0.2f, 0.9f};
        }
        if (sample.value() >= 1.05f) {
            return new float[] {0.35f, 0.95f, 1.0f};
        }
        if (sample.value() <= 0.55f) {
            return new float[] {0.95f, 0.45f, 0.2f};
        }
        return null;
    }
}
