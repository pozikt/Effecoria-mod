package com.effecoria.client.gui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

import com.effecoria.core.magic.RadialCategory;
import com.effecoria.core.magic.SpellDefinition;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.magic.SpellRegistry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Constellation layout: player core at center, spells on orbits grouped by category.
 */
public final class SpellHubLayout {
    public static final float BASE_CORE_RADIUS = 34f;
    public static final float BASE_MIN_ORBIT = 78f;
    public static final float BASE_ORBIT_STEP = 30f;
    /** Fixed orbit for the breathing-train node (left of core). */
    public static final float BASE_TRAIN_ORBIT = 92f;
    private static final float NODE_GAP = 8f;
    private static final float MAX_CATEGORY_SPAN = (float) (Math.PI * 0.46);
    private static final int SEPARATION_ITERATIONS = 14;
    /** Screen angle: left (−X) so the train node stays clear of spell fans. */
    private static final float TRAIN_ANGLE = (float) Math.PI;

    public record SpellNode(
            ResourceLocation spellId,
            int knownIndex,
            RadialCategory category,
            float offsetX,
            float offsetY) {}

    public record TrainNode(float offsetX, float offsetY, float radius) {
        public boolean contains(float dx, float dy) {
            float ndx = dx - offsetX;
            float ndy = dy - offsetY;
            float hit = radius + 6f;
            return ndx * ndx + ndy * ndy <= hit * hit;
        }
    }

    public record Layout(float scale, float coreRadius, float nodeRadius, List<SpellNode> nodes, TrainNode trainNode) {}

    private SpellHubLayout() {}

    public static float menuScale(int screenWidth, int screenHeight) {
        float minDim = Math.min(screenWidth, screenHeight);
        return Mth.clamp(minDim / 420f, 0.62f, 0.92f);
    }

    public static Layout build(PlayerPsiData data, int screenWidth, int screenHeight) {
        float scale = menuScale(screenWidth, screenHeight);
        float coreRadius = BASE_CORE_RADIUS * scale;
        float nodeRadius = Math.max(13f, 17f * scale);
        float minOrbit = BASE_MIN_ORBIT * scale;
        float orbitStep = BASE_ORBIT_STEP * scale;
        float maxOrbit = Math.min(screenWidth, screenHeight) * 0.40f * scale;
        orbitStep = Math.max(orbitStep, minCenterDistance(nodeRadius) * 0.95f);
        float trainOrbit = Math.min(BASE_TRAIN_ORBIT * scale, maxOrbit);
        float trainRadius = nodeRadius * 1.15f;

        TrainNode trainNode = new TrainNode(
                (float) Math.cos(TRAIN_ANGLE) * trainOrbit,
                (float) Math.sin(TRAIN_ANGLE) * trainOrbit,
                trainRadius);

        List<ResourceLocation> known = data.knownSpells();
        EnumMap<RadialCategory, List<IndexedSpell>> byCategory = new EnumMap<>(RadialCategory.class);
        for (RadialCategory category : RadialCategory.values()) {
            byCategory.put(category, new ArrayList<>());
        }
        for (int i = 0; i < known.size(); i++) {
            ResourceLocation id = known.get(i);
            Optional<SpellDefinition> def = SpellRegistry.get(id);
            if (def.isEmpty()) {
                continue;
            }
            byCategory.get(def.get().radialCategory()).add(new IndexedSpell(id, i));
        }

        int activeCategories = (int) byCategory.values().stream().filter(list -> !list.isEmpty()).count();
        float categorySpan = categorySpanFor(activeCategories);

        List<SpellNode> nodes = new ArrayList<>();
        float[] categoryAngle = categoryBaseAngles();
        RadialCategory[] order = RadialCategory.outerRingOrder();
        for (int c = 0; c < order.length; c++) {
            RadialCategory category = order[c];
            List<IndexedSpell> spells = byCategory.get(category);
            if (spells.isEmpty()) {
                continue;
            }
            placeCategoryFan(
                    nodes,
                    spells,
                    category,
                    categoryAngle[c],
                    categorySpan,
                    minOrbit,
                    orbitStep,
                    maxOrbit,
                    nodeRadius);
        }

        nodes = separateNodes(nodes, nodeRadius, coreRadius + 4f, maxOrbit);
        // Keep spell nodes out of the fixed train node.
        nodes = pushAwayFromTrain(nodes, trainNode, nodeRadius);
        return new Layout(scale, coreRadius, nodeRadius, List.copyOf(nodes), trainNode);
    }

    public static Optional<SpellNode> pick(Layout layout, float dx, float dy) {
        if (layout.trainNode() != null && layout.trainNode().contains(dx, dy)) {
            return Optional.empty();
        }
        float hit = layout.nodeRadius() + 6f;
        SpellNode best = null;
        float bestDist = hit * hit;
        for (SpellNode node : layout.nodes()) {
            float ndx = dx - node.offsetX();
            float ndy = dy - node.offsetY();
            float distSq = ndx * ndx + ndy * ndy;
            if (distSq <= bestDist) {
                bestDist = distSq;
                best = node;
            }
        }
        return Optional.ofNullable(best);
    }

    public static Optional<TrainNode> pickTrain(Layout layout, float dx, float dy) {
        if (layout.trainNode() != null && layout.trainNode().contains(dx, dy)) {
            return Optional.of(layout.trainNode());
        }
        return Optional.empty();
    }

    private static List<SpellNode> pushAwayFromTrain(List<SpellNode> nodes, TrainNode train, float nodeRadius) {
        float minDist = train.radius() + nodeRadius + NODE_GAP;
        float minDistSq = minDist * minDist;
        List<SpellNode> out = new ArrayList<>(nodes.size());
        for (SpellNode node : nodes) {
            float dx = node.offsetX() - train.offsetX();
            float dy = node.offsetY() - train.offsetY();
            float distSq = dx * dx + dy * dy;
            if (distSq >= minDistSq || distSq < 1.0E-4f) {
                out.add(node);
                continue;
            }
            float dist = (float) Math.sqrt(distSq);
            float scale = minDist / dist;
            out.add(new SpellNode(
                    node.spellId(),
                    node.knownIndex(),
                    node.category(),
                    train.offsetX() + dx * scale,
                    train.offsetY() + dy * scale));
        }
        return out;
    }

    private static float categorySpanFor(int activeCategories) {
        if (activeCategories <= 1) {
            return (float) (Math.PI * 0.9);
        }
        float evenSpan = (float) (Math.PI * 2.0 / activeCategories) * 0.82f;
        return Math.min(MAX_CATEGORY_SPAN, evenSpan);
    }

    private static float minCenterDistance(float nodeRadius) {
        return nodeRadius * 2f + NODE_GAP;
    }

    private static float[] categoryBaseAngles() {
        return new float[] {
            (float) (-Math.PI / 2.0),
            0f,
            (float) (Math.PI / 2.0),
            (float) Math.PI
        };
    }

    private static void placeCategoryFan(
            List<SpellNode> nodes,
            List<IndexedSpell> spells,
            RadialCategory category,
            float baseAngle,
            float span,
            float minOrbit,
            float orbitStep,
            float maxOrbit,
            float nodeRadius) {
        int count = spells.size();
        float minDist = minCenterDistance(nodeRadius);
        int index = 0;
        int ring = 0;

        while (index < count) {
            float orbit = minOrbit + ring * orbitStep;
            if (orbit > maxOrbit) {
                orbit = maxOrbit;
            }

            int capacity = maxNodesOnRing(orbit, span, minDist);
            int remaining = count - index;
            int onRing = Math.min(capacity, remaining);

            for (int j = 0; j < onRing; j++) {
                IndexedSpell spell = spells.get(index++);
                float t = onRing == 1 ? 0.5f : j / (float) (onRing - 1);
                float angle = baseAngle - span * 0.5f + t * span;
                float nx = (float) Math.cos(angle) * orbit;
                float ny = (float) Math.sin(angle) * orbit;
                nodes.add(new SpellNode(spell.id(), spell.knownIndex(), category, nx, ny));
            }
            ring++;
            if (ring > 8) {
                break;
            }
        }
    }

    /** How many nodes fit on one ring without overlapping (chord spacing). */
    private static int maxNodesOnRing(float orbit, float span, float minDist) {
        if (orbit < 1f || span < 0.01f) {
            return 1;
        }
        float arcLength = span * orbit;
        int byArc = Math.max(1, (int) Math.floor(arcLength / minDist));
        if (byArc <= 1) {
            return 1;
        }
        float halfAngle = span * 0.5f;
        float sinHalf = (float) Math.sin(Math.min(Math.PI * 0.49, halfAngle));
        if (sinHalf < 0.01f) {
            return 1;
        }
        int byChord = Math.max(1, (int) Math.floor((2f * orbit * sinHalf) / minDist) + 1);
        return Math.max(1, Math.min(byArc, byChord));
    }

    private static List<SpellNode> separateNodes(
            List<SpellNode> nodes, float nodeRadius, float minRadius, float maxRadius) {
        if (nodes.size() < 2) {
            return nodes;
        }

        float minDist = minCenterDistance(nodeRadius);
        float minDistSq = minDist * minDist;
        int n = nodes.size();
        float[] xs = new float[n];
        float[] ys = new float[n];

        for (int i = 0; i < n; i++) {
            SpellNode node = nodes.get(i);
            xs[i] = node.offsetX();
            ys[i] = node.offsetY();
        }

        for (int iter = 0; iter < SEPARATION_ITERATIONS; iter++) {
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    float dx = xs[j] - xs[i];
                    float dy = ys[j] - ys[i];
                    float distSq = dx * dx + dy * dy;
                    if (distSq >= minDistSq || distSq < 1.0e-4f) {
                        if (distSq < 1.0e-4f) {
                            float jitter = 0.5f + i * 0.1f;
                            xs[j] += jitter;
                            ys[j] += jitter * 0.7f;
                        }
                        continue;
                    }
                    float dist = (float) Math.sqrt(distSq);
                    float push = (minDist - dist) * 0.55f;
                    float ux = dx / dist;
                    float uy = dy / dist;
                    xs[i] -= ux * push;
                    ys[i] -= uy * push;
                    xs[j] += ux * push;
                    ys[j] += uy * push;
                }
            }
            for (int i = 0; i < n; i++) {
                clampToAnnulus(xs, ys, i, minRadius, maxRadius);
            }
        }

        List<SpellNode> resolved = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            SpellNode old = nodes.get(i);
            resolved.add(new SpellNode(old.spellId(), old.knownIndex(), old.category(), xs[i], ys[i]));
        }
        return resolved;
    }

    private static void clampToAnnulus(float[] xs, float[] ys, int i, float minR, float maxR) {
        float dist = (float) Math.hypot(xs[i], ys[i]);
        if (dist < 1.0e-4f) {
            xs[i] = minR;
            ys[i] = 0f;
            return;
        }
        float clamped = Mth.clamp(dist, minR, maxR);
        if (Math.abs(clamped - dist) > 0.01f) {
            float s = clamped / dist;
            xs[i] *= s;
            ys[i] *= s;
        }
    }

    public static String categoryLabelKey(RadialCategory category) {
        return category.langKey();
    }

    private record IndexedSpell(ResourceLocation id, int knownIndex) {}
}
