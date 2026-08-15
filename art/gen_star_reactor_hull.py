#!/usr/bin/env python3
"""Bake Star Reactor BER hull textures from concept (tower cyan + star glyph)."""
from __future__ import annotations

import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src/main/resources/assets/effecoria/textures/block"
ART = ROOT / "art/star_reactor"
SIZE = 64

NAVY = (18, 28, 40, 255)
SLATE = (34, 48, 62, 255)
METAL = (148, 168, 184, 255)
METAL_HI = (210, 224, 236, 255)
METAL_LO = (90, 108, 124, 255)
OBSID = (32, 18, 42, 255)
OBSID_GLOW = (120, 70, 160, 255)
GLASS = (28, 58, 78, 255)
CYAN = (70, 190, 230, 255)
CYAN_HI = (190, 236, 255, 255)
GOLD = (240, 200, 72, 255)
CORE = (255, 250, 230, 255)
SEAM = (48, 110, 140, 255)
CIRCUIT = (42, 96, 120, 255)


def put(px, x, y, c):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        px[x, y] = c


def fill_rect(px, x0, y0, x1, y1, c):
    for y in range(y0, y1):
        for x in range(x0, x1):
            put(px, x, y, c)


def mix(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3)) + (255,)


def draw_hull(px, lit: bool):
    fill_rect(px, 0, 0, SIZE, SIZE, NAVY)
    # silver frame band
    fill_rect(px, 4, 4, SIZE - 4, SIZE - 4, METAL_LO)
    fill_rect(px, 6, 6, SIZE - 6, SIZE - 6, METAL)
    fill_rect(px, 8, 8, SIZE - 8, SIZE - 8, SLATE)
    # glass face
    fill_rect(px, 12, 12, SIZE - 12, SIZE - 12, GLASS)
    # circuit dots
    for y in range(14, SIZE - 14, 3):
        for x in range(14, SIZE - 14, 3):
            if (x + y) % 6 == 0:
                put(px, x, y, CIRCUIT if not lit else SEAM)
    # cyan face frame
    for i in range(12, SIZE - 12):
        put(px, i, 12, CYAN if lit else SEAM)
        put(px, i, SIZE - 13, CYAN if lit else SEAM)
        put(px, 12, i, CYAN if lit else SEAM)
        put(px, SIZE - 13, i, CYAN if lit else SEAM)
    # vertical conduit accents mid-sides
    for y in range(16, SIZE - 16):
        put(px, 5, y, CYAN_HI if lit else CYAN)
        put(px, SIZE - 6, y, CYAN_HI if lit else CYAN)
        put(px, y, 5, METAL_HI)
        put(px, y, SIZE - 6, METAL_LO)
    # corner caps + purple star
    for x0, y0 in ((0, 0), (SIZE - 10, 0), (0, SIZE - 10), (SIZE - 10, SIZE - 10)):
        fill_rect(px, x0, y0, x0 + 10, y0 + 10, OBSID)
        cx, cy = x0 + 5, y0 + 5
        col = OBSID_GLOW if lit else (70, 40, 95, 255)
        for d in range(-3, 4):
            put(px, cx + d, cy, col)
            put(px, cx, cy + d, col)
        put(px, cx, cy, CYAN_HI if lit else METAL_HI)
    # rivets on metal
    for x in (7, SIZE // 2, SIZE - 8):
        put(px, x, 7, METAL_HI)
        put(px, x, SIZE - 8, METAL_HI)
        put(px, 7, x, METAL_HI)
        put(px, SIZE - 8, x, METAL_HI)


def draw_star(px, lit: bool):
    cx = cy = SIZE // 2
    for y in range(SIZE):
        for x in range(SIZE):
            dx, dy = x - cx, y - cy
            d2 = dx * dx + dy * dy
            if d2 <= 9 * 9:
                put(px, x, y, CYAN_HI if lit else CYAN)
            elif d2 <= 14 * 14:
                put(px, x, y, mix(GLASS, CYAN, 0.55 if lit else 0.35))
    arm = CYAN_HI if lit else CYAN
    for d in range(-20, 21):
        put(px, cx + d, cy, arm)
        put(px, cx, cy + d, arm)
        if abs(d) <= 14:
            put(px, cx + d, cy + d, arm)
            put(px, cx + d, cy - d, arm)
    put(px, cx, cy, CORE if lit else GOLD)
    for o in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, 1), (1, -1), (-1, -1)):
        put(px, cx + o[0], cy + o[1], GOLD if lit else CYAN_HI)
    if lit:
        for a in range(0, 360, 3):
            r = 18
            x = int(cx + math.cos(math.radians(a)) * r)
            y = int(cy + math.sin(math.radians(a)) * r)
            put(px, x, y, GOLD)


def bake(lit: bool) -> Image.Image:
    im = Image.new("RGBA", (SIZE, SIZE), NAVY)
    px = im.load()
    draw_hull(px, lit)
    draw_star(px, lit)
    return im


def bake_side(lit: bool) -> Image.Image:
    im = Image.new("RGBA", (16, 16), SLATE)
    px = im.load()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                px[x, y] = METAL
            elif x in (1, 14) or y in (1, 14):
                px[x, y] = METAL_LO
            elif 5 <= x <= 10 and 5 <= y <= 10:
                px[x, y] = CYAN_HI if lit else CYAN
            elif x in (3, 12) or y in (3, 12):
                px[x, y] = SEAM
    if lit:
        px[8, 8] = CORE
        for d in range(-3, 4):
            px[8 + d, 8] = GOLD
            px[8, 8 + d] = GOLD
    return im


def main():
    ART.mkdir(parents=True, exist_ok=True)
    off = bake(False)
    on = bake(True)
    off.save(OUT / "star_reactor_hull.png")
    on.save(OUT / "star_reactor_hull_on.png")
    off.resize((SIZE * 4, SIZE * 4), Image.NEAREST).save(ART / "star_reactor_hull_preview.png")
    on.resize((SIZE * 4, SIZE * 4), Image.NEAREST).save(ART / "star_reactor_hull_on_preview.png")
    bake_side(False).save(OUT / "star_reactor_side.png")
    bake_side(True).save(OUT / "star_reactor_side_on.png")
    top = bake_side(True)
    tp = top.load()
    for i in range(16):
        tp[i, 8] = GOLD
        tp[8, i] = GOLD
    tp[8, 8] = CORE
    top.save(OUT / "star_reactor_top.png")
    print("ok")


if __name__ == "__main__":
    main()
