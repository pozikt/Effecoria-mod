package com.effecoria.client.glue;

import com.effecoria.content.ModItems;
import com.effecoria.core.glue.EssenceGlueService;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Axe-style outlines: glued cells, pending pos1, live cuboid preview, last volume box. */
@EventBusSubscriber(modid = com.effecoria.EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class EssenceGlueHighlight {
    private static final double INSET = 0.002;
    private static final double OUTSET = 0.004;

    private EssenceGlueHighlight() {}

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        boolean holdingGlue = mc.player.getMainHandItem().is(ModItems.ESSENCE_GLUE.get())
                || mc.player.getOffhandItem().is(ModItems.ESSENCE_GLUE.get());
        if (!holdingGlue) {
            return;
        }

        BlockPos pending = EssenceGlueClient.pending();
        boolean hasSession = !EssenceGlueClient.session().isEmpty();
        boolean hasGlued = !EssenceGlueClient.glued().isEmpty();
        if (!hasGlued && !hasSession && pending == null) {
            return;
        }

        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        VertexConsumer lines = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());

        for (BlockPos pos : EssenceGlueClient.glued()) {
            if (mc.player.distanceToSqr(Vec3.atCenterOf(pos)) > 96 * 96) {
                continue;
            }
            drawAxeOutline(pose, lines, mc, pos, 1.0f, 0.88f, 0.25f, 0.85f);
        }

        // Last confirmed volume — one outer wireframe (WE-style).
        if (hasSession) {
            AABB sessionBox = boundsOf(EssenceGlueClient.session());
            if (sessionBox != null) {
                LevelRenderer.renderLineBox(pose, lines, sessionBox.inflate(OUTSET), 0.35f, 0.95f, 1.0f, 0.95f);
                LevelRenderer.renderLineBox(pose, lines, sessionBox.deflate(INSET), 0.15f, 0.45f, 0.55f, 0.55f);
            }
        }

        if (pending != null) {
            drawAxeOutline(pose, lines, mc, pending, 0.25f, 1.0f, 0.45f, 1.0f);

            // Live preview while aiming at pos2.
            if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos look = hit.getBlockPos();
                if (!look.equals(pending)) {
                    AABB preview = EssenceGlueService.cuboid(pending, look);
                    LevelRenderer.renderLineBox(pose, lines, preview.inflate(OUTSET), 0.55f, 1.0f, 0.65f, 0.9f);
                    LevelRenderer.renderLineBox(pose, lines, preview.deflate(INSET), 0.2f, 0.55f, 0.3f, 0.45f);
                }
            }
        }

        pose.popPose();
    }

    private static AABB boundsOf(java.util.Set<BlockPos> cells) {
        if (cells.isEmpty()) {
            return null;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos p : cells) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            maxZ = Math.max(maxZ, p.getZ());
        }
        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    private static void drawAxeOutline(
            PoseStack pose,
            VertexConsumer lines,
            Minecraft mc,
            BlockPos pos,
            float r,
            float g,
            float b,
            float a) {
        VoxelShape shape = mc.level.getBlockState(pos).getShape(mc.level, pos);
        if (shape.isEmpty()) {
            shape = Shapes.box(INSET, INSET, INSET, 1 - INSET, 1 - INSET, 1 - INSET);
        }
        AABB box = shape.bounds().move(pos).inflate(OUTSET);
        LevelRenderer.renderLineBox(pose, lines, box, r, g, b, a);
        AABB inner = shape.bounds().move(pos).deflate(INSET);
        LevelRenderer.renderLineBox(pose, lines, inner, r * 0.35f, g * 0.35f, b * 0.35f, a * 0.65f);
    }
}
