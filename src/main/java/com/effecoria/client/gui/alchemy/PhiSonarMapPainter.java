package com.effecoria.client.gui.alchemy;

import com.effecoria.client.ClientPhiSonarMap;
import com.effecoria.core.tower.PhiSonarService;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Shared Φ-sonar map painter: terrain-colored cells, height shading, blips, zoom/pan.
 */
public final class PhiSonarMapPainter {
    public static final int BLIP_LIVING = 0xFF55FF88;
    public static final int BLIP_UNDEAD = 0xFFC080FF;
    public static final int BLIP_PLAYER = 0xFFFFD060;
    public static final int BLIP_GEYSER = 0xFF40E0FF;
    public static final int BLIP_OMEGA = 0xFFAA40FF;
    public static final int CROSSHAIR = 0xFFFF6060;
    public static final int FRAME = 0xFF3A5A70;
    public static final int BG = 0xFF162430;
    public static final int LABEL = 0xFFD0D8E0;
    public static final int MUTED = 0xFF8899AA;

    private float zoom = 1f;
    private float panX;
    private float panY;
    private boolean dragging;
    private double dragLastX;
    private double dragLastY;

    public float zoom() {
        return zoom;
    }

    public void resetView() {
        zoom = 1f;
        panX = 0f;
        panY = 0f;
    }

    public boolean mouseScrolled(int mapX, int mapY, int mapSize, double mouseX, double mouseY, double scrollY) {
        if (mouseX < mapX || mouseX >= mapX + mapSize || mouseY < mapY || mouseY >= mapY + mapSize) {
            return false;
        }
        if (!ClientPhiSonarMap.hasMap()) {
            return false;
        }
        float old = zoom;
        float next = (float) (zoom * (scrollY > 0 ? 1.15 : 1 / 1.15));
        zoom = Math.max(1f, Math.min(8f, next));
        if (zoom == old) {
            return true;
        }
        // Keep world point under cursor stable
        float u = (float) ((mouseX - mapX) / (double) mapSize);
        float v = (float) ((mouseY - mapY) / (double) mapSize);
        float worldU = panX + u / old;
        float worldV = panY + v / old;
        panX = worldU - u / zoom;
        panY = worldV - v / zoom;
        clampPan();
        return true;
    }

    public boolean mouseClicked(int mapX, int mapY, int mapSize, double mouseX, double mouseY, int button) {
        if (button == 0
                && mouseX >= mapX
                && mouseX < mapX + mapSize
                && mouseY >= mapY
                && mouseY < mapY + mapSize
                && ClientPhiSonarMap.hasMap()) {
            dragging = true;
            dragLastX = mouseX;
            dragLastY = mouseY;
            return true;
        }
        return false;
    }

    public boolean mouseReleased(int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(int mapSize, double mouseX, double mouseY, int button) {
        if (!dragging || button != 0 || !ClientPhiSonarMap.hasMap()) {
            return false;
        }
        float du = (float) ((mouseX - dragLastX) / mapSize);
        float dv = (float) ((mouseY - dragLastY) / mapSize);
        panX -= du / zoom;
        panY -= dv / zoom;
        dragLastX = mouseX;
        dragLastY = mouseY;
        clampPan();
        return true;
    }

    private void clampPan() {
        float max = Math.max(0f, 1f - 1f / zoom);
        panX = Math.max(0f, Math.min(max, panX));
        panY = Math.max(0f, Math.min(max, panY));
    }

    public void draw(
            GuiGraphics graphics,
            Font font,
            int mapX,
            int mapY,
            int mapSize,
            int mouseX,
            int mouseY,
            int footerX,
            int footerY) {
        graphics.fill(mapX - 2, mapY - 2, mapX + mapSize + 2, mapY + mapSize + 2, FRAME);
        graphics.fill(mapX, mapY, mapX + mapSize, mapY + mapSize, BG);

        if (!ClientPhiSonarMap.hasMap()) {
            return;
        }

        byte[] heights = ClientPhiSonarMap.heights();
        byte[] terrain = ClientPhiSonarMap.terrain();
        int width = ClientPhiSonarMap.width();
        if (heights == null || terrain == null || width <= 0) {
            return;
        }

        int minH = 127;
        int maxH = -128;
        boolean anyInside = false;
        for (int idx = 0; idx < heights.length; idx++) {
            if (terrain[idx] == PhiSonarService.TERRAIN_OUTSIDE) {
                continue;
            }
            anyInside = true;
            minH = Math.min(minH, heights[idx]);
            maxH = Math.max(maxH, heights[idx]);
        }
        if (!anyInside) {
            minH = 0;
            maxH = 0;
        }
        int span = Math.max(1, maxH - minH);

        float view = 1f / zoom;
        float u0 = panX;
        float v0 = panY;
        float u1 = panX + view;
        float v1 = panY + view;

        int ix0 = Math.max(0, (int) Math.floor(u0 * width));
        int iz0 = Math.max(0, (int) Math.floor(v0 * width));
        int ix1 = Math.min(width, (int) Math.ceil(u1 * width));
        int iz1 = Math.min(width, (int) Math.ceil(v1 * width));

        for (int iz = iz0; iz < iz1; iz++) {
            for (int ix = ix0; ix < ix1; ix++) {
                int idx = iz * width + ix;
                if (terrain[idx] == PhiSonarService.TERRAIN_OUTSIDE) {
                    continue;
                }
                float cellU0 = ix / (float) width;
                float cellV0 = iz / (float) width;
                float cellU1 = (ix + 1) / (float) width;
                float cellV1 = (iz + 1) / (float) width;
                int x0 = mapX + Math.round((cellU0 - u0) / view * mapSize);
                int y0 = mapY + Math.round((cellV0 - v0) / view * mapSize);
                int x1 = mapX + Math.round((cellU1 - u0) / view * mapSize);
                int y1 = mapY + Math.round((cellV1 - v0) / view * mapSize);
                if (x1 <= mapX || y1 <= mapY || x0 >= mapX + mapSize || y0 >= mapY + mapSize) {
                    continue;
                }
                x0 = Math.max(mapX, x0);
                y0 = Math.max(mapY, y0);
                x1 = Math.min(mapX + mapSize, x1);
                y1 = Math.min(mapY + mapSize, y1);
                float t = (heights[idx] - minH) / (float) span;
                int color = shade(terrainColor(terrain[idx]), t);
                graphics.fill(x0, y0, Math.max(x0 + 1, x1), Math.max(y0 + 1, y1), color);
            }
        }

        // Origin crosshair
        float originU = 0.5f;
        float originV = 0.5f;
        if (originU >= u0 && originU <= u1 && originV >= v0 && originV <= v1) {
            int cx = mapX + Math.round((originU - u0) / view * mapSize);
            int cy = mapY + Math.round((originV - v0) / view * mapSize);
            graphics.fill(cx - 2, cy, cx + 3, cy + 1, CROSSHAIR);
            graphics.fill(cx, cy - 2, cx + 1, cy + 3, CROSSHAIR);
        }

        int radius = ClientPhiSonarMap.radius();
        for (PhiSonarService.Blip blip : ClientPhiSonarMap.blips()) {
            float bu = 0.5f + blip.relX() / (float) (radius * 2 + 1);
            float bv = 0.5f + blip.relZ() / (float) (radius * 2 + 1);
            if (bu < u0 || bu > u1 || bv < v0 || bv > v1) {
                continue;
            }
            int px = mapX + Math.round((bu - u0) / view * mapSize);
            int pz = mapY + Math.round((bv - v0) / view * mapSize);
            int color = switch (blip.kind()) {
                case PhiSonarService.BLIP_PLAYER -> BLIP_PLAYER;
                case PhiSonarService.BLIP_UNDEAD -> BLIP_UNDEAD;
                case PhiSonarService.BLIP_GEYSER -> BLIP_GEYSER;
                case PhiSonarService.BLIP_OMEGA -> BLIP_OMEGA;
                default -> BLIP_LIVING;
            };
            graphics.fill(px - 1, pz - 1, px + 2, pz + 2, color);
        }

        if (mouseX >= mapX && mouseX < mapX + mapSize && mouseY >= mapY && mouseY < mapY + mapSize) {
            float u = u0 + (mouseX - mapX) / (float) mapSize * view;
            float v = v0 + (mouseY - mapY) / (float) mapSize * view;
            int gj = Math.min(width - 1, Math.max(0, (int) (u * width)));
            int gi = Math.min(width - 1, Math.max(0, (int) (v * width)));
            int relX = Math.round((u - 0.5f) * 2f * radius);
            int relZ = Math.round((v - 0.5f) * 2f * radius);
            int worldX = ClientPhiSonarMap.originX() + relX;
            int worldZ = ClientPhiSonarMap.originZ() + relZ;
            int surface = ClientPhiSonarMap.originY() + heights[gi * width + gj];
            byte kind = terrain[gi * width + gj];
            if (kind == PhiSonarService.TERRAIN_OUTSIDE) {
                graphics.drawString(
                        font,
                        Component.translatable("gui.effecoria.phi_sonar.outside"),
                        footerX,
                        footerY,
                        MUTED,
                        false);
            } else {
                graphics.drawString(
                        font,
                        Component.translatable(
                                "gui.effecoria.phi_sonar.cursor",
                                worldX,
                                surface,
                                worldZ,
                                Component.translatable("gui.effecoria.phi_sonar.terrain." + terrainKey(kind))),
                        footerX,
                        footerY,
                        LABEL,
                        false);
            }
        } else {
            PhiSonarService.Mode mode = PhiSonarService.Mode.fromId(ClientPhiSonarMap.modeId());
            graphics.drawString(
                    font,
                    Component.translatable(
                            "gui.effecoria.phi_sonar.origin",
                            ClientPhiSonarMap.originX(),
                            ClientPhiSonarMap.originY(),
                            ClientPhiSonarMap.originZ(),
                            Component.translatable("gui.effecoria.phi_sonar.mode." + mode.name().toLowerCase()),
                            String.format("%.1fx", zoom)),
                    footerX,
                    footerY,
                    MUTED,
                    false);
        }
    }

    private static String terrainKey(byte kind) {
        return switch (kind) {
            case PhiSonarService.TERRAIN_WATER -> "water";
            case PhiSonarService.TERRAIN_STONE -> "stone";
            case PhiSonarService.TERRAIN_FOLIAGE -> "foliage";
            case PhiSonarService.TERRAIN_SAND -> "sand";
            case PhiSonarService.TERRAIN_ESSONITE -> "essonite";
            case PhiSonarService.TERRAIN_MITHRIL -> "mithril";
            case PhiSonarService.TERRAIN_OMEGA -> "omega";
            case PhiSonarService.TERRAIN_GEYSER -> "geyser";
            case PhiSonarService.TERRAIN_SHIELD -> "shield";
            case PhiSonarService.TERRAIN_CAVE -> "cave";
            case PhiSonarService.TERRAIN_METAL -> "metal";
            case PhiSonarService.TERRAIN_OUTSIDE -> "outside";
            default -> "ground";
        };
    }

    private static int terrainColor(byte kind) {
        return switch (kind) {
            case PhiSonarService.TERRAIN_WATER -> 0xFF2A6A9A;
            case PhiSonarService.TERRAIN_STONE -> 0xFF5A6670;
            case PhiSonarService.TERRAIN_FOLIAGE -> 0xFF2F7A48;
            case PhiSonarService.TERRAIN_SAND -> 0xFFC2B06A;
            case PhiSonarService.TERRAIN_ESSONITE -> 0xFF3AD0E8;
            case PhiSonarService.TERRAIN_MITHRIL -> 0xFF7EC8FF;
            case PhiSonarService.TERRAIN_OMEGA -> 0xFF6A2A9A;
            case PhiSonarService.TERRAIN_GEYSER -> 0xFF40E8FF;
            case PhiSonarService.TERRAIN_SHIELD -> 0xFFD0B040;
            case PhiSonarService.TERRAIN_CAVE -> 0xFF1A2030;
            case PhiSonarService.TERRAIN_METAL -> 0xFF8A9AAA;
            case PhiSonarService.TERRAIN_OUTSIDE -> 0xFF0A1018;
            default -> 0xFF3A5A48; // ground / grass
        };
    }

    /** Mix terrain base with height shade (dark low → pale high). */
    private static int shade(int base, float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        int r = (base >> 16) & 0xFF;
        int g = (base >> 8) & 0xFF;
        int b = base & 0xFF;
        float factor = 0.55f + 0.55f * clamped;
        r = Math.min(255, Math.round(r * factor));
        g = Math.min(255, Math.round(g * factor));
        b = Math.min(255, Math.round(b * factor));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
