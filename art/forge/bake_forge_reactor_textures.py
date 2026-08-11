"""Bake Forge Reactor («Кузница») solid-megablock hull + split GUI.

Side faces are 3×4 blocks → 48×64 px sheet (exact aspect).
Top/bottom are 3×3 → 64×64 px sheet (same language as Heart).
Palette: lead/void frame + molten gold viewport (warmer than Heart cyan).
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ART = Path(__file__).resolve().parent
TEX = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures"

OUT = (8, 9, 12, 255)
EDGE = (22, 24, 28, 255)
METAL = (42, 46, 54, 255)
METAL_H = (68, 74, 86, 255)
METAL_L = (30, 33, 40, 255)
VOID = (18, 14, 22, 255)
VOID_H = (36, 28, 42, 255)
RIVET = (140, 148, 160, 255)
RIVET_D = (20, 22, 26, 255)
SEAM = (16, 18, 22, 255)
LED = (255, 180, 40, 255)
LED_D = (90, 55, 18, 255)
GLASS = (72, 42, 18, 255)
GLASS_D = (36, 22, 12, 255)
GLASS_H = (210, 140, 45, 255)
CORE = (255, 245, 200, 255)
CORE_M = (255, 200, 70, 255)
BEAM = (230, 150, 40, 255)
FRAME = (55, 62, 72, 255)
FRAME_H = (95, 105, 120, 255)
COPPER = (72, 58, 48, 255)
COPPER_H = (140, 100, 55, 255)
HAZARD = (160, 120, 40, 255)


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


def hazard_corner(img: Image.Image, x0: int, y0: int, size: int = 10) -> None:
    fill_rect(img, x0, y0, x0 + size - 1, y0 + size - 1, VOID)
    fill_rect(img, x0 + 1, y0 + 1, x0 + size - 2, y0 + size - 2, VOID_H)
    for i in range(2, size - 2):
        px(img, x0 + i, y0 + i, HAZARD)
        px(img, x0 + i, y0 + size - 1 - i, HAZARD)


def molten_viewport(img: Image.Image, vx0: int, vy0: int, vx1: int, vy1: int, lit: bool) -> None:
    fill_rect(img, vx0, vy0, vx1, vy1, FRAME_H if lit else FRAME)
    fill_rect(img, vx0 + 1, vy0 + 1, vx1 - 1, vy1 - 1, SEAM)
    fill_rect(img, vx0 + 2, vy0 + 2, vx1 - 2, vy1 - 2, GLASS if lit else GLASS_D)

    cx = (vx0 + vx1) / 2
    cy = (vy0 + vy1) / 2
    rx = max(1.0, (vx1 - vx0) / 2)
    ry = max(1.0, (vy1 - vy0) / 2)
    for y in range(vy0 + 3, vy1 - 2):
        for x in range(vx0 + 3, vx1 - 2):
            dx = abs(x - cx) / rx
            dy = abs(y - cy) / ry
            d = (dx * dx + dy * dy) ** 0.5
            if d < 0.22:
                c = CORE if lit else CORE_M
            elif d < 0.42:
                c = CORE_M if lit else BEAM
            elif d < 0.68:
                c = BEAM if lit else GLASS_H
            else:
                c = GLASS_H if lit else GLASS
            if (x - vx0) % 5 == 0 or (y - vy0) % 5 == 0:
                c = (max(0, c[0] - 28), max(0, c[1] - 30), max(0, c[2] - 10), 255)
            px(img, x, y, c)

    mid_x = (vx0 + vx1) // 2
    mid_y = (vy0 + vy1) // 2
    for t in range(vx0 + 5, vx1 - 4):
        px(img, t, mid_y, CORE if lit else CORE_M)
        px(img, t, mid_y - 1, BEAM)
        px(img, t, mid_y + 1, BEAM)
    for t in range(vy0 + 5, vy1 - 4):
        px(img, mid_x, t, CORE if lit else CORE_M)
        px(img, mid_x - 1, t, BEAM)
        px(img, mid_x + 1, t, BEAM)
    for i in range(1, 10):
        for s in (1, -1):
            px(img, mid_x + i, mid_y + s * i, BEAM if lit else GLASS_H)
            px(img, mid_x - i, mid_y + s * i, BEAM if lit else GLASS_H)
    for i in range(0, 12):
        px(img, vx0 + 5 + i, vy0 + 5, (255, 250, 220, 255) if lit else (220, 170, 80, 255))
        if i < 8:
            px(img, vx0 + 5 + i, vy0 + 6, GLASS_H)


def make_side(w: int = 48, h: int = 64, lit: bool = False) -> Image.Image:
    """One unbroken 3×4 megablock face."""
    img = Image.new("RGBA", (w, h), OUT)
    fill_rect(img, 0, 0, w - 1, h - 1, EDGE)
    fill_rect(img, 1, 1, w - 2, h - 2, METAL)

    # void-obsidian corner pillars (full height)
    pillar = 9
    fill_rect(img, 1, 1, pillar, h - 2, VOID)
    fill_rect(img, w - 1 - pillar, 1, w - 2, h - 2, VOID)
    fill_rect(img, 2, 2, pillar - 1, h - 3, VOID_H)
    fill_rect(img, w - pillar, 2, w - 3, h - 3, VOID_H)
    for y in range(4, h - 4, 6):
        rivet(img, 4, y)
        rivet(img, w - 6, y)
        px(img, 6, y + 1, LED if lit else LED_D)
        px(img, w - 7, y + 1, LED if lit else LED_D)

    hazard_corner(img, 2, 2, 8)
    hazard_corner(img, w - 10, 2, 8)
    hazard_corner(img, 2, h - 10, 8)
    hazard_corner(img, w - 10, h - 10, 8)

    # lead floor / roof bands
    fill_rect(img, pillar + 1, 2, w - 2 - pillar, 9, FRAME)
    fill_rect(img, pillar + 1, h - 10, w - 2 - pillar, h - 3, FRAME)
    fill_rect(img, pillar + 2, 3, w - 3 - pillar, 7, METAL_H if lit else METAL)
    fill_rect(img, pillar + 2, h - 8, w - 3 - pillar, h - 4, METAL_L)
    hline(img, pillar + 1, w - 2 - pillar, 10, SEAM)
    hline(img, pillar + 1, w - 2 - pillar, h - 11, SEAM)
    for i in range(pillar + 3, w - pillar - 2, 5):
        px(img, i, 10, LED if lit else LED_D)
        px(img, i, h - 11, LED if lit else LED_D)
        rivet(img, i, 5)
        rivet(img, i, h - 7)

    # mid structural rails
    fill_rect(img, pillar + 1, h // 2 - 2, w - 2 - pillar, h // 2 + 1, FRAME)
    hline(img, pillar + 1, w - 2 - pillar, h // 2 - 3, SEAM)
    hline(img, pillar + 1, w - 2 - pillar, h // 2 + 2, SEAM)

    # tall Φ-glass forge viewport
    vx0, vy0 = pillar + 3, 13
    vx1, vy1 = w - pillar - 4, h - 14
    molten_viewport(img, vx0, vy0, vx1, vy1, lit)

    hline(img, 0, w - 1, 0, OUT)
    hline(img, 0, w - 1, h - 1, OUT)
    vline(img, 0, 0, h - 1, OUT)
    vline(img, w - 1, 0, h - 1, OUT)
    return img


def make_top(size: int = 64, lit: bool = False) -> Image.Image:
    """3×3 roof / floor face — lead plating, central hatch."""
    img = Image.new("RGBA", (size, size), OUT)
    fill_rect(img, 0, 0, size - 1, size - 1, EDGE)
    fill_rect(img, 1, 1, size - 2, size - 2, METAL)

    corner = 11
    for cx, cy in ((1, 1), (size - 1 - corner, 1), (1, size - 1 - corner), (size - 1 - corner, size - 1 - corner)):
        fill_rect(img, cx, cy, cx + corner - 1, cy + corner - 1, VOID)
        fill_rect(img, cx + 1, cy + 1, cx + corner - 2, cy + corner - 2, VOID_H)
        fill_rect(img, cx + 3, cy + 3, cx + corner - 4, cy + corner - 4, COPPER if not lit else COPPER_H)
        px(img, cx + 5, cy + 5, RIVET)
        px(img, cx + corner - 4, cy + 3, LED if lit else LED_D)

    fill_rect(img, corner, 2, size - 1 - corner, 9, FRAME)
    fill_rect(img, corner, size - 10, size - 1 - corner, size - 3, FRAME)
    fill_rect(img, 2, corner, 9, size - 1 - corner, FRAME)
    fill_rect(img, size - 10, corner, size - 3, size - 1 - corner, FRAME)
    for i in range(corner + 2, size - corner - 1, 5):
        rivet(img, i, 5)
        rivet(img, i, size - 7)
        rivet(img, 5, i)
        rivet(img, size - 7, i)
        px(img, i, 10, LED if lit else LED_D)
        px(img, i, size - 11, LED if lit else LED_D)

    # hatch / gold ring
    mid = size // 2
    r_outer = 16
    fill_rect(img, mid - r_outer, mid - r_outer, mid + r_outer, mid + r_outer, FRAME_H if lit else FRAME)
    fill_rect(img, mid - r_outer + 1, mid - r_outer + 1, mid + r_outer - 1, mid + r_outer - 1, SEAM)
    molten_viewport(img, mid - 12, mid - 12, mid + 12, mid + 12, lit)

    hline(img, 0, size - 1, 0, OUT)
    hline(img, 0, size - 1, size - 1, OUT)
    vline(img, 0, 0, size - 1, OUT)
    vline(img, size - 1, 0, size - 1, OUT)
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
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px(img, x, y, RIVET)
    return img


def slot(draw: ImageDraw.ImageDraw, x: int, y: int, accent: tuple[int, int, int, int] | None = None) -> None:
    draw.rectangle([x, y, x + 17, y + 17], fill=(55, 55, 55, 255), outline=(25, 25, 25, 255))
    draw.rectangle([x + 1, y + 1, x + 16, y + 16], fill=(139, 139, 139, 255))
    if accent:
        draw.rectangle([x, y + 16, x + 17, y + 17], fill=accent)


def make_gui() -> Image.Image:
    """256×256 panel: left forge bay, right reactor controls, vanilla inv below."""
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # outer panel 220×166
    W, H = 220, 166
    draw.rectangle([0, 0, W - 1, H - 1], fill=(198, 198, 198, 255), outline=(55, 55, 55, 255))
    draw.rectangle([1, 1, W - 2, H - 2], outline=(255, 255, 255, 255))
    draw.rectangle([2, 2, W - 3, H - 3], outline=(85, 85, 85, 255))

    # left forge zone
    draw.rectangle([6, 14, 108, 78], fill=(170, 170, 175, 255), outline=(90, 90, 95, 255))
    draw.rectangle([7, 15, 107, 77], outline=(210, 210, 215, 255))
    # right control zone
    draw.rectangle([112, 14, 213, 78], fill=(155, 158, 165, 255), outline=(70, 75, 85, 255))
    draw.rectangle([113, 15, 212, 77], outline=(190, 195, 205, 255))
    # divider
    draw.line([(110, 14), (110, 78)], fill=(60, 60, 65, 255))
    draw.line([(111, 14), (111, 78)], fill=(230, 230, 235, 255))

    # gold accents under forge header
    draw.rectangle([8, 16, 106, 18], fill=(200, 150, 50, 255))
    draw.rectangle([114, 16, 211, 18], fill=(80, 160, 180, 255))

    # slots painted for reference (actual hitboxes come from menu coords)
    # fuel ×2 + catalyst
    slot(draw, 16, 22, (255, 170, 40, 255))
    slot(draw, 36, 22, (255, 170, 40, 255))
    slot(draw, 64, 22, (180, 80, 220, 255))
    # inputs → output
    slot(draw, 20, 52, None)
    slot(draw, 40, 52, None)
    # arrow stub
    draw.rectangle([60, 56, 78, 64], fill=(100, 100, 110, 255))
    slot(draw, 82, 52, (80, 200, 120, 255))

    # gauge tracks (right)
    for gy, col in ((24, (232, 176, 32)), (34, (255, 85, 51)), (44, (170, 34, 255))):
        draw.rectangle([120, gy, 206, gy + 5], fill=(40, 40, 45, 255), outline=(20, 20, 22, 255))
        draw.rectangle([121, gy + 1, 150, gy + 4], fill=col)

    # button wells
    for by in (54, 64, 72):
        draw.rectangle([120, by, 206, by + 8], fill=(120, 120, 125, 255), outline=(50, 50, 55, 255))

    # player inventory wells (offset for 220-wide panel: x=29)
    inv_x = 29
    for row in range(3):
        for col in range(9):
            slot(draw, inv_x + col * 18, 84 + row * 18)
    for col in range(9):
        slot(draw, inv_x + col * 18, 142)

    return img


def main() -> None:
    ART.mkdir(parents=True, exist_ok=True)
    (TEX / "block").mkdir(parents=True, exist_ok=True)
    (TEX / "item").mkdir(parents=True, exist_ok=True)
    (TEX / "gui").mkdir(parents=True, exist_ok=True)

    side = make_side(48, 64, lit=False)
    side_on = make_side(48, 64, lit=True)
    top = make_top(64, lit=False)
    top_on = make_top(64, lit=True)
    core = make_core(16, lit=False)
    core_on = make_core(16, lit=True)
    gui = make_gui()

    # shipping
    side.save(TEX / "block" / "forge_reactor_hull.png")
    side_on.save(TEX / "block" / "forge_reactor_hull_on.png")
    top.save(TEX / "block" / "forge_reactor_hull_top.png")
    top_on.save(TEX / "block" / "forge_reactor_hull_top_on.png")
    # keep square alias for any old refs / item preview
    side.resize((64, 64), Image.NEAREST).save(ART / "forge_reactor_hull_side_preview_64.png")
    core.save(TEX / "block" / "forge_reactor_core.png")
    core_on.save(TEX / "block" / "forge_reactor_core_on.png")
    core.save(TEX / "item" / "forge_reactor_core.png")
    gui.save(TEX / "gui" / "forge_reactor.png")

    # art previews
    side.save(ART / "forge_reactor_hull_side_48x64.png")
    side_on.save(ART / "forge_reactor_hull_side_on_48x64.png")
    top.save(ART / "forge_reactor_hull_top_64.png")
    top_on.save(ART / "forge_reactor_hull_top_on_64.png")
    core.save(ART / "forge_reactor_core_16.png")
    gui.save(ART / "forge_reactor_gui_preview.png")

    sheet = Image.new("RGBA", (48 + 64 + 48 + 24, 72), (28, 30, 36, 255))
    sheet.paste(side, (4, 4))
    sheet.paste(side_on, (56, 4))
    sheet.paste(top.resize((48, 48), Image.NEAREST), (108, 12))
    sheet.save(ART / "forge_reactor_texture_sheet_preview.png")
    print("baked side", side.size, "top", top.size, "gui", gui.size)


if __name__ == "__main__":
    main()
