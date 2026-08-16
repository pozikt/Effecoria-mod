#!/usr/bin/env python3
"""Shared helpers for Effecoria unique fauna geo + opaque atlases (no vanilla UV)."""
from __future__ import annotations

import json
import random
from pathlib import Path
from typing import Callable

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/effecoria"

# Kind painters receive (im, u, v, w, h, seed)
PaintFn = Callable[[Image.Image, int, int, int, int, int], None]


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


def validate_parts(parts: dict, atlas: int, label: str) -> None:
    occupied: list[tuple[int, int, int, int, str]] = []
    for bone, spec in parts.items():
        for i, (_o, size, uv) in enumerate(spec["cubes"]):
            w, h, d = size
            fw, fh = footprint(w, h, d)
            u, v = uv
            name = f"{label}.{bone}[{i}]"
            if u + fw > atlas or v + fh > atlas:
                raise SystemExit(f"{name} UV out of {atlas}: {(u, v)}+{(fw, fh)}")
            rect = (u, v, u + fw, v + fh)
            for ou, ov, ou2, ov2, oname in occupied:
                if not (rect[2] <= ou or rect[0] >= ou2 or rect[3] <= ov or rect[1] >= ov2):
                    raise SystemExit(f"UV overlap {name} vs {oname}")
            occupied.append((*rect, name))
    print(f"  UV OK {label}: {len(occupied)} islands")


def force_opaque(im: Image.Image) -> None:
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if 0 < a < 255:
                px[x, y] = (r, g, b, 255)


def paint_fill(
    im: Image.Image,
    u: int,
    v: int,
    w: int,
    h: int,
    base: tuple[int, int, int, int],
    mid: tuple[int, int, int, int],
    hi: tuple[int, int, int, int],
    edge: tuple[int, int, int, int],
    seed: int,
    *,
    flecks: tuple[int, int, int, int] | None = None,
) -> None:
    rng = random.Random(seed)
    draw = ImageDraw.Draw(im)
    draw.rectangle([u, v, u + w - 1, v + h - 1], fill=base)
    for y in range(v, v + h):
        for x in range(u, u + w):
            t = (x - u) / max(1, w - 1)
            s = (y - v) / max(1, h - 1)
            col = hi if s < 0.25 else (mid if t > 0.55 else base)
            if rng.random() < 0.45:
                im.putpixel((x, y), col)
            if flecks and rng.random() < 0.04:
                im.putpixel((x, y), flecks)
    if w > 2 and h > 2:
        draw.rectangle([u, v, u + w - 1, v + h - 1], outline=edge)


def paint_eye(
    im: Image.Image,
    u: int,
    v: int,
    w: int,
    h: int,
    color: tuple[int, int, int, int],
    *,
    twin: bool = True,
) -> None:
    if w < 3 or h < 3:
        return
    ey = v + max(1, h // 3)
    if twin and w >= 5:
        im.putpixel((u + w // 3, ey), color)
        im.putpixel((u + 2 * w // 3, ey), color)
    else:
        im.putpixel((u + w // 2, ey), color)


def build_geo(
    identifier: str,
    parts: dict,
    atlas: int,
    *,
    bounds_w: float = 2.5,
    bounds_h: float = 2.5,
) -> dict:
    bones = [{"name": "root", "pivot": [0, 0, 0]}]
    for name, spec in parts.items():
        cubes = []
        for origin, size, uv in spec["cubes"]:
            cubes.append(
                {
                    "origin": [float(x) for x in origin],
                    "size": [float(x) for x in size],
                    "uv": [int(uv[0]), int(uv[1])],
                }
            )
        bones.append(
            {
                "name": name,
                "parent": spec["parent"],
                "pivot": [float(x) for x in spec["pivot"]],
                "cubes": cubes,
            }
        )
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": identifier,
                    "texture_width": atlas,
                    "texture_height": atlas,
                    "visible_bounds_width": bounds_w,
                    "visible_bounds_height": bounds_h,
                    "visible_bounds_offset": [0, bounds_h / 2, 0],
                },
                "bones": bones,
            }
        ],
    }


def simple_idle_walk_anim(
    mob_id: str,
    *,
    limbs: tuple[str, ...] = ("left_leg", "right_leg"),
    arms: tuple[str, ...] = (),
    fly_bones: tuple[str, ...] = (),
    mode: str = "walk",
) -> dict:
    """Minimal Bedrock-style anim JSON for GeckoLib."""
    move_name = {
        "walk": f"animation.{mob_id}.walk",
        "crawl": f"animation.{mob_id}.crawl",
        "fly": f"animation.{mob_id}.fly",
    }[mode]
    idle_bones: dict = {}
    move_bones: dict = {}

    def rot_keys(a, b, c=(0, 0, 0)):
        return {
            "0": {"vector": list(a)},
            "0.5": {"vector": list(b)},
            "1.0": {"vector": list(c if c != (0, 0, 0) else a)},
        }

    idle_bones["body"] = {"rotation": rot_keys((0, 0, -1), (0, 0, 1), (0, 0, -1))}
    if "head" in (limbs + arms + ("head",)):
        idle_bones["head"] = {"rotation": rot_keys((0, -3, 0), (0, 3, 0), (0, -3, 0))}

    for i, bone in enumerate(limbs):
        sign = 1 if i % 2 == 0 else -1
        move_bones[bone] = {
            "rotation": {
                "0": {"vector": [20 * sign, 0, 0]},
                "0.5": {"vector": [-20 * sign, 0, 0]},
                "1.0": {"vector": [20 * sign, 0, 0]},
            }
        }
    for i, bone in enumerate(arms):
        sign = 1 if i % 2 == 0 else -1
        move_bones[bone] = {
            "rotation": {
                "0": {"vector": [-15 * sign, 0, 4]},
                "0.5": {"vector": [15 * sign, 0, 4]},
                "1.0": {"vector": [-15 * sign, 0, 4]},
            }
        }
    for bone in fly_bones:
        move_bones[bone] = {
            "rotation": {
                "0": {"vector": [0, 0, -35]},
                "0.5": {"vector": [0, 0, 35]},
                "1.0": {"vector": [0, 0, -35]},
            }
        }

    anims = {
        f"animation.{mob_id}.idle": {
            "loop": True,
            "animation_length": 2.0,
            "bones": idle_bones,
        },
        move_name: {
            "loop": True,
            "animation_length": 1.0,
            "bones": move_bones or idle_bones,
        },
    }
    return {"format_version": "1.8.0", "animations": anims}


def write_mob_assets(
    mob_id: str,
    art_dir: Path,
    parts: dict,
    paint_kinds: dict[tuple[str, int], str],
    painters: dict[str, PaintFn],
    *,
    atlas: int = 64,
    identifier: str | None = None,
    anim_mode: str = "walk",
    limbs: tuple[str, ...] = ("left_leg", "right_leg"),
    arms: tuple[str, ...] = ("left_arm", "right_arm"),
    fly_bones: tuple[str, ...] = (),
    bounds: tuple[float, float] = (2.0, 2.0),
) -> None:
    identifier = identifier or f"geometry.{mob_id}"
    art_dir.mkdir(parents=True, exist_ok=True)
    validate_parts(parts, atlas, mob_id)

    im = Image.new("RGBA", (atlas, atlas), (0, 0, 0, 0))
    seed = 100
    for bone, spec in parts.items():
        for i, (_o, size, uv) in enumerate(spec["cubes"]):
            kind = paint_kinds.get((bone, i), "fur")
            painter = painters.get(kind) or painters["fur"]
            w, h, d = size
            faces = box_faces(uv[0], uv[1], w, h, d)
            for fname, (fu, fv, fw, fh) in faces.items():
                painter(im, fu, fv, fw, fh, seed)
                if fname == "front" and kind in ("face", "head"):
                    paint_eye(im, fu, fv, fw, fh, (240, 220, 80, 255))
                seed += 7
    force_opaque(im)

    geo = build_geo(identifier, parts, atlas, bounds_w=bounds[0], bounds_h=bounds[1])
    anim = simple_idle_walk_anim(
        mob_id, limbs=limbs, arms=arms, fly_bones=fly_bones, mode=anim_mode
    )

    (art_dir / f"{mob_id}.geo.json").write_text(json.dumps(geo, indent=2) + "\n", encoding="utf-8")
    (ASSETS / "geo" / f"{mob_id}.geo.json").write_text(json.dumps(geo, indent=2) + "\n", encoding="utf-8")
    (ASSETS / "animations" / f"{mob_id}.animation.json").write_text(
        json.dumps(anim, indent=2) + "\n", encoding="utf-8"
    )
    im.save(art_dir / f"{mob_id}_atlas.png")
    im.save(ASSETS / "textures/entity" / f"{mob_id}.png")
    im.resize((atlas * 4, atlas * 4), Image.NEAREST).save(art_dir / f"{mob_id}_atlas_4x.png")
    print(f"wrote {mob_id} ({sum(len(s['cubes']) for s in parts.values())} cubes)")
