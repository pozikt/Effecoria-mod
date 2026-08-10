package com.effecoria.core.technomagic;

/** Technomagic progression eras (I–V). */
public enum TechnomagicEra {
    I(1),
    II(2),
    III(3),
    IV(4),
    V(5);

    private final int number;

    TechnomagicEra(int number) {
        this.number = number;
    }

    public int number() {
        return number;
    }

    public String translationKey() {
        return "technomagic.effecoria.era." + number;
    }

    public static TechnomagicEra fromNumber(int n) {
        for (TechnomagicEra era : values()) {
            if (era.number == n) {
                return era;
            }
        }
        throw new IllegalArgumentException("Unknown technomagic era: " + n);
    }
}
