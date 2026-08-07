#!/usr/bin/env python3
"""
Vitrified Golem from scratch: integer box geo + opaque atlas + iron-style anims.

Proportions locked in art/vitrified_wastes/golem/DESIGN.md (~2.1 blocks tall).
Feet at y=0. 1 cube unit = 1 texel. No float-scaled iron UV.

  python scripts/build_vitrified_golem_from_scratch.py
"""

from __future__ import annotations

import json
import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/effecoria"
ART = ROOT / "art/vitrified_wastes/golem"
ATLAS = 128

# name -> (parent, pivot, [(origin, size, uv), ...])
# Height stack: legs 0–14, body 14–26, head 26–34 → ~2.125 blocks
PARTS = {
    "head": {
        "parent": "root",
        "pivot": (0, 26, 0),
        "cubes": [
            ((-4, 26, -4), (8, 8, 8), (0, 0)),  # head
            ((-2, 29, -5), (4, 3, 1), (32, 0)),  # Φ-eye plate (front)
        ],
    },
    "body": {
        "parent": "root",
        "pivot": (0, 20, 0),
        "cubes": [
            ((-5, 14, -3), (10, 12, 6), (0, 40)),  # torso
            ((-6, 24, -4), (12, 3, 8), (0, 70)),  # shoulder plate
        ],
    },
    "right_arm": {
        "parent": "root",
        "pivot": (-6, 24, 0),
        "cubes": [
            ((-10, 10, -2), (4, 16, 4), (60, 16)),
        ],
    },
    "left_arm": {
        "parent": "root",
        "pivot": (6, 24, 0),
        "cubes": [
            ((6, 10, -2), (4, 16, 4), (80, 16)),
        ],
    },
    "right_leg": {
        "parent": "root",
        "pivot": (-2.5, 14, 0),
        "cubes": [
            ((-5, 0, -2), (4, 14, 4), (40, 0)),
        ],
    },
    "left_leg": {
        "parent": "root",
        "pivot": (2.5, 14, 0),
        "cubes": [
            ((1, 0, -2), (4, 14, 4), (56, 0)),
        ],
    },
}

# Paint colors (opaque) — dark glass + varied gold veins + cyan eye
BASE = (10, 14, 32, 255)
BASE_MID = (18, 28, 58, 255)
BASE_HI = (32, 48, 92, 255)
ULTRA_DEEP = (6, 10, 28, 255)
ULTRA = (16, 24, 72, 255)
ULTRA_RICH = (28, 40, 110, 255)
EDGE = (4, 6, 16, 255)
GLOSS = (70, 95, 150, 255)
GLOSS_HI = (120, 150, 200, 255)

GOLD_PALETTE = [
    (255, 230, 120, 255),  # bright highlight
    (240, 195, 55, 255),  # classic gold
    (210, 150, 35, 255),  # warm amber
    (180, 120, 28, 255),  # deep brass
    (150, 95, 22, 255),  # shadow gold
]
GOLD = GOLD_PALETTE[1]
GOLD_HI = GOLD_PALETTE[0]
GOLD_LO = GOLD_PALETTE[3]

CYAN = (20, 200, 245, 255)
CYAN_CORE = (210, 250, 255, 255)
CYAN_RING = (50, 160, 200, 255)
CYAN_GLOW = (90, 230, 255, 255)


def box_faces(u: int, v: int, w: int, h: int, d: int) -> dict[str, tuple[int, int, int, int]]:
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + 2 * d + w, v + d, w, h),
    }


def _put(im: Image.Image, x: int, y: int, color: tuple[int, int, int, int], u0: int, v0: int, w: int, h: int) -> None:
    if u0 <= x < u0 + w and v0 <= y < v0 + h:
        im.putpixel((x, y), color)


def _line(
    im: Image.Image,
    x0: int,
    y0: int,
    x1: int,
    y1: int,
    u0: int,
    v0: int,
    fw: int,
    fh: int,
    *,
    colors: list[tuple[int, int, int, int]],
    seed: int = 0,
) -> None:
    """Bresenham crack with varying gold shades along the stroke."""
    rng = __import__("random").Random(seed ^ (x0 * 31 + y0))
    dx = abs(x1 - x0)
    dy = -abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx + dy
    x, y = x0, y0
    i = 0
    while True:
        color = colors[i % len(colors)] if i % 5 else colors[0]
        if i % 7 == 3:
            color = colors[-1]
        _put(im, x, y, color, u0, v0, fw, fh)
        if x == x1 and y == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x += sx
        if e2 <= dx:
            err += dx
            y += sy
        i += 1


def _polyline(
    im: Image.Image,
    points: list[tuple[int, int]],
    u0: int,
    v0: int,
    fw: int,
    fh: int,
    *,
    seed: int = 0,
) -> None:
    for a, b in zip(points, points[1:]):
        # shuffle palette order per segment for variety
        pal = GOLD_PALETTE[seed % len(GOLD_PALETTE) :] + GOLD_PALETTE[: seed % len(GOLD_PALETTE)]
        _line(im, a[0], a[1], b[0], b[1], u0, v0, fw, fh, colors=pal, seed=seed)
        seed += 1


def _paint_glass_base(im: Image.Image, u: int, v: int, w: int, h: int, seed: int) -> None:
    """Dark ultramarine glass with depth pockets and specular gloss."""
    rng = __import__("random").Random(seed)
    draw = ImageDraw.Draw(im)
    draw.rectangle([u, v, u + w - 1, v + h - 1], fill=BASE)

    # Rich ultramarine body — not flat black
    for y in range(v, v + h):
        for x in range(u, u + w):
            t = (x - u) / max(1, w - 1)
            s = (y - v) / max(1, h - 1)
            # left/bottom = deeper ultra, right/top = richer mid
            if t < 0.35:
                col = ULTRA_DEEP if s > 0.55 else ULTRA
            elif t > 0.7:
                col = ULTRA_RICH if s < 0.45 else BASE_MID
            else:
                col = BASE_MID if (int(x) + int(y) + seed) % 5 == 0 else BASE
            if rng.random() < 0.55:
                im.putpixel((x, y), col)

    # Extra deep pockets
    for _ in range(max(2, (w * h) // 12)):
        px = u + rng.randint(0, max(0, w - 1))
        py = v + rng.randint(0, max(0, h - 1))
        im.putpixel((px, py), ULTRA_DEEP)
        if px + 1 < u + w:
            im.putpixel((px + 1, py), ULTRA)

    # Specular gloss streak (clear shine)
    if w >= 3 and h >= 3:
        gx = u + max(1, w // 5)
        length = min(w, h) - 1
        for t in range(length):
            x = gx + t // 2
            y = v + 1 + t
            if not (u <= x < u + w and v <= y < v + h):
                continue
            if t <= 1:
                im.putpixel((x, y), GLOSS_HI)
            elif t <= length // 2:
                im.putpixel((x, y), GLOSS)
            else:
                im.putpixel((x, y), BASE_HI)
            if x + 1 < u + w and t % 2 == 0:
                im.putpixel((x + 1, y), GLOSS if t < length // 3 else BASE_HI)

    # Rim
    if w > 2 and h > 2:
        draw.rectangle([u, v, u + w - 1, v + h - 1], outline=EDGE)
        for x in range(u + 1, u + w - 1):
            im.putpixel((x, v + 1), BASE_HI)
        for y in range(v + 2, v + h - 1):
            im.putpixel((u + w - 2, y), ULTRA_RICH)


def _gold_web(
    im: Image.Image,
    u: int,
    v: int,
    w: int,
    h: int,
    *,
    density: float,
    seed: int,
    avoid: tuple[int, int, int] | None = None,
) -> None:
    """Sparse continuous gold cracks. `avoid` = (cx, cy, radius) keeps eye clear."""
    rng = __import__("random").Random(seed)
    if w < 2 or h < 2:
        return

    def clamp_pt(x: int, y: int) -> tuple[int, int]:
        return (
            max(u, min(u + w - 1, x)),
            max(v, min(v + h - 1, y)),
        )

    def near_avoid(x: int, y: int) -> bool:
        if avoid is None:
            return False
        ax, ay, r = avoid
        return abs(x - ax) <= r and abs(y - ay) <= r

    spines = 1 if density < 0.8 else 2
    if w >= 8 and h >= 8 and density >= 0.95:
        spines = 3

    for i in range(spines):
        if h >= w:
            x = u + max(0, min(w - 1, (i + 1) * w // (spines + 1)))
            pts = [clamp_pt(x, v)]
            y = v
            while y < v + h - 1:
                step = max(2, h // 5)
                y = min(v + h - 1, y + step)
                x += rng.choice([-1, 0, 1])
                pts.append(clamp_pt(x, y))
            # skip segments that cross the eye
            clean = [p for p in pts if not near_avoid(p[0], p[1])]
            if len(clean) >= 2:
                _polyline(im, clean, u, v, w, h, seed=seed + i * 17)
            if len(pts) >= 3 and w > 3:
                mx, my = pts[len(pts) // 2]
                if not near_avoid(mx, my):
                    bx = mx + rng.choice([-2, -1, 1, 2])
                    _polyline(im, [clamp_pt(mx, my), clamp_pt(bx, my + rng.randint(1, 3))], u, v, w, h, seed=seed + 40 + i)
        else:
            y = v + max(0, min(h - 1, (i + 1) * h // (spines + 1)))
            pts = [clamp_pt(u, y)]
            x = u
            while x < u + w - 1:
                step = max(2, w // 5)
                x = min(u + w - 1, x + step)
                y += rng.choice([-1, 0, 1])
                pts.append(clamp_pt(x, y))
            clean = [p for p in pts if not near_avoid(p[0], p[1])]
            if len(clean) >= 2:
                _polyline(im, clean, u, v, w, h, seed=seed + i * 19)

    if w >= 6 and h >= 6 and density >= 0.7:
        a = clamp_pt(u + 1, v + 1)
        b = clamp_pt(u + w // 2, v + h // 2 + rng.randint(-1, 1))
        c = clamp_pt(u + w - 2, v + h - 2)
        pts = [p for p in (a, b, c) if not near_avoid(p[0], p[1])]
        if len(pts) >= 2:
            _polyline(im, pts, u, v, w, h, seed=seed + 99)

    # Rays toward eye ring — stop outside avoid radius
    if avoid is not None:
        ax, ay, r = avoid
        for cx, cy in ((u, v), (u + w - 1, v), (u, v + h - 1), (u + w - 1, v + h - 1)):
            # endpoint just outside the eye
            dx, dy = cx - ax, cy - ay
            dist = max(1, abs(dx) + abs(dy))
            stop = clamp_pt(ax + int(dx * (r + 1) / dist * (1 if dx or dy else 1)), ay + int(dy * (r + 1) / max(1, dist)))
            # better: walk from corner toward center, stop at radius
            pts = []
            steps = max(abs(cx - ax), abs(cy - ay), 1)
            for s in range(steps + 1):
                t = s / steps
                x = int(round(cx + (ax - cx) * t))
                y = int(round(cy + (ay - cy) * t))
                if abs(x - ax) <= r and abs(y - ay) <= r:
                    break
                pts.append((x, y))
            if len(pts) >= 2:
                _polyline(im, pts, u, v, w, h, seed=seed + 200)


def _paint_eye(im: Image.Image, u: int, v: int, w: int, h: int) -> None:
    """Φ-eye last — overwrites any veins underneath. Clear cyan gem with gold bezel."""
    cx, cy = u + w // 2, v + h // 2
    if w < 4 or h < 4:
        _put(im, cx, cy, CYAN_CORE, u, v, w, h)
        if w > 1:
            _put(im, cx - 1, cy, CYAN, u, v, w, h)
            _put(im, cx + 1, cy, CYAN, u, v, w, h)
        return
    # Socket
    for dy in range(-2, 3):
        for dx in range(-2, 3):
            if dx * dx + dy * dy <= 5:
                _put(im, cx + dx, cy + dy, ULTRA_DEEP, u, v, w, h)
    # Gold bezel (varied shades)
    bezel = [
        (-2, -1, GOLD_HI), (-2, 0, GOLD), (-2, 1, GOLD_LO),
        (2, -1, GOLD), (2, 0, GOLD_HI), (2, 1, GOLD),
        (-1, -2, GOLD_HI), (0, -2, GOLD), (1, -2, GOLD_LO),
        (-1, 2, GOLD), (0, 2, GOLD_HI), (1, 2, GOLD),
    ]
    for dx, dy, col in bezel:
        _put(im, cx + dx, cy + dy, col, u, v, w, h)
    # Iris — bright cyan, clearly visible
    iris = [
        (0, 0, CYAN_CORE),
        (-1, 0, CYAN), (1, 0, CYAN), (0, -1, CYAN), (0, 1, CYAN),
        (-1, -1, CYAN_GLOW), (1, -1, CYAN_GLOW), (-1, 1, CYAN_RING), (1, 1, CYAN_RING),
    ]
    for dx, dy, col in iris:
        _put(im, cx + dx, cy + dy, col, u, v, w, h)


def paint_face(
    im: Image.Image,
    box: tuple[int, int, int, int],
    *,
    face: str,
    part: str,
    accent: bool = False,
) -> None:
    u, v, w, h = box
    seed = hash((part, face, w, h)) & 0xFFFFFFFF
    _paint_glass_base(im, u, v, w, h, seed)

    density = {
        "head": 0.85,
        "body": 1.0,
        "right_arm": 0.75,
        "left_arm": 0.75,
        "right_leg": 0.7,
        "left_leg": 0.7,
    }.get(part, 0.7)
    if face in ("top", "bottom"):
        density *= 0.85

    avoid = None
    if accent and face == "front" and w >= 4 and h >= 4:
        avoid = (u + w // 2, v + h // 2, 2)

    _gold_web(im, u, v, w, h, density=density, seed=seed, avoid=avoid)

    # Eye LAST so veins never cover it
    if accent and face == "front":
        _paint_eye(im, u, v, w, h)
    # Separate eye-plate cube (4×3×1) — paint solid cyan gem
    if part == "head" and w == 4 and h == 3 and face == "front":
        for yy in range(v, v + h):
            for xx in range(u, u + w):
                if xx in (u, u + w - 1) or yy in (v, v + h - 1):
                    im.putpixel((xx, yy), GOLD)
                elif xx == u + w // 2 and yy == v + h // 2:
                    im.putpixel((xx, yy), CYAN_CORE)
                else:
                    im.putpixel((xx, yy), CYAN)


def build_geo() -> dict:
    bones = [{"name": "root", "pivot": [0, 0, 0]}]
    for name, meta in PARTS.items():
        bones.append(
            {
                "name": name,
                "parent": meta["parent"],
                "pivot": list(meta["pivot"]),
                "cubes": [
                    {
                        "origin": list(origin),
                        "size": list(size),
                        "uv": list(uv),
                    }
                    for origin, size, uv in meta["cubes"]
                ],
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
                    "visible_bounds_width": 2.5,
                    "visible_bounds_height": 2.8,
                    "visible_bounds_offset": [0, 1.2, 0],
                },
                "bones": bones,
            }
        ],
    }


def build_atlas() -> Image.Image:
    im = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))
    for name, meta in PARTS.items():
        for _origin, size, uv in meta["cubes"]:
            w, h, d = size
            u, v = uv
            faces = box_faces(u, v, w, h, d)
            accent = name == "head"
            for face, box in faces.items():
                paint_face(im, box, face=face, part=name, accent=accent and face == "front")
    # Hard opaque: no soft alpha on painted pixels
    px = im.load()
    for y in range(ATLAS):
        for x in range(ATLAS):
            r, g, b, a = px[x, y]
            if a > 0:
                px[x, y] = (r, g, b, 255)
    return im


def deg(rad: float) -> float:
    return round(math.degrees(rad), 2)


def vec3(x, y, z):
    return {"vector": [round(x, 2), round(y, 2), round(z, 2)]}


def rot_kf(items):
    return {str(round(t, 3)): vec3(x, y, z) for t, x, y, z in items}


def tri_wave(t: float, period: float) -> float:
    x = (t / period) % 1.0
    return 1.0 - 4.0 * abs(round(x - 0.25) - x + 0.25)


def build_animations() -> dict:
    # Iron golem walk math, milder amplitudes for shorter limbs
    walk = []
    for i in range(5):
        t = i * 0.25
        w = tri_wave(t, 13.0)
        leg = 1.1 * w  # ~63° peak vs iron 86°
        arm = (-0.15 + 1.0 * w)
        walk.append((t, leg, arm))

    attack = []
    for i in range(11):
        t = i * 0.1
        tick = t * 20.0
        w = tri_wave(tick, 10.0)
        attack.append((t, deg(-1.7 + 1.2 * w), 0, 0))  # smash down

    anims = {
        "animation.vitrified_golem.idle": {
            "loop": True,
            "animation_length": 2.0,
            "bones": {
                "body": {"rotation": rot_kf([(0, 0, 0, -1), (1, 0, 0, 1), (2, 0, 0, -1)])},
                "right_arm": {"rotation": rot_kf([(0, -8, 0, 4), (1, -12, 0, 4), (2, -8, 0, 4)])},
                "left_arm": {"rotation": rot_kf([(0, -8, 0, -4), (1, -4, 0, -4), (2, -8, 0, -4)])},
            },
        },
        "animation.vitrified_golem.walk": {
            "loop": True,
            "animation_length": 1.0,
            "bones": {
                "right_leg": {"rotation": rot_kf([(t, deg(-leg), 0, 0) for t, leg, _ in walk])},
                "left_leg": {"rotation": rot_kf([(t, deg(leg), 0, 0) for t, leg, _ in walk])},
                "right_arm": {"rotation": rot_kf([(t, deg(arm), 0, 0) for t, _, arm in walk])},
                "left_arm": {"rotation": rot_kf([(t, deg(-arm), 0, 0) for t, _, arm in walk])},
                "body": {"rotation": rot_kf([(0, 2, 0, 2), (0.5, 2, 0, -2), (1, 2, 0, 2)])},
            },
        },
        "animation.vitrified_golem.detect": {
            "animation_length": 0.8,
            "bones": {
                "head": {"rotation": rot_kf([(0, 0, 0, 0), (0.2, -8, 25, 0), (0.8, 0, 0, 0)])},
            },
        },
        "animation.vitrified_golem.attack_1": {
            "animation_length": 1.0,
            "bones": {
                "right_arm": {"rotation": rot_kf(attack)},
                "left_arm": {"rotation": rot_kf([(t, x * 0.3, 0, 0) for t, x, _, _ in attack])},
                "body": {"rotation": rot_kf([(0, 0, 0, 0), (0.35, -8, -6, 0), (1, 0, 0, 0)])},
            },
        },
        "animation.vitrified_golem.attack_2": {
            "animation_length": 1.0,
            "bones": {
                "left_arm": {"rotation": rot_kf(attack)},
                "right_arm": {"rotation": rot_kf([(t, x * 0.3, 0, 0) for t, x, _, _ in attack])},
                "body": {"rotation": rot_kf([(0, 0, 0, 0), (0.35, -8, 6, 0), (1, 0, 0, 0)])},
            },
        },
        "animation.vitrified_golem.rush": {
            "animation_length": 1.2,
            "bones": {
                "body": {"rotation": rot_kf([(0, 18, 0, 0), (0.6, 28, 0, 0), (1.2, 18, 0, 0)])},
                "head": {"rotation": rot_kf([(0, -12, 0, 0), (1.2, -12, 0, 0)])},
                "right_arm": {"rotation": rot_kf([(0, -70, 0, 0), (1.2, -70, 0, 0)])},
                "left_arm": {"rotation": rot_kf([(0, -70, 0, 0), (1.2, -70, 0, 0)])},
            },
        },
        "animation.vitrified_golem.special": {
            "animation_length": 2.5,
            "bones": {
                "body": {"rotation": rot_kf([(0, 0, 0, 0), (1.0, -6, 0, 0), (1.6, 8, 0, 0), (2.5, 0, 0, 0)])},
                "head": {"rotation": rot_kf([(0, -15, 0, 0), (1.2, -25, 0, 0), (2.5, -15, 0, 0)])},
                "right_arm": {"rotation": rot_kf([(0, -120, 0, 10), (1, -140, 0, 10), (2.5, -120, 0, 10)])},
                "left_arm": {"rotation": rot_kf([(0, -120, 0, -10), (1, -140, 0, -10), (2.5, -120, 0, -10)])},
            },
        },
        "animation.vitrified_golem.hurt": {
            "animation_length": 0.35,
            "bones": {
                "body": {"rotation": rot_kf([(0, 0, 0, 0), (0.1, -10, 0, 8), (0.35, 0, 0, 0)])},
            },
        },
        "animation.vitrified_golem.death": {
            "animation_length": 1.5,
            "bones": {
                "body": {"rotation": rot_kf([(0, 0, 0, 0), (0.7, 35, 0, 0), (1.5, 75, 0, 12)])},
                "head": {"rotation": rot_kf([(0, 0, 0, 0), (1.5, 20, 0, 0)])},
                "right_leg": {"rotation": rot_kf([(0, 0, 0, 0), (1.5, -20, 0, 0)])},
                "left_leg": {"rotation": rot_kf([(0, 0, 0, 0), (1.5, 18, 0, 0)])},
            },
        },
    }
    return {"format_version": "1.8.0", "animations": anims}


def write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2), encoding="utf-8")


def main() -> None:
    geo = build_geo()
    anims = build_animations()
    atlas = build_atlas()

    for p in (ASSETS / "geo/vitrified_golem.geo.json", ART / "vitrified_golem.geo.json"):
        write_json(p, geo)
    for p in (
        ASSETS / "animations/vitrified_golem.animation.json",
        ART / "vitrified_golem.animation.json",
    ):
        write_json(p, anims)

    tex = ASSETS / "textures/entity/vitrified_golem.png"
    tex.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(tex)
    ART.mkdir(parents=True, exist_ok=True)
    atlas.save(ART / "vitrified_golem.png")
    atlas.resize((ATLAS * 4, ATLAS * 4), Image.NEAREST).save(ART / "uv_guide_preview.png")

    print("Built from-scratch golem:")
    print("  height units: 34 (~2.125 blocks), feet at y=0")
    print("  arms: 4x16x4, legs: 4x14x4, body: 10x12x6")
    print("  ->", tex)


if __name__ == "__main__":
    main()
