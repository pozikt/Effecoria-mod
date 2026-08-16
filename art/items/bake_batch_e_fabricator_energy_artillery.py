"""Batch E: redesign fabricators, sparse energy cores, artillery modules."""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageEnhance, ImageOps

ROOT = Path(__file__).resolve().parents[2]
REF = ROOT / "art" / "items" / "vanilla_refs_batch_e"
TEX = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "block"
PREVIEW = ROOT / "art" / "items" / "for_artist"


def load(name: str) -> Image.Image:
    return Image.open(REF / name).convert("RGBA")


def recolor(img: Image.Image, tint: tuple[int, int, int], bright: float = 1.05) -> Image.Image:
    r, g, b, a = img.split()
    gray = ImageOps.grayscale(Image.merge("RGB", (r, g, b)))
    gray = ImageEnhance.Brightness(gray).enhance(bright)
    gray = ImageEnhance.Contrast(gray).enhance(1.12)
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
                int(min(255, tr * v * 1.18)),
                int(min(255, tg * v * 1.18)),
                int(min(255, tb * v * 1.18)),
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


def save(name: str, img: Image.Image) -> None:
    TEX.mkdir(parents=True, exist_ok=True)
    PREVIEW.mkdir(parents=True, exist_ok=True)
    img.save(TEX / f"{name}.png")
    img.resize((96, 96), Image.NEAREST).save(PREVIEW / f"{name}_8x.png")
    print("wrote", name)


# --- Fabricators from unique per-tier concept art ---

SKETCH = ROOT / "art" / "items" / "sketches"

FABRICATOR_CONCEPTS = {
    "phi_fabricator": "phi_fabricator_i_concept.png",
    "phi_fabricator_ii": "phi_fabricator_ii_concept.png",
    "phi_fabricator_iii": "phi_fabricator_iii_concept.png",
}

# Side/top accents keyed off each concept's identity
FABRICATOR_ACCENTS = {
    "phi_fabricator": {
        "metal": (70, 80, 95),
        "accent": (70, 210, 245),
        "panel": (25, 32, 45),
    },
    "phi_fabricator_ii": {
        "metal": (35, 105, 115),
        "accent": (60, 240, 235),
        "panel": (15, 40, 48),
    },
    "phi_fabricator_iii": {
        "metal": (70, 40, 95),
        "accent": (230, 90, 220),
        "panel": (28, 14, 40),
    },
}


def fill_rect(img: Image.Image, x0: int, y0: int, x1: int, y1: int, c: tuple[int, int, int, int]) -> None:
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(img, x, y, c)


def crop_face(im: Image.Image, pad: float = 0.04) -> Image.Image:
    """Crop near-empty margins; keep the machine face square."""
    rgb = im.convert("RGB")
    w, h = rgb.size
    px = rgb.load()
    # Background is dark charcoal — treat very dark / very even dark as margin
    def is_bg(x: int, y: int) -> bool:
        r, g, b = px[x, y]
        return r + g + b < 55

    xs = [x for x in range(w) for y in range(0, h, 8) if not is_bg(x, y)]
    ys = [y for y in range(h) for x in range(0, w, 8) if not is_bg(x, y)]
    if not xs or not ys:
        return im
    x0, x1 = min(xs), max(xs)
    y0, y1 = min(ys), max(ys)
    dx = int((x1 - x0) * pad)
    dy = int((y1 - y0) * pad)
    x0, y0 = max(0, x0 - dx), max(0, y0 - dy)
    x1, y1 = min(w - 1, x1 + dx), min(h - 1, y1 + dy)
    side = max(x1 - x0, y1 - y0)
    cx, cy = (x0 + x1) // 2, (y0 + y1) // 2
    half = side // 2
    left, top = max(0, cx - half), max(0, cy - half)
    right, bottom = min(w, left + side), min(h, top + side)
    return im.crop((left, top, right, bottom))


def concept_to_16(path: Path) -> Image.Image:
    """Downscale concept face to readable Minecraft 16×16."""
    raw = Image.open(path).convert("RGBA")
    face = crop_face(raw)
    # two-step box shrink keeps silhouette better than one-shot 1024→16
    mid = face.resize((64, 64), Image.Resampling.BOX)
    mid = ImageEnhance.Contrast(mid).enhance(1.25)
    mid = ImageEnhance.Color(mid).enhance(1.15)
    small = mid.resize((16, 16), Image.Resampling.BOX)
    small = ImageEnhance.Contrast(small).enhance(1.12)
    # force opaque
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    out.paste(small, (0, 0))
    px = out.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            px[x, y] = (r, g, b, 255)
    return out


def lit_front(img: Image.Image, accent: tuple[int, int, int]) -> Image.Image:
    """Boost emissive / accent pixels for lit=true."""
    out = img.copy()
    px = out.load()
    ar, ag, ab = accent
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            # bright or strongly tinted toward accent
            bright = r + g + b > 380
            tinted = (abs(r - ar) + abs(g - ag) + abs(b - ab)) < 180 and max(r, g, b) > 90
            if bright or tinted:
                px[x, y] = (
                    min(255, int(r * 1.25 + 20)),
                    min(255, int(g * 1.25 + 20)),
                    min(255, int(b * 1.25 + 20)),
                    255,
                )
    # center sparkle
    put(out, 7, 7, (255, 255, 255, 255))
    put(out, 8, 8, (*accent, 255))
    return out


def bake_fabricator_side(name: str) -> Image.Image:
    cfg = FABRICATOR_ACCENTS[name]
    metal, accent, panel = cfg["metal"], cfg["accent"], cfg["panel"]
    hi = tuple(min(255, c + 40) for c in metal)
    lo = tuple(max(0, c - 30) for c in metal)
    img = recolor(load("iron_block.png"), metal, 0.92)
    fill_rect(img, 0, 0, 15, 0, (*hi, 255))
    fill_rect(img, 0, 15, 15, 15, (*lo, 255))
    fill_rect(img, 2, 2, 13, 13, (*panel, 255))
    if name.endswith("_iii"):
        # purple armored ribs
        for y in range(3, 13):
            put(img, 3, y, (*accent, 255) if y % 2 == 0 else (*lo, 255))
            put(img, 12, y, (*accent, 255) if y % 2 == 0 else (*lo, 255))
        fill_rect(img, 6, 5, 9, 10, (*lo, 255))
        put(img, 7, 7, (*accent, 255))
        put(img, 8, 8, (255, 200, 255, 255))
    elif name.endswith("_ii"):
        # teal vents
        for y in (4, 6, 8, 10):
            for x in range(4, 12):
                put(img, x, y, (*lo, 255))
            for x in range(5, 11):
                put(img, x, y + 1, (*accent, 255) if y in (6, 8) else (*metal, 255))
    else:
        # class I cable ports
        for y in (5, 7, 9):
            for x in range(3, 6):
                put(img, x, y, (*accent, 255))
            for x in range(10, 13):
                put(img, x, y, (*accent, 255))
        fill_rect(img, 6, 4, 9, 11, (*lo, 255))
        for x in range(6, 10):
            put(img, x, 6, (*accent, 255))
    return img


def bake_fabricator_top(name: str) -> Image.Image:
    cfg = FABRICATOR_ACCENTS[name]
    metal, accent, panel = cfg["metal"], cfg["accent"], cfg["panel"]
    hi = tuple(min(255, c + 35) for c in metal)
    lo = tuple(max(0, c - 25) for c in metal)
    img = recolor(load("iron_block.png"), metal, 0.95)
    for i in range(16):
        put(img, i, 0, (*hi, 255))
        put(img, i, 15, (*lo, 255))
        put(img, 0, i, (*hi, 255))
        put(img, 15, i, (*lo, 255))
    fill_rect(img, 2, 2, 13, 13, (*panel, 255))
    if name.endswith("_iii"):
        # circular core plate
        for x in range(16):
            for y in range(16):
                d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
                if 4.5 < d < 6:
                    put(img, x, y, (*accent, 255))
                elif d < 3:
                    put(img, x, y, (255, 180, 255, 255) if d < 1.5 else (*accent, 255))
        for x, y in [(2, 2), (13, 2), (2, 13), (13, 13)]:
            put(img, x, y, (*accent, 255))
    elif name.endswith("_ii"):
        # dual socket marks + center Φ stem
        for ox in (3, 9):
            fill_rect(img, ox, 5, ox + 3, 10, (*lo, 255))
            put(img, ox + 1, 7, (*accent, 255))
            put(img, ox + 2, 8, (*accent, 255))
        for y in range(4, 12):
            put(img, 7, y, (*accent, 255))
            put(img, 8, y, (*accent, 255))
    else:
        # scan grate + single crystal well
        for i in range(3, 13, 2):
            for j in range(3, 13):
                blend(img, i, j, lo, 0.5)
        fill_rect(img, 5, 5, 10, 10, (*lo, 255))
        put(img, 7, 7, (*accent, 255))
        put(img, 8, 8, (200, 245, 255, 255))
        put(img, 7, 8, (*accent, 255))
    return img


def bake_fabricators() -> None:
    for name, concept_name in FABRICATOR_CONCEPTS.items():
        path = SKETCH / concept_name
        if not path.exists():
            raise FileNotFoundError(path)
        front = concept_to_16(path)
        save(f"{name}_front", front)
        save(f"{name}_front_on", lit_front(front, FABRICATOR_ACCENTS[name]["accent"]))
        save(f"{name}_side", bake_fabricator_side(name))
        save(f"{name}_top", bake_fabricator_top(name))
        # also keep a crop preview of the concept face
        PREVIEW.mkdir(parents=True, exist_ok=True)
        crop_face(Image.open(path)).resize((256, 256), Image.Resampling.BOX).save(
            PREVIEW / f"{name}_concept_face.png"
        )


# --- Energy ---

def bake_cores() -> None:
    base = recolor(load("respawn_anchor_side0.png"), (55, 40, 50), 0.95)
    heart = base.copy()
    for x in range(4, 12):
        for y in range(4, 12):
            blend(heart, x, y, (200, 60, 110), 0.55)
    # heart-ish diamond
    for x, y in [(7, 5), (8, 5), (6, 6), (9, 6), (5, 7), (10, 7), (6, 8), (9, 8), (7, 9), (8, 9)]:
        put(heart, x, y, (255, 120, 160, 255))
    put(heart, 7, 7, (255, 200, 220, 255))
    put(heart, 8, 7, (255, 200, 220, 255))
    save("heart_reactor_core", heart)
    heart_on = heart.copy()
    for x in range(5, 11):
        for y in range(5, 11):
            blend(heart_on, x, y, (255, 180, 210), 0.35)
    put(heart_on, 7, 7, (255, 255, 255, 255))
    save("heart_reactor_core_on", heart_on)

    forge = recolor(load("blast_furnace_front.png"), (70, 45, 30), 0.95)
    for x in range(5, 11):
        for y in range(5, 11):
            blend(forge, x, y, (220, 100, 40), 0.5)
    put(forge, 7, 7, (255, 200, 80, 255))
    put(forge, 8, 8, (255, 160, 40, 255))
    save("forge_reactor_core", forge)
    forge_on = forge.copy()
    for x in range(4, 12):
        blend(forge_on, x, 6, (255, 180, 60), 0.6)
        blend(forge_on, x, 7, (255, 120, 30), 0.55)
    put(forge_on, 7, 7, (255, 255, 200, 255))
    save("forge_reactor_core_on", forge_on)

    # reactor_casing: keep prior plating (user preference) — do not overwrite


def bake_spark_sparse() -> None:
    bottom = recolor(load("blast_furnace_top.png"), (45, 50, 65), 0.9)
    for x in range(5, 11):
        for y in range(5, 11):
            blend(bottom, x, y, (60, 70, 90), 0.4)
    put(bottom, 7, 7, (90, 180, 220, 255))
    save("spark_reactor_bottom", bottom)

    top_on = recolor(load("blast_furnace_top.png"), (50, 70, 100), 1.1)
    for x in range(4, 12):
        for y in range(4, 12):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 3:
                blend(top_on, x, y, (100, 220, 255), 0.7)
    put(top_on, 7, 7, (255, 255, 255, 255))
    put(top_on, 8, 7, (200, 250, 255, 255))
    save("spark_reactor_top_on", top_on)


def bake_contactor() -> None:
    base = recolor(load("iron_block.png"), (60, 70, 90), 0.95)
    closed = base.copy()
    for x in range(3, 13):
        blend(closed, x, 7, (90, 200, 230), 0.55)
        blend(closed, x, 8, (70, 160, 200), 0.5)
    put(closed, 7, 7, (180, 240, 255, 255))
    put(closed, 8, 8, (180, 240, 255, 255))
    save("phi_contactor", closed)

    on = closed.copy()
    for x in range(3, 13):
        blend(on, x, 7, (140, 240, 255), 0.7)
    put(on, 7, 7, (255, 255, 255, 255))
    save("phi_contactor_on", on)

    open_ = base.copy()
    for y in range(3, 13):
        blend(open_, 7, y, (40, 45, 55), 0.6)
        blend(open_, 8, y, (40, 45, 55), 0.6)
    # gap
    for x in range(5, 11):
        put(open_, x, 7, (20, 22, 28, 255))
        put(open_, x, 8, (20, 22, 28, 255))
    save("phi_contactor_open", open_)


# --- Artillery ---

def bake_artillery() -> None:
    base = recolor(load("blast_furnace_top.png"), (50, 55, 70), 0.95)
    # mount ring
    for x in range(16):
        for y in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 5.2 <= d <= 6.5:
                blend(base, x, y, (140, 150, 170), 0.65)
            elif d < 3:
                blend(base, x, y, (40, 45, 55), 0.5)
    # yaw marks
    put(base, 7, 2, (90, 200, 255, 255))
    put(base, 8, 2, (90, 200, 255, 255))
    put(base, 7, 13, (90, 200, 255, 255))
    put(base, 2, 7, (90, 200, 255, 255))
    put(base, 13, 7, (90, 200, 255, 255))
    save("phi_artillery_base", base)

    lens = recolor(load("tinted_glass.png"), (40, 90, 130), 1.1)
    for x in range(16):
        for y in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 5:
                blend(lens, x, y, (60, 180, 230), 0.55)
            if d < 2:
                blend(lens, x, y, (140, 230, 255), 0.7)
    # frame
    for i in range(16):
        blend(lens, i, 0, (80, 90, 110), 0.5)
        blend(lens, i, 15, (80, 90, 110), 0.5)
        blend(lens, 0, i, (80, 90, 110), 0.5)
        blend(lens, 15, i, (80, 90, 110), 0.5)
    put(lens, 7, 7, (220, 250, 255, 255))
    put(lens, 8, 8, (180, 240, 255, 255))
    save("phi_beam_lens", lens)

    lens_on = lens.copy()
    for x in range(3, 13):
        for y in range(3, 13):
            blend(lens_on, x, y, (120, 240, 255), 0.45)
    put(lens_on, 7, 7, (255, 255, 255, 255))
    put(lens_on, 8, 7, (255, 255, 255, 255))
    put(lens_on, 7, 8, (200, 255, 255, 255))
    save("phi_beam_lens_on", lens_on)


def main() -> None:
    bake_fabricators()
    bake_cores()
    bake_spark_sparse()
    bake_contactor()
    bake_artillery()

    strip_names = [
        "phi_fabricator_front",
        "phi_fabricator_ii_front",
        "phi_fabricator_iii_front",
        "phi_fabricator_front_on",
        "heart_reactor_core",
        "forge_reactor_core",
        "phi_contactor",
        "phi_artillery_base",
        "phi_beam_lens",
        "phi_beam_lens_on",
        "spark_reactor_top_on",
    ]
    cell = 80
    cols = 4
    rows = 3
    sheet = Image.new("RGBA", (cols * cell + 16, rows * cell + 16), (26, 28, 34, 255))
    for i, n in enumerate(strip_names):
        im = Image.open(TEX / f"{n}.png").convert("RGBA").resize((cell, cell), Image.NEAREST)
        sheet.paste(im, (8 + (i % cols) * cell, 8 + (i // cols) * cell), im)
    sheet.save(PREVIEW / "batch_e_strip_8x.png")
    print("done")


if __name__ == "__main__":
    main()
