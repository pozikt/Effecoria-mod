package com.effecoria.client.render;

import com.effecoria.block.SubspacePortalBlock;
import com.effecoria.block.SubspacePortalBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

import org.joml.Matrix4f;

/**
 * Two-block puncture: solid dark void with stars and soft chromatic edge warp.
 * Drawn via Tesselator/POSITION_COLOR so Veil/Sodium cannot leave the buffer "Not building".
 */
public final class SubspacePortalRenderer implements BlockEntityRenderer<SubspacePortalBlockEntity> {
    private static final int SEGMENTS = 40;
    private static final int STAR_COUNT = 48;

    public SubspacePortalRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            SubspacePortalBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof SubspacePortalBlock)
                || state.getValue(SubspacePortalBlock.HALF) != DoubleBlockHalf.LOWER) {
            return;
        }

        Direction facing = state.getValue(SubspacePortalBlock.FACING);
        float time = (be.getLevel() != null ? be.getLevel().getGameTime() : 0) + partialTick;
        float rx = 0.56f;
        float ry = 1.04f;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.translate(0.0, 1.0, 0.0);
        Matrix4f mat = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // Solid void
        BufferBuilder voidBb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        drawVoidDisc(voidBb, mat, rx, ry, 0.02f, 4, 1, 10, 245);
        drawVoidDisc(voidBb, mat, rx, ry, -0.02f, 4, 1, 10, 245);
        BufferUploader.drawWithShader(voidBb.buildOrThrow());

        // Soft edge distortion
        BufferBuilder edgeBb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        drawEdgeDistortion(edgeBb, mat, time, rx, ry);
        BufferUploader.drawWithShader(edgeBb.buildOrThrow());

        // Stars (additive-ish via bright translucent verts)
        BufferBuilder starBb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        drawStars(starBb, mat, be.getBlockPos(), time, rx, ry);
        BufferUploader.drawWithShader(starBb.buildOrThrow());

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void drawVoidDisc(
            BufferBuilder buf, Matrix4f mat, float rx, float ry, float z, int r, int g, int b, int a) {
        for (int side = 0; side < 2; side++) {
            float zz = side == 0 ? z : -z;
            for (int i = 0; i < SEGMENTS; i++) {
                float a0 = (float) (i * Math.PI * 2.0 / SEGMENTS);
                float a1 = (float) ((i + 1) * Math.PI * 2.0 / SEGMENTS);
                float x0 = Mth.cos(a0) * rx;
                float y0 = Mth.sin(a0) * ry;
                float x1 = Mth.cos(a1) * rx;
                float y1 = Mth.sin(a1) * ry;
                if (side == 0) {
                    vert(buf, mat, 0, 0, zz, r, g, b, a);
                    vert(buf, mat, x0, y0, zz, r, g, b, a);
                    vert(buf, mat, x1, y1, zz, r, g, b, a);
                } else {
                    vert(buf, mat, 0, 0, zz, r, g, b, a);
                    vert(buf, mat, x1, y1, zz, r, g, b, a);
                    vert(buf, mat, x0, y0, zz, r, g, b, a);
                }
            }
        }
    }

    private static void drawEdgeDistortion(BufferBuilder buf, Matrix4f mat, float time, float rx, float ry) {
        drawWarpBand(buf, mat, time, rx, ry, 0.00f, 0.07f, 0.18f, 90, 40, 160, 90);
        drawWarpBand(buf, mat, time + 11f, rx, ry, 0.04f, 0.11f, -0.16f, 40, 160, 220, 80);
        drawWarpBand(buf, mat, time + 23f, rx, ry, 0.08f, 0.16f, 0.12f, 180, 200, 255, 55);
    }

    private static void drawWarpBand(
            BufferBuilder buf,
            Matrix4f mat,
            float time,
            float rx,
            float ry,
            float inset,
            float width,
            float phase,
            int r,
            int g,
            int b,
            int a) {
        for (int i = 0; i < SEGMENTS; i++) {
            float t0 = i / (float) SEGMENTS;
            float t1 = (i + 1) / (float) SEGMENTS;
            float warp0 = 1f
                    + 0.07f * Mth.sin(t0 * 21f + time * 0.19f + phase)
                    + 0.04f * Mth.sin(t0 * 47f - time * 0.11f);
            float warp1 = 1f
                    + 0.07f * Mth.sin(t1 * 21f + time * 0.19f + phase)
                    + 0.04f * Mth.sin(t1 * 47f - time * 0.11f);
            float a0 = t0 * Mth.TWO_PI;
            float a1 = t1 * Mth.TWO_PI;
            float inner = 1f - inset;
            float outer = inner + width;
            float ix0 = Mth.cos(a0) * rx * inner * warp0;
            float iy0 = Mth.sin(a0) * ry * inner * warp0;
            float ix1 = Mth.cos(a1) * rx * inner * warp1;
            float iy1 = Mth.sin(a1) * ry * inner * warp1;
            float ox0 = Mth.cos(a0) * rx * outer * warp0;
            float oy0 = Mth.sin(a0) * ry * outer * warp0;
            float ox1 = Mth.cos(a1) * rx * outer * warp1;
            float oy1 = Mth.sin(a1) * ry * outer * warp1;
            float z = 0.035f + 0.01f * Mth.sin(time * 0.2f + t0 * 9f);

            for (float zs : new float[] {z, -z}) {
                vert(buf, mat, ix0, iy0, zs, r, g, b, a);
                vert(buf, mat, ox0, oy0, zs, r, g, b, a / 5);
                vert(buf, mat, ox1, oy1, zs, r, g, b, a / 5);
                vert(buf, mat, ix0, iy0, zs, r, g, b, a);
                vert(buf, mat, ox1, oy1, zs, r, g, b, a / 5);
                vert(buf, mat, ix1, iy1, zs, r, g, b, a);
            }
        }
    }

    private static void drawStars(
            BufferBuilder buf, Matrix4f mat, BlockPos seedPos, float time, float rx, float ry) {
        RandomSource rng = RandomSource.create(
                BlockPos.asLong(seedPos.getX(), seedPos.getY(), seedPos.getZ()) ^ 0x51F15EEDL);
        for (int i = 0; i < STAR_COUNT; i++) {
            float px;
            float py;
            int guard = 0;
            do {
                px = (rng.nextFloat() * 2f - 1f) * rx * 0.92f;
                py = (rng.nextFloat() * 2f - 1f) * ry * 0.92f;
                guard++;
            } while ((px * px) / (rx * rx) + (py * py) / (ry * ry) > 0.85f && guard < 8);

            float twinkle = 0.55f + 0.45f * Mth.sin(time * (0.15f + (i % 7) * 0.04f) + i * 1.7f);
            float size = (0.012f + (i % 5) * 0.004f) * (0.75f + 0.5f * twinkle);
            int bright = (int) (180 + 75 * twinkle);
            int cr = bright;
            int cg = bright - (i % 3) * 12;
            int cb = Math.min(255, bright + (i % 4) * 10);
            float z = ((i & 1) == 0 ? 0.025f : -0.025f) + 0.008f * Mth.sin(time * 0.08f + i);
            drawStarQuad(buf, mat, px, py, z, size, cr, cg, cb, (int) (200 * twinkle + 40));
        }
    }

    private static void drawStarQuad(
            BufferBuilder buf, Matrix4f mat, float x, float y, float z, float s, int r, int g, int b, int a) {
        vert(buf, mat, x - s, y, z, r, g, b, a);
        vert(buf, mat, x + s, y, z, r, g, b, a);
        vert(buf, mat, x, y + s * 0.35f, z, r, g, b, a / 2);

        vert(buf, mat, x, y - s, z, r, g, b, a);
        vert(buf, mat, x, y + s, z, r, g, b, a);
        vert(buf, mat, x + s * 0.35f, y, z, r, g, b, a / 2);

        float c = s * 0.45f;
        vert(buf, mat, x - c, y - c, z, 255, 255, 255, a);
        vert(buf, mat, x + c, y - c, z, 255, 255, 255, a);
        vert(buf, mat, x, y + c, z, 255, 255, 255, a);
    }

    private static void vert(BufferBuilder buf, Matrix4f mat, float x, float y, float z, int r, int g, int b, int a) {
        buf.addVertex(mat, x, y, z).setColor(r, g, b, Math.max(0, Math.min(255, a)));
    }

    @Override
    public boolean shouldRenderOffScreen(SubspacePortalBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public AABB getRenderBoundingBox(SubspacePortalBlockEntity be) {
        return new AABB(be.getBlockPos()).expandTowards(0, 1, 0).inflate(1.0);
    }
}
