"""Bake Batch C flat block textures from vanilla bases + Φ accents."""
from __future__ import annotations

import json
from pathlib import Path

from PIL import Image, ImageEnhance, ImageOps

ROOT = Path(__file__).resolve().parents[2]
REF = ROOT / "art" / "items" / "vanilla_refs_batch_c"
TEX = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "block"
MODELS = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "models" / "block"
PREVIEW = ROOT / "art" / "items" / "for_artist"


def load(name: str) -> Image.Image:
    return Image.open(REF / name).convert("RGBA")


def recolor(img: Image.Image, tint: tuple[int, int, int], bright: float = 1.05) -> Image.Image:
    r, g, b, a = img.split()
    gray = ImageOps.grayscale(Image.merge("RGB", (r, g, b)))
    gray = ImageEnhance.Brightness(gray).enhance(bright)
    gray = ImageEnhance.Contrast(gray).enhance(1.1)
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


def overlay_rect(img: Image.Image, x0: int, y0: int, x1: int, y1: int, color: tuple[int, int, int, int]) -> None:
    px = img.load()
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= x < 16 and 0 <= y < 16:
                px[x, y] = color


def blend_pixel(img: Image.Image, x: int, y: int, color: tuple[int, int, int], amount: float = 0.65) -> None:
    px = img.load()
    r, g, b, a = px[x, y]
    if a == 0:
        return
    nr = int(r * (1 - amount) + color[0] * amount)
    ng = int(g * (1 - amount) + color[1] * amount)
    nb = int(b * (1 - amount) + color[2] * amount)
    px[x, y] = (nr, ng, nb, a)


def save(name: str, img: Image.Image) -> None:
    TEX.mkdir(parents=True, exist_ok=True)
    PREVIEW.mkdir(parents=True, exist_ok=True)
    img.save(TEX / f"{name}.png")
    img.resize((128, 128), Image.NEAREST).save(PREVIEW / f"{name}_8x.png")
    print("wrote", name)


def bake_skiff() -> None:
    base = recolor(load("crafter_north.png"), (55, 70, 95), bright=0.95)
    # cyan HUD bars
    for y in (5, 7, 9):
        for x in range(4, 12):
            blend_pixel(base, x, y, (80, 220, 255), 0.7)
    overlay_rect(base, 7, 11, 8, 12, (140, 90, 220, 255))  # Φ glyph
    save("skiff_control_panel", base)


def bake_lift() -> None:
    base = recolor(load("blast_furnace_front.png"), (45, 55, 75), bright=0.9)
    # vertical Φ column
    for y in range(2, 14):
        blend_pixel(base, 7, y, (90, 220, 255), 0.75)
        blend_pixel(base, 8, y, (120, 235, 255), 0.8)
    overlay_rect(base, 6, 3, 9, 4, (180, 240, 255, 255))
    save("essence_lift_core", base)


def bake_thruster() -> None:
    base = recolor(load("blast_furnace_side.png"), (50, 45, 70), bright=0.95)
    # nozzle ring
    for x, y in [(5, 5), (6, 4), (7, 4), (8, 4), (9, 4), (10, 5), (10, 6), (10, 7), (10, 8), (9, 9), (8, 9), (7, 9), (6, 9), (5, 8), (5, 7), (5, 6)]:
        blend_pixel(base, x, y, (160, 70, 220), 0.8)
    overlay_rect(base, 6, 6, 9, 8, (40, 20, 60, 255))
    overlay_rect(base, 7, 7, 8, 7, (220, 120, 255, 255))
    save("essence_thruster", base)


def bake_anchor() -> None:
    base = recolor(load("lodestone_side.png"), (70, 75, 90), bright=1.0)
    # circular seal
    for x in range(4, 12):
        for y in range(4, 12):
            dx, dy = x - 7.5, y - 7.5
            if 4.5 <= (dx * dx + dy * dy) ** 0.5 <= 5.5:
                blend_pixel(base, x, y, (160, 170, 190), 0.7)
            elif (dx * dx + dy * dy) ** 0.5 < 2.2:
                blend_pixel(base, x, y, (120, 60, 180), 0.75)
    overlay_rect(base, 7, 7, 8, 8, (180, 230, 255, 255))
    save("tower_anchor", base)


def bake_bus(on: bool) -> None:
    metal = (28, 30, 36, 255)
    edge = (55, 60, 70, 255)
    glow = (90, 220, 255, 255) if on else (50, 120, 150, 255)
    core = (160, 240, 255, 255) if on else (70, 150, 180, 255)
    img = Image.new("RGBA", (16, 16), metal)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                px[x, y] = edge
            elif 6 <= x <= 9:
                px[x, y] = glow if y % 3 != 0 else core
            elif 6 <= y <= 9 and (x < 6 or x > 9):
                px[x, y] = glow
    save("phi_bus_on" if on else "phi_bus", img)


def bake_coupler(name: str, accent: tuple[int, int, int]) -> None:
    base = recolor(load("iron_block.png"), (60, 65, 80), bright=0.95)
    # frame + channel accent cross
    for i in range(16):
        blend_pixel(base, i, 7, accent, 0.7)
        blend_pixel(base, i, 8, accent, 0.55)
        blend_pixel(base, 7, i, accent, 0.7)
        blend_pixel(base, 8, i, accent, 0.55)
    overlay_rect(base, 6, 6, 9, 9, (*accent, 255))
    overlay_rect(base, 7, 7, 8, 8, (230, 240, 255, 255))
    save(name, base)


def bake_matcher(on: bool) -> None:
    base = recolor(load("observer_front.png"), (50, 55, 80), bright=0.95)
    accent = (100, 230, 255) if on else (90, 120, 180)
    for r in (2, 4, 6):
        for x in range(16):
            for y in range(16):
                d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
                if abs(d - r) < 0.7:
                    blend_pixel(base, x, y, accent, 0.65 if on else 0.45)
    if on:
        overlay_rect(base, 7, 7, 8, 8, (200, 250, 255, 255))
    save("phi_matcher_on" if on else "phi_matcher", base)


def bake_beacon() -> None:
    base = recolor(load("observer_top.png"), (55, 70, 100), bright=1.05)
    for x in range(4, 12):
        for y in range(4, 12):
            blend_pixel(base, x, y, (80, 200, 255), 0.35)
    overlay_rect(base, 6, 6, 9, 9, (140, 230, 255, 255))
    overlay_rect(base, 7, 7, 8, 8, (255, 255, 255, 255))
    save("phi_beacon", base)
    # point model at own texture
    path = MODELS / "phi_beacon.json"
    path.write_text(
        json.dumps(
            {"parent": "minecraft:block/cube_all", "textures": {"all": "effecoria:block/phi_beacon"}},
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )


def main() -> None:
    bake_skiff()
    bake_lift()
    bake_thruster()
    bake_anchor()
    bake_bus(False)
    bake_bus(True)
    bake_coupler("phi_coupler", (120, 200, 220))
    bake_coupler("phi_coupler_life", (80, 220, 140))
    bake_coupler("phi_coupler_industry", (220, 160, 70))
    bake_coupler("phi_coupler_defense", (220, 80, 90))
    bake_coupler("phi_coupler_psi", (160, 100, 230))
    bake_matcher(False)
    bake_matcher(True)
    bake_beacon()

    names = [
        "skiff_control_panel",
        "essence_lift_core",
        "essence_thruster",
        "tower_anchor",
        "phi_bus",
        "phi_bus_on",
        "phi_coupler",
        "phi_matcher",
        "phi_beacon",
    ]
    cell = 96
    cols = 3
    rows = 3
    sheet = Image.new("RGBA", (cols * cell + 16, rows * cell + 16), (28, 28, 36, 255))
    for i, n in enumerate(names):
        im = Image.open(TEX / f"{n}.png").convert("RGBA").resize((cell, cell), Image.NEAREST)
        sheet.paste(im, (8 + (i % cols) * cell, 8 + (i // cols) * cell), im)
    sheet.save(PREVIEW / "batch_c_strip_8x.png")
    print("done")


if __name__ == "__main__":
    main()
