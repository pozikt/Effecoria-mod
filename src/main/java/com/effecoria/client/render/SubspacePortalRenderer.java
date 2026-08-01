package com.effecoria.client.render;

import com.effecoria.block.SubspacePortalBlock;
import com.effecoria.block.SubspacePortalBlockEntity;
import com.effecoria.client.SubspacePortalLensClient;
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
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

/**
 * Living puncture: crawling jagged void + starfield. Screen refraction comes from
 * {@link SubspacePortalLensClient} (Veil gravitational lens).
 */
public final class SubspacePortalRenderer implements BlockEntityRenderer<SubspacePortalBlockEntity> {
    private static final int SEGMENTS = 48;
    private static final int STAR_COUNT = 52;

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

        BlockPos pos = be.getBlockPos();
        Vec3 lensCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        SubspacePortalLensClient.present(lensCenter, facing, 1.05f);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.translate(0.0, 1.0, 0.0);
        Matrix4f mat = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder voidBb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        drawLivingVoid(voidBb, mat, time, rx, ry, 0.02f);
        drawLivingVoid(voidBb, mat, time, rx, ry, -0.02f);
        BufferUploader.drawWithShader(voidBb.buildOrThrow());

        BufferBuilder edgeBb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        drawCrawlingRim(edgeBb, mat, time, rx, ry);
        BufferUploader.drawWithShader(edgeBb.buildOrThrow());

        BufferBuilder starBb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        drawStars(starBb, mat, pos, time, rx, ry);
        BufferUploader.drawWithShader(starBb.buildOrThrow());

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    /** Animated jagged silhouette — the tear edge crawls like stressed spacetime. */
    private static float rimScale(float ang, float time) {
        return 1f
                + 0.08f * Mth.sin(ang * 5f + time * 0.31f)
                + 0.055f * Mth.sin(ang * 11f - time * 0.42f)
                + 0.04f * Mth.sin(ang * 17f + time * 0.19f)
                + 0.03f * Mth.sin(ang * 29f + time * 0.55f + 1.7f)
                + 0.02f * Mth.sin(ang * 41f - time * 0.67f);
    }

    private static void drawLivingVoid(
            BufferBuilder buf, Matrix4f mat, float time, float rx, float ry, float z) {
        for (int side = 0; side < 2; side++) {
            float zz = side == 0 ? z : -z;
            for (int i = 0; i < SEGMENTS; i++) {
                float a0 = (float) (i * Math.PI * 2.0 / SEGMENTS);
                float a1 = (float) ((i + 1) * Math.PI * 2.0 / SEGMENTS);
                float s0 = rimScale(a0, time);
                float s1 = rimScale(a1, time);
                float x0 = Mth.cos(a0) * rx * s0;
                float y0 = Mth.sin(a0) * ry * s0;
                float x1 = Mth.cos(a1) * rx * s1;
                float y1 = Mth.sin(a1) * ry * s1;
                if (side == 0) {
                    vert(buf, mat, 0, 0, zz, 3, 1, 8, 250);
                    vert(buf, mat, x0, y0, zz, 3, 1, 8, 250);
                    vert(buf, mat, x1, y1, zz, 3, 1, 8, 250);
                } else {
                    vert(buf, mat, 0, 0, zz, 3, 1, 8, 250);
                    vert(buf, mat, x1, y1, zz, 3, 1, 8, 250);
                    vert(buf, mat, x0, y0, zz, 3, 1, 8, 250);
                }
            }
        }
    }

    private static void drawCrawlingRim(BufferBuilder buf, Matrix4f mat, float time, float rx, float ry) {
        drawWarpBand(buf, mat, time, rx, ry, -0.02f, 0.09f, 0.0f, 70, 30, 140, 70);
        drawWarpBand(buf, mat, time + 9f, rx, ry, 0.03f, 0.12f, 1.1f, 50, 140, 210, 65);
        drawWarpBand(buf, mat, time + 17f, rx, ry, 0.08f, 0.15f, -0.8f, 160, 190, 255, 40);
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
            float a0 = t0 * Mth.TWO_PI;
            float a1 = t1 * Mth.TWO_PI;
            float s0 = rimScale(a0, time + phase);
            float s1 = rimScale(a1, time + phase);
            // Extra crawl on the fringe itself.
            float crawl0 = 1f + 0.04f * Mth.sin(a0 * 13f + time * 0.48f + phase);
            float crawl1 = 1f + 0.04f * Mth.sin(a1 * 13f + time * 0.48f + phase);
            float inner = (1f - inset) * s0 * crawl0;
            float outer = (1f - inset + width) * s0 * crawl0;
            float inner1 = (1f - inset) * s1 * crawl1;
            float outer1 = (1f - inset + width) * s1 * crawl1;
            float ix0 = Mth.cos(a0) * rx * inner;
            float iy0 = Mth.sin(a0) * ry * inner;
            float ix1 = Mth.cos(a1) * rx * inner1;
            float iy1 = Mth.sin(a1) * ry * inner1;
            float ox0 = Mth.cos(a0) * rx * outer;
            float oy0 = Mth.sin(a0) * ry * outer;
            float ox1 = Mth.cos(a1) * rx * outer1;
            float oy1 = Mth.sin(a1) * ry * outer1;
            float z = 0.04f + 0.012f * Mth.sin(time * 0.22f + t0 * 11f);

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
                px = (rng.nextFloat() * 2f - 1f) * rx * 0.88f;
                py = (rng.nextFloat() * 2f - 1f) * ry * 0.88f;
                guard++;
            } while ((px * px) / (rx * rx) + (py * py) / (ry * ry) > 0.75f && guard < 8);

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
        return new AABB(be.getBlockPos()).expandTowards(0, 1, 0).inflate(1.25);
    }
}
