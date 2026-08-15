package com.effecoria.core.tower;

/**
 * Facility block entity that can carry a Lex Loci label ({@code kind#name} in the console).
 */
public interface NamedFacilityDevice {
    String facilityName();

    /** Trim / clamp; empty clears the name. Returns false if rejected (e.g. beacon name taken). */
    boolean setFacilityName(String name);
}
