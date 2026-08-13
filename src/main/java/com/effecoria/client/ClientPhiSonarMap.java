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
    private static int modeId;
    @Nullable
    private static byte[] heights;
    @Nullable
    private static byte[] terrain;
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
            int mode,
            byte[] heightBytes,
            byte[] terrainBytes,
            List<PhiSonarService.Blip> mapBlips) {
        originX = ox;
        originY = oy;
        originZ = oz;
        radius = r;
        step = s;
        width = w;
        modeId = mode;
        heights = heightBytes;
        terrain = terrainBytes;
        blips = List.copyOf(mapBlips);
        hasMap = heightBytes != null
                && terrainBytes != null
                && heightBytes.length == w * w
                && terrainBytes.length == w * w;
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

    public static int modeId() {
        return modeId;
    }

    @Nullable
    public static byte[] heights() {
        return heights;
    }

    @Nullable
    public static byte[] terrain() {
        return terrain;
    }

    public static List<PhiSonarService.Blip> blips() {
        return blips;
    }

    public static void clear() {
        hasMap = false;
        heights = null;
        terrain = null;
        blips = List.of();
        modeId = 0;
    }
}
