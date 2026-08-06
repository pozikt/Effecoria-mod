"""Generate placeholder textures for Φ-alchemy station (vanilla recolors)."""
from pathlib import Path
import zipfile

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
JAR = ROOT / "build/moddev/artifacts/neoforge-21.1.242-client-extra-aka-minecraft-resources.jar"
TMP = ROOT / "tmp/alchemy"
OUT_BLOCK = ROOT / "src/main/resources/assets/effecoria/textures/block"
OUT_ITEM = ROOT / "src/main/resources/assets/effecoria/textures/item"

CYAN_LO, CYAN_HI = (20, 60, 110), (120, 200, 255)
GOLD, GOLD_ALT = (210, 180, 60), (170, 150, 70)
STONE_LO, STONE_HI = (30, 45, 80), (90, 120, 160)


def lum(r, g, b):
    return (0.3 * r + 0.59 * g + 0.11 * b) / 255.0


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def recolor(im: Image.Image, lo, hi, gold_thresh=0.7):
    im = im.convert("RGBA")
    w, _ = im.size
    out = []
    for i, (r, g, b, a) in enumerate(im.getdata()):
        if a == 0:
            out.append((0, 0, 0, 0))
            continue
        t = lum(r, g, b)
        rgb = lerp(lo, hi, t)
        x, y = i % w, i // w
        if t > gold_thresh and ((x * 3 + y * 5) % 8 == 0):
            rgb = GOLD if ((x + y) % 2 == 0) else GOLD_ALT
        out.append((*rgb, a))
    res = Image.new("RGBA", im.size)
    res.putdata(out)
    return res


def tint_bottle(im: Image.Image, liquid):
    im = im.convert("RGBA")
    w, _ = im.size
    out = []
    for i, (r, g, b, a) in enumerate(im.getdata()):
        if a == 0:
            out.append((0, 0, 0, 0))
            continue
        t = lum(r, g, b)
        # keep glass highlights greyish; liquid-ish mid tones get tint
        if t < 0.85 and abs(r - g) < 40:
            rgb = lerp(liquid[0], liquid[1], t)
        else:
            rgb = (r, g, b)
        out.append((*rgb, a))
    res = Image.new("RGBA", im.size)
    res.putdata(out)
    return res


def main():
    for p in (TMP, OUT_BLOCK, OUT_ITEM):
        p.mkdir(parents=True, exist_ok=True)
    need = [
        "assets/minecraft/textures/block/flower_pot.png",
        "assets/minecraft/textures/block/furnace_side.png",
        "assets/minecraft/textures/block/furnace_front_on.png",
        "assets/minecraft/textures/block/blue_stained_glass.png",
        "assets/minecraft/textures/item/paper.png",
        "assets/minecraft/textures/item/glass_bottle.png",
        "assets/minecraft/textures/item/potion.png",
        "assets/minecraft/textures/item/gold_nugget.png",
        "assets/minecraft/textures/item/iron_nugget.png",
    ]
    with zipfile.ZipFile(JAR) as z:
        for n in need:
            z.extract(n, TMP)

    mapping = [
        (TMP / "assets/minecraft/textures/block/flower_pot.png", OUT_BLOCK / "mortar_and_pestle.png", lambda i: recolor(i, STONE_LO, STONE_HI)),
        (TMP / "assets/minecraft/textures/block/furnace_side.png", OUT_BLOCK / "essence_burner.png", lambda i: recolor(i, STONE_LO, CYAN_HI, 0.75)),
        (TMP / "assets/minecraft/textures/block/furnace_front_on.png", OUT_BLOCK / "essence_burner_on.png", lambda i: recolor(i, (40, 80, 140), (180, 230, 255), 0.5)),
        (TMP / "assets/minecraft/textures/block/blue_stained_glass.png", OUT_BLOCK / "essence_alembic.png", lambda i: recolor(i, CYAN_LO, CYAN_HI, 0.6)),
        (TMP / "assets/minecraft/textures/item/paper.png", OUT_ITEM / "phi_paper.png", lambda i: recolor(i, (40, 70, 120), (160, 200, 255), 0.8)),
        (TMP / "assets/minecraft/textures/item/glass_bottle.png", OUT_ITEM / "phi_flask.png", lambda i: recolor(i, (80, 100, 120), (200, 230, 255), 0.9)),
        (TMP / "assets/minecraft/textures/item/glass_bottle.png", OUT_ITEM / "phi_flask_water.png", lambda i: tint_bottle(i, ((30, 100, 160), (140, 220, 255)))),
        (TMP / "assets/minecraft/textures/item/gold_nugget.png", OUT_ITEM / "gold_filter.png", lambda i: recolor(i, (120, 90, 20), (255, 220, 100), 0.5)),
        (TMP / "assets/minecraft/textures/item/iron_nugget.png", OUT_ITEM / "lead_filter.png", lambda i: recolor(i, (40, 45, 55), (140, 145, 155), 0.9)),
        (TMP / "assets/minecraft/textures/item/potion.png", OUT_ITEM / "potion_phi_tonic.png", lambda i: tint_bottle(i, ((40, 140, 200), (160, 230, 255)))),
        (TMP / "assets/minecraft/textures/item/potion.png", OUT_ITEM / "potion_phi_resonance.png", lambda i: tint_bottle(i, ((60, 80, 200), (180, 170, 255)))),
        (TMP / "assets/minecraft/textures/item/potion.png", OUT_ITEM / "potion_phi_stimulant.png", lambda i: tint_bottle(i, ((180, 140, 40), (255, 230, 120)))),
    ]
    for src, dst, fn in mapping:
        img = fn(Image.open(src))
        img.save(dst)
        print("wrote", dst.relative_to(ROOT))


if __name__ == "__main__":
    main()
