#!/usr/bin/env python3
"""
Build Crystal Crab from SEGMENT_LOCK (stone-beige shell + amber crystals).

  python scripts/build_crystal_crab.py
"""

from __future__ import annotations

import json
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/effecoria"
ART = ROOT / "art/crystal_crab"
ATLAS = 128

SHELL = (168, 136, 88, 255)
SHELL_MID = (176, 144, 96, 255)
SHELL_DARK = (88, 72, 48, 255)
SHELL_HI = (192, 160, 112, 255)
JOINT = (56, 48, 32, 255)
BELLY = (120, 96, 64, 255)
CRYSTAL = (208, 168, 112, 255)
CRYSTAL_HI = (248, 216, 104, 255)
CRYSTAL_LO = (168, 120, 64, 255)
EYE = (20, 16, 12, 255)

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

    # Feet at y=0. Body sits ~3–8 high. Claws toward -Z (forward).
    add_bone(
        "body",
        "root",
        (0, 5, 0),
        [
            ((-6, 3, -5), (12, 5, 10)),  # carapace
            ((-5, 3, -4), (10, 2, 8)),  # belly
            ((-5.5, 7.5, -3.5), (11, 2, 7)),  # dome
        ],
    )
    add_bone("crystal_main", "body", (0, 9, 0), [((-1.5, 8, -1.2), (3, 5, 2.5))])
    for i, (ox, oz) in enumerate(((-3.5, -1), (-2.5, 1.5), (-4, 0.5))):
        add_bone(f"crystal_l{i}", "body", (ox, 8.5, oz), [((ox - 0.8, 7.5, oz - 0.7), (1.6, 3.2 + i * 0.3, 1.4))])
    for i, (ox, oz) in enumerate(((3.5, -1), (2.5, 1.5), (4, 0.5))):
        add_bone(f"crystal_r{i}", "body", (ox, 8.5, oz), [((ox - 0.8, 7.5, oz - 0.7), (1.6, 3.2 + i * 0.3, 1.4))])

    add_bone("eye_l", "body", (-2.2, 7, -4.5), [((-2.5, 6.5, -5.0), (0.8, 1.8, 0.8)), ((-2.7, 8.0, -5.2), (1.2, 1.2, 1.2))])
    add_bone("eye_r", "body", (2.2, 7, -4.5), [((1.7, 6.5, -5.0), (0.8, 1.8, 0.8)), ((1.5, 8.0, -5.2), (1.2, 1.2, 1.2))])

    # Claws: arm + tip (pincer)
    add_bone(
        "claw_l",
        "body",
        (5.5, 4.5, -4),
        [
            ((4.5, 3.5, -7), (3.5, 2.8, 4.5)),
            ((5.5, 5.5, -6.5), (1.5, 2.0, 1.5)),  # claw crystal
        ],
    )
    add_bone("claw_l_tip", "claw_l", (6, 3.8, -7), [((5.0, 2.8, -10.5), (2.8, 2.2, 4.0))])
    add_bone(
        "claw_r",
        "body",
        (-5.5, 4.5, -4),
        [
            ((-8.0, 3.5, -7), (3.5, 2.8, 4.5)),
            ((-7.0, 5.5, -6.5), (1.5, 2.0, 1.5)),
        ],
    )
    add_bone("claw_r_tip", "claw_r", (-6, 3.8, -7), [((-7.8, 2.8, -10.5), (2.8, 2.2, 4.0))])

    # 3 legs per side: outer / mid / inner. Upper + shin to y=0.
    # Left (+X)
    for i, z in enumerate((-3.5, 0.0, 3.0)):
        x = 5.5 + i * 0.2
        add_bone(f"leg_l{i}", "body", (x, 3.5, z), [((x - 0.7, 1.5, z - 0.7), (1.8, 2.5, 1.8))])
        add_bone(f"leg_l{i}_shin", f"leg_l{i}", (x + 1.2, 1.5, z), [((x + 0.4, 0.0, z - 0.6), (1.5, 2.0, 1.5))])
    # Right (-X)
    for i, z in enumerate((-3.5, 0.0, 3.0)):
        x = -5.5 - i * 0.2
        add_bone(f"leg_r{i}", "body", (x, 3.5, z), [((x - 1.1, 1.5, z - 0.7), (1.8, 2.5, 1.8))])
        add_bone(f"leg_r{i}_shin", f"leg_r{i}", (x - 1.2, 1.5, z), [((x - 1.9, 0.0, z - 0.6), (1.5, 2.0, 1.5))])


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

    if kind == "eye":
        draw.rectangle([u, v, u + w - 1, v + h - 1], fill=JOINT)
        if face in ("front", "top", "left", "right") and w >= 2 and h >= 2:
            cx, cy = u + w // 2, v + h // 2
            put(im, cx, cy, EYE, u, v, w, h)
            put(im, cx - 1, cy, EYE, u, v, w, h)
        return
    if kind == "crystal":
        for y in range(v, v + h):
            for x in range(u, u + w):
                t = (x + y) % 3
                im.putpixel((x, y), [CRYSTAL, CRYSTAL_HI, CRYSTAL_LO][t])
        if w > 2 and h > 2:
            draw.rectangle([u, v, u + w - 1, v + h - 1], outline=CRYSTAL_LO)
        # tip highlight
        for x in range(u, u + w):
            put(im, x, v, CRYSTAL_HI, u, v, w, h)
        return
    if kind == "belly":
        for y in range(v, v + h):
            for x in range(u, u + w):
                im.putpixel((x, y), BELLY if rng.random() > 0.2 else SHELL_DARK)
        return
    if kind == "joint":
        draw.rectangle([u, v, u + w - 1, v + h - 1], fill=JOINT)
        return

    for y in range(v, v + h):
        for x in range(u, u + w):
            n = (x * 2 + y * 5 + rng.randint(0, 2)) % 4
            im.putpixel((x, y), [SHELL, SHELL_MID, SHELL_DARK, SHELL_HI][n])
    if w > 2 and h > 2:
        draw.rectangle([u, v, u + w - 1, v + h - 1], outline=SHELL_DARK)
    # plate seams
    if kind == "shell" and h > 4:
        for y in range(v + 2, v + h - 1, 3):
            for x in range(u, u + w):
                put(im, x, y, SHELL_DARK, u, v, w, h)
    px = im.load()
    for y in range(v, v + h):
        for x in range(u, u + w):
            rr, gg, bb, aa = px[x, y]
            if aa > 0:
                px[x, y] = (rr, gg, bb, 255)


def classify(name: str, i: int) -> str:
    if name.startswith("crystal"):
        return "crystal"
    if name.startswith("eye_") and i == 1:
        return "eye"
    if name.startswith("eye_"):
        return "joint"
    if name.startswith("claw_") and i == 1 and not name.endswith("_tip"):
        return "crystal"
    if name == "body" and i == 1:
        return "belly"
    if "shin" in name:
        return "joint"
    if name.startswith("leg_"):
        return "shell"
    return "shell"


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
                    "identifier": "geometry.crystal_crab",
                    "texture_width": ATLAS,
                    "texture_height": ATLAS,
                    "visible_bounds_width": 4,
                    "visible_bounds_height": 2.5,
                    "visible_bounds_offset": [0, 1.0, 0],
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


def build_animations():
    idle = {
        "body": {"rotation": rot_kf([(0, 0, 0, 0), (1.0, 1, 0, 0), (2.0, 0, 0, 0)])},
        "claw_l": {"rotation": rot_kf([(0, 0, 0, -6), (1.0, 0, 0, 4), (2.0, 0, 0, -6)])},
        "claw_r": {"rotation": rot_kf([(0, 0, 0, 6), (1.0, 0, 0, -4), (2.0, 0, 0, 6)])},
        "claw_l_tip": {"rotation": rot_kf([(0, 8, 0, 0), (1.0, 18, 0, 0), (2.0, 8, 0, 0)])},
        "claw_r_tip": {"rotation": rot_kf([(0, 8, 0, 0), (1.0, 18, 0, 0), (2.0, 8, 0, 0)])},
        "eye_l": {"rotation": rot_kf([(0, 0, -4, 0), (1.0, 0, 4, 0), (2.0, 0, -4, 0)])},
        "eye_r": {"rotation": rot_kf([(0, 0, 4, 0), (1.0, 0, -4, 0), (2.0, 0, 4, 0)])},
        "crystal_main": {"rotation": rot_kf([(0, 0, 0, 0), (1.0, 0, 3, 0), (2.0, 0, 0, 0)])},
    }

    # Alternating tripod-ish walk
    walk = {"body": {"rotation": rot_kf([(0, 2, 0, 2), (0.5, 2, 0, -2), (1.0, 2, 0, 2)])}}
    for i in range(3):
        # left up when right down
        walk[f"leg_l{i}"] = {
            "rotation": rot_kf(
                [
                    (0, 20 if i % 2 == 0 else -15, 0, 12),
                    (0.5, -15 if i % 2 == 0 else 20, 0, 8),
                    (1.0, 20 if i % 2 == 0 else -15, 0, 12),
                ]
            )
        }
        walk[f"leg_l{i}_shin"] = {
            "rotation": rot_kf([(0, 10, 0, 0), (0.5, 25, 0, 0), (1.0, 10, 0, 0)])
        }
        walk[f"leg_r{i}"] = {
            "rotation": rot_kf(
                [
                    (0, -15 if i % 2 == 0 else 20, 0, -12),
                    (0.5, 20 if i % 2 == 0 else -15, 0, -8),
                    (1.0, -15 if i % 2 == 0 else 20, 0, -12),
                ]
            )
        }
        walk[f"leg_r{i}_shin"] = {
            "rotation": rot_kf([(0, 10, 0, 0), (0.5, 25, 0, 0), (1.0, 10, 0, 0)])
        }
    walk["claw_l"] = {"rotation": rot_kf([(0, 5, 0, -10), (1.0, 5, 0, -10)])}
    walk["claw_r"] = {"rotation": rot_kf([(0, 5, 0, 10), (1.0, 5, 0, 10)])}

    attack = {
        "claw_l": {"rotation": rot_kf([(0, 0, 0, -8), (0.15, -25, -20, -30), (0.35, 15, 10, 5), (0.55, 0, 0, -8)])},
        "claw_r": {"rotation": rot_kf([(0, 0, 0, 8), (0.15, -25, 20, 30), (0.35, 15, -10, -5), (0.55, 0, 0, 8)])},
        "claw_l_tip": {"rotation": rot_kf([(0, 10, 0, 0), (0.2, 35, 0, 0), (0.35, 5, 0, 0), (0.55, 10, 0, 0)])},
        "claw_r_tip": {"rotation": rot_kf([(0, 10, 0, 0), (0.2, 35, 0, 0), (0.35, 5, 0, 0), (0.55, 10, 0, 0)])},
        "body": {"rotation": rot_kf([(0, 0, 0, 0), (0.2, -8, 0, 0), (0.55, 0, 0, 0)])},
    }

    return {
        "format_version": "1.8.0",
        "animations": {
            "animation.crystal_crab.idle": {"loop": True, "animation_length": 2.0, "bones": idle},
            "animation.crystal_crab.walk": {"loop": True, "animation_length": 1.0, "bones": walk},
            "animation.crystal_crab.attack": {"animation_length": 0.55, "bones": attack},
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
    for p in (ASSETS / "geo/crystal_crab.geo.json", ART / "crystal_crab.geo.json"):
        write_json(p, geo)
    for p in (ASSETS / "animations/crystal_crab.animation.json", ART / "crystal_crab.animation.json"):
        write_json(p, anims)
    tex = ASSETS / "textures/entity/crystal_crab.png"
    tex.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(tex)
    ART.mkdir(parents=True, exist_ok=True)
    atlas.save(ART / "crystal_crab.png")
    atlas.resize((ATLAS * 2, ATLAS * 2), Image.NEAREST).save(ART / "uv_guide_preview.png")
    print("Rebuilt crystal_crab — beige shell + amber crystals")
    print(" bones:", len(BONE_ORDER), BONE_ORDER)
    print(" ->", tex)


if __name__ == "__main__":
    main()
