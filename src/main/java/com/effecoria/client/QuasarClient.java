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
 * Veil post for elemental Quasar: a compact violet epicenter glow (≈1 block), world-anchored.
 */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class QuasarClient {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final ResourceLocation PIPELINE = EffecoriaMod.id("quasar");
    public static final ResourceLocation SHADER = EffecoriaMod.id("quasar");

    /** Smoothed world anchor used for projection (lerps toward server updates). */
    private static Vec3 displayPos = Vec3.ZERO;
    private static Vec3 targetPos = Vec3.ZERO;
    private static float intensity;
    /** Visual core size in blocks — kept tiny so it never reads as a screen black hole. */
    private static float coreBlocks = 1.0f;
    private static int remainingTicks;
    private static int totalTicks = 1;
    private static float timeSeconds;
    private static boolean pipelineActive;
    private static boolean loggedIrisSkip;
    private static boolean eventsHooked;
    private static boolean hasAnchor;

    private QuasarClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(QuasarClient::hookVeilEvents);
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
            float progress = pulseProgress();
            float radiusUv = estimateCoreRadiusUv(coreBlocks);
            shader.getUniformSafe("Intensity").setFloat(intensity);
            shader.getUniformSafe("Progress").setFloat(progress);
            shader.getUniformSafe("Time").setFloat(timeSeconds);
            shader.getUniformSafe("RadiusUV").setFloat(radiusUv);
            shader.getUniformSafe("CenterUV").setVector(projected.u, projected.v);
            shader.getUniformSafe("CenterDepth").setFloat(projected.depth);
            shader.getUniformSafe("BehindCamera").setFloat(projected.behind ? 1f : 0f);
        });
        LOGGER.info("Quasar Veil hooks registered");
    }

    public static void trigger(double x, double y, double z, float intensityIn, float radius, int durationTicks) {
        if (isIrisShaderPackInUse()) {
            if (!loggedIrisSkip) {
                LOGGER.info("Quasar FX skipped — Iris/Oculus shaderpack is active");
                loggedIrisSkip = true;
            }
            return;
        }
        targetPos = new Vec3(x, y, z);
        displayPos = targetPos;
        hasAnchor = true;
        intensity = Mth.clamp(intensityIn, 0.7f, 1.45f);
        // Ignore gameplay field radius for Veil — epicenter is always ~1 block.
        coreBlocks = 1.0f;
        totalTicks = Math.max(40, durationTicks);
        remainingTicks = totalTicks;
        timeSeconds = 0f;
        hookVeilEvents();
        ensurePipeline(true);
    }

    /** Refresh lifetime / target while the field is still active on the server. */
    public static void pulse(double x, double y, double z, float intensityIn, float radius, int extendTicks) {
        if (isIrisShaderPackInUse()) {
            return;
        }
        targetPos = new Vec3(x, y, z);
        if (!hasAnchor) {
            displayPos = targetPos;
            hasAnchor = true;
        }
        intensity = Mth.clamp(intensityIn, 0.7f, 1.45f);
        coreBlocks = 1.0f;
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
        return 0.85f + 0.15f * (float) Math.sin(timeSeconds * 3.1);
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
        // Smooth chase — avoids teleport snaps when packets arrive every few ticks.
        displayPos = displayPos.lerp(targetPos, 0.42);
        remainingTicks--;
        timeSeconds += 0.05f;
        if (remainingTicks <= 0) {
            ensurePipeline(false);
            hasAnchor = false;
        }
    }

    /** Screen footprint of a ~1-block core at the quasar's world distance. */
    private static float estimateCoreRadiusUv(float blocks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 0.025f;
        }
        Camera camera = mc.gameRenderer.getMainCamera();
        double dist = camera.getPosition().distanceTo(displayPos);
        dist = Math.max(2.0, dist);
        float fov = mc.options.fov().get().floatValue();
        float worldSpan = Math.max(0.85f, blocks) * 1.15f;
        float approx = (float) (worldSpan / (dist * Math.tan(Math.toRadians(fov * 0.5)) * 2.0));
        return Mth.clamp(approx, 0.01f, 0.07f);
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
            LOGGER.warn("Failed to toggle quasar pipeline", t);
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
        }
        try {
            Class<?> iris = Class.forName("net.coderbot.iris.Iris");
            Object config = iris.getMethod("getIrisConfig").invoke(null);
            if (config != null) {
                Object enabled = config.getClass().getMethod("areShadersEnabled").invoke(config);
                return enabled instanceof Boolean b && b;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private record Projected(float u, float v, float depth, boolean behind) {}
}
