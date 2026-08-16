"""Bake Batch D: star reactor faces, cartography bottom, medical pack icons."""
from __future__ import annotations

import json
from pathlib import Path

from PIL import Image, ImageEnhance, ImageOps

ROOT = Path(__file__).resolve().parents[2]
REF = ROOT / "art" / "items" / "vanilla_refs_batch_d"
TEX_B = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "block"
TEX_I = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "item"
MODELS_I = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "models" / "item"
PREVIEW = ROOT / "art" / "items" / "for_artist"


def load(name: str) -> Image.Image:
    return Image.open(REF / name).convert("RGBA")


def recolor(img: Image.Image, tint: tuple[int, int, int], bright: float = 1.05) -> Image.Image:
    r, g, b, a = img.split()
    gray = ImageOps.grayscale(Image.merge("RGB", (r, g, b)))
    gray = ImageEnhance.Brightness(gray).enhance(bright)
    gray = ImageEnhance.Contrast(gray).enhance(1.08)
    out = Image.new("RGBA", img.size)
    gp, op, ap = gray.load(), out.load(), a.load()
    tr, tg, tb = tint
    for y in range(img.size[1]):
        for x in range(img.size[0]):
            aa = ap[x, y]
            if aa == 0:
                op[x, y] = (0, 0, 0, 0)
                continue
            v = gp[x, y] / 255.0
            op[x, y] = (
                int(min(255, tr * v * 1.15)),
                int(min(255, tg * v * 1.15)),
                int(min(255, tb * v * 1.15)),
                aa,
            )
    return out


def blend(img: Image.Image, x: int, y: int, rgb: tuple[int, int, int], amt: float = 0.65) -> None:
    px = img.load()
    r, g, b, a = px[x, y]
    if a == 0:
        return
    px[x, y] = (
        int(r * (1 - amt) + rgb[0] * amt),
        int(g * (1 - amt) + rgb[1] * amt),
        int(b * (1 - amt) + rgb[2] * amt),
        a,
    )


def put(img: Image.Image, x: int, y: int, c: tuple[int, int, int, int]) -> None:
    if 0 <= x < 16 and 0 <= y < 16:
        img.load()[x, y] = c


def save_block(name: str, img: Image.Image) -> None:
    TEX_B.mkdir(parents=True, exist_ok=True)
    PREVIEW.mkdir(parents=True, exist_ok=True)
    img.save(TEX_B / f"{name}.png")
    img.resize((128, 128), Image.NEAREST).save(PREVIEW / f"{name}_8x.png")
    print("block", name)


def save_item(name: str, img: Image.Image) -> None:
    TEX_I.mkdir(parents=True, exist_ok=True)
    PREVIEW.mkdir(parents=True, exist_ok=True)
    img.save(TEX_I / f"{name}.png")
    img.resize((128, 128), Image.NEAREST).save(PREVIEW / f"{name}_8x.png")
    MODELS_I.mkdir(parents=True, exist_ok=True)
    (MODELS_I / f"{name}.json").write_text(
        json.dumps(
            {"parent": "minecraft:item/generated", "textures": {"layer0": f"effecoria:item/{name}"}},
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    print("item", name)


def bake_star() -> None:
    side = recolor(load("respawn_anchor_top_off.png"), (40, 55, 85), bright=0.95)
    for y in range(3, 13):
        blend(side, 3, y, (90, 200, 255), 0.55)
        blend(side, 12, y, (90, 200, 255), 0.55)
    for x in range(5, 11):
        blend(side, x, 7, (140, 230, 255), 0.5)
        blend(side, x, 8, (100, 180, 240), 0.45)
    put(side, 7, 4, (200, 240, 255, 255))
    put(side, 8, 4, (200, 240, 255, 255))
    save_block("star_reactor_side", side)

    side_on = side.copy()
    for y in range(3, 13):
        blend(side_on, 3, y, (160, 240, 255), 0.75)
        blend(side_on, 12, y, (160, 240, 255), 0.75)
    put(side_on, 7, 7, (255, 255, 255, 255))
    put(side_on, 8, 8, (255, 255, 255, 255))
    save_block("star_reactor_side_on", side_on)

    top = recolor(load("beacon.png"), (50, 80, 120), bright=1.05)
    for x in range(16):
        for y in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 3:
                blend(top, x, y, (140, 230, 255), 0.7)
            elif 3 <= d <= 5:
                blend(top, x, y, (180, 190, 210), 0.45)
    put(top, 7, 7, (255, 255, 255, 255))
    put(top, 8, 7, (220, 250, 255, 255))
    put(top, 7, 8, (220, 250, 255, 255))
    put(top, 8, 8, (180, 240, 255, 255))
    save_block("star_reactor_top", top)


def bake_cartography_bottom() -> None:
    # Wood underside with faint Φ map grid (not solid gray)
    base = recolor(load("cartography_table_side3.png"), (90, 70, 45), bright=0.9)
    for x in range(2, 14, 3):
        for y in range(2, 14):
            blend(base, x, y, (60, 120, 140), 0.25)
    for y in range(2, 14, 3):
        for x in range(2, 14):
            blend(base, x, y, (60, 120, 140), 0.2)
    put(base, 7, 7, (100, 180, 200, 255))
    save_block("phi_cartography_table_bottom", base)


def bottle(liquid: tuple[int, int, int], cork: tuple[int, int, int] = (140, 90, 50)) -> Image.Image:
    """Honey bottle silhouette remapped to liquid color; keep glass highlights."""
    base = load("honey_bottle.png")
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    sp, op = base.load(), out.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = sp[x, y]
            if a == 0:
                continue
            if r > 220 and g > 200 and b > 160:
                op[x, y] = (230, 235, 245, a)  # glass
            elif y < 5 and r > 140 and g < 120:
                op[x, y] = (*cork, a)
            else:
                v = (0.3 * r + 0.5 * g + 0.2 * b) / 255.0
                op[x, y] = (
                    int(min(255, liquid[0] * v * 1.25 + 15)),
                    int(min(255, liquid[1] * v * 1.25 + 10)),
                    int(min(255, liquid[2] * v * 1.25 + 10)),
                    a,
                )
    return out


def bake_medical() -> None:
    save_item("anti_phi_serum", bottle((90, 210, 230), (160, 170, 180)))  # lead cork
    save_item("lung_rinse", bottle((70, 140, 230)))
    save_item("orkanumn_stimulant", bottle((220, 90, 40)))
    save_item("potion_omega_cleanse", bottle((140, 50, 180)))
    # salve: squat jar from honey — darker, cream rim
    salve = bottle((180, 140, 200), (210, 200, 180))
    save_item("omega_amputation_salve", salve)
    # essence dew: small soft cyan-green
    dew = bottle((120, 220, 160))
    # add sparkle
    put(dew, 6, 9, (200, 255, 230, 255))
    put(dew, 9, 11, (180, 250, 220, 255))
    save_item("essence_dew", dew)


def main() -> None:
    bake_star()
    bake_cartography_bottom()
    bake_medical()

    names_b = ["star_reactor_side", "star_reactor_side_on", "star_reactor_top", "phi_cartography_table_bottom"]
    names_i = [
        "anti_phi_serum",
        "lung_rinse",
        "orkanumn_stimulant",
        "potion_omega_cleanse",
        "omega_amputation_salve",
        "essence_dew",
    ]
    cell = 96
    sheet = Image.new("RGBA", (4 * cell + 16, 3 * cell + 16), (28, 28, 36, 255))
    for i, n in enumerate(names_b + names_i):
        path = (TEX_B if i < 4 else TEX_I) / f"{n}.png"
        im = Image.open(path).convert("RGBA").resize((cell, cell), Image.NEAREST)
        sheet.paste(im, (8 + (i % 4) * cell, 8 + (i // 4) * cell), im)
    sheet.save(PREVIEW / "batch_d_strip_8x.png")
    print("done")


if __name__ == "__main__":
    main()
