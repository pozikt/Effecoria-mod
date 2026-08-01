package com.effecoria.client;

import com.effecoria.EffecoriaMod;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.platform.VeilEventPlatform;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
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
 * Persistent Veil lens for open subspace punctures — supermassive refraction around the tear.
 * BER calls {@link #present} each frame a portal is drawn; pipeline drops when none refresh.
 */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class SubspacePortalLensClient {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final ResourceLocation PIPELINE = EffecoriaMod.id("subspace_puncture");
    public static final ResourceLocation SHADER = EffecoriaMod.id("subspace_puncture");

    private static Vec3 worldPos = Vec3.ZERO;
    private static float intensity = 1f;
    private static float ellipseY = 1.15f;
    private static int staleTicks = 0;
    private static float timeSeconds;
    private static boolean pipelineActive;
    private static boolean eventsHooked;
    private static boolean loggedIrisSkip;

    private SubspacePortalLensClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(SubspacePortalLensClient::hookVeilEvents);
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
            shader.getUniformSafe("Intensity").setFloat(intensity);
            shader.getUniformSafe("Progress").setFloat(1f);
            shader.getUniformSafe("Time").setFloat(timeSeconds);
            shader.getUniformSafe("CenterUV").setVector(projected.u, projected.v);
            shader.getUniformSafe("CenterDepth").setFloat(projected.depth);
            shader.getUniformSafe("BehindCamera").setFloat(projected.behind ? 1f : 0f);
            shader.getUniformSafe("EllipseY").setFloat(ellipseY);
        });
        LOGGER.info("Subspace puncture Veil lens registered");
    }

    /**
     * Called from the portal BER while the puncture is on screen.
     * Keeps the gravitational lens alive without a network pulse.
     */
    public static void present(Vec3 center, Direction facing, float intensityIn) {
        if (isIrisShaderPackInUse()) {
            if (!loggedIrisSkip) {
                LOGGER.info("Subspace puncture lens skipped — Iris/Oculus shaderpack is active");
                loggedIrisSkip = true;
            }
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.distanceToSqr(center) > 48 * 48) {
            return;
        }
        // Prefer the closest portal if several notify in one frame.
        if (staleTicks > 0 && mc.player != null) {
            double existing = mc.player.distanceToSqr(worldPos);
            double next = mc.player.distanceToSqr(center);
            if (next > existing + 0.5) {
                staleTicks = 3;
                return;
            }
        }
        worldPos = center;
        intensity = Mth.clamp(intensityIn, 0.55f, 1.45f);
        // Face-on portals read taller; edge-on compresses the ellipse.
        float viewDot = 0.65f;
        if (mc.gameRenderer != null) {
            Vec3 look = Vec3.directionFromRotation(mc.gameRenderer.getMainCamera().getXRot(), mc.gameRenderer.getMainCamera().getYRot());
            viewDot = (float) Math.abs(look.dot(Vec3.atLowerCornerOf(facing.getNormal())));
        }
        ellipseY = Mth.lerp(viewDot, 0.75f, 1.35f);
        staleTicks = 3;
        hookVeilEvents();
        ensurePipeline(true);
    }

    public static boolean isActive() {
        return staleTicks > 0;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (staleTicks <= 0) {
            if (pipelineActive) {
                ensurePipeline(false);
            }
            return;
        }
        staleTicks--;
        timeSeconds += 0.05f;
        if (staleTicks <= 0) {
            ensurePipeline(false);
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
            LOGGER.warn("Failed to toggle subspace puncture pipeline", t);
            pipelineActive = false;
            staleTicks = 0;
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
