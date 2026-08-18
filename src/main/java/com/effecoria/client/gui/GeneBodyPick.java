package com.effecoria.client.gui;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.effecoria.EffecoriaMod;
import com.effecoria.effect.organic.gene.GeneAnatomySlot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import software.bernie.geckolib.animatable.GeoEntity;

/**
 * Projects geo cubes (or a humanoid fallback) into the gene-editor preview so clicks hit real parts.
 */
@OnlyIn(Dist.CLIENT)
public final class GeneBodyPick {
    public record Part(GeneAnatomySlot slot, AABB box) {}

    public record ScreenBox(int x0, int y0, int x1, int y1, float area) {
        public int width() {
            return x1 - x0;
        }

        public int height() {
            return y1 - y0;
        }
    }

    private static final Map<ResourceLocation, List<Part>> GEO_CACHE = new ConcurrentHashMap<>();

    private GeneBodyPick() {}

    public static List<Part> partsFor(LivingEntity entity) {
        ResourceLocation geo = geoResource(entity);
        if (geo != null) {
            List<Part> loaded = GEO_CACHE.computeIfAbsent(geo, GeneBodyPick::loadGeo);
            if (!loaded.isEmpty()) {
                return loaded;
            }
        }
        return fallback(entity);
    }

    public static boolean hasSlot(LivingEntity entity, GeneAnatomySlot slot) {
        for (Part part : partsFor(entity)) {
            if (part.slot() == slot) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static GeneAnatomySlot hit(
            LivingEntity entity,
            double mouseX,
            double mouseY,
            int x1,
            int y1,
            int x2,
            int y2,
            int scale,
            float yOffset,
            float yawDeg,
            float pitchDeg) {
        GeneAnatomySlot best = null;
        float bestArea = Float.MAX_VALUE;
        PoseStack pose = previewPose(entity, x1, y1, x2, y2, scale, yOffset, yawDeg, pitchDeg);
        for (Part part : partsFor(entity)) {
            ScreenBox box = project(part.box(), pose);
            if (box == null) {
                continue;
            }
            if (mouseX >= box.x0() && mouseX < box.x1() && mouseY >= box.y0() && mouseY < box.y1()) {
                if (box.area() < bestArea) {
                    bestArea = box.area();
                    best = part.slot();
                }
            }
        }
        return best;
    }

    public static List<ScreenBox> screenBoxesForSlot(
            LivingEntity entity,
            GeneAnatomySlot slot,
            int x1,
            int y1,
            int x2,
            int y2,
            int scale,
            float yOffset,
            float yawDeg,
            float pitchDeg) {
        List<ScreenBox> out = new ArrayList<>();
        PoseStack pose = previewPose(entity, x1, y1, x2, y2, scale, yOffset, yawDeg, pitchDeg);
        for (Part part : partsFor(entity)) {
            if (part.slot() != slot) {
                continue;
            }
            ScreenBox box = project(part.box(), pose);
            if (box != null) {
                out.add(box);
            }
        }
        return out;
    }

    public static PoseStack previewPose(
            LivingEntity entity,
            int x1,
            int y1,
            int x2,
            int y2,
            int scale,
            float yOffset,
            float yawDeg,
            float pitchDeg) {
        PoseStack pose = new PoseStack();
        pose.translate((x1 + x2) / 2.0f, (y1 + y2) / 2.0f, 50.0f);
        pose.scale(scale, scale, -scale);
        pose.translate(0.0f, entity.getBbHeight() / 2.0f + yOffset, 0.0f);
        float pitchRad = pitchDeg * ((float) Math.PI / 180.0f);
        pose.mulPose(new Quaternionf().rotateZ((float) Math.PI).rotateX(pitchRad));
        pose.mulPose(new Quaternionf().rotateY(-yawDeg * ((float) Math.PI / 180.0f)));
        return pose;
    }

    @Nullable
    private static ScreenBox project(AABB box, PoseStack pose) {
        Matrix4f matrix = pose.last().pose();
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        boolean any = false;
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? box.minX : box.maxX;
            double y = (i & 2) == 0 ? box.minY : box.maxY;
            double z = (i & 4) == 0 ? box.minZ : box.maxZ;
            Vector4f v = new Vector4f((float) x, (float) y, (float) z, 1.0f);
            v.mul(matrix);
            if (!Float.isFinite(v.x) || !Float.isFinite(v.y)) {
                continue;
            }
            any = true;
            minX = Math.min(minX, v.x);
            minY = Math.min(minY, v.y);
            maxX = Math.max(maxX, v.x);
            maxY = Math.max(maxY, v.y);
        }
        if (!any) {
            return null;
        }
        int x0 = Mth.floor(minX);
        int y0 = Mth.floor(minY);
        int x1 = Mth.ceil(maxX);
        int y1 = Mth.ceil(maxY);
        if (x1 <= x0 || y1 <= y0) {
            return null;
        }
        float area = Math.max(1.0f, (float) (x1 - x0) * (y1 - y0));
        return new ScreenBox(x0, y0, x1, y1, area);
    }

    @Nullable
    private static ResourceLocation geoResource(LivingEntity entity) {
        if (!(entity instanceof GeoEntity)) {
            return null;
        }
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (key == null || !key.getNamespace().equals(EffecoriaMod.MOD_ID)) {
            return null;
        }
        String path = key.getPath();
        if ("phi_construct".equals(path)) {
            path = "vitrified_golem";
        }
        return EffecoriaMod.id("geo/" + path + ".geo.json");
    }

    private static List<Part> loadGeo(ResourceLocation geo) {
        List<Part> out = new ArrayList<>();
        var resource = Minecraft.getInstance().getResourceManager().getResource(geo);
        if (resource.isEmpty()) {
            return out;
        }
        try (var reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray geos = root.getAsJsonArray("minecraft:geometry");
            if (geos == null || geos.isEmpty()) {
                return out;
            }
            JsonArray bones = geos.get(0).getAsJsonObject().getAsJsonArray("bones");
            if (bones == null) {
                return out;
            }
            for (JsonElement el : bones) {
                JsonObject bone = el.getAsJsonObject();
                String name = bone.has("name") ? bone.get("name").getAsString() : "";
                GeneAnatomySlot slot = slotForBone(name);
                if (slot == null || !bone.has("cubes")) {
                    continue;
                }
                AABB union = null;
                JsonArray cubes = bone.getAsJsonArray("cubes");
                for (JsonElement cubeEl : cubes) {
                    JsonObject cube = cubeEl.getAsJsonObject();
                    JsonArray origin = cube.getAsJsonArray("origin");
                    JsonArray size = cube.getAsJsonArray("size");
                    if (origin == null || size == null || origin.size() < 3 || size.size() < 3) {
                        continue;
                    }
                    float ox = origin.get(0).getAsFloat() / 16.0f;
                    float oy = origin.get(1).getAsFloat() / 16.0f;
                    float oz = origin.get(2).getAsFloat() / 16.0f;
                    float w = size.get(0).getAsFloat() / 16.0f;
                    float h = size.get(1).getAsFloat() / 16.0f;
                    float d = size.get(2).getAsFloat() / 16.0f;
                    AABB box = new AABB(ox, oy, oz, ox + w, oy + h, oz + d).inflate(0.02);
                    union = union == null ? box : encompass(union, box);
                }
                if (union != null) {
                    out.add(new Part(slot, union));
                }
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return out;
    }

    private static AABB encompass(AABB a, AABB b) {
        return new AABB(
                Math.min(a.minX, b.minX),
                Math.min(a.minY, b.minY),
                Math.min(a.minZ, b.minZ),
                Math.max(a.maxX, b.maxX),
                Math.max(a.maxY, b.maxY),
                Math.max(a.maxZ, b.maxZ));
    }

    @Nullable
    private static GeneAnatomySlot slotForBone(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.equals("root")) {
            return null;
        }
        if (n.contains("wing") || n.contains("cloak") || n.contains("tendril")) {
            return GeneAnatomySlot.DORSUM;
        }
        if (n.contains("tail") || n.contains("trail")) {
            return GeneAnatomySlot.TAIL;
        }
        if (n.contains("head")
                || n.contains("eye")
                || n.contains("jaw")
                || n.contains("horn")
                || n.contains("snout")
                || n.contains("antenna")
                || n.contains("mandible")
                || n.contains("ear")
                || n.contains("crown")
                || n.contains("neck")) {
            return GeneAnatomySlot.HEAD;
        }
        if (n.contains("arm") || n.contains("claw") || n.contains("grab")) {
            return GeneAnatomySlot.FORE;
        }
        if (n.contains("leg") || n.contains("shin") || n.contains("foot") || n.contains("hind")) {
            return GeneAnatomySlot.HIND;
        }
        if (n.contains("body")
                || n.contains("torso")
                || n.contains("chest")
                || n.startsWith("seg_")
                || n.contains("crystal")
                || n.contains("ring")
                || n.contains("colony")
                || n.contains("rise")
                || n.equals("base")) {
            return GeneAnatomySlot.TORSO;
        }
        return null;
    }

    private static List<Part> fallback(LivingEntity entity) {
        List<Part> out = new ArrayList<>();
        float w = entity.getBbWidth();
        float h = entity.getBbHeight();
        float hw = w * 0.5f;
        boolean biped = entity instanceof Player || h > w * 1.35f;
        if (biped) {
            out.add(new Part(GeneAnatomySlot.HEAD, new AABB(-hw * 0.55, h * 0.75, -hw * 0.55, hw * 0.55, h, hw * 0.55)));
            out.add(new Part(
                    GeneAnatomySlot.TORSO, new AABB(-hw * 0.7, h * 0.38, -hw * 0.35, hw * 0.7, h * 0.75, hw * 0.35)));
            out.add(new Part(
                    GeneAnatomySlot.DORSUM, new AABB(-hw * 0.55, h * 0.42, hw * 0.15, hw * 0.55, h * 0.78, hw * 0.55)));
            out.add(new Part(
                    GeneAnatomySlot.FORE, new AABB(-hw * 1.05, h * 0.38, -hw * 0.25, hw * 1.05, h * 0.72, hw * 0.25)));
            out.add(new Part(GeneAnatomySlot.HIND, new AABB(-hw * 0.55, 0.0, -hw * 0.3, hw * 0.55, h * 0.38, hw * 0.3)));
            return out;
        }
        out.add(new Part(GeneAnatomySlot.HEAD, new AABB(-hw * 0.55, h * 0.35, -hw * 1.05, hw * 0.55, h, -hw * 0.25)));
        out.add(new Part(
                GeneAnatomySlot.TORSO, new AABB(-hw * 0.7, h * 0.2, -hw * 0.35, hw * 0.7, h * 0.85, hw * 0.45)));
        out.add(new Part(
                GeneAnatomySlot.DORSUM, new AABB(-hw * 0.55, h * 0.55, -hw * 0.2, hw * 0.55, h, hw * 0.45)));
        out.add(new Part(GeneAnatomySlot.FORE, new AABB(-hw, 0.0, -hw * 0.85, hw, h * 0.45, -hw * 0.1)));
        out.add(new Part(GeneAnatomySlot.HIND, new AABB(-hw, 0.0, hw * 0.05, hw, h * 0.45, hw * 0.85)));
        out.add(new Part(
                GeneAnatomySlot.TAIL, new AABB(-hw * 0.35, h * 0.15, hw * 0.4, hw * 0.35, h * 0.7, hw * 1.2)));
        return out;
    }
}
