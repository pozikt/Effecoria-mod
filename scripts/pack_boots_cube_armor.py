"""Pack Phi-chitin boots cube-face grid → foot UV on layer_1 (keep helmet/chest/pauldrons)."""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = Path(r"C:\Users\2005t\.cursor\projects\c-Users-2005t-Effecoria-mod\assets")
CUBES = ROOT / "art/items/armor_cubes"
FACE = ROOT / "art/items/for_artist"
VAN = ROOT / "art/items/vanilla_refs/armor"
OUT = ROOT / "src/main/resources/assets/effecoria/textures/models/armor"

FACE_ORDER = ["Front", "Back", "Left", "Right", "Top", "Bottom"]

# Right foot uses same UV as right leg on layer_1; left mirrors.
# Iron boots only paint sole + lower ~6 rows of the 4×12 sides.
FOOT_SIDE = {
    "Right": (0, 20, 4, 12),
    "Front": (4, 20, 4, 12),
    "Left": (8, 20, 4, 12),
    "Back": (12, 20, 4, 12),
}
SOLE_UV = (8, 16, 4, 4)  # Bottom
# Visible boot rows on iron (ankle/toe), absolute Y on atlas:
BOOT_Y0, BOOT_H = 26, 6


def copy_grid() -> Path:
    CUBES.mkdir(parents=True, exist_ok=True)
    src = ASSETS / "armor_boots_cube_grid_v1.png"
    dst = CUBES / "boots_cube_grid_v1.png"
    dst.write_bytes(src.read_bytes())
    return dst


def find_six_tiles(im: Image.Image) -> list[tuple[int, int, int, int]]:
    a = np.array(im.convert("RGBA"))
    rgb = a[:, :, :3].astype(np.float32)
    lum = rgb.mean(2)
    sat = rgb.max(2) - rgb.min(2)
    purple = (rgb[:, :, 2] > 40) & (rgb[:, :, 0] < 140) & (lum < 190)
    gold = (rgb[:, :, 0] > 140) & (rgb[:, :, 1] > 90) & (rgb[:, :, 2] < 140)
    cyan = (rgb[:, :, 2] > 160) & (rgb[:, :, 1] > 120) & (rgb[:, :, 0] < 180)
    mask = ((lum > 18) & (lum < 210) & (sat > 10)) | purple | gold | cyan

    def runs(flag: np.ndarray, min_len: int = 80) -> list[tuple[int, int]]:
        out = []
        i, n = 0, len(flag)
        while i < n:
            if flag[i]:
                j = i
                while j < n and flag[j]:
                    j += 1
                if j - i > min_len:
                    out.append((i, j))
                i = j
            else:
                i += 1
        return out

    col_runs = runs(mask.mean(axis=0) > 0.02)
    ys = np.where(mask.any(axis=1))[0]
    y0, y1 = int(ys.min()), int(ys.max()) + 1
    # Prefer one tall content band (ignore labels).
    row_bands = runs(mask.mean(axis=1) > 0.02)
    if row_bands:
        # pick tallest band
        y0, y1 = max(row_bands, key=lambda r: r[1] - r[0])
    if len(col_runs) < 6:
        xs = np.where(mask.any(axis=0))[0]
        x0, x1 = int(xs.min()), int(xs.max()) + 1
        step = (x1 - x0) // 6
        col_runs = [(x0 + i * step, x0 + (i + 1) * step) for i in range(6)]
    col_runs = col_runs[:6]

    tiles = []
    for cx0, cx1 in col_runs:
        cell = mask[y0:y1, cx0:cx1]
        if not cell.any():
            tiles.append((cx0, y0, cx1, y1))
            continue
        cy, cx = np.where(cell)
        tiles.append(
            (
                cx0 + int(cx.min()),
                y0 + int(cy.min()),
                cx0 + int(cx.max()) + 1,
                y0 + int(cy.max()) + 1,
            )
        )
    return tiles


def crop_opaque(tile: Image.Image) -> np.ndarray:
    a = np.array(tile.convert("RGBA"))
    rgb = a[:, :, :3].astype(np.float32)
    near_white = (rgb[:, :, 0] > 220) & (rgb[:, :, 1] > 220) & (rgb[:, :, 2] > 220)
    a[near_white, 3] = 0
    m = a[:, :, 3] > 40
    if m.any():
        ys, xs = np.where(m)
        a = a[int(ys.min()) : int(ys.max()) + 1, int(xs.min()) : int(xs.max()) + 1]
    return a


def scale_arr(a: np.ndarray, tw: int, th: int) -> np.ndarray:
    return np.array(Image.fromarray(a, "RGBA").resize((tw, th), Image.Resampling.NEAREST))


def stamp(dst: np.ndarray, face: np.ndarray, x: int, y: int, w: int, h: int) -> None:
    f = face
    if f.shape[1] != w or f.shape[0] != h:
        f = scale_arr(f, w, h)
    for yy in range(h):
        for xx in range(w):
            if f[yy, xx, 3] > 64:
                dst[y + yy, x + xx] = f[yy, xx]
            else:
                dst[y + yy, x + xx] = (0, 0, 0, 0)


def clear_boot_uv(dst: np.ndarray) -> None:
    # Sole
    for y in range(16, 20):
        for x in range(8, 12):
            dst[y, x] = (0, 0, 0, 0)
    # Full right-leg island (we'll re-stamp visible boot + leave rest clear for leggings)
    for y in range(16, 32):
        for x in range(0, 16):
            dst[y, x] = (0, 0, 0, 0)


def apply_iron_boot_mask(dst: np.ndarray, iron: np.ndarray) -> None:
    """Only touch the right-leg / sole island — leave chest/arms/head alone."""
    iron_a = iron[:, :, 3] > 16
    for y in range(16, 32):
        for x in range(0, 16):
            if not iron_a[y, x]:
                dst[y, x, 3] = 0


def main() -> None:
    path = copy_grid()
    im = Image.open(path)
    tiles = find_six_tiles(im)
    print("tiles", len(tiles))
    if len(tiles) < 6:
        raise SystemExit(f"expected 6 tiles, got {len(tiles)}")

    faces_dir = CUBES / "boots_faces_v1"
    faces_dir.mkdir(exist_ok=True)
    named: dict[str, Image.Image] = {}
    for i, box in enumerate(tiles[:6]):
        fname = FACE_ORDER[i]
        crop = im.crop(box)
        crop.save(faces_dir / f"BOOT_{fname}.png")
        named[fname] = crop
        print(f"  BOOT_{fname}", box, crop.size)

    layer_path = OUT / "phi_chitin_layer_1.png"
    layer = np.array(Image.open(layer_path).convert("RGBA"))
    if layer.shape[0] < 64 or layer.shape[1] < 64:
        big = np.zeros((64, 64, 4), np.uint8)
        big[: layer.shape[0], : layer.shape[1]] = layer
        layer = big

    iron = np.array(Image.open(VAN / "iron_layer_1.png").convert("RGBA"))
    clear_boot_uv(layer)

    # Sides: iron only shows lower BOOT_H rows — pack LOWER part of art (ankle band + toe).
    for fname, (x, _y, w, _h) in FOOT_SIDE.items():
        raw = crop_opaque(named[fname])
        # lower ~70% of face → cuff/ankle band + toe fit in the 6 visible rows
        y_cut = max(1, int(raw.shape[0] * 0.30))
        lower = raw[y_cut:, :, :]
        face = scale_arr(lower, w, BOOT_H)
        face[:, :, 3] = 255
        stamp(layer, face, x, BOOT_Y0, w, BOOT_H)
        Image.fromarray(face, "RGBA").save(faces_dir / f"BOOT_{fname}_{w}x{BOOT_H}.png")

    # Sole
    sx, sy, sw, sh = SOLE_UV
    sole = scale_arr(crop_opaque(named["Bottom"]), sw, sh)
    sole[:, :, 3] = 255
    stamp(layer, sole, sx, sy, sw, sh)
    Image.fromarray(sole, "RGBA").save(faces_dir / f"BOOT_Bottom_{sw}x{sh}.png")

    apply_iron_boot_mask(layer, iron)

    OUT.mkdir(parents=True, exist_ok=True)
    Image.fromarray(layer, "RGBA").save(layer_path)
    Image.fromarray(layer, "RGBA").save(CUBES / "phi_chitin_layer_1_with_boots.png")

    FACE.mkdir(parents=True, exist_ok=True)
    crop = layer[16:32, 0:16].copy()
    prev = Image.new("RGBA", (16 * 10, 16 * 10), (18, 20, 28, 255))
    big = Image.fromarray(crop, "RGBA").resize((16 * 10, 16 * 10), Image.Resampling.NEAREST)
    prev.paste(big, (0, 0), big)
    prev.save(FACE / "phi_chitin_boots_uv_v1_10x.png")

    full = Image.new("RGBA", (64 * 3, 64 * 3), (18, 20, 28, 255))
    fbig = Image.fromarray(layer, "RGBA").resize((64 * 3, 64 * 3), Image.Resampling.NEAREST)
    full.paste(fbig, (0, 0), fbig)
    full.save(FACE / "phi_chitin_layer_1_full_with_boots_3x.png")

    print("boot front UV (4-7,26-31):")
    for y in range(26, 32):
        chars = []
        for x in range(4, 8):
            r, g, b, a = map(int, layer[y, x])
            if a < 16:
                chars.append(".")
            elif r > 180 and g > 140 and b < 130:
                chars.append("G")
            elif b > 180 and g > 140:
                chars.append("C")
            else:
                chars.append("#")
        print(y, "".join(chars))
    print("sole (8-11,16-19):")
    for y in range(16, 20):
        chars = []
        for x in range(8, 12):
            r, g, b, a = map(int, layer[y, x])
            if a < 16:
                chars.append(".")
            elif b > 180 and g > 140:
                chars.append("C")
            else:
                chars.append("#")
        print(y, "".join(chars))
    print("wrote boots into layer_1")


if __name__ == "__main__":
    main()
