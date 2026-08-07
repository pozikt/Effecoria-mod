#!/usr/bin/env python3
"""
Build Essence Wyvern from user contour segment method.

Source: art/essence_wyvern/concept_turnaround_contur.png + SEGMENT_LOCK.md

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
ATLAS = 512

# Concept palette — gray-beige stone + gold (NO purple)
STONE = (120, 118, 112, 255)
STONE_MID = (128, 122, 116, 255)
STONE_DARK = (56, 48, 40, 255)
STONE_HI = (148, 142, 132, 255)
BELLY = (104, 88, 72, 255)
BELLY_HI = (120, 100, 80, 255)
CLAW = (28, 24, 18, 255)
GOLD = (220, 175, 45, 255)
GOLD_HI = (255, 230, 110, 255)
GOLD_LO = (168, 120, 32, 255)
EYE = (255, 210, 40, 255)
EYE_CORE = (255, 240, 120, 255)
EYE_PUPIL = (12, 10, 8, 255)
MEMBRANE = (58, 52, 46, 255)
MEMBRANE_DARK = (40, 36, 32, 255)

PARTS: dict = {}
BONE_ORDER: list[str] = []
# UV packer state: [u, v, row_h]
_UV = [0, 0, 0]


def _alloc_uv(w: float, h: float, d: float) -> tuple[int, int]:
    iw, ih, id_ = max(1, int(round(w))), max(1, int(round(h))), max(1, int(round(d)))
    need_w = 2 * id_ + 2 * iw
    need_h = id_ + ih
    if _UV[0] + need_w > ATLAS:
        _UV[0] = 0
        _UV[1] += _UV[2] + 1
        _UV[2] = 0
    if _UV[1] + need_h > ATLAS:
        raise SystemExit(f"Atlas overflow at UV ({_UV[0]},{_UV[1]}) need {need_w}x{need_h}")
    u, v = _UV[0], _UV[1]
    _UV[0] += need_w + 1
    _UV[2] = max(_UV[2], need_h)
    return u, v


def add_bone(name: str, parent: str, pivot: tuple[float, float, float], cubes: list):
    packed = []
    for origin, size in cubes:
        packed.append((origin, size, _alloc_uv(*size)))
    PARTS[name] = {"parent": parent, "pivot": pivot, "cubes": packed}
    BONE_ORDER.append(name)


def build_parts():
    PARTS.clear()
    BONE_ORDER.clear()
    _UV[0] = _UV[1] = _UV[2] = 0

    # Torso: 3 blue plates (chest / mid / hips)
    add_bone(
        "body_1",
        "root",
        (0, 28, 0),
        [
            ((-8, 20, -4), (16, 14, 10)),
            ((-7, 20, -3), (14, 4, 8)),
            ((-1, 33, -1), (2, 6, 3)),
        ],
    )
    add_bone(
        "body_0",
        "body_1",
        (0, 29, -4),
        [
            ((-7.5, 21, -12), (15, 13, 8)),
            ((-6.5, 21, -11), (13, 3.5, 6)),
            ((-1, 33, -10), (2, 5, 3)),
        ],
    )
    add_bone(
        "body_2",
        "body_1",
        (0, 27, 6),
        [
            ((-7, 19, 6), (14, 12, 8)),
            ((-6, 19, 7), (12, 3.5, 6)),
            ((-1, 30, 8), (2, 5, 3)),
        ],
    )

    # Neck: 6 blue rings (contour front~4 + side density)
    for name, parent, pivot, w, h, d in (
        ("neck_0", "body_0", (0, 30, -12), 7.0, 7.0, 5.0),
        ("neck_1", "neck_0", (0, 32, -17), 6.6, 6.6, 5.0),
        ("neck_2", "neck_1", (0, 35, -22), 6.2, 6.2, 5.0),
        ("neck_3", "neck_2", (0, 38, -27), 5.8, 5.8, 5.0),
        ("neck_4", "neck_3", (0, 40, -32), 5.4, 5.4, 4.5),
        ("neck_5", "neck_4", (0, 41, -36), 5.0, 5.2, 4.5),
    ):
        px, py, pz = pivot
        add_bone(
            name,
            parent,
            pivot,
            [
                ((px - w / 2, py - h / 2, pz - d / 2), (w, h, d)),
                ((px - w * 0.4, py - h / 2, pz - d * 0.4), (w * 0.8, h * 0.35, d * 0.8)),
                ((px - 1, py + h / 2 - 0.5, pz - 1), (2, 4, 2)),
            ],
        )

    # Head: long predator snout (not pig stub) + jaw + side eyes + horns
    # Skull back at ~-40, snout tip ~-62
    add_bone(
        "head",
        "neck_5",
        (0, 42, -40),
        [
            ((-5, 37, -52), (10, 8, 14)),  # cranium (long)
            ((-3.5, 38, -62), (7, 5, 11)),  # upper jaw / snout
            ((-2.5, 40, -64), (5, 2.5, 4)),  # nose tip taper
        ],
    )
    add_bone(
        "jaw",
        "head",
        (0, 37, -48),
        [
            ((-3, 34, -61), (6, 3, 14)),  # long lower jaw matching snout
        ],
    )
    # Dedicated eye bulbs on skull sides (visible from front/side)
    add_bone(
        "left_eye",
        "head",
        (-5, 41, -46),
        [((-6.2, 39.5, -48), (2.2, 2.5, 2.5))],
    )
    add_bone(
        "right_eye",
        "head",
        (5, 41, -46),
        [((4.0, 39.5, -48), (2.2, 2.5, 2.5))],
    )
    for name, origin, size in (
        ("horn_0", (-4, 45, -42), (3, 5, 3)),
        ("horn_1", (1, 45, -42), (3, 5, 3)),
        ("horn_2", (-5, 43, -38), (2.5, 4, 3)),
        ("horn_3", (2.5, 43, -38), (2.5, 4, 3)),
        ("horn_4", (-1, 46, -45), (2, 3.5, 2.5)),
    ):
        ox, oy, oz = origin
        add_bone(name, "head", (ox + size[0] / 2, oy, oz + size[2] / 2), [(origin, size)])

    # Wings
    add_bone(
        "left_wing",
        "body_0",
        (-8, 30, -6),
        [
            ((-22, 28, -9), (14, 4, 7)),
            ((-22, 29, -12), (14, 1, 16)),
            ((-12, 29, -12), (3, 3, 4)),
        ],
    )
    add_bone(
        "left_wing_mid",
        "left_wing",
        (-22, 30, -6),
        [
            ((-40, 29, -8), (18, 3, 5)),
            ((-40, 29.5, -16), (18, 1, 22)),
        ],
    )
    add_bone(
        "left_wing_tip",
        "left_wing_mid",
        (-40, 30, -6),
        [
            ((-54, 29, -7), (14, 2, 4)),
            ((-54, 29.5, -14), (14, 1, 18)),
        ],
    )
    add_bone(
        "right_wing",
        "body_0",
        (8, 30, -6),
        [
            ((8, 28, -9), (14, 4, 7)),
            ((8, 29, -12), (14, 1, 16)),
            ((9, 29, -12), (3, 3, 4)),
        ],
    )
    add_bone(
        "right_wing_mid",
        "right_wing",
        (22, 30, -6),
        [
            ((22, 29, -8), (18, 3, 5)),
            ((22, 29.5, -16), (18, 1, 22)),
        ],
    )
    add_bone(
        "right_wing_tip",
        "right_wing_mid",
        (40, 30, -6),
        [
            ((40, 29, -7), (14, 2, 4)),
            ((40, 29.5, -14), (14, 1, 18)),
        ],
    )

    # Legs on root (not body) so torso bob/sit does not yank feet off the ground.
    # Bind pose: foot pad bottom at y=0, claws at y=0.
    for side, hip_x in (("left", -7.0), ("right", 7.0)):
        add_bone(f"{side}_leg", "root", (hip_x, 20, 4), [((hip_x - 5, 12, -1), (10, 10, 10))])
        add_bone(f"{side}_leg_knee", f"{side}_leg", (hip_x, 12, 5), [((hip_x - 3, 8, 2), (6, 5, 6))])
        add_bone(f"{side}_leg_shin", f"{side}_leg_knee", (hip_x, 8, 6), [((hip_x - 2.5, 2, 3), (5, 7, 5))])
        add_bone(
            f"{side}_leg_foot",
            f"{side}_leg_shin",
            (hip_x, 2, 5),
            [
                ((hip_x - 4, 0, -3), (8, 2.5, 12)),  # plantar pad on ground
                ((hip_x - 5, 0, -6), (2, 2, 3)),
                ((hip_x - 2, 0, -7), (2, 2, 4)),
                ((hip_x + 1, 0, -6), (2, 2, 3)),
                ((hip_x + 3, 0, -1), (2, 2, 3)),
            ],
        )

    # Tail: pivots every ~5.5u, cubes depth ~8 → ~2.5u overlap (no hollow gaps when bent)
    for name, parent, pivot, w, h, d in (
        ("tail_0", "body_2", (0, 24, 12), 8.0, 8.0, 9.0),
        ("tail_1", "tail_0", (0, 23.5, 17.5), 7.2, 7.2, 9.0),
        ("tail_2", "tail_1", (0, 23, 23), 6.4, 6.4, 8.5),
        ("tail_3", "tail_2", (0, 22.5, 28.5), 5.6, 5.6, 8.5),
        ("tail_4", "tail_3", (0, 22, 34), 4.8, 4.8, 8.0),
        ("tail_5", "tail_4", (0, 21.5, 39.5), 4.2, 4.2, 8.0),
        ("tail_6", "tail_5", (0, 21, 45), 3.6, 3.6, 7.5),
        ("tail_7", "tail_6", (0, 20.5, 50), 3.0, 3.0, 7.0),
        ("tail_8", "tail_7", (0, 20, 55), 2.4, 2.4, 6.5),
        ("tail_9", "tail_8", (0, 19.5, 59.5), 1.8, 1.8, 6.0),
    ):
        px, py, pz = pivot
        crest_h = max(2.0, h * 0.65)
        # cube centered on pivot but deeper than spacing → overlaps neighbor
        add_bone(
            name,
            parent,
            pivot,
            [
                ((px - w / 2, py - h / 2, pz - d / 2), (w, h, d)),
                ((px - 1, py + h / 2 - 0.4, pz - 1), (2, crest_h, max(1.5, d * 0.3))),
            ],
        )


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


def paint_face(im, box, *, part, face, kind):
    u, v, w, h = box
    if w <= 0 or h <= 0:
        return
    draw = ImageDraw.Draw(im)
    rng = random.Random(hash((part, face, w, h, u, v)) & 0xFFFFFFFF)

    if kind == "claw":
        draw.rectangle([u, v, u + w - 1, v + h - 1], fill=CLAW)
        return
    if kind == "eye":
        # Yellow iris + sharp black diamond pupil
        draw.rectangle([u, v, u + w - 1, v + h - 1], fill=EYE)
        for y in range(v, v + h):
            for x in range(u, u + w):
                # brighter rim / sclera blend toward center
                t = abs((x - (u + w / 2)) / max(1, w)) + abs((y - (v + h / 2)) / max(1, h))
                if t < 0.55:
                    im.putpixel((x, y), EYE_CORE)
                else:
                    im.putpixel((x, y), EYE)
        cx = u + max(0, (w - 1) // 2)
        cy = v + max(0, (h - 1) // 2)
        # Vertical sharp pupil (slit)
        for dy in range(-(h // 2), h // 2 + 1):
            yy = cy + dy
            width = 0 if abs(dy) >= h // 2 else (0 if abs(dy) > h // 3 else 1)
            for dx in range(-width, width + 1):
                put(im, cx + dx, yy, EYE_PUPIL, u, v, w, h)
        # Point tips of slit
        put(im, cx, v, EYE_PUPIL, u, v, w, h)
        put(im, cx, v + h - 1, EYE_PUPIL, u, v, w, h)
        if w > 1 and h > 1:
            draw.rectangle([u, v, u + w - 1, v + h - 1], outline=STONE_DARK)
        px = im.load()
        for y in range(v, v + h):
            for x in range(u, u + w):
                rr, gg, bb, aa = px[x, y]
                if aa > 0:
                    px[x, y] = (rr, gg, bb, 255)
        return
    if kind in ("crest", "horn"):
        draw.rectangle([u, v, u + w - 1, v + h - 1], fill=STONE_DARK)
        tip = max(1, h // 3)
        for y in range(v, v + tip):
            for x in range(u, u + w):
                im.putpixel((x, y), GOLD_HI if y == v else GOLD)
        return
    if kind == "wing":
        draw.rectangle([u, v, u + w - 1, v + h - 1], fill=MEMBRANE)
        for y in range(v, v + h):
            for x in range(u, u + w):
                if rng.random() < 0.12:
                    im.putpixel((x, y), MEMBRANE_DARK)
        step = max(3, w // 5)
        for i in range(0, w, step):
            x = u + i
            for y in range(v, v + h):
                put(im, x + ((y // 2) % 3) - 1, y, GOLD if y % 2 == 0 else GOLD_LO, u, v, w, h)
        draw.rectangle([u, v, u + w - 1, v + h - 1], outline=STONE_DARK)
        return
    if kind == "belly":
        for y in range(v, v + h):
            for x in range(u, u + w):
                im.putpixel((x, y), BELLY if rng.random() > 0.15 else BELLY_HI)
        for y in range(v + 1, v + h - 1, 2):
            for x in range(u, u + w):
                put(im, x, y, STONE_DARK, u, v, w, h)
        return

    for y in range(v, v + h):
        for x in range(u, u + w):
            n = (x * 3 + y * 7 + rng.randint(0, 2)) % 5
            col = [STONE, STONE_MID, STONE_DARK, STONE_HI, BELLY][n]
            if rng.random() < 0.05:
                col = STONE_DARK
            im.putpixel((x, y), col)
    if w > 2 and h > 2:
        draw.rectangle([u, v, u + w - 1, v + h - 1], outline=STONE_DARK)

    px = im.load()
    for y in range(v, v + h):
        for x in range(u, u + w):
            rr, gg, bb, aa = px[x, y]
            if aa > 0:
                px[x, y] = (rr, gg, bb, 255)


def classify(name: str, i: int) -> str:
    if name in ("left_eye", "right_eye"):
        return "eye"
    if name.startswith("horn_"):
        return "horn"
    if "wing" in name:
        if (name.endswith("wing") and i == 1) or (name.endswith("_mid") and i == 1) or (name.endswith("_tip") and i == 1):
            return "wing"
        if name.endswith("wing") and i == 2:
            return "claw"
        return "stone"
    if name.endswith("_foot") and i >= 1:
        return "claw"
    if name == "jaw":
        return "belly"
    if name.startswith(("body_", "neck_")) and i == 1:
        return "belly"
    if name.startswith(("body_", "neck_")) and i >= 2:
        return "crest"
    if name.startswith("tail_") and i == 1:
        return "crest"
    return "stone"


def build_geo():
    bones = [{"name": "root", "pivot": [0, 0, 0]}]
    for name in BONE_ORDER:
        meta = PARTS[name]
        bones.append(
            {
                "name": name,
                "parent": meta["parent"],
                "pivot": [round(c, 2) for c in meta["pivot"]],
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
                    "visible_bounds_width": 16,
                    "visible_bounds_height": 8,
                    "visible_bounds_offset": [0, 2.5, 0],
                },
                "bones": bones,
            }
        ],
    }


def build_atlas():
    im = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))
    for name, meta in PARTS.items():
        for i, (_o, size, uv) in enumerate(meta["cubes"]):
            w, h, d = [max(1, int(round(c))) for c in size]
            faces = box_faces(uv[0], uv[1], w, h, d)
            kind = classify(name, i)
            for face, box in faces.items():
                paint_face(im, box, part=name, face=face, kind=kind)
    return im


def rot_kf(items):
    return {str(round(t, 3)): {"vector": [round(x, 2), round(y, 2), round(z, 2)]} for t, x, y, z in items}


def build_animations():
    neck_idle = {}
    for i in range(6):
        a = -10 + (i % 3) * 6
        neck_idle[f"neck_{i}"] = {"rotation": rot_kf([(0, a, 0, 0), (1.0, a + 2, 1, 0), (2.0, a, 0, 0)])}

    tail_idle = {}
    for i in range(10):
        amp = max(1.5, 4 - i * 0.25)
        tail_idle[f"tail_{i}"] = {"rotation": rot_kf([(0, 1, amp, 0), (1.0, 1, -amp, 0), (2.0, 1, amp, 0)])}

    bones_idle = {
        "body_1": {"rotation": rot_kf([(0, 4, 0, 0), (1.0, 5, 0, 0), (2.0, 4, 0, 0)])},
        "jaw": {"rotation": rot_kf([(0, 0, 0, 0), (1.0, 2, 0, 0), (2.0, 0, 0, 0)])},
        "head": {"rotation": rot_kf([(0, 4, 0, 0), (1.0, 2, 0, 0), (2.0, 4, 0, 0)])},
        "left_wing": {"rotation": rot_kf([(0, 5, 25, 18), (1.0, 4, 22, 14), (2.0, 5, 25, 18)])},
        "left_wing_mid": {"rotation": rot_kf([(0, 0, 8, -90), (2.0, 0, 8, -90)])},
        "left_wing_tip": {"rotation": rot_kf([(0, 0, 0, -18), (2.0, 0, 0, -18)])},
        "right_wing": {"rotation": rot_kf([(0, 5, -25, -18), (1.0, 4, -22, -14), (2.0, 5, -25, -18)])},
        "right_wing_mid": {"rotation": rot_kf([(0, 0, -8, 90), (2.0, 0, -8, 90)])},
        "right_wing_tip": {"rotation": rot_kf([(0, 0, 0, 18), (2.0, 0, 0, 18)])},
        # Near bind-pose so foot pads stay planted (no hover)
        "left_leg": {"rotation": rot_kf([(0, 2, 0, -2), (1.0, 0, 0, -2), (2.0, 2, 0, -2)])},
        "left_leg_knee": {"rotation": rot_kf([(0, 4, 0, 0), (1.0, 6, 0, 0), (2.0, 4, 0, 0)])},
        "left_leg_shin": {"rotation": rot_kf([(0, 2, 0, 0), (1.0, 3, 0, 0), (2.0, 2, 0, 0)])},
        "left_leg_foot": {"rotation": rot_kf([(0, -4, 0, 0), (1.0, -2, 0, 0), (2.0, -4, 0, 0)])},
        "right_leg": {"rotation": rot_kf([(0, 2, 0, 2), (1.0, 0, 0, 2), (2.0, 2, 0, 2)])},
        "right_leg_knee": {"rotation": rot_kf([(0, 4, 0, 0), (1.0, 6, 0, 0), (2.0, 4, 0, 0)])},
        "right_leg_shin": {"rotation": rot_kf([(0, 2, 0, 0), (1.0, 3, 0, 0), (2.0, 2, 0, 0)])},
        "right_leg_foot": {"rotation": rot_kf([(0, -4, 0, 0), (1.0, -2, 0, 0), (2.0, -4, 0, 0)])},
    }
    bones_idle.update(neck_idle)
    bones_idle.update(tail_idle)

    return {
        "format_version": "1.8.0",
        "animations": {
            "animation.essence_wyvern.idle": {"loop": True, "animation_length": 2.0, "bones": bones_idle},
            "animation.essence_wyvern.walk": {
                "loop": True,
                "animation_length": 1.0,
                "bones": {
                    "left_leg": {"rotation": rot_kf([(0, 30, 0, -6), (0.25, 5, 0, -2), (0.5, -26, 0, 6), (0.75, 4, 0, 0), (1.0, 30, 0, -6)])},
                    "left_leg_knee": {"rotation": rot_kf([(0, 10, 0, 0), (0.25, 28, 0, 0), (0.5, 6, 0, 0), (0.75, 22, 0, 0), (1.0, 10, 0, 0)])},
                    "left_leg_shin": {"rotation": rot_kf([(0, 12, 0, 0), (0.25, 32, 0, 0), (0.5, 8, 0, 0), (0.75, 24, 0, 0), (1.0, 12, 0, 0)])},
                    "left_leg_foot": {"rotation": rot_kf([(0, -8, 0, 0), (0.25, 24, 0, 0), (0.5, -12, 0, 0), (0.75, 14, 0, 0), (1.0, -8, 0, 0)])},
                    "right_leg": {"rotation": rot_kf([(0, -26, 0, 6), (0.25, 4, 0, 0), (0.5, 30, 0, -6), (0.75, 5, 0, -2), (1.0, -26, 0, 6)])},
                    "right_leg_knee": {"rotation": rot_kf([(0, 6, 0, 0), (0.25, 22, 0, 0), (0.5, 10, 0, 0), (0.75, 28, 0, 0), (1.0, 6, 0, 0)])},
                    "right_leg_shin": {"rotation": rot_kf([(0, 8, 0, 0), (0.25, 24, 0, 0), (0.5, 12, 0, 0), (0.75, 32, 0, 0), (1.0, 8, 0, 0)])},
                    "right_leg_foot": {"rotation": rot_kf([(0, -12, 0, 0), (0.25, 14, 0, 0), (0.5, -8, 0, 0), (0.75, 24, 0, 0), (1.0, -12, 0, 0)])},
                    "body_1": {
                        "rotation": rot_kf([(0, 6, 0, 2), (0.5, 6, 0, -2), (1.0, 6, 0, 2)]),
                        "position": {
                            "0": {"vector": [0, 0, 0]},
                            "0.25": {"vector": [0, -0.5, 0]},
                            "0.5": {"vector": [0, 0, 0]},
                            "0.75": {"vector": [0, -0.5, 0]},
                            "1": {"vector": [0, 0, 0]},
                        },
                    },
                    "neck_0": {"rotation": rot_kf([(0, -6, 0, 0), (0.5, -4, 2, 0), (1.0, -6, 0, 0)])},
                    "tail_0": {"rotation": rot_kf([(0, 2, 6, 0), (0.5, 2, -6, 0), (1.0, 2, 6, 0)])},
                    "left_wing": {"rotation": rot_kf([(0, 10, 32, 8), (1.0, 10, 32, 8)])},
                    "left_wing_mid": {"rotation": rot_kf([(0, 4, 4, -70), (1.0, 4, 4, -70)])},
                    "right_wing": {"rotation": rot_kf([(0, 10, -32, -8), (1.0, 10, -32, -8)])},
                    "right_wing_mid": {"rotation": rot_kf([(0, 4, -4, 70), (1.0, 4, -4, 70)])},
                },
            },
            "animation.essence_wyvern.sit": {
                "loop": True,
                "animation_length": 4.0,
                "bones": {
                    # Legs parented to root — crouch body only; feet stay planted
                    "body_1": {
                        "rotation": rot_kf([(0, 14, 0, 0), (4.0, 14, 0, 0)]),
                        "position": {"0": {"vector": [0, -2, 1]}, "4": {"vector": [0, -2, 1]}},
                    },
                    "neck_0": {"rotation": rot_kf([(0, -4, 0, 0), (4.0, -4, 0, 0)])},
                    "neck_3": {"rotation": rot_kf([(0, 8, 0, 0), (4.0, 8, 0, 0)])},
                    "left_leg": {"rotation": rot_kf([(0, -28, 5, -5), (4.0, -28, 5, -5)])},
                    "left_leg_knee": {"rotation": rot_kf([(0, 48, 0, 0), (4.0, 48, 0, 0)])},
                    "left_leg_shin": {"rotation": rot_kf([(0, -12, 0, 0), (4.0, -12, 0, 0)])},
                    "left_leg_foot": {"rotation": rot_kf([(0, -2, 0, 0), (4.0, -2, 0, 0)])},
                    "right_leg": {"rotation": rot_kf([(0, -28, -5, 5), (4.0, -28, -5, 5)])},
                    "right_leg_knee": {"rotation": rot_kf([(0, 48, 0, 0), (4.0, 48, 0, 0)])},
                    "right_leg_shin": {"rotation": rot_kf([(0, -12, 0, 0), (4.0, -12, 0, 0)])},
                    "right_leg_foot": {"rotation": rot_kf([(0, -2, 0, 0), (4.0, -2, 0, 0)])},
                    "left_wing": {"rotation": rot_kf([(0, 50, 12, 50), (4.0, 50, 12, 50)])},
                    "left_wing_mid": {"rotation": rot_kf([(0, 20, 0, 20), (4.0, 20, 0, 20)])},
                    "right_wing": {"rotation": rot_kf([(0, 50, -12, -50), (4.0, 50, -12, -50)])},
                    "right_wing_mid": {"rotation": rot_kf([(0, 20, 0, -20), (4.0, 20, 0, -20)])},
                    "tail_0": {"rotation": rot_kf([(0, 10, 0, 0), (4.0, 10, 0, 0)])},
                },
            },
            "animation.essence_wyvern.fly": {
                "loop": True,
                "animation_length": 0.5,
                "bones": {
                    "left_wing": {"rotation": rot_kf([(0, 0, -18, -50), (0.25, 0, -30, 40), (0.5, 0, -18, -50)])},
                    "left_wing_mid": {"rotation": rot_kf([(0, 0, 4, -20), (0.25, 0, 0, 18), (0.5, 0, 4, -20)])},
                    "left_wing_tip": {"rotation": rot_kf([(0, 0, 0, -12), (0.25, 0, 0, 20), (0.5, 0, 0, -12)])},
                    "right_wing": {"rotation": rot_kf([(0, 0, 18, 50), (0.25, 0, 30, -40), (0.5, 0, 18, 50)])},
                    "right_wing_mid": {"rotation": rot_kf([(0, 0, -4, 20), (0.25, 0, 0, -18), (0.5, 0, -4, 20)])},
                    "right_wing_tip": {"rotation": rot_kf([(0, 0, 0, 12), (0.25, 0, 0, -20), (0.5, 0, 0, 12)])},
                    "body_1": {"rotation": rot_kf([(0, 14, 0, 0), (0.25, 10, 0, 0), (0.5, 14, 0, 0)])},
                    "left_leg": {"rotation": rot_kf([(0, 40, 0, 0), (0.5, 40, 0, 0)])},
                    "left_leg_knee": {"rotation": rot_kf([(0, 25, 0, 0), (0.5, 25, 0, 0)])},
                    "left_leg_shin": {"rotation": rot_kf([(0, 30, 0, 0), (0.5, 30, 0, 0)])},
                    "right_leg": {"rotation": rot_kf([(0, 40, 0, 0), (0.5, 40, 0, 0)])},
                    "right_leg_knee": {"rotation": rot_kf([(0, 25, 0, 0), (0.5, 25, 0, 0)])},
                    "right_leg_shin": {"rotation": rot_kf([(0, 30, 0, 0), (0.5, 30, 0, 0)])},
                    "tail_0": {"rotation": rot_kf([(0, -10, 0, 0), (0.25, -6, 8, 0), (0.5, -10, 0, 0)])},
                },
            },
            "animation.essence_wyvern.attack": {
                "animation_length": 1.0,
                "bones": {
                    "neck_0": {"rotation": rot_kf([(0, -8, 0, 0), (0.3, -16, 0, 0), (0.55, 18, 0, 0), (1, -8, 0, 0)])},
                    "neck_3": {"rotation": rot_kf([(0, 0, 0, 0), (0.3, -12, 0, 0), (0.55, 16, 0, 0), (1, 0, 0, 0)])},
                    "head": {"rotation": rot_kf([(0, 0, 0, 0), (0.3, -10, 0, 0), (0.55, 14, 0, 0), (1, 0, 0, 0)])},
                    "jaw": {"rotation": rot_kf([(0, 0, 0, 0), (0.4, 18, 0, 0), (0.7, 0, 0, 0), (1, 0, 0, 0)])},
                },
            },
            "animation.essence_wyvern.breath": {
                "animation_length": 1.6,
                "bones": {
                    "neck_0": {"rotation": rot_kf([(0, -12, 0, 0), (0.6, -20, 0, 0), (1.0, 6, 0, 0), (1.6, -8, 0, 0)])},
                    "head": {"rotation": rot_kf([(0, -6, 0, 0), (0.8, -12, 0, 0), (1.1, 4, 0, 0), (1.6, 0, 0, 0)])},
                    "jaw": {"rotation": rot_kf([(0, 0, 0, 0), (0.7, 22, 0, 0), (1.3, 4, 0, 0), (1.6, 0, 0, 0)])},
                    "body_1": {"rotation": rot_kf([(0, 4, 0, 0), (0.8, -4, 0, 0), (1.6, 6, 0, 0)])},
                    "left_wing": {"rotation": rot_kf([(0, 0, -12, -28), (0.8, 0, -20, -40), (1.6, 5, 22, 16)])},
                    "right_wing": {"rotation": rot_kf([(0, 0, 12, 28), (0.8, 0, 20, 40), (1.6, 5, -22, -16)])},
                },
            },
            "animation.essence_wyvern.hurt": {
                "animation_length": 0.4,
                "bones": {"body_1": {"rotation": rot_kf([(0, 6, 0, 0), (0.1, -4, 0, 8), (0.4, 6, 0, 0)])}},
            },
            "animation.essence_wyvern.death": {
                "animation_length": 1.8,
                "bones": {
                    "body_1": {"rotation": rot_kf([(0, 6, 0, 0), (0.8, 40, 0, 0), (1.8, 85, 0, 10)])},
                    "left_wing": {"rotation": rot_kf([(0, 5, 25, 18), (1.8, -8, 35, 65)])},
                    "right_wing": {"rotation": rot_kf([(0, 5, -25, -18), (1.8, -8, -35, -65)])},
                    "neck_0": {"rotation": rot_kf([(0, -8, 0, 0), (1.8, 30, 20, 0)])},
                    "tail_0": {"rotation": rot_kf([(0, 8, 0, 0), (1.8, 18, -25, 0)])},
                },
            },
        },
    }


def write_json(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2), encoding="utf-8")


def main():
    build_parts()
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
    atlas.resize((256, 256), Image.NEAREST).save(ART / "uv_guide_preview.png")
    print("Rebuilt from contour segment method")
    print(" bones:", len(BONE_ORDER))
    print(" ", BONE_ORDER)
    print(" atlas:", ATLAS, "->", tex)


if __name__ == "__main__":
    main()
