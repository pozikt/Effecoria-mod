"""Recolor vanilla grass + oak sapling into Effecoria Φ palette (silhouette/alpha preserved)."""
from __future__ import annotations

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ART_EARTH = ROOT / "art/phi_earth"
ART_FLORA = ROOT / "art/phi_flora"
OUT = ROOT / "src/main/resources/assets/effecoria/textures/block"

# Foliage: same family as phi_leaves
FOLIAGE_LO = (18, 26, 78)
FOLIAGE_HI = (110, 150, 220)
GOLD = (200, 180, 60)
GOLD_ALT = (170, 155, 80)

# Soil: match existing phi_dirt purple steps (dark -> light)
SOIL = [
    (10, 6, 70),
    (40, 22, 57),
    (49, 0, 98),
    (78, 32, 123),
    (104, 29, 180),
    (18, 10, 143),
]

# Sapling stem: near phi_log bark
STEM_LO = (13, 10, 15)
STEM_HI = (40, 28, 48)


def lerp(a: tuple[int, int, int], b: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    t = max(0.0, min(1.0, t))
    return (
        int(a[0] + (b[0] - a[0]) * t),
        int(a[1] + (b[1] - a[1]) * t),
        int(a[2] + (b[2] - a[2]) * t),
    )


def lum(r: int, g: int, b: int) -> float:
    return (0.3 * r + 0.59 * g + 0.11 * b) / 255.0


def is_foliage(r: int, g: int, b: int) -> bool:
    # green grass / sapling leaves, or grayscale grass_top (r≈g≈b)
    if abs(r - g) <= 3 and abs(g - b) <= 3 and abs(r - b) <= 3:
        return True  # grayscale top
    return g > r + 10 and g > b + 5


def is_stone_speck(r: int, g: int, b: int) -> bool:
    return abs(r - g) <= 2 and abs(g - b) <= 2 and 100 <= r <= 140


def foliage_color(r: int, g: int, b: int, x: int, y: int) -> tuple[int, int, int]:
    t = lum(r, g, b)
    # slight stretch so mid-greys become readable blues
    t = t ** 0.9
    rgb = lerp(FOLIAGE_LO, FOLIAGE_HI, t)
    if t > 0.62 and ((x * 3 + y * 5) % 9 == 0):
        return GOLD if ((x + y) % 2 == 0) else GOLD_ALT
    return rgb


def soil_color(r: int, g: int, b: int) -> tuple[int, int, int]:
    if is_stone_speck(r, g, b):
        # keep pebble as cool grey-violet
        t = lum(r, g, b)
        return lerp((60, 55, 80), (140, 135, 160), t)
    t = lum(r, g, b)
    # map across SOIL stops
    idx = t * (len(SOIL) - 1)
    i0 = int(idx)
    i1 = min(i0 + 1, len(SOIL) - 1)
    return lerp(SOIL[i0], SOIL[i1], idx - i0)


def stem_color(r: int, g: int, b: int) -> tuple[int, int, int]:
    t = lum(r, g, b)
    return lerp(STEM_LO, STEM_HI, t)


def recolor_grass_top(im: Image.Image) -> Image.Image:
    im = im.convert("RGBA")
    w, h = im.size
    out = []
    for i, (r, g, b, a) in enumerate(im.getdata()):
        if a == 0:
            out.append((0, 0, 0, 0))
            continue
        x, y = i % w, i // w
        nr, ng, nb = foliage_color(r, g, b, x, y)
        out.append((nr, ng, nb, a))
    res = Image.new("RGBA", im.size)
    res.putdata(out)
    return res


def recolor_dirt(im: Image.Image) -> Image.Image:
    im = im.convert("RGBA")
    out = []
    for r, g, b, a in im.getdata():
        if a == 0:
            out.append((0, 0, 0, 0))
            continue
        out.append((*soil_color(r, g, b), a))
    res = Image.new("RGBA", im.size)
    res.putdata(out)
    return res


def recolor_grass_side(im: Image.Image) -> Image.Image:
    im = im.convert("RGBA")
    w, h = im.size
    out = []
    for i, (r, g, b, a) in enumerate(im.getdata()):
        if a == 0:
            out.append((0, 0, 0, 0))
            continue
        x, y = i % w, i // w
        if is_foliage(r, g, b) and not is_stone_speck(r, g, b):
            # side fringe greens (and any greys that are grass overlay)
            # On grass_block_side, greys are stone pebbles in dirt — only treat green as foliage
            if g > r + 8:
                rgb = foliage_color(r, g, b, x, y)
            else:
                rgb = soil_color(r, g, b)
        else:
            rgb = soil_color(r, g, b)
        out.append((*rgb, a))
    res = Image.new("RGBA", im.size)
    res.putdata(out)
    return res


def recolor_sapling(im: Image.Image) -> Image.Image:
    im = im.convert("RGBA")
    w, h = im.size
    out = []
    for i, (r, g, b, a) in enumerate(im.getdata()):
        if a == 0:
            out.append((0, 0, 0, 0))
            continue
        x, y = i % w, i // w
        if g > r + 5:
            rgb = foliage_color(r, g, b, x, y)
        else:
            rgb = stem_color(r, g, b)
        out.append((*rgb, a))
    res = Image.new("RGBA", im.size)
    res.putdata(out)
    return res


def save_both(img: Image.Image, name: str, art_dir: Path) -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    art_dir.mkdir(parents=True, exist_ok=True)
    img.save(OUT / name)
    img.save(art_dir / name)
    print("wrote", name, img.size)


def main() -> None:
    top = recolor_grass_top(Image.open(ART_EARTH / "vanilla_grass_top_16.png"))
    side = recolor_grass_side(Image.open(ART_EARTH / "vanilla_grass_side_16.png"))
    bottom = recolor_dirt(Image.open(ART_EARTH / "vanilla_dirt_16.png"))
    sapling = recolor_sapling(Image.open(ART_FLORA / "oak_sapling.png"))

    save_both(top, "phi_grass_top.png", ART_EARTH)
    save_both(side, "phi_grass_side.png", ART_EARTH)
    save_both(bottom, "phi_grass_bottom.png", ART_EARTH)
    # Keep dirt coherent with grass underside
    save_both(bottom, "phi_dirt.png", ART_EARTH)
    save_both(sapling, "phi_sapling.png", ART_FLORA)

    # alpha checks
    for src_name, dst, kind in [
        (ART_EARTH / "vanilla_grass_top_16.png", top, "top"),
        (ART_EARTH / "vanilla_grass_side_16.png", side, "side"),
        (ART_EARTH / "vanilla_dirt_16.png", bottom, "dirt"),
        (ART_FLORA / "oak_sapling.png", sapling, "sapling"),
    ]:
        src_a = [p[3] for p in Image.open(src_name).convert("RGBA").getdata()]
        dst_a = [p[3] for p in dst.getdata()]
        assert src_a == dst_a, kind
        print(kind, "alpha ok")


if __name__ == "__main__":
    main()
