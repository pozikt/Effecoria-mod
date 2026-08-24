"""Recolor vanilla closed book → Magic Primer (indigo leather, cream pages, gold Φ)."""
from __future__ import annotations

import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
JAR = ROOT / "build/moddev/artifacts/neoforge-21.1.242-client-extra-aka-minecraft-resources.jar"
ART = ROOT / "art/primer"
OUT = ROOT / "src/main/resources/assets/effecoria/textures/item/magic_primer.png"
SRC_IN_JAR = "assets/minecraft/textures/item/book.png"

INDIGO_LO = (18, 28, 72)
INDIGO_HI = (70, 110, 190)
GOLD_LO = (160, 110, 28)
GOLD_HI = (255, 230, 120)
PAGE_LO = (170, 155, 110)
PAGE_HI = (255, 248, 220)
CYAN = (90, 230, 255)


def lum(r: int, g: int, b: int) -> float:
    return (0.3 * r + 0.59 * g + 0.11 * b) / 255.0


def lerp(a, b, t: float):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def extract_book() -> Image.Image:
    ART.mkdir(parents=True, exist_ok=True)
    dest = ART / "book.png"
    if JAR.exists():
        with zipfile.ZipFile(JAR) as z:
            dest.write_bytes(z.read(SRC_IN_JAR))
    return Image.open(dest).convert("RGBA")


def recolor(im: Image.Image) -> Image.Image:
    w, h = im.size
    px = list(im.getdata())
    out = []
    for i, (r, g, b, a) in enumerate(px):
        if a == 0:
            out.append((0, 0, 0, 0))
            continue
        t = lum(r, g, b)
        # Page stack is near-neutral gray
        if max(r, g, b) - min(r, g, b) < 18 and max(r, g, b) > 80:
            rgb = lerp(PAGE_LO, PAGE_HI, t)
        else:
            rgb = lerp(INDIGO_LO, INDIGO_HI, t)
        out.append((*rgb, a))
    img = Image.new("RGBA", (w, h))
    img.putdata(out)
    p = img.load()
    # Gold latch pixels (vanilla dark ticks on the cover)
    for x, y in ((7, 4), (10, 5), (6, 6), (10, 6), (11, 7), (10, 8)):
        r, g, b, a = p[x, y]
        if a:
            p[x, y] = (*GOLD_HI, a)
    # Φ-like mark: two uprights + bar (cover face)
    gold_mark = [(8, 4), (9, 4), (8, 5), (9, 5), (8, 6), (9, 6), (7, 6), (10, 6), (8, 7), (9, 7)]
    for x, y in gold_mark:
        r, g, b, a = p[x, y]
        if a:
            p[x, y] = (*CYAN, a)
    # Specular glint on spine
    if p[2, 5][3]:
        p[2, 5] = (*GOLD_HI, 255)
    return img


def main() -> None:
    img = recolor(extract_book())
    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT)
    print("wrote", OUT, OUT.stat().st_size)


if __name__ == "__main__":
    main()
