"""Party B: jewelry / charms / amulets / kit / seal primer from vanilla silhouettes + painted rings."""
from __future__ import annotations

import zipfile
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
JAR = ROOT / "build/moddev/artifacts/neoforge-21.1.242-client-extra-aka-minecraft-resources.jar"
VAN = ROOT / "art/items/vanilla_refs"
OUT = ROOT / "src/main/resources/assets/effecoria/textures/item"
FACE = ROOT / "art/items/for_artist"
MODELS = ROOT / "src/main/resources/assets/effecoria/models/item"

EXTRACT = [
    "gold_nugget.png",
    "iron_nugget.png",
    "amethyst_shard.png",
    "diamond.png",
    "nether_star.png",
    "name_tag.png",
    "bundle.png",
    "paper.png",
    "emerald.png",
]

GOLD = ((90, 55, 12), (180, 120, 28), (230, 180, 50), (255, 235, 140))
LEAD = ((28, 32, 38), (70, 78, 88), (120, 128, 138), (180, 188, 198))
CYAN = ((12, 50, 80), (30, 110, 160), (70, 200, 230), (200, 250, 255))
INDIGO = ((18, 22, 70), (40, 55, 140), (80, 120, 210), (160, 210, 255))
STAR = ((40, 30, 10), (160, 120, 30), (240, 200, 80), (255, 250, 200))
MED = ((40, 70, 90), (80, 130, 150), (140, 200, 210), (230, 245, 250))


def extract() -> None:
    VAN.mkdir(parents=True, exist_ok=True)
    if not JAR.exists():
        return
    with zipfile.ZipFile(JAR) as z:
        for name in EXTRACT:
            src = f"assets/minecraft/textures/item/{name}"
            if src in z.namelist():
                (VAN / name).write_bytes(z.read(src))


def lum(r: int, g: int, b: int) -> float:
    return (0.3 * r + 0.59 * g + 0.11 * b) / 255.0


def lerp(a, b, t: float):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def save_item(name: str, img: Image.Image) -> None:
    img = img.convert("RGBA")
    OUT.mkdir(parents=True, exist_ok=True)
    FACE.mkdir(parents=True, exist_ok=True)
    img.save(OUT / f"{name}.png")
    img.save(FACE / f"{name}_16x.png")
    prev = Image.new("RGBA", (128, 128), (18, 20, 28, 255))
    big = img.resize((128, 128), Image.Resampling.NEAREST)
    prev.paste(big, (0, 0), big)
    prev.save(FACE / f"{name}_16x_8x.png")
    print("wrote", name, (OUT / f"{name}.png").stat().st_size)


def recolor_map(src: Path, pal, keep_neutral: bool = False) -> Image.Image:
    lo, mid, hi, accent = pal
    im = Image.open(src).convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a < 16:
                px[x, y] = (0, 0, 0, 0)
                continue
            if keep_neutral and max(r, g, b) - min(r, g, b) < 28 and max(r, g, b) > 90:
                px[x, y] = (r, g, b, 255)
                continue
            t = lum(r, g, b)
            if t > 0.72:
                rgb = accent if (x + y) % 7 == 0 else hi
            elif t > 0.4:
                rgb = lerp(mid, hi, (t - 0.4) / 0.32)
            else:
                rgb = lerp(lo, mid, t / 0.4)
            px[x, y] = (*rgb, 255)
    return im


def put(px, x: int, y: int, rgb, a: int = 255) -> None:
    if 0 <= x < 16 and 0 <= y < 16:
        px[x, y] = (*rgb, a)


def shade(pal, t: float):
    lo, mid, hi, accent = pal
    if t > 0.75:
        return accent
    if t > 0.5:
        return lerp(mid, hi, (t - 0.5) / 0.25)
    if t > 0.25:
        return lerp(lo, mid, (t - 0.25) / 0.25)
    return lo


def paint_ring(pal, gem_pal=None, gem_at=(8, 5)) -> Image.Image:
    """Hollow oval ring at inventory tilt — readable as jewelry blank / assembled ring."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(3, 12):
        for x in range(4, 13):
            nx = (x - 8) / 4.2
            ny = (y - 7) / 3.6
            if nx * nx + ny * ny <= 1.0:
                hx = (x - 8) / 2.4
                hy = (y - 7) / 2.0
                if hx * hx + hy * hy >= 0.55:
                    t = 0.35 + 0.35 * (1.0 - ((x - 5) + (y - 3)) / 18.0)
                    put(px, x, y, shade(pal, t))
    put(px, 6, 4, shade(pal, 0.95))
    put(px, 7, 4, shade(pal, 0.9))
    if gem_pal is not None:
        gx, gy = gem_at
        for ox, oy, t in (
            (0, 0, 0.9), (1, 0, 0.7), (-1, 0, 0.55),
            (0, 1, 0.5), (0, -1, 0.75), (1, -1, 0.65),
        ):
            put(px, gx + ox, gy + oy, shade(gem_pal, t))
    return img


def paint_band_flat(pal) -> Image.Image:
    """Flat cuff / phi_band — wide metallic strip with buckle glint."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(6, 11):
        for x in range(2, 14):
            t = 0.3 + 0.4 * ((x - 2) / 12.0)
            if y in (6, 10):
                t *= 0.7
            put(px, x, y, shade(pal, t))
    # buckle / clasp
    for x, y, t in ((7, 7, 0.95), (8, 7, 0.9), (7, 8, 0.6), (8, 8, 0.55), (9, 7, 0.75)):
        put(px, x, y, shade(CYAN, t))
    return img


def paint_amulet(pal, gem_pal, starry: bool = False) -> Image.Image:
    """Pendant on a short chain — amulet silhouette."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    # chain
    chain = [(7, 1), (8, 2), (7, 3), (8, 4)]
    for x, y in chain:
        put(px, x, y, shade(pal, 0.55 if y % 2 else 0.75))
    # bail
    put(px, 7, 5, shade(pal, 0.8))
    put(px, 8, 5, shade(pal, 0.7))
    # pendant body (rounded diamond)
    for y in range(6, 14):
        for x in range(4, 12):
            dx, dy = abs(x - 7.5), abs(y - 9.5)
            if dx + dy * 0.85 < 4.2:
                t = 0.35 + 0.4 * (1.0 - (dx + dy) / 6.0)
                put(px, x, y, shade(pal, t))
    # gem / star core
    if starry:
        for x, y, t in (
            (7, 8, 1.0), (8, 8, 0.95), (7, 9, 0.85), (8, 9, 0.8),
            (6, 8, 0.7), (9, 8, 0.7), (7, 7, 0.75), (8, 10, 0.6),
        ):
            put(px, x, y, shade(gem_pal, t))
    else:
        for x, y, t in ((7, 8, 0.95), (8, 8, 0.85), (7, 9, 0.7), (8, 9, 0.6)):
            put(px, x, y, shade(gem_pal, t))
    return img


def paint_charm(pal, accent_pal) -> Image.Image:
    """Small charm: loop + teardrop body (Curios charm slot)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    # loop
    for x, y in ((7, 2), (8, 2), (6, 3), (9, 3), (7, 4), (8, 4)):
        put(px, x, y, shade(pal, 0.7))
    # body
    for y in range(5, 14):
        span = 1 + (y - 5) // 2
        if y > 10:
            span = max(1, 4 - (y - 10))
        for x in range(8 - span, 8 + span):
            t = 0.3 + 0.45 * ((14 - y) / 9.0)
            put(px, x, y, shade(pal, t))
    # accent mark
    put(px, 7, 8, shade(accent_pal, 0.9))
    put(px, 8, 8, shade(accent_pal, 0.75))
    put(px, 7, 9, shade(accent_pal, 0.55))
    return img


def paint_seal_primer(paper: Image.Image) -> Image.Image:
    """Phi-paper blank with a gold program seal stamp."""
    img = paper.copy()
    px = img.load()
    # stamp circle + plus
    for ang in range(0, 360, 20):
        import math
        rad = math.radians(ang)
        x = 8 + int(round(3.2 * math.cos(rad)))
        y = 8 + int(round(3.2 * math.sin(rad)))
        put(px, x, y, shade(GOLD, 0.8))
    put(px, 8, 8, shade(GOLD, 1.0))
    put(px, 8, 7, shade(GOLD, 0.9))
    put(px, 8, 9, shade(GOLD, 0.9))
    put(px, 7, 8, shade(GOLD, 0.9))
    put(px, 9, 8, shade(GOLD, 0.9))
    return img


def write_model(name: str) -> None:
    path = MODELS / f"{name}.json"
    path.write_text(
        '{\n  "parent": "minecraft:item/generated",\n'
        f'  "textures": {{\n    "layer0": "effecoria:item/{name}"\n  }}\n}}\n',
        encoding="utf-8",
    )
    print("model", name)


def paint_essentocyte_kit() -> Image.Image:
    """Medical pouch: teal satchel + white clasp + cyan vial glint."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    # body
    for y in range(5, 14):
        for x in range(3, 13):
            if y == 5 and x in (3, 12):
                continue
            t = 0.25 + 0.4 * ((y - 5) / 9.0)
            if x <= 4 or x >= 11:
                t *= 0.75
            put(px, x, y, shade(MED, t))
    # flap
    for x in range(4, 12):
        put(px, x, 4, shade(MED, 0.55))
        put(px, x, 5, shade(MED, 0.7))
    # clasp
    put(px, 7, 5, shade(CYAN, 0.95))
    put(px, 8, 5, shade(CYAN, 0.85))
    # medical cross
    for x, y in ((7, 8), (8, 8), (7, 9), (8, 9), (6, 8), (9, 8), (7, 7), (8, 10)):
        put(px, x, y, (240, 245, 250))
    # side vial peek
    put(px, 11, 9, shade(CYAN, 0.9))
    put(px, 11, 10, shade(CYAN, 0.7))
    put(px, 11, 11, shade(CYAN, 0.5))
    return img


def main() -> None:
    extract()
    # Gems / focus — vanilla crystal silhouettes
    save_item("jewelry_gem", recolor_map(VAN / "amethyst_shard.png", CYAN))
    save_item("faceted_focus", recolor_map(VAN / "diamond.png", INDIGO))
    # Bands / rings — painted
    save_item("jewelry_band", paint_ring(GOLD))
    save_item("assembled_ring", paint_ring(GOLD, CYAN, gem_at=(8, 5)))
    save_item("essonite_ring", paint_ring(INDIGO, CYAN, gem_at=(8, 5)))
    save_item("phi_band", paint_band_flat(CYAN))
    # Charms
    save_item("lead_charm", paint_charm(LEAD, LEAD))
    save_item("assembled_charm", paint_charm(GOLD, CYAN))
    # Amulets
    save_item("assembled_amulet", paint_amulet(GOLD, CYAN, starry=False))
    save_item("star_amulet", paint_amulet(STAR, CYAN, starry=True))
    # Keep gold_amulet readable — gold pendant without cyan (pure gold gem)
    save_item("gold_amulet", paint_amulet(GOLD, GOLD, starry=False))
    # Medical kit
    save_item("essentocyte_kit", paint_essentocyte_kit())
    # Seal primer — own texture + model (was phi_paper)
    paper = recolor_map(VAN / "paper.png", ((40, 70, 120), (90, 130, 190), (160, 200, 240), (220, 240, 255)))
    save_item("item_seal_primer", paint_seal_primer(paper))
    write_model("item_seal_primer")


if __name__ == "__main__":
    main()
