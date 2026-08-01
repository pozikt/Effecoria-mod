package com.effecoria.core.seal;

/** Lexical role of a seal programming word. */
public enum SealWordKind {
    /** Magnitude / duration value. */
    NUMBER,
    /** Always-on or reactive block property (hardness, glow). */
    PROPERTY,
    /** Reactive or passive action (hurt, sound, push…). */
    TRIGGER,
    /** Material / loudness / sound flavour. */
    MODIFIER,
    /** Event reader that emits a unit pulse when matched. */
    SENSE,
    /** Narrows the preceding sense (player, step, hit…). */
    SPEC,
    /** Opens a duration clause ({@code time} + NUMBER). */
    DURATION;

    public static SealWordKind fromSerialized(String raw) {
        return switch (raw.toLowerCase()) {
            case "number" -> NUMBER;
            case "property" -> PROPERTY;
            case "trigger" -> TRIGGER;
            case "modifier" -> MODIFIER;
            case "sense" -> SENSE;
            case "spec" -> SPEC;
            case "duration" -> DURATION;
            default -> throw new IllegalArgumentException("Unknown seal word kind: " + raw);
        };
    }

    public String serializedName() {
        return name().toLowerCase();
    }

    /** Words that can be inscribed as block effects (passive or after a sense). */
    public boolean isAction() {
        return this == PROPERTY || this == TRIGGER;
    }
}
