package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.effecoria.block.PhiArtilleryBaseBlock;
import com.effecoria.block.PhiArtilleryBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import org.joml.Matrix4f;

/**
 * Formed Φ-artillery: yaw/pitch barrel + lens, plus a cyan thermal beam when firing.
 * Local space: +Z is muzzle forward (matches {@link PhiArtilleryBlockEntity} aim).
 */
public final class PhiArtilleryRenderer implements BlockEntityRenderer<PhiArtilleryBlockEntity> {
    private static final int OVERLAY = OverlayTexture.NO_OVERLAY;
    private static final ResourceLocation HULL =
            ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "textures/block/phi_artillery_base.png");
    private static final ResourceLocation LENS =
            ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "textures/block/phi_beam_lens.png");
    private static final ResourceLocation LENS_ON =
            ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "textures/block/phi_beam_lens_on.png");
    private static final ResourceLocation BEAM =
            ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "textures/block/phi_beam_lens_on.png");

    public PhiArtilleryRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            PhiArtilleryBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof PhiArtilleryBaseBlock) || !state.getValue(PhiArtilleryBaseBlock.FORMED)) {
            return;
        }
        boolean lit = be.beamActive();
        float yaw = be.yaw();
        float pitch = be.pitch();

        poseStack.pushPose();
        poseStack.translate(0.5, 1.15, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        {
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(HULL));
            drawBox(consumer, poseStack, -0.28f, -0.18f, -0.35f, 0.28f, 0.18f, 0.15f, packedLight, 255);
            drawBox(consumer, poseStack, -0.16f, -0.16f, 0.15f, 0.16f, 0.16f, 0.95f, packedLight, 255);
            drawBox(consumer, poseStack, -0.22f, -0.22f, 0.95f, 0.22f, 0.22f, 1.12f, packedLight, 255);
        }
        {
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(lit ? LENS_ON : LENS));
            drawBox(consumer, poseStack, -0.12f, -0.12f, 0.25f, 0.12f, 0.12f, 1.05f, packedLight, 255);
            drawBox(consumer, poseStack, -0.18f, -0.18f, 1.05f, 0.18f, 0.18f, 1.22f, packedLight, 255);
        }

        if (lit) {
            float reach = Math.max(4f, be.beamReach());
            float pulse = 0.85f + 0.15f * Mth.sin((be.getLevel() != null ? be.getLevel().getGameTime() : 0) * 0.45f + partialTick);
            int fullBright = 0xF000F0;
            VertexConsumer beam = buffer.getBuffer(RenderType.entityTranslucent(BEAM));
            // Outer glow
            drawBox(beam, poseStack, -0.18f * pulse, -0.18f * pulse, 1.15f, 0.18f * pulse, 0.18f * pulse, 1.15f + reach, fullBright, 90);
            // Core
            drawBox(beam, poseStack, -0.07f, -0.07f, 1.15f, 0.07f, 0.07f, 1.15f + reach, fullBright, 210);
            // Hot tip
            drawBox(beam, poseStack, -0.12f, -0.12f, 1.15f + reach - 0.35f, 0.12f, 0.12f, 1.15f + reach + 0.15f, fullBright, 255);
        }

        poseStack.popPose();
    }

    private static void drawBox(
            VertexConsumer consumer,
            PoseStack poseStack,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            int light,
            int alpha) {
        Matrix4f mat = poseStack.last().pose();
        PoseStack.Pose pose = poseStack.last();
        quad(consumer, pose, mat, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, 0, -1, 0, light, alpha);
        quad(consumer, pose, mat, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0, light, alpha);
        quad(consumer, pose, mat, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0, 0, 0, -1, light, alpha);
        quad(consumer, pose, mat, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1, 0, 0, 1, light, alpha);
        quad(consumer, pose, mat, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1, -1, 0, 0, light, alpha);
        quad(consumer, pose, mat, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0, 1, 0, 0, light, alpha);
    }

    private static void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Matrix4f mat,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float nx,
            float ny,
            float nz,
            int light,
            int alpha) {
        vert(consumer, pose, mat, x0, y0, z0, 0f, 0f, nx, ny, nz, light, alpha);
        vert(consumer, pose, mat, x1, y1, z1, 0f, 1f, nx, ny, nz, light, alpha);
        vert(consumer, pose, mat, x2, y2, z2, 1f, 1f, nx, ny, nz, light, alpha);
        vert(consumer, pose, mat, x3, y3, z3, 1f, 0f, nx, ny, nz, light, alpha);
    }

    private static void vert(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Matrix4f mat,
            float x,
            float y,
            float z,
            float u,
            float v,
            float nx,
            float ny,
            float nz,
            int light,
            int alpha) {
        consumer.addVertex(mat, x, y, z)
                .setColor(120, 210, 255, alpha)
                .setUv(u, v)
                .setOverlay(OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    @Override
    public boolean shouldRenderOffScreen(PhiArtilleryBlockEntity be) {
        return be.getBlockState().hasProperty(PhiArtilleryBaseBlock.FORMED)
                && be.getBlockState().getValue(PhiArtilleryBaseBlock.FORMED);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(PhiArtilleryBlockEntity be) {
        float reach = Math.max(2.5f, be.beamReach() + 4f);
        return new AABB(be.getBlockPos()).inflate(reach);
    }
}
