"""Bake Batch A item textures from vanilla bases (silhouette preserved).

Writes:
  src/main/resources/assets/effecoria/textures/item/*.png
  art/items/for_artist/*_8x.png
"""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageEnhance, ImageOps

ROOT = Path(__file__).resolve().parents[2]
REF = ROOT / "art" / "items" / "vanilla_refs_batch_a"
OUT = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "item"
PREVIEW = ROOT / "art" / "items" / "for_artist"


def load(name: str) -> Image.Image:
    return Image.open(REF / name).convert("RGBA")


def tint_by_luma(img: Image.Image, tint: tuple[int, int, int], bright: float = 1.1) -> Image.Image:
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
                int(min(255, tr * v * 1.2)),
                int(min(255, tg * v * 1.2)),
                int(min(255, tb * v * 1.2)),
                aa,
            )
    return out


def map_hsv_shift(
    img: Image.Image,
    *,
    keep_dark: bool = True,
    hue_shift: float = 0.0,
    sat_mul: float = 1.0,
    val_mul: float = 1.0,
    only_if=None,
) -> Image.Image:
    """Pixel HSV remap. only_if(r,g,b,a) -> bool selects pixels to transform."""
    import colorsys

    out = img.copy()
    px = out.load()
    for y in range(out.size[1]):
        for x in range(out.size[0]):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            if only_if is not None and not only_if(r, g, b, a):
                continue
            if keep_dark and r + g + b < 40:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            h = (h + hue_shift) % 1.0
            s = min(1.0, s * sat_mul)
            v = min(1.0, v * val_mul)
            nr, ng, nb = colorsys.hsv_to_rgb(h, s, v)
            px[x, y] = (int(nr * 255), int(ng * 255), int(nb * 255), a)
    return out


def sparkle(img: Image.Image, color: tuple[int, int, int], positions: list[tuple[int, int]]) -> Image.Image:
    out = img.copy()
    px = out.load()
    for x, y in positions:
        if 0 <= x < out.size[0] and 0 <= y < out.size[1] and px[x, y][3] > 0:
            px[x, y] = (*color, 255)
    return out


def save(name: str, img: Image.Image) -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    PREVIEW.mkdir(parents=True, exist_ok=True)
    img.save(OUT / f"{name}.png")
    img.resize((img.size[0] * 8, img.size[1] * 8), Image.NEAREST).save(PREVIEW / f"{name}_8x.png")
    print("wrote", name, "opaque_colors", len({p for p in img.getdata() if p[3] > 0}))


def bake_fireflower() -> None:
    # Torchflower silhouette; keep fiery bloom, Φ-purple only on leaf tips.
    base = load("torchflower.png")
    out = base.copy()
    px = out.load()
    for y in range(out.size[1]):
        for x in range(out.size[0]):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            # Leaf edge: teal/green fringe → violet
            if g > r + 20 and g > b and y > 6 and (r + g + b) > 80:
                if x <= 3 or x >= 12:
                    px[x, y] = (min(255, r + 40), max(0, g - 30), min(255, b + 80), a)
            # Bright tip highlight → soft magenta spark (1–2 px)
            if y <= 2 and r > 200 and g > 180:
                px[x, y] = (220, 90, 255, a)
    save("fireflower", out)


def bake_phi_nectar() -> None:
    # Honey bottle + cyan Φ sparkles on highlights
    base = load("honey_bottle.png")
    out = map_hsv_shift(base, hue_shift=-0.02, sat_mul=1.1, val_mul=1.05)
    out = sparkle(out, (90, 230, 255), [(5, 7), (6, 9), (9, 8), (7, 11)])
    save("phi_nectar", out)


def bake_lonver_blood() -> None:
    # Honey bottle → deep Lonver maroon (preserve glass + cork)
    base = load("honey_bottle.png")
    out = Image.new("RGBA", base.size)
    sp, op = base.load(), out.load()
    for y in range(base.size[1]):
        for x in range(base.size[0]):
            r, g, b, a = sp[x, y]
            if a == 0:
                op[x, y] = (0, 0, 0, 0)
                continue
            # Glass / cork highlights
            if r > 220 and g > 200 and b > 160:
                op[x, y] = (230, 235, 245, a)
                continue
            if r > 160 and g < 90 and b < 90 and y < 5:
                op[x, y] = (130, 55, 45, a)  # cork
                continue
            # Liquid body → maroon with purple lowlights
            v = (0.3 * r + 0.5 * g + 0.2 * b) / 255.0
            op[x, y] = (
                int(min(255, 150 * v + 20)),
                int(min(255, 28 * v)),
                int(min(255, 55 * v + 10)),
                a,
            )
    out = sparkle(out, (170, 80, 200), [(6, 10), (8, 12)])
    save("lonver_blood_vial", out)


def bake_omega_dust() -> None:
    # Redstone pile → Ω purple (HSV keep grain)
    base = load("redstone.png")
    out = map_hsv_shift(base, hue_shift=0.72, sat_mul=1.2, val_mul=1.0)
    out = sparkle(out, (255, 100, 230), [(6, 7), (9, 9), (7, 11), (10, 6)])
    save("omega_dust", out)


def bake_essence_glue() -> None:
    # Slime ball → Φ adhesive
    base = load("slime_ball.png")
    out = map_hsv_shift(base, hue_shift=0.55, sat_mul=1.15, val_mul=1.05)
    src = base.load()
    px = out.load()
    for y in range(out.size[1]):
        for x in range(out.size[0]):
            r, g, b, a = src[x, y]
            if a == 0:
                continue
            if r > 200 and g > 220:
                px[x, y] = (180, 235, 255, a)
    save("essence_glue", out)


def bake_psi_key() -> None:
    # Trial key → silver + cyan Ψ glow (replace red core)
    base = load("trial_key.png")
    out = Image.new("RGBA", base.size)
    sp, op = base.load(), out.load()
    for y in range(base.size[1]):
        for x in range(base.size[0]):
            r, g, b, a = sp[x, y]
            if a == 0:
                op[x, y] = (0, 0, 0, 0)
                continue
            # Warm/red glow shaft → cyan Φ
            if r > 140 and g < 100:
                t = r / 255.0
                op[x, y] = (int(60 * t), int(210 * t), int(255 * t), a)
            elif r > 160 and g > 140 and b > 140:
                # bright metal → colder silver
                v = (r + g + b) / (3 * 255.0)
                op[x, y] = (int(200 * v), int(210 * v), int(230 * v), a)
            else:
                # dark metal stay cool grey
                v = (r + g + b) / (3 * 255.0)
                op[x, y] = (int(90 * v + 40), int(95 * v + 45), int(110 * v + 50), a)
    save("psi_key", out)


def main() -> None:
    bake_fireflower()
    bake_phi_nectar()
    bake_lonver_blood()
    bake_omega_dust()
    bake_essence_glue()
    bake_psi_key()
    print("done ->", OUT)


if __name__ == "__main__":
    main()
