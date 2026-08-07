"""Pack Phi-chitin helmet cube-face grid → head UV on layer_1 (keep chest/pauldrons)."""
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

# Head UV on armor layer (64×32 top half)
HEAD_UV = {
    "Top": (8, 0, 8, 8),
    "Bottom": (16, 0, 8, 8),
    "Right": (0, 8, 8, 8),
    "Front": (8, 8, 8, 8),
    "Left": (16, 8, 8, 8),
    "Back": (24, 8, 8, 8),
}


def copy_grid() -> Path:
    CUBES.mkdir(parents=True, exist_ok=True)
    src = ASSETS / "armor_helmet_cube_grid_v1.png"
    dst = CUBES / "helmet_cube_grid_v1.png"
    dst.write_bytes(src.read_bytes())
    return dst


def find_six_tiles(im: Image.Image) -> list[tuple[int, int, int, int]]:
    a = np.array(im.convert("RGBA"))
    rgb = a[:, :, :3].astype(np.float32)
    lum = rgb.mean(2)
    sat = rgb.max(2) - rgb.min(2)
    purple = (rgb[:, :, 2] > 40) & (rgb[:, :, 0] < 140) & (lum < 190)
    mask = ((lum > 18) & (lum < 210) & (sat > 10)) | purple
    col_density = mask.mean(axis=0)
    cols = col_density > 0.02

    def runs(flag: np.ndarray) -> list[tuple[int, int]]:
        out = []
        i, n = 0, len(flag)
        while i < n:
            if flag[i]:
                j = i
                while j < n and flag[j]:
                    j += 1
                if j - i > 20:
                    out.append((i, j))
                i = j
            else:
                i += 1
        return out

    col_runs = runs(cols)
    ys = np.where(mask.any(axis=1))[0]
    y0, y1 = int(ys.min()), int(ys.max()) + 1
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
        tiles.append((cx0 + int(cx.min()), y0 + int(cy.min()), cx0 + int(cx.max()) + 1, y0 + int(cy.max()) + 1))
    return tiles


def scale_face(tile: Image.Image, tw: int, th: int) -> np.ndarray:
    a = np.array(tile.convert("RGBA"))
    rgb = a[:, :, :3].astype(np.float32)
    near_white = (rgb[:, :, 0] > 220) & (rgb[:, :, 1] > 220) & (rgb[:, :, 2] > 220)
    a[near_white, 3] = 0
    im = Image.fromarray(a, "RGBA")
    m = a[:, :, 3] > 40
    if m.any():
        ys, xs = np.where(m)
        im = im.crop((int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1))
    return np.array(im.resize((tw, th), Image.Resampling.NEAREST).convert("RGBA"))


def stamp(dst: np.ndarray, face: np.ndarray, x: int, y: int, w: int, h: int) -> None:
    f = face
    if f.shape[1] != w or f.shape[0] != h:
        f = np.array(Image.fromarray(f, "RGBA").resize((w, h), Image.Resampling.NEAREST))
    for yy in range(h):
        for xx in range(w):
            if f[yy, xx, 3] > 64:
                dst[y + yy, x + xx] = f[yy, xx]
            else:
                dst[y + yy, x + xx] = (0, 0, 0, 0)


def apply_helmet_cutouts(dst: np.ndarray, iron: np.ndarray) -> None:
    """Keep Front cheek plates from art; thin eye slits; gold Φ accents from item icon.

    Iron face opening is NOT applied to the Front island — it was deleting cheeks.
    Gold layout follows art/items/for_artist/phi_chitin_helmet_v2_16x.png, scaled to 8×8 Front UV.
    """
    iron_a = iron[:, :, 3] > 16
    for y in range(0, 16):
        for x in range(0, 32):
            # Leave Front UV (8,8)-(16,16) to the stamped art.
            if 8 <= x < 16 and 8 <= y < 16:
                continue
            if not iron_a[y, x]:
                dst[y, x, 3] = 0

    # Narrow center face gap between cheek plates (looking down / mouth).
    # Starts at y=12 so eye row y=11 stays free.
    for y in range(12, 16):
        for x in (11, 12):
            dst[y, x, 3] = 0

    # Ensure cheek columns stay opaque (restore if scale left holes).
    plate = (48, 38, 95, 255)
    for y in range(11, 16):
        for x in list(range(8, 11)) + list(range(13, 16)):
            if x in (11, 12):
                continue
            if dst[y, x, 3] < 128:
                dst[y, x] = plate
            else:
                dst[y, x, 3] = 255

    # Thin cyan eye slits: 1×2 px each, lowered one row vs previous (was y=10).
    cyan = (90, 220, 235, 255)
    brow = (30, 22, 55, 255)
    plate = (48, 38, 95, 255)
    gold = (253, 206, 90, 255)
    gold_dim = (210, 170, 55, 255)
    # Strip any cyan leftover from the downscaled Front tile.
    for y in range(8, 16):
        for x in range(8, 16):
            if dst[y, x, 3] == 0:
                continue
            r, g, b = int(dst[y, x, 0]), int(dst[y, x, 1]), int(dst[y, x, 2])
            if b > 180 and g > 150:
                dst[y, x] = plate if y >= 12 else brow
    eye_y = 11
    for x in (9, 10):
        dst[eye_y, x] = cyan
    for x in (13, 14):
        dst[eye_y, x] = cyan

    # Gold accents from generated helmet icon (v2), remapped to Front UV (8,8)-(15,15).
    # Crown ticks (icon y=3) → top row of Front.
    dst[8, 10] = gold
    dst[8, 13] = gold
    # Brow bar just above eyes (icon y=6 GG).
    dst[10, 11] = gold
    dst[10, 12] = gold
    # Outer temple ticks (icon x=3/12 y=9).
    dst[12, 8] = gold_dim
    dst[12, 15] = gold_dim
    # Lower cheek ticks (icon x=4/11 y=10).
    dst[13, 9] = gold
    dst[13, 14] = gold
    # Chin prong tips on cheek bottoms (icon mandible tips).
    if dst[15, 10, 3] > 64:
        dst[15, 10] = gold_dim
    if dst[15, 13, 3] > 64:
        dst[15, 13] = gold_dim

    # Small crest flecks on Top UV (8,0)-(15,7) — continues crown.
    if dst[2, 11, 3] > 64:
        dst[2, 11] = gold_dim
    if dst[2, 12, 3] > 64:
        dst[2, 12] = gold_dim


def main() -> None:
    path = copy_grid()
    im = Image.open(path)
    tiles = find_six_tiles(im)
    print("tiles", len(tiles))
    if len(tiles) < 6:
        raise SystemExit(f"expected 6 tiles, got {len(tiles)}")

    faces_dir = CUBES / "helmet_faces_v1"
    faces_dir.mkdir(exist_ok=True)
    named: dict[str, Image.Image] = {}
    for i, box in enumerate(tiles[:6]):
        fname = FACE_ORDER[i]
        crop = im.crop(box)
        crop.save(faces_dir / f"HELMET_{fname}.png")
        named[fname] = crop
        print(f"  HELMET_{fname}", box, crop.size)

    # Load existing 64×64 layer (chest + pauldrons) or bootstrap from iron
    layer_path = OUT / "phi_chitin_layer_1.png"
    if layer_path.exists():
        layer = np.array(Image.open(layer_path).convert("RGBA"))
        if layer.shape[0] < 64 or layer.shape[1] < 64:
            big = np.zeros((64, 64, 4), np.uint8)
            big[: layer.shape[0], : layer.shape[1]] = layer
            layer = big
    else:
        iron = np.array(Image.open(VAN / "iron_layer_1.png").convert("RGBA"))
        layer = np.zeros((64, 64, 4), np.uint8)
        layer[0:32, 0:64] = iron

    iron = np.array(Image.open(VAN / "iron_layer_1.png").convert("RGBA"))

    # Clear old head UV before stamp
    for y in range(0, 16):
        for x in range(0, 32):
            layer[y, x] = (0, 0, 0, 0)

    for fname, (x, y, w, h) in HEAD_UV.items():
        face = scale_face(named[fname], w, h)
        face[:, :, 3] = 255
        stamp(layer, face, x, y, w, h)
        Image.fromarray(face, "RGBA").save(faces_dir / f"HELMET_{fname}_{w}x{h}.png")

    apply_helmet_cutouts(layer, iron)

    OUT.mkdir(parents=True, exist_ok=True)
    Image.fromarray(layer, "RGBA").save(layer_path)
    Image.fromarray(layer, "RGBA").save(CUBES / "phi_chitin_layer_1_with_helmet.png")

    FACE.mkdir(parents=True, exist_ok=True)
    # head-only crop preview
    head = layer[0:16, 0:32].copy()
    prev = Image.new("RGBA", (32 * 8, 16 * 8), (18, 20, 28, 255))
    big = Image.fromarray(head, "RGBA").resize((32 * 8, 16 * 8), Image.Resampling.NEAREST)
    prev.paste(big, (0, 0), big)
    prev.save(FACE / "phi_chitin_helmet_uv_v1_8x.png")

    full = Image.new("RGBA", (64 * 3, 64 * 3), (18, 20, 28, 255))
    fbig = Image.fromarray(layer, "RGBA").resize((64 * 3, 64 * 3), Image.Resampling.NEAREST)
    full.paste(fbig, (0, 0), fbig)
    full.save(FACE / "phi_chitin_layer_1_full_with_helmet_3x.png")

    # ascii check front (G=gold, C=cyan, #=plate, .=hole)
    print("front face UV (8-15,8-15):")
    for y in range(8, 16):
        chars = []
        for x in range(8, 16):
            r, g, b, a = map(int, layer[y, x])
            if a < 16:
                chars.append(".")
            elif r > 180 and g > 140 and b < 130:
                chars.append("G")
            elif b > 180 and g > 150:
                chars.append("C")
            else:
                chars.append("#")
        print(y, "".join(chars))
    print("wrote helmet into layer_1")


if __name__ == "__main__":
    main()
