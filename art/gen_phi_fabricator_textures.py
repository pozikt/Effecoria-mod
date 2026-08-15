"""Recolor vanilla crafter + amethyst_shard for Φ-fabricator blocks / memory crystal."""
from __future__ import annotations

import zipfile
from pathlib import Path

from PIL import Image, ImageEnhance, ImageOps

ROOT = Path(__file__).resolve().parents[1]
JAR = Path.home() / ".gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar"
TEX_BLOCK = ROOT / "src/main/resources/assets/effecoria/textures/block"
TEX_ITEM = ROOT / "src/main/resources/assets/effecoria/textures/item"


def extract(path: str) -> Image.Image:
    with zipfile.ZipFile(JAR) as zf:
        with zf.open(path) as f:
            return Image.open(f).convert("RGBA")


def recolor(img: Image.Image, tint: tuple[int, int, int], sat=0.9, bright=1.05) -> Image.Image:
    r, g, b, a = img.split()
    rgb = Image.merge("RGB", (r, g, b))
    gray = ImageOps.grayscale(rgb)
    gray = ImageEnhance.Brightness(gray).enhance(bright)
    gray = ImageEnhance.Contrast(gray).enhance(1.08)
    out = Image.new("RGBA", img.size)
    gp = gray.load()
    op = out.load()
    tr, tg, tb = tint
    for y in range(img.size[1]):
        for x in range(img.size[0]):
            aa = a.getpixel((x, y))
            if aa == 0:
                op[x, y] = (0, 0, 0, 0)
                continue
            v = gp[x, y] / 255.0
            op[x, y] = (
                int(min(255, tr * v * 1.2)),
                int(min(255, tg * v * 1.2)),
                int(min(255, tb * v * 1.2)),
                aa,
            )
    return out


def main() -> None:
    TEX_BLOCK.mkdir(parents=True, exist_ok=True)
    TEX_ITEM.mkdir(parents=True, exist_ok=True)

    side = extract("assets/minecraft/textures/block/crafter_east.png")
    top = extract("assets/minecraft/textures/block/crafter_top.png")
    front = extract("assets/minecraft/textures/block/crafter_north.png")
    front_on = extract("assets/minecraft/textures/block/crafter_north_crafting.png")

    tiers = {
        "phi_fabricator": (90, 160, 210),
        "phi_fabricator_ii": (60, 180, 190),
        "phi_fabricator_iii": (140, 90, 200),
    }
    for name, tint in tiers.items():
        recolor(side, tint).save(TEX_BLOCK / f"{name}_side.png")
        recolor(top, tint, bright=1.1).save(TEX_BLOCK / f"{name}_top.png")
        recolor(front, tint, bright=0.95).save(TEX_BLOCK / f"{name}_front.png")
        recolor(front_on, tint, bright=1.2).save(TEX_BLOCK / f"{name}_front_on.png")

    crystal = extract("assets/minecraft/textures/item/amethyst_shard.png")
    recolor(crystal, (70, 190, 230), bright=1.15).save(TEX_ITEM / "memory_crystal.png")

    # GUI atlas — see art/gen_phi_fabricator_gui.py
    print("Wrote fabricator block/item textures (GUI: python art/gen_phi_fabricator_gui.py)")


if __name__ == "__main__":
    main()
