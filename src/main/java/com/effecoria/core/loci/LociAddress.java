package com.effecoria.core.loci;

import com.effecoria.core.tower.FacilityNames;
import com.effecoria.core.tower.NamedFacilityDevice;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Lex device address: {@code kind*} (all of kind) or {@code kind#name} (named facility device).
 */
public record LociAddress(String kind, boolean all, String name) {
    public static Optional<LociAddress> parse(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) {
            return Optional.empty();
        }
        if (raw.indexOf(':') >= 0) {
            // ResourceLocation word tokens are never addresses.
            return Optional.empty();
        }
        if (raw.endsWith("*") && raw.indexOf('#') < 0) {
            String kind = raw.substring(0, raw.length() - 1);
            if (isAddressableKind(kind)) {
                return Optional.of(new LociAddress(kind, true, ""));
            }
            return Optional.empty();
        }
        int hash = raw.indexOf('#');
        if (hash > 0) {
            String kind = raw.substring(0, hash);
            String label = FacilityNames.sanitize(raw.substring(hash + 1));
            if (isAddressableKind(kind) && !label.isEmpty()) {
                return Optional.of(new LociAddress(kind, false, label));
            }
        }
        return Optional.empty();
    }

    public static boolean isAddressableKind(String kind) {
        return "turret".equals(kind) || "signal".equals(kind);
    }

    /** Kind required before this actuator, or empty if the actuator rejects addresses. */
    public static Optional<String> kindForActuator(LociActuator actuator) {
        return switch (actuator) {
            case AUTONOM -> Optional.of("turret");
            case SIGNAL -> Optional.of("signal");
            case SHED, BEACON -> Optional.empty();
        };
    }

    public boolean matchesDevice(String deviceKind, NamedFacilityDevice named) {
        if (!kind.equals(deviceKind)) {
            return false;
        }
        return all || named.facilityName().equals(name);
    }

    public String serialize() {
        return all ? kind + "*" : kind + "#" + name;
    }
}
