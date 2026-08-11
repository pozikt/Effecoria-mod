"""Bake Φ-turret mount/hull/accent textures for 2-block rotating turrets."""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ART = ROOT / "art" / "turrets"
BLOCK = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "block"
ITEM = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "item"
GUI = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "gui"
for p in (ART, BLOCK, ITEM, GUI):
    p.mkdir(parents=True, exist_ok=True)

EDGE = (18, 20, 24, 255)
METAL = (52, 56, 62, 255)
METAL_H = (88, 94, 102, 255)
METAL_D = (34, 36, 40, 255)
RIVET = (160, 166, 174, 255)

KINDS = {
    "plasma_turret": ((30, 190, 220), (180, 245, 255)),
    "kinetic_turret": ((120, 128, 138), (210, 215, 220)),
    "spatial_turret": ((80, 30, 150), (190, 130, 255)),
    "mental_turret": ((50, 110, 210), (150, 200, 255)),
    "omega_turret": ((36, 8, 48), (170, 50, 210)),
}


def px(img: Image.Image, x: int, y: int, c) -> None:
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), c)


def make_face(core, glow, lit: bool = False):
    img = Image.new("RGBA", (16, 16), EDGE)
    for y in range(1, 15):
        for x in range(1, 15):
            band = METAL_H if (x + y) % 3 == 0 else METAL
            if y < 3 or y > 12:
                band = METAL_D
            px(img, x, y, band)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13), (8, 2), (8, 13)):
        px(img, x, y, RIVET)
    dim = (core[0] // 2, core[1] // 2, core[2] // 2, 255)
    for y in range(4, 12):
        for x in range(5, 11):
            px(img, x, y, core if lit else dim)
    c = glow if lit else core
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        px(img, x, y, c)
    # muzzle teeth
    for x in (5, 10):
        for y in range(5, 11):
            px(img, x, y, EDGE)
    return img


def make_side(core):
    img = Image.new("RGBA", (16, 16), EDGE)
    for y in range(1, 15):
        for x in range(1, 15):
            px(img, x, y, METAL if x % 2 else METAL_H)
    for y in range(5, 11):
        for x in range(3, 14):
            px(img, x, y, core if 6 <= y <= 9 else (core[0] // 2, core[1] // 2, core[2] // 2, 255))
    for x in (2, 13):
        for y in range(4, 12):
            px(img, x, y, RIVET if y % 2 == 0 else METAL_D)
    return img


def make_top():
    img = Image.new("RGBA", (16, 16), EDGE)
    for y in range(1, 15):
        for x in range(1, 15):
            px(img, x, y, METAL_H if 4 <= x <= 11 and 4 <= y <= 11 else METAL)
    for x in range(6, 10):
        for y in range(6, 10):
            px(img, x, y, METAL_D)
    return img


def make_mount():
    """Solid lead/mithril plate — no checker noise. Bevel + rivets + Φ socket."""
    img = Image.new("RGBA", (16, 16), EDGE)
    # base fill
    for y in range(1, 15):
        for x in range(1, 15):
            px(img, x, y, METAL)
    # outer bevel highlight / shadow (like iron block)
    for i in range(1, 15):
        px(img, i, 1, METAL_H)
        px(img, 1, i, METAL_H)
        px(img, i, 14, METAL_D)
        px(img, 14, i, METAL_D)
    # inner recessed tray
    for y in range(3, 13):
        for x in range(3, 13):
            px(img, x, y, METAL_D if 4 <= x <= 11 and 4 <= y <= 11 else METAL)
    for i in range(3, 13):
        px(img, i, 3, (44, 48, 54, 255))
        px(img, 3, i, (44, 48, 54, 255))
        px(img, i, 12, EDGE)
        px(img, 12, i, EDGE)
    # corner rivets
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px(img, x, y, RIVET)
        px(img, x + (1 if x < 8 else -1), y, METAL_H)
    # mid-edge bolts
    for x, y in ((8, 2), (8, 13), (2, 8), (13, 8)):
        px(img, x, y, RIVET)
    # circular Φ coupling socket
    ring = ((6, 5), (7, 5), (8, 5), (9, 5), (5, 6), (10, 6), (5, 7), (10, 7), (5, 8), (10, 8), (5, 9), (10, 9), (6, 10), (7, 10), (8, 10), (9, 10))
    for x, y in ring:
        px(img, x, y, (28, 120, 140, 255))
    for x, y in ((6, 6), (9, 6), (6, 9), (9, 9)):
        px(img, x, y, (20, 90, 110, 255))
    for x, y in ((7, 6), (8, 6), (6, 7), (9, 7), (6, 8), (9, 8), (7, 9), (8, 9)):
        px(img, x, y, (36, 150, 170, 255))
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        px(img, x, y, (70, 220, 235, 255))
    return img


def make_bolt_entity():
    """16x16 projectile strip for kinetic/Ω bolts."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    body = (170, 190, 205, 255)
    tip = (230, 245, 255, 255)
    core = (90, 210, 230, 255)
    for x in range(2, 14):
        for y in (7, 8):
            px(img, x, y, tip if x >= 11 else body)
    for x in range(3, 12):
        px(img, x, 6, (120, 140, 155, 200))
        px(img, x, 9, (120, 140, 155, 200))
    for x in range(4, 11):
        px(img, x, 7, core)
        px(img, x, 8, core)
    px(img, 13, 7, tip)
    px(img, 13, 8, tip)
    px(img, 14, 7, (255, 255, 255, 255))
    px(img, 14, 8, (255, 255, 255, 255))
    return img


def make_hull(core, glow, lit: bool = False):
    img = Image.new("RGBA", (16, 16), EDGE)
    for y in range(16):
        for x in range(16):
            band = METAL_H if (y // 2 + x) % 4 == 0 else METAL
            if x < 2 or x > 13:
                band = METAL_D
            px(img, x, y, band)
    c = glow if lit else core
    for y in range(1, 15):
        for x in range(5, 11):
            px(img, x, y, c if 6 <= x <= 9 else (c[0] // 2, c[1] // 2, c[2] // 2, 255))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px(img, x, y, RIVET)
    # segmented rings
    for y in (4, 8, 12):
        for x in range(3, 13):
            px(img, x, y, EDGE)
    return img


def make_accent(core, glow, lit: bool = False):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    c = glow if lit else core
    bright = glow if lit else (min(255, core[0] + 40), min(255, core[1] + 40), min(255, core[2] + 40), 255)
    for y in range(16):
        for x in range(16):
            if 4 <= x <= 11 and 1 <= y <= 14:
                px(img, x, y, bright if 6 <= x <= 9 else c)
    for y in range(2, 14, 3):
        for x in range(5, 11):
            px(img, x, y, (255, 255, 255, 220) if lit else bright)
    return img


def main() -> None:
    mount = make_mount()
    mount.save(BLOCK / "turret_mount.png")
    mount.save(ITEM / "turret_mount.png")
    mount.save(ART / "turret_mount_16.png")

    for name, (core, glow) in KINDS.items():
        front = make_face(core, glow, False)
        front_on = make_face(core, glow, True)
        side = make_side(core)
        top = make_top()
        hull = make_hull(core, glow, False)
        hull_on = make_hull(core, glow, True)
        accent = make_accent(core, glow, False)
        accent_on = make_accent(core, glow, True)
        front.save(BLOCK / f"{name}_front.png")
        front_on.save(BLOCK / f"{name}_front_on.png")
        side.save(BLOCK / f"{name}_side.png")
        top.save(BLOCK / f"{name}_top.png")
        kind = name.replace("_turret", "")
        hull.save(BLOCK / f"{kind}_turret_hull.png")
        hull_on.save(BLOCK / f"{kind}_turret_hull_on.png")
        accent.save(BLOCK / f"{kind}_turret_accent.png")
        accent_on.save(BLOCK / f"{kind}_turret_accent_on.png")
        front.save(ITEM / f"{name}.png")
        front.save(ART / f"{name}_16.png")

    bolt_item = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(2, 14):
        for x in (7, 8):
            bolt_item.putpixel((x, y), (180, 200, 210, 255) if y < 5 else (120, 140, 150, 255))
    bolt_item.putpixel((6, 4), (160, 180, 190, 255))
    bolt_item.putpixel((9, 4), (160, 180, 190, 255))
    bolt_item.save(ITEM / "mithril_bolt.png")
    make_bolt_entity().save(BLOCK / "turret_bolt.png")
    make_bolt_entity().save(ART / "turret_bolt_16.png")

    gui = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(gui)
    d.rectangle([0, 0, 175, 165], fill=(198, 198, 198, 255), outline=(55, 55, 55, 255))
    d.rectangle([1, 1, 174, 164], outline=(255, 255, 255, 255))
    d.rectangle([79, 34, 96, 51], fill=(55, 55, 55, 255))
    d.rectangle([80, 35, 95, 50], fill=(139, 139, 139, 255))
    d.rectangle([62, 58, 113, 73], fill=(120, 120, 125, 255), outline=(50, 50, 55, 255))
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
    gui.save(GUI / "phi_turret.png")
    print("baked turrets ok")


if __name__ == "__main__":
    main()
