package com.effecoria.core.loci;

import com.effecoria.core.circuit.PhiChannel;
import com.effecoria.core.tower.FacilityNames;
import com.effecoria.core.tower.NamedFacilityDevice;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Lex address: {@code kind*} / {@code kind#name} for devices, or {@code bus:channel} /
 * {@code шина:channel} for Φ-buses.
 */
public record LociAddress(String kind, boolean all, String name) {
    public static final String KIND_BUS = "bus";

    public static Optional<LociAddress> parse(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) {
            return Optional.empty();
        }
        Optional<LociAddress> bus = parseBus(raw);
        if (bus.isPresent()) {
            return bus;
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

    private static Optional<LociAddress> parseBus(String raw) {
        String channelName = null;
        if (raw.startsWith("bus:")) {
            channelName = raw.substring(4);
        } else if (raw.startsWith("шина:")) {
            channelName = raw.substring("шина:".length());
        }
        if (channelName == null || channelName.isEmpty()) {
            return Optional.empty();
        }
        if (resolveChannel(channelName).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new LociAddress(KIND_BUS, false, channelName));
    }

    public static Optional<PhiChannel> resolveChannel(String channelName) {
        if (channelName == null || channelName.isEmpty()) {
            return Optional.empty();
        }
        for (PhiChannel channel : PhiChannel.values()) {
            if (channel.getSerializedName().equals(channelName)) {
                return Optional.of(channel);
            }
        }
        return Optional.empty();
    }

    public static boolean isAddressableKind(String kind) {
        return "turret".equals(kind) || "signal".equals(kind);
    }

    /**
     * Kind required when an address precedes this actuator. Empty = addresses forbidden.
     * SHED accepts only {@code bus}; null target (bare SHED) is always allowed by the compiler.
     */
    public static Optional<String> kindForActuator(LociActuator actuator) {
        return switch (actuator) {
            case AUTONOM, ARM, DISARM -> Optional.of("turret");
            case SIGNAL -> Optional.of("signal");
            case SHED -> Optional.of(KIND_BUS);
            case BEACON -> Optional.empty();
        };
    }

    public boolean isBus() {
        return KIND_BUS.equals(kind);
    }

    public Optional<PhiChannel> phiChannel() {
        return isBus() ? resolveChannel(name) : Optional.empty();
    }

    public boolean matchesDevice(String deviceKind, NamedFacilityDevice named) {
        if (isBus() || !kind.equals(deviceKind)) {
            return false;
        }
        return all || named.facilityName().equals(name);
    }

    public String serialize() {
        if (isBus()) {
            return "шина:" + name;
        }
        return all ? kind + "*" : kind + "#" + name;
    }
}
