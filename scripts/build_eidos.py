#!/usr/bin/env python3
"""
Build Eidos from SEGMENT_LOCK (beige field body + amber Φ rings/trail).

  python scripts/build_eidos.py
"""

from __future__ import annotations

import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/effecoria"
ART = ROOT / "art/eidos"
ATLAS = 128

# Stronger contrast — pale wash read as "bleached" in-game
BODY = (148, 120, 88, 255)
BODY_MID = (168, 136, 96, 255)
BODY_DARK = (88, 68, 48, 255)
BODY_HI = (188, 156, 112, 255)
GLOW = (240, 180, 48, 255)
GLOW_HI = (255, 235, 120, 255)
GLOW_LO = (180, 110, 28, 255)
EYE_CORE = (255, 250, 200, 255)
EYE_IRIS = (255, 200, 40, 255)
PUPIL = (16, 12, 8, 255)
DARK = (32, 24, 16, 255)

PARTS: dict = {}
BONE_ORDER: list[str] = []
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
        raise SystemExit(f"Atlas overflow ({_UV[0]},{_UV[1]}) need {need_w}x{need_h}")
    u, v = _UV[0], _UV[1]
    _UV[0] += need_w + 1
    _UV[2] = max(_UV[2], need_h)
    return u, v


def add_bone(name: str, parent: str, pivot: tuple[float, float, float], cubes: list):
    PARTS[name] = {"parent": parent, "pivot": pivot, "cubes": [(o, s, _alloc_uv(*s)) for o, s in cubes]}
    BONE_ORDER.append(name)


def build_parts():
    PARTS.clear()
    BONE_ORDER.clear()
    _UV[:] = [0, 0, 0]

    # Floating humanoid; entity height ~1.4 blocks ≈ 22u. Pivot mid-torso ~14.
    add_bone(
        "torso",
        "root",
        (0, 14, 0),
        [
            ((-2.5, 10, -1.5), (5, 8, 3)),
            ((-2.0, 10, -1.2), (4, 2, 2.4)),  # lower taper start
        ],
    )
    add_bone(
        "head",
        "torso",
        (0, 20, 0),
        [
            ((-2.2, 17.8, -2.2), (4.4, 4.4, 4.4)),
        ],
    )
    # Large protruding eye — readable from front/sides
    add_bone(
        "eye",
        "head",
        (0, 19.8, -2.2),
        [
            ((-1.4, 18.6, -3.6), (2.8, 2.8, 1.8)),
        ],
    )
    add_bone("arm_l", "torso", (2.5, 16, 0), [((2.2, 11, -0.7), (1.4, 5.5, 1.4))])
    add_bone("arm_r", "torso", (-2.5, 16, 0), [((-3.6, 11, -0.7), (1.4, 5.5, 1.4))])
    add_bone("phi_chest", "torso", (0, 15, -1.6), [((-1.4, 13.6, -2.2), (2.8, 2.8, 0.8))])

    add_bone("trail_0", "torso", (0, 10, 0), [((-1.5, 7, -1.0), (3, 3.5, 2))])
    add_bone("trail_1", "trail_0", (0, 7, 0), [((-1.0, 4, -0.7), (2, 3.2, 1.4))])
    add_bone("trail_2", "trail_1", (0, 4, 0), [((-0.6, 1, -0.5), (1.2, 3.0, 1.0))])

    # Orbital rings as segment cubes on spinning bones
    ring_a_cubes = []
    ring_b_cubes = []
    for i in range(6):
        ang = i * math.tau / 6
        ra, rb = 5.5, 4.0
        xa, za = ra * math.cos(ang), ra * math.sin(ang)
        xb, zb = rb * math.cos(ang + 0.2), rb * math.sin(ang + 0.2)
        ring_a_cubes.append(((xa - 0.7, 14.3, za - 0.7), (1.4, 1.2, 1.4)))
        ring_b_cubes.append(((xb - 0.55, 16.0, zb - 0.55), (1.1, 1.0, 1.1)))
    add_bone("ring_a", "torso", (0, 14.5, 0), ring_a_cubes)
    add_bone("ring_b", "torso", (0, 16.2, 0), ring_b_cubes)


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

    if kind == "glow":
        for y in range(v, v + h):
            for x in range(u, u + w):
                t = (x + y) % 3
                im.putpixel((x, y), [GLOW, GLOW_HI, GLOW_LO][t])
        if w > 2 and h > 2:
            draw.rectangle([u, v, u + w - 1, v + h - 1], outline=GLOW_LO)
        return
    if kind == "phi":
        draw.rectangle([u, v, u + w - 1, v + h - 1], fill=BODY_DARK)
        cx = u + w // 2
        for y in range(v + 1, v + h - 1):
            put(im, cx, y, GLOW_HI, u, v, w, h)
            put(im, cx - 1, y, GLOW, u, v, w, h)
        oy0, oy1 = v + max(1, h // 4), v + min(h - 2, 3 * h // 4)
        ox0, ox1 = u + max(1, w // 4), u + min(w - 2, 3 * w // 4)
        for y in range(oy0, oy1 + 1):
            for x in range(ox0, ox1 + 1):
                if x in (ox0, ox1) or y in (oy0, oy1):
                    put(im, x, y, GLOW, u, v, w, h)
        return
    if kind == "eye":
        # Solid amber iris + sharp black pupil on every outward face
        draw.rectangle([u, v, u + w - 1, v + h - 1], fill=EYE_IRIS)
        for y in range(v, v + h):
            for x in range(u, u + w):
                dx = abs(x - (u + w / 2)) / max(1, w)
                dy = abs(y - (v + h / 2)) / max(1, h)
                if dx + dy < 0.55:
                    im.putpixel((x, y), GLOW_HI if dx + dy < 0.25 else EYE_IRIS)
                else:
                    im.putpixel((x, y), GLOW_LO)
        cx = u + max(0, (w - 1) // 2)
        cy = v + max(0, (h - 1) // 2)
        # Vertical slit pupil
        for dy in range(-(max(1, h // 2)), max(1, h // 2) + 1):
            yy = cy + dy
            width = 0 if abs(dy) >= h // 2 else (1 if abs(dy) < h // 3 else 0)
            for dx in range(-width, width + 1):
                put(im, cx + dx, yy, PUPIL, u, v, w, h)
        put(im, cx, cy, EYE_CORE, u, v, w, h)
        if w > 1 and h > 1:
            draw.rectangle([u, v, u + w - 1, v + h - 1], outline=DARK)
        px = im.load()
        for y in range(v, v + h):
            for x in range(u, u + w):
                rr, gg, bb, aa = px[x, y]
                if aa > 0:
                    px[x, y] = (rr, gg, bb, 255)
        return

    for y in range(v, v + h):
        for x in range(u, u + w):
            n = (x * 3 + y * 5 + rng.randint(0, 2)) % 4
            im.putpixel((x, y), [BODY, BODY_MID, BODY_DARK, BODY_HI][n])
    if w > 2 and h > 2:
        draw.rectangle([u, v, u + w - 1, v + h - 1], outline=BODY_DARK)
    # vertical Φ energy line on torso front
    if kind == "body_line" and face == "front" and w >= 3:
        cx = u + w // 2
        for y in range(v, v + h):
            put(im, cx, y, GLOW, u, v, w, h)
    px = im.load()
    for y in range(v, v + h):
        for x in range(u, u + w):
            rr, gg, bb, aa = px[x, y]
            if aa > 0:
                px[x, y] = (rr, gg, bb, 255)


def classify(name: str, i: int) -> str:
    if name == "eye":
        return "eye"
    if name.startswith("ring_"):
        return "glow"
    if name.startswith("trail_"):
        return "glow"
    if name == "phi_chest":
        return "phi"
    if name == "head":
        return "body"
    if name == "torso" and i == 0:
        return "body_line"
    return "body"


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
                    {"origin": [round(c, 2) for c in o], "size": [round(c, 2) for c in s], "uv": list(uv)}
                    for o, s, uv in meta["cubes"]
                ],
            }
        )
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": "geometry.eidos",
                    "texture_width": ATLAS,
                    "texture_height": ATLAS,
                    "visible_bounds_width": 4,
                    "visible_bounds_height": 3,
                    "visible_bounds_offset": [0, 1.2, 0],
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
            kind = classify(name, i)
            for face, box in box_faces(uv[0], uv[1], w, h, d).items():
                paint_face(im, box, part=name, face=face, kind=kind)
    return im


def rot_kf(items):
    return {str(round(t, 3)): {"vector": [round(x, 2), round(y, 2), round(z, 2)]} for t, x, y, z in items}


def pos_kf(items):
    return {str(round(t, 3)): {"vector": [round(x, 2), round(y, 2), round(z, 2)]} for t, x, y, z in items}


def build_animations():
    idle = {
        "torso": {
            "position": pos_kf([(0, 0, 0, 0), (1.0, 0, 0.8, 0), (2.0, 0, 0, 0)]),
            "rotation": rot_kf([(0, 0, 0, 0), (1.0, 2, 4, 0), (2.0, 0, 0, 0)]),
        },
        "head": {"rotation": rot_kf([(0, 0, 0, 0), (1.0, 4, 6, 0), (2.0, 0, 0, 0)])},
        "eye": {"scale": {"0": {"vector": [1, 1, 1]}, "1": {"vector": [1.12, 1.12, 1.12]}, "2": {"vector": [1, 1, 1]}}},
        "arm_l": {"rotation": rot_kf([(0, 8, 0, -6), (1.0, -4, 0, 6), (2.0, 8, 0, -6)])},
        "arm_r": {"rotation": rot_kf([(0, 8, 0, 6), (1.0, -4, 0, -6), (2.0, 8, 0, 6)])},
        "ring_a": {"rotation": rot_kf([(0, 0, 0, 0), (2.0, 0, 360, 0)])},
        "ring_b": {"rotation": rot_kf([(0, 0, 0, 0), (2.0, 0, -360, 0)])},
        "trail_0": {"rotation": rot_kf([(0, 0, 4, 0), (1.0, 0, -4, 0), (2.0, 0, 4, 0)])},
        "trail_1": {"rotation": rot_kf([(0, 0, -6, 0), (1.0, 0, 6, 0), (2.0, 0, -6, 0)])},
        "trail_2": {"rotation": rot_kf([(0, 0, 8, 0), (1.0, 0, -8, 0), (2.0, 0, 8, 0)])},
        "phi_chest": {"scale": {"0": {"vector": [1, 1, 1]}, "1": {"vector": [1.15, 1.15, 1.15]}, "2": {"vector": [1, 1, 1]}}},
    }

    gift = {
        "torso": {"position": pos_kf([(0, 0, 0, 0), (0.4, 0, 1.5, 0), (1.0, 0, 0, 0)])},
        "arm_l": {"rotation": rot_kf([(0, 8, 0, -6), (0.3, -40, 0, -35), (1.0, 8, 0, -6)])},
        "arm_r": {"rotation": rot_kf([(0, 8, 0, 6), (0.3, -40, 0, 35), (1.0, 8, 0, 6)])},
        "ring_a": {
            "rotation": rot_kf([(0, 0, 0, 0), (1.0, 0, 180, 0)]),
            "scale": {"0": {"vector": [1, 1, 1]}, "0.4": {"vector": [1.35, 1.35, 1.35]}, "1": {"vector": [1, 1, 1]}},
        },
        "ring_b": {
            "rotation": rot_kf([(0, 0, 0, 0), (1.0, 0, -180, 0)]),
            "scale": {"0": {"vector": [1, 1, 1]}, "0.4": {"vector": [1.4, 1.4, 1.4]}, "1": {"vector": [1, 1, 1]}},
        },
        "head": {"rotation": rot_kf([(0, 0, 0, 0), (0.4, -10, 0, 0), (1.0, 0, 0, 0)])},
        "trail_0": {"scale": {"0": {"vector": [1, 1, 1]}, "0.4": {"vector": [1.2, 1.3, 1.2]}, "1": {"vector": [1, 1, 1]}}},
        "phi_chest": {"scale": {"0": {"vector": [1, 1, 1]}, "0.35": {"vector": [1.5, 1.5, 1.5]}, "1": {"vector": [1, 1, 1]}}},
    }

    return {
        "format_version": "1.8.0",
        "animations": {
            "animation.eidos.idle": {"loop": True, "animation_length": 2.0, "bones": idle},
            "animation.eidos.gift": {"animation_length": 1.0, "bones": gift},
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
    for p in (ASSETS / "geo/eidos.geo.json", ART / "eidos.geo.json"):
        write_json(p, geo)
    for p in (ASSETS / "animations/eidos.animation.json", ART / "eidos.animation.json"):
        write_json(p, anims)
    tex = ASSETS / "textures/entity/eidos.png"
    tex.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(tex)
    ART.mkdir(parents=True, exist_ok=True)
    atlas.save(ART / "eidos.png")
    atlas.resize((ATLAS * 2, ATLAS * 2), Image.NEAREST).save(ART / "uv_guide_preview.png")
    print("Rebuilt eidos — beige field body + amber rings/trail")
    print(" bones:", BONE_ORDER)
    print(" ->", tex)


if __name__ == "__main__":
    main()
