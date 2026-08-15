package com.effecoria.core.fabricator;

/** Fabricator machine tier (Lonver Class IV out of scope). */
public enum FabricatorClass {
    I(1),
    II(2),
    III(3);

    private final int level;

    FabricatorClass(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }

    public boolean supports(int minClass) {
        return level >= minClass;
    }
}
