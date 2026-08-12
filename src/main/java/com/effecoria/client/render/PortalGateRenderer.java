package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.effecoria.block.PortalGateBlockEntity;
import com.effecoria.content.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

import org.joml.Matrix4f;

/**
 * Hyper-mirror membrane: end-portal hyperspace under a continuous shimmer skin.
 * Shared faces between adjacent film cells are culled so the sheet reads as one surface.
 */
public final class PortalGateRenderer implements BlockEntityRenderer<PortalGateBlockEntity> {
    private static final ResourceLocation MIRROR =
            ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "textures/block/hyper_mirror.png");
    /** Slight outward overshoot so neighboring cells fuse without hairline gaps. */
    private static final float PAD = 0.002f;

    public PortalGateRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            PortalGateBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        var level = be.getLevel();
        BlockPos pos = be.getBlockPos();
        float time = (level != null ? level.getGameTime() : 0) + partialTick;
        float pulse = 0.55f + 0.45f * Mth.sin(time * 0.12f);
        float swirl = time * 0.025f;

        poseStack.pushPose();
        Matrix4f mat = poseStack.last().pose();
        PoseStack.Pose pose = poseStack.last();
        int overlay = OverlayTexture.NO_OVERLAY;
        int light = 0x00F000F0;

        float min = -PAD;
        float max = 1f + PAD;

        boolean hideDown = isFilm(level, pos, Direction.DOWN);
        boolean hideUp = isFilm(level, pos, Direction.UP);
        boolean hideNorth = isFilm(level, pos, Direction.NORTH);
        boolean hideSouth = isFilm(level, pos, Direction.SOUTH);
        boolean hideWest = isFilm(level, pos, Direction.WEST);
        boolean hideEast = isFilm(level, pos, Direction.EAST);

        VertexConsumer voidBuf = buffer.getBuffer(RenderType.endPortal());
        if (!hideDown) {
            faceEnd(voidBuf, mat, min, min, min, max, min, min, max, min, max, min, min, max);
        }
        if (!hideUp) {
            faceEnd(voidBuf, mat, min, max, max, max, max, max, max, max, min, min, max, min);
        }
        if (!hideNorth) {
            faceEnd(voidBuf, mat, min, min, min, min, max, min, max, max, min, max, min, min);
        }
        if (!hideSouth) {
            faceEnd(voidBuf, mat, max, min, max, max, max, max, min, max, max, min, min, max);
        }
        if (!hideWest) {
            faceEnd(voidBuf, mat, min, min, max, min, max, max, min, max, min, min, min, min);
        }
        if (!hideEast) {
            faceEnd(voidBuf, mat, max, min, min, max, max, min, max, max, max, max, min, max);
        }

        VertexConsumer skin = buffer.getBuffer(RenderType.entityTranslucent(MIRROR));
        int a = Mth.clamp((int) (110 + 70 * pulse), 90, 200);
        int r = Mth.clamp((int) (35 + 25 * pulse), 25, 80);
        int g = Mth.clamp((int) (80 + 60 * pulse), 60, 170);
        int b = Mth.clamp((int) (150 + 70 * pulse), 130, 255);

        // World-locked UVs so the shimmer tiles continuously across the membrane.
        float uBase = pos.getX() * 0.25f + swirl;
        float vBase = pos.getZ() * 0.25f + swirl * 0.7f;
        float uY = pos.getY() * 0.25f + swirl * 0.5f;

        if (!hideDown) {
            skinFace(skin, pose, mat, min, min, min, max, min, min, max, min, max, min, min, max, 0, -1, 0, uBase, vBase + 1f, uBase + 1f, vBase, r, g, b, a, light, overlay);
        }
        if (!hideUp) {
            skinFace(skin, pose, mat, min, max, max, max, max, max, max, max, min, min, max, min, 0, 1, 0, uBase, vBase, uBase + 1f, vBase + 1f, r, g, b, a, light, overlay);
        }
        if (!hideNorth) {
            skinFace(skin, pose, mat, min, min, min, min, max, min, max, max, min, max, min, min, 0, 0, -1, uBase, uY + 1f, uBase + 1f, uY, r, g, b, a, light, overlay);
        }
        if (!hideSouth) {
            skinFace(skin, pose, mat, max, min, max, max, max, max, min, max, max, min, min, max, 0, 0, 1, uBase, uY, uBase + 1f, uY + 1f, r, g, b, a, light, overlay);
        }
        if (!hideWest) {
            skinFace(skin, pose, mat, min, min, max, min, max, max, min, max, min, min, min, min, -1, 0, 0, vBase, uY + 1f, vBase + 1f, uY, r, g, b, a, light, overlay);
        }
        if (!hideEast) {
            skinFace(skin, pose, mat, max, min, min, max, max, min, max, max, max, max, min, max, 1, 0, 0, vBase, uY, vBase + 1f, uY + 1f, r, g, b, a, light, overlay);
        }

        poseStack.popPose();
    }

    private static boolean isFilm(net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction dir) {
        return level != null && level.getBlockState(pos.relative(dir)).is(ModBlocks.PORTAL_GATE.get());
    }

    private static void faceEnd(
            VertexConsumer c,
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
            float z3) {
        c.addVertex(mat, x0, y0, z0);
        c.addVertex(mat, x1, y1, z1);
        c.addVertex(mat, x2, y2, z2);
        c.addVertex(mat, x3, y3, z3);
    }

    private static void skinFace(
            VertexConsumer c,
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
            float u0,
            float v0,
            float u1,
            float v1,
            int r,
            int g,
            int b,
            int a,
            int light,
            int overlay) {
        vert(c, pose, mat, x0, y0, z0, u0, v0, nx, ny, nz, r, g, b, a, light, overlay);
        vert(c, pose, mat, x1, y1, z1, u0, v1, nx, ny, nz, r, g, b, a, light, overlay);
        vert(c, pose, mat, x2, y2, z2, u1, v1, nx, ny, nz, r, g, b, a, light, overlay);
        vert(c, pose, mat, x3, y3, z3, u1, v0, nx, ny, nz, r, g, b, a, light, overlay);
    }

    private static void vert(
            VertexConsumer c,
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
            int r,
            int g,
            int b,
            int a,
            int light,
            int overlay) {
        c.addVertex(mat, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    @Override
    public boolean shouldRenderOffScreen(PortalGateBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(PortalGateBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(0.05);
    }
}
