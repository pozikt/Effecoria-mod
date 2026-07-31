package com.effecoria.client;

import com.effecoria.EffecoriaMod;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.platform.VeilEventPlatform;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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
 * Veil-driven world-anchored singularity (black-hole lens) for Spatial casts.
 */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class SpatialSingularityClient {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final ResourceLocation PIPELINE = EffecoriaMod.id("singularity");
    public static final ResourceLocation SHADER = EffecoriaMod.id("singularity");

    private static Vec3 worldPos = Vec3.ZERO;
    private static float intensity;
    private static int remainingTicks;
    private static int totalTicks = 1;
    private static float timeSeconds;
    private static boolean pipelineActive;
    private static boolean loggedIrisSkip;
    private static boolean eventsHooked;

    private SpatialSingularityClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(SpatialSingularityClient::hookVeilEvents);
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
            Projected projected = projectToScreen(worldPos);
            float progress = pulseProgress();
            shader.getUniformSafe("Intensity").setFloat(intensity);
            shader.getUniformSafe("Progress").setFloat(progress);
            shader.getUniformSafe("Time").setFloat(timeSeconds);
            shader.getUniformSafe("CenterUV").setVector(projected.u, projected.v);
            shader.getUniformSafe("CenterDepth").setFloat(projected.depth);
            shader.getUniformSafe("BehindCamera").setFloat(projected.behind ? 1f : 0f);
        });
        LOGGER.info("Spatial singularity Veil hooks registered");
    }

    public static void trigger(double x, double y, double z, float intensityIn, int durationTicks) {
        if (isIrisShaderPackInUse()) {
            if (!loggedIrisSkip) {
                LOGGER.info("Singularity FX skipped — Iris/Oculus shaderpack is active");
                loggedIrisSkip = true;
            }
            return;
        }
        worldPos = new Vec3(x, y, z);
        intensity = Mth.clamp(intensityIn, 0.45f, 1.6f);
        totalTicks = Math.max(24, durationTicks);
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
        return 0.2f + 0.8f * (float) Math.sin(Math.PI * Mth.clamp(life, 0f, 1f));
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

    private static void ensurePipeline(boolean enable) {
        try {
            PostProcessingManager manager = VeilRenderSystem.renderer().getPostProcessingManager();
            if (enable) {
                if (!manager.isActive(PIPELINE)) {
                    manager.add(PIPELINE);
                    LOGGER.debug("Singularity pipeline added");
                }
                pipelineActive = true;
            } else {
                if (manager.isActive(PIPELINE)) {
                    manager.remove(PIPELINE);
                }
                pipelineActive = false;
            }
        } catch (Throwable t) {
            LOGGER.warn("Failed to toggle singularity pipeline", t);
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

        // Camera looks down -Z; anything with z >= 0 is behind the near plane / camera.
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
        boolean offscreen = u < -0.15f || u > 1.15f || v < -0.15f || v > 1.15f;
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
