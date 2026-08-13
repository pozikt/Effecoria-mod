package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModBlockTags;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItems;
import com.effecoria.core.artifact.CuriosAccess;
import com.effecoria.core.tower.PhiSonarService;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Through-wall (x-ray) outlines for ore / anomaly cells from the tower Φ-sonar feed.
 * Visible only while Φ-sonar goggles are equipped.
 *
 * <p>Markers are rebuilt periodically and culled by distance to the player so the whole
 * circular sweep is considered — not just the first N cells of the grid walk.
 */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientPhiSonarGogglesOutline {
    private static final double VIEW_RANGE = 96.0;
    private static final int MAX_MARKERS = 768;
    private static final int DEEP_PROBE = 56;
    private static final int REBUILD_INTERVAL_TICKS = 8;

    private static List<XrayMark> cachedMarks = List.of();
    private static int rebuildCooldown;
    private static int lastMapStamp;
    private static BlockPos lastPlayerCell = BlockPos.ZERO;

    private ClientPhiSonarGogglesOutline() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (rebuildCooldown > 0) {
            rebuildCooldown--;
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (!CuriosAccess.hasEquipped(mc.player, ModItems.PHI_SONAR_GOGGLES.get())) {
            return;
        }
        if (!ClientPhiSonarMap.hasMap()) {
            return;
        }

        maybeRebuild(mc.player, mc.level);
        if (cachedMarks.isEmpty()) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f mat = pose.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder fill = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (XrayMark mark : cachedMarks) {
            int a = Math.max(18, Math.min(120, Math.round(mark.fillAlpha * 255f)));
            int r = Math.round(mark.rgb[0] * 255f);
            int g = Math.round(mark.rgb[1] * 255f);
            int b = Math.round(mark.rgb[2] * 255f);
            putFilledBox(fill, mat, mark.box, r, g, b, a);
        }
        BufferUploader.drawWithShader(fill.buildOrThrow());

        RenderSystem.lineWidth(2.25f);
        BufferBuilder lines =
                Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        for (XrayMark mark : cachedMarks) {
            int a = 220;
            int r = Math.round(mark.rgb[0] * 255f);
            int g = Math.round(mark.rgb[1] * 255f);
            int b = Math.round(mark.rgb[2] * 255f);
            putLineBox(lines, mat, mark.box, r, g, b, a);
        }
        BufferUploader.drawWithShader(lines.buildOrThrow());

        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        pose.popPose();
    }

    private static void maybeRebuild(Player player, Level level) {
        int stamp = mapStamp();
        BlockPos cell = player.blockPosition();
        boolean moved = cell.distManhattan(lastPlayerCell) >= 4;
        if (stamp == lastMapStamp && !moved && rebuildCooldown > 0 && !cachedMarks.isEmpty()) {
            return;
        }
        lastMapStamp = stamp;
        lastPlayerCell = cell;
        rebuildCooldown = REBUILD_INTERVAL_TICKS;
        cachedMarks = rebuildMarks(player, level);
    }

    private static int mapStamp() {
        return ClientPhiSonarMap.originX()
                * 31
                + ClientPhiSonarMap.originZ()
                + ClientPhiSonarMap.width() * 17
                + ClientPhiSonarMap.modeId() * 13
                + ClientPhiSonarMap.radius() * 7
                + (ClientPhiSonarMap.hasMap() ? 1 : 0);
    }

    private static List<XrayMark> rebuildMarks(Player player, Level level) {
        byte[] heights = ClientPhiSonarMap.heights();
        byte[] terrain = ClientPhiSonarMap.terrain();
        int width = ClientPhiSonarMap.width();
        if (heights == null || terrain == null || width <= 0 || heights.length != width * width) {
            return List.of();
        }

        int ox = ClientPhiSonarMap.originX();
        int oy = ClientPhiSonarMap.originY();
        int oz = ClientPhiSonarMap.originZ();
        int radius = ClientPhiSonarMap.radius();
        int step = Math.max(1, ClientPhiSonarMap.step());
        double rangeSq = VIEW_RANGE * VIEW_RANGE;

        List<Candidate> candidates = new ArrayList<>(256);
        Set<Long> seen = new HashSet<>();

        int i = 0;
        for (int iz = -radius; iz <= radius; iz += step) {
            for (int ix = -radius; ix <= radius; ix += step) {
                byte kind = terrain[i];
                if (kind != PhiSonarService.TERRAIN_OUTSIDE && PhiSonarService.inCircle(ix, iz, radius)) {
                    float[] rgb = terrainRgb(kind);
                    if (rgb != null) {
                        int wx = ox + ix;
                        int wz = oz + iz;
                        int surfaceY = oy + heights[i];
                        // Sample cell
                        addResolved(candidates, seen, player, level, wx, surfaceY, wz, kind, rgb, rangeSq);
                        // Densify coarse steps so Active (step 2) / Long-range still fill the circle
                        if (step > 1 && isOreLike(kind)) {
                            int half = step - 1;
                            for (int dz = -half; dz <= half; dz++) {
                                for (int dx = -half; dx <= half; dx++) {
                                    if (dx == 0 && dz == 0) {
                                        continue;
                                    }
                                    int nx = wx + dx;
                                    int nz = wz + dz;
                                    if (!PhiSonarService.inCircle(nx - ox, nz - oz, radius)) {
                                        continue;
                                    }
                                    addResolved(candidates, seen, player, level, nx, surfaceY, nz, kind, rgb, rangeSq);
                                }
                            }
                        }
                    }
                }
                i++;
            }
        }

        for (PhiSonarService.Blip blip : ClientPhiSonarMap.blips()) {
            if (blip.kind() != PhiSonarService.BLIP_GEYSER && blip.kind() != PhiSonarService.BLIP_OMEGA) {
                continue;
            }
            if (!PhiSonarService.inCircle(blip.relX(), blip.relZ(), radius)) {
                continue;
            }
            int wx = ox + blip.relX();
            int wz = oz + blip.relZ();
            int gj = Math.min(width - 1, Math.max(0, (blip.relX() + radius) / step));
            int gi = Math.min(width - 1, Math.max(0, (blip.relZ() + radius) / step));
            int surface = oy + heights[gi * width + gj];
            byte kind = blip.kind() == PhiSonarService.BLIP_OMEGA
                    ? PhiSonarService.TERRAIN_OMEGA
                    : PhiSonarService.TERRAIN_GEYSER;
            float[] rgb = blip.kind() == PhiSonarService.BLIP_OMEGA
                    ? new float[] {0.75f, 0.25f, 1.0f}
                    : new float[] {0.25f, 0.95f, 1.0f};
            addResolved(candidates, seen, player, level, wx, surface, wz, kind, rgb, rangeSq);
        }

        if (candidates.isEmpty()) {
            return List.of();
        }

        candidates.sort(Comparator.comparingDouble(Candidate::distSq));
        int limit = Math.min(MAX_MARKERS, candidates.size());
        List<XrayMark> out = new ArrayList<>(limit);
        for (int n = 0; n < limit; n++) {
            Candidate c = candidates.get(n);
            out.add(new XrayMark(new AABB(c.pos).inflate(c.pad), c.rgb, fillAlpha(c.kind), true));
        }
        return List.copyOf(out);
    }

    private static void addResolved(
            List<Candidate> candidates,
            Set<Long> seen,
            Player player,
            Level level,
            int wx,
            int surfaceY,
            int wz,
            byte kind,
            float[] rgb,
            double rangeSq) {
        BlockPos target = resolveTarget(level, wx, surfaceY, wz, kind);
        // Only keep if we found a real matching block (avoid surface false positives)
        if (!matchesKind(level.getBlockState(target), kind) && kind != PhiSonarService.TERRAIN_CAVE) {
            return;
        }
        long key = BlockPos.asLong(target.getX(), target.getY(), target.getZ());
        if (!seen.add(key)) {
            return;
        }
        double distSq = player.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        if (distSq > rangeSq) {
            return;
        }
        float pad = kind == PhiSonarService.TERRAIN_CAVE ? 0.002f : 0.01f;
        candidates.add(new Candidate(target, rgb, kind, distSq, pad));
    }

    private static boolean isOreLike(byte kind) {
        return kind == PhiSonarService.TERRAIN_ESSONITE
                || kind == PhiSonarService.TERRAIN_MITHRIL
                || kind == PhiSonarService.TERRAIN_OMEGA
                || kind == PhiSonarService.TERRAIN_GEYSER
                || kind == PhiSonarService.TERRAIN_SHIELD;
    }

    private static BlockPos resolveTarget(Level level, int wx, int surfaceY, int wz, byte kind) {
        int minY = Math.max(level.getMinBuildHeight() + 1, surfaceY - DEEP_PROBE);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, surfaceY + 6);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int y = Math.min(surfaceY, maxY); y >= minY; y--) {
            if (!level.isLoaded(cursor.set(wx, y, wz))) {
                break;
            }
            if (matchesKind(level.getBlockState(cursor), kind)) {
                return cursor.immutable();
            }
        }
        for (int y = surfaceY + 1; y <= maxY; y++) {
            if (!level.isLoaded(cursor.set(wx, y, wz))) {
                break;
            }
            if (matchesKind(level.getBlockState(cursor), kind)) {
                return cursor.immutable();
            }
        }

        int fy = Math.max(level.getMinBuildHeight(), Math.min(level.getMaxBuildHeight() - 1, surfaceY - 1));
        return new BlockPos(wx, fy, wz);
    }

    private static boolean matchesKind(BlockState state, byte kind) {
        return switch (kind) {
            case PhiSonarService.TERRAIN_ESSONITE ->
                    state.is(ModBlockTags.ESSONITE_ORES)
                            || state.is(ModBlocks.ESSONITE_BLOCK.get())
                            || state.is(ModBlocks.STAR_ESSONITE_BLOCK.get());
            case PhiSonarService.TERRAIN_MITHRIL ->
                    state.is(ModBlockTags.MITHRIL_ORES) || state.is(ModBlocks.MITHRIL_BLOCK.get());
            case PhiSonarService.TERRAIN_OMEGA ->
                    state.is(ModBlocks.VOID_OBSIDIAN.get())
                            || state.is(ModBlocks.OMEGA_TAINTED_OBSIDIAN.get())
                            || state.is(ModBlocks.OMEGA_CRYSTAL.get())
                            || state.is(ModBlocks.ASH_SOIL.get())
                            || state.is(ModBlocks.OMEGA_BLADES.get());
            case PhiSonarService.TERRAIN_GEYSER -> state.is(ModBlocks.PHI_GEYSER.get());
            case PhiSonarService.TERRAIN_SHIELD ->
                    state.is(ModBlockTags.ZERO_FLUX)
                            || state.is(Blocks.GOLD_BLOCK)
                            || state.is(Blocks.GOLD_ORE)
                            || state.is(Blocks.DEEPSLATE_GOLD_ORE)
                            || state.is(Blocks.RAW_GOLD_BLOCK)
                            || state.is(ModBlocks.LEAD_BLOCK.get())
                            || state.is(ModBlocks.LEAD_ORE.get())
                            || state.is(ModBlocks.DEEPSLATE_LEAD_ORE.get());
            case PhiSonarService.TERRAIN_CAVE -> state.isAir();
            default -> false;
        };
    }

    private static float fillAlpha(byte kind) {
        return switch (kind) {
            case PhiSonarService.TERRAIN_ESSONITE, PhiSonarService.TERRAIN_MITHRIL -> 0.32f;
            case PhiSonarService.TERRAIN_OMEGA, PhiSonarService.TERRAIN_GEYSER -> 0.28f;
            case PhiSonarService.TERRAIN_SHIELD -> 0.24f;
            case PhiSonarService.TERRAIN_CAVE -> 0.12f;
            default -> 0.2f;
        };
    }

    @Nullable
    private static float[] terrainRgb(byte kind) {
        return switch (kind) {
            case PhiSonarService.TERRAIN_ESSONITE -> new float[] {0.2f, 0.9f, 1.0f};
            case PhiSonarService.TERRAIN_MITHRIL -> new float[] {0.45f, 0.78f, 1.0f};
            case PhiSonarService.TERRAIN_OMEGA -> new float[] {0.7f, 0.22f, 1.0f};
            case PhiSonarService.TERRAIN_GEYSER -> new float[] {0.3f, 0.98f, 1.0f};
            case PhiSonarService.TERRAIN_SHIELD -> new float[] {1.0f, 0.82f, 0.25f};
            // caves omitted from goggles — too many and drowned the ore circle
            default -> null;
        };
    }

    private static void putFilledBox(BufferBuilder buf, Matrix4f mat, AABB box, int r, int g, int b, int a) {
        float x0 = (float) box.minX;
        float y0 = (float) box.minY;
        float z0 = (float) box.minZ;
        float x1 = (float) box.maxX;
        float y1 = (float) box.maxY;
        float z1 = (float) box.maxZ;
        quad(buf, mat, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a);
        quad(buf, mat, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, r, g, b, a);
        quad(buf, mat, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r, g, b, a);
        quad(buf, mat, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, r, g, b, a);
        quad(buf, mat, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, r, g, b, a);
        quad(buf, mat, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r, g, b, a);
    }

    private static void putLineBox(BufferBuilder buf, Matrix4f mat, AABB box, int r, int g, int b, int a) {
        float x0 = (float) box.minX;
        float y0 = (float) box.minY;
        float z0 = (float) box.minZ;
        float x1 = (float) box.maxX;
        float y1 = (float) box.maxY;
        float z1 = (float) box.maxZ;
        line(buf, mat, x0, y0, z0, x1, y0, z0, r, g, b, a);
        line(buf, mat, x1, y0, z0, x1, y0, z1, r, g, b, a);
        line(buf, mat, x1, y0, z1, x0, y0, z1, r, g, b, a);
        line(buf, mat, x0, y0, z1, x0, y0, z0, r, g, b, a);
        line(buf, mat, x0, y1, z0, x1, y1, z0, r, g, b, a);
        line(buf, mat, x1, y1, z0, x1, y1, z1, r, g, b, a);
        line(buf, mat, x1, y1, z1, x0, y1, z1, r, g, b, a);
        line(buf, mat, x0, y1, z1, x0, y1, z0, r, g, b, a);
        line(buf, mat, x0, y0, z0, x0, y1, z0, r, g, b, a);
        line(buf, mat, x1, y0, z0, x1, y1, z0, r, g, b, a);
        line(buf, mat, x1, y0, z1, x1, y1, z1, r, g, b, a);
        line(buf, mat, x0, y0, z1, x0, y1, z1, r, g, b, a);
    }

    private static void quad(
            BufferBuilder buf,
            Matrix4f mat,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            int r,
            int g,
            int b,
            int a) {
        buf.addVertex(mat, x0, y0, z0).setColor(r, g, b, a);
        buf.addVertex(mat, x1, y1, z1).setColor(r, g, b, a);
        buf.addVertex(mat, x2, y2, z2).setColor(r, g, b, a);
        buf.addVertex(mat, x3, y3, z3).setColor(r, g, b, a);
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

    private record Candidate(BlockPos pos, float[] rgb, byte kind, double distSq, float pad) {}

    private record XrayMark(AABB box, float[] rgb, float fillAlpha, boolean brightEdge) {}
}
