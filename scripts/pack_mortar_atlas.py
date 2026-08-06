"""Pack edited mortar atlas sketch into game textures + update hollow model UVs."""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
SKETCH = ROOT / "art/phi_alchemy/sketches/mortar_atlas_sketch.png"
ITEM_SKETCH = ROOT / "art/phi_alchemy/sketches/mortar_pestle_item_icon.png"
FACE_DIR = ROOT / "art/phi_alchemy/mortar_template/faces"
OUT_BLOCK = ROOT / "src/main/resources/assets/effecoria/textures/block"
OUT_ITEM = ROOT / "src/main/resources/assets/effecoria/textures/item"
OUT_MODEL = ROOT / "src/main/resources/assets/effecoria/models/block"
OUT_ITEM_MODEL = ROOT / "src/main/resources/assets/effecoria/models/item"


def content_mask(arr: np.ndarray) -> np.ndarray:
    return (arr[:, :, 3] > 200) & ((arr[:, :, 0].astype(int) + arr[:, :, 1] + arr[:, :, 2]) > 90)


def find_square_panel(im: Image.Image, search: tuple[int, int, int, int], prefer_hollow: bool = True) -> Image.Image:
    """Find largest near-square opaque panel in search box (skips label text)."""
    a = np.array(im)
    x0, y0, x1, y1 = search
    sub = a[y0:y1, x0:x1]
    m = content_mask(sub)
    # erase thin text-like rows (low fill)
    row_fill = m.mean(axis=1)
    col_fill = m.mean(axis=0)
    # keep rows/cols with substantial fill (texture frames, not text)
    row_ok = row_fill > 0.08
    col_ok = col_fill > 0.05
    if not row_ok.any() or not col_ok.any():
        raise RuntimeError(f"no panel in {search}")
    ys = np.where(row_ok)[0]
    xs = np.where(col_ok)[0]
    # split into vertical bands of continuous rows
    bands = []
    start = int(ys[0])
    prev = int(ys[0])
    for y in ys[1:]:
        y = int(y)
        if y > prev + 2:
            bands.append((start, prev + 1))
            start = y
        prev = y
    bands.append((start, prev + 1))
    # pick band with largest area among roughly square-ish crops
    best = None
    best_score = -1.0
    for by0, by1 in bands:
        band = m[by0:by1]
        if band.shape[0] < 24:
            continue
        cx = np.where(band.any(axis=0))[0]
        if len(cx) == 0:
            continue
        bx0, bx1 = int(cx[0]), int(cx[-1]) + 1
        h = by1 - by0
        w = bx1 - bx0
        if w < 24 or h < 24:
            continue
        # prefer near-square for side/inner/rim; pestle handled separately
        aspect = w / max(h, 1)
        square_score = 1.0 - abs(1.0 - aspect)
        area = w * h
        hollow = 0.0
        if prefer_hollow and h > 10 and w > 10:
            inner = band[h // 4 : 3 * h // 4, w // 4 : 3 * w // 4]
            hollow = 1.0 - float(inner.mean())
        score = area * (0.5 + 0.5 * max(square_score, 0)) + hollow * 5000
        if score > best_score:
            best_score = score
            best = (bx0 + x0, by0 + y0, bx1 + x0, by1 + y0)
    if best is None:
        # fallback full content bbox
        ys2, xs2 = np.where(m)
        best = (int(xs2.min()) + x0, int(ys2.min()) + y0, int(xs2.max()) + x0 + 1, int(ys2.max()) + y0 + 1)
    crop = im.crop(best)
    # trim residual thin label strips at top if present (low-density first rows)
    ca = np.array(crop)
    cm = content_mask(ca)
    rf = cm.mean(axis=1)
    # drop leading rows that are sparse (text)
    top = 0
    while top < len(rf) and rf[top] < 0.25:
        top += 1
    bottom = len(rf)
    while bottom > top and rf[bottom - 1] < 0.12:
        bottom -= 1
    crop = crop.crop((0, top, crop.width, bottom))
    return crop


def find_pestle(im: Image.Image, search: tuple[int, int, int, int]) -> Image.Image:
    a = np.array(im)
    x0, y0, x1, y1 = search
    sub = a[y0:y1, x0:x1]
    # brown wood
    wood = (
        (sub[:, :, 0] > sub[:, :, 1] + 15)
        & (sub[:, :, 1] > sub[:, :, 2] + 5)
        & (sub[:, :, 0] > 70)
        & (sub[:, :, 3] > 200)
    )
    ys, xs = np.where(wood)
    if len(xs) == 0:
        raise RuntimeError("pestle wood not found")
    bx0, by0 = int(xs.min()) + x0, int(ys.min()) + y0
    bx1, by1 = int(xs.max()) + x0 + 1, int(ys.max()) + y0 + 1
    crop = im.crop((bx0, by0, bx1, by1))
    # trim label above if any non-wood
    return crop


def to_size(im: Image.Image, size: tuple[int, int]) -> Image.Image:
    return im.resize(size, Image.Resampling.NEAREST)


def quantize_minecraft(im: Image.Image) -> Image.Image:
    """Keep sharp pixels; force fully opaque or transparent."""
    a = np.array(im.convert("RGBA"))
    alpha = a[:, :, 3]
    a[:, :, 3] = np.where(alpha > 128, 255, 0)
    return Image.fromarray(a, "RGBA")


def make_item_icon(side: Image.Image, pestle: Image.Image, item_sketch: Path) -> Image.Image:
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # bowl from side frame downscaled
    bowl = to_size(side, (12, 10))
    icon.paste(bowl, (2, 5), bowl)
    # pestle slanted paste as vertical stick
    p = to_size(pestle, (3, 12))
    icon.paste(p, (9, 1), p)
    # if item sketch exists, blend silhouette hints by downscaling center crop
    if item_sketch.exists():
        sk = Image.open(item_sketch).convert("RGBA")
        # center square content
        w, h = sk.size
        m = min(w, h)
        skc = sk.crop(((w - m) // 2, (h - m) // 2, (w + m) // 2, (h + m) // 2))
        sk16 = to_size(skc, (16, 16))
        # use sketch where alpha strong, else keep composite
        sa = np.array(sk16)
        ca = np.array(icon)
        mix = sa[:, :, 3] > 180
        # prefer sketch colors but keep our pestle/bowl if sketch too painterly soft
        # take sketch as primary item look
        out = sa.copy()
        # punch near-background greys to transparent
        lum = out[:, :, 0].astype(int) + out[:, :, 1] + out[:, :, 2]
        bg = (out[:, :, 3] < 40) | ((lum < 90) & (out[:, :, 3] < 220))
        out[bg, 3] = 0
        return quantize_minecraft(Image.fromarray(out, "RGBA"))
    return quantize_minecraft(icon)


def write_model() -> None:
    model = """{
  "ambientocclusion": false,
  "parent": "minecraft:block/block",
  "textures": {
    "particle": "effecoria:block/mortar_side",
    "side": "effecoria:block/mortar_side",
    "inner": "effecoria:block/mortar_inner",
    "rim": "effecoria:block/mortar_rim",
    "bottom": "effecoria:block/mortar_bottom",
    "pestle": "effecoria:block/mortar_pestle"
  },
  "elements": [
    {
      "name": "wall_west",
      "from": [5, 0, 5],
      "to": [6, 6, 11],
      "faces": {
        "north": { "uv": [0, 0, 2, 16], "texture": "#side" },
        "south": { "uv": [14, 0, 16, 16], "texture": "#side" },
        "west": { "uv": [0, 0, 16, 16], "texture": "#side" },
        "east": { "uv": [0, 0, 16, 16], "texture": "#inner" },
        "up": { "uv": [0, 0, 2, 16], "texture": "#rim" },
        "down": { "uv": [0, 0, 2, 16], "texture": "#bottom", "cullface": "down" }
      }
    },
    {
      "name": "wall_east",
      "from": [10, 0, 5],
      "to": [11, 6, 11],
      "faces": {
        "north": { "uv": [14, 0, 16, 16], "texture": "#side" },
        "south": { "uv": [0, 0, 2, 16], "texture": "#side" },
        "west": { "uv": [0, 0, 16, 16], "texture": "#inner" },
        "east": { "uv": [0, 0, 16, 16], "texture": "#side" },
        "up": { "uv": [14, 0, 16, 16], "texture": "#rim" },
        "down": { "uv": [14, 0, 16, 16], "texture": "#bottom", "cullface": "down" }
      }
    },
    {
      "name": "wall_north",
      "from": [6, 0, 5],
      "to": [10, 6, 6],
      "faces": {
        "north": { "uv": [3, 0, 13, 16], "texture": "#side" },
        "south": { "uv": [3, 0, 13, 16], "texture": "#inner" },
        "up": { "uv": [3, 0, 13, 2], "texture": "#rim" },
        "down": { "uv": [3, 14, 13, 16], "texture": "#bottom", "cullface": "down" }
      }
    },
    {
      "name": "wall_south",
      "from": [6, 0, 10],
      "to": [10, 6, 11],
      "faces": {
        "north": { "uv": [3, 0, 13, 16], "texture": "#inner" },
        "south": { "uv": [3, 0, 13, 16], "texture": "#side" },
        "up": { "uv": [3, 14, 13, 16], "texture": "#rim" },
        "down": { "uv": [3, 0, 13, 2], "texture": "#bottom", "cullface": "down" }
      }
    },
    {
      "name": "floor",
      "from": [6, 0, 6],
      "to": [10, 1, 10],
      "faces": {
        "up": { "uv": [3, 3, 13, 13], "texture": "#inner" },
        "down": { "uv": [3, 3, 13, 13], "texture": "#bottom", "cullface": "down" }
      }
    },
    {
      "name": "pestle",
      "from": [8.5, 1, 7.5],
      "to": [9.5, 8, 8.5],
      "rotation": { "origin": [9, 1, 8], "axis": "z", "angle": -22.5 },
      "faces": {
        "north": { "uv": [0, 0, 4, 16], "texture": "#pestle" },
        "south": { "uv": [4, 0, 8, 16], "texture": "#pestle" },
        "west": { "uv": [8, 0, 12, 16], "texture": "#pestle" },
        "east": { "uv": [12, 0, 16, 16], "texture": "#pestle" },
        "up": { "uv": [0, 0, 4, 4], "texture": "#pestle" },
        "down": { "uv": [0, 12, 4, 16], "texture": "#pestle" }
      }
    }
  ]
}
"""
    (OUT_MODEL / "mortar_and_pestle.json").write_text(model, encoding="utf-8")
    (OUT_ITEM_MODEL / "mortar_and_pestle.json").write_text(
        '{ "parent": "minecraft:item/generated", "textures": { "layer0": "effecoria:item/mortar_and_pestle" } }\n',
        encoding="utf-8",
    )


def main() -> None:
    FACE_DIR.mkdir(parents=True, exist_ok=True)
    OUT_BLOCK.mkdir(parents=True, exist_ok=True)
    OUT_ITEM.mkdir(parents=True, exist_ok=True)

    im = Image.open(SKETCH).convert("RGBA")
    panels = {
        "side": find_square_panel(im, (20, 20, 480, 460)),
        "inner": find_square_panel(im, (500, 20, 980, 460)),
        "rim": find_square_panel(im, (20, 430, 480, 860)),
        "bottom": find_square_panel(im, (20, 860, 520, 1010), prefer_hollow=False),
        "pestle": find_pestle(im, (500, 430, 780, 980)),
    }

    # normalize to minecraft sizes
    side16 = quantize_minecraft(to_size(panels["side"], (16, 16)))
    inner16 = quantize_minecraft(to_size(panels["inner"], (16, 16)))
    rim16 = quantize_minecraft(to_size(panels["rim"], (16, 16)))
    # bottom is a strip — fit into 16x16 (paint on bottom rows)
    bot = panels["bottom"]
    bot16 = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    bot_r = quantize_minecraft(to_size(bot, (16, max(3, min(8, round(16 * bot.height / max(bot.width, 1)))))))
    bot16.paste(bot_r, (0, 16 - bot_r.height), bot_r)
    # also fill full for underside sampling
    bot_full = quantize_minecraft(to_size(bot, (16, 16)))

    pest = panels["pestle"]
    # pestle atlas 16x16: 4 vertical strips (cylinder sides)
    pest16 = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    strip = quantize_minecraft(to_size(pest, (4, 16)))
    for i in range(4):
        # slight shade variants
        s = strip.copy()
        if i in (1, 2):
            arr = np.array(s)
            arr[:, :, :3] = np.clip(arr[:, :, :3].astype(int) + 12, 0, 255)
            s = Image.fromarray(arr.astype(np.uint8), "RGBA")
        if i in (0, 3):
            arr = np.array(s)
            arr[:, :, :3] = np.clip(arr[:, :, :3].astype(int) - 18, 0, 255)
            s = Image.fromarray(arr.astype(np.uint8), "RGBA")
        pest16.paste(s, (i * 4, 0), s)

    for name, img in [
        ("mortar_side", side16),
        ("mortar_inner", inner16),
        ("mortar_rim", rim16),
        ("mortar_bottom", bot_full),
        ("mortar_pestle", pest16),
    ]:
        img.save(OUT_BLOCK / f"{name}.png")
        img.save(FACE_DIR / f"{name}.png")
        print("wrote", name, img.size)

    # combined preview atlas for artist QA
    preview = Image.new("RGBA", (80, 32), (20, 20, 24, 255))
    preview.paste(side16, (0, 0))
    preview.paste(inner16, (16, 0))
    preview.paste(rim16, (32, 0))
    preview.paste(bot_full, (48, 0))
    preview.paste(pest16, (64, 0))
    preview_big = preview.resize((80 * 8, 32 * 8), Image.Resampling.NEAREST)
    preview_big.save(FACE_DIR / "packed_preview_8x.png")

    # legacy single atlas (optional particle / fallback)
    legacy = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    legacy.paste(to_size(side16, (6, 6)), (0, 0))
    legacy.paste(to_size(inner16, (6, 6)), (8, 0))
    legacy.paste(to_size(rim16, (6, 6)), (0, 7))
    legacy.paste(to_size(strip, (2, 6)), (8, 7))
    legacy.paste(to_size(bot_full, (6, 3)), (0, 13))
    legacy.save(OUT_BLOCK / "mortar_and_pestle.png")

    item = make_item_icon(side16, pest, ITEM_SKETCH)
    item.save(OUT_ITEM / "mortar_and_pestle.png")
    item.save(FACE_DIR / "mortar_and_pestle_item.png")

    write_model()
    print("model updated; item icon written")


if __name__ == "__main__":
    main()
