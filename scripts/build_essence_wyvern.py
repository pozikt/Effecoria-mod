#!/usr/bin/env python3
"""
Build Essence Wyvern (Φ-Виверна) — classical wyvern, NO front legs.

Apex silhouette (not chicken): elongated body, long neck/tail, HUGE wings.
16 model units ≈ 1 block. Standing ~3.5 blocks tall; wingspan open ~10 blocks.

  python scripts/build_essence_wyvern.py
"""

from __future__ import annotations

import json
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/effecoria"
ART = ROOT / "art/essence_wyvern"
ATLAS = 256

# Horizontal predatory stance: chest low-forward, head high on long neck,
# massive wings (visible even when folded), long tapering tail, thick hind legs.
# Feet at y=0. NO front legs.
PARTS = {
    "body": {
        "parent": "root",
        "pivot": (0, 28, 0),
        "cubes": [
            # deep elongated torso (~1×0.9×2 blocks)
            ((-8, 20, -14), (16, 14, 30), (0, 0)),
            # shoulder ridge
            ((-7, 32, -10), (14, 4, 10), (0, 48)),
            # dorsal crest spines (gold)
            ((-1, 34, -8), (2, 5, 3), (80, 0)),
            ((-1, 34, -2), (2, 6, 3), (90, 0)),
            ((-1, 33, 4), (2, 5, 3), (100, 0)),
            ((-1, 32, 10), (2, 4, 3), (110, 0)),
        ],
    },
    "neck": {
        "parent": "body",
        "pivot": (0, 30, -14),
        "cubes": [
            # long forward+up neck (not stubby chicken)
            ((-3.5, 28, -34), (7, 7, 22), (0, 64)),
            ((-2.5, 34, -30), (5, 3, 4), (120, 0)),  # neck crest
        ],
    },
    "head": {
        "parent": "neck",
        "pivot": (0, 34, -34),
        "cubes": [
            # massive skull / crushing jaws
            ((-5, 30, -48), (10, 8, 14), (64, 48)),
            # snout
            ((-3.5, 31, -56), (7, 5, 8), (120, 48)),
            # lower jaw plate
            ((-3, 28, -52), (6, 3, 10), (150, 48)),
            # horns
            ((-5, 37, -40), (3, 6, 3), (180, 0)),
            ((2, 37, -40), (3, 6, 3), (190, 0)),
            # brow crest
            ((-1, 38, -46), (2, 4, 4), (200, 0)),
        ],
    },
    # Wing chain (Tiny Dragons / Antarchy brutalfly lesson):
    # shoulder → mid → tip. Flat membranes (thin Y). NO front legs.
    "left_wing": {
        "parent": "body",
        "pivot": (-8, 30, -6),
        "cubes": [
            # humerus / wing arm
            ((-22, 28, -9), (14, 4, 7), (0, 100)),
            # inner membrane
            ((-22, 29, -12), (14, 1, 18), (50, 100)),
            # thumb claw
            ((-12, 29, -12), (3, 3, 4), (100, 100)),
        ],
    },
    "left_wing_mid": {
        "parent": "left_wing",
        "pivot": (-22, 30, -6),
        "cubes": [
            # radius spar
            ((-40, 29, -8), (18, 3, 5), (0, 120)),
            # mid membrane sail
            ((-40, 29.5, -16), (18, 1, 24), (40, 120)),
        ],
    },
    "left_wing_tip": {
        "parent": "left_wing_mid",
        "pivot": (-40, 30, -6),
        "cubes": [
            # tip spar
            ((-56, 29, -7), (16, 2, 4), (0, 150)),
            # tip membrane
            ((-56, 29.5, -14), (16, 1, 20), (40, 150)),
        ],
    },
    "right_wing": {
        "parent": "body",
        "pivot": (8, 30, -6),
        "cubes": [
            ((8, 28, -9), (14, 4, 7), (120, 100)),
            ((8, 29, -12), (14, 1, 18), (170, 100)),
            ((9, 29, -12), (3, 3, 4), (210, 100)),
        ],
    },
    "right_wing_mid": {
        "parent": "right_wing",
        "pivot": (22, 30, -6),
        "cubes": [
            ((22, 29, -8), (18, 3, 5), (120, 120)),
            ((22, 29.5, -16), (18, 1, 24), (160, 120)),
        ],
    },
    "right_wing_tip": {
        "parent": "right_wing_mid",
        "pivot": (40, 30, -6),
        "cubes": [
            ((40, 29, -7), (16, 2, 4), (120, 150)),
            ((40, 29.5, -14), (16, 1, 20), (160, 150)),
        ],
    },
    # Hind legs — Tiny Dragons: hip(thigh) → shin → foot. Digitigrade hock.
    "left_leg": {
        "parent": "root",
        "pivot": (-7, 22, 6),
        "cubes": [
            ((-12, 12, 1), (10, 12, 11), (0, 180)),
        ],
    },
    "left_leg_shin": {
        "parent": "left_leg",
        "pivot": (-7, 13, 8),
        "cubes": [
            ((-10, 3, 5), (6, 11, 6), (40, 180)),
        ],
    },
    "left_leg_foot": {
        "parent": "left_leg_shin",
        "pivot": (-7, 3, 8),
        "cubes": [
            ((-11, 0, -3), (8, 3, 12), (70, 180)),
            ((-11, 0, -6), (2, 2, 3), (110, 180)),
            ((-8, 0, -7), (2, 2, 4), (120, 180)),
            ((-5, 0, -6), (2, 2, 3), (130, 180)),
            ((-2, 0, -4), (2, 2, 3), (140, 180)),
        ],
    },
    "right_leg": {
        "parent": "root",
        "pivot": (7, 22, 6),
        "cubes": [
            ((2, 12, 1), (10, 12, 11), (160, 180)),
        ],
    },
    "right_leg_shin": {
        "parent": "right_leg",
        "pivot": (7, 13, 8),
        "cubes": [
            ((4, 3, 5), (6, 11, 6), (200, 180)),
        ],
    },
    "right_leg_foot": {
        "parent": "right_leg_shin",
        "pivot": (7, 3, 8),
        "cubes": [
            ((3, 0, -3), (8, 3, 12), (70, 220)),
            ((3, 0, -6), (2, 2, 3), (110, 220)),
            ((6, 0, -7), (2, 2, 4), (120, 220)),
            ((9, 0, -6), (2, 2, 3), (130, 220)),
            ((12, 0, -4), (2, 2, 3), (140, 220)),
        ],
    },
    "tail": {
        "parent": "body",
        "pivot": (0, 26, 16),
        "cubes": [
            # long tapering whip (~3.5 blocks)
            ((-4, 22, 16), (8, 8, 18), (200, 48)),
            ((-3, 20, 34), (6, 6, 16), (200, 80)),
            ((-2, 18, 50), (4, 4, 14), (200, 110)),
            ((-1, 17, 64), (2, 3, 10), (220, 130)),
            # crest spines along tail
            ((-1, 30, 22), (2, 4, 3), (210, 0)),
            ((-1, 28, 38), (2, 4, 3), (220, 0)),
            ((-1, 24, 52), (2, 3, 3), (230, 0)),
        ],
    },
}

ROCK = (68, 64, 58, 255)
ROCK_MID = (88, 82, 74, 255)
ROCK_DARK = (42, 38, 34, 255)
ROCK_HI = (115, 108, 96, 255)
ULTRA = (24, 36, 88, 255)
GOLD = (220, 175, 45, 255)
GOLD_HI = (255, 230, 120, 255)
GOLD_LO = (160, 110, 28, 255)
EYE = (255, 210, 60, 255)
EYE_CORE = (255, 250, 200, 255)
MEMBRANE = (38, 36, 44, 255)
MEMBRANE_VEIN = (200, 160, 40, 255)
SCALE_EDGE = (55, 50, 45, 255)


def box_faces(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + 2 * d + w, v + d, w, h),
    }


def put(im, x, y, c, u0, v0, fw, fh):
    if u0 <= x < u0 + fw and v0 <= y < v0 + fh:
        im.putpixel((x, y), c)


def paint_face(im, box, *, part, face, wing=False, crest=False, eye=False, claw=False):
    u, v, w, h = box
    if w <= 0 or h <= 0:
        return
    draw = ImageDraw.Draw(im)
    rng = random.Random(hash((part, face, w, h, u, v)) & 0xFFFFFFFF)

    if claw:
        draw.rectangle([u, v, u + w - 1, v + h - 1], fill=ROCK_DARK)
        if w >= 1 and h >= 1:
            im.putpixel((u + w // 2, v), GOLD_LO)
        return

    if wing:
        draw.rectangle([u, v, u + w - 1, v + h - 1], fill=MEMBRANE)
        for y in range(v, v + h):
            for x in range(u, u + w):
                if rng.random() < 0.1:
                    im.putpixel((x, y), ROCK_DARK)
                elif rng.random() < 0.04:
                    im.putpixel((x, y), ULTRA)
        # Φ-fiber veins along span
        step = max(3, w // 6)
        for i in range(0, w, step):
            x = u + i
            for y in range(v, v + h):
                ox = ((y // 2) % 3) - 1
                put(im, x + ox, y, MEMBRANE_VEIN if y % 2 == 0 else GOLD_LO, u, v, w, h)
        draw.rectangle([u, v, u + w - 1, v + h - 1], outline=ROCK_DARK)
        return

    if crest:
        draw.rectangle([u, v, u + w - 1, v + h - 1], fill=GOLD_LO)
        for y in range(v, v + h):
            for x in range(u, u + w):
                if rng.random() < 0.25:
                    im.putpixel((x, y), GOLD if y < v + h // 2 else GOLD_HI)
        return

    # corundum rock scales
    for y in range(v, v + h):
        for x in range(u, u + w):
            n = (x * 3 + y * 7 + rng.randint(0, 2)) % 5
            col = [ROCK, ROCK_MID, ROCK_DARK, ROCK_HI, ULTRA][n]
            if rng.random() < 0.07:
                col = ULTRA
            im.putpixel((x, y), col)
    # scale hatch
    for y in range(v + 1, v + h - 1, 3):
        for x in range(u + 1, u + w - 1, 3):
            put(im, x, y, SCALE_EDGE, u, v, w, h)
    # sparse gold Φ-lines
    if w >= 4 and h >= 4:
        x0 = u + w // 2
        for y in range(v + 1, v + h - 1, 2):
            put(im, x0 + ((y // 2) % 3) - 1, y, GOLD if y % 4 == 0 else GOLD_LO, u, v, w, h)
    if w > 2 and h > 2:
        draw.rectangle([u, v, u + w - 1, v + h - 1], outline=ROCK_DARK)

    if eye and face == "front" and w >= 5 and h >= 4:
        cx, cy = u + w // 2 - 1, v + h // 2
        for dx, dy, c in (
            (0, 0, EYE_CORE),
            (1, 0, EYE),
            (-1, 0, EYE),
            (0, 1, GOLD),
            (0, -1, GOLD_LO),
            (2, 0, GOLD),
            (-2, 0, GOLD),
        ):
            put(im, cx + dx, cy + dy, c, u, v, w, h)

    px = im.load()
    for y in range(v, v + h):
        for x in range(u, u + w):
            r, g, b, a = px[x, y]
            if a > 0:
                px[x, y] = (r, g, b, 255)


def classify_cube(name: str, i: int) -> dict:
    if "wing" in name:
        # shoulder: 0 arm, 1 membrane, 2 claw; mid/tip: 0 spar, 1 membrane
        if name in ("left_wing", "right_wing"):
            if i == 2:
                return {"claw": True}
            if i == 1:
                return {"wing": True}
            return {}
        if i >= 1:
            return {"wing": True}
        return {}
    if name == "body" and i >= 2:
        return {"crest": True}
    if name == "neck" and i >= 1:
        return {"crest": True}
    if name == "head" and i >= 3:
        return {"crest": True}
    if name == "tail" and i >= 4:
        return {"crest": True}
    if name.endswith("_foot"):
        if i >= 1:
            return {"claw": True}
        return {}
    if name.endswith("_shin"):
        return {}
    if name in ("left_leg", "right_leg"):
        return {}
    if name.endswith("leg") and i >= 3:
        return {"claw": True}
    if name == "head" and i == 0:
        return {"eye": True}
    return {}


def build_geo():
    bones = [{"name": "root", "pivot": [0, 0, 0]}]
    order = [
        "body",
        "neck",
        "head",
        "left_wing",
        "left_wing_mid",
        "left_wing_tip",
        "right_wing",
        "right_wing_mid",
        "right_wing_tip",
        "left_leg",
        "left_leg_shin",
        "left_leg_foot",
        "right_leg",
        "right_leg_shin",
        "right_leg_foot",
        "tail",
    ]
    for name in order:
        meta = PARTS[name]
        bones.append(
            {
                "name": name,
                "parent": meta["parent"],
                "pivot": list(meta["pivot"]),
                "cubes": [
                    {
                        "origin": [round(c, 2) for c in o],
                        "size": [round(c, 2) for c in s],
                        "uv": list(uv),
                    }
                    for o, s, uv in meta["cubes"]
                ],
            }
        )
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": "geometry.essence_wyvern",
                    "texture_width": ATLAS,
                    "texture_height": ATLAS,
                    "visible_bounds_width": 14,
                    "visible_bounds_height": 6,
                    "visible_bounds_offset": [0, 2.0, 0],
                },
                "bones": bones,
            }
        ],
    }


def build_atlas():
    im = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))
    for name, meta in PARTS.items():
        for i, (_o, size, uv) in enumerate(meta["cubes"]):
            w, h, d = [int(round(c)) for c in size]
            faces = box_faces(uv[0], uv[1], w, h, d)
            flags = classify_cube(name, i)
            for face, box in faces.items():
                paint_face(im, box, part=name, face=face, **flags)
    return im


def rot_kf(items):
    return {str(round(t, 3)): {"vector": [round(x, 2), round(y, 2), round(z, 2)]} for t, x, y, z in items}


def build_animations():
    # Lessons from Tiny Dragons (3-bone wing) + Antarchy brutalfly fly:
    # root Z large swing, mid/tip lag with opposite Z; slight Y for sweep.
    # Idle: folded cloak (mid Z large), not rigid flat boards.
    return {
        "format_version": "1.8.0",
        "animations": {
            "animation.essence_wyvern.idle": {
                "loop": True,
                "animation_length": 2.0,
                "bones": {
                    "body": {"rotation": rot_kf([(0, 8, 0, 0), (0.5, 10, 0, 0), (1.0, 8, 0, 0), (1.5, 10, 0, 0), (2.0, 8, 0, 0)])},
                    "neck": {"rotation": rot_kf([(0, -12, 0, 0), (1.0, -8, 4, 0), (2.0, -12, 0, 0)])},
                    "head": {"rotation": rot_kf([(0, 6, 0, 0), (1.0, 4, -3, 0), (2.0, 6, 0, 0)])},
                    "left_wing": {"rotation": rot_kf([(0, 5, 25, 20), (1.0, 4, 22, 16), (2.0, 5, 25, 20)])},
                    "left_wing_mid": {"rotation": rot_kf([(0, 0, 10, -95), (1.0, 0, 8, -100), (2.0, 0, 10, -95)])},
                    "left_wing_tip": {"rotation": rot_kf([(0, 0, 0, -20), (1.0, 0, 0, -15), (2.0, 0, 0, -20)])},
                    "right_wing": {"rotation": rot_kf([(0, 5, -25, -20), (1.0, 4, -22, -16), (2.0, 5, -25, -20)])},
                    "right_wing_mid": {"rotation": rot_kf([(0, 0, -10, 95), (1.0, 0, -8, 100), (2.0, 0, -10, 95)])},
                    "right_wing_tip": {"rotation": rot_kf([(0, 0, 0, 20), (1.0, 0, 0, 15), (2.0, 0, 0, 20)])},
                    "tail": {"rotation": rot_kf([(0, 12, 10, 0), (1.0, 10, -10, 0), (2.0, 12, 10, 0)])},
                    # Tiny Dragons idle: hip ~20↔12.5, shin 0↔22.5, foot 0↔25 (soft weight shift)
                    "left_leg": {"rotation": rot_kf([(0, 18, 0, -4), (0.5, 12, 0, -4), (1.0, 18, 0, -4), (1.5, 12, 0, -4), (2.0, 18, 0, -4)])},
                    "left_leg_shin": {"rotation": rot_kf([(0, 8, 0, 0), (0.5, 22, 0, 0), (1.0, 8, 0, 0), (1.5, 22, 0, 0), (2.0, 8, 0, 0)])},
                    "left_leg_foot": {"rotation": rot_kf([(0, -5, 0, 0), (0.5, 18, 0, 0), (1.0, -5, 0, 0), (1.5, 18, 0, 0), (2.0, -5, 0, 0)])},
                    "right_leg": {"rotation": rot_kf([(0, 18, 0, 4), (0.5, 12, 0, 4), (1.0, 18, 0, 4), (1.5, 12, 0, 4), (2.0, 18, 0, 4)])},
                    "right_leg_shin": {"rotation": rot_kf([(0, 8, 0, 0), (0.5, 22, 0, 0), (1.0, 8, 0, 0), (1.5, 22, 0, 0), (2.0, 8, 0, 0)])},
                    "right_leg_foot": {"rotation": rot_kf([(0, -5, 0, 0), (0.5, 18, 0, 0), (1.0, -5, 0, 0), (1.5, 18, 0, 0), (2.0, -5, 0, 0)])},
                },
            },
            "animation.essence_wyvern.walk": {
                "loop": True,
                "animation_length": 1.0,
                "bones": {
                    # Alternating digitigrade stride (Tiny Dragons joint ranges, opposite phase)
                    "left_leg": {
                        "rotation": rot_kf(
                            [(0, 32, 0, -6), (0.25, 8, 0, -2), (0.5, -28, 0, 6), (0.75, 5, 0, 0), (1.0, 32, 0, -6)]
                        )
                    },
                    "left_leg_shin": {
                        "rotation": rot_kf(
                            [(0, 12, 0, 0), (0.25, 38, 0, 0), (0.5, 8, 0, 0), (0.75, 28, 0, 0), (1.0, 12, 0, 0)]
                        )
                    },
                    "left_leg_foot": {
                        "rotation": rot_kf(
                            [(0, -8, 0, 0), (0.25, 28, 0, 0), (0.5, -12, 0, 0), (0.75, 15, 0, 0), (1.0, -8, 0, 0)]
                        )
                    },
                    "right_leg": {
                        "rotation": rot_kf(
                            [(0, -28, 0, 6), (0.25, 5, 0, 0), (0.5, 32, 0, -6), (0.75, 8, 0, -2), (1.0, -28, 0, 6)]
                        )
                    },
                    "right_leg_shin": {
                        "rotation": rot_kf(
                            [(0, 8, 0, 0), (0.25, 28, 0, 0), (0.5, 12, 0, 0), (0.75, 38, 0, 0), (1.0, 8, 0, 0)]
                        )
                    },
                    "right_leg_foot": {
                        "rotation": rot_kf(
                            [(0, -12, 0, 0), (0.25, 15, 0, 0), (0.5, -8, 0, 0), (0.75, 28, 0, 0), (1.0, -12, 0, 0)]
                        )
                    },
                    "body": {
                        "rotation": rot_kf(
                            [(0, 12, 0, 4), (0.5, 12, 0, -4), (1.0, 12, 0, 4)]
                        ),
                        "position": {
                            "0": {"vector": [0, 0, 0]},
                            "0.25": {"vector": [0, -1.5, 0]},
                            "0.5": {"vector": [0, 0, 0]},
                            "0.75": {"vector": [0, -1.5, 0]},
                            "1": {"vector": [0, 0, 0]},
                        },
                    },
                    "tail": {"rotation": rot_kf([(0, 6, 22, 0), (0.5, 6, -22, 0), (1.0, 6, 22, 0)])},
                    "neck": {"rotation": rot_kf([(0, -8, 0, 0), (0.5, -6, 3, 0), (1.0, -8, 0, 0)])},
                    "left_wing": {"rotation": rot_kf([(0, 12, 35, 8), (0.5, 10, 32, 12), (1.0, 12, 35, 8)])},
                    "left_wing_mid": {"rotation": rot_kf([(0, 5, 5, -70), (1.0, 5, 5, -70)])},
                    "left_wing_tip": {"rotation": rot_kf([(0, 15, 0, 35), (1.0, 15, 0, 35)])},
                    "right_wing": {"rotation": rot_kf([(0, 12, -35, -8), (0.5, 10, -32, -12), (1.0, 12, -35, -8)])},
                    "right_wing_mid": {"rotation": rot_kf([(0, 5, -5, 70), (1.0, 5, -5, 70)])},
                    "right_wing_tip": {"rotation": rot_kf([(0, 15, 0, -35), (1.0, 15, 0, -35)])},
                },
            },
            # Resting: crouch on hind legs, wing knuckles planted as fore-props (wyvern sit)
            "animation.essence_wyvern.sit": {
                "loop": True,
                "animation_length": 4.0,
                "bones": {
                    "body": {
                        "rotation": rot_kf([(0, 22, 0, 0), (2.0, 20, 0, 0), (4.0, 22, 0, 0)]),
                        "position": {
                            "0": {"vector": [0, -6, 2]},
                            "2": {"vector": [0, -5.5, 2]},
                            "4": {"vector": [0, -6, 2]},
                        },
                    },
                    "neck": {"rotation": rot_kf([(0, -5, 0, 0), (2.0, -2, 4, 0), (4.0, -5, 0, 0)])},
                    "head": {"rotation": rot_kf([(0, 8, 0, 0), (2.0, 6, -3, 0), (4.0, 8, 0, 0)])},
                    "left_leg": {"rotation": rot_kf([(0, -35, 10, -10), (4.0, -35, 10, -10)])},
                    "left_leg_shin": {"rotation": rot_kf([(0, 55, 0, 0), (4.0, 55, 0, 0)])},
                    "left_leg_foot": {"rotation": rot_kf([(0, 15, 0, 0), (4.0, 15, 0, 0)])},
                    "right_leg": {"rotation": rot_kf([(0, -35, -10, 10), (4.0, -35, -10, 10)])},
                    "right_leg_shin": {"rotation": rot_kf([(0, 55, 0, 0), (4.0, 55, 0, 0)])},
                    "right_leg_foot": {"rotation": rot_kf([(0, 15, 0, 0), (4.0, 15, 0, 0)])},
                    "tail": {"rotation": rot_kf([(0, 25, 0, 0), (2.0, 22, 6, 0), (4.0, 25, 0, 0)])},
                    "left_wing": {"rotation": rot_kf([(0, 55, 15, 55), (2.0, 52, 12, 52), (4.0, 55, 15, 55)])},
                    "left_wing_mid": {"rotation": rot_kf([(0, 25, 0, 25), (4.0, 25, 0, 25)])},
                    "left_wing_tip": {"rotation": rot_kf([(0, 10, 0, 40), (4.0, 10, 0, 40)])},
                    "right_wing": {"rotation": rot_kf([(0, 55, -15, -55), (2.0, 52, -12, -52), (4.0, 55, -15, -55)])},
                    "right_wing_mid": {"rotation": rot_kf([(0, 25, 0, -25), (4.0, 25, 0, -25)])},
                    "right_wing_tip": {"rotation": rot_kf([(0, 10, 0, -40), (4.0, 10, 0, -40)])},
                },
            },
            "animation.essence_wyvern.fly": {
                "loop": True,
                # brutalfly fly is ~0.25s; dragon cruise slower — compromise
                "animation_length": 0.5,
                "bones": {
                    # Downstroke Z+ → upstroke Z- on left (mirrored right), with Y sweep
                    "left_wing": {
                        "rotation": rot_kf(
                            [(0, 0, -20, -55), (0.25, 0, -35, 45), (0.5, 0, -20, -55)]
                        )
                    },
                    "left_wing_mid": {
                        "rotation": rot_kf(
                            [(0, 0, 5, -25), (0.25, 0, 0, 20), (0.5, 0, 5, -25)]
                        )
                    },
                    "left_wing_tip": {
                        "rotation": rot_kf(
                            [(0, 0, 0, -15), (0.25, 0, 0, 25), (0.5, 0, 0, -15)]
                        )
                    },
                    "right_wing": {
                        "rotation": rot_kf(
                            [(0, 0, 20, 55), (0.25, 0, 35, -45), (0.5, 0, 20, 55)]
                        )
                    },
                    "right_wing_mid": {
                        "rotation": rot_kf(
                            [(0, 0, -5, 25), (0.25, 0, 0, -20), (0.5, 0, -5, 25)]
                        )
                    },
                    "right_wing_tip": {
                        "rotation": rot_kf(
                            [(0, 0, 0, 15), (0.25, 0, 0, -25), (0.5, 0, 0, 15)]
                        )
                    },
                    "body": {"rotation": rot_kf([(0, 16, 0, 0), (0.25, 10, 0, 0), (0.5, 16, 0, 0)])},
                    "neck": {"rotation": rot_kf([(0, -5, 0, 0), (0.5, -5, 0, 0)])},
                    "tail": {"rotation": rot_kf([(0, -12, 0, 0), (0.25, -8, 10, 0), (0.5, -12, 0, 0)])},
                    "left_leg": {"rotation": rot_kf([(0, 45, 0, 0), (0.5, 45, 0, 0)])},
                    "left_leg_shin": {"rotation": rot_kf([(0, 35, 0, 0), (0.5, 35, 0, 0)])},
                    "left_leg_foot": {"rotation": rot_kf([(0, 25, 0, 0), (0.5, 25, 0, 0)])},
                    "right_leg": {"rotation": rot_kf([(0, 45, 0, 0), (0.5, 45, 0, 0)])},
                    "right_leg_shin": {"rotation": rot_kf([(0, 35, 0, 0), (0.5, 35, 0, 0)])},
                    "right_leg_foot": {"rotation": rot_kf([(0, 25, 0, 0), (0.5, 25, 0, 0)])},
                },
            },
            "animation.essence_wyvern.attack": {
                "animation_length": 1.0,
                "bones": {
                    "neck": {"rotation": rot_kf([(0, -10, 0, 0), (0.3, -35, 0, 0), (0.55, 40, 0, 0), (1, -10, 0, 0)])},
                    "head": {"rotation": rot_kf([(0, 0, 0, 0), (0.3, -20, 0, 0), (0.55, 25, 0, 0), (1, 0, 0, 0)])},
                    "body": {"rotation": rot_kf([(0, 8, 0, 0), (0.55, 18, 0, 0), (1, 8, 0, 0)])},
                },
            },
            "animation.essence_wyvern.breath": {
                "animation_length": 1.6,
                "bones": {
                    "neck": {"rotation": rot_kf([(0, -15, 0, 0), (0.6, -30, 0, 0), (1.0, 8, 0, 0), (1.6, -10, 0, 0)])},
                    "head": {"rotation": rot_kf([(0, -8, 0, 0), (0.8, -18, 0, 0), (1.1, 5, 0, 0), (1.6, 0, 0, 0)])},
                    "body": {"rotation": rot_kf([(0, 5, 0, 0), (0.8, -5, 0, 0), (1.6, 8, 0, 0)])},
                    "left_wing": {"rotation": rot_kf([(0, 0, -15, -30), (0.8, 0, -25, -45), (1.6, 5, 25, 20)])},
                    "left_wing_mid": {"rotation": rot_kf([(0, 0, 0, -10), (0.8, 0, 0, 5), (1.6, 0, 10, -95)])},
                    "right_wing": {"rotation": rot_kf([(0, 0, 15, 30), (0.8, 0, 25, 45), (1.6, 5, -25, -20)])},
                    "right_wing_mid": {"rotation": rot_kf([(0, 0, 0, 10), (0.8, 0, 0, -5), (1.6, 0, -10, 95)])},
                },
            },
            "animation.essence_wyvern.hurt": {
                "animation_length": 0.4,
                "bones": {
                    "body": {"rotation": rot_kf([(0, 8, 0, 0), (0.1, -5, 0, 10), (0.4, 8, 0, 0)])},
                    "left_wing": {"rotation": rot_kf([(0, 5, 25, 20), (0.1, 0, 10, 40), (0.4, 5, 25, 20)])},
                    "right_wing": {"rotation": rot_kf([(0, 5, -25, -20), (0.1, 0, -10, -40), (0.4, 5, -25, -20)])},
                },
            },
            "animation.essence_wyvern.death": {
                "animation_length": 1.8,
                "bones": {
                    "body": {"rotation": rot_kf([(0, 8, 0, 0), (0.8, 45, 0, 0), (1.8, 90, 0, 12)])},
                    "left_wing": {"rotation": rot_kf([(0, 5, 25, 20), (1.8, -10, 40, 70)])},
                    "left_wing_mid": {"rotation": rot_kf([(0, 0, 10, -95), (1.8, 0, 0, -20)])},
                    "right_wing": {"rotation": rot_kf([(0, 5, -25, -20), (1.8, -10, -40, -70)])},
                    "right_wing_mid": {"rotation": rot_kf([(0, 0, -10, 95), (1.8, 0, 0, 20)])},
                    "neck": {"rotation": rot_kf([(0, -12, 0, 0), (1.8, 35, 25, 0)])},
                    "tail": {"rotation": rot_kf([(0, 12, 0, 0), (1.8, 20, -30, 0)])},
                },
            },
        },
    }


def write_json(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2), encoding="utf-8")


def main():
    # UV island bounds check
    for name, meta in PARTS.items():
        for i, (_o, size, uv) in enumerate(meta["cubes"]):
            w, h, d = [int(round(c)) for c in size]
            need_w = 2 * d + 2 * w
            need_h = d + h
            if uv[0] + need_w > ATLAS or uv[1] + need_h > ATLAS:
                raise SystemExit(f"UV overflow {name}[{i}] uv={uv} need={need_w}x{need_h}")

    geo = build_geo()
    anims = build_animations()
    atlas = build_atlas()
    for p in (ASSETS / "geo/essence_wyvern.geo.json", ART / "essence_wyvern.geo.json"):
        write_json(p, geo)
    for p in (ASSETS / "animations/essence_wyvern.animation.json", ART / "essence_wyvern.animation.json"):
        write_json(p, anims)
    tex = ASSETS / "textures/entity/essence_wyvern.png"
    tex.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(tex)
    ART.mkdir(parents=True, exist_ok=True)
    atlas.save(ART / "essence_wyvern.png")
    atlas.resize((ATLAS * 2, ATLAS * 2), Image.NEAREST).save(ART / "uv_guide_preview.png")
    print("Rebuilt essence_wyvern — 3-bone wing chain (Tiny Dragons / Antarchy)")
    print(" ->", tex)


if __name__ == "__main__":
    main()
