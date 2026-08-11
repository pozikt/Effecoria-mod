package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.effecoria.block.PhiTurretBlockEntity;
import com.effecoria.block.TurretMountBlock;
import com.effecoria.core.alchemy.TurretAssembly;
import com.effecoria.core.alchemy.TurretKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;

import org.joml.Matrix4f;

/**
 * Assembled 2-block turret: fixed mount plate + yaw/pitch barrel assembly.
 * Local barrel space: +Z is muzzle forward.
 * <p>Only one {@link VertexConsumer} may be active at a time (1.21 buffer builder).
 */
public final class TurretMountRenderer implements BlockEntityRenderer<PhiTurretBlockEntity> {
    private static final int OVERLAY = OverlayTexture.NO_OVERLAY;

    public TurretMountRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            PhiTurretBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof TurretMountBlock) || !state.getValue(TurretMountBlock.FORMED)) {
            return;
        }
        TurretKind kind = state.getValue(TurretMountBlock.KIND);
        if (!kind.isEmitter()) {
            return;
        }
        boolean lit = state.getValue(TurretMountBlock.LIT);
        String base = kind.getSerializedName();
        ResourceLocation metalTex = tex(base + "_turret_hull" + (lit ? "_on" : ""));
        ResourceLocation accentTex = tex(base + "_turret_accent" + (lit ? "_on" : ""));
        ResourceLocation mountTex = tex("turret_mount");

        Direction out = TurretAssembly.barrelDirection(state);
        AttachFace face = state.getValue(TurretMountBlock.FACE);
        float yaw = be.getClientAimYaw(partialTick);
        float pitch = be.getClientAimPitch(partialTick);

        // 1) Mount plate — finish before next getBuffer
        {
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(mountTex));
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            orientMountPlate(poseStack, face, state.getValue(TurretMountBlock.FACING));
            drawBox(consumer, poseStack, -0.48f, -0.42f, -0.48f, 0.48f, -0.12f, 0.48f, packedLight);
            drawBox(consumer, poseStack, -0.34f, -0.12f, -0.34f, 0.34f, 0.05f, 0.34f, packedLight);
            drawBox(consumer, poseStack, -0.16f, 0.05f, -0.16f, 0.16f, 0.14f, 0.16f, packedLight);
            poseStack.popPose();
        }

        // 2) Metal yoke + barrel
        {
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(metalTex));
            poseStack.pushPose();
            poseStack.translate(
                    0.5 + out.getStepX() * 0.5,
                    0.5 + out.getStepY() * 0.5,
                    0.5 + out.getStepZ() * 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

            float breech = be.getBreechProgress(partialTick);
            // Side rails kick rearward (-Z) on shot.
            float railZ = breech * -0.28f;

            drawBox(consumer, poseStack, -0.32f, -0.22f, -0.22f, 0.32f, 0.22f, 0.22f, packedLight);
            drawBox(consumer, poseStack, -0.42f, -0.08f, -0.08f, -0.32f, 0.08f, 0.08f, packedLight);
            drawBox(consumer, poseStack, 0.32f, -0.08f, -0.08f, 0.42f, 0.08f, 0.08f, packedLight);
            drawBox(consumer, poseStack, -0.22f, -0.22f, -0.35f, 0.22f, 0.22f, 0.05f, packedLight);
            drawBox(consumer, poseStack, -0.14f, -0.14f, 0.05f, 0.14f, 0.14f, 1.05f, packedLight);
            drawBox(consumer, poseStack, -0.18f, -0.18f, 1.05f, 0.18f, 0.18f, 1.18f, packedLight);
            drawBox(consumer, poseStack, -0.04f, 0.14f, 0.2f, 0.04f, 0.22f, 0.7f, packedLight);
            // Breech / heatsink rails — retract on fire
            drawBox(
                    consumer,
                    poseStack,
                    -0.24f,
                    -0.07f,
                    0.22f + railZ,
                    -0.14f,
                    0.07f,
                    0.88f + railZ,
                    packedLight);
            drawBox(
                    consumer,
                    poseStack,
                    0.14f,
                    -0.07f,
                    0.22f + railZ,
                    0.24f,
                    0.07f,
                    0.88f + railZ,
                    packedLight);
            // Small breech block behind yoke also kicks back
            drawBox(
                    consumer,
                    poseStack,
                    -0.12f,
                    -0.12f,
                    -0.42f + railZ * 0.6f,
                    0.12f,
                    0.12f,
                    -0.28f + railZ * 0.6f,
                    packedLight);
            poseStack.popPose();
        }

        // 3) Accent core
        {
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(accentTex));
            poseStack.pushPose();
            poseStack.translate(
                    0.5 + out.getStepX() * 0.5,
                    0.5 + out.getStepY() * 0.5,
                    0.5 + out.getStepZ() * 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            drawBox(consumer, poseStack, -0.08f, -0.08f, 0.15f, 0.08f, 0.08f, 0.95f, packedLight);
            poseStack.popPose();
        }
    }

    private static ResourceLocation tex(String path) {
        return ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "textures/block/" + path + ".png");
    }

    /** Local -Y sits against the support (floor / ceiling / wall). */
    private static void orientMountPlate(PoseStack poseStack, AttachFace face, Direction facing) {
        switch (face) {
            case FLOOR -> {}
            case CEILING -> poseStack.mulPose(Axis.XP.rotationDegrees(180));
            case WALL -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
            }
        }
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
            int light) {
        Matrix4f mat = poseStack.last().pose();
        PoseStack.Pose pose = poseStack.last();
        quad(consumer, pose, mat, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, 0, -1, 0, light);
        quad(consumer, pose, mat, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0, light);
        quad(consumer, pose, mat, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0, 0, 0, -1, light);
        quad(consumer, pose, mat, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1, 0, 0, 1, light);
        quad(consumer, pose, mat, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1, -1, 0, 0, light);
        quad(consumer, pose, mat, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0, 1, 0, 0, light);
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
            int light) {
        vert(consumer, pose, mat, x0, y0, z0, 0f, 0f, nx, ny, nz, light);
        vert(consumer, pose, mat, x1, y1, z1, 0f, 1f, nx, ny, nz, light);
        vert(consumer, pose, mat, x2, y2, z2, 1f, 1f, nx, ny, nz, light);
        vert(consumer, pose, mat, x3, y3, z3, 1f, 0f, nx, ny, nz, light);
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
            int light) {
        consumer.addVertex(mat, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    @Override
    public boolean shouldRenderOffScreen(PhiTurretBlockEntity be) {
        return be.getBlockState().hasProperty(TurretMountBlock.FORMED)
                && be.getBlockState().getValue(TurretMountBlock.FORMED);
    }

    @Override
    public AABB getRenderBoundingBox(PhiTurretBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(2.0);
    }
}
