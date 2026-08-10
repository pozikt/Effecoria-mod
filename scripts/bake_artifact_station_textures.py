"""Bake artifact station block textures from concept face sheet (crop + downscale).

Source art (generate / replace manually):
  art/artifact_stations/face_sheet.png  — 5×4 grid of flat face swatches
  art/artifact_stations/concept_blocks.png — isometric reference (not baked)

Regenerate game PNGs:
  python scripts/bake_artifact_station_textures.py
"""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SHEET = ROOT / "art/artifact_stations/face_sheet.png"
OUT = ROOT / "src/main/resources/assets/effecoria/textures/block"
PREVIEW = ROOT / "art/artifact_stations/baked_preview.png"

COLS = 5
ROWS = 4
# Trim dark gutter between generated cells (fraction of cell size)
GUTTER = 0.06


def is_bg(rgb: np.ndarray) -> bool:
    r, g, b = int(rgb[0]), int(rgb[1]), int(rgb[2])
    if max(r, g, b) < 22:
        return True
    return abs(r - g) <= 8 and abs(g - b) <= 8 and 24 <= r <= 60


def trim_content(im: Image.Image) -> Image.Image:
    arr = np.array(im.convert("RGBA"))
    h, w = arr.shape[:2]
    mask = np.zeros((h, w), dtype=bool)
    for y in range(h):
        for x in range(w):
            if arr[y, x, 3] < 8:
                continue
            if not is_bg(arr[y, x, :3]):
                mask[y, x] = True
    ys, xs = np.where(mask)
    if len(xs) == 0:
        return im
    pad = max(1, min(w, h) // 32)
    x0 = max(0, int(xs.min()) + pad)
    y0 = max(0, int(ys.min()) + pad)
    x1 = min(w, int(xs.max()) + 1 - pad)
    y1 = min(h, int(ys.max()) + 1 - pad)
    return im.crop((x0, y0, x1, y1))


def to_16(im: Image.Image) -> Image.Image:
    im = trim_content(im)
    w, h = im.size
    side = min(w, h)
    left = (w - side) // 2
    top = (h - side) // 2
    square = im.crop((left, top, left + side, top + side))
    return square.resize((16, 16), Image.Resampling.NEAREST)


def cell_crop(sheet: Image.Image, col: int, row: int) -> Image.Image:
    w, h = sheet.size
    cw = w / COLS
    ch = h / ROWS
    gx = int(cw * GUTTER)
    gy = int(ch * GUTTER)
    x0 = int(col * cw) + gx
    y0 = int(row * ch) + gy
    x1 = int((col + 1) * cw) - gx
    y1 = int((row + 1) * ch) - gy
    return sheet.crop((x0, y0, x1, y1))


def main() -> None:
    if not SHEET.is_file():
        raise SystemExit(f"Missing concept sheet: {SHEET}")

    OUT.mkdir(parents=True, exist_ok=True)
    sheet = Image.open(SHEET).convert("RGBA")

    mapping: list[tuple[str, Image.Image]] = []

    def add(name: str, col: int, row: int) -> None:
        mapping.append((name, to_16(cell_crop(sheet, col, row))))

    # Row 0 — shaft lathe
    add("shaft_lathe_bottom.png", 0, 0)
    add("shaft_lathe_top.png", 1, 0)
    add("shaft_lathe_side.png", 2, 0)
    add("shaft_lathe_saw.png", 3, 0)

    # Row 1 — facet cutter
    add("facet_cutter_bottom.png", 0, 1)
    add("facet_cutter_top.png", 1, 1)
    add("facet_cutter_side.png", 2, 1)
    add("facet_cutter_disc.png", 3, 1)
    arm_pivot = cell_crop(sheet, 4, 1)
    aw, ah = arm_pivot.size
    mapping.append(("facet_cutter_arm.png", to_16(arm_pivot.crop((0, 0, aw // 2, ah)))))
    mapping.append(("facet_cutter_pivot.png", to_16(arm_pivot.crop((aw // 2, 0, aw, ah)))))

    # Row 2 — assembler
    add("artifact_assembler_bottom.png", 0, 2)
    add("artifact_assembler_top.png", 1, 2)
    add("artifact_assembler_side.png", 2, 2)
    add("artifact_assembler_front.png", 3, 2)

    # Row 3 — seal inscriber
    add("seal_inscriber_bottom.png", 0, 3)
    add("seal_inscriber_side.png", 1, 3)
    add("seal_inscriber_top.png", 2, 3)

    for name, tex in mapping:
        tex.save(OUT / name)

    # Preview atlas of baked 16×16 (×8)
    scale = 8
    cols = 5
    preview = Image.new("RGBA", (cols * 16 * scale, ((len(mapping) + cols - 1) // cols) * 16 * scale), (24, 24, 28, 255))
    for i, (name, tex) in enumerate(mapping):
        up = tex.resize((16 * scale, 16 * scale), Image.NEAREST)
        ox = (i % cols) * 16 * scale
        oy = (i // cols) * 16 * scale
        preview.paste(up, (ox, oy))
    preview.save(PREVIEW)

    print(f"Baked {len(mapping)} textures from {SHEET.relative_to(ROOT)} -> {OUT.relative_to(ROOT)}")
    print(f"Preview: {PREVIEW.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
