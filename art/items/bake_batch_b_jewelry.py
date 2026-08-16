"""Bake Batch B jewelry/charm item textures + point models at them."""
from __future__ import annotations

import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
REF = ROOT / "art" / "items" / "vanilla_refs_batch_b"
TEX = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "item"
MODELS = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "models" / "item"
PREVIEW = ROOT / "art" / "items" / "for_artist"
EXISTING = TEX


def load_ref(name: str) -> Image.Image:
    return Image.open(REF / name).convert("RGBA")


def load_mod(name: str) -> Image.Image:
    return Image.open(EXISTING / f"{name}.png").convert("RGBA")


def blank() -> Image.Image:
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def put(img: Image.Image, pixels: dict[tuple[int, int], tuple[int, int, int, int]]) -> None:
    px = img.load()
    for (x, y), c in pixels.items():
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = c


def sample(img: Image.Image, x: int, y: int) -> tuple[int, int, int, int]:
    return img.getpixel((x, y))


def gold_palette() -> dict[str, tuple[int, int, int, int]]:
    g = load_ref("gold_nugget.png")
    return {
        "hi": sample(g, 7, 5),
        "mid": sample(g, 8, 8),
        "lo": sample(g, 10, 11),
        "outline": (60, 40, 10, 255),
    }


def lead_palette() -> dict[str, tuple[int, int, int, int]]:
    i = load_ref("iron_nugget.png")
    return {
        "hi": (170, 175, 185, 255),
        "mid": sample(i, 8, 8),
        "lo": (70, 75, 85, 255),
        "outline": (30, 32, 38, 255),
    }


def gem_palette() -> dict[str, tuple[int, int, int, int]]:
    a = load_ref("amethyst_shard.png")
    return {
        "hi": (210, 170, 255, 255),
        "mid": sample(a, 8, 8),
        "lo": (90, 40, 160, 255),
        "core": (120, 230, 255, 255),
    }


def ring(img: Image.Image, pal: dict, *, gem: tuple[int, int, int, int] | None = None) -> None:
    """Draw a simple oval ring centered."""
    outline, mid, hi, lo = pal["outline"], pal["mid"], pal["hi"], pal["lo"]
    # outer oval
    for x, y in [
        (5, 6), (6, 5), (7, 5), (8, 5), (9, 5), (10, 6),
        (4, 7), (4, 8), (4, 9), (11, 7), (11, 8), (11, 9),
        (5, 10), (6, 11), (7, 11), (8, 11), (9, 11), (10, 10),
    ]:
        put(img, {(x, y): outline})
    for x, y in [
        (6, 6), (7, 6), (8, 6), (9, 6),
        (5, 7), (5, 8), (5, 9), (10, 7), (10, 8), (10, 9),
        (6, 10), (7, 10), (8, 10), (9, 10),
    ]:
        put(img, {(x, y): mid})
    put(img, {(6, 7): hi, (7, 7): hi, (8, 7): mid})
    put(img, {(8, 9): lo, (9, 9): lo})
    if gem is not None:
        put(img, {(7, 4): gem, (8, 4): gem, (7, 3): (255, 255, 255, 220), (8, 5): gem})


def band(img: Image.Image, pal: dict) -> None:
    """Thicker blank band (pre-ring)."""
    outline, mid, hi = pal["outline"], pal["mid"], pal["hi"]
    for y in range(6, 11):
        for x in range(3, 13):
            if x in (3, 12) or y in (6, 10):
                put(img, {(x, y): outline})
            else:
                put(img, {(x, y): mid})
    put(img, {(5, 7): hi, (6, 7): hi})


def charm(img: Image.Image, pal: dict, fill: tuple[int, int, int, int]) -> None:
    """Hanging teardrop charm."""
    o, m, h = pal["outline"], pal["mid"], pal["hi"]
    put(img, {(7, 2): o, (8, 2): o, (7, 3): m, (8, 3): m})  # loop
    for x, y in [(6, 5), (7, 4), (8, 4), (9, 5), (5, 6), (10, 6), (5, 7), (10, 7), (6, 8), (9, 8), (7, 9), (8, 9)]:
        put(img, {(x, y): o})
    for x, y in [(6, 6), (7, 5), (8, 5), (9, 6), (6, 7), (7, 6), (8, 6), (9, 7), (7, 7), (8, 7), (7, 8), (8, 8)]:
        put(img, {(x, y): fill})
    put(img, {(7, 6): h})


def amulet(img: Image.Image, pal: dict, core: tuple[int, int, int, int]) -> None:
    o, m, h = pal["outline"], pal["mid"], pal["hi"]
    # chain
    for y in range(1, 5):
        put(img, {(7, y): o, (8, y): m})
    # pendant
    for x, y in [(5, 6), (6, 5), (7, 5), (8, 5), (9, 5), (10, 6), (4, 7), (11, 7), (4, 8), (11, 8), (5, 9), (10, 9), (6, 10), (7, 10), (8, 10), (9, 10)]:
        put(img, {(x, y): o})
    for x in range(5, 11):
        for y in range(6, 10):
            if img.getpixel((x, y))[3] == 0:
                put(img, {(x, y): m})
    put(img, {(7, 7): core, (8, 7): core, (7, 8): core, (8, 8): h})


def gem_alone(img: Image.Image, gp: dict) -> None:
    o = (40, 20, 70, 255)
    for x, y in [(7, 3), (8, 3), (6, 4), (9, 4), (5, 5), (10, 5), (5, 6), (10, 6), (6, 7), (9, 7), (7, 8), (8, 8)]:
        put(img, {(x, y): o})
    put(img, {
        (7, 4): gp["hi"], (8, 4): gp["mid"],
        (6, 5): gp["mid"], (7, 5): gp["core"], (8, 5): gp["hi"], (9, 5): gp["mid"],
        (6, 6): gp["lo"], (7, 6): gp["mid"], (8, 6): gp["lo"], (9, 6): gp["lo"],
        (7, 7): gp["lo"], (8, 7): gp["lo"],
    })


def faceted_focus(img: Image.Image) -> None:
    # Hex lens — distinct from resonance_focus swirl
    o = (30, 20, 55, 255)
    rim = (140, 90, 210, 255)
    glass = (70, 40, 140, 255)
    core = (100, 220, 255, 255)
    # hex outline
    pts = [
        (8, 2), (11, 4), (11, 8), (8, 10), (5, 8), (5, 4),
    ]
    # fill diamond/hex body
    for y in range(3, 10):
        for x in range(5, 12):
            dx, dy = abs(x - 8), abs(y - 6)
            if dx + dy // 2 <= 4:
                put(img, {(x, y): glass})
    for x, y in [(8, 2), (9, 2), (10, 3), (11, 4), (11, 5), (11, 7), (11, 8), (10, 9), (9, 10), (8, 10), (7, 10), (6, 9), (5, 8), (5, 7), (5, 5), (5, 4), (6, 3), (7, 2)]:
        put(img, {(x, y): o})
    for x, y in [(7, 4), (8, 4), (9, 4), (6, 5), (10, 5), (6, 7), (10, 7), (7, 8), (8, 8), (9, 8)]:
        put(img, {(x, y): rim})
    put(img, {(7, 6): core, (8, 6): (230, 250, 255, 255), (8, 5): core})


def essentocyte_kit(img: Image.Image) -> None:
    # Small case with lead frame + purple vial
    frame = (90, 95, 105, 255)
    dark = (40, 42, 48, 255)
    vial = (130, 50, 160, 255)
    hi = (180, 190, 200, 255)
    for y in range(4, 13):
        for x in range(3, 13):
            put(img, {(x, y): frame if x in (3, 12) or y in (4, 12) else dark})
    put(img, {(4, 5): hi})
    # vial inside
    for y in range(6, 11):
        put(img, {(7, y): vial, (8, y): vial})
    put(img, {(7, 6): (200, 140, 255, 255), (8, 5): (150, 150, 160, 255)})  # cork


def save(name: str, img: Image.Image) -> None:
    TEX.mkdir(parents=True, exist_ok=True)
    PREVIEW.mkdir(parents=True, exist_ok=True)
    img.save(TEX / f"{name}.png")
    img.resize((128, 128), Image.NEAREST).save(PREVIEW / f"{name}_8x.png")
    print("wrote", name)


def point_model(name: str) -> None:
    path = MODELS / f"{name}.json"
    data = {"parent": "minecraft:item/generated", "textures": {"layer0": f"effecoria:item/{name}"}}
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    gold = gold_palette()
    lead = lead_palette()
    gem = gem_palette()

    img = blank()
    band(img, gold)
    save("jewelry_band", img)
    point_model("jewelry_band")

    img = blank()
    ring(img, gold)
    save("assembled_ring", img)
    point_model("assembled_ring")

    img = blank()
    ring(img, gold, gem=gem["core"])
    save("essonite_ring", img)
    point_model("essonite_ring")

    img = blank()
    gem_alone(img, gem)
    save("jewelry_gem", img)
    point_model("jewelry_gem")

    img = blank()
    charm(img, lead, (120, 125, 135, 255))
    save("assembled_charm", img)
    point_model("assembled_charm")

    img = blank()
    charm(img, lead, (85, 90, 100, 255))
    # darker lead blot
    put(img, {(7, 7): (55, 60, 70, 255), (8, 7): (55, 60, 70, 255)})
    save("lead_charm", img)
    point_model("lead_charm")

    img = blank()
    amulet(img, gold, (220, 180, 60, 255))
    save("assembled_amulet", img)
    point_model("assembled_amulet")

    img = blank()
    amulet(img, gold, (140, 230, 255, 255))
    put(img, {(7, 7): (255, 255, 255, 255)})  # star core
    save("star_amulet", img)
    point_model("star_amulet")

    img = blank()
    cyan = {
        "outline": (20, 60, 90, 255),
        "mid": (60, 170, 210, 255),
        "hi": (160, 240, 255, 255),
        "lo": (30, 90, 130, 255),
    }
    band(img, cyan)
    put(img, {(7, 8): (120, 80, 200, 255), (8, 8): (120, 80, 200, 255)})
    save("phi_band", img)
    point_model("phi_band")

    img = blank()
    faceted_focus(img)
    save("faceted_focus", img)
    point_model("faceted_focus")

    img = blank()
    essentocyte_kit(img)
    save("essentocyte_kit", img)
    point_model("essentocyte_kit")

    # strip preview
    names = [
        "jewelry_band",
        "assembled_ring",
        "essonite_ring",
        "jewelry_gem",
        "assembled_charm",
        "lead_charm",
        "assembled_amulet",
        "star_amulet",
        "phi_band",
        "faceted_focus",
        "essentocyte_kit",
    ]
    cols = 4
    rows = 3
    cell = 128
    sheet = Image.new("RGBA", (cols * cell + 16, rows * cell + 16), (30, 30, 40, 255))
    for i, n in enumerate(names):
        im = Image.open(TEX / f"{n}.png").convert("RGBA").resize((cell, cell), Image.NEAREST)
        sheet.paste(im, (8 + (i % cols) * cell, 8 + (i // cols) * cell), im)
    sheet.save(PREVIEW / "batch_b_strip_8x.png")
    print("done")


if __name__ == "__main__":
    main()
