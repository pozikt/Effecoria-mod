#!/usr/bin/env python3
"""Bake unique natural fauna geos (no Φ glyphs). Batch A biome + Batch B atlas refresh.

  python scripts/build_unique_fauna.py
"""
from __future__ import annotations

import random
from pathlib import Path

from PIL import Image, ImageDraw

from fauna_geo_bake import (
    ROOT,
    ASSETS,
    paint_fill,
    paint_eye,
    write_mob_assets,
    force_opaque,
    box_faces,
)

ART_ROOT = ROOT / "art"


# --- painters ---

def _fur(im, u, v, w, h, seed, base, mid, hi, edge, fleck=None):
    paint_fill(im, u, v, w, h, base, mid, hi, edge, seed, flecks=fleck)


def painter_set(palette: dict):
    def fur(im, u, v, w, h, seed):
        _fur(im, u, v, w, h, seed, palette["base"], palette["mid"], palette["hi"], palette["edge"], palette.get("fleck"))

    def belly(im, u, v, w, h, seed):
        _fur(im, u, v, w, h, seed, palette["belly"], palette["belly_mid"], palette["belly_hi"], palette["edge"])

    def bone(im, u, v, w, h, seed):
        _fur(im, u, v, w, h, seed, palette.get("bone", (180, 170, 140, 255)), palette.get("bone_mid", (200, 190, 160, 255)), (230, 220, 190, 255), palette["edge"])

    def crystal(im, u, v, w, h, seed):
        _fur(im, u, v, w, h, seed, palette.get("crystal", (60, 120, 180, 255)), palette.get("crystal_mid", (90, 160, 210, 255)), palette.get("crystal_hi", (160, 220, 245, 255)), palette.get("crystal_edge", (40, 80, 120, 255)))

    def bark(im, u, v, w, h, seed):
        _fur(im, u, v, w, h, seed, palette["base"], palette["mid"], palette["hi"], palette["edge"], palette.get("fleck"))

    def moss(im, u, v, w, h, seed):
        _fur(im, u, v, w, h, seed, palette.get("moss", (40, 90, 50, 255)), palette.get("moss_mid", (60, 120, 70, 255)), (90, 150, 90, 255), palette["edge"])

    def membrane(im, u, v, w, h, seed):
        _fur(im, u, v, w, h, seed, palette.get("mem", (90, 100, 140, 255)), palette.get("mem_mid", (120, 130, 170, 255)), palette.get("mem_hi", (170, 180, 210, 255)), palette["edge"])

    def head(im, u, v, w, h, seed):
        fur(im, u, v, w, h, seed)

    def face(im, u, v, w, h, seed):
        fur(im, u, v, w, h, seed)

    return {
        "fur": fur,
        "belly": belly,
        "bone": bone,
        "crystal": crystal,
        "bark": bark,
        "moss": moss,
        "membrane": membrane,
        "head": head,
        "face": face,
    }


def bake_rotfang_mink() -> None:
    # Long weasel: body ~12 long, legs short, nose cube, tail segments
    parts = {
        "body": {
            "parent": "root",
            "pivot": (0, 4, 0),
            "cubes": [((-3, 3, -5), (6, 4, 10), (0, 0))],
        },
        "head": {
            "parent": "body",
            "pivot": (0, 5, -5),
            "cubes": [
                ((-2.5, 3.5, -9), (5, 4, 4), (32, 0)),
                ((-1, 4.5, -11), (2, 2, 2), (50, 0)),  # snout
                ((-2.5, 7.5, -8), (1, 2, 1), (0, 28)),  # ear L
                ((1.5, 7.5, -8), (1, 2, 1), (4, 28)),
            ],
        },
        "tail": {
            "parent": "body",
            "pivot": (0, 5, 5),
            "cubes": [
                ((-1.5, 3.5, 5), (3, 3, 4), (0, 16)),
                ((-1, 4, 9), (2, 2, 4), (14, 16)),
            ],
        },
        "left_leg": {
            "parent": "body",
            "pivot": (2, 3, -3),
            "cubes": [((1, 0, -4), (2, 3, 2), (32, 16))],
        },
        "right_leg": {
            "parent": "body",
            "pivot": (-2, 3, -3),
            "cubes": [((-3, 0, -4), (2, 3, 2), (40, 16))],
        },
        "left_arm": {
            "parent": "body",
            "pivot": (2, 3, 2),
            "cubes": [((1, 0, 1), (2, 3, 2), (48, 16))],
        },
        "right_arm": {
            "parent": "body",
            "pivot": (-2, 3, 2),
            "cubes": [((-3, 0, 1), (2, 3, 2), (56, 16))],
        },
    }
    kinds = {(b, i): "fur" for b, s in parts.items() for i, _ in enumerate(s["cubes"])}
    kinds[("head", 0)] = "face"
    kinds[("head", 1)] = "bone"
    pal = {
        "base": (28, 18, 32, 255),
        "mid": (48, 28, 55, 255),
        "hi": (70, 45, 80, 255),
        "edge": (12, 8, 16, 255),
        "fleck": (90, 40, 120, 255),
        "belly": (60, 50, 55, 255),
        "belly_mid": (80, 70, 75, 255),
        "belly_hi": (110, 100, 105, 255),
        "bone": (160, 150, 130, 255),
        "bone_mid": (190, 180, 160, 255),
    }
    write_mob_assets(
        "rotfang_mink",
        ART_ROOT / "scar" / "rotfang_mink",
        parts,
        kinds,
        painter_set(pal),
        atlas=64,
        anim_mode="walk",
        limbs=("left_leg", "right_leg", "left_arm", "right_arm"),
        arms=(),
        bounds=(2.2, 1.2),
    )
    (ART_ROOT / "scar" / "rotfang_mink" / "SEGMENT_LOCK.md").write_text(
        "# Rotfang Mink\nNatural weasel: snout cube, ear stubs, segmented tail. No Φ.\nPalette: charcoal / rotten purple / bone.\n",
        encoding="utf-8",
    )


def bake_phi_lemur() -> None:
    parts = {
        "body": {
            "parent": "root",
            "pivot": (0, 5, 0),
            "cubes": [((-3, 3, -3), (6, 5, 6), (0, 0))],
        },
        "head": {
            "parent": "body",
            "pivot": (0, 8, -2),
            "cubes": [
                ((-3, 7, -6), (6, 5, 5), (24, 0)),
                ((-4, 11, -5), (2, 3, 1), (0, 32)),
                ((2, 11, -5), (2, 3, 1), (8, 32)),
            ],
        },
        "tail": {
            "parent": "body",
            "pivot": (0, 5, 3),
            "cubes": [
                ((-1.5, 4, 3), (3, 3, 5), (0, 16)),
                ((-1, 5, 8), (2, 2, 5), (16, 16)),
            ],
        },
        "left_leg": {"parent": "body", "pivot": (2, 3, 1), "cubes": [((1, 0, 0), (2, 3, 2), (32, 16))]},
        "right_leg": {"parent": "body", "pivot": (-2, 3, 1), "cubes": [((-3, 0, 0), (2, 3, 2), (40, 16))]},
        "left_arm": {"parent": "body", "pivot": (3, 6, -1), "cubes": [((3, 2, -2), (2, 4, 2), (48, 16))]},
        "right_arm": {"parent": "body", "pivot": (-3, 6, -1), "cubes": [((-5, 2, -2), (2, 4, 2), (56, 16))]},
    }

    kinds = {(b, i): "fur" for b, s in parts.items() for i, _ in enumerate(s["cubes"])}
    kinds[("body", 0)] = "belly"
    kinds[("head", 0)] = "face"
    pal = {
        "base": (45, 40, 90, 255),
        "mid": (70, 60, 120, 255),
        "hi": (100, 90, 150, 255),
        "edge": (25, 20, 50, 255),
        "belly": (200, 185, 160, 255),
        "belly_mid": (220, 205, 180, 255),
        "belly_hi": (240, 230, 210, 255),
    }
    write_mob_assets(
        "phi_lemur",
        ART_ROOT / "canopy" / "phi_lemur",
        parts,
        kinds,
        painter_set(pal),
        atlas=64,
        limbs=("left_leg", "right_leg"),
        arms=("left_arm", "right_arm"),
        bounds=(2.0, 1.5),
    )
    (ART_ROOT / "canopy" / "phi_lemur" / "SEGMENT_LOCK.md").write_text(
        "# Phi Lemur\nCanopy lemur: big ears, cream belly, long tail. No Φ.\n",
        encoding="utf-8",
    )


def bake_phi_ent() -> None:
    parts = {
        "body": {
            "parent": "root",
            "pivot": (0, 16, 0),
            "cubes": [
                ((-5, 10, -3), (10, 12, 6), (0, 0)),
                ((-6, 20, -4), (12, 3, 8), (32, 0)),  # shoulder canopy
            ],
        },
        "head": {
            "parent": "body",
            "pivot": (0, 24, 0),
            "cubes": [
                ((-4, 23, -4), (8, 7, 8), (0, 24)),
                ((-5, 29, -1), (2, 4, 2), (32, 24)),  # branch L
                ((3, 29, -1), (2, 4, 2), (40, 24)),
                ((-1, 30, 1), (2, 3, 2), (48, 24)),
            ],
        },
        "right_arm": {
            "parent": "body",
            "pivot": (-6, 20, 0),
            "cubes": [((-10, 6, -2), (4, 14, 4), (0, 48))],
        },
        "left_arm": {
            "parent": "body",
            "pivot": (6, 20, 0),
            "cubes": [((6, 6, -2), (4, 14, 4), (16, 48))],
        },
        "right_leg": {
            "parent": "body",
            "pivot": (-2.5, 10, 0),
            "cubes": [((-5, 0, -2), (4, 10, 4), (32, 48))],
        },
        "left_leg": {
            "parent": "body",
            "pivot": (2.5, 10, 0),
            "cubes": [((1, 0, -2), (4, 10, 4), (48, 48))],
        },
    }
    kinds = {
        ("body", 0): "bark",
        ("body", 1): "moss",
        ("head", 0): "face",
        ("head", 1): "moss",
        ("head", 2): "moss",
        ("head", 3): "moss",
        ("right_arm", 0): "bark",
        ("left_arm", 0): "bark",
        ("right_leg", 0): "bark",
        ("left_leg", 0): "bark",
    }
    pal = {
        "base": (55, 38, 22, 255),
        "mid": (80, 55, 30, 255),
        "hi": (110, 80, 45, 255),
        "edge": (30, 20, 12, 255),
        "fleck": (40, 100, 55, 255),
        "moss": (35, 85, 45, 255),
        "moss_mid": (55, 115, 60, 255),
        "belly": (70, 90, 50, 255),
        "belly_mid": (90, 110, 70, 255),
        "belly_hi": (120, 140, 90, 255),
    }
    write_mob_assets(
        "phi_ent",
        ART_ROOT / "canopy" / "phi_ent",
        parts,
        kinds,
        painter_set(pal),
        atlas=128,
        limbs=("left_leg", "right_leg"),
        arms=("left_arm", "right_arm"),
        bounds=(2.8, 2.6),
    )
    (ART_ROOT / "canopy" / "phi_ent" / "SEGMENT_LOCK.md").write_text(
        "# Phi Ent\nTreant: bark body, moss shoulder, branch antlers. No face Φ.\n",
        encoding="utf-8",
    )


def bake_glass_worm() -> None:
    parts = {
        "body": {
            "parent": "root",
            "pivot": (0, 2, 0),
            "cubes": [
                ((-2, 1, -4), (4, 3, 4), (0, 0)),
                ((-2, 1, 0), (4, 3, 4), (16, 0)),
                ((-1.5, 1.5, 4), (3, 2, 3), (32, 0)),
            ],
        },
        "head": {
            "parent": "body",
            "pivot": (0, 2.5, -4),
            "cubes": [((-2, 1, -7), (4, 3, 3), (0, 12))],
        },
        "left_leg": {"parent": "body", "pivot": (2, 1, -1), "cubes": [((1.5, 0, -2), (1, 1, 1), (48, 0))]},
        "right_leg": {"parent": "body", "pivot": (-2, 1, -1), "cubes": [((-2.5, 0, -2), (1, 1, 1), (52, 0))]},
    }
    kinds = {(b, i): "crystal" for b, s in parts.items() for i, _ in enumerate(s["cubes"])}
    kinds[("head", 0)] = "face"
    pal = {
        "base": (20, 40, 70, 255),
        "mid": (40, 70, 110, 255),
        "hi": (70, 110, 160, 255),
        "edge": (10, 20, 40, 255),
        "crystal": (50, 100, 160, 255),
        "crystal_mid": (80, 140, 200, 255),
        "crystal_hi": (140, 200, 240, 255),
        "crystal_edge": (200, 170, 60, 255),
        "belly": (30, 50, 80, 255),
        "belly_mid": (50, 80, 110, 255),
        "belly_hi": (80, 120, 160, 255),
    }
    art = ART_ROOT / "vitrified_wastes" / "glass_worm"
    write_mob_assets(
        "glass_worm",
        art,
        parts,
        kinds,
        painter_set(pal),
        atlas=64,
        anim_mode="crawl",
        limbs=("left_leg", "right_leg"),
        arms=(),
        bounds=(1.8, 0.8),
    )
    (art / "SEGMENT_LOCK.md").write_text(
        "# Glass Worm\nThin segmented glass body + mandibles. No Φ.\nPalette: ultramarine glass / gold cracks.\n",
        encoding="utf-8",
    )


def bake_omega_worm() -> None:
    parts = {
        "body": {
            "parent": "root",
            "pivot": (0, 2, 0),
            "cubes": [
                ((-2.5, 1, -5), (5, 3, 5), (0, 0)),
                ((-2.5, 1, 0), (5, 3, 5), (20, 0)),
                ((-2, 1.5, 5), (4, 2, 4), (40, 0)),
            ],
        },
        "head": {
            "parent": "body",
            "pivot": (0, 2.5, -5),
            "cubes": [
                ((-2.5, 1, -8), (5, 3, 3), (0, 16)),
                ((-3, 1.5, -9), (1, 1, 2), (24, 16)),
                ((2, 1.5, -9), (1, 1, 2), (30, 16)),
            ],
        },
        "left_leg": {"parent": "body", "pivot": (2.5, 1, 0), "cubes": [((2, 0, -1), (1, 1, 1), (56, 0))]},
        "right_leg": {"parent": "body", "pivot": (-2.5, 1, 0), "cubes": [((-3, 0, -1), (1, 1, 1), (60, 0))]},
    }
    kinds = {(b, i): "fur" for b, s in parts.items() for i, _ in enumerate(s["cubes"])}
    kinds[("head", 0)] = "face"
    kinds[("head", 1)] = "bone"
    kinds[("head", 2)] = "bone"
    pal = {
        "base": (25, 12, 30, 255),
        "mid": (45, 20, 55, 255),
        "hi": (70, 35, 85, 255),
        "edge": (10, 5, 14, 255),
        "fleck": (100, 40, 130, 255),
        "belly": (40, 25, 45, 255),
        "belly_mid": (60, 35, 70, 255),
        "belly_hi": (90, 50, 100, 255),
        "bone": (90, 70, 100, 255),
        "bone_mid": (120, 90, 130, 255),
    }
    art = ART_ROOT / "scar" / "omega_worm"
    write_mob_assets(
        "omega_worm",
        art,
        parts,
        kinds,
        painter_set(pal),
        atlas=64,
        anim_mode="crawl",
        limbs=("left_leg", "right_leg"),
        arms=(),
        bounds=(2.0, 0.9),
    )
    (art / "SEGMENT_LOCK.md").write_text(
        "# Omega Worm\nSegmented chitin worm + jaws. No Φ.\nPalette: dark chitin / violet seams.\n",
        encoding="utf-8",
    )


def bake_wailer_bat() -> None:
    parts = {
        "body": {
            "parent": "root",
            "pivot": (0, 8, 0),
            "cubes": [((-2, 6, -2), (4, 5, 4), (0, 0))],
        },
        "head": {
            "parent": "body",
            "pivot": (0, 11, -1),
            "cubes": [
                ((-2.5, 10, -4), (5, 4, 4), (16, 0)),
                ((-3, 13, -3), (1, 2, 1), (40, 0)),
                ((2, 13, -3), (1, 2, 1), (44, 0)),
            ],
        },
        "left_wing": {
            "parent": "body",
            "pivot": (2, 10, 0),
            "cubes": [((2, 8, -1), (8, 1, 5), (0, 16))],
        },
        "right_wing": {
            "parent": "body",
            "pivot": (-2, 10, 0),
            "cubes": [((-10, 8, -1), (8, 1, 5), (0, 24))],
        },
        "left_leg": {"parent": "body", "pivot": (1, 6, 1), "cubes": [((0.5, 4, 0), (1, 2, 1), (48, 8))]},
        "right_leg": {"parent": "body", "pivot": (-1, 6, 1), "cubes": [((-1.5, 4, 0), (1, 2, 1), (52, 8))]},
    }
    kinds = {(b, i): "fur" for b, s in parts.items() for i, _ in enumerate(s["cubes"])}
    kinds[("left_wing", 0)] = "membrane"
    kinds[("right_wing", 0)] = "membrane"
    kinds[("head", 0)] = "face"
    pal = {
        "base": (30, 28, 50, 255),
        "mid": (50, 45, 75, 255),
        "hi": (75, 70, 105, 255),
        "edge": (15, 12, 25, 255),
        "mem": (100, 110, 150, 255),
        "mem_mid": (130, 140, 180, 255),
        "mem_hi": (180, 190, 220, 255),
        "belly": (50, 48, 70, 255),
        "belly_mid": (70, 65, 95, 255),
        "belly_hi": (100, 95, 130, 255),
    }
    art = ART_ROOT / "canopy" / "wailer_bat"
    write_mob_assets(
        "wailer_bat",
        art,
        parts,
        kinds,
        painter_set(pal),
        atlas=64,
        anim_mode="fly",
        limbs=("left_leg", "right_leg"),
        arms=(),
        fly_bones=("left_wing", "right_wing"),
        bounds=(2.5, 1.5),
    )
    (art / "SEGMENT_LOCK.md").write_text(
        "# Wailer Bat\nBody cube, membrane wing planes, ear stubs. No Φ.\nPalette: night blue / pale membrane.\n",
        encoding="utf-8",
    )


def bake_omega_shade() -> None:
    parts = {
        "body": {
            "parent": "root",
            "pivot": (0, 10, 0),
            "cubes": [((-2, 6, -2), (4, 8, 3), (0, 0))],
        },
        "head": {
            "parent": "body",
            "pivot": (0, 14, 0),
            "cubes": [((-2.5, 13, -2.5), (5, 5, 5), (14, 0))],
        },
        "cloak_l": {
            "parent": "body",
            "pivot": (2, 12, 0),
            "cubes": [((2, 4, -1), (3, 10, 1), (0, 16))],
        },
        "cloak_r": {
            "parent": "body",
            "pivot": (-2, 12, 0),
            "cubes": [((-5, 4, -1), (3, 10, 1), (8, 16))],
        },
        "left_arm": {"parent": "body", "pivot": (2, 12, 0), "cubes": [((2, 7, -1), (2, 6, 2), (16, 32))]},
        "right_arm": {"parent": "body", "pivot": (-2, 12, 0), "cubes": [((-4, 7, -1), (2, 6, 2), (24, 32))]},
        "left_leg": {"parent": "body", "pivot": (1, 6, 0), "cubes": [((0, 2, -1), (2, 4, 2), (32, 32))]},
        "right_leg": {"parent": "body", "pivot": (-1, 6, 0), "cubes": [((-2, 2, -1), (2, 4, 2), (40, 32))]},
    }

    kinds = {(b, i): "fur" for b, s in parts.items() for i, _ in enumerate(s["cubes"])}
    kinds[("head", 0)] = "face"
    kinds[("cloak_l", 0)] = "membrane"
    kinds[("cloak_r", 0)] = "membrane"
    pal = {
        "base": (12, 8, 18, 255),
        "mid": (28, 16, 40, 255),
        "hi": (50, 30, 70, 255),
        "edge": (5, 3, 8, 255),
        "fleck": (80, 40, 100, 255),
        "mem": (35, 20, 50, 255),
        "mem_mid": (55, 30, 75, 255),
        "mem_hi": (90, 50, 110, 255),
        "belly": (20, 12, 28, 255),
        "belly_mid": (35, 20, 45, 255),
        "belly_hi": (55, 35, 70, 255),
    }
    art = ART_ROOT / "scar" / "omega_shade"
    write_mob_assets(
        "omega_shade",
        art,
        parts,
        kinds,
        painter_set(pal),
        atlas=64,
        anim_mode="fly",
        limbs=("left_leg", "right_leg"),
        arms=("left_arm", "right_arm"),
        fly_bones=("cloak_l", "cloak_r"),
        bounds=(2.0, 2.0),
    )
    (art / "SEGMENT_LOCK.md").write_text(
        "# Omega Shade\nThin flying silhouette, ragged cloak cubes. No Φ glyph face.\n"
        "Palette: near-black / dull Ω-purple accents.\n",
        encoding="utf-8",
    )


def refresh_batch_b_no_phi() -> None:
    """Repaint existing GeckoLib atlases: remove bright glyph-like centers, naturalize."""
    targets = ["phi_larva", "crystal_crab", "eidos", "essence_wyvern"]
    for name in targets:
        path = ASSETS / "textures/entity" / f"{name}.png"
        if not path.exists():
            print("skip missing", name)
            continue
        im = Image.open(path).convert("RGBA")
        px = im.load()
        w, h = im.size
        # Neutralize near-white / cyan “glyph” pixels into surrounding material
        for y in range(h):
            for x in range(w):
                r, g, b, a = px[x, y]
                if a == 0:
                    continue
                bright = r + g + b > 600
                cyanish = b > 180 and g > 140 and r < 120
                if bright or cyanish:
                    # soft animal/crystal tone instead of logo glow
                    if name == "eidos":
                        px[x, y] = (220, 200, 120, 255)
                    elif name == "essence_wyvern":
                        px[x, y] = (180, 140, 60, 255)
                    elif name == "crystal_crab":
                        px[x, y] = (100, 140, 200, 255)
                    else:
                        px[x, y] = (80, 140, 200, 255)
        force_opaque(im)
        im.save(path)
        art = ART_ROOT / name
        art.mkdir(parents=True, exist_ok=True)
        im.save(art / f"{name}_atlas_natural.png")
        print("refreshed atlas", name)


def main() -> None:
    print("=== Batch A1 ===")
    bake_rotfang_mink()
    bake_phi_lemur()
    bake_phi_ent()
    print("=== Batch A2 ===")
    bake_glass_worm()
    bake_omega_worm()
    bake_wailer_bat()
    bake_omega_shade()
    print("=== Batch B ===")
    refresh_batch_b_no_phi()
    print("done")


if __name__ == "__main__":
    main()
