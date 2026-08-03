package com.effecoria.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.effecoria.EffecoriaMod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Slanted lightning arcs from cast hand to strike point (not sky-down vanilla bolts).
 */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class LightningArcClient {
    private static final List<Arc> ARCS = new ArrayList<>();

    private LightningArcClient() {}

    public static void trigger(
            double x0, double y0, double z0, double x1, double y1, double z1, float intensity, int durationTicks) {
        Vec3 from = new Vec3(x0, y0, z0);
        Vec3 to = new Vec3(x1, y1, z1);
        float clamped = Mth.clamp(intensity, 0.35f, 1.75f);
        int life = Math.max(4, durationTicks);
        long seed = Double.doubleToLongBits(x0 * 12.9898 + y0 * 78.233 + z1 * 37.719);
        ARCS.add(new Arc(from, to, clamped, life, life, seed));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Iterator<Arc> it = ARCS.iterator();
        while (it.hasNext()) {
            Arc arc = it.next();
            arc.remaining--;
            if (arc.remaining <= 0) {
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || ARCS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lightning = buffers.getBuffer(RenderType.lightning());

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        var matrix = poseStack.last().pose();

        for (Arc arc : ARCS) {
            float life = arc.remaining / (float) Math.max(1, arc.total);
            float alpha = Mth.clamp(life * 1.4f, 0f, 1f) * (0.55f + 0.45f * arc.intensity);
            List<Vec3> path = buildJaggedPath(arc.from, arc.to, arc.seed ^ (arc.remaining * 31L), arc.intensity);
            drawBranch(lightning, matrix, path, 0.045f * arc.intensity, 0.55f, 0.75f, 1f, alpha);
            drawBranch(lightning, matrix, path, 0.018f * arc.intensity, 0.85f, 0.95f, 1f, alpha);
            // Side forks
            if (path.size() >= 4) {
                int forkAt = 1 + (int) ((path.size() - 2) * ((arc.seed & 7) / 7.0));
                Vec3 forkStart = path.get(Mth.clamp(forkAt, 1, path.size() - 2));
                Vec3 along = arc.to.subtract(arc.from);
                Vec3 side = along.cross(new Vec3(0, 1, 0));
                if (side.lengthSqr() < 1.0e-6) {
                    side = along.cross(new Vec3(1, 0, 0));
                }
                side = side.normalize().scale(0.6 + 0.5 * arc.intensity);
                Vec3 forkEnd = forkStart.add(side).add(along.normalize().scale(0.8));
                List<Vec3> fork = buildJaggedPath(forkStart, forkEnd, arc.seed + 99, arc.intensity * 0.7f);
                drawBranch(lightning, matrix, fork, 0.02f * arc.intensity, 0.65f, 0.85f, 1f, alpha * 0.75f);
            }
        }

        poseStack.popPose();
        buffers.endBatch(RenderType.lightning());
    }

    private static List<Vec3> buildJaggedPath(Vec3 from, Vec3 to, long seed, float intensity) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 0.05) {
            return List.of(from, to);
        }
        Vec3 dir = delta.scale(1.0 / length);
        Vec3 up = Math.abs(dir.y) > 0.92 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = dir.cross(up).normalize();
        Vec3 bitangent = right.cross(dir).normalize();

        int segments = Mth.clamp(4 + (int) (length * 1.8), 5, 28);
        Random random = new Random(seed);
        List<Vec3> points = new ArrayList<>(segments + 1);
        points.add(from);
        double wander = 0.12 + 0.18 * intensity;
        for (int i = 1; i < segments; i++) {
            double t = i / (double) segments;
            // Soft envelope — less jitter near ends so it reads as hand → target.
            double envelope = Math.sin(Math.PI * t);
            double ox = (random.nextDouble() - 0.5) * 2.0 * wander * envelope * length * 0.08;
            double oy = (random.nextDouble() - 0.5) * 2.0 * wander * envelope * length * 0.08;
            Vec3 p = from.add(dir.scale(length * t)).add(right.scale(ox)).add(bitangent.scale(oy));
            points.add(p);
        }
        points.add(to);
        return points;
    }

    private static void drawBranch(
            VertexConsumer consumer,
            org.joml.Matrix4f matrix,
            List<Vec3> path,
            float halfWidth,
            float r,
            float g,
            float b,
            float a) {
        if (path.size() < 2) {
            return;
        }
        for (int i = 0; i < path.size() - 1; i++) {
            Vec3 aPos = path.get(i);
            Vec3 bPos = path.get(i + 1);
            Vec3 along = bPos.subtract(aPos);
            if (along.lengthSqr() < 1.0e-8) {
                continue;
            }
            Vec3 side = along.normalize().cross(new Vec3(0, 1, 0));
            if (side.lengthSqr() < 1.0e-6) {
                side = along.normalize().cross(new Vec3(1, 0, 0));
            }
            side = side.normalize().scale(halfWidth);
            Vec3 p0 = aPos.add(side);
            Vec3 p1 = aPos.subtract(side);
            Vec3 p2 = bPos.subtract(side);
            Vec3 p3 = bPos.add(side);
            put(consumer, matrix, p0, r, g, b, a);
            put(consumer, matrix, p1, r, g, b, a);
            put(consumer, matrix, p2, r, g, b, a);
            put(consumer, matrix, p3, r, g, b, a);

            Vec3 core = side.scale(0.35);
            put(consumer, matrix, aPos.add(core), 1f, 1f, 1f, a);
            put(consumer, matrix, aPos.subtract(core), 1f, 1f, 1f, a);
            put(consumer, matrix, bPos.subtract(core), 1f, 1f, 1f, a);
            put(consumer, matrix, bPos.add(core), 1f, 1f, 1f, a);
        }
    }

    private static void put(
            VertexConsumer consumer, org.joml.Matrix4f matrix, Vec3 p, float r, float g, float b, float a) {
        consumer.addVertex(matrix, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, a);
    }

    private static final class Arc {
        final Vec3 from;
        final Vec3 to;
        final float intensity;
        final int total;
        final long seed;
        int remaining;

        Arc(Vec3 from, Vec3 to, float intensity, int remaining, int total, long seed) {
            this.from = from;
            this.to = to;
            this.intensity = intensity;
            this.remaining = remaining;
            this.total = total;
            this.seed = seed;
        }
    }
}
