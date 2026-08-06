"""Paint Phi-chitin armor entity UV to match approved item icons.

Focus (v1): chestplate body + arm pauldrons on layer_1.
Helmet / boots left as mild base recolor until their turn.
"""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
VAN = ROOT / "art/items/vanilla_refs/armor"
FACE = ROOT / "art/items/for_artist"
OUT = ROOT / "src/main/resources/assets/effecoria/textures/models/armor"
OUT_ITEM = ROOT / "src/main/resources/assets/effecoria/textures/item"

# From phi_chitin_chestplate_v1 icon
D = (18, 12, 30)  # outline / deep shadow
M = (45, 34, 78)  # mid indigo
L = (95, 78, 160)  # plate
H = (155, 135, 220)  # highlight lavender
C = (90, 220, 235)  # cyan glow
G = (210, 175, 55)  # gold pauldron tip


def put(a: np.ndarray, x: int, y: int, rgb: tuple[int, int, int], alpha: int = 255) -> None:
    if 0 <= x < a.shape[1] and 0 <= y < a.shape[0]:
        a[y, x, 0] = rgb[0]
        a[y, x, 1] = rgb[1]
        a[y, x, 2] = rgb[2]
        a[y, x, 3] = alpha


def fill_rect(a: np.ndarray, x0: int, y0: int, w: int, h: int, rgb: tuple[int, int, int]) -> None:
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            put(a, x, y, rgb)


def shade_box(
    a: np.ndarray,
    x0: int,
    y0: int,
    w: int,
    h: int,
    base: tuple[int, int, int] = M,
    hi: tuple[int, int, int] = L,
    lo: tuple[int, int, int] = D,
) -> None:
    """Simple top-left light for a UV face."""
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            ty = (y - y0) / max(1, h - 1)
            tx = (x - x0) / max(1, w - 1)
            if ty < 0.25 and tx < 0.45:
                col = hi
            elif ty > 0.75 or tx > 0.75:
                col = lo
            else:
                col = base
            put(a, x, y, col)


def paint_chestplate(a: np.ndarray) -> None:
    """Body + arms to match chestplate icon pauldrons / cyan core."""

    # --- Body top (collar / shoulder bridge) — fill even if iron left empty ---
    # UV body top: (20,16) 8x4
    fill_rect(a, 20, 16, 8, 4, M)
    put(a, 20, 16, H)
    put(a, 27, 16, H)
    put(a, 23, 17, C)
    put(a, 24, 17, C)
    put(a, 20, 19, D)
    put(a, 27, 19, D)

    # Body bottom (28,16) 8x4 — waist underside
    fill_rect(a, 28, 16, 8, 4, D)

    # Body right side (16,20) 4x12
    shade_box(a, 16, 20, 4, 12, M, L, D)
    put(a, 16, 20, H)  # shoulder edge
    put(a, 17, 21, C)

    # Body front (20,20) 8x12 — cyan gem pattern like icon
    shade_box(a, 20, 20, 8, 12, M, L, D)
    # neck shadow
    for x in range(22, 26):
        put(a, x, 20, D)
    # cyan core (sternum + V)
    put(a, 23, 22, C)
    put(a, 24, 22, C)
    put(a, 22, 24, C)
    put(a, 25, 24, C)
    put(a, 23, 25, C)
    put(a, 24, 25, C)
    put(a, 21, 26, C)
    put(a, 26, 26, C)
    # lower plate
    for x in range(21, 27):
        put(a, x, 30, D)
        put(a, x, 31, D)

    # Body left side (28,20) 4x12
    shade_box(a, 28, 20, 4, 12, M, L, D)
    put(a, 31, 20, H)
    put(a, 30, 21, C)

    # Body back (32,20) 8x12
    shade_box(a, 32, 20, 8, 12, M, L, D)
    put(a, 35, 23, C)
    put(a, 36, 23, C)

    # --- Arms / pauldrons (mirrored arm UV at 40–55; also left-arm top at 8,16) ---
    # Right-arm UV (mirrored to both arms in-game). Do NOT paint (8,16) — that is foot top.
    paint_pauldron_arm(a, top_xy=(44, 16), outer_x=40, front_x=44, inner_x=48, back_x=52)


def paint_pauldron_top_only(a: np.ndarray, x0: int, y0: int) -> None:
    fill_rect(a, x0, y0, 4, 4, L)
    put(a, x0, y0, G)
    put(a, x0 + 3, y0, G)
    put(a, x0 + 1, y0 + 1, H)
    put(a, x0 + 2, y0 + 1, H)
    put(a, x0 + 1, y0 + 2, C)
    put(a, x0 + 2, y0 + 3, D)


def paint_pauldron_arm(
    a: np.ndarray,
    top_xy: tuple[int, int],
    outer_x: int,
    front_x: int,
    inner_x: int,
    back_x: int,
) -> None:
    tx, ty = top_xy
    # Top of shoulder — gold tips like icon
    paint_pauldron_top_only(a, tx, ty)

    # Outer face — cyan gem on upper pauldron (icon outer cyan)
    shade_box(a, outer_x, 20, 4, 12, M, L, D)
    put(a, outer_x + 1, 20, H)
    put(a, outer_x + 2, 20, H)
    put(a, outer_x + 1, 21, C)
    put(a, outer_x + 2, 21, C)
    put(a, outer_x, 20, G)

    # Front
    shade_box(a, front_x, 20, 4, 12, M, L, D)
    put(a, front_x + 1, 20, H)
    put(a, front_x + 1, 22, C)

    # Inner
    shade_box(a, inner_x, 20, 4, 12, M, D, D)

    # Back
    shade_box(a, back_x, 20, 4, 12, M, L, D)
    put(a, back_x + 1, 21, C)

    # Arm bottoms (48,16) if present — dark
    fill_rect(a, 48, 16, 4, 4, D)


def base_recolor_other(src: np.ndarray, dst: np.ndarray) -> None:
    """Mild indigo recolor for helmet/boots pixels not painted as chest."""
    m = src[:, :, 3] > 16
    lum = (0.3 * src[:, :, 0] + 0.59 * src[:, :, 1] + 0.11 * src[:, :, 2]) / 255.0
    lo = np.array(D, np.float32)
    mid = np.array(M, np.float32)
    hi = np.array(L, np.float32)
    for y in range(src.shape[0]):
        for x in range(src.shape[1]):
            if not m[y, x]:
                continue
            if dst[y, x, 3] > 0:
                continue  # already painted chest
            t = float(lum[y, x])
            if t > 0.65:
                col = hi
            elif t > 0.35:
                col = mid
            else:
                col = lo
            put(dst, x, y, tuple(int(c) for c in col))


def save_preview(a: np.ndarray, name: str) -> None:
    FACE.mkdir(parents=True, exist_ok=True)
    img = Image.fromarray(a, "RGBA")
    img.save(FACE / f"{name}.png")
    prev = Image.new("RGBA", (a.shape[1] * 4, a.shape[0] * 4), (18, 20, 28, 255))
    big = img.resize((a.shape[1] * 4, a.shape[0] * 4), Image.Resampling.NEAREST)
    prev.paste(big, (0, 0), big)
    prev.save(FACE / f"{name}_4x.png")


def main() -> None:
    iron = np.array(Image.open(VAN / "iron_layer_1.png").convert("RGBA"))
    out = np.zeros_like(iron)

    # Start transparent; paint chest strongly; fill rest from iron mask
    paint_chestplate(out)
    base_recolor_other(iron, out)

    # Keep iron alpha as minimum silhouette, but allow our added shoulder fills
    # (we intentionally added body top / arm bottoms)
    OUT.mkdir(parents=True, exist_ok=True)
    Image.fromarray(out, "RGBA").save(OUT / "phi_chitin_layer_1.png")
    save_preview(out, "phi_chitin_layer_1_chest_v1")

    # Also bake chestplate item icon again
    item = FACE / "phi_chitin_chestplate_v1_16x.png"
    if item.exists():
        ia = np.array(Image.open(item).convert("RGBA"))
        ia[:, :, 3] = np.where(ia[:, :, 3] > 96, 255, 0).astype(np.uint8)
        Image.fromarray(ia, "RGBA").save(OUT_ITEM / "phi_chitin_chestplate.png")

    # Crop preview of body+arms only for review
    crop = out[16:32, 16:56].copy()
    save_preview(crop, "phi_chitin_chest_uv_crop")
    print("wrote layer_1 chest pauldrons + previews")
    print("opaque", int((out[:, :, 3] > 0).sum()))


if __name__ == "__main__":
    main()
