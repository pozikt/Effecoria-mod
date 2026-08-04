#!/usr/bin/env python3
"""Circular pixel-art spell icons for Effecoria (RPG reference style)."""
from __future__ import annotations

import math
from dataclasses import dataclass
from typing import Callable

from PIL import Image, ImageDraw

ICON = 64


@dataclass(frozen=True)
class Pal:
    """Full school look: frame + filled scene + glyph hues."""

    frame: tuple[int, int, int]
    frame_hi: tuple[int, int, int]
    frame_lo: tuple[int, int, int]
    bg_center: tuple[int, int, int]
    bg_edge: tuple[int, int, int]
    primary: tuple[int, int, int]
    secondary: tuple[int, int, int]
    glow: tuple[int, int, int]
    ink: tuple[int, int, int]
    spark: tuple[int, int, int]


# Unique full palettes — match visual-action prompt examples
SCHOOL_PAL: dict[str, Pal] = {
    "mental": Pal(
        frame=(80, 140, 255), frame_hi=(255, 240, 120), frame_lo=(30, 50, 120),
        bg_center=(20, 28, 55), bg_edge=(8, 10, 22),
        primary=(255, 230, 110), secondary=(120, 170, 255), glow=(255, 255, 255),
        ink=(12, 16, 36), spark=(255, 255, 255),
    ),
    "elemental": Pal(
        frame=(255, 150, 40), frame_hi=(255, 230, 90), frame_lo=(140, 50, 15),
        bg_center=(40, 18, 10), bg_edge=(12, 8, 6),
        primary=(255, 140, 35), secondary=(255, 220, 70), glow=(255, 250, 200),
        ink=(40, 14, 8), spark=(255, 255, 220),
    ),
    "organic": Pal(
        frame=(70, 200, 80), frame_hi=(160, 255, 100), frame_lo=(30, 90, 35),
        bg_center=(18, 40, 18), bg_edge=(8, 14, 8),
        primary=(120, 80, 40), secondary=(90, 200, 70), glow=(180, 255, 100),
        ink=(20, 40, 16), spark=(200, 255, 140),
    ),
    "necromancy": Pal(
        frame=(230, 230, 220), frame_hi=(255, 255, 255), frame_lo=(90, 90, 85),
        bg_center=(22, 22, 24), bg_edge=(8, 8, 10),
        primary=(235, 230, 210), secondary=(140, 140, 135), glow=(255, 255, 245),
        ink=(20, 20, 22), spark=(255, 255, 255),
    ),
    "spatial": Pal(
        frame=(240, 240, 245), frame_hi=(255, 255, 255), frame_lo=(90, 70, 140),
        bg_center=(28, 16, 48), bg_edge=(10, 6, 20),
        primary=(160, 70, 220), secondary=(220, 80, 200), glow=(255, 255, 255),
        ink=(16, 8, 30), spark=(255, 255, 255),
    ),
    "corruption": Pal(
        frame=(50, 120, 45), frame_hi=(120, 200, 70), frame_lo=(25, 55, 22),
        bg_center=(36, 10, 12), bg_edge=(12, 4, 6),
        primary=(180, 40, 45), secondary=(110, 200, 55), glow=(180, 255, 90),
        ink=(30, 8, 10), spark=(200, 255, 120),
    ),
    "seals": Pal(
        frame=(255, 210, 80), frame_hi=(255, 245, 180), frame_lo=(100, 70, 20),
        bg_center=(40, 30, 12), bg_edge=(14, 10, 5),
        primary=(255, 210, 90), secondary=(255, 245, 180), glow=(255, 250, 220),
        ink=(32, 22, 8), spark=(255, 255, 240),
    ),
    "common": Pal(
        frame=(70, 220, 230), frame_hi=(200, 255, 255), frame_lo=(30, 90, 100),
        bg_center=(14, 28, 32), bg_edge=(6, 12, 14),
        primary=(80, 230, 240), secondary=(60, 140, 200), glow=(255, 255, 255),
        ink=(10, 24, 28), spark=(255, 255, 255),
    ),
}

# Elemental motif overrides (interior only; frame stays elemental)
ELEM_PAL: dict[str, Pal] = {
    "fire": SCHOOL_PAL["elemental"],
    "ice": Pal(
        frame=(255, 155, 45), frame_hi=(255, 235, 130), frame_lo=(115, 28, 10),
        bg_center=(40, 95, 155), bg_edge=(12, 28, 55),
        primary=(150, 220, 255), secondary=(235, 250, 255), glow=(255, 255, 255),
        ink=(10, 22, 45), spark=(255, 255, 255),
    ),
    "water": Pal(
        frame=(255, 155, 45), frame_hi=(255, 235, 130), frame_lo=(115, 28, 10),
        bg_center=(30, 85, 145), bg_edge=(8, 24, 48),
        primary=(55, 165, 255), secondary=(165, 225, 255), glow=(225, 248, 255),
        ink=(8, 20, 40), spark=(255, 255, 255),
    ),
    "air": Pal(
        frame=(255, 155, 45), frame_hi=(255, 235, 130), frame_lo=(115, 28, 10),
        bg_center=(60, 90, 120), bg_edge=(18, 26, 38),
        primary=(195, 225, 255), secondary=(255, 255, 255), glow=(245, 252, 255),
        ink=(18, 26, 38), spark=(255, 255, 255),
    ),
    "plasma": Pal(
        frame=(255, 155, 45), frame_hi=(255, 235, 130), frame_lo=(115, 28, 10),
        bg_center=(95, 40, 140), bg_edge=(30, 12, 42),
        primary=(185, 105, 255), secondary=(125, 235, 255), glow=(255, 255, 255),
        ink=(28, 12, 40), spark=(255, 255, 255),
    ),
    "steam": Pal(
        frame=(255, 155, 45), frame_hi=(255, 235, 130), frame_lo=(115, 28, 10),
        bg_center=(85, 90, 100), bg_edge=(28, 30, 34),
        primary=(215, 230, 240), secondary=(255, 205, 145), glow=(255, 255, 255),
        ink=(28, 30, 34), spark=(255, 255, 255),
    ),
}


def rgba(c: tuple[int, int, int], a: int = 255) -> tuple[int, int, int, int]:
    return (c[0], c[1], c[2], a)


def disc(d: ImageDraw.ImageDraw, cx: int, cy: int, r: int, fill, outline=None, width: int = 1):
    box = (cx - r, cy - r, cx + r, cy + r)
    d.ellipse(box, fill=fill, outline=outline, width=width if outline else 0)


def soft_glow(img: Image.Image, cx: int, cy: int, r: int, color: tuple[int, int, int], alpha: int = 110):
    """Unused in pixel mode — kept for motif helpers that may call it."""
    return img


def paint_plate(pal: Pal, common: bool = False) -> tuple[Image.Image, ImageDraw.ImageDraw, int, int]:
    """Flat dark circular plate + thin school rim (reference-style pixel icons)."""
    img = Image.new("RGBA", (ICON, ICON), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx = cy = ICON // 2

    # Flat near-black fill (like the reference grid), slight school tint
    bg = (
        max(8, pal.bg_edge[0] // 2),
        max(8, pal.bg_edge[1] // 2),
        max(10, pal.bg_edge[2] // 2),
    )
    disc(d, cx, cy, 30, rgba(bg, 255))
    # Very subtle inner tint so schools still read apart
    disc(d, cx, cy, 22, rgba(pal.bg_edge, 180))

    return img, d, cx, cy


def paint_frame(img: Image.Image, pal: Pal, common: bool = False) -> Image.Image:
    """Thin circular border (1–2px), like the prompt examples."""
    d = ImageDraw.Draw(img)
    cx = cy = ICON // 2
    disc(d, cx, cy, 30, None, outline=rgba(pal.frame, 255), width=2)
    # Tiny highlight for readability at 64px
    d.point([(cx - 18, cy - 20), (cx - 17, cy - 21)], fill=rgba(pal.frame_hi, 255))
    return img


def apply_circle_mask(img: Image.Image) -> Image.Image:
    mask = Image.new("L", (ICON, ICON), 0)
    ImageDraw.Draw(mask).ellipse((1, 1, ICON - 2, ICON - 2), fill=255)
    out = Image.new("RGBA", (ICON, ICON), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def pixelate(img: Image.Image, logical: int = 32) -> Image.Image:
    """Downscale then upscale with NEAREST for chunky RPG pixel look."""
    small = img.resize((logical, logical), Image.Resampling.NEAREST)
    return small.resize((ICON, ICON), Image.Resampling.NEAREST)

# --- Motif primitives (large, fill the circle) ---

def m_flame(d, cx, cy, p: Pal):
    # Large fireball + jagged top flames (great_fireball style)
    disc(d, cx, cy + 2, 12, rgba(p.primary))
    disc(d, cx, cy + 2, 7, rgba(p.secondary))
    disc(d, cx, cy + 2, 3, rgba(p.spark))
    for ox, h in ((-8, 10), (-3, 14), (3, 12), (8, 9)):
        d.polygon(
            [(cx + ox, cy - h), (cx + ox + 3, cy - 2), (cx + ox - 3, cy - 2)],
            fill=rgba(p.primary if abs(ox) > 4 else p.secondary),
        )


def m_embers(d, cx, cy, p: Pal):
    for ox, oy, r in ((-10, 8, 4), (0, -10, 5), (10, 6, 4), (-5, -2, 3), (6, 12, 3), (2, 4, 2)):
        disc(d, cx + ox, cy + oy, r, rgba(p.primary if r > 3 else p.secondary))


def m_ice(d, cx, cy, p: Pal):
    d.polygon([(cx, cy - 17), (cx + 11, cy - 2), (cx + 7, cy + 14), (cx - 7, cy + 14), (cx - 11, cy - 2)], fill=rgba(p.primary))
    d.line([(cx, cy - 14), (cx, cy + 12)], fill=rgba(p.spark), width=2)
    d.line([(cx - 8, cy), (cx + 8, cy)], fill=rgba(p.secondary), width=2)
    d.line([(cx - 6, cy - 7), (cx + 6, cy + 7)], fill=rgba(p.glow, 200), width=1)
    disc(d, cx + 8, cy - 8, 2, rgba(p.spark, 230))


def m_water(d, cx, cy, p: Pal):
    d.polygon([(cx, cy - 16), (cx + 11, cy - 2), (cx + 10, cy + 8), (cx, cy + 15), (cx - 10, cy + 8), (cx - 11, cy - 2)], fill=rgba(p.primary))
    disc(d, cx - 3, cy - 2, 3, rgba(p.glow, 220))
    d.arc((cx - 8, cy + 2, cx + 8, cy + 12), 200, 340, fill=rgba(p.secondary, 200), width=2)


def m_wind(d, cx, cy, p: Pal):
    for i, (oy, span) in enumerate(((-10, 16), (-1, 18), (8, 14))):
        y = cy + oy
        d.arc((cx - span, y - 6, cx + span - 2, y + 8), 200, 340, fill=rgba(p.primary if i else p.glow), width=3)
        d.line([(cx + span - 4, y), (cx + span + 2, y - 4)], fill=rgba(p.secondary), width=2)


def m_bolt(d, cx, cy, p: Pal):
    d.polygon([
        (cx + 1, cy - 17), (cx + 9, cy - 1), (cx + 3, cy - 1),
        (cx + 9, cy + 17), (cx - 3, cy + 1), (cx + 2, cy + 1), (cx - 7, cy - 17),
    ], fill=rgba(p.primary))
    d.line([(cx, cy - 12), (cx + 4, cy - 1), (cx - 1, cy + 1), (cx + 3, cy + 12)], fill=rgba(p.spark), width=1)


def m_plasma(d, cx, cy, p: Pal):
    disc(d, cx, cy, 13, rgba(p.secondary, 200))
    disc(d, cx, cy, 9, rgba(p.primary))
    disc(d, cx, cy, 4, rgba(p.spark))
    for ang in (30, 120, 210, 300):
        rad = math.radians(ang)
        d.line([(cx, cy), (cx + int(math.cos(rad) * 16), cy + int(math.sin(rad) * 16))], fill=rgba(p.secondary), width=2)


def m_steam(d, cx, cy, p: Pal):
    for ox, oy, r in ((-9, 2, 6), (0, -8, 7), (9, 0, 6), (-2, 9, 5), (6, 8, 4)):
        disc(d, cx + ox, cy + oy, r, rgba(p.primary, 210))
    disc(d, cx, cy - 4, 3, rgba(p.spark, 180))


def m_tornado(d, cx, cy, p: Pal):
    d.polygon([(cx - 14, cy - 14), (cx + 14, cy - 14), (cx + 5, cy + 15), (cx - 5, cy + 15)], fill=rgba(p.primary, 230))
    for oy in (-6, 0, 6):
        d.line([(cx - 8 + oy // 2, cy + oy), (cx + 8 - oy // 2, cy + oy)], fill=rgba(p.glow), width=2)
    disc(d, cx, cy - 16, 3, rgba(p.spark))


def m_force(d, cx, cy, p: Pal):
    for r in (16, 11, 6):
        disc(d, cx, cy, r, None, outline=rgba(p.primary, 230), width=2)
    disc(d, cx, cy, 4, rgba(p.glow))
    disc(d, cx, cy, 2, rgba(p.spark))
    d.line([(cx - 14, cy - 2), (cx - 18, cy - 6)], fill=rgba(p.secondary), width=2)
    d.line([(cx + 14, cy + 2), (cx + 18, cy + 6)], fill=rgba(p.secondary), width=2)


def m_shard(d, cx, cy, p: Pal):
    d.polygon([(cx + 14, cy - 14), (cx - 2, cy - 2), (cx + 2, cy + 4), (cx - 12, cy + 14), (cx - 4, cy + 2), (cx + 4, cy - 4)], fill=rgba(p.primary))
    d.line([(cx - 6, cy + 6), (cx + 10, cy - 10)], fill=rgba(p.spark), width=1)


def m_eye(d, cx, cy, p: Pal):
    d.ellipse((cx - 16, cy - 9, cx + 16, cy + 9), fill=rgba(p.glow, 240))
    disc(d, cx, cy, 7, rgba(p.secondary))
    disc(d, cx, cy, 3, rgba(p.ink))
    disc(d, cx + 2, cy - 2, 1, rgba(p.spark))


def m_lance(d, cx, cy, p: Pal):
    # Cracked void spear + spark pixels (void_lance style)
    d.line([(cx - 14, cy + 12), (cx + 12, cy - 12)], fill=rgba(p.primary), width=4)
    d.polygon([(cx + 12, cy - 12), (cx + 2, cy - 10), (cx + 10, cy - 2)], fill=rgba(p.secondary))
    # Crack
    d.line([(cx - 2, cy + 2), (cx + 4, cy - 4)], fill=rgba(p.ink), width=1)
    for ox, oy in ((10, -14), (14, -6), (-8, 8)):
        disc(d, cx + ox, cy + oy, 1, rgba(p.spark))


def m_focus(d, cx, cy, p: Pal):
    disc(d, cx, cy + 3, 7, rgba(p.primary))
    disc(d, cx, cy + 3, 3, rgba(p.spark))
    d.ellipse((cx - 14, cy - 16, cx + 14, cy - 2), outline=rgba(p.secondary), width=3)
    d.line([(cx, cy - 2), (cx, cy + 10)], fill=rgba(p.glow), width=2)


def m_scream(d, cx, cy, p: Pal):
    for r in (7, 12, 17):
        d.ellipse((cx - r, cy - r // 2 - 2, cx + r, cy + r // 2 + 2), outline=rgba(p.primary, 220), width=2)
    disc(d, cx, cy, 4, rgba(p.glow))


def m_lock(d, cx, cy, p: Pal):
    d.line([(cx, cy - 14), (cx, cy + 14)], fill=rgba(p.secondary), width=2)
    d.line([(cx - 14, cy), (cx + 14, cy)], fill=rgba(p.secondary), width=2)
    d.rectangle((cx - 6, cy - 2, cx + 6, cy + 8), fill=rgba(p.primary))
    d.arc((cx - 6, cy - 10, cx + 6, cy + 2), 180, 0, fill=rgba(p.glow), width=3)
    disc(d, cx, cy + 3, 2, rgba(p.ink))


def m_crush(d, cx, cy, p: Pal):
    disc(d, cx, cy, 10, rgba(p.primary))
    disc(d, cx, cy, 5, rgba(p.ink))
    disc(d, cx, cy, 16, None, outline=rgba(p.glow, 180), width=2)
    for ang in (45, 135, 225, 315):
        rad = math.radians(ang)
        d.line([(cx, cy), (cx + int(math.cos(rad) * 14), cy + int(math.sin(rad) * 14))], fill=rgba(p.secondary), width=2)


def m_fog(d, cx, cy, p: Pal):
    for ox, oy, r in ((-8, -5, 8), (7, 2, 7), (0, 8, 6), (-4, -10, 5), (8, -8, 4)):
        disc(d, cx + ox, cy + oy, r, rgba(p.primary, 200))
    disc(d, cx, cy, 3, rgba(p.glow))


def m_shield(d, cx, cy, p: Pal):
    d.polygon([(cx, cy - 16), (cx + 14, cy - 4), (cx + 10, cy + 14), (cx - 10, cy + 14), (cx - 14, cy - 4)], fill=rgba(p.primary))
    d.polygon([(cx, cy - 12), (cx + 9, cy - 3), (cx + 6, cy + 10), (cx - 6, cy + 10), (cx - 9, cy - 3)], fill=rgba(p.secondary, 220))
    disc(d, cx, cy, 4, rgba(p.glow))


def m_probe(d, cx, cy, p: Pal):
    m_eye(d, cx, cy - 3, p)
    d.line([(cx, cy + 6), (cx, cy + 16)], fill=rgba(p.secondary), width=3)
    disc(d, cx, cy + 16, 3, rgba(p.glow))


def m_echo(d, cx, cy, p: Pal):
    disc(d, cx - 3, cy - 2, 10, rgba(p.primary))
    d.polygon([(cx - 10, cy + 4), (cx + 5, cy + 4), (cx + 3, cy + 14), (cx - 8, cy + 14)], fill=rgba(p.primary))
    d.polygon([(cx + 2, cy - 12), (cx + 16, cy - 14), (cx + 15, cy + 5), (cx + 1, cy + 7)], fill=rgba(p.glow, 240))
    for i, oy in enumerate((-6, -2, 2)):
        d.line([(cx + 5, cy + oy), (cx + 12, cy + oy - 1)], fill=rgba(p.ink), width=1)
    disc(d, cx - 12, cy - 10, 2, rgba(p.secondary, 220))


def m_synapse(d, cx, cy, p: Pal):
    disc(d, cx, cy, 4, rgba(p.spark))
    for i in range(6):
        ang = math.radians(i * 60)
        x2 = cx + int(math.cos(ang) * 14)
        y2 = cy + int(math.sin(ang) * 14)
        d.line([(cx, cy), (x2, y2)], fill=rgba(p.secondary), width=2)
        disc(d, x2, y2, 3, rgba(p.primary))


def m_drain(d, cx, cy, p: Pal):
    d.arc((cx - 14, cy - 12, cx + 4, cy + 12), 200, 40, fill=rgba(p.primary), width=4)
    d.arc((cx - 4, cy - 10, cx + 14, cy + 10), 20, 200, fill=rgba(p.secondary), width=4)
    disc(d, cx + 9, cy - 5, 4, rgba(p.glow))
    disc(d, cx - 9, cy + 5, 3, rgba(p.ink, 230))


def m_fortress(d, cx, cy, p: Pal):
    d.rectangle((cx - 12, cy - 4, cx + 12, cy + 14), fill=rgba(p.primary))
    d.polygon([(cx - 14, cy - 4), (cx, cy - 16), (cx + 14, cy - 4)], fill=rgba(p.secondary))
    disc(d, cx, cy + 2, 4, rgba(p.glow))
    d.rectangle((cx - 4, cy + 4, cx + 4, cy + 14), fill=rgba(p.ink))


def m_bomb(d, cx, cy, p: Pal):
    disc(d, cx, cy, 9, rgba(p.primary))
    disc(d, cx, cy, 4, rgba(p.spark))
    for ang in range(0, 360, 40):
        rad = math.radians(ang)
        disc(d, cx + int(math.cos(rad) * 14), cy + int(math.sin(rad) * 14), 3, rgba(p.secondary, 230))


def m_storm(d, cx, cy, p: Pal):
    m_fog(d, cx, cy, p)
    m_bolt(d, cx - 4, cy, p)


def m_terror(d, cx, cy, p: Pal):
    d.polygon([(cx, cy - 14), (cx + 12, cy + 2), (cx + 8, cy + 14), (cx - 8, cy + 14), (cx - 12, cy + 2)], fill=rgba(p.ink))
    disc(d, cx - 4, cy - 2, 3, rgba(p.primary))
    disc(d, cx + 4, cy - 2, 3, rgba(p.primary))
    d.arc((cx - 6, cy + 4, cx + 6, cy + 14), 200, 340, fill=rgba(p.secondary), width=2)


def m_urge_cliff(d, cx, cy, p: Pal):
    # Hypnotic yellow eye with spiral (prompt example)
    disc(d, cx, cy, 12, rgba(p.primary))
    disc(d, cx, cy, 9, rgba(p.secondary))
    disc(d, cx, cy, 5, rgba(p.ink))
    # Spiral ticks
    d.arc((cx - 7, cy - 7, cx + 7, cy + 7), 40, 280, fill=rgba(p.spark), width=2)
    d.arc((cx - 4, cy - 4, cx + 4, cy + 4), 120, 300, fill=rgba(p.primary), width=1)
    disc(d, cx, cy, 2, rgba(p.spark))


def m_urge_drown(d, cx, cy, p: Pal):
    disc(d, cx, cy - 2, 11, rgba(p.ink))
    d.arc((cx - 14, cy + 4, cx + 14, cy + 16), 200, 340, fill=rgba(p.secondary), width=3)
    disc(d, cx - 4, cy - 4, 2, rgba(p.primary))
    disc(d, cx + 4, cy - 4, 2, rgba(p.primary))


def m_frenzy(d, cx, cy, p: Pal):
    d.polygon([(cx - 12, cy + 10), (cx - 4, cy - 14), (cx + 2, cy - 2), (cx + 12, cy + 12)], fill=rgba(p.primary))
    d.polygon([(cx + 10, cy - 10), (cx + 2, cy + 12), (cx - 2, cy + 2)], fill=rgba(p.secondary))
    disc(d, cx, cy, 3, rgba(p.spark))


def m_omega(d, cx, cy, p: Pal):
    m_eye(d, cx, cy, p)
    disc(d, cx, cy, 16, None, outline=rgba(p.primary, 200), width=2)
    for ang in (0, 120, 240):
        rad = math.radians(ang)
        disc(d, cx + int(math.cos(rad) * 14), cy + int(math.sin(rad) * 14), 3, rgba(p.glow))


def m_hydro_slice(d, cx, cy, p: Pal):
    d.pieslice((cx - 16, cy - 16, cx + 16, cy + 16), 200, 20, fill=rgba(p.primary))
    d.pieslice((cx - 8, cy - 8, cx + 8, cy + 8), 200, 20, fill=rgba(p.bg_edge))
    d.arc((cx - 15, cy - 15, cx + 15, cy + 15), 205, 15, fill=rgba(p.spark), width=2)
    for ox, oy in ((11, -7), (13, 2), (9, 9)):
        disc(d, cx + ox, cy + oy, 2, rgba(p.secondary))


def m_frost_wall(d, cx, cy, p: Pal):
    for ox in (-12, -2, 8):
        d.rectangle((cx + ox, cy - 14, cx + ox + 7, cy + 14), fill=rgba(p.primary))
        d.line([(cx + ox + 3, cy - 12), (cx + ox + 3, cy + 12)], fill=rgba(p.spark, 200), width=1)


def m_vacuum(d, cx, cy, p: Pal):
    disc(d, cx, cy, 14, rgba(p.secondary, 200))
    disc(d, cx, cy, 9, rgba(p.ink))
    disc(d, cx, cy, 4, rgba(p.bg_edge))
    for ang in range(0, 360, 45):
        rad = math.radians(ang)
        d.line([
            (cx + int(math.cos(rad) * 16), cy + int(math.sin(rad) * 16)),
            (cx + int(math.cos(rad) * 10), cy + int(math.sin(rad) * 10)),
        ], fill=rgba(p.primary), width=2)


def m_sonic(d, cx, cy, p: Pal):
    for r in (7, 12, 17):
        disc(d, cx, cy, r, None, outline=rgba(p.primary, 220), width=2)
    disc(d, cx, cy, 4, rgba(p.spark))


def m_hand(d, cx, cy, p: Pal):
    disc(d, cx, cy + 3, 8, rgba(p.primary, 240))
    for ox in (-9, -3, 3, 9):
        d.line([(cx + ox // 2, cy), (cx + ox, cy - 14)], fill=rgba(p.glow), width=3)
    m_wind(d, cx, cy + 10, p)


def m_bubble(d, cx, cy, p: Pal):
    disc(d, cx, cy, 14, None, outline=rgba(p.primary), width=3)
    disc(d, cx - 4, cy - 5, 4, rgba(p.glow, 200))
    m_water(d, cx + 2, cy + 3, p)


def m_mirage(d, cx, cy, p: Pal):
    for oy in (-10, 0, 10):
        d.arc((cx - 14, cy + oy - 5, cx + 14, cy + oy + 7), 200, 340, fill=rgba(p.secondary, 200), width=2)
    m_steam(d, cx, cy - 2, p)


def m_quasar(d, cx, cy, p: Pal):
    disc(d, cx, cy, 5, rgba(p.spark))
    disc(d, cx, cy, 10, None, outline=rgba(p.secondary), width=2)
    disc(d, cx, cy, 16, None, outline=rgba(p.primary, 200), width=2)
    for ang in range(0, 360, 72):
        rad = math.radians(ang)
        disc(d, cx + int(math.cos(rad) * 16), cy + int(math.sin(rad) * 16), 3, rgba(p.glow, 220))


def m_cells(d, cx, cy, p: Pal):
    disc(d, cx - 6, cy, 9, rgba((200, 55, 70)))
    disc(d, cx - 6, cy, 4, rgba((140, 30, 45)))
    disc(d, cx + 7, cy - 2, 8, rgba(p.glow, 240))
    disc(d, cx + 6, cy - 2, 3, rgba(p.secondary))


def m_thorns(d, cx, cy, p: Pal):
    for ang in (-55, -18, 18, 55):
        rad = math.radians(ang - 90)
        x2 = cx + int(math.cos(rad) * 17)
        y2 = cy + int(math.sin(rad) * 17)
        d.line([(cx, cy + 8), (x2, y2)], fill=rgba(p.primary), width=3)
        d.line([(x2, y2), (x2 - 3, y2 + 5)], fill=rgba(p.secondary), width=2)
    disc(d, cx, cy + 8, 4, rgba(p.ink))


def m_roots(d, cx, cy, p: Pal):
    # 3 twisted roots caging a green pixel (root_bind prompt)
    brown = p.primary
    lime = p.glow
    paths = [
        [(cx - 2, cy - 14), (cx - 10, cy - 2), (cx - 12, cy + 12), (cx - 4, cy + 14)],
        [(cx, cy - 12), (cx + 2, cy + 2), (cx, cy + 16)],
        [(cx + 2, cy - 14), (cx + 10, cy - 2), (cx + 12, cy + 12), (cx + 4, cy + 14)],
    ]
    for pts in paths:
        d.line(pts, fill=rgba(brown), width=3)
    disc(d, cx, cy + 2, 3, rgba(lime))
    disc(d, cx, cy + 2, 1, rgba(p.spark))


def m_thorn_lash(d, cx, cy, p: Pal):
    d.line([(cx - 15, cy + 12), (cx + 15, cy + 12)], fill=rgba((70, 50, 30)), width=3)
    for i, h in enumerate((10, 16, 13, 18, 11)):
        x = cx - 12 + i * 6
        d.line([(x, cy + 12), (x, cy + 12 - h)], fill=rgba(p.primary), width=3)
        d.line([(x, cy + 12 - h), (x + 4, cy + 12 - h + 5)], fill=rgba(p.secondary), width=2)


def m_leaf(d, cx, cy, p: Pal):
    d.polygon([(cx, cy - 16), (cx + 13, cy - 2), (cx + 5, cy + 14), (cx - 5, cy + 14), (cx - 13, cy - 2)], fill=rgba(p.primary))
    d.line([(cx, cy - 12), (cx, cy + 12)], fill=rgba(p.ink), width=2)
    disc(d, cx + 4, cy - 4, 2, rgba(p.glow, 200))


def m_sap(d, cx, cy, p: Pal):
    amber = (230, 170, 50)
    d.ellipse((cx - 7, cy - 2, cx + 7, cy + 14), fill=rgba(amber))
    d.ellipse((cx - 4, cy - 12, cx + 4, cy - 2), fill=rgba(amber))
    disc(d, cx - 2, cy + 2, 2, rgba(p.glow, 220))


def m_virus(d, cx, cy, p: Pal):
    disc(d, cx, cy, 9, rgba(p.primary))
    disc(d, cx, cy, 4, rgba(p.glow))
    for ang in range(0, 360, 40):
        rad = math.radians(ang)
        x2 = cx + int(math.cos(rad) * 15)
        y2 = cy + int(math.sin(rad) * 15)
        d.line([(cx, cy), (x2, y2)], fill=rgba(p.secondary), width=2)
        disc(d, x2, y2, 3, rgba(p.secondary))


def m_parasite(d, cx, cy, p: Pal):
    body = (200, 170, 55)
    d.arc((cx - 16, cy - 12, cx + 6, cy + 14), 200, 40, fill=rgba(body), width=5)
    d.arc((cx - 4, cy - 10, cx + 16, cy + 12), 20, 200, fill=rgba(body), width=5)
    disc(d, cx + 12, cy - 2, 4, rgba(p.glow))


def m_bone(d, cx, cy, p: Pal):
    bone = p.secondary
    d.rectangle((cx - 4, cy - 14, cx + 4, cy + 14), fill=rgba(bone))
    for oy in (-14, 14):
        disc(d, cx - 6, cy + oy, 5, rgba(bone))
        disc(d, cx + 6, cy + oy, 5, rgba(bone))


def m_chitin(d, cx, cy, p: Pal):
    shell = (120, 100, 50)
    d.polygon([(cx, cy - 16), (cx + 14, cy - 2), (cx + 10, cy + 14), (cx - 10, cy + 14), (cx - 14, cy - 2)], fill=rgba(shell))
    d.line([(cx, cy - 12), (cx, cy + 12)], fill=rgba(p.ink), width=2)
    disc(d, cx, cy, 3, rgba(p.primary, 200))


def m_muscle(d, cx, cy, p: Pal):
    fiber = (190, 50, 65)
    for oy in (-10, -2, 6, 14):
        d.arc((cx - 15, cy + oy - 7, cx + 15, cy + oy + 7), 200, 340, fill=rgba(fiber), width=4)


def m_nerve(d, cx, cy, p: Pal):
    d.line([(cx, cy - 16), (cx, cy + 16)], fill=rgba(p.secondary), width=3)
    d.line([(cx - 14, cy), (cx + 14, cy)], fill=rgba(p.secondary), width=3)
    d.line([(cx - 11, cy - 11), (cx + 11, cy + 11)], fill=rgba(p.primary), width=2)
    disc(d, cx, cy, 5, rgba(p.glow))


def m_dna(d, cx, cy, p: Pal):
    a, b = p.secondary, p.primary
    for i in range(-14, 15, 2):
        ox = int(round(math.sin(i * 0.45) * 9))
        disc(d, cx + ox, cy + i, 3, rgba(a))
        disc(d, cx - ox, cy + i, 3, rgba(b))
        if i % 4 == 0:
            d.line([(cx + ox, cy + i), (cx - ox, cy + i)], fill=rgba(p.glow, 180), width=1)


def m_spore(d, cx, cy, p: Pal):
    for ox, oy, r in ((0, 0, 6), (-10, -5, 4), (10, -4, 4), (-6, 9, 3), (7, 8, 3), (0, -11, 3), (4, 2, 2)):
        disc(d, cx + ox, cy + oy, r, rgba(p.primary if r > 3 else p.secondary, 220))


def m_soul(d, cx, cy, p: Pal):
    disc(d, cx, cy - 5, 10, rgba(p.primary))
    d.polygon([(cx - 10, cy - 2), (cx + 10, cy - 2), (cx, cy + 16)], fill=rgba(p.primary))
    disc(d, cx, cy - 3, 4, rgba(p.ink))
    d.line([(cx + 11, cy - 9), (cx + 17, cy - 15)], fill=rgba(p.glow), width=2)


def m_wither(d, cx, cy, p: Pal):
    d.polygon([(cx - 12, cy + 10), (cx - 4, cy - 14), (cx + 2, cy - 2), (cx + 12, cy + 12)], fill=rgba(p.ink))
    disc(d, cx + 2, cy, 4, rgba(p.primary))
    disc(d, cx - 5, cy + 5, 3, rgba(p.secondary, 220))


def m_shade(d, cx, cy, p: Pal):
    d.polygon([(cx, cy - 16), (cx + 13, cy + 14), (cx + 4, cy + 12), (cx - 4, cy + 12), (cx - 13, cy + 14)], fill=rgba(p.ink))
    d.polygon([(cx, cy - 8), (cx + 7, cy + 5), (cx - 7, cy + 5)], fill=rgba(p.secondary, 200))
    disc(d, cx - 3, cy - 2, 2, rgba(p.spark))
    disc(d, cx + 3, cy - 2, 2, rgba(p.spark))


def m_shades(d, cx, cy, p: Pal):
    for ox in (-10, 0, 10):
        d.polygon([(cx + ox, cy - 12), (cx + ox + 7, cy + 10), (cx + ox - 7, cy + 10)], fill=rgba(p.ink if ox else p.secondary))


def m_skull(d, cx, cy, p: Pal):
    # Skull + crossbones (raise_skeleton prompt)
    # Crossbones behind
    d.line([(cx - 14, cy + 2), (cx + 14, cy + 14)], fill=rgba(p.secondary), width=3)
    d.line([(cx - 14, cy + 14), (cx + 14, cy + 2)], fill=rgba(p.secondary), width=3)
    disc(d, cx - 14, cy + 2, 2, rgba(p.primary))
    disc(d, cx + 14, cy + 14, 2, rgba(p.primary))
    disc(d, cx - 14, cy + 14, 2, rgba(p.primary))
    disc(d, cx + 14, cy + 2, 2, rgba(p.primary))
    # Skull
    disc(d, cx, cy - 2, 10, rgba(p.primary))
    d.rectangle((cx - 7, cy + 4, cx + 7, cy + 11), fill=rgba(p.primary))
    disc(d, cx - 4, cy - 3, 2, rgba(p.ink))
    disc(d, cx + 4, cy - 3, 2, rgba(p.ink))
    d.line([(cx - 2, cy + 3), (cx + 2, cy + 3)], fill=rgba(p.ink), width=1)


def m_siphon(d, cx, cy, p: Pal):
    disc(d, cx - 6, cy, 8, rgba(p.primary, 220))
    disc(d, cx + 8, cy - 2, 6, rgba(p.secondary))
    d.line([(cx - 2, cy), (cx + 6, cy - 2)], fill=rgba(p.glow), width=3)
    disc(d, cx + 8, cy - 2, 2, rgba(p.ink))


def m_coil(d, cx, cy, p: Pal):
    d.arc((cx - 14, cy - 14, cx + 14, cy + 14), 30, 300, fill=rgba(p.ink), width=4)
    d.arc((cx - 9, cy - 9, cx + 9, cy + 9), 60, 280, fill=rgba(p.secondary), width=3)
    disc(d, cx + 7, cy - 7, 4, rgba(p.primary))


def m_gate(d, cx, cy, p: Pal):
    d.rectangle((cx - 10, cy - 14, cx + 10, cy + 14), outline=rgba(p.secondary), width=3)
    d.ellipse((cx - 7, cy - 6, cx + 7, cy + 10), fill=rgba(p.ink))
    disc(d, cx, cy + 2, 4, rgba(p.primary, 200))


def m_reaper(d, cx, cy, p: Pal):
    d.line([(cx - 5, cy + 14), (cx + 3, cy - 16)], fill=rgba(p.ink), width=4)
    d.polygon([(cx + 3, cy - 16), (cx + 16, cy - 6), (cx + 5, cy - 4)], fill=rgba(p.primary))
    disc(d, cx - 2, cy + 4, 3, rgba(p.glow))


def m_blink(d, cx, cy, p: Pal):
    disc(d, cx, cy, 14, None, outline=rgba(p.primary), width=3)
    disc(d, cx - 6, cy, 5, rgba(p.glow))
    disc(d, cx + 7, cy, 5, rgba(p.secondary))
    d.line([(cx - 2, cy), (cx + 3, cy)], fill=rgba(p.spark), width=2)


def m_rift(d, cx, cy, p: Pal):
    d.arc((cx - 16, cy - 16, cx + 16, cy + 16), 40, 280, fill=rgba(p.primary), width=4)
    d.arc((cx - 10, cy - 10, cx + 10, cy + 10), 60, 260, fill=rgba(p.secondary), width=3)
    disc(d, cx + 4, cy - 4, 3, rgba(p.glow))


def m_veil(d, cx, cy, p: Pal):
    d.ellipse((cx - 14, cy - 17, cx + 14, cy + 10), outline=rgba(p.primary), width=3)
    for oy in (-4, 2, 8):
        d.arc((cx - 10, cy + oy - 3, cx + 10, cy + oy + 5), 200, 340, fill=rgba(p.secondary, 200), width=2)


def m_void(d, cx, cy, p: Pal):
    disc(d, cx, cy, 12, rgba(p.ink))
    disc(d, cx, cy, 12, None, outline=rgba(p.primary), width=3)
    disc(d, cx, cy, 5, rgba(p.bg_edge))
    disc(d, cx, cy, 2, rgba(p.secondary, 200))


def m_well(d, cx, cy, p: Pal):
    for r in (16, 11, 6):
        disc(d, cx, cy, r, None, outline=rgba(p.primary if r > 10 else p.secondary), width=2)
    disc(d, cx, cy, 3, rgba(p.glow))


def m_corrupt(d, cx, cy, p: Pal):
    disc(d, cx, cy, 13, None, outline=rgba(p.primary), width=3)
    d.line([(cx, cy - 11), (cx, cy + 11)], fill=rgba(p.primary), width=3)
    d.line([(cx - 11, cy), (cx + 11, cy)], fill=rgba(p.primary), width=3)
    disc(d, cx + 9, cy - 9, 3, rgba(p.secondary))


def m_binding(d, cx, cy, p: Pal):
    for oy in (-10, 0, 10):
        d.ellipse((cx - 8, cy + oy - 5, cx + 8, cy + oy + 5), outline=rgba(p.primary), width=3)
    d.line([(cx, cy - 12), (cx, cy + 12)], fill=rgba(p.secondary), width=3)


def m_blight(d, cx, cy, p: Pal):
    for r in (7, 12, 17):
        disc(d, cx, cy, r, None, outline=rgba(p.secondary, 230), width=2)
    disc(d, cx, cy, 4, rgba(p.glow))


def m_rot(d, cx, cy, p: Pal):
    d.polygon([(cx - 12, cy + 10), (cx - 4, cy - 14), (cx + 2, cy - 2), (cx + 12, cy + 12)], fill=rgba((120, 95, 40)))
    disc(d, cx + 4, cy + 2, 4, rgba((170, 35, 45)))
    disc(d, cx - 6, cy + 5, 3, rgba(p.secondary, 220))


def m_lash(d, cx, cy, p: Pal):
    d.line([(cx - 14, cy + 12), (cx + 14, cy - 14)], fill=rgba(p.primary), width=4)
    for ox, oy in ((-7, 5), (0, -2), (7, -9), (11, -4)):
        disc(d, cx + ox, cy + oy, 3, rgba(p.secondary, 230))


def m_plague(d, cx, cy, p: Pal):
    d.polygon([(cx - 2, cy - 16), (cx + 8, cy - 2), (cx + 2, cy), (cx + 9, cy + 16), (cx - 3, cy + 2), (cx + 2, cy)], fill=rgba(p.secondary))
    disc(d, cx, cy, 3, rgba(p.glow))


def m_wound(d, cx, cy, p: Pal):
    # Open red wound + 3 green pus drops (festering_wound prompt)
    d.ellipse((cx - 12, cy - 10, cx + 12, cy + 6), fill=rgba(p.primary))
    d.polygon([(cx - 6, cy - 4), (cx + 8, cy - 8), (cx + 2, cy + 2)], fill=rgba(p.ink))
    for ox, oy in ((-6, 10), (0, 14), (6, 10)):
        disc(d, cx + ox, cy + oy, 2, rgba(p.secondary))
        d.line([(cx + ox, cy + oy - 2), (cx + ox, cy + 4)], fill=rgba(p.secondary), width=1)


def m_miasma(d, cx, cy, p: Pal):
    for ox, oy, r in ((-8, -3, 9), (8, 3, 8), (0, -10, 6), (-3, 10, 5)):
        disc(d, cx + ox, cy + oy, r, rgba(p.primary, 200))
    disc(d, cx, cy, 3, rgba(p.secondary))


def m_crown(d, cx, cy, p: Pal):
    d.polygon([(cx - 14, cy + 4), (cx - 9, cy - 12), (cx - 2, cy), (cx + 5, cy - 14), (cx + 12, cy - 2), (cx + 14, cy + 6)], fill=rgba(p.primary))
    disc(d, cx, cy - 2, 4, rgba(p.secondary))


def m_trap(d, cx, cy, p: Pal):
    d.polygon([(cx, cy - 14), (cx + 14, cy + 12), (cx - 14, cy + 12)], outline=rgba(p.primary), width=3)
    disc(d, cx, cy + 2, 4, rgba(p.glow))
    d.line([(cx, cy - 8), (cx, cy + 8)], fill=rgba(p.secondary), width=2)


def m_glyph(d, cx, cy, p: Pal):
    disc(d, cx, cy, 12, None, outline=rgba(p.primary), width=3)
    d.polygon([(cx, cy - 8), (cx + 7, cy + 5), (cx - 7, cy + 5)], fill=rgba(p.secondary))
    disc(d, cx, cy, 2, rgba(p.spark))


def m_snare(d, cx, cy, p: Pal):
    disc(d, cx, cy, 14, None, outline=rgba(p.primary), width=3)
    d.line([(cx - 10, cy - 10), (cx + 10, cy + 10)], fill=rgba(p.secondary), width=3)
    d.line([(cx + 10, cy - 10), (cx - 10, cy + 10)], fill=rgba(p.secondary), width=3)


def m_beacon(d, cx, cy, p: Pal):
    d.polygon([(cx, cy - 16), (cx + 8, cy + 12), (cx - 8, cy + 12)], fill=rgba(p.primary))
    d.line([(cx, cy - 16), (cx, cy + 18)], fill=rgba(p.glow), width=3)
    disc(d, cx, cy - 16, 3, rgba(p.spark))


def m_glow(d, cx, cy, p: Pal):
    for r, a in ((16, 50), (11, 100), (7, 180)):
        disc(d, cx, cy, r, rgba(p.primary, a))
    disc(d, cx, cy, 4, rgba(p.spark))


def m_chain(d, cx, cy, p: Pal):
    disc(d, cx - 7, cy, 7, None, outline=rgba(p.primary), width=3)
    disc(d, cx + 7, cy, 7, None, outline=rgba(p.secondary), width=3)
    d.line([(cx - 2, cy), (cx + 2, cy)], fill=rgba(p.glow), width=2)


def m_wave(d, cx, cy, p: Pal):
    for i, ox in enumerate((-4, 4, 12)):
        d.arc((cx + ox - 16, cy - 10, cx + ox, cy + 10), 300, 60, fill=rgba(p.primary if i else p.glow), width=3)


def m_pulse(d, cx, cy, p: Pal):
    pts = [(cx + x, cy + int(7 * math.sin(x / 3))) for x in range(-16, 17, 2)]
    d.line(pts, fill=rgba(p.primary), width=3)
    disc(d, cx, cy, 3, rgba(p.glow))


def m_arrow(d, cx, cy, p: Pal):
    d.polygon([(cx + 16, cy), (cx - 12, cy - 12), (cx - 12, cy + 12)], fill=rgba(p.primary))
    disc(d, cx - 4, cy, 3, rgba(p.glow))


def m_sear(d, cx, cy, p: Pal):
    d.ellipse((cx - 14, cy + 2, cx + 14, cy + 16), fill=rgba(p.secondary, 230))
    d.line([(cx + 12, cy + 9), (cx + 18, cy + 4)], fill=rgba(p.secondary), width=3)
    m_flame(d, cx, cy - 4, p)


def m_ore(d, cx, cy, p: Pal):
    d.polygon([(cx - 12, cy + 3), (cx - 5, cy - 10), (cx + 7, cy - 7), (cx + 12, cy + 5), (cx, cy + 12)], fill=rgba((100, 110, 130)))
    disc(d, cx - 2, cy, 3, rgba(p.glow, 220))
    d.polygon([(cx + 2, cy - 14), (cx + 10, cy - 2), (cx - 2, cy - 2)], fill=rgba(p.primary))


def m_adrenaline(d, cx, cy, p: Pal):
    d.polygon([(cx, cy - 16), (cx + 8, cy - 1), (cx + 3, cy - 1), (cx + 10, cy + 16), (cx - 5, cy + 1), (cx + 1, cy + 1)], fill=rgba(p.primary))
    disc(d, cx, cy, 11, None, outline=rgba(p.glow), width=2)
    disc(d, cx, cy, 3, rgba(p.spark))


def m_charge(d, cx, cy, p: Pal):
    disc(d, cx, cy, 8, rgba(p.secondary))
    disc(d, cx, cy, 4, rgba(p.spark))
    for ang in (0, 72, 144, 216, 288):
        rad = math.radians(ang)
        d.line([(cx, cy), (cx + int(math.cos(rad) * 15), cy + int(math.sin(rad) * 15))], fill=rgba(p.primary), width=2)


def m_link(d, cx, cy, p: Pal):
    disc(d, cx - 9, cy, 8, rgba(p.primary))
    disc(d, cx + 9, cy, 8, rgba(p.secondary))
    d.arc((cx - 12, cy - 10, cx + 12, cy + 10), 200, 340, fill=rgba(p.glow), width=3)
    disc(d, cx, cy - 2, 3, rgba(p.spark))


def m_ward(d, cx, cy, p: Pal):
    # Cyan shield + plus (psi_ward prompt)
    d.polygon(
        [(cx, cy - 16), (cx + 13, cy - 4), (cx + 10, cy + 14), (cx - 10, cy + 14), (cx - 13, cy - 4)],
        fill=rgba(p.primary),
    )
    d.polygon(
        [(cx, cy - 12), (cx + 9, cy - 3), (cx + 7, cy + 10), (cx - 7, cy + 10), (cx - 9, cy - 3)],
        fill=rgba(p.secondary),
    )
    # Magical plus
    d.rectangle((cx - 2, cy - 7, cx + 2, cy + 7), fill=rgba(p.spark))
    d.rectangle((cx - 7, cy - 2, cx + 7, cy + 2), fill=rgba(p.spark))


def m_mark(d, cx, cy, p: Pal):
    d.line([(cx, cy - 16), (cx, cy + 16)], fill=rgba(p.primary), width=4)
    d.line([(cx - 12, cy - 6), (cx + 12, cy - 6)], fill=rgba(p.primary), width=4)
    disc(d, cx, cy + 6, 4, rgba(p.secondary))


def m_anchor(d, cx, cy, p: Pal):
    disc(d, cx, cy - 6, 6, rgba(p.primary))
    d.line([(cx, cy - 2), (cx, cy + 12)], fill=rgba(p.secondary), width=3)
    d.arc((cx - 10, cy + 4, cx + 10, cy + 16), 200, 340, fill=rgba(p.glow), width=3)


def m_apotheosis(d, cx, cy, p: Pal):
    m_soul(d, cx, cy, p)
    disc(d, cx, cy, 16, None, outline=rgba(p.secondary, 200), width=2)
    disc(d, cx, cy - 16, 3, rgba(p.glow))


def m_supremacy(d, cx, cy, p: Pal):
    m_flame(d, cx - 8, cy - 2, ELEM_PAL["fire"])
    m_ice(d, cx + 8, cy + 2, ELEM_PAL["ice"])
    m_wind(d, cx, cy - 10, ELEM_PAL["air"])


def m_cataclysm(d, cx, cy, p: Pal):
    m_tornado(d, cx, cy, ELEM_PAL["air"])
    m_embers(d, cx, cy + 4, ELEM_PAL["fire"])
    m_bolt(d, cx + 8, cy - 8, ELEM_PAL["air"])


# Spell → motif
MOTIF: dict[str, Callable] = {}


def _reg(names: list[str], fn: Callable):
    for n in names:
        MOTIF[n] = fn


_reg(["mental_push"], m_force)
_reg(["mental_sting"], m_shard)
_reg(["sense_phi"], m_eye)
_reg(["mind_probe"], m_probe)
_reg(["mind_lance", "thought_lance"], m_lance)
_reg(["psychic_focus", "psychic_amplify"], m_focus)
_reg(["mind_bolt"], m_bolt)
_reg(["psychic_scream"], m_scream)
_reg(["neural_lock"], m_lock)
_reg(["telekinetic_crush"], m_crush)
_reg(["mass_confusion"], m_fog)
_reg(["psychic_barrier"], m_shield)
_reg(["locus_echo"], m_echo)
_reg(["synaptic_overload"], m_synapse)
_reg(["psychic_drain"], m_drain)
_reg(["mental_fortress"], m_fortress)
_reg(["thought_bomb"], m_bomb)
_reg(["psychic_storm"], m_storm)
_reg(["mass_hysteria"], lambda d, cx, cy, p: (m_fog(d, cx, cy, p), m_terror(d, cx + 4, cy - 4, p)))
_reg(["omega_mind"], m_omega)
_reg(["mind_terror"], m_terror)
_reg(["cliff_urge"], m_urge_cliff)
_reg(["drown_urge"], m_urge_drown)
_reg(["psychic_frenzy"], m_frenzy)

_reg(["fire_burst", "great_fireball"], m_flame)
_reg(["sear"], m_sear)
_reg(["ore_smelt"], m_ore)
_reg(["wind_push", "weak_breeze", "air_shroud", "air_form"], m_wind)
_reg(["water_stream", "water_shroud"], m_water)
_reg(["steam_jet", "steam_veil"], m_steam)
_reg(["ember_volley"], m_embers)
_reg(["ice_shard", "hyper_cooling"], m_ice)
_reg(["frost_bastion", "ice_prison", "ward_glyph", "omega_ward"], m_frost_wall)
_reg(["plasma_bolt", "thermonuclear_pulse"], m_plasma)
_reg(["hydro_slice"], m_hydro_slice)
_reg(["steam_flight"], lambda d, cx, cy, p: (m_steam(d, cx, cy + 2, p), m_wind(d, cx, cy - 8, p)))
_reg(["air_hand"], m_hand)
_reg(["water_prison"], lambda d, cx, cy, p: (disc(d, cx, cy, 15, None, outline=rgba(p.primary), width=3), m_water(d, cx, cy, p)))
_reg(["vacuum_cage"], m_vacuum)
_reg(["water_shield"], m_shield)
_reg(["shockwave", "atmospheric_pressure", "sonic_lance"], m_sonic)
_reg(["ice_sheet", "cryo_wave"], lambda d, cx, cy, p: (m_wave(d, cx, cy, p), m_ice(d, cx, cy - 2, p)))
_reg(["breath_bubble"], m_bubble)
_reg(["air_ionization", "lightning_spear", "ion_storm"], m_bolt)
_reg(["mirage"], m_mirage)
_reg(["tornado", "hurricane_storm"], m_tornado)
_reg(["elemental_supremacy"], m_supremacy)
_reg(["absolute_zero"], lambda d, cx, cy, p: (m_frost_wall(d, cx, cy, p), disc(d, cx, cy, 4, rgba(p.spark))))
_reg(["meteorological_cataclysm"], m_cataclysm)
_reg(["quasar"], m_quasar)
_reg(["plasma_barrage"], lambda d, cx, cy, p: [disc(d, cx + ox, cy + oy, 4, rgba(p.primary)) or disc(d, cx + ox, cy + oy, 2, rgba(p.spark)) for ox, oy in ((-8, -5), (0, 0), (8, 5), (-4, 7), (5, -8))])

_reg(["vitality_pulse", "verdant_mend", "blood_stasis", "biological_field", "super_regeneration",
      "absolute_regeneration", "symbiotic_graft", "limb_regeneration", "vital_infusion",
      "soothing_sap", "vital_ward", "life_creation", "biological_immortality"], m_cells)
_reg(["thorn_lash"], m_thorn_lash)
_reg(["root_bind"], m_roots)
_reg(["briar_surge", "scorched_earth", "verdant_bloom"], lambda d, cx, cy, p: (m_roots(d, cx, cy, p), m_thorns(d, cx, cy - 2, p)))
_reg(["diagnostic_glimpse", "life_sense", "sense_sharpening", "pain_inhibitor", "metabolic_shock",
      "metabolic_boost", "adrenal_gift"], m_nerve)
_reg(["bio_strike", "muscle_spasm", "beast_form", "biological_cleaving"], m_muscle)
_reg(["bone_needle", "bone_spur"], m_bone)
_reg(["foreign_agent", "immune_suppression", "population_control"], m_virus)
_reg(["chitin_plates", "living_armor"], m_chitin)
_reg(["acid_gland", "organic_necrosis"], m_sap)
_reg(["parasitic_infection"], m_parasite)
_reg(["poison_thorns"], m_thorns)
_reg(["bio_mimicry"], m_veil)
_reg(["organism_adaptation", "full_restructuring", "bio_fission", "cellular_dominion",
      "evolutionary_leap", "genetic_lock", "full_transformation", "biological_singularity"], m_dna)
_reg(["biological_plague", "spore_storm"], m_spore)
_reg(["bio_cataclysm"], m_cataclysm)

_reg(["soul_drain", "life_tap"], m_soul)
_reg(["wither_touch", "death_mark"], m_wither)
_reg(["shade_summon", "death_shadow", "phantom_step"], m_shade)
_reg(["grave_leech", "siphon_pulse", "tainted_leech"], m_siphon)
_reg(["shade_swarm", "shade_brood", "army_of_dead"], m_shades)
_reg(["bone_chill", "bone_armor", "bone_volley"], m_bone)
_reg(["death_sense"], m_eye)
_reg(["grave_whisper"], lambda d, cx, cy, p: (d.rectangle((cx - 9, cy - 4, cx + 9, cy + 14), fill=rgba(p.secondary)), d.ellipse((cx - 9, cy - 14, cx + 9, cy), fill=rgba(p.secondary)), disc(d, cx, cy - 2, 3, rgba(p.primary))))
_reg(["wither_wave"], lambda d, cx, cy, p: (m_wave(d, cx, cy, p), m_wither(d, cx, cy, p)))
_reg(["dark_pact"], lambda d, cx, cy, p: (m_soul(d, cx - 4, cy, p), m_skull(d, cx + 6, cy, p)))
_reg(["soul_shackle", "grave_bind", "decay_bind"], m_chain)
_reg(["grave_field", "necrotic_aura"], lambda d, cx, cy, p: (m_fog(d, cx, cy, p), m_soul(d, cx, cy, p)))
_reg(["raise_skeleton", "raise_zombie"], m_skull)
_reg(["lich_ward"], m_shield)
_reg(["death_coil"], m_coil)
_reg(["soul_cataclysm"], m_synapse)
_reg(["death_apotheosis", "lich_ascension"], m_apotheosis)
_reg(["necrotic_bolt"], m_plague)
_reg(["curse_of_frailty"], m_mark)
_reg(["haunting_visage"], m_terror)
_reg(["corpse_burst"], m_bomb)
_reg(["soul_anchor"], m_anchor)
_reg(["death_gate"], m_gate)
_reg(["soul_reaper"], m_reaper)
_reg(["phylactery_surge"], lambda d, cx, cy, p: (d.rectangle((cx - 8, cy - 4, cx + 8, cy + 12), outline=rgba(p.secondary), width=2), m_soul(d, cx, cy, p)))

_reg(["blink", "far_blink"], m_blink)
_reg(["rift_yank", "warp_exchange"], m_rift)
_reg(["phase_veil"], m_veil)
_reg(["void_step", "absolute_fold"], m_void)
_reg(["gravity_well", "gravity_snare", "spatial_singularity"], m_well)
_reg(["warp_bolt", "rift_slash", "void_lance"], m_lance)
_reg(["spatial_ward"], m_shield)
_reg(["fold_repulse"], m_arrow)
_reg(["dimensional_anchor"], m_chain)
_reg(["spatial_surge"], m_wave)
_reg(["rift_burst"], m_pulse)

_reg(["corrupt_mark", "blight_brand", "prey_mark"], m_corrupt)
_reg(["binding_seal"], m_binding)
_reg(["blight_pulse", "blight_surge", "pestilence_wave", "virulent_wave", "omega_blight"], m_blight)
_reg(["rot_touch"], m_rot)
_reg(["entropy_lash"], m_lash)
_reg(["plague_bolt"], m_plague)
_reg(["festering_wound"], m_wound)
_reg(["miasma_cloak"], m_miasma)
_reg(["blight_field"], lambda d, cx, cy, p: (m_fog(d, cx, cy, p), m_blight(d, cx, cy, p)))
_reg(["entropy_aegis"], m_shield)
_reg(["plague_crown"], m_crown)

_reg(["trap_seal", "shock_glyph", "shock_trap"], m_trap)
_reg(["fortify_seal", "anchor_fortify"], m_shield)
_reg(["glow_seal", "permanent_glow"], m_glow)
_reg(["snare_glyph", "snare_matrix"], m_snare)
_reg(["beacon_seal"], m_beacon)
_reg(["repulsion_seal"], m_wind)

_reg(["psi_adrenaline"], m_adrenaline)
_reg(["phi_glow"], m_glow)
_reg(["psi_charge"], m_charge)
_reg(["psi_link"], m_link)
_reg(["psi_ward"], m_ward)

# Elemental spell → interior palette key
ELEM_SPELL: dict[str, str] = {
    "fire_burst": "fire", "sear": "fire", "ore_smelt": "fire", "ember_volley": "fire",
    "great_fireball": "fire", "thermonuclear_pulse": "plasma",
    "ice_shard": "ice", "frost_bastion": "ice", "ice_sheet": "ice", "ice_prison": "ice",
    "hyper_cooling": "ice", "cryo_wave": "ice", "absolute_zero": "ice",
    "water_stream": "water", "hydro_slice": "water", "water_prison": "water",
    "water_shield": "water", "breath_bubble": "water", "water_shroud": "water",
    "wind_push": "air", "air_hand": "air", "shockwave": "air", "sonic_lance": "air",
    "tornado": "air", "weak_breeze": "air", "air_ionization": "air", "lightning_spear": "air",
    "air_shroud": "air", "atmospheric_pressure": "air", "air_form": "air",
    "hurricane_storm": "air", "ion_storm": "air",
    "steam_jet": "steam", "steam_veil": "steam", "steam_flight": "steam", "mirage": "steam",
    "plasma_bolt": "plasma", "plasma_barrage": "plasma", "quasar": "plasma",
    "vacuum_cage": "plasma",
}


def resolve_pal(school: str, spell: str) -> Pal:
    if school == "elemental":
        key = ELEM_SPELL.get(spell)
        if key:
            return ELEM_PAL[key]
    return SCHOOL_PAL[school]


def make_icon(spell: str, school: str) -> Image.Image:
    """Reference-style pixel icon: flat dark well, chunky motif, thin circular frame."""
    pal = resolve_pal(school, spell)
    frame_pal = SCHOOL_PAL[school]
    common = school == "common"
    img, d, cx, cy = paint_plate(frame_pal, common=common)

    # Mild elemental wash (still flat, not LoL painterly)
    if school == "elemental" and pal is not frame_pal:
        disc(d, cx, cy, 20, rgba(pal.bg_edge, 160))

    fn = MOTIF.get(spell)
    if fn is None:
        m_glyph(d, cx, cy, pal)
    else:
        fn(d, cx, cy, pal)

    # Chunky pixel look, then clean circular mask + crisp frame
    img = apply_circle_mask(img)
    img = pixelate(img, logical=32)
    img = apply_circle_mask(img)
    img = paint_frame(img, frame_pal, common=common)
    return apply_circle_mask(img)

