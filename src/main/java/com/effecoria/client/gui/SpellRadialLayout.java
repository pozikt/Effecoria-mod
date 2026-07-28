package com.effecoria.client.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

import com.effecoria.core.magic.RadialCategory;
import com.effecoria.core.magic.SpellDefinition;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.magic.SpellRegistry;

import net.minecraft.resources.ResourceLocation;

/**
 * Pure layout / hit-test for the hold-X radial spell menu.
 * Angles: 0 rad = east (+X), increasing clockwise (screen Y-down).
 */
public final class SpellRadialLayout {
    public static final float DEAD_ZONE = 28f;
    public static final float FAVORITES_OUTER = 72f;
    public static final float RING_THICKNESS = 42f;
    public static final int FAVORITES_CAP = 5;

    public record Sector(
            ResourceLocation spellId,
            int knownIndex,
            float startAngleRad,
            float endAngleRad,
            float innerRadius,
            float outerRadius) {
        public float midAngleRad() {
            return (startAngleRad + endAngleRad) * 0.5f;
        }
    }

    public record Ring(String labelKey, int baseColorArgb, float innerRadius, float outerRadius, List<Sector> sectors) {}

    public record Layout(float deadZone, List<Ring> rings) {}

    private SpellRadialLayout() {}

    public static Layout build(PlayerPsiData data) {
        List<ResourceLocation> known = data.knownSpells();
        List<Ring> rings = new ArrayList<>();

        List<IndexedSpell> favorites = pickFavorites(data);
        if (!favorites.isEmpty()) {
            rings.add(buildRing(
                    "gui.effecoria.radial.favorites",
                    0xC8C9A227,
                    DEAD_ZONE,
                    FAVORITES_OUTER,
                    favorites));
        }

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

        float inner = favorites.isEmpty() ? DEAD_ZONE : FAVORITES_OUTER;
        for (RadialCategory category : RadialCategory.outerRingOrder()) {
            List<IndexedSpell> spells = byCategory.get(category);
            if (spells.isEmpty()) {
                continue;
            }
            float outer = inner + RING_THICKNESS;
            rings.add(buildRing(category.langKey(), colorFor(category), inner, outer, spells));
            inner = outer;
        }

        return new Layout(DEAD_ZONE, List.copyOf(rings));
    }

    public static Optional<Sector> pick(Layout layout, float dx, float dy) {
        float dist = (float) Math.hypot(dx, dy);
        if (dist < layout.deadZone()) {
            return Optional.empty();
        }
        float angle = normalizeAngle((float) Math.atan2(dy, dx));
        for (Ring ring : layout.rings()) {
            if (dist < ring.innerRadius() || dist > ring.outerRadius()) {
                continue;
            }
            for (Sector sector : ring.sectors()) {
                if (angleInSector(angle, sector.startAngleRad(), sector.endAngleRad())) {
                    return Optional.of(sector);
                }
            }
        }
        return Optional.empty();
    }

    private static List<IndexedSpell> pickFavorites(PlayerPsiData data) {
        List<ResourceLocation> known = data.knownSpells();
        List<IndexedSpell> all = new ArrayList<>(known.size());
        for (int i = 0; i < known.size(); i++) {
            all.add(new IndexedSpell(known.get(i), i));
        }
        all.sort(Comparator
                .comparingInt((IndexedSpell s) -> data.spellCastCount(s.id())).reversed()
                .thenComparing((IndexedSpell s) -> data.spellLastCastAt(s.id()), Comparator.reverseOrder())
                .thenComparingInt(IndexedSpell::knownIndex));
        if (all.size() > FAVORITES_CAP) {
            return new ArrayList<>(all.subList(0, FAVORITES_CAP));
        }
        return all;
    }

    private static Ring buildRing(
            String labelKey, int color, float inner, float outer, List<IndexedSpell> spells) {
        List<Sector> sectors = new ArrayList<>(spells.size());
        float slice = (float) (Math.PI * 2.0 / spells.size());
        for (int i = 0; i < spells.size(); i++) {
            IndexedSpell spell = spells.get(i);
            float start = normalizeAngle(i * slice);
            float end = normalizeAngle((i + 1) * slice);
            sectors.add(new Sector(spell.id(), spell.knownIndex(), start, end, inner, outer));
        }
        return new Ring(labelKey, color, inner, outer, List.copyOf(sectors));
    }

    private static int colorFor(RadialCategory category) {
        return switch (category) {
            case MOVEMENT -> 0xC83AA0C8;
            case COMBAT -> 0xC8C84A3A;
            case UTILITY -> 0xC84AB86A;
            case SEALS -> 0xC8A878C8;
        };
    }

    /** Normalize to [0, 2π). */
    public static float normalizeAngle(float angle) {
        float twoPi = (float) (Math.PI * 2.0);
        float a = angle % twoPi;
        if (a < 0f) {
            a += twoPi;
        }
        return a;
    }

    private static boolean angleInSector(float angle, float start, float end) {
        if (start <= end) {
            return angle >= start && angle < end;
        }
        // wraps around 2π
        return angle >= start || angle < end;
    }

    private record IndexedSpell(ResourceLocation id, int knownIndex) {}
}
