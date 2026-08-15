"""Bake original Φ-fabricator GUI atlas (256×256, panel 176×166)."""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
GUI_PATH = ROOT / "src/main/resources/assets/effecoria/textures/gui/phi_fabricator.png"
PREVIEW = ROOT / "art/phi_fabricator_gui_preview.png"

# Φ cyan technomagic palette (distinct from mortar gray / damper purple)
PANEL = (168, 188, 198, 255)
PANEL_HI = (220, 236, 242, 255)
PANEL_LO = (70, 92, 104, 255)
INNER = (120, 148, 162, 255)
INNER_HI = (150, 178, 192, 255)
WELL = (28, 42, 52, 255)
SLOT = (92, 112, 124, 255)
SLOT_EDGE = (36, 48, 58, 255)
ACCENT = (70, 190, 230, 255)
ACCENT_DIM = (40, 110, 140, 255)
SCAN = (180, 120, 220, 255)
MAT = (90, 200, 160, 255)
OUT_ACCENT = (240, 200, 80, 255)
POWER_TRACK = (24, 36, 44, 255)
ARROW = (88, 108, 120, 255)


def slot(d: ImageDraw.ImageDraw, x: int, y: int, accent=None) -> None:
    d.rectangle([x - 1, y - 1, x + 16, y + 16], fill=SLOT_EDGE)
    d.rectangle([x, y, x + 15, y + 15], fill=SLOT)
    if accent is not None:
        d.rectangle([x, y + 15, x + 15, y + 16], fill=accent)


def bevel_rect(d: ImageDraw.ImageDraw, x0, y0, x1, y1, fill, hi, lo) -> None:
    d.rectangle([x0, y0, x1, y1], fill=fill)
    d.line([(x0, y0), (x1, y0), (x0, y0), (x0, y1)], fill=hi)
    d.line([(x1, y0), (x1, y1), (x0, y1), (x1, y1)], fill=lo)


def make_gui() -> Image.Image:
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Outer panel
    bevel_rect(d, 0, 0, 175, 165, PANEL, PANEL_HI, PANEL_LO)
    d.rectangle([1, 1, 174, 164], outline=PANEL_HI)

    # Machine work area (top)
    bevel_rect(d, 6, 14, 169, 78, INNER, INNER_HI, PANEL_LO)
    d.rectangle([7, 15, 168, 77], outline=(200, 220, 230, 255))
    # cyan header strip
    d.rectangle([8, 16, 167, 19], fill=ACCENT_DIM)
    d.rectangle([8, 16, 80, 19], fill=ACCENT)

    # Left: memory column (program + Φ-write well + scan)
    bevel_rect(d, 10, 22, 52, 74, WELL, ACCENT_DIM, PANEL_LO)
    d.rectangle([11, 23, 51, 73], outline=ACCENT_DIM)
    slot(d, 23, 24, ACCENT)
    # write well (clickable in screen — no vanilla button chrome)
    bevel_rect(d, 16, 44, 46, 56, (55, 80, 95, 255), ACCENT, PANEL_LO)
    d.rectangle([17, 45, 45, 55], fill=(40, 68, 84, 255))
    d.ellipse([27, 46, 35, 54], outline=ACCENT)
    d.line([(31, 46), (31, 54)], fill=ACCENT)
    d.line([(28, 50), (34, 50)], fill=ACCENT)
    slot(d, 23, 58, SCAN)

    # Center: materials tray
    bevel_rect(d, 58, 26, 124, 66, WELL, MAT, PANEL_LO)
    d.rectangle([59, 27, 123, 65], outline=(60, 140, 120, 255))
    slot(d, 66, 35, MAT)
    slot(d, 84, 35, MAT)
    slot(d, 102, 35, MAT)
    for x in (73, 91, 109):
        d.rectangle([x, 54, x + 2, 56], fill=ACCENT_DIM)

    # Progress arrow track
    ax, ay = 126, 38
    d.rectangle([ax, ay + 4, ax + 14, ay + 10], fill=ARROW)
    d.polygon(
        [(ax + 14, ay + 1), (ax + 22, ay + 7), (ax + 14, ay + 13)],
        fill=ARROW,
    )
    d.rectangle([ax + 1, ay + 5, ax + 13, ay + 9], fill=WELL)
    d.polygon(
        [(ax + 14, ay + 3), (ax + 20, ay + 7), (ax + 14, ay + 11)],
        fill=WELL,
    )

    # Output well
    bevel_rect(d, 150, 26, 170, 66, WELL, OUT_ACCENT, PANEL_LO)
    d.rectangle([151, 27, 169, 65], outline=(180, 150, 40, 255))
    slot(d, 152, 35, OUT_ACCENT)

    # Power gem track
    bevel_rect(d, 152, 18, 170, 24, POWER_TRACK, ACCENT_DIM, PANEL_LO)
    d.rectangle([153, 19, 161, 23], fill=ACCENT_DIM)

    # Divider above player inv
    d.line([(6, 80), (169, 80)], fill=PANEL_LO)
    d.line([(6, 81), (169, 81)], fill=PANEL_HI)

    # Player inventory + hotbar
    for row in range(3):
        for col in range(9):
            slot(d, 8 + col * 18, 84 + row * 18)
    for col in range(9):
        slot(d, 8 + col * 18, 142)

    return img


def main() -> None:
    GUI_PATH.parent.mkdir(parents=True, exist_ok=True)
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    gui = make_gui()
    gui.save(GUI_PATH)
    # readable preview
    preview = gui.crop((0, 0, 176, 166)).resize((176 * 3, 166 * 3), Image.NEAREST)
    preview.save(PREVIEW)
    print("wrote", GUI_PATH, "and", PREVIEW)


if __name__ == "__main__":
    main()
