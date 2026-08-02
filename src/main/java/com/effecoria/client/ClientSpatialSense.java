package com.effecoria.client;

import java.util.ArrayList;
import java.util.List;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.effect.spatial.SpatialSenseService;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import org.joml.Matrix4f;

/**
 * Client spatial sonar: through-wall cavity outlines that fade with path distance
 * and reveal in expanding ping bands (every 5 blocks).
 */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSpatialSense {
    private static final int BAND_BLOCKS = 5;
    private static final int TICKS_PER_BAND = 4;

    private static BlockPos origin = BlockPos.ZERO;
    private static final List<SenseMark> marks = new ArrayList<>();
    private static int remainingTicks;
    private static int totalTicks = 1;
    private static int ageTicks;

    private ClientSpatialSense() {}

    public static void activate(int ox, int oy, int oz, int durationTicks, List<SpatialSenseService.Hit> hits) {
        origin = new BlockPos(ox, oy, oz);
        marks.clear();
        for (SpatialSenseService.Hit hit : hits) {
            int manhattan = Math.abs(hit.dx()) + Math.abs(hit.dy()) + Math.abs(hit.dz());
            marks.add(new SenseMark(
                    origin.offset(hit.dx(), hit.dy(), hit.dz()),
                    hit.strength() / 100f,
                    hit.kind(),
                    manhattan));
        }
        totalTicks = Math.max(40, durationTicks);
        remainingTicks = totalTicks;
        ageTicks = 0;
    }

    public static boolean isActive() {
        return remainingTicks > 0 && !marks.isEmpty();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (remainingTicks <= 0) {
            return;
        }
        remainingTicks--;
        ageTicks++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            return;
        }
        if (ageTicks % 3 != 0) {
            return;
        }
        int revealedBand = ageTicks / TICKS_PER_BAND;
        for (SenseMark mark : marks) {
            int band = mark.pathHint / BAND_BLOCKS;
            if (band > revealedBand) {
                continue;
            }
            if (mark.kind != SpatialSenseService.KIND_VOID && mark.kind != SpatialSenseService.KIND_TRAP) {
                continue;
            }
            if (((mark.pos.getX() * 31) ^ (mark.pos.getZ() * 17) ^ ageTicks) % 11 != 0) {
                continue;
            }
            float fade = lifeFade() * mark.strength;
            if (fade < 0.08f) {
                continue;
            }
            double x = mark.pos.getX() + 0.5;
            double y = mark.pos.getY() + 0.55;
            double z = mark.pos.getZ() + 0.5;
            if (mark.kind == SpatialSenseService.KIND_TRAP) {
                mc.level.addParticle(ModParticleTypes.CORRUPTION_RUNE.get(), x, y, z, 0, 0.02, 0);
            } else {
                mc.level.addParticle(ModParticleTypes.PHI_SPARK.get(), x, y, z, 0, 0.01 * fade, 0);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        if (!isActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        float life = lifeFade();
        int revealedBand = ageTicks / TICKS_PER_BAND;

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f mat = poseStack.last().pose();

        // Immediate draw with depth disabled — RenderType.lines() re-enables depth on flush.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(2.5f);

        // Pre-filter so we never flush an empty BufferBuilder.
        int revealed = revealedBand;
        float lifeAlpha = life;
        boolean any = false;
        for (SenseMark mark : marks) {
            if (mark.pathHint / BAND_BLOCKS <= revealed
                    && lifeAlpha * mark.strength * Math.max(0.08f, 1f - (mark.pathHint / BAND_BLOCKS) * 0.12f)
                            >= 0.05f) {
                any = true;
                break;
            }
        }
        if (any) {
            BufferBuilder buffer = Tesselator.getInstance()
                    .begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            for (SenseMark mark : marks) {
                int band = mark.pathHint / BAND_BLOCKS;
                if (band > revealed) {
                    continue;
                }
                float bandFade = Math.max(0.08f, 1f - band * 0.12f);
                float alpha = lifeAlpha * mark.strength * bandFade;
                if (alpha < 0.05f) {
                    continue;
                }
                float[] rgb = colorFor(mark.kind);
                int a = Math.max(12, Math.min(255, Math.round(alpha * 255f)));
                int r = Math.round(rgb[0] * 255f);
                int g = Math.round(rgb[1] * 255f);
                int b = Math.round(rgb[2] * 255f);
                double pad = mark.kind == SpatialSenseService.KIND_WALL ? 0.02 : -0.12;
                putBox(buffer, mat, new AABB(mark.pos).inflate(pad), r, g, b, a);
            }
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }

        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void putBox(BufferBuilder buf, Matrix4f mat, AABB box, int r, int g, int b, int a) {
        float x0 = (float) box.minX;
        float y0 = (float) box.minY;
        float z0 = (float) box.minZ;
        float x1 = (float) box.maxX;
        float y1 = (float) box.maxY;
        float z1 = (float) box.maxZ;
        // bottom
        line(buf, mat, x0, y0, z0, x1, y0, z0, r, g, b, a);
        line(buf, mat, x1, y0, z0, x1, y0, z1, r, g, b, a);
        line(buf, mat, x1, y0, z1, x0, y0, z1, r, g, b, a);
        line(buf, mat, x0, y0, z1, x0, y0, z0, r, g, b, a);
        // top
        line(buf, mat, x0, y1, z0, x1, y1, z0, r, g, b, a);
        line(buf, mat, x1, y1, z0, x1, y1, z1, r, g, b, a);
        line(buf, mat, x1, y1, z1, x0, y1, z1, r, g, b, a);
        line(buf, mat, x0, y1, z1, x0, y1, z0, r, g, b, a);
        // pillars
        line(buf, mat, x0, y0, z0, x0, y1, z0, r, g, b, a);
        line(buf, mat, x1, y0, z0, x1, y1, z0, r, g, b, a);
        line(buf, mat, x1, y0, z1, x1, y1, z1, r, g, b, a);
        line(buf, mat, x0, y0, z1, x0, y1, z1, r, g, b, a);
    }

    private static void line(
            BufferBuilder buf,
            Matrix4f mat,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            int r,
            int g,
            int b,
            int a) {
        buf.addVertex(mat, x0, y0, z0).setColor(r, g, b, a);
        buf.addVertex(mat, x1, y1, z1).setColor(r, g, b, a);
    }

    private static float lifeFade() {
        if (remainingTicks <= 0) {
            return 0f;
        }
        float life = remainingTicks / (float) totalTicks;
        if (life < 0.25f) {
            return life / 0.25f;
        }
        return 1f;
    }

    private static float[] colorFor(byte kind) {
        return switch (kind) {
            case SpatialSenseService.KIND_TRAP -> new float[] {1.0f, 0.35f, 0.2f};
            case SpatialSenseService.KIND_VOID -> new float[] {0.45f, 0.85f, 1.0f};
            default -> new float[] {0.55f, 0.95f, 1.0f};
        };
    }

    private record SenseMark(BlockPos pos, float strength, byte kind, int pathHint) {}
}
