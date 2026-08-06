"""Recolor vanilla textures for early material sinks."""
from pathlib import Path
import zipfile

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
JAR = ROOT / "build/moddev/artifacts/neoforge-21.1.242-client-extra-aka-minecraft-resources.jar"
TMP = ROOT / "tmp/vanilla_mat"
OUT_BLOCK = ROOT / "src/main/resources/assets/effecoria/textures/block"
OUT_ITEM = ROOT / "src/main/resources/assets/effecoria/textures/item"
OUT_ARMOR = ROOT / "src/main/resources/assets/effecoria/textures/models/armor"
ART = ROOT / "art/material_sinks"

FOL_LO, FOL_HI = (18, 26, 78), (110, 150, 220)
GOLD, GOLD_ALT = (200, 180, 60), (170, 155, 80)
STEM_LO, STEM_HI = (13, 10, 15), (48, 36, 58)


def lum(r, g, b):
    return (0.3 * r + 0.59 * g + 0.11 * b) / 255.0


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def recolor_blue(im: Image.Image) -> Image.Image:
    im = im.convert("RGBA")
    w, _h = im.size
    out = []
    for i, (r, g, b, a) in enumerate(im.getdata()):
        if a == 0:
            out.append((0, 0, 0, 0))
            continue
        t = lum(r, g, b) ** 0.9
        rgb = lerp(FOL_LO, FOL_HI, t)
        x, y = i % w, i // w
        if t > 0.55 and ((x * 3 + y * 5) % 9 == 0):
            rgb = GOLD if ((x + y) % 2 == 0) else GOLD_ALT
        out.append((*rgb, a))
    res = Image.new("RGBA", im.size)
    res.putdata(out)
    return res


def recolor_chitin(im: Image.Image) -> Image.Image:
    im = im.convert("RGBA")
    w, _h = im.size
    out = []
    for i, (r, g, b, a) in enumerate(im.getdata()):
        if a == 0:
            out.append((0, 0, 0, 0))
            continue
        t = lum(r, g, b)
        rgb = lerp((20, 40, 90), (90, 170, 210), t)
        x, y = i % w, i // w
        if t > 0.65 and ((x + y * 3) % 11 == 0):
            rgb = (200, 180, 60)
        out.append((*rgb, a))
    res = Image.new("RGBA", im.size)
    res.putdata(out)
    return res


def recolor_sword(im: Image.Image) -> Image.Image:
    im = im.convert("RGBA")
    w, _h = im.size
    out = []
    for i, (r, g, b, a) in enumerate(im.getdata()):
        if a == 0:
            out.append((0, 0, 0, 0))
            continue
        x, y = i % w, i // w
        if r > g + 10 and r > b + 10:
            rgb = lerp(STEM_LO, STEM_HI, lum(r, g, b))
        elif abs(r - g) <= 8 and abs(g - b) <= 8:
            t = lum(r, g, b)
            rgb = lerp((40, 90, 140), (180, 230, 255), t)
            if t > 0.7 and ((x * 2 + y) % 7 == 0):
                rgb = GOLD
        else:
            rgb = lerp(FOL_LO, FOL_HI, lum(r, g, b))
        out.append((*rgb, a))
    res = Image.new("RGBA", im.size)
    res.putdata(out)
    return res


def main() -> None:
    for p in (TMP, OUT_BLOCK, OUT_ITEM, OUT_ARMOR, ART):
        p.mkdir(parents=True, exist_ok=True)
    need = [
        "assets/minecraft/textures/block/glass.png",
        "assets/minecraft/textures/block/oak_planks.png",
        "assets/minecraft/textures/item/iron_sword.png",
        "assets/minecraft/textures/item/iron_pickaxe.png",
        "assets/minecraft/textures/item/iron_axe.png",
        "assets/minecraft/textures/item/iron_shovel.png",
        "assets/minecraft/textures/item/leather_helmet.png",
        "assets/minecraft/textures/item/leather_chestplate.png",
        "assets/minecraft/textures/item/leather_leggings.png",
        "assets/minecraft/textures/item/leather_boots.png",
        "assets/minecraft/textures/models/armor/leather_layer_1.png",
        "assets/minecraft/textures/models/armor/leather_layer_2.png",
    ]
    with zipfile.ZipFile(JAR) as z:
        for n in need:
            z.extract(n, TMP)

    mapping = [
        (TMP / "assets/minecraft/textures/block/glass.png", OUT_BLOCK / "phi_glass.png", recolor_blue),
        (TMP / "assets/minecraft/textures/block/oak_planks.png", OUT_BLOCK / "phi_planks.png", recolor_blue),
        (TMP / "assets/minecraft/textures/item/iron_sword.png", OUT_ITEM / "vitrified_glass_sword.png", recolor_sword),
        (TMP / "assets/minecraft/textures/item/iron_pickaxe.png", OUT_ITEM / "vitrified_glass_pickaxe.png", recolor_sword),
        (TMP / "assets/minecraft/textures/item/iron_axe.png", OUT_ITEM / "vitrified_glass_axe.png", recolor_sword),
        (TMP / "assets/minecraft/textures/item/iron_shovel.png", OUT_ITEM / "vitrified_glass_shovel.png", recolor_sword),
        (TMP / "assets/minecraft/textures/item/leather_helmet.png", OUT_ITEM / "phi_chitin_helmet.png", recolor_chitin),
        (
            TMP / "assets/minecraft/textures/item/leather_chestplate.png",
            OUT_ITEM / "phi_chitin_chestplate.png",
            recolor_chitin,
        ),
        (TMP / "assets/minecraft/textures/item/leather_leggings.png", OUT_ITEM / "phi_chitin_leggings.png", recolor_chitin),
        (TMP / "assets/minecraft/textures/item/leather_boots.png", OUT_ITEM / "phi_chitin_boots.png", recolor_chitin),
        (
            TMP / "assets/minecraft/textures/models/armor/leather_layer_1.png",
            OUT_ARMOR / "phi_chitin_layer_1.png",
            recolor_chitin,
        ),
        (
            TMP / "assets/minecraft/textures/models/armor/leather_layer_2.png",
            OUT_ARMOR / "phi_chitin_layer_2.png",
            recolor_chitin,
        ),
    ]
    for src, dst, fn in mapping:
        img = fn(Image.open(src))
        img.save(dst)
        img.save(ART / dst.name)
        print("wrote", dst.relative_to(ROOT))


if __name__ == "__main__":
    main()
