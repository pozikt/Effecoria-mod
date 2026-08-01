package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModParticleTypes;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.platform.VeilEventPlatform;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * World-anchored dimensional cuts: Veil post projects from/to into screen space so every
 * nearby client sees the same seam in the world, plus dense particle arcs.
 */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class SpatialCutClient {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final ResourceLocation PIPELINE = EffecoriaMod.id("spatial_cut");
    public static final ResourceLocation SHADER = EffecoriaMod.id("spatial_cut");

    /** Line segment caster→target (or yank path). */
    public static final int MODE_LINE = 0;
    /** Radial Judgement cuts around a focus. */
    public static final int MODE_AROUND = 1;

    private static Vec3 worldFrom = Vec3.ZERO;
    private static Vec3 worldTo = Vec3.ZERO;
    private static float intensity;
    private static float slashSeed;
    private static int cutMode;
    private static int slashCount = 3;
    private static int remainingTicks;
    private static int totalTicks = 1;
    private static float timeSeconds;
    private static boolean pipelineActive;
    private static boolean loggedIrisSkip;
    private static boolean eventsHooked;

    private SpatialCutClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(SpatialCutClient::hookVeilEvents);
    }

    private static void hookVeilEvents() {
        if (eventsHooked) {
            return;
        }
        eventsHooked = true;
        VeilEventPlatform.INSTANCE.preVeilPostProcessing((pipelineName, pipeline, context) -> {
            if (!PIPELINE.equals(pipelineName) || !isActive()) {
                return;
            }
            ShaderProgram shader = context.getShader(SHADER);
            if (shader == null || !shader.isValid()) {
                return;
            }
            Projected a = projectToScreen(worldFrom);
            Projected b = projectToScreen(worldTo);
            boolean behind = cutMode == MODE_AROUND ? b.behind : (a.behind && b.behind);
            float progress = pulseProgress();
            shader.getUniformSafe("Intensity").setFloat(intensity);
            shader.getUniformSafe("Progress").setFloat(progress);
            shader.getUniformSafe("Time").setFloat(timeSeconds);
            shader.getUniformSafe("SlashSeed").setFloat(slashSeed);
            shader.getUniformSafe("CutMode").setFloat(cutMode);
            shader.getUniformSafe("FromUV").setVector(a.u, a.v);
            shader.getUniformSafe("ToUV").setVector(b.u, b.v);
            shader.getUniformSafe("FromDepth").setFloat(a.depth);
            shader.getUniformSafe("ToDepth").setFloat(b.depth);
            shader.getUniformSafe("BehindCamera").setFloat(behind ? 1f : 0f);
        });
        LOGGER.info("Spatial cut Veil hooks registered");
    }

    public static void trigger(
            double x0,
            double y0,
            double z0,
            double x1,
            double y1,
            double z1,
            float intensityIn,
            int slashCountIn,
            int mode) {
        worldFrom = new Vec3(x0, y0, z0);
        worldTo = new Vec3(x1, y1, z1);
        cutMode = mode == MODE_AROUND ? MODE_AROUND : MODE_LINE;
        slashCount = Mth.clamp(slashCountIn, 1, 6);
        intensity = Mth.clamp(intensityIn, 0.35f, 1.5f);
        slashSeed = (float) ((x0 * 12.9898 + y0 * 78.233 + z1 * 37.719) % 1000.0);

        spawnWorldArcs(worldFrom, worldTo, slashCount, intensity, cutMode);

        if (isIrisShaderPackInUse()) {
            if (!loggedIrisSkip) {
                LOGGER.info("Spatial cut PostChain skipped — Iris/Oculus shaderpack is active");
                loggedIrisSkip = true;
            }
            return;
        }

        totalTicks = Math.max(10, 8 + Math.round(intensity * 8f));
        remainingTicks = totalTicks;
        timeSeconds = 0f;
        hookVeilEvents();
        ensurePipeline(true);
    }

    public static boolean isActive() {
        return remainingTicks > 0;
    }

    public static float pulseProgress() {
        if (!isActive()) {
            return 0f;
        }
        float life = 1f - (remainingTicks / (float) totalTicks);
        // Sharp strike: peak early, hold briefly, fade
        float t = Mth.clamp(life, 0f, 1f);
        if (t < 0.2f) {
            return t / 0.2f;
        }
        if (t < 0.45f) {
            return 1f;
        }
        return 1f - (t - 0.45f) / 0.55f;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (remainingTicks <= 0) {
            if (pipelineActive) {
                ensurePipeline(false);
            }
            return;
        }
        remainingTicks--;
        timeSeconds += 0.05f;
        if (remainingTicks <= 0) {
            ensurePipeline(false);
        }
    }

    private static void spawnWorldArcs(Vec3 from, Vec3 to, int count, float intensityIn, int mode) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        RandomSource random = level.random;
        if (mode == MODE_AROUND) {
            spawnAroundCuts(level, random, to, count, intensityIn);
        } else {
            spawnLineCuts(level, random, from, to, count, intensityIn);
        }
    }

    private static void spawnLineCuts(
            ClientLevel level, RandomSource random, Vec3 from, Vec3 to, int count, float intensityIn) {
        Vec3 delta = to.subtract(from);
        double len = delta.length();
        if (len < 0.2) {
            delta = new Vec3(0, 0, 1);
            len = 1;
        }
        Vec3 dir = delta.scale(1.0 / len);
        Vec3 ortho = dir.cross(new Vec3(0, 1, 0));
        if (ortho.lengthSqr() < 1e-4) {
            ortho = dir.cross(new Vec3(1, 0, 0));
        }
        ortho = ortho.normalize();
        Vec3 ortho2 = dir.cross(ortho).normalize();

        int steps = Mth.clamp((int) (len * 12), 12, 64);
        for (int s = 0; s < count; s++) {
            double offset = (s - (count - 1) * 0.5) * (0.22 + 0.14 * intensityIn);
            double twist = (random.nextDouble() - 0.5) * 0.4;
            Vec3 side = ortho.scale(offset).add(ortho2.scale(twist * Math.abs(offset)));
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                Vec3 p = from.lerp(to, t).add(side);
                double speed = 0.22 + 0.1 * intensityIn;
                level.addParticle(
                        ModParticleTypes.SPATIAL_RIFT.get(),
                        p.x,
                        p.y,
                        p.z,
                        dir.x * speed,
                        dir.y * speed,
                        dir.z * speed);
                if (i % 2 == 0) {
                    level.addParticle(
                            ModParticleTypes.SPATIAL_WARP.get(),
                            p.x,
                            p.y,
                            p.z,
                            -dir.x * 0.1 + (random.nextDouble() - 0.5) * 0.05,
                            (random.nextDouble() - 0.5) * 0.05,
                            -dir.z * 0.1 + (random.nextDouble() - 0.5) * 0.05);
                }
                if (i % 4 == 0) {
                    level.addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, 0, 0.01, 0);
                }
            }
        }
        // Impact bloom at the tip — readable from any angle
        for (int i = 0; i < 10 + Math.round(intensityIn * 8); i++) {
            level.addParticle(
                    ModParticleTypes.SPATIAL_RIFT.get(),
                    to.x + (random.nextDouble() - 0.5) * 0.6,
                    to.y + (random.nextDouble() - 0.5) * 0.6,
                    to.z + (random.nextDouble() - 0.5) * 0.6,
                    (random.nextDouble() - 0.5) * 0.2,
                    (random.nextDouble() - 0.5) * 0.2,
                    (random.nextDouble() - 0.5) * 0.2);
        }
    }

    private static void spawnAroundCuts(
            ClientLevel level, RandomSource random, Vec3 center, int count, float intensityIn) {
        double radius = 1.1 + intensityIn * 0.9;
        int cuts = Math.max(3, count);
        for (int s = 0; s < cuts; s++) {
            double ang = (Math.PI * 2.0 * s) / cuts + (random.nextDouble() - 0.5) * 0.35;
            double pitch = (random.nextDouble() - 0.5) * 0.7;
            Vec3 dir = new Vec3(Math.cos(ang) * Math.cos(pitch), Math.sin(pitch), Math.sin(ang) * Math.cos(pitch))
                    .normalize();
            Vec3 a = center.subtract(dir.scale(radius));
            Vec3 b = center.add(dir.scale(radius));
            int steps = Mth.clamp((int) (radius * 2 * 14), 14, 48);
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                Vec3 p = a.lerp(b, t);
                level.addParticle(
                        ModParticleTypes.SPATIAL_RIFT.get(),
                        p.x,
                        p.y,
                        p.z,
                        dir.x * 0.18,
                        dir.y * 0.18,
                        dir.z * 0.18);
                if (i % 2 == 0) {
                    level.addParticle(
                            ModParticleTypes.SPATIAL_WARP.get(),
                            p.x,
                            p.y,
                            p.z,
                            (random.nextDouble() - 0.5) * 0.08,
                            (random.nextDouble() - 0.5) * 0.08,
                            (random.nextDouble() - 0.5) * 0.08);
                }
            }
        }
        for (int i = 0; i < 16; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = random.nextDouble() * radius * 0.5;
            level.addParticle(
                    ParticleTypes.END_ROD,
                    center.x + Math.cos(a) * r,
                    center.y + (random.nextDouble() - 0.5) * 0.8,
                    center.z + Math.sin(a) * r,
                    0,
                    0.02,
                    0);
        }
    }

    private static void ensurePipeline(boolean enable) {
        try {
            PostProcessingManager manager = VeilRenderSystem.renderer().getPostProcessingManager();
            if (enable) {
                if (!manager.isActive(PIPELINE)) {
                    manager.add(PIPELINE);
                }
                pipelineActive = true;
            } else {
                if (manager.isActive(PIPELINE)) {
                    manager.remove(PIPELINE);
                }
                pipelineActive = false;
            }
        } catch (Throwable t) {
            LOGGER.warn("Failed to toggle spatial cut pipeline", t);
            pipelineActive = false;
            remainingTicks = 0;
        }
    }

    private static Projected projectToScreen(Vec3 world) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 rel = world.subtract(camera.getPosition());

        org.joml.Vector3f local = new org.joml.Vector3f((float) rel.x, (float) rel.y, (float) rel.z);
        camera.rotation().transformInverse(local);

        boolean behind = local.z >= -0.05f;

        Matrix4f proj = new Matrix4f(mc.gameRenderer.getProjectionMatrix(mc.options.fov().get()));
        Vector4f clip = proj.transform(new Vector4f(local.x, local.y, local.z, 1f));
        if (Math.abs(clip.w) < 1e-5f) {
            return new Projected(0.5f, 0.5f, 1f, true);
        }
        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;
        float ndcZ = clip.z / clip.w;
        float u = ndcX * 0.5f + 0.5f;
        float v = ndcY * 0.5f + 0.5f;
        float depth = ndcZ * 0.5f + 0.5f;
        boolean offscreen = u < -0.2f || u > 1.2f || v < -0.2f || v > 1.2f;
        return new Projected(u, v, depth, behind || offscreen);
    }

    private static boolean isIrisShaderPackInUse() {
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Object inUse = apiClass.getMethod("isShaderPackInUse").invoke(api);
            return inUse instanceof Boolean b && b;
        } catch (Throwable ignored) {
            // Iris not present
        }
        try {
            Class<?> iris = Class.forName("net.coderbot.iris.Iris");
            Object config = iris.getMethod("getIrisConfig").invoke(null);
            if (config != null) {
                Object enabled = config.getClass().getMethod("areShadersEnabled").invoke(config);
                return enabled instanceof Boolean b && b;
            }
        } catch (Throwable ignored) {
            // not present
        }
        return false;
    }

    private record Projected(float u, float v, float depth, boolean behind) {}
}
