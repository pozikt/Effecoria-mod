package com.effecoria.core.progression;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.psi.PlayerPsiData;

import net.minecraft.network.chat.Component;

/**
 * Expandable Magic Primer chapters. Bit indices are stable — append new chapters only at the end.
 */
public final class PrimerChapters {
    public enum Chapter {
        CAST_LOOP(0),
        PSI_PHI(1),
        ENTROPY(2),
        BREATHING(3),
        HUB_KEYS(4),
        SEALS(5),
        SCHOOL(6),
        MENTAL(7),
        ORKANUM(8);

        private final int bitIndex;

        Chapter(int bitIndex) {
            this.bitIndex = bitIndex;
        }

        public int bitIndex() {
            return bitIndex;
        }

        public int mask() {
            return 1 << bitIndex;
        }

        public String id() {
            return name().toLowerCase();
        }

        public Component title() {
            return Component.translatable("guide.effecoria.chapter." + id());
        }

        public Component body() {
            return Component.translatable("guide.effecoria.body." + id());
        }
    }

    private PrimerChapters() {}

    public static boolean isSeen(PlayerPsiData data, Chapter chapter) {
        return (data.primerSeenMask() & chapter.mask()) != 0;
    }

    public static boolean hasUnseen(PlayerPsiData data) {
        for (Chapter chapter : visible(data)) {
            if (!isSeen(data, chapter)) {
                return true;
            }
        }
        return false;
    }

    public static List<Chapter> visible(PlayerPsiData data) {
        List<Chapter> out = new ArrayList<>();
        for (Chapter chapter : Chapter.values()) {
            if (visibility(chapter).test(data)) {
                out.add(chapter);
            }
        }
        return out;
    }

    public static Chapter byBitIndex(int bitIndex) {
        for (Chapter chapter : Chapter.values()) {
            if (chapter.bitIndex == bitIndex) {
                return chapter;
            }
        }
        return null;
    }

    private static Predicate<PlayerPsiData> visibility(Chapter chapter) {
        if (chapter == Chapter.SEALS) {
            return data -> !data.initiated() || data.school() == MagicSchool.SEALS;
        }
        if (chapter == Chapter.MENTAL) {
            return data -> !data.initiated() || data.school() == MagicSchool.MENTAL;
        }
        return data -> true;
    }
}
