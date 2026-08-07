"""Generate vanilla-format (256x256) alchemy GUI panels.

Minecraft GuiGraphics.blit(texture, x, y, u, v, w, h) assumes texture size 256x256.
Content lives in the top-left 176x166, matching AbstractContainerScreen defaults.
Slot ITEM positions (menu coords) are the top-left of the 16x16 item icon.
Painted frames are 18x18 at (itemX - 1, itemY - 1) — same as vanilla furnace.png.
"""
from PIL import Image, ImageDraw
from pathlib import Path

OUT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/effecoria/textures/gui"
OUT.mkdir(parents=True, exist_ok=True)

TEX = 256
PANEL_W, PANEL_H = 176, 166
INV_ITEM_Y = 84  # vanilla player inventory first row (item coord)

# Vanilla furnace palette — soft greys, not near-black wells
PANEL = (198, 198, 198, 255)
PANEL_DARK = (139, 139, 139, 255)
EDGE_L = (255, 255, 255, 255)
EDGE_D = (55, 55, 55, 255)
# Vanilla slot well is ~#8B8B8B face with darker inset (~#55), never #000/#080808
SLOT_FACE = (139, 139, 139, 255)
SLOT_INNER = (110, 110, 110, 255)
SLOT_TL = (55, 55, 55, 255)
SLOT_BR = (255, 255, 255, 255)
ACCENT = (55, 140, 200, 255)


def new_tex():
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # outer panel bevel (vanilla container look)
    d.rectangle([0, 0, PANEL_W - 1, PANEL_H - 1], fill=PANEL)
    # light top/left, dark bottom/right
    d.line([0, 0, PANEL_W - 2, 0], fill=EDGE_L)
    d.line([0, 0, 0, PANEL_H - 2], fill=EDGE_L)
    d.line([PANEL_W - 1, 0, PANEL_W - 1, PANEL_H - 1], fill=EDGE_D)
    d.line([0, PANEL_H - 1, PANEL_W - 1, PANEL_H - 1], fill=EDGE_D)
    # inner shade under title
    d.rectangle([7, 16, PANEL_W - 8, 78], fill=(180, 188, 198, 255))
    return img, d


def slot_frame(d, item_x, item_y):
    """Draw 18x18 vanilla-style slot frame for item at (item_x, item_y)."""
    x, y = item_x - 1, item_y - 1
    # face + bevel (same language as furnace.png)
    d.rectangle([x, y, x + 17, y + 17], fill=SLOT_FACE)
    d.line([x, y, x + 16, y], fill=SLOT_TL)
    d.line([x, y, x, y + 16], fill=SLOT_TL)
    d.line([x + 17, y, x + 17, y + 17], fill=SLOT_BR)
    d.line([x, y + 17, x + 17, y + 17], fill=SLOT_BR)
    # soft inset well (readable items, not eye-searing black)
    d.rectangle([x + 1, y + 1, x + 16, y + 16], fill=SLOT_INNER)


def player_inventory(d):
    # 3 rows at y=84,102,120 — hotbar at 142 (=84+58)
    for row in range(3):
        for col in range(9):
            slot_frame(d, 8 + col * 18, INV_ITEM_Y + row * 18)
    for col in range(9):
        slot_frame(d, 8 + col * 18, INV_ITEM_Y + 58)


def progress_arrow_empty(d, x, y):
    """Hollow arrow like furnace (22x15-ish region starting at x,y)."""
    fill = (180, 180, 180, 255)
    d.rectangle([x, y + 4, x + 22, y + 10], outline=PANEL_DARK, fill=fill)
    tip = [(x + 22, y + 1), (x + 29, y + 7), (x + 22, y + 13)]
    d.polygon(tip, outline=PANEL_DARK, fill=fill)


def accent_dot(d, item_x, item_y, color):
    d.rectangle([item_x - 1, item_y + 15, item_x + 16, item_y + 16], fill=color)


# --- Mortar layout (menu slot item coords) ---
# input 44,35 | drive 26,53 | primary 116,17 | byproduct 134,35 | waste 116,53
# shifted primary/byproduct slightly for clearer triangle — KEEP MENU COORDS IN SYNC
MORTAR = {
    "input": (44, 35),
    "drive": (26, 53),
    "primary": (116, 17),
    "byproduct": (134, 35),
    "waste": (116, 53),
    "arrow": (74, 35),
}

img, d = new_tex()
for key in ("input", "drive", "primary", "byproduct", "waste"):
    slot_frame(d, *MORTAR[key])
accent_dot(d, *MORTAR["input"], (70, 160, 255, 255))
accent_dot(d, *MORTAR["drive"], (140, 100, 220, 255))
accent_dot(d, *MORTAR["primary"], (80, 200, 140, 255))
accent_dot(d, *MORTAR["byproduct"], (220, 190, 70, 255))
accent_dot(d, *MORTAR["waste"], (150, 120, 90, 255))
progress_arrow_empty(d, *MORTAR["arrow"])
player_inventory(d)
img.save(OUT / "mortar.png")

# --- Burner: fuel 56,35 | catalyst 80,17 ---
img, d = new_tex()
slot_frame(d, 56, 35)
slot_frame(d, 80, 17)
accent_dot(d, 56, 35, (255, 150, 50, 255))
accent_dot(d, 80, 17, (255, 220, 80, 255))
# flame well under fuel — warm grey, not void black
d.rectangle([56, 54, 71, 69], outline=PANEL_DARK, fill=(150, 130, 110, 255))
# temp button wells (screen places buttons at 103,14 / 32 / 50 size 54x16)
for y in (14, 32, 50):
    d.rectangle([102, y, 157, y + 15], outline=ACCENT, fill=(170, 180, 195, 255))
player_inventory(d)
img.save(OUT / "burner.png")

# --- Alembic: water 26,35 | r1 62,17 | r2 62,35 | r3 62,53 | out 116,35 ---
img, d = new_tex()
for pos in [(26, 35), (62, 17), (62, 35), (62, 53), (116, 35)]:
    slot_frame(d, *pos)
accent_dot(d, 26, 35, (80, 180, 255, 255))
accent_dot(d, 62, 17, (100, 220, 255, 255))
accent_dot(d, 62, 35, (90, 140, 180, 255))
accent_dot(d, 62, 53, (90, 140, 180, 255))
accent_dot(d, 116, 35, (120, 230, 180, 255))
progress_arrow_empty(d, 84, 35)
d.rectangle([8, 18, 17, 61], outline=PANEL_DARK, fill=(150, 130, 110, 255))
player_inventory(d)
img.save(OUT / "alembic.png")

# also write a blank progress arrow strip for filled blit (optional)
print("saved 256x256:", sorted(p.name for p in OUT.glob("*.png")))
for p in OUT.glob("*.png"):
    with Image.open(p) as im:
        print(p.name, im.size)
