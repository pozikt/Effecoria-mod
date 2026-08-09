"""Crop Emerald Canopy concept panels and downscale to 16x16 block textures.

Does NOT recolor vanilla — takes pixels from art/emerald_canopy/concept_block_sheet.png.
"""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "art/emerald_canopy"
CONCEPT = ART / "concept_block_sheet.png"
OUT = ROOT / "src/main/resources/assets/effecoria/textures/block"
CROPS = ART / "crops_16"
OUT.mkdir(parents=True, exist_ok=True)
CROPS.mkdir(parents=True, exist_ok=True)

# Sheet is 3x2 tiles (1536x1024). Mapping matches the generated panel layout.
# 0 bark side, 1 wood top, 2 golden bark side
# 3 golden bark top, 4 snare vine sprite, 5 moss litter
PANEL_TO_TEXTURE = {
    0: ("ancient_essence_wood.png", False),
    1: ("ancient_essence_wood_top.png", False),
    2: ("golden_bark.png", False),
    3: ("golden_bark_top.png", False),
    4: ("phi_snare_vine.png", True),  # checkerboard → alpha
    5: None,  # moss accent unused as block for now; still export crop
}


def is_bg(rgb: np.ndarray) -> bool:
    """Neutral sheet background (dark gray / near-black outside frames)."""
    r, g, b = int(rgb[0]), int(rgb[1]), int(rgb[2])
    if max(r, g, b) < 18:
        return True
    # flat gray backdrop
    return abs(r - g) <= 6 and abs(g - b) <= 6 and 25 <= r <= 55


def is_checker(rgb: np.ndarray) -> bool:
    r, g, b = int(rgb[0]), int(rgb[1]), int(rgb[2])
    # Concept sheet uses classic light/dark gray checkers behind the vine.
    if abs(r - g) <= 12 and abs(g - b) <= 12:
        if r >= 95:
            return True
        if 55 <= r <= 94:
            return True
    return False


def panel_bbox(arr: np.ndarray, x0: int, y0: int, x1: int, y1: int) -> tuple[int, int, int, int]:
    """Tight content box inside a grid cell (skip sheet bg)."""
    cell = arr[y0:y1, x0:x1]
    h, w = cell.shape[:2]
    mask = np.zeros((h, w), dtype=bool)
    for y in range(h):
        for x in range(w):
            mask[y, x] = not is_bg(cell[y, x])
    ys, xs = np.where(mask)
    if len(xs) == 0:
        return x0, y0, x1, y1
    # inset a few px to drop soft drop-shadow / frame glow
    pad = 6
    return (
        x0 + max(0, int(xs.min()) + pad),
        y0 + max(0, int(ys.min()) + pad),
        x0 + min(w, int(xs.max()) + 1 - pad),
        y0 + min(h, int(ys.max()) + 1 - pad),
    )


def to_square(im: Image.Image) -> Image.Image:
    w, h = im.size
    side = min(w, h)
    left = (w - side) // 2
    top = (h - side) // 2
    return im.crop((left, top, left + side, top + side))


def checker_to_alpha(im: Image.Image) -> Image.Image:
    """Turn gray checkerboard into transparency; keep plant pixels opaque."""
    rgba = im.convert("RGBA")
    arr = np.asarray(rgba).copy()
    for y in range(arr.shape[0]):
        for x in range(arr.shape[1]):
            if is_checker(arr[y, x, :3]):
                arr[y, x] = (0, 0, 0, 0)
            else:
                arr[y, x, 3] = 255
    return Image.fromarray(arr, "RGBA")


def opaque_square(im: Image.Image) -> Image.Image:
    """Crop to opaque content, then pad to square on transparent."""
    arr = np.asarray(im.convert("RGBA"))
    alpha = arr[..., 3]
    ys, xs = np.where(alpha > 16)
    if len(xs) == 0:
        return im
    x0, x1 = int(xs.min()), int(xs.max()) + 1
    y0, y1 = int(ys.min()), int(ys.max()) + 1
    cropped = arr[y0:y1, x0:x1]
    h, w = cropped.shape[:2]
    side = max(h, w)
    canvas = np.zeros((side, side, 4), dtype=np.uint8)
    oy, ox = (side - h) // 2, (side - w) // 2
    canvas[oy : oy + h, ox : ox + w] = cropped
    return Image.fromarray(canvas, "RGBA")


def downscale_16(im: Image.Image) -> Image.Image:
    """Area-average fragment → 16×16 (real downsample of the crop)."""
    return im.resize((16, 16), Image.Resampling.BOX)


def main() -> None:
    sheet = Image.open(CONCEPT).convert("RGB")
    w, h = sheet.size
    cols, rows = 3, 2
    cw, rh = w // cols, h // rows
    arr = np.asarray(sheet)

    for idx in range(cols * rows):
        col, row = idx % cols, idx // cols
        x0, y0 = col * cw, row * rh
        x1, y1 = x0 + cw, y0 + rh
        bx0, by0, bx1, by1 = panel_bbox(arr, x0, y0, x1, y1)
        # Prefer interior of framed tile: shrink toward center if bbox is huge
        frag = sheet.crop((bx0, by0, bx1, by1))
        frag = to_square(frag)

        # Save full-res crop for debugging
        frag.save(CROPS / f"panel_{idx}_full.png")

        mapping = PANEL_TO_TEXTURE.get(idx)
        if mapping is None:
            small = downscale_16(frag)
            small.save(CROPS / f"panel_{idx}_16.png")
            print(f"panel {idx}: accent only -> crops")
            continue

        name, vine = mapping
        work = frag.convert("RGBA")
        if vine:
            work = checker_to_alpha(work)
            work = opaque_square(work)
        small = downscale_16(work)
        if vine:
            # Hard alpha after BOX so fringes don't leave gray mud.
            a = np.asarray(small).copy()
            for y in range(16):
                for x in range(16):
                    if a[y, x, 3] < 96 or is_checker(a[y, x, :3]):
                        a[y, x] = (0, 0, 0, 0)
                    else:
                        a[y, x, 3] = 255
            small = Image.fromarray(a, "RGBA")

        small.save(OUT / name)
        small.save(CROPS / name)
        print(f"panel {idx}: {bx0},{by0}-{bx1},{by1} -> {name} ({small.size})")


if __name__ == "__main__":
    main()
