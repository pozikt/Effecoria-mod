"""Party C: Phi-bus cable faces + split duplicate / thin gadget icons."""
from __future__ import annotations

import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
JAR = ROOT / "build/moddev/artifacts/neoforge-21.1.242-client-extra-aka-minecraft-resources.jar"
VAN = ROOT / "art/items/vanilla_refs"
OUT_ITEM = ROOT / "src/main/resources/assets/effecoria/textures/item"
OUT_BLOCK = ROOT / "src/main/resources/assets/effecoria/textures/block"
FACE = ROOT / "art/items/for_artist"

EXTRACT_ITEM = [
    "string.png",
    "gold_nugget.png",
    "comparator.png",
    "leather_chestplate.png",
    "sugar.png",
    "paper.png",
    "fire_charge.png",
    "flint.png",
    "amethyst_shard.png",
    "magma_cream.png",
    "iron_ingot.png",
    "nether_star.png",
]
EXTRACT_BLOCK = ["obsidian.png"]

INSUL = ((12, 18, 32), (28, 42, 62), (48, 70, 96), (90, 130, 150))
CYAN = ((10, 70, 90), (30, 150, 180), (80, 230, 245), (220, 255, 255))
MITHRIL = ((40, 70, 90), (90, 140, 165), (160, 210, 230), (230, 250, 255))
LEAD = ((28, 32, 38), (70, 78, 88), (120, 128, 138), (190, 196, 204))
VOID = ((8, 4, 16), (28, 12, 48), (70, 30, 90), (140, 80, 180))
FLUX = ((12, 40, 70), (30, 110, 160), (80, 210, 240), (200, 250, 255))
OMEGA = ((40, 8, 50), (110, 20, 90), (200, 40, 80), (255, 160, 200))
VITRI = ((20, 50, 55), (50, 120, 130), (120, 200, 210), (230, 250, 255))
CHASSIS = ((35, 38, 42), (90, 95, 100), (150, 155, 160), (220, 225, 230))


def extract() -> None:
    VAN.mkdir(parents=True, exist_ok=True)
    if not JAR.exists():
        return
    with zipfile.ZipFile(JAR) as z:
        for name in EXTRACT_ITEM:
            src = f"assets/minecraft/textures/item/{name}"
            if src in z.namelist():
                (VAN / name).write_bytes(z.read(src))
        for name in EXTRACT_BLOCK:
            src = f"assets/minecraft/textures/block/{name}"
            if src in z.namelist():
                (VAN / name).write_bytes(z.read(src))


def lum(r: int, g: int, b: int) -> float:
    return (0.3 * r + 0.59 * g + 0.11 * b) / 255.0


def lerp(a, b, t: float):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def shade(pal, t: float):
    lo, mid, hi, accent = pal
    if t > 0.78:
        return accent
    if t > 0.5:
        return lerp(mid, hi, (t - 0.5) / 0.28)
    if t > 0.22:
        return lerp(lo, mid, (t - 0.22) / 0.28)
    return lo


def save_png(folder: Path, name: str, img: Image.Image, preview: bool = False) -> None:
    folder.mkdir(parents=True, exist_ok=True)
    img = img.convert("RGBA")
    path = folder / f"{name}.png"
    img.save(path)
    if preview:
        FACE.mkdir(parents=True, exist_ok=True)
        img.save(FACE / f"{name}_16x.png")
        prev = Image.new("RGBA", (128, 128), (18, 20, 28, 255))
        big = img.resize((128, 128), Image.Resampling.NEAREST)
        prev.paste(big, (0, 0), big)
        prev.save(FACE / f"{name}_16x_8x.png")
    print("wrote", path.relative_to(ROOT), path.stat().st_size)


def recolor_map(src: Path, pal, keep_glass: bool = False) -> Image.Image:
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
            if keep_glass and max(r, g, b) - min(r, g, b) < 28 and max(r, g, b) > 90:
                px[x, y] = (min(255, r), min(255, g + 6), min(255, b + 12), 255)
                continue
            t = lum(r, g, b)
            if t > 0.72:
                rgb = accent if (x + y) % 6 == 0 else hi
            elif t > 0.4:
                rgb = lerp(mid, hi, (t - 0.4) / 0.32)
            else:
                rgb = lerp(lo, mid, t / 0.4)
            px[x, y] = (*rgb, 255)
    return im


def put(px, x: int, y: int, rgb, a: int = 255) -> None:
    if 0 <= x < 16 and 0 <= y < 16:
        px[x, y] = (*rgb, a)


def paint_bus(on: bool) -> Image.Image:
    """Atlas for phi_bus UVs: rows 0-3 = long faces, cols 6-9 = ends, 6-9/6-9 = core."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    core = CYAN if on else ((18, 50, 70), (40, 90, 110), (70, 140, 160), (140, 200, 210))

    def insul(x: int, y: int) -> tuple[int, int, int]:
        t = 0.28 + 0.18 * ((x * 3 + y * 7) % 7) / 6.0
        if (x + y) % 5 == 0:
            t += 0.08
        return shade(INSUL, t)

    def filament(x: int, y: int) -> tuple[int, int, int]:
        pulse = 0.12 if on and ((x + y * 2) % 4 == 0) else 0.0
        t = (0.72 if on else 0.48) + 0.1 * ((x * 5 + y) % 3) / 2.0 + pulse
        return shade(core, t)

    for y in range(16):
        for x in range(16):
            put(px, x, y, insul(x, y))

    # Long-face strip (uv 0,0-16,4): jacket + cyan core along X.
    for x in range(16):
        put(px, x, 0, shade(INSUL, 0.22 + 0.05 * (x % 3)))
        put(px, x, 3, shade(INSUL, 0.18 + 0.04 * ((x + 1) % 3)))
        put(px, x, 1, filament(x, 1))
        put(px, x, 2, filament(x, 2))

    # End/side strip (uv 6,0-10,16): jacket + cyan core along Y. Keep y 0-3 from long-face.
    for y in range(4, 16):
        put(px, 6, y, shade(INSUL, 0.35))
        put(px, 9, y, shade(INSUL, 0.22))
        put(px, 7, y, filament(7, y))
        put(px, 8, y, filament(8, y))

    # Junction cube (uv 6,6-10,10)
    for y in range(6, 10):
        for x in range(6, 10):
            t = 0.95 if on else 0.62
            if x in (7, 8) and y in (7, 8):
                t = 1.0 if on else 0.82
            elif x in (6, 9) or y in (6, 9):
                t = 0.55 if on else 0.4
            put(px, x, y, shade(core, t))
    return img


def paint_pill() -> Image.Image:
    """Two-tone lead capsule (not a sugar pile)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    body = [
        (5, 6, 0.45), (6, 5, 0.7), (7, 5, 0.85), (8, 5, 0.75), (9, 6, 0.5),
        (4, 7, 0.4), (5, 7, 0.65), (6, 6, 0.9), (7, 6, 1.0), (8, 6, 0.8),
        (9, 7, 0.55), (10, 7, 0.35),
        (4, 8, 0.35), (5, 8, 0.55), (6, 7, 0.8), (7, 7, 0.7), (8, 7, 0.55),
        (9, 8, 0.4), (10, 8, 0.28),
        (5, 9, 0.3), (6, 8, 0.5), (7, 8, 0.45), (8, 8, 0.38), (9, 9, 0.28),
        (6, 9, 0.32), (7, 9, 0.3), (8, 9, 0.25),
    ]
    for x, y, t in body:
        # Split capsule: left dull lead, right slightly bluer
        pal = LEAD if x < 8 else ((32, 36, 48), (80, 88, 108), (130, 140, 160), (200, 210, 220))
        put(px, x, y, shade(pal, t))
    put(px, 7, 6, shade(LEAD, 1.0))
    return img


def paint_goggles() -> Image.Image:
    """Two cyan lenses + dark strap (inventory tilt)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    strap = [(2, 7), (3, 6), (12, 6), (13, 7), (3, 8), (12, 8)]
    for x, y in strap:
        put(px, x, y, shade(INSUL, 0.45))
    for x in range(4, 12):
        put(px, x, 7, shade(INSUL, 0.55))
    # lenses
    for cx in (5, 10):
        for oy in range(-2, 3):
            for ox in range(-2, 3):
                if abs(ox) + abs(oy) <= 3:
                    t = 0.9 if ox == -1 and oy == -1 else 0.55 - 0.1 * abs(ox)
                    put(px, cx + ox, 7 + oy, shade(CYAN, t))
        put(px, cx, 7, shade(CYAN, 1.0))
    return img


def main() -> None:
    extract()
    save_png(OUT_BLOCK, "phi_bus", paint_bus(False))
    save_png(OUT_BLOCK, "phi_bus_on", paint_bus(True))
    save_png(OUT_ITEM, "mithril_wire", recolor_map(VAN / "string.png", MITHRIL), preview=True)
    save_png(OUT_ITEM, "mithril_nugget", recolor_map(VAN / "gold_nugget.png", MITHRIL), preview=True)
    save_png(OUT_ITEM, "telegraph_module", recolor_map(VAN / "comparator.png", CYAN), preview=True)
    save_png(OUT_ITEM, "lead_pill", paint_pill(), preview=True)
    save_png(OUT_ITEM, "lead_cloak", recolor_map(VAN / "leather_chestplate.png", LEAD), preview=True)
    save_png(OUT_ITEM, "lead_foil", recolor_map(VAN / "paper.png", LEAD), preview=True)
    save_png(OUT_ITEM, "phi_sonar_goggles", paint_goggles(), preview=True)
    save_png(OUT_ITEM, "phi_flux_slug", recolor_map(VAN / "magma_cream.png", FLUX), preview=True)
    save_png(OUT_ITEM, "memory_crystal", recolor_map(VAN / "amethyst_shard.png", CYAN), preview=True)
    save_png(OUT_ITEM, "void_obsidian_insert", recolor_map(VAN / "flint.png", VOID), preview=True)
    save_png(OUT_ITEM, "omega_crystal_shard", recolor_map(VAN / "amethyst_shard.png", OMEGA), preview=True)
    save_png(OUT_ITEM, "golem_chassis", recolor_map(VAN / "iron_ingot.png", CHASSIS), preview=True)
    save_png(OUT_ITEM, "vitrified_golem_core", recolor_map(VAN / "nether_star.png", VITRI), preview=True)


if __name__ == "__main__":
    main()
