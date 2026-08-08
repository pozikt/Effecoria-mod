package com.effecoria.armor;

import java.util.Locale;
import java.util.Optional;

/** Seal Φ-phonemes written onto essonite armor via CustomData. */
public enum EssonitePhoneme {
    FIRMITAS,
    UMBRA,
    ABNEGATIO,
    SERVARE,
    CLAUSURA;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<EssonitePhoneme> fromId(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
