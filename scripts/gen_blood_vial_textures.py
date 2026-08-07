"""Recolor phi_flask / potion textures into blood vial item icons."""
from __future__ import annotations

import os

from PIL import Image

BASE = os.path.join(
    os.path.dirname(__file__),
    "..",
    "src",
    "main",
    "resources",
    "assets",
    "effecoria",
    "textures",
    "item",
)


def recolor(img: Image.Image, liquid_rgb: tuple[int, int, int]) -> Image.Image:
    out = img.copy()
    px = out.load()
    w, h = out.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a < 20:
                continue
            # keep glass / metal neutrals and gold rims
            if max(r, g, b) - min(r, g, b) < 35 and max(r, g, b) > 80:
                continue
            if r > 180 and g > 150 and b < 130:
                continue
            is_liquid = (b > r + 10) or (g > 120 and b > 90 and r < 200) or (y > h * 0.35 and y < h * 0.92)
            if not is_liquid:
                continue
            lum = (0.3 * r + 0.59 * g + 0.11 * b) / 255.0
            nr = min(255, int(liquid_rgb[0] * lum * 1.15))
            ng = min(255, int(liquid_rgb[1] * lum * 1.15))
            nb = min(255, int(liquid_rgb[2] * lum * 1.15))
            px[x, y] = (nr, ng, nb, a)
    return out


def main() -> None:
    src = Image.open(os.path.join(BASE, "phi_flask.png")).convert("RGBA")
    potion = Image.open(os.path.join(BASE, "potion_phi_resonance.png")).convert("RGBA")

    empty = src.copy()
    px = empty.load()
    w, h = empty.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a < 20:
                continue
            if b > r + 8 and y > h * 0.35:
                gray = (r + g + b) // 3
                px[x, y] = (gray, gray, gray, max(40, a // 2))
    empty.save(os.path.join(BASE, "blood_vial_empty.png"))

    recolor(potion, (140, 22, 28)).save(os.path.join(BASE, "blood_vial.png"))
    recolor(potion, (170, 36, 48)).save(os.path.join(BASE, "mage_blood_vial.png"))
    recolor(potion, (190, 95, 28)).save(os.path.join(BASE, "wyvern_blood_vial.png"))
    recolor(potion, (48, 72, 52)).save(os.path.join(BASE, "omega_blood_vial.png"))
    print("blood vial textures written")


if __name__ == "__main__":
    main()
