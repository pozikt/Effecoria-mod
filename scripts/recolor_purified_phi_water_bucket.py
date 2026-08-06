"""Recolor phi_water_bucket → purified (lighter cyan + gold flecks)."""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/resources/assets/effecoria/textures/item/phi_water_bucket.png"
OUT = ROOT / "src/main/resources/assets/effecoria/textures/item/purified_phi_water_bucket.png"
ART = ROOT / "art/phi_water/purified_phi_water_bucket.png"

CYAN_LO, CYAN_HI = (40, 120, 170), (170, 230, 255)
GOLD, GOLD_ALT = (220, 190, 70), (180, 160, 90)
METAL_LO, METAL_HI = (70, 70, 75), (180, 180, 190)


def lum(r, g, b):
    return (0.3 * r + 0.59 * g + 0.11 * b) / 255.0


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def main() -> None:
    im = Image.open(SRC).convert("RGBA")
    w, _h = im.size
    out = []
    for i, (r, g, b, a) in enumerate(im.getdata()):
        if a == 0:
            out.append((0, 0, 0, 0))
            continue
        t = lum(r, g, b)
        x, y = i % w, i // w
        # Bucket metal: low-saturation greyish pixels
        if abs(r - g) <= 18 and abs(g - b) <= 18 and t < 0.55:
            rgb = lerp(METAL_LO, METAL_HI, t)
        else:
            rgb = lerp(CYAN_LO, CYAN_HI, min(1.0, t * 1.15))
            if t > 0.45 and ((x * 3 + y * 5) % 7 == 0):
                rgb = GOLD if ((x + y) % 2 == 0) else GOLD_ALT
        out.append((*rgb, a))
    res = Image.new("RGBA", im.size)
    res.putdata(out)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    ART.parent.mkdir(parents=True, exist_ok=True)
    res.save(OUT)
    res.save(ART)
    print("wrote", OUT.relative_to(ROOT))


if __name__ == "__main__":
    main()
