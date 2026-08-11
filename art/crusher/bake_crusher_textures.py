"""Bake Φ-crusher GUI + item/block 16x16 textures."""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ART = ROOT / "art" / "crusher"
BLOCK = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "block"
ITEM = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "item"
GUI = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "gui"
for p in (ART, BLOCK, ITEM, GUI):
    p.mkdir(parents=True, exist_ok=True)

# Palette tuned for a more "lattice" lead + hotter Phi glow.
EDGE = (18, 20, 24, 255)
STONE = (62, 70, 78, 255)
STONE_H = (98, 110, 120, 255)
LEAD = (42, 46, 52, 255)
LEAD_H = (88, 98, 108, 255)
TEETH = (185, 205, 215, 255)
GLOW = (35, 225, 245, 255)


def px(img, x, y, c):
    if 0 <= x < img.size[0] and 0 <= y < img.size[1]:
        img.putpixel((x, y), c)


def fill(img, c):
    for y in range(img.size[1]):
        for x in range(img.size[0]):
            px(img, x, y, c)


def make_item(base, accent=None, grit=False):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(3, 13):
        for x in range(3, 13):
            px(img, x, y, base if (x + y) % 2 == 0 else tuple(max(0, c - 20) for c in base[:3]) + (255,))
    if accent:
        for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
            px(img, x, y, accent)
    if grit:
        for x, y in ((4, 5), (11, 6), (6, 11), (10, 10), (5, 9)):
            px(img, x, y, accent or (200, 200, 200, 255))
    return img


def make_crusher_side(lit=False):
    img = Image.new("RGBA", (16, 16), EDGE)
    for y in range(1, 15):
        for x in range(1, 15):
            px(img, x, y, LEAD if y > 10 else STONE)
    for y in range(4, 12):
        for x in range(3, 13):
            px(img, x, y, STONE_H if (x + y) % 3 else STONE)
    # "Teeth" grille with a bit of anisotropy.
    for x in range(3, 13):
        for y in (5, 7, 9, 11):
            if (x + y) % 3 == 0:
                px(img, x, y, TEETH if lit else (82, 92, 104, 255))
            else:
                px(img, x, y, (110, 120, 130, 255) if not lit else (145, 165, 175, 255))

    if lit:
        # inner glow corners (more saturated than the old layout)
        for x, y in ((6, 6), (9, 6), (6, 8), (9, 8), (7, 7), (8, 7)):
            px(img, x, y, GLOW)
        # faint scanline shimmer
        for y in range(6, 10):
            for x in range(5, 11):
                if (x + y) % 2 == 0:
                    px(img, x, y, tuple(max(0, c - 40) for c in GLOW[:3]) + (255,))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px(img, x, y, LEAD_H)
    return img


def make_crusher_top():
    img = Image.new("RGBA", (16, 16), EDGE)
    for y in range(1, 15):
        for x in range(1, 15):
            px(img, x, y, STONE)
    # center ring + lead lattice.
    for y in range(4, 12):
        for x in range(4, 12):
            if 6 <= x <= 9 and 6 <= y <= 9:
                px(img, x, y, EDGE)
            elif (x + y) % 2 == 0:
                px(img, x, y, LEAD_H)
            else:
                px(img, x, y, LEAD)
    return img


def make_hopper_side():
    img = Image.new("RGBA", (16, 16), EDGE)
    for y in range(16):
        for x in range(16):
            # funnel silhouette
            width = 14 - (y // 2)
            cx = 8
            if abs(x - cx) <= width // 2 and y >= 1:
                px(img, x, y, STONE_H if y < 6 else STONE)
            else:
                px(img, x, y, (0, 0, 0, 0))
    for x in range(5, 11):
        for y in range(12, 16):
            px(img, x, y, LEAD)
    return img


def make_hopper_top():
    img = Image.new("RGBA", (16, 16), EDGE)
    for y in range(1, 15):
        for x in range(1, 15):
            px(img, x, y, STONE_H)
    for y in range(4, 12):
        for x in range(4, 12):
            px(img, x, y, EDGE)
    return img


def make_gui():
    gui = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(gui)
    d.rectangle([0, 0, 175, 165], fill=(198, 198, 198, 255), outline=(55, 55, 55, 255))
    d.rectangle([1, 1, 174, 164], outline=(255, 255, 255, 255))
    # slots: input 44,35 ; primary 116,17 ; by 134,35 ; waste 116,53 ; cell 26,53
    for x, y in ((44, 35), (116, 17), (134, 35), (116, 53), (26, 53)):
        d.rectangle([x - 1, y - 1, x + 16, y + 16], fill=(55, 55, 55, 255))
        d.rectangle([x, y, x + 15, y + 15], fill=(139, 139, 139, 255))
    # progress bar bg
    d.rectangle([62, 58, 113, 73], fill=(120, 120, 125, 255), outline=(50, 50, 55, 255))
    # mode button area
    d.rectangle([62, 18, 113, 33], fill=(100, 110, 120, 255), outline=(50, 50, 55, 255))
    # energy bar
    d.rectangle([8, 18, 20, 70], fill=(40, 50, 55, 255), outline=(30, 30, 35, 255))
    for row in range(3):
        for col in range(9):
            x = 8 + col * 18
            y = 84 + row * 18
            d.rectangle([x - 1, y - 1, x + 16, y + 16], fill=(55, 55, 55, 255))
            d.rectangle([x, y, x + 15, y + 15], fill=(139, 139, 139, 255))
    for col in range(9):
        x = 8 + col * 18
        y = 142
        d.rectangle([x - 1, y - 1, x + 16, y + 16], fill=(55, 55, 55, 255))
        d.rectangle([x, y, x + 15, y + 15], fill=(139, 139, 139, 255))
    return gui


def main():
    # Generate a "design sheet" first, then crop it into actual 16x16 textures.
    # Layout (vertical):
    #  y=0..15   : side (unlit)
    #  y=16..31  : side (lit)
    #  y=32..47  : top
    side_unlit = make_crusher_side(False)
    side_lit = make_crusher_side(True)
    top = make_crusher_top()

    sheet = Image.new("RGBA", (16, 16 * 3), (0, 0, 0, 0))
    sheet.paste(side_unlit, (0, 0))
    sheet.paste(side_lit, (0, 16))
    sheet.paste(top, (0, 32))

    sheet_path = ART / "phi_crusher_sheet.png"
    sheet.save(sheet_path)

    BLOCK.mkdir(parents=True, exist_ok=True)
    ITEM.mkdir(parents=True, exist_ok=True)

    # Crop & save
    sheet.crop((0, 0, 16, 16)).save(BLOCK / "phi_crusher_side.png")
    sheet.crop((0, 16, 16, 32)).save(BLOCK / "phi_crusher_side_on.png")
    sheet.crop((0, 32, 16, 48)).save(BLOCK / "phi_crusher_top.png")
    sheet.crop((0, 0, 16, 16)).save(ITEM / "phi_crusher.png")

    # Hopper has no separate lit variant in our current block model,
    # but we still keep the "sheet then crop" workflow.
    hs = make_hopper_side()
    ht = make_hopper_top()

    hopper_sheet = Image.new("RGBA", (16, 16 * 2), (0, 0, 0, 0))
    hopper_sheet.paste(hs, (0, 0))
    hopper_sheet.paste(ht, (0, 16))
    hopper_sheet_path = ART / "phi_crusher_hopper_sheet.png"
    hopper_sheet.save(hopper_sheet_path)

    hopper_sheet.crop((0, 0, 16, 16)).save(BLOCK / "phi_crusher_hopper_side.png")
    hopper_sheet.crop((0, 16, 16, 32)).save(BLOCK / "phi_crusher_hopper_top.png")
    hopper_sheet.crop((0, 0, 16, 16)).save(ITEM / "phi_crusher_hopper.png")

    items = {
        "phi_stone_grit": ((90, 100, 110, 255), (180, 200, 210, 255), True),
        "bone_grit": ((200, 195, 180, 255), (240, 235, 220, 255), True),
        "phi_bone_paste": ((180, 200, 210, 255), (60, 180, 200, 255), False),
        "phi_wood_shavings": ((120, 90, 50, 255), (180, 140, 80, 255), True),
        "phi_fiber": ((100, 140, 90, 255), (60, 200, 160, 255), False),
        "obsidian_grit": ((30, 20, 40, 255), (80, 40, 100, 255), True),
        "omega_nugget": ((50, 20, 60, 255), (180, 60, 200, 255), False),
        "soul_shard": ((140, 200, 220, 255), (220, 250, 255, 255), False),
        "lead_foil": ((70, 75, 80, 255), (140, 145, 150, 255), False),
        "omega_waste": ((40, 30, 20, 255), (120, 80, 40, 255), True),
    }
    for name, (base, accent, grit) in items.items():
        make_item(base, accent, grit).save(ITEM / f"{name}.png")

    # phi cobble block
    cobble = Image.new("RGBA", (16, 16), EDGE)
    for y in range(16):
        for x in range(16):
            px(cobble, x, y, STONE if (x // 4 + y // 4) % 2 == 0 else STONE_H)
    cobble.save(BLOCK / "phi_cobble.png")
    cobble.save(ITEM / "phi_cobble.png")

    make_gui().save(GUI / "phi_crusher.png")
    print("baked crusher textures ok")


if __name__ == "__main__":
    main()
