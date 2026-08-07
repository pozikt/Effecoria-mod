"""Pack Phi-chitin leggings cube-face grid → armor layer_2 (legs + waist)."""
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

# Right leg UV (left leg mirrors in HumanoidModel). Sizes match player leg cuboid 4×12×4.
LEG_UV = {
    "Top": (4, 16, 4, 4),
    "Bottom": (8, 16, 4, 4),
    "Right": (0, 20, 4, 12),
    "Front": (4, 20, 4, 12),
    "Left": (8, 20, 4, 12),
    "Back": (12, 20, 4, 12),
}

# Waistband uses body UV on layer_2; iron only keeps the lower belt rows.
BODY_UV = {
    "Top": (20, 16, 8, 4),
    "Bottom": (28, 16, 8, 4),
    "Right": (16, 20, 4, 12),
    "Front": (20, 20, 8, 12),
    "Left": (28, 20, 4, 12),
    "Back": (32, 20, 8, 12),
}


def copy_grid() -> Path:
    CUBES.mkdir(parents=True, exist_ok=True)
    src = ASSETS / "armor_leggings_cube_grid_v1.png"
    dst = CUBES / "leggings_cube_grid_v1.png"
    dst.write_bytes(src.read_bytes())
    return dst


def find_tiles(im: Image.Image) -> list[tuple[int, int, int, int]]:
    """Detect 12 tiles (2 rows × 6)."""
    a = np.array(im.convert("RGBA"))
    rgb = a[:, :, :3].astype(np.float32)
    lum = rgb.mean(2)
    sat = rgb.max(2) - rgb.min(2)
    purple = (rgb[:, :, 2] > 40) & (rgb[:, :, 0] < 140) & (lum < 190)
    mask = ((lum > 18) & (lum < 210) & (sat > 10)) | purple
    # gold/cyan accents count as content
    gold = (rgb[:, :, 0] > 140) & (rgb[:, :, 1] > 90) & (rgb[:, :, 2] < 140)
    cyan = (rgb[:, :, 2] > 160) & (rgb[:, :, 1] > 120) & (rgb[:, :, 0] < 180)
    mask = mask | gold | cyan

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

    # Prefer tall content bands (ignore label flecks above tiles).
    row_runs = [r for r in runs(mask.mean(axis=1) > 0.02) if r[1] - r[0] > 80]
    col_runs = [r for r in runs(mask.mean(axis=0) > 0.02) if r[1] - r[0] > 80]
    if len(row_runs) < 2:
        ys = np.where(mask.any(axis=1))[0]
        y0, y1 = int(ys.min()), int(ys.max()) + 1
        step = (y1 - y0) // 2
        row_runs = [(y0 + i * step, y0 + (i + 1) * step) for i in range(2)]
    if len(col_runs) < 6:
        xs = np.where(mask.any(axis=0))[0]
        x0, x1 = int(xs.min()), int(xs.max()) + 1
        step = (x1 - x0) // 6
        col_runs = [(x0 + i * step, x0 + (i + 1) * step) for i in range(6)]
    row_runs, col_runs = row_runs[:2], col_runs[:6]

    tiles = []
    for ry0, ry1 in row_runs:
        for cx0, cx1 in col_runs:
            cell = mask[ry0:ry1, cx0:cx1]
            if not cell.any():
                tiles.append((cx0, ry0, cx1, ry1))
                continue
            ys, xs = np.where(cell)
            tiles.append(
                (
                    cx0 + int(xs.min()),
                    ry0 + int(ys.min()),
                    cx0 + int(xs.max()) + 1,
                    ry0 + int(ys.max()) + 1,
                )
            )
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


def apply_iron_silhouette(dst: np.ndarray, iron: np.ndarray) -> None:
    """Keep only pixels iron leggings paint — boots peek through, belt is lower body rows."""
    iron_a = iron[:, :, 3] > 16
    for y in range(dst.shape[0]):
        for x in range(dst.shape[1]):
            if not iron_a[y, x]:
                dst[y, x, 3] = 0


def main() -> None:
    path = copy_grid()
    im = Image.open(path)
    tiles = find_tiles(im)
    print("tiles", len(tiles))
    if len(tiles) < 12:
        raise SystemExit(f"expected 12 tiles, got {len(tiles)}")

    faces_dir = CUBES / "leggings_faces_v1"
    faces_dir.mkdir(exist_ok=True)
    # Prompt order: row0 = RIGHT_LEG, row1 = WAIST/BODY
    named: dict[str, dict[str, Image.Image]] = {"LEG": {}, "BODY": {}}
    groups = ["LEG", "BODY"]
    for i, box in enumerate(tiles[:12]):
        g = groups[i // 6]
        fname = FACE_ORDER[i % 6]
        crop = im.crop(box)
        crop.save(faces_dir / f"{g}_{fname}.png")
        named[g][fname] = crop
        print(f"  {g}_{fname}", box, crop.size)

    iron = np.array(Image.open(VAN / "iron_layer_2.png").convert("RGBA"))
    layer = np.zeros_like(iron)

    # Skip Top/Bottom caps that iron leaves empty (boots / looking down).
    skip = {"Top", "Bottom"}

    for fname, (x, y, w, h) in LEG_UV.items():
        if fname in skip:
            continue
        face = scale_face(named["LEG"][fname], w, h)
        face[:, :, 3] = 255
        stamp(layer, face, x, y, w, h)
        Image.fromarray(face, "RGBA").save(faces_dir / f"LEG_{fname}_{w}x{h}.png")

    # Leg top plate (hip) — iron keeps (4,16) 4×4
    face = scale_face(named["LEG"]["Top"], 4, 4)
    face[:, :, 3] = 255
    stamp(layer, face, 4, 16, 4, 4)
    Image.fromarray(face, "RGBA").save(faces_dir / "LEG_Top_4x4.png")

    # Waistband: iron only keeps y=27..31 (5px). Stamp the TOP of each BODY face
    # (belt / gold band) into that strip — not the bottom of a full 12-tall scale.
    belt_h = 5
    belt_y = 27
    for fname, (x, _y, w, _h) in BODY_UV.items():
        if fname in skip:
            continue
        # Prefer upper third of art (horizontal gold belt lives there).
        raw = np.array(named["BODY"][fname].convert("RGBA"))
        rgb = raw[:, :, :3].astype(np.float32)
        near_white = (rgb[:, :, 0] > 220) & (rgb[:, :, 1] > 220) & (rgb[:, :, 2] > 220)
        raw[near_white, 3] = 0
        m = raw[:, :, 3] > 40
        if m.any():
            ys, xs = np.where(m)
            raw = raw[int(ys.min()) : int(ys.max()) + 1, int(xs.min()) : int(xs.max()) + 1]
        top = raw[: max(1, raw.shape[0] // 3), :, :]
        face = np.array(
            Image.fromarray(top, "RGBA").resize((w, belt_h), Image.Resampling.NEAREST)
        )
        face[:, :, 3] = 255
        stamp(layer, face, x, belt_y, w, belt_h)
        Image.fromarray(face, "RGBA").save(faces_dir / f"BODY_{fname}_{w}x{belt_h}_belt.png")

    apply_iron_silhouette(layer, iron)

    OUT.mkdir(parents=True, exist_ok=True)
    Image.fromarray(layer, "RGBA").save(OUT / "phi_chitin_layer_2.png")
    Image.fromarray(layer, "RGBA").save(CUBES / "phi_chitin_layer_2_leggings.png")

    FACE.mkdir(parents=True, exist_ok=True)
    prev = Image.new("RGBA", (64 * 8, 32 * 8), (18, 20, 28, 255))
    big = Image.fromarray(layer, "RGBA").resize((64 * 8, 32 * 8), Image.Resampling.NEAREST)
    prev.paste(big, (0, 0), big)
    prev.save(FACE / "phi_chitin_leggings_uv_v1_8x.png")

    crop = layer[16:32, 0:40].copy()
    cprev = Image.new("RGBA", (40 * 8, 16 * 8), (18, 20, 28, 255))
    cbig = Image.fromarray(crop, "RGBA").resize((40 * 8, 16 * 8), Image.Resampling.NEAREST)
    cprev.paste(cbig, (0, 0), cbig)
    cprev.save(FACE / "phi_chitin_leggings_uv_crop_8x.png")

    m = layer[:, :, 3] > 16
    print("leg front UV (4-7,20-31):")
    for y in range(20, 32):
        chars = []
        for x in range(4, 8):
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
    print("body front belt (20-27,27-31):")
    for y in range(27, 32):
        chars = []
        for x in range(20, 28):
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
    print("wrote layer_2 leggings, opaque", int(m.sum()))


if __name__ == "__main__":
    main()
