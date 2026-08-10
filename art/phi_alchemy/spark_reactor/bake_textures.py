"""Downscale Spark Reactor concept refs into crisp 16x16 Minecraft block faces.

Pipeline: crop content -> box-downsample -> palette quantize -> hand polish key pixels.
Writes art/ previews and assets/effecoria/textures/block/ shipping textures.
"""
from __future__ import annotations

from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[3]  # Effecoria-mod
ART = ROOT / "art" / "phi_alchemy" / "spark_reactor"
OUT = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "block"
ART.mkdir(parents=True, exist_ok=True)
OUT.mkdir(parents=True, exist_ok=True)

# Lead / mithril palette (vanilla-friendly)
LEAD0 = (38, 40, 44, 255)
LEAD1 = (58, 62, 68, 255)
LEAD2 = (78, 84, 92, 255)
LEAD3 = (102, 110, 120, 255)
LEAD4 = (130, 138, 148, 255)
EDGE = (28, 30, 34, 255)
SLOT = (22, 24, 28, 255)
SLOT_LIP = (48, 52, 58, 255)
GEM_OFF = (42, 58, 78, 255)
GEM_ON = (70, 190, 255, 255)
GEM_CORE = (200, 240, 255, 255)
CONTACT_RING = (70, 100, 130, 255)
CONTACT_MID = (55, 150, 220, 255)
CONTACT_HOT = (140, 220, 255, 255)
CONTACT_OFF = (55, 70, 90, 255)
BOLT = (150, 156, 164, 255)


def crop_square_content(im: Image.Image) -> Image.Image:
    im = im.convert("RGBA")
    # Prefer center square crop — refs are already square faces
    w, h = im.size
    side = min(w, h)
    left = (w - side) // 2
    top = (h - side) // 2
    return im.crop((left, top, left + side, top + side))


def down_16(im: Image.Image) -> Image.Image:
    """Aggressive average downsample for silhouette, then nearest for pixel feel."""
    sq = crop_square_content(im)
    mid = sq.resize((32, 32), Image.Resampling.BOX)
    return mid.resize((16, 16), Image.Resampling.NEAREST)


def paint_frame(px, bolts=True):
    for i in range(16):
        px[i, 0] = EDGE
        px[i, 15] = EDGE
        px[0, i] = EDGE
        px[15, i] = EDGE
    if bolts:
        for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
            px[x, y] = BOLT


def paint_fins(px, x0=1, x1=15, y0=1, y1=15, shift=0):
    for y in range(y0, y1):
        for x in range(x0, x1):
            band = (x + shift) % 3
            if band == 0:
                px[x, y] = LEAD1
            elif band == 1:
                px[x, y] = LEAD3
            else:
                px[x, y] = LEAD2
            # horizontal shading
            if y in (2, 8, 13):
                r, g, b, a = px[x, y]
                px[x, y] = (max(0, r - 12), max(0, g - 12), max(0, b - 10), a)


def make_front(lit: bool) -> Image.Image:
    img = Image.new("RGBA", (16, 16), LEAD2)
    px = img.load()
    paint_fins(px)
    paint_frame(px)
    # fuel hatch
    for y in range(10, 14):
        for x in range(5, 11):
            px[x, y] = SLOT if y > 10 and x not in (5, 10) else SLOT_LIP
    px[5, 10] = LEAD1
    px[10, 10] = LEAD1
    px[7, 11] = LEAD3  # handle hint
    px[8, 11] = LEAD4
    # indicator gem
    gx, gy = 12, 3
    if lit:
        px[gx, gy] = GEM_CORE
        px[gx + 1, gy] = GEM_ON
        px[gx, gy + 1] = GEM_ON
        px[gx + 1, gy + 1] = CONTACT_MID
        px[gx - 1, gy] = CONTACT_RING
        px[gx, gy - 1] = CONTACT_RING
    else:
        px[gx, gy] = GEM_OFF
        px[gx + 1, gy] = CONTACT_OFF
        px[gx, gy + 1] = CONTACT_OFF
        px[gx + 1, gy + 1] = LEAD1
    return img


def make_side(lit: bool) -> Image.Image:
    img = Image.new("RGBA", (16, 16), LEAD2)
    px = img.load()
    paint_fins(px, shift=1)
    paint_frame(px)
    # subtle side pipe / vent when lit
    if lit:
        for y in range(6, 10):
            px[2, y] = CONTACT_RING if y % 2 == 0 else CONTACT_MID
    return img


def make_top(lit: bool) -> Image.Image:
    img = Image.new("RGBA", (16, 16), LEAD2)
    px = img.load()
    for y in range(16):
        for x in range(16):
            # machined plate
            px[x, y] = LEAD2 if (x + y) % 2 == 0 else LEAD1
    paint_frame(px)
    # outer ring plate
    for y in range(3, 13):
        for x in range(3, 13):
            dx, dy = x - 7.5, y - 7.5
            d = (dx * dx + dy * dy) ** 0.5
            if d <= 5.4:
                if lit:
                    if d <= 1.6:
                        px[x, y] = GEM_CORE
                    elif d <= 3.0:
                        px[x, y] = CONTACT_HOT
                    elif d <= 4.2:
                        px[x, y] = CONTACT_MID
                    else:
                        px[x, y] = CONTACT_RING
                else:
                    if d <= 1.6:
                        px[x, y] = LEAD4
                    elif d <= 3.0:
                        px[x, y] = CONTACT_OFF
                    elif d <= 4.2:
                        px[x, y] = LEAD2
                    else:
                        px[x, y] = LEAD1
    # cross contact marks
    if lit:
        px[7, 5] = GEM_CORE
        px[8, 5] = GEM_CORE
        px[7, 10] = GEM_CORE
        px[8, 10] = GEM_CORE
        px[5, 7] = GEM_CORE
        px[5, 8] = GEM_CORE
        px[10, 7] = GEM_CORE
        px[10, 8] = GEM_CORE
    return img


def make_bottom() -> Image.Image:
    img = Image.new("RGBA", (16, 16), LEAD1)
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = LEAD0 if (x // 2 + y // 2) % 2 == 0 else LEAD1
    paint_frame(px)
    # center plate
    for y in range(4, 12):
        for x in range(4, 12):
            px[x, y] = LEAD2 if (x + y) % 2 else LEAD1
    return img


def save(name: str, img: Image.Image):
    img.save(OUT / name)
    # 8x preview for art review
    prev = img.resize((128, 128), Image.Resampling.NEAREST)
    prev.save(ART / f"preview_{name}")
    print("wrote", name, img.size)


def blend_ref_hint(painted: Image.Image, ref_path: Path, amount: float = 0.22) -> Image.Image:
    """Lightly borrow color from downscaled AI ref without muddying silhouette."""
    if not ref_path.exists():
        return painted
    ref = down_16(Image.open(ref_path))
    out = painted.copy()
    a = out.load()
    b = ref.load()
    for y in range(16):
        for x in range(16):
            pr, pg, pb, pa = a[x, y]
            rr, rg, rb, ra = b[x, y]
            # keep edge/frame crisp
            if x in (0, 15) or y in (0, 15):
                continue
            nr = int(pr * (1 - amount) + rr * amount)
            ng = int(pg * (1 - amount) + rg * amount)
            nb = int(pb * (1 - amount) + rb * amount)
            a[x, y] = (nr, ng, nb, 255)
    return out


def main():
    # Keep AI downs as reference dumps
    for src, dst in (
        ("front_ref.png", "ai_down_front.png"),
        ("side_ref.png", "ai_down_side.png"),
        ("top_ref.png", "ai_down_top.png"),
    ):
        p = ART / src
        if p.exists():
            d = down_16(Image.open(p))
            d.save(ART / dst)
            d.resize((128, 128), Image.Resampling.NEAREST).save(ART / f"preview_{dst}")

    front = blend_ref_hint(make_front(False), ART / "front_ref.png")
    front_on = blend_ref_hint(make_front(True), ART / "front_ref.png", 0.12)
    # force lit gem brighter after blend
    px = front_on.load()
    px[12, 3] = GEM_CORE
    px[13, 3] = GEM_ON
    px[12, 4] = GEM_ON
    px[13, 4] = CONTACT_MID

    side = blend_ref_hint(make_side(False), ART / "side_ref.png")
    side_on = blend_ref_hint(make_side(True), ART / "side_ref.png", 0.12)
    top = blend_ref_hint(make_top(False), ART / "top_ref.png", 0.18)
    top_on = make_top(True)  # keep glow clean
    bottom = make_bottom()

    # Ship: front used as primary side for cube; also keep distinct side/front names
    save("spark_reactor_front.png", front)
    save("spark_reactor_front_on.png", front_on)
    save("spark_reactor_side.png", side)
    save("spark_reactor_side_on.png", side_on)
    save("spark_reactor_top.png", top)
    save("spark_reactor_top_on.png", top_on)
    save("spark_reactor_bottom.png", bottom)
    # legacy aliases used by old models
    save("spark_reactor.png", front)
    save("spark_reactor_on.png", front_on)

    # contact sheet
    sheet = Image.new("RGBA", (16 * 5 + 8, 16 + 4), (0, 0, 0, 255))
    for i, im in enumerate((front, side, top, bottom, front_on)):
        sheet.paste(im, (2 + i * 17, 2))
    sheet.resize((sheet.width * 8, sheet.height * 8), Image.Resampling.NEAREST).save(
        ART / "faces_16_sheet_8x.png"
    )
    print("done ->", ART)


if __name__ == "__main__":
    main()
