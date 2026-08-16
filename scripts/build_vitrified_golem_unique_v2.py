#!/usr/bin/env python3
"""
Vitrified Golem unique pipeline v2 — custom geo + opaque atlas (no vanilla UV).

Reads SEGMENT_LOCK layout. Writes:
  art/vitrified_wastes/golem/vitrified_golem.geo.json
  art/vitrified_wastes/golem/vitrified_golem_atlas_v2.png
  src/.../geo/vitrified_golem.geo.json
  src/.../textures/entity/vitrified_golem.png

  python scripts/build_vitrified_golem_unique_v2.py
"""
from __future__ import annotations

import json
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "art/vitrified_wastes/golem"
ASSETS = ROOT / "src/main/resources/assets/effecoria"
ATLAS = 128

# Palette (opaque)
BASE = (10, 14, 32, 255)
BASE_MID = (18, 28, 58, 255)
ULTRA = (16, 24, 72, 255)
ULTRA_RICH = (28, 40, 110, 255)
EDGE = (4, 6, 16, 255)
GOLD = (240, 195, 55, 255)
GOLD_HI = (255, 230, 120, 255)
GOLD_LO = (180, 120, 28, 255)
CYAN = (20, 200, 245, 255)
CYAN_CORE = (210, 250, 255, 255)
CRYSTAL = (70, 150, 210, 255)
CRYSTAL_HI = (140, 210, 245, 255)
CRYSTAL_LO = (40, 90, 140, 255)

# name -> (parent, pivot, [(origin, size, uv_origin), ...])
PARTS: dict = {
    "head": {
        "parent": "root",
        "pivot": (0, 26, 0),
        "cubes": [
            ((-4, 26, -4), (8, 8, 8), (0, 0)),  # head
            ((-2, 29, -5), (4, 3, 1), (32, 0)),  # visor
            ((-4, 34, -1), (2, 3, 2), (48, 0)),  # horn_l
            ((2, 34, -1), (2, 3, 2), (56, 0)),  # horn_r
        ],
    },
    "body": {
        "parent": "root",
        "pivot": (0, 20, 0),
        "cubes": [
            ((-5, 14, -3), (10, 12, 6), (0, 20)),  # torso
            ((-2, 18, -6), (4, 4, 3), (34, 20)),  # core protrudes from chest (not buried)
            ((-9, 23, -2), (4, 3, 5), (34, 30)),  # pauldron_l (entity left = +X)
            ((5, 23, -2), (4, 3, 5), (54, 30)),  # pauldron_r
            ((-2, 24, 3), (3, 6, 2), (74, 0)),  # spike1
            ((1, 25, 3), (3, 7, 2), (86, 0)),  # spike2
            ((-1, 22, 4), (2, 5, 2), (98, 0)),  # spike3
        ],
    },
    "right_arm": {
        "parent": "root",
        "pivot": (-7, 24, 0),
        "cubes": [
            ((-11, 10, -2), (4, 14, 4), (0, 42)),
            ((-12, 8, -3), (3, 8, 2), (18, 42)),  # blade_a
            ((-13, 6, -1), (2, 6, 2), (30, 42)),  # blade_b
        ],
    },
    "left_arm": {
        "parent": "root",
        "pivot": (7, 24, 0),
        "cubes": [
            ((7, 16, -1), (3, 6, 3), (40, 42)),
            ((8, 10, -2), (3, 8, 3), (54, 42)),
            ((9, 8, 0), (2, 5, 2), (68, 42)),
        ],
    },
    "right_leg": {
        "parent": "root",
        "pivot": (-2.5, 14, 0),
        "cubes": [
            ((-5, 2, -2), (4, 12, 4), (80, 42)),
            ((-5.5, 0, -2.5), (5, 2, 5), (98, 42)),
        ],
    },
    "left_leg": {
        "parent": "root",
        "pivot": (2.5, 14, 0),
        "cubes": [
            ((1, 2, -2), (4, 12, 4), (0, 64)),
            ((0.5, 0, -2.5), (5, 2, 5), (18, 64)),
        ],
    },
}


def box_faces(u: int, v: int, w: int, h: int, d: int) -> dict[str, tuple[int, int, int, int]]:
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + 2 * d + w, v + d, w, h),
    }


def footprint(w: int, h: int, d: int) -> tuple[int, int]:
    return 2 * d + 2 * w, d + h


def validate_uv() -> None:
    occupied: list[tuple[int, int, int, int, str]] = []
    for bone, spec in PARTS.items():
        for i, (_o, size, uv) in enumerate(spec["cubes"]):
            w, h, d = size
            fw, fh = footprint(w, h, d)
            u, v = uv
            name = f"{bone}[{i}]"
            if u + fw > ATLAS or v + fh > ATLAS:
                raise SystemExit(f"{name} UV out of atlas: {(u,v)}+{(fw,fh)}")
            rect = (u, v, u + fw, v + fh)
            for ou, ov, ou2, ov2, oname in occupied:
                if not (rect[2] <= ou or rect[0] >= ou2 or rect[3] <= ov or rect[1] >= ov2):
                    raise SystemExit(f"UV overlap {name} vs {oname}")
            occupied.append((*rect, name))
    print("UV validate OK,", len(occupied), "islands")


def paint_glass(im: Image.Image, u: int, v: int, w: int, h: int, seed: int) -> None:
    rng = random.Random(seed)
    draw = ImageDraw.Draw(im)
    draw.rectangle([u, v, u + w - 1, v + h - 1], fill=BASE)
    for y in range(v, v + h):
        for x in range(u, u + w):
            t = (x - u) / max(1, w - 1)
            col = ULTRA if t < 0.35 else (ULTRA_RICH if t > 0.7 else BASE_MID)
            if rng.random() < 0.5:
                im.putpixel((x, y), col)
    if w > 2 and h > 2:
        draw.rectangle([u, v, u + w - 1, v + h - 1], outline=EDGE)
    # gold cracks
    if w >= 3 and h >= 4:
        x = u + w // 2
        for y in range(v + 1, v + h - 1):
            xx = x + rng.choice([-1, 0, 0, 1])
            if u < xx < u + w - 1:
                im.putpixel((xx, y), GOLD if y % 3 else GOLD_HI)
        if h >= 6:
            y = v + h // 2
            for x in range(u + 1, u + w - 1):
                im.putpixel((x, y), GOLD_LO if x % 2 else GOLD)


def paint_crystal(im: Image.Image, u: int, v: int, w: int, h: int, seed: int) -> None:
    rng = random.Random(seed)
    draw = ImageDraw.Draw(im)
    draw.rectangle([u, v, u + w - 1, v + h - 1], fill=CRYSTAL_LO)
    for y in range(v, v + h):
        for x in range(u, u + w):
            s = (y - v) / max(1, h - 1)
            col = CRYSTAL_HI if s < 0.35 else (CRYSTAL if s < 0.7 else CRYSTAL_LO)
            if rng.random() < 0.7:
                im.putpixel((x, y), col)
    if w > 2 and h > 2:
        draw.rectangle([u, v, u + w - 1, v + h - 1], outline=CYAN)
        im.putpixel((u + w // 2, v + 1), CYAN_CORE)


def paint_phi(im: Image.Image, u: int, v: int, w: int, h: int) -> None:
    """Small Φ glyph centered on face."""
    if w < 3 or h < 4:
        return
    cx, cy = u + w // 2, v + h // 2
    for dy in range(-2, 3):
        im.putpixel((cx, cy + dy), CYAN_CORE)
    for dx in (-1, 1):
        for dy in (-1, 0, 1):
            x, y = cx + dx, cy + dy
            if u <= x < u + w and v <= y < v + h:
                im.putpixel((x, y), CYAN)


def paint_box(im: Image.Image, uv: tuple[int, int], size: tuple[int, int, int], kind: str, seed: int) -> None:
    w, h, d = size
    faces = box_faces(uv[0], uv[1], w, h, d)
    for name, (fu, fv, fw, fh) in faces.items():
        if kind == "crystal":
            paint_crystal(im, fu, fv, fw, fh, seed + hash(name) % 97)
        else:
            paint_glass(im, fu, fv, fw, fh, seed + hash(name) % 97)
        if name == "front" and kind in ("head", "core", "visor"):
            paint_phi(im, fu, fv, fw, fh)
        if kind == "core":
            # all faces glow-ish
            draw = ImageDraw.Draw(im)
            draw.rectangle([fu, fv, fu + fw - 1, fv + fh - 1], outline=CYAN)
            if fw >= 2 and fh >= 2:
                im.putpixel((fu + fw // 2, fv + fh // 2), CYAN_CORE)


def build_atlas() -> Image.Image:
    im = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))
    # classify cubes by index per bone
    kinds = {
        ("head", 0): "head",
        ("head", 1): "visor",
        ("head", 2): "crystal",
        ("head", 3): "crystal",
        ("body", 0): "glass",
        ("body", 1): "core",
        ("body", 2): "glass",
        ("body", 3): "glass",
        ("body", 4): "crystal",
        ("body", 5): "crystal",
        ("body", 6): "crystal",
        ("right_arm", 0): "glass",
        ("right_arm", 1): "crystal",
        ("right_arm", 2): "crystal",
        ("left_arm", 0): "crystal",
        ("left_arm", 1): "crystal",
        ("left_arm", 2): "crystal",
        ("right_leg", 0): "glass",
        ("right_leg", 1): "glass",
        ("left_leg", 0): "glass",
        ("left_leg", 1): "glass",
    }
    seed = 42
    for bone, spec in PARTS.items():
        for i, (_o, size, uv) in enumerate(spec["cubes"]):
            kind = kinds.get((bone, i), "glass")
            paint_box(im, uv, size, kind, seed)
            seed += 11
    # force opaque on all painted pixels
    px = im.load()
    for y in range(ATLAS):
        for x in range(ATLAS):
            r, g, b, a = px[x, y]
            if a > 0 and a < 255:
                px[x, y] = (r, g, b, 255)
    return im


def build_geo() -> dict:
    bones = [{"name": "root", "pivot": [0, 0, 0]}]
    for name, spec in PARTS.items():
        cubes = []
        for origin, size, uv in spec["cubes"]:
            cubes.append(
                {
                    "origin": [float(origin[0]), float(origin[1]), float(origin[2])],
                    "size": [float(size[0]), float(size[1]), float(size[2])],
                    "uv": [int(uv[0]), int(uv[1])],
                }
            )
        bones.append(
            {
                "name": name,
                "parent": spec["parent"],
                "pivot": [float(spec["pivot"][0]), float(spec["pivot"][1]), float(spec["pivot"][2])],
                "cubes": cubes,
            }
        )
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": "geometry.vitrified_golem",
                    "texture_width": ATLAS,
                    "texture_height": ATLAS,
                    "visible_bounds_width": 3.0,
                    "visible_bounds_height": 3.2,
                    "visible_bounds_offset": [0, 1.3, 0],
                },
                "bones": bones,
            }
        ],
    }


def write_labeled_uv(im: Image.Image) -> None:
    """Preview strip for artists — regions outlined."""
    preview = im.copy().convert("RGBA")
    draw = ImageDraw.Draw(preview)
    for bone, spec in PARTS.items():
        for i, (_o, size, uv) in enumerate(spec["cubes"]):
            w, h, d = size
            fw, fh = footprint(w, h, d)
            u, v = uv
            draw.rectangle([u, v, u + fw - 1, v + fh - 1], outline=(255, 0, 255, 255))
    preview = preview.resize((ATLAS * 4, ATLAS * 4), Image.NEAREST)
    preview.save(ART / "vitrified_golem_uv_labeled_v2_4x.png")


def main() -> None:
    ART.mkdir(parents=True, exist_ok=True)
    validate_uv()
    geo = build_geo()
    atlas = build_atlas()
    write_labeled_uv(atlas)

    geo_path = ART / "vitrified_golem.geo.json"
    geo_game = ASSETS / "geo/vitrified_golem.geo.json"
    tex_art = ART / "vitrified_golem_atlas_v2.png"
    tex_game = ASSETS / "textures/entity/vitrified_golem.png"

    geo_path.write_text(json.dumps(geo, indent=2) + "\n", encoding="utf-8")
    geo_game.write_text(json.dumps(geo, indent=2) + "\n", encoding="utf-8")
    atlas.save(tex_art)
    atlas.save(tex_game)
    # 8x preview
    atlas.resize((ATLAS * 4, ATLAS * 4), Image.NEAREST).save(ART / "vitrified_golem_atlas_v2_4x.png")
    print("wrote", geo_game.relative_to(ROOT))
    print("wrote", tex_game.relative_to(ROOT))
    print("cubes", sum(len(s["cubes"]) for s in PARTS.values()))


if __name__ == "__main__":
    main()
