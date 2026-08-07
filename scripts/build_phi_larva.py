#!/usr/bin/env python3
"""
Build Φ-Larva from SEGMENT_LOCK (beige chitin + amber Φ glow).

  python scripts/build_phi_larva.py
"""

from __future__ import annotations

import json
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/effecoria"
ART = ROOT / "art/phi_larva"
ATLAS = 128

CHITIN = (184, 152, 104, 255)
CHITIN_MID = (176, 144, 96, 255)
CHITIN_DARK = (144, 112, 72, 255)
CHITIN_HI = (200, 168, 120, 255)
BELLY = (200, 176, 128, 255)
BELLY_HI = (216, 192, 144, 255)
GLOW = (248, 216, 104, 255)
GLOW_HI = (255, 240, 160, 255)
GLOW_LO = (220, 160, 64, 255)
DARK = (20, 16, 12, 255)

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
        raise SystemExit(f"Atlas overflow UV ({_UV[0]},{_UV[1]}) need {need_w}x{need_h}")
    u, v = _UV[0], _UV[1]
    _UV[0] += need_w + 1
    _UV[2] = max(_UV[2], need_h)
    return u, v


def add_bone(name: str, parent: str, pivot: tuple[float, float, float], cubes: list):
    packed = [(o, s, _alloc_uv(*s)) for o, s in cubes]
    PARTS[name] = {"parent": parent, "pivot": pivot, "cubes": packed}
    BONE_ORDER.append(name)


def seg_cubes(cx: float, cy: float, cz: float, w: float, h: float, d: float, *, rear_phi: bool = False):
    """Main ring + belly + L/R glow spots + dorsal speck (+ optional rear Φ plate)."""
    cubes = [
        ((cx - w / 2, cy - h / 2, cz - d / 2), (w, h, d)),
        ((cx - w * 0.4, cy - h / 2, cz - d * 0.4), (w * 0.8, h * 0.35, d * 0.8)),
        ((cx - w / 2 - 0.6, cy - 0.4, cz - 0.5), (1.0, 1.2, 1.0)),  # L glow
        ((cx + w / 2 - 0.4, cy - 0.4, cz - 0.5), (1.0, 1.2, 1.0)),  # R glow
        ((cx - 0.4, cy + h / 2 - 0.3, cz - 0.4), (0.8, 0.8, 0.8)),  # dorsal
    ]
    if rear_phi:
        cubes.append(((cx - 1.2, cy - 1.0, cz + d / 2 - 0.4), (2.4, 2.4, 0.5)))
    return cubes


def build_parts():
    PARTS.clear()
    BONE_ORDER.clear()
    _UV[0] = _UV[1] = _UV[2] = 0

    # Ground at y=0; grub sits on belly. Length along -Z (head) to +Z (tail).
    # Overlapping depths so rings don't hollow when bent.
    specs = [
        # name, parent, pivot, w,h,d, rear_phi
        ("seg_0", "root", (0, 2.2, -2.5), 5.0, 4.0, 3.2, False),
        ("seg_1", "seg_0", (0, 2.3, 0.2), 5.5, 4.2, 3.2, False),
        ("seg_2", "seg_1", (0, 2.4, 2.9), 6.0, 4.5, 3.2, False),
        ("seg_3", "seg_2", (0, 2.3, 5.6), 5.5, 4.2, 3.2, False),
        ("seg_4", "seg_3", (0, 2.2, 8.3), 5.0, 4.0, 3.2, False),
        ("seg_5", "seg_4", (0, 2.0, 10.8), 4.0, 3.5, 3.0, True),
    ]
    for name, parent, pivot, w, h, d, rear in specs:
        px, py, pz = pivot
        add_bone(name, parent, pivot, seg_cubes(px, py, pz, w, h, d, rear_phi=rear))

    add_bone(
        "head",
        "seg_0",
        (0, 2.4, -4.2),
        [
            ((-2.1, 1.0, -7.0), (4.2, 3.4, 3.2)),
            ((-1.6, 1.0, -6.5), (3.2, 1.2, 2.4)),  # belly under head
        ],
    )
    add_bone(
        "left_antenna",
        "head",
        (-1.2, 4.2, -6.2),
        [
            ((-1.4, 4.0, -6.4), (0.6, 1.6, 0.6)),
            ((-1.55, 5.4, -6.55), (0.9, 0.9, 0.9)),
        ],
    )
    add_bone(
        "right_antenna",
        "head",
        (1.2, 4.2, -6.2),
        [
            ((0.8, 4.0, -6.4), (0.6, 1.6, 0.6)),
            ((0.65, 5.4, -6.55), (0.9, 0.9, 0.9)),
        ],
    )
    add_bone("mandible_l", "head", (-1.2, 1.4, -6.8), [((-1.8, 1.1, -7.4), (1.0, 0.9, 1.2))])
    add_bone("mandible_r", "head", (1.2, 1.4, -6.8), [((0.8, 1.1, -7.4), (1.0, 0.9, 1.2))])


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

    if kind == "dark":
        draw.rectangle([u, v, u + w - 1, v + h - 1], fill=DARK)
        return
    if kind == "glow":
        draw.rectangle([u, v, u + w - 1, v + h - 1], fill=GLOW)
        cx, cy = u + w // 2, v + h // 2
        put(im, cx, cy, GLOW_HI, u, v, w, h)
        if w > 2 and h > 2:
            draw.rectangle([u, v, u + w - 1, v + h - 1], outline=GLOW_LO)
        return
    if kind == "phi":
        draw.rectangle([u, v, u + w - 1, v + h - 1], fill=CHITIN_DARK)
        # crude Φ: vertical bar + oval
        cx = u + w // 2
        for y in range(v + 1, v + h - 1):
            put(im, cx, y, GLOW_HI, u, v, w, h)
            put(im, cx - 1, y, GLOW, u, v, w, h)
        oy0, oy1 = v + h // 4, v + 3 * h // 4
        ox0, ox1 = u + w // 4, u + 3 * w // 4
        for y in range(oy0, oy1 + 1):
            for x in range(ox0, ox1 + 1):
                # ring
                on_edge = x in (ox0, ox1) or y in (oy0, oy1)
                if on_edge:
                    put(im, x, y, GLOW, u, v, w, h)
        return
    if kind == "belly":
        for y in range(v, v + h):
            for x in range(u, u + w):
                im.putpixel((x, y), BELLY if rng.random() > 0.2 else BELLY_HI)
        for y in range(v + 1, v + h - 1, 2):
            for x in range(u, u + w):
                put(im, x, y, CHITIN_DARK, u, v, w, h)
        return
    if kind == "eye":
        for y in range(v, v + h):
            for x in range(u, u + w):
                n = (x + y) % 3
                im.putpixel((x, y), [CHITIN, CHITIN_MID, CHITIN_HI][n])
        if face == "front" and w >= 4 and h >= 3:
            # two black eyes
            ey = v + h // 2
            put(im, u + w // 3, ey, DARK, u, v, w, h)
            put(im, u + 2 * w // 3, ey, DARK, u, v, w, h)
            put(im, u + w // 3, ey - 1, DARK, u, v, w, h)
            put(im, u + 2 * w // 3, ey - 1, DARK, u, v, w, h)
        if w > 2 and h > 2:
            draw.rectangle([u, v, u + w - 1, v + h - 1], outline=CHITIN_DARK)
        px = im.load()
        for y in range(v, v + h):
            for x in range(u, u + w):
                rr, gg, bb, aa = px[x, y]
                if aa > 0:
                    px[x, y] = (rr, gg, bb, 255)
        return

    # chitin
    for y in range(v, v + h):
        for x in range(u, u + w):
            n = (x * 3 + y * 5 + rng.randint(0, 2)) % 4
            col = [CHITIN, CHITIN_MID, CHITIN_DARK, CHITIN_HI][n]
            im.putpixel((x, y), col)
    if w > 2 and h > 2:
        draw.rectangle([u, v, u + w - 1, v + h - 1], outline=CHITIN_DARK)
    px = im.load()
    for y in range(v, v + h):
        for x in range(u, u + w):
            rr, gg, bb, aa = px[x, y]
            if aa > 0:
                px[x, y] = (rr, gg, bb, 255)


def classify(name: str, i: int) -> str:
    if name.startswith("mandible"):
        return "dark"
    if "antenna" in name:
        return "glow" if i == 1 else "chitin"
    if name == "head" and i == 0:
        return "eye"
    if name == "head" and i == 1:
        return "belly"
    if name.startswith("seg_"):
        if i == 1:
            return "belly"
        if i in (2, 3, 4):
            return "glow"
        if i == 5:
            return "phi"
        return "chitin"
    return "chitin"


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
                    "identifier": "geometry.phi_larva",
                    "texture_width": ATLAS,
                    "texture_height": ATLAS,
                    "visible_bounds_width": 3,
                    "visible_bounds_height": 1.5,
                    "visible_bounds_offset": [0, 0.5, 0],
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
    idle_bones = {
        "head": {"rotation": rot_kf([(0, 0, 0, 0), (1.0, 2, 4, 0), (2.0, 0, 0, 0)])},
        "left_antenna": {"rotation": rot_kf([(0, 0, 0, -8), (1.0, 0, 0, 10), (2.0, 0, 0, -8)])},
        "right_antenna": {"rotation": rot_kf([(0, 0, 0, 8), (1.0, 0, 0, -10), (2.0, 0, 0, 8)])},
        "mandible_l": {"rotation": rot_kf([(0, 0, 0, -4), (1.0, 0, 0, 4), (2.0, 0, 0, -4)])},
        "mandible_r": {"rotation": rot_kf([(0, 0, 0, 4), (1.0, 0, 0, -4), (2.0, 0, 0, 4)])},
    }
    for i in range(6):
        amp = 3 + (i % 2)
        idle_bones[f"seg_{i}"] = {
            "rotation": rot_kf([(0, 0, amp if i % 2 == 0 else -amp, 0), (1.0, 0, -amp if i % 2 == 0 else amp, 0), (2.0, 0, amp if i % 2 == 0 else -amp, 0)])
        }

    crawl_bones = {
        "head": {"rotation": rot_kf([(0, 6, 0, 0), (0.4, -4, 0, 0), (0.8, 6, 0, 0)])},
        "left_antenna": {"rotation": rot_kf([(0, 0, 0, -12), (0.4, 0, 0, 14), (0.8, 0, 0, -12)])},
        "right_antenna": {"rotation": rot_kf([(0, 0, 0, 12), (0.4, 0, 0, -14), (0.8, 0, 0, 12)])},
    }
    # Peristaltic wave along segments
    for i in range(6):
        phase = i * 0.1
        crawl_bones[f"seg_{i}"] = {
            "position": pos_kf(
                [
                    (0.0, 0, 0, 0),
                    (0.2 + phase * 0.1, 0, 0.55 if i % 2 == 0 else 0.15, 0),
                    (0.4 + phase * 0.1, 0, 0.15 if i % 2 == 0 else 0.55, 0),
                    (0.8, 0, 0, 0),
                ]
            ),
            "rotation": rot_kf(
                [
                    (0.0, 0, 4 if i % 2 == 0 else -4, 0),
                    (0.4, 0, -6 if i % 2 == 0 else 6, 0),
                    (0.8, 0, 4 if i % 2 == 0 else -4, 0),
                ]
            ),
        }

    return {
        "format_version": "1.8.0",
        "animations": {
            "animation.phi_larva.idle": {"loop": True, "animation_length": 2.0, "bones": idle_bones},
            "animation.phi_larva.crawl": {"loop": True, "animation_length": 0.8, "bones": crawl_bones},
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
    for p in (ASSETS / "geo/phi_larva.geo.json", ART / "phi_larva.geo.json"):
        write_json(p, geo)
    for p in (ASSETS / "animations/phi_larva.animation.json", ART / "phi_larva.animation.json"):
        write_json(p, anims)
    tex = ASSETS / "textures/entity/phi_larva.png"
    tex.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(tex)
    ART.mkdir(parents=True, exist_ok=True)
    atlas.save(ART / "phi_larva.png")
    atlas.resize((ATLAS * 2, ATLAS * 2), Image.NEAREST).save(ART / "uv_guide_preview.png")
    print("Rebuilt phi_larva — beige chitin + amber glow, 6 segments")
    print(" bones:", BONE_ORDER)
    print(" ->", tex)


if __name__ == "__main__":
    main()
