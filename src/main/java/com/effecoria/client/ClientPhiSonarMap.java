package com.effecoria.client;

import com.effecoria.core.tower.PhiSonarService;

import javax.annotation.Nullable;
import java.util.List;

/** Client cache of the last tower Φ-sonar map payload. */
public final class ClientPhiSonarMap {
    private static int originX;
    private static int originY;
    private static int originZ;
    private static int radius;
    private static int step;
    private static int width;
    @Nullable
    private static byte[] heights;
    private static List<PhiSonarService.Blip> blips = List.of();
    private static boolean hasMap;

    private ClientPhiSonarMap() {}

    public static void accept(
            int ox,
            int oy,
            int oz,
            int r,
            int s,
            int w,
            byte[] heightBytes,
            List<PhiSonarService.Blip> mapBlips) {
        originX = ox;
        originY = oy;
        originZ = oz;
        radius = r;
        step = s;
        width = w;
        heights = heightBytes;
        blips = List.copyOf(mapBlips);
        hasMap = heightBytes != null && heightBytes.length == w * w;
    }

    public static boolean hasMap() {
        return hasMap;
    }

    public static int originX() {
        return originX;
    }

    public static int originY() {
        return originY;
    }

    public static int originZ() {
        return originZ;
    }

    public static int radius() {
        return radius;
    }

    public static int step() {
        return step;
    }

    public static int width() {
        return width;
    }

    @Nullable
    public static byte[] heights() {
        return heights;
    }

    public static List<PhiSonarService.Blip> blips() {
        return blips;
    }

    public static void clear() {
        hasMap = false;
        heights = null;
        blips = List.of();
    }
}
