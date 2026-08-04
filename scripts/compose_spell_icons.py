#!/usr/bin/env python3
"""Compose AI/reference pixel art into 64x64 circular spell icons."""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw

from elemental_icon_prompts import ELEMENTAL_PROMPTS
from organic_icon_prompts import ORGANIC_PROMPTS
from spatial_icon_prompts import SPATIAL_PROMPTS

ROOT = Path(__file__).resolve().parents[1]
SRC_ASSETS = Path(r"C:\Users\2005t\.cursor\projects\c-Users-2005t-Effecoria-mod\assets")
HQ_DIR = ROOT / "tmp_icon_hq"
OUT_DIR = ROOT / "src/main/resources/assets/effecoria/textures/gui/sprites/spells"
OUT = 64

# Non-school-batch AI overlays (school frame)
EXAMPLE_SCHOOL = {
    "festering_wound": "corruption",
    "cliff_urge": "mental",
    "raise_skeleton": "necromancy",
    "psi_ward": "common",
    "psi_adrenaline": "common",
    "phi_glow": "common",
    "psi_charge": "common",
    "psi_link": "common",
}

# School batches: hub frame color by school
for _sid in ELEMENTAL_PROMPTS:
    EXAMPLE_SCHOOL[_sid] = "elemental"
for _sid in SPATIAL_PROMPTS:
    EXAMPLE_SCHOOL[_sid] = "spatial"
for _sid in ORGANIC_PROMPTS:
    EXAMPLE_SCHOOL[_sid] = "organic"

FRAME_RGB = {
    "spatial": (240, 240, 245),
    "corruption": (50, 120, 45),
    "mental": (80, 140, 255),
    "elemental": (255, 150, 40),
    "necromancy": (230, 230, 220),
    "organic": (70, 200, 80),
    "common": (70, 220, 230),
}


def circle_mask(size: int) -> Image.Image:
    m = Image.new("L", (size, size), 0)
    ImageDraw.Draw(m).ellipse((1, 1, size - 2, size - 2), fill=255)
    return m


def to_pixel_circle(src: Path, school: str) -> Image.Image:
    raw = Image.open(src).convert("RGBA")
    w, h = raw.size
    side = min(w, h)
    left = (w - side) // 2
    top = (h - side) // 2
    art = raw.crop((left, top, left + side, top + side))
    # Force chunky pixels
    art = art.resize((32, 32), Image.Resampling.NEAREST).resize((OUT, OUT), Image.Resampling.NEAREST)
    mask = circle_mask(OUT)
    out = Image.new("RGBA", (OUT, OUT), (0, 0, 0, 0))
    out.paste(art, (0, 0), mask)
    # Thin school frame on top
    d = ImageDraw.Draw(out)
    col = FRAME_RGB.get(school, (255, 255, 255))
    d.ellipse((1, 1, OUT - 2, OUT - 2), outline=col + (255,), width=2)
    # Re-mask so corners stay transparent
    final = Image.new("RGBA", (OUT, OUT), (0, 0, 0, 0))
    final.paste(out, (0, 0), mask)
    return final


def main(only: list[str] | None = None):
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    HQ_DIR.mkdir(parents=True, exist_ok=True)
    spells = only if only is not None else list(EXAMPLE_SCHOOL)
    for spell in spells:
        if spell not in EXAMPLE_SCHOOL:
            print(f"SKIP unknown {spell}")
            continue
        src = SRC_ASSETS / f"{spell}.png"
        if not src.exists():
            src = HQ_DIR / f"{spell}.png"
        if not src.exists():
            print(f"MISSING {spell}")
            continue
        school = EXAMPLE_SCHOOL[spell]
        icon = to_pixel_circle(src, school)
        icon.save(OUT_DIR / f"{spell}.png")
        icon.save(HQ_DIR / f"{spell}_final.png")
        if SRC_ASSETS.joinpath(f"{spell}.png").exists():
            Image.open(SRC_ASSETS / f"{spell}.png").save(HQ_DIR / f"{spell}.png")
        print(f"OK {spell} ({school})")


if __name__ == "__main__":
    import sys

    only = [a for a in sys.argv[1:] if not a.startswith("-")]
    main(only or None)
