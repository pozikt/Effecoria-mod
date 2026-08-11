"""Bake Φ-bus textures (phi_bus.png + phi_bus_on.png) + item texture.

We generate both states from scratch for a consistent "new design" and
to avoid relying on an older texture's pixel palette.
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[3]
BLOCK_TEX = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "block"
ITEM_TEX = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "item"


def clamp(v: int) -> int:
    return max(0, min(255, v))


def make_bus(on: bool) -> Image.Image:
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    # Unpowered looks like dark conductive sheath + dim cyan filaments.
    base = (25, 30, 38, 255)
    lead = (45, 52, 62, 255)
    cyan = (60, 190, 255, 255) if on else (30, 120, 170, 255)
    cyan2 = (25, 110, 150, 255) if on else (15, 70, 105, 255)
    glow = (90, 240, 255, 255) if on else (40, 160, 200, 180)

    # Cable voxel: centered 8x8, with "square corners" silhouette.
    for y in range(4, 12):
        for x in range(4, 12):
            # Outer sheath ring
            if x in (4, 11) or y in (4, 11):
                px[x, y] = lead
            else:
                px[x, y] = base

    # Filament pattern
    # Diagonal lattice + straight stripe through center.
    for i in range(4, 12):
        px[i, i] = cyan if ((i + (0 if on else 1)) % 2 == 0) else cyan2
        px[15 - i, i] = cyan2 if (i % 2 == 0) else cyan

    for x in range(4, 12):
        px[x, 8] = cyan
    for y in range(4, 12):
        px[8, y] = cyan

    # "Powered" glow bulge around the cable silhouette.
    if on:
        for y in range(3, 13):
            for x in range(3, 13):
                if 4 <= x <= 11 and 4 <= y <= 11:
                    continue
                # radial-ish falloff
                dist = max(abs(x - 8), abs(y - 8))
                if dist <= 5 and (x + y) % 2 == 0:
                    alpha = 255 - dist * 32
                    px[x, y] = (glow[0], glow[1], glow[2], max(0, alpha))

    # Corner details
    for x, y in ((4, 6), (6, 4), (10, 4), (11, 6), (4, 10), (6, 11), (10, 11), (11, 10)):
        px[x, y] = glow if on else (80, 95, 110, 255)
    return img


def main() -> None:
    BLOCK_TEX.mkdir(parents=True, exist_ok=True)
    ITEM_TEX.mkdir(parents=True, exist_ok=True)

    bus = make_bus(on=False)
    bus_on = make_bus(on=True)

    # Generate a design sheet first, then crop:
    #  y=0..15  : phi_bus (unpowered)
    #  y=16..31 : phi_bus_on (powered)
    sheet = Image.new("RGBA", (16, 16 * 2), (0, 0, 0, 0))
    sheet.paste(bus, (0, 0))
    sheet.paste(bus_on, (0, 16))

    art_sheet = ROOT / "art" / "phi_alchemy" / "bus" / "phi_bus_sheet.png"
    art_sheet.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(art_sheet)

    # Crop & save
    sheet.crop((0, 0, 16, 16)).save(BLOCK_TEX / "phi_bus.png")
    sheet.crop((0, 16, 16, 32)).save(BLOCK_TEX / "phi_bus_on.png")
    sheet.crop((0, 0, 16, 16)).save(ITEM_TEX / "phi_bus.png")

    print("baked phi_bus.png + phi_bus_on.png (+ sheet) + item")


if __name__ == "__main__":
    main()

