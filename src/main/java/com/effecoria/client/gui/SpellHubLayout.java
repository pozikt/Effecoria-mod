package com.effecoria.client.gui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

import com.effecoria.core.magic.RadialCategory;
import com.effecoria.core.magic.SpellDefinition;
import com.effecoria.core.progression.SpellUnlockService;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.magic.SpellRegistry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Constellation layout: player core at center, spells on orbits grouped by category.
 * Dense categories get wider wedges; rings never collapse onto the same radius.
 */
public final class SpellHubLayout {
    public static final float BASE_CORE_RADIUS = 34f;
    public static final float BASE_MIN_ORBIT = 78f;
    public static final float BASE_ORBIT_STEP = 34f;
    /** Fixed orbit for the breathing-train node (left of core). */
    public static final float BASE_TRAIN_ORBIT = 92f;
    private static final float NODE_GAP = 10f;
    /** Keep a clear pie slice around the train node (screen left). */
    private static final float TRAIN_GAP = (float) (Math.PI * 0.42);
    private static final float TRAIN_ANGLE = (float) Math.PI;
    private static final int SEPARATION_ITERATIONS = 28;
    private static final float MIN_CATEGORY_SHARE = 0.12f;

    public record SpellNode(
            ResourceLocation spellId,
            int knownIndex,
            RadialCategory category,
            float offsetX,
            float offsetY,
            boolean locked) {}

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
        float nodeRadius = Math.max(12f, 16f * scale);
        float minOrbit = BASE_MIN_ORBIT * scale;
        float orbitStep = Math.max(BASE_ORBIT_STEP * scale, minCenterDistance(nodeRadius));
        float screenCap = Math.min(screenWidth, screenHeight) * 0.46f;
        float trainOrbit = Math.min(BASE_TRAIN_ORBIT * scale, screenCap);
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
            byCategory.get(def.get().radialCategory()).add(new IndexedSpell(id, i, false));
        }

        for (ResourceLocation id : SpellUnlockService.upcomingLocked(data, 4)) {
            Optional<SpellDefinition> def = SpellRegistry.get(id);
            if (def.isEmpty()) {
                continue;
            }
            byCategory.get(def.get().radialCategory()).add(new IndexedSpell(id, -1, true));
        }

        List<RadialCategory> active = new ArrayList<>();
        int totalSpells = 0;
        for (RadialCategory category : RadialCategory.outerRingOrder()) {
            List<IndexedSpell> spells = byCategory.get(category);
            if (!spells.isEmpty()) {
                active.add(category);
                totalSpells += spells.size();
            }
        }

        float[] shares = proportionalShares(active, byCategory, totalSpells);
        float usable = (float) (Math.PI * 2.0) - TRAIN_GAP;
        float cursor = normalizeAngle(TRAIN_ANGLE + TRAIN_GAP * 0.5f);

        // Grow outer radius so the densest category can spread without stacking rings.
        float maxOrbit = minOrbit;
        for (int i = 0; i < active.size(); i++) {
            float span = usable * shares[i];
            int count = byCategory.get(active.get(i)).size();
            maxOrbit = Math.max(maxOrbit, requiredOuterOrbit(count, span, minOrbit, orbitStep, nodeRadius));
        }
        maxOrbit = Math.min(maxOrbit, screenCap);

        List<SpellNode> nodes = new ArrayList<>();
        for (int i = 0; i < active.size(); i++) {
            RadialCategory category = active.get(i);
            float span = usable * shares[i];
            float baseAngle = normalizeAngle(cursor + span * 0.5f);
            placeCategoryFan(
                    nodes,
                    byCategory.get(category),
                    category,
                    baseAngle,
                    span,
                    minOrbit,
                    orbitStep,
                    maxOrbit,
                    nodeRadius);
            cursor = normalizeAngle(cursor + span);
        }

        float annulusMin = coreRadius + nodeRadius + 6f;
        nodes = separateNodes(nodes, nodeRadius, annulusMin, maxOrbit);
        nodes = pushAwayFromTrain(nodes, trainNode, nodeRadius);
        // Train push can reintroduce overlaps — resolve again within a slightly wider cap.
        nodes = separateNodes(nodes, nodeRadius, annulusMin, Math.min(screenCap, maxOrbit + orbitStep * 0.5f));
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

    private static float[] proportionalShares(
            List<RadialCategory> active,
            EnumMap<RadialCategory, List<IndexedSpell>> byCategory,
            int totalSpells) {
        float[] shares = new float[active.size()];
        if (active.isEmpty()) {
            return shares;
        }
        if (active.size() == 1) {
            shares[0] = 1f;
            return shares;
        }
        float rawSum = 0f;
        for (int i = 0; i < active.size(); i++) {
            float share = Math.max(MIN_CATEGORY_SHARE, byCategory.get(active.get(i)).size() / (float) Math.max(1, totalSpells));
            shares[i] = share;
            rawSum += share;
        }
        for (int i = 0; i < shares.length; i++) {
            shares[i] /= rawSum;
        }
        return shares;
    }

    /** Outer orbit needed so every spell fits without stacking rings at the same radius. */
    private static float requiredOuterOrbit(
            int count, float span, float minOrbit, float orbitStep, float nodeRadius) {
        float minDist = minCenterDistance(nodeRadius);
        int remaining = count;
        int ring = 0;
        float orbit = minOrbit;
        while (remaining > 0 && ring < 12) {
            orbit = minOrbit + ring * orbitStep;
            int capacity = Math.max(1, maxNodesOnRing(orbit, span, minDist));
            remaining -= capacity;
            ring++;
        }
        return orbit;
    }

    private static List<SpellNode> pushAwayFromTrain(List<SpellNode> nodes, TrainNode train, float nodeRadius) {
        float minDist = train.radius() + nodeRadius + NODE_GAP;
        float minDistSq = minDist * minDist;
        List<SpellNode> out = new ArrayList<>(nodes.size());
        for (SpellNode node : nodes) {
            float dx = node.offsetX() - train.offsetX();
            float dy = node.offsetY() - train.offsetY();
            float distSq = dx * dx + dy * dy;
            if (distSq >= minDistSq) {
                out.add(node);
                continue;
            }
            if (distSq < 1.0E-4f) {
                // Nudge along the train gap edge rather than stacking on the train.
                float edge = TRAIN_ANGLE + TRAIN_GAP * 0.55f;
                out.add(new SpellNode(
                        node.spellId(),
                        node.knownIndex(),
                        node.category(),
                        train.offsetX() + (float) Math.cos(edge) * minDist,
                        train.offsetY() + (float) Math.sin(edge) * minDist,
                        node.locked()));
                continue;
            }
            float dist = (float) Math.sqrt(distSq);
            float scale = minDist / dist;
            out.add(new SpellNode(
                    node.spellId(),
                    node.knownIndex(),
                    node.category(),
                    train.offsetX() + dx * scale,
                    train.offsetY() + dy * scale,
                    node.locked()));
        }
        return out;
    }

    private static float minCenterDistance(float nodeRadius) {
        return nodeRadius * 2f + NODE_GAP;
    }

    private static float normalizeAngle(float angle) {
        float twoPi = (float) (Math.PI * 2.0);
        float a = angle % twoPi;
        if (a < 0f) {
            a += twoPi;
        }
        return a;
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

        while (index < count && ring < 12) {
            float orbit = Math.min(minOrbit + ring * orbitStep, maxOrbit);
            // If we have already hit the outer cap, keep walking outward in tiny steps
            // so remaining spells are not forced onto the exact same circle.
            if (ring > 0 && orbit >= maxOrbit - 0.5f) {
                orbit = Math.min(maxOrbit + (ring - estimatedRingsToCap(minOrbit, orbitStep, maxOrbit)) * (orbitStep * 0.55f),
                        maxOrbit + orbitStep);
            }

            int capacity = maxNodesOnRing(orbit, span, minDist);
            // Never place more than capacity; if capacity is 1 forever, still advance radius.
            int remaining = count - index;
            int onRing = Math.min(Math.max(1, capacity), remaining);

            for (int j = 0; j < onRing; j++) {
                IndexedSpell spell = spells.get(index++);
                float t = onRing == 1 ? 0.5f : j / (float) (onRing - 1);
                // Inset slightly from wedge edges so neighboring categories do not kiss.
                float edgePad = onRing == 1 ? 0f : 0.08f;
                float localSpan = span * (1f - edgePad);
                float angle = baseAngle - localSpan * 0.5f + t * localSpan;
                float nx = (float) Math.cos(angle) * orbit;
                float ny = (float) Math.sin(angle) * orbit;
                nodes.add(new SpellNode(spell.id(), spell.knownIndex(), category, nx, ny, spell.locked()));
            }
            ring++;
        }
    }

    private static int estimatedRingsToCap(float minOrbit, float orbitStep, float maxOrbit) {
        if (orbitStep < 1f) {
            return 0;
        }
        return Math.max(0, (int) Math.floor((maxOrbit - minOrbit) / orbitStep));
    }

    /** How many nodes fit on one ring without overlapping (adjacent chord spacing). */
    private static int maxNodesOnRing(float orbit, float span, float minDist) {
        if (orbit < 1f || span < 0.01f) {
            return 1;
        }
        // For n>=2 equally spaced on an arc: chord = 2 r sin(Δθ/2), Δθ = span/(n-1).
        int best = 1;
        for (int n = 2; n <= 24; n++) {
            float delta = span / (n - 1);
            float chord = 2f * orbit * (float) Math.sin(delta * 0.5);
            if (chord + 0.01f < minDist) {
                break;
            }
            best = n;
        }
        return best;
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
                    if (distSq >= minDistSq) {
                        continue;
                    }
                    if (distSq < 1.0e-4f) {
                        float jitter = 0.8f + i * 0.15f;
                        xs[j] += jitter;
                        ys[j] += jitter * 0.6f;
                        continue;
                    }
                    float dist = (float) Math.sqrt(distSq);
                    float push = (minDist - dist) * 0.62f;
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
            resolved.add(new SpellNode(old.spellId(), old.knownIndex(), old.category(), xs[i], ys[i], old.locked()));
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

    private record IndexedSpell(ResourceLocation id, int knownIndex, boolean locked) {}
}
