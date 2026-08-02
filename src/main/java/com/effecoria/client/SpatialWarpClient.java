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

/** Area spacetime curvature for gravity wells — bowl warp, not a black hole. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class SpatialWarpClient {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final ResourceLocation PIPELINE = EffecoriaMod.id("spatial_warp");
    public static final ResourceLocation SHADER = EffecoriaMod.id("spatial_warp");

    private static Vec3 displayPos = Vec3.ZERO;
    private static Vec3 targetPos = Vec3.ZERO;
    private static float intensity;
    private static float radiusBlocks = 8f;
    private static int remainingTicks;
    private static int totalTicks = 1;
    private static float timeSeconds;
    private static boolean pipelineActive;
    private static boolean loggedIrisSkip;
    private static boolean eventsHooked;
    private static boolean hasAnchor;

    private SpatialWarpClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(SpatialWarpClient::hookVeilEvents);
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
            Projected projected = projectToScreen(displayPos);
            shader.getUniformSafe("Intensity").setFloat(intensity);
            shader.getUniformSafe("Progress").setFloat(pulseProgress());
            shader.getUniformSafe("Time").setFloat(timeSeconds);
            shader.getUniformSafe("RadiusUV").setFloat(estimateRadiusUv(radiusBlocks));
            shader.getUniformSafe("CenterUV").setVector(projected.u, projected.v);
            shader.getUniformSafe("CenterDepth").setFloat(projected.depth);
            shader.getUniformSafe("BehindCamera").setFloat(projected.behind ? 1f : 0f);
        });
        LOGGER.info("Spatial warp Veil hooks registered");
    }

    public static void trigger(double x, double y, double z, float intensityIn, float radius, int durationTicks) {
        if (isIrisShaderPackInUse()) {
            if (!loggedIrisSkip) {
                LOGGER.info("Spatial warp FX skipped — Iris/Oculus shaderpack is active");
                loggedIrisSkip = true;
            }
            return;
        }
        targetPos = new Vec3(x, y, z);
        displayPos = targetPos;
        hasAnchor = true;
        intensity = Mth.clamp(intensityIn, 0.45f, 1.4f);
        radiusBlocks = Mth.clamp(radius, 2f, 16f);
        totalTicks = Math.max(40, durationTicks);
        remainingTicks = totalTicks;
        timeSeconds = 0f;
        hookVeilEvents();
        ensurePipeline(true);
    }

    public static void pulse(double x, double y, double z, float intensityIn, float radius, int extendTicks) {
        if (isIrisShaderPackInUse()) {
            return;
        }
        targetPos = new Vec3(x, y, z);
        if (!hasAnchor) {
            displayPos = targetPos;
            hasAnchor = true;
        }
        intensity = Mth.clamp(intensityIn, 0.45f, 1.4f);
        radiusBlocks = Mth.clamp(radius, 2f, 16f);
        int extend = Math.max(20, extendTicks);
        remainingTicks = Math.max(remainingTicks, extend);
        totalTicks = Math.max(totalTicks, remainingTicks);
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
        return 0.75f + 0.25f * (float) Math.sin(timeSeconds * 2.4);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (remainingTicks <= 0) {
            if (pipelineActive) {
                ensurePipeline(false);
            }
            hasAnchor = false;
            return;
        }
        displayPos = displayPos.lerp(targetPos, 0.35);
        remainingTicks--;
        timeSeconds += 0.05f;
        if (remainingTicks <= 0) {
            ensurePipeline(false);
            hasAnchor = false;
        }
    }

    private static float estimateRadiusUv(float blocks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 0.18f;
        }
        Camera camera = mc.gameRenderer.getMainCamera();
        double dist = Math.max(2.0, camera.getPosition().distanceTo(displayPos));
        float fov = mc.options.fov().get().floatValue();
        float worldSpan = Math.max(2f, blocks) * 1.05f;
        float approx = (float) (worldSpan / (dist * Math.tan(Math.toRadians(fov * 0.5)) * 2.0));
        return Mth.clamp(approx, 0.06f, 0.55f);
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
            LOGGER.warn("Failed to toggle spatial warp pipeline", t);
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
        float u = clip.x / clip.w * 0.5f + 0.5f;
        float v = clip.y / clip.w * 0.5f + 0.5f;
        float depth = clip.z / clip.w * 0.5f + 0.5f;
        boolean offscreen = u < -0.25f || u > 1.25f || v < -0.25f || v > 1.25f;
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
