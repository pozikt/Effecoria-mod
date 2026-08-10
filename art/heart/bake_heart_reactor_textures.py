"""Bake Heart Reactor hull/casing/core textures from the solid-megablock concept."""

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ART = Path(__file__).resolve().parent
TEX = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures"

OUT = (8, 9, 12, 255)
EDGE = (22, 24, 28, 255)
METAL = (42, 46, 54, 255)
METAL_H = (68, 74, 86, 255)
METAL_L = (30, 33, 40, 255)
RIVET = (140, 148, 160, 255)
RIVET_D = (20, 22, 26, 255)
SEAM = (16, 18, 22, 255)
LED = (40, 210, 230, 255)
LED_D = (20, 90, 110, 255)
GLASS = (18, 78, 98, 255)
GLASS_D = (10, 42, 58, 255)
GLASS_H = (70, 190, 220, 255)
CORE = (200, 250, 255, 255)
CORE_M = (90, 230, 255, 255)
BEAM = (50, 200, 230, 255)
FRAME = (55, 62, 72, 255)
FRAME_H = (95, 105, 120, 255)
COPPER = (72, 58, 48, 255)
COPPER_H = (110, 88, 62, 255)


def px(img: Image.Image, x: int, y: int, c: tuple[int, int, int, int]) -> None:
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), c)


def fill_rect(img: Image.Image, x0: int, y0: int, x1: int, y1: int, c: tuple[int, int, int, int]) -> None:
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px(img, x, y, c)


def hline(img: Image.Image, x0: int, x1: int, y: int, c: tuple[int, int, int, int]) -> None:
    for x in range(x0, x1 + 1):
        px(img, x, y, c)


def vline(img: Image.Image, x: int, y0: int, y1: int, c: tuple[int, int, int, int]) -> None:
    for y in range(y0, y1 + 1):
        px(img, x, y, c)


def rivet(img: Image.Image, x: int, y: int) -> None:
    px(img, x, y, RIVET)
    px(img, x + 1, y, RIVET_D)
    px(img, x, y + 1, RIVET_D)


def make_hull(size: int = 64, lit: bool = False) -> Image.Image:
    """One full megablock face: ~1/3 rim = 26-shell frame language, center = Φ viewport."""
    img = Image.new("RGBA", (size, size), OUT)
    fill_rect(img, 0, 0, size - 1, size - 1, EDGE)
    fill_rect(img, 1, 1, size - 2, size - 2, METAL)

    corner = 12
    for cx, cy in ((1, 1), (size - 1 - corner, 1), (1, size - 1 - corner), (size - 1 - corner, size - 1 - corner)):
        fill_rect(img, cx, cy, cx + corner - 1, cy + corner - 1, METAL_L)
        fill_rect(img, cx + 1, cy + 1, cx + corner - 2, cy + corner - 2, EDGE)
        fill_rect(img, cx + 3, cy + 3, cx + corner - 4, cy + corner - 4, METAL_L)
        fill_rect(img, cx + 5, cy + 5, cx + corner - 6, cy + corner - 6, COPPER if not lit else COPPER_H)
        px(img, cx + 6, cy + 6, RIVET)
        px(img, cx + 7, cy + 6, RIVET_D)
        px(img, cx + corner - 4, cy + 3, LED if lit else LED_D)

    # edge beams spanning between corners (reactor_casing language)
    fill_rect(img, corner, 2, size - 1 - corner, 10, FRAME)
    fill_rect(img, corner, size - 11, size - 1 - corner, size - 3, FRAME)
    fill_rect(img, 2, corner, 10, size - 1 - corner, FRAME)
    fill_rect(img, size - 11, corner, size - 3, size - 1 - corner, FRAME)
    fill_rect(img, corner + 1, 3, size - 2 - corner, 8, METAL_H if lit else METAL)
    fill_rect(img, corner + 1, size - 9, size - 2 - corner, size - 4, METAL_L)
    fill_rect(img, 3, corner + 1, 8, size - 2 - corner, METAL_H if lit else METAL)
    fill_rect(img, size - 9, corner + 1, size - 4, size - 2 - corner, METAL_L)

    hline(img, corner, size - 1 - corner, 11, SEAM)
    hline(img, corner, size - 1 - corner, size - 12, SEAM)
    vline(img, 11, corner, size - 1 - corner, SEAM)
    vline(img, size - 12, corner, size - 1 - corner, SEAM)
    for i in range(corner + 2, size - corner - 1, 5):
        px(img, i, 11, LED if lit else LED_D)
        px(img, i, size - 12, LED if lit else LED_D)
        px(img, 11, i, LED if lit else LED_D)
        px(img, size - 12, i, LED if lit else LED_D)
        rivet(img, i, 5)
        rivet(img, i, size - 7)
        rivet(img, 5, i)
        rivet(img, size - 7, i)

    # Φ-glass viewport (face-center of the 3×3)
    vx0, vy0, vx1, vy1 = 14, 14, size - 15, size - 15
    fill_rect(img, vx0, vy0, vx1, vy1, FRAME_H if lit else FRAME)
    fill_rect(img, vx0 + 1, vy0 + 1, vx1 - 1, vy1 - 1, SEAM)
    fill_rect(img, vx0 + 2, vy0 + 2, vx1 - 2, vy1 - 2, GLASS if lit else GLASS_D)

    cx = (vx0 + vx1) / 2
    cy = (vy0 + vy1) / 2
    for y in range(vx0 + 3, vx1 - 2):
        for x in range(vx0 + 3, vx1 - 2):
            dx = abs(x - cx) / max(1.0, (vx1 - vx0) / 2)
            dy = abs(y - cy) / max(1.0, (vy1 - vy0) / 2)
            d = (dx * dx + dy * dy) ** 0.5
            if d < 0.28:
                c = CORE if lit else CORE_M
            elif d < 0.5:
                c = CORE_M if lit else BEAM
            elif d < 0.78:
                c = BEAM if lit else GLASS_H
            else:
                c = GLASS_H if lit else GLASS
            if (x - vx0) % 5 == 0 or (y - vy0) % 5 == 0:
                c = (max(0, c[0] - 22), max(0, c[1] - 22), max(0, c[2] - 18), 255)
            px(img, x, y, c)

    mid = size // 2
    for t in range(vx0 + 5, vx1 - 4):
        px(img, t, mid, CORE if lit else CORE_M)
        px(img, t, mid - 1, BEAM)
        px(img, t, mid + 1, BEAM)
        px(img, mid, t, CORE if lit else CORE_M)
        px(img, mid - 1, t, BEAM)
        px(img, mid + 1, t, BEAM)
    for i in range(1, 9):
        for s in (1, -1):
            px(img, mid + i, mid + s * i, BEAM if lit else GLASS_H)
            px(img, mid - i, mid + s * i, BEAM if lit else GLASS_H)

    for i in range(0, 14):
        px(img, vx0 + 5 + i, vy0 + 5, (230, 248, 255, 255) if lit else (160, 210, 230, 255))
        if i < 10:
            px(img, vx0 + 5 + i, vy0 + 6, GLASS_H)

    hline(img, 0, size - 1, 0, OUT)
    hline(img, 0, size - 1, size - 1, OUT)
    vline(img, 0, 0, size - 1, OUT)
    vline(img, size - 1, 0, size - 1, OUT)
    return img


def make_casing(size: int = 16) -> Image.Image:
    """Edge/shell material — strict plated panel (Spark-adjacent gunmetal)."""
    img = Image.new("RGBA", (size, size), EDGE)
    fill_rect(img, 0, 0, size - 1, size - 1, EDGE)
    fill_rect(img, 1, 1, size - 2, size - 2, METAL)
    fill_rect(img, 2, 2, size - 3, size - 3, METAL_L)
    fill_rect(img, 3, 3, size - 4, size - 4, METAL)
    # inset hatch
    fill_rect(img, 5, 5, 10, 10, METAL_L)
    fill_rect(img, 6, 6, 9, 9, SEAM)
    hline(img, 3, size - 4, 4, SEAM)
    hline(img, 3, size - 4, 11, SEAM)
    vline(img, 4, 3, size - 4, SEAM)
    vline(img, 11, 3, size - 4, SEAM)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px(img, x, y, RIVET)
        px(img, x + 1 if x < 8 else x - 1, y, RIVET_D)
    px(img, 7, 7, LED_D)
    px(img, 8, 7, LED_D)
    px(img, 7, 8, FRAME_H)
    px(img, 8, 8, FRAME)
    return img


def make_core(size: int = 16, lit: bool = False) -> Image.Image:
    img = Image.new("RGBA", (size, size), EDGE)
    fill_rect(img, 1, 1, size - 2, size - 2, METAL_L)
    fill_rect(img, 2, 2, size - 3, size - 3, FRAME)
    fill_rect(img, 3, 3, size - 4, size - 4, SEAM)
    fill_rect(img, 4, 4, size - 5, size - 5, GLASS if lit else GLASS_D)
    mid = size // 2
    for t in range(5, size - 5):
        px(img, t, mid, CORE if lit else CORE_M)
        px(img, mid, t, CORE if lit else CORE_M)
    px(img, mid, mid, CORE)
    hline(img, 3, size - 4, 3, FRAME_H)
    vline(img, 3, 3, size - 4, FRAME_H)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px(img, x, y, RIVET)
    return img


def main() -> None:
    ART.mkdir(parents=True, exist_ok=True)
    (TEX / "block").mkdir(parents=True, exist_ok=True)
    (TEX / "item").mkdir(parents=True, exist_ok=True)

    hull = make_hull(64, lit=False)
    hull_on = make_hull(64, lit=True)
    casing = make_casing(16)
    core = make_core(16, lit=False)
    core_on = make_core(16, lit=True)

    sheet = Image.new("RGBA", (64 * 2 + 16 + 64 + 12, 64 + 8), (30, 32, 40, 255))
    sheet.paste(hull, (4, 4))
    sheet.paste(hull_on, (72, 4))
    sheet.paste(casing.resize((64, 64), Image.NEAREST), (140, 4))
    sheet.save(ART / "heart_reactor_texture_sheet_preview.png")

    hull.save(TEX / "block" / "heart_reactor_hull.png")
    hull_on.save(TEX / "block" / "heart_reactor_hull_on.png")
    casing.save(TEX / "block" / "reactor_casing.png")
    core.save(TEX / "block" / "heart_reactor_core.png")
    core_on.save(TEX / "block" / "heart_reactor_core_on.png")
    core.save(TEX / "item" / "heart_reactor_core.png")
    casing.save(TEX / "item" / "reactor_casing.png")

    hull.save(ART / "heart_reactor_hull_64.png")
    hull_on.save(ART / "heart_reactor_hull_on_64.png")
    casing.save(ART / "reactor_casing_16.png")
    core.save(ART / "heart_reactor_core_16.png")
    core_on.save(ART / "heart_reactor_core_on_16.png")
    print("baked", hull.size, casing.size, core.size)


if __name__ == "__main__":
    main()
