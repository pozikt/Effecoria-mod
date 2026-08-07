#!/usr/bin/env python3
"""
Rebuild vitrified golem as a scaled iron golem (geo + iron-style anims + recolored 128px atlas).

Run from repo root:
  python scripts/build_vitrified_golem_iron_base.py
"""

from __future__ import annotations

import glob
import json
import math
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/effecoria"
ART = ROOT / "art/vitrified_wastes/golem"

# ~88% iron golem — reads as “compact iron golem” at entity height ~2.45 blocks
SCALE = 0.88
FOOT_Y_JAVA = 8.0  # vanilla iron golem mesh — feet at y=8 in model space

BONES_SPEC = [
    # name, parent, pose (px,py,pz), [(box xyz, box size, uv), ...]
    (
        "head",
        "root",
        (0.0, -7.0, -2.0),
        [
            ((-4.0, -12.0, -5.5), (8.0, 10.0, 8.0), (0, 0)),
            ((-1.0, -5.0, -7.5), (2.0, 4.0, 2.0), (24, 0)),
        ],
    ),
    (
        "body",
        "root",
        (0.0, -7.0, 0.0),
        [
            ((-9.0, -2.0, -6.0), (18.0, 12.0, 11.0), (0, 40)),
            ((-4.5, 10.0, -3.0), (9.0, 5.0, 6.0), (0, 70)),
        ],
    ),
    (
        "right_arm",
        "root",
        (0.0, -7.0, 0.0),
        [((-13.0, -2.5, -3.0), (4.0, 30.0, 6.0), (60, 21))],
    ),
    (
        "left_arm",
        "root",
        (0.0, -7.0, 0.0),
        [((9.0, -2.5, -3.0), (4.0, 30.0, 6.0), (60, 58))],
    ),
    (
        "right_leg",
        "root",
        (-4.0, 11.0, 0.0),
        [((-3.5, -3.0, -3.0), (6.0, 16.0, 5.0), (37, 0))],
    ),
    (
        "left_leg",
        "root",
        (5.0, 11.0, 0.0),
        [((-3.5, -3.0, -3.0), (6.0, 16.0, 5.0), (60, 0))],
    ),
]


def sy(y: float) -> float:
    return (y - FOOT_Y_JAVA) * SCALE


def sx(v: float) -> float:
    return v * SCALE


def cube_from_java(_pose, box_origin, box_size, uv):
    bx, by, bz = box_origin
    return {
        "origin": [round(sx(bx), 3), round(sx(by), 3), round(sx(bz), 3)],
        "size": [round(sx(box_size[0]), 3), round(sx(box_size[1]), 3), round(sx(box_size[2]), 3)],
        "uv": [uv[0], uv[1]],
    }


def bone_pivot(pose):
    px, py, pz = pose
    return [round(sx(px), 3), round(sy(py), 3), round(sx(pz), 3)]


def build_geo() -> dict:
    bones = [{"name": "root", "pivot": [0, 0, 0]}]
    for name, parent, pose, cubes in BONES_SPEC:
        bones.append(
            {
                "name": name,
                "parent": parent,
                "pivot": bone_pivot(pose),
                "cubes": [cube_from_java(pose, bo, bs, uv) for bo, bs, uv in cubes],
            }
        )
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": "geometry.vitrified_golem",
                    "texture_width": 128,
                    "texture_height": 128,
                    "visible_bounds_width": 2.5,
                    "visible_bounds_height": 3.0,
                    "visible_bounds_offset": [0, 1.25, 0],
                },
                "bones": bones,
            }
        ],
    }


def tri_wave(t: float, period: float) -> float:
    """Minecraft Mth.triangleWave(t, period) — 1.21 iron golem limb phase."""
    x = (t / period) % 1.0
    return 1.0 - 4.0 * abs(round(x - 0.25) - x + 0.25)


def deg(rad: float) -> float:
    return round(math.degrees(rad), 2)


def vec3(x, y, z):
    return {"vector": [round(x, 2), round(y, 2), round(z, 2)]}


def rot_keyframes(times_values):
    out = {}
    for item in times_values:
        t = item[0]
        out[str(round(t, 3))] = vec3(item[1], item[2], item[3])
    return out


def build_animations() -> dict:
    walk_len = 1.0
    walk_steps = 13.0
    walk_kf = []
    for i in range(5):
        t = i * (walk_len / 4.0)
        w = tri_wave(t, 1.0 / walk_steps) if walk_steps else 0
        amount = 1.0
        leg = 1.5 * w * amount
        arm = (-0.2 + 1.5 * w) * amount
        walk_kf.append((t, leg, arm))

    leg_r = []
    leg_l = []
    arm_r = []
    arm_l = []
    for t in [0.0, 0.25, 0.5, 0.75, 1.0]:
        w = tri_wave(t, walk_steps)
        leg = 1.5 * w
        arm = -0.2 + 1.5 * w
        leg_r.append((t, deg(-leg), 0, 0))
        leg_l.append((t, deg(leg), 0, 0))
        arm_r.append((t, deg(arm), 0, 0))
        arm_l.append((t, deg(-arm), 0, 0))

    attack_len = 1.0
    attack_kf = []
    for i in range(11):
        t = i * (attack_len / 10.0)
        tick = t * 20.0
        w = tri_wave(tick, 10.0)
        x = -2.0 + 1.5 * w
        attack_kf.append((t, deg(x), 0, 0))

    anims = {
        "animation.vitrified_golem.idle": {
            "loop": True,
            "animation_length": 2.0,
            "bones": {
                "right_arm": {"rotation": rot_keyframes([(0, deg(-0.2), 0, 0), (1, deg(-0.35), 0, 0), (2, deg(-0.2), 0, 0)])},
                "left_arm": {"rotation": rot_keyframes([(0, deg(-0.2), 0, 0), (1, deg(-0.05), 0, 0), (2, deg(-0.2), 0, 0)])},
            },
        },
        "animation.vitrified_golem.walk": {
            "loop": True,
            "animation_length": walk_len,
            "bones": {
                "right_leg": {"rotation": rot_keyframes(leg_r)},
                "left_leg": {"rotation": rot_keyframes(leg_l)},
                "right_arm": {"rotation": rot_keyframes(arm_r)},
                "left_arm": {"rotation": rot_keyframes(arm_l)},
            },
        },
        "animation.vitrified_golem.detect": {
            "animation_length": 0.8,
            "bones": {
                "head": {
                    "rotation": rot_keyframes([(0, 0, 0, 0), (0.15, deg(-0.15), deg(0.4), 0), (0.8, 0, 0, 0)])
                },
                "body": {"rotation": rot_keyframes([(0, 0, 0, 0), (0.2, deg(-0.08), 0, 0), (0.8, 0, 0, 0)])},
            },
        },
        "animation.vitrified_golem.attack_1": {
            "animation_length": attack_len,
            "bones": {
                "right_arm": {"rotation": rot_keyframes(attack_kf)},
                "left_arm": {"rotation": rot_keyframes([(t, rx * 0.35, 0, 0) for t, rx, _, _ in attack_kf])},
                "body": {"rotation": rot_keyframes([(0, 0, 0, 0), (0.35, deg(-0.15), deg(-0.12), 0), (1, 0, 0, 0)])},
            },
        },
        "animation.vitrified_golem.attack_2": {
            "animation_length": attack_len,
            "bones": {
                "left_arm": {"rotation": rot_keyframes(attack_kf)},
                "right_arm": {"rotation": rot_keyframes([(t, rx * 0.35, 0, 0) for t, rx, _, _ in attack_kf])},
                "body": {"rotation": rot_keyframes([(0, 0, 0, 0), (0.35, deg(-0.15), deg(0.12), 0), (1, 0, 0, 0)])},
            },
        },
        "animation.vitrified_golem.rush": {
            "animation_length": 1.2,
            "bones": {
                "body": {"rotation": rot_keyframes([(0, deg(0.35), 0, 0), (0.6, deg(0.55), 0, 0), (1.2, deg(0.35), 0, 0)])},
                "head": {"rotation": rot_keyframes([(0, deg(-0.25), 0, 0), (1.2, deg(-0.25), 0, 0)])},
                "right_arm": {"rotation": rot_keyframes([(0, deg(-1.2), 0, 0), (1.2, deg(-1.2), 0, 0)])},
                "left_arm": {"rotation": rot_keyframes([(0, deg(-1.2), 0, 0), (1.2, deg(-1.2), 0, 0)])},
            },
        },
        "animation.vitrified_golem.special": {
            "animation_length": 2.5,
            "bones": {
                "body": {
                    "rotation": rot_keyframes(
                        [(0, 0, 0, 0), (0.8, deg(-0.08), 0, 0), (1.6, deg(0.12), 0, 0), (2.5, 0, 0, 0)]
                    )
                },
                "head": {"rotation": rot_keyframes([(0, deg(-0.35), 0, 0), (1.2, deg(-0.5), 0, 0), (2.5, deg(-0.35), 0, 0)])},
                "right_arm": {"rotation": rot_keyframes([(0, deg(-2.2), 0, 0), (1, deg(-2.6), 0, 0), (2.5, deg(-2.2), 0, 0)])},
                "left_arm": {"rotation": rot_keyframes([(0, deg(-2.2), 0, 0), (1, deg(-2.6), 0, 0), (2.5, deg(-2.2), 0, 0)])},
            },
        },
        "animation.vitrified_golem.hurt": {
            "animation_length": 0.35,
            "bones": {
                "body": {
                    "rotation": rot_keyframes([(0, 0, 0, 0), (0.1, deg(-0.2), 0, deg(0.15)), (0.35, 0, 0, 0)])
                },
            },
        },
        "animation.vitrified_golem.death": {
            "animation_length": 1.5,
            "bones": {
                "body": {"rotation": rot_keyframes([(0, 0, 0, 0), (0.6, deg(0.5), 0, 0), (1.5, deg(1.2), 0, deg(0.25))])},
                "head": {"rotation": rot_keyframes([(0, 0, 0, 0), (1.5, deg(0.35), 0, 0)])},
                "right_leg": {"rotation": rot_keyframes([(0, 0, 0, 0), (1.5, deg(-0.4), 0, 0)])},
                "left_leg": {"rotation": rot_keyframes([(0, 0, 0, 0), (1.5, deg(0.35), 0, 0)])},
            },
        },
    }
    return {"format_version": "1.8.0", "animations": anims}


def find_iron_texture() -> bytes:
    jars = glob.glob(str(ROOT / "build/moddev/artifacts/*extra*.jar"))
    if not jars:
        raise SystemExit("Run gradlew compileJava once so Minecraft extra jar exists.")
    path = "assets/minecraft/textures/entity/iron_golem/iron_golem.png"
    with zipfile.ZipFile(jars[0]) as z:
        return z.read(path)


def recolor_iron_to_vitrified(src: Image.Image) -> Image.Image:
    """Iron golem atlas → dark vitrified glass; keep vine gold; cyan eyes."""
    im = src.convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a < 8:
                px[x, y] = (0, 0, 0, 0)
                continue
            # eyes / nose highlight (bright desaturated on iron face)
            if r > 210 and g > 210 and b > 210:
                px[x, y] = (200, 245, 255, 255)
                continue
            if g > r + 25 and g > b + 10 and r > 60:  # vines
                px[x, y] = (min(255, r + 20), min(255, g + 10), max(40, b // 2), 255)
                continue
            if r > 150 and g > 80 and b < 80:  # rust → gold fleck
                px[x, y] = (min(255, r), min(255, g + 30), 80, 255)
                continue
            lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
            # body metal → indigo glass
            nr = int(max(8, min(55, lum * 0.22 + 10)))
            ng = int(max(10, min(65, lum * 0.28 + 12)))
            nb = int(max(18, min(95, lum * 0.45 + 22)))
            px[x, y] = (nr, ng, nb, 255)
    return im


def write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2), encoding="utf-8")


def main() -> None:
    geo = build_geo()
    anims = build_animations()
    geo_paths = [
        ASSETS / "geo/vitrified_golem.geo.json",
        ART / "vitrified_golem.geo.json",
    ]
    anim_paths = [
        ASSETS / "animations/vitrified_golem.animation.json",
        ART / "vitrified_golem.animation.json",
    ]
    for p in geo_paths:
        write_json(p, geo)
    for p in anim_paths:
        write_json(p, anims)

    iron_bytes = find_iron_texture()
    iron = Image.open(__import__("io").BytesIO(iron_bytes))
    tex = recolor_iron_to_vitrified(iron)
    tex_path = ASSETS / "textures/entity/vitrified_golem.png"
    tex_path.parent.mkdir(parents=True, exist_ok=True)
    tex.save(tex_path)
    ART.mkdir(parents=True, exist_ok=True)
    tex.save(ART / "vitrified_golem.png")

    print("Wrote geo + animations (iron golem @ scale", SCALE, ")")
    print("Texture:", tex_path)


if __name__ == "__main__":
    main()
