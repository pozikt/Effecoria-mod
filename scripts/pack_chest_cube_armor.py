"""Pack chestplate cube-face grid → armor layer_1 body UV + pauldron face crops."""
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

# BODY UV on armor layer_1 (64x32)
BODY_UV = {
    "Front": (20, 20, 8, 12),
    "Back": (32, 20, 8, 12),
    "Left": (28, 20, 4, 12),
    "Right": (16, 20, 4, 12),
    "Top": (20, 16, 8, 4),
    "Bottom": (28, 16, 8, 4),
}
FACE_ORDER = ["Front", "Back", "Left", "Right", "Top", "Bottom"]


def copy_grid() -> Path:
    CUBES.mkdir(parents=True, exist_ok=True)
    src = ASSETS / "armor_chest_cube_grid_v1.png"
    dst = CUBES / "chest_cube_grid_v1.png"
    dst.write_bytes(src.read_bytes())
    return dst


def find_tiles(im: Image.Image) -> list[tuple[int, int, int, int]]:
    """Detect 18 roughly-square content tiles (3 rows x 6)."""
    a = np.array(im.convert("RGBA"))
    rgb = a[:, :, :3].astype(np.float32)
    # content: purple/cyan plates, not dark bg, not white labels
    lum = rgb.mean(2)
    sat = rgb.max(2) - rgb.min(2)
    mask = (lum > 18) & (lum < 210) & ((sat > 12) | (rgb[:, :, 2] > rgb[:, :, 0] + 10))
    # also keep near-black plate interiors inside bounding boxes later — use purple-ish
    purple = (rgb[:, :, 2] > 40) & (rgb[:, :, 0] < 120) & (lum < 180)
    mask = mask | purple

    # project to find row bands
    row_density = mask.mean(axis=1)
    col_density = mask.mean(axis=0)
    # threshold
    rows = row_density > 0.02
    cols = col_density > 0.02

    def runs(flag: np.ndarray) -> list[tuple[int, int]]:
        out = []
        i = 0
        n = len(flag)
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

    row_runs = runs(rows)
    col_runs = runs(cols)
    # expect ~3 row bands and ~6 col bands; if merged, split evenly
    h, w = a.shape[:2]
    if len(row_runs) < 3:
        # split content bbox into 3
        ys = np.where(mask.any(axis=1))[0]
        y0, y1 = int(ys.min()), int(ys.max()) + 1
        step = (y1 - y0) // 3
        row_runs = [(y0 + i * step, y0 + (i + 1) * step) for i in range(3)]
    if len(col_runs) < 6:
        xs = np.where(mask.any(axis=0))[0]
        x0, x1 = int(xs.min()), int(xs.max()) + 1
        step = (x1 - x0) // 6
        col_runs = [(x0 + i * step, x0 + (i + 1) * step) for i in range(6)]

    # take first 3 rows and 6 cols
    row_runs = row_runs[:3]
    col_runs = col_runs[:6]
    tiles = []
    for ry0, ry1 in row_runs:
        for cx0, cx1 in col_runs:
            # tighten to content inside cell
            cell = mask[ry0:ry1, cx0:cx1]
            if cell.mean() < 0.01:
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
    # punch light bg / labels leftover
    a = np.array(tile.convert("RGBA"))
    rgb = a[:, :, :3].astype(np.float32)
    near_white = (rgb[:, :, 0] > 220) & (rgb[:, :, 1] > 220) & (rgb[:, :, 2] > 220)
    a[near_white, 3] = 0
    # if mostly transparent after punch, keep original
    im = Image.fromarray(a, "RGBA")
    # crop opaque
    m = a[:, :, 3] > 40
    if m.any():
        ys, xs = np.where(m)
        im = im.crop((int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1))
    return np.array(im.resize((tw, th), Image.Resampling.NEAREST).convert("RGBA"))


def stamp(dst: np.ndarray, face: np.ndarray, x: int, y: int, w: int, h: int) -> None:
    f = face
    if f.shape[1] != w or f.shape[0] != h:
        f = np.array(Image.fromarray(f, "RGBA").resize((w, h), Image.Resampling.NEAREST))
    # opaque stamp
    for yy in range(h):
        for xx in range(w):
            if f[yy, xx, 3] > 64:
                dst[y + yy, x + xx] = f[yy, xx]
            else:
                # keep plate fill if transparent hole
                if dst[y + yy, x + xx, 3] == 0:
                    dst[y + yy, x + xx] = (30, 22, 55, 255)


def apply_body_cutouts(dst: np.ndarray, iron: np.ndarray) -> None:
    """Neck scoop + free waist — iron silhouette, then deepen for comfort."""
    # Body region x16-39, y16-31
    iron_a = iron[:, :, 3] > 16
    for y in range(16, 32):
        for x in range(16, 40):
            if not iron_a[y, x]:
                dst[y, x, 3] = 0

    # Never fill body top/bottom caps — frees looking down / waist underside
    for y in range(16, 20):
        for x in range(20, 36):
            dst[y, x, 3] = 0

    # Deeper neck scoop on front (x20-27, y20-21)
    for y in (20, 21):
        for x in range(21, 27):
            dst[y, x, 3] = 0
    dst[20, 20, 3] = 0
    dst[20, 27, 3] = 0

    # Conical waist hem (not a solid flat crop): taper inward toward bottom.
    # Front (20-27) and back (32-39):
    #   y28: clear outer 1px each side
    #   y29: clear outer 2px
    #   y30: clear outer 3px
    #   y31: fully clear
    for base_x in (20, 32):  # front, back
        for y, inset in ((28, 1), (29, 2), (30, 3), (31, 4)):
            for x in range(base_x, base_x + inset):
                dst[y, x, 3] = 0
            for x in range(base_x + 8 - inset, base_x + 8):
                dst[y, x, 3] = 0
        for x in range(base_x, base_x + 8):
            dst[31, x, 3] = 0

    # Sides (4px wide): taper so lower rows shrink to a point
    # right x16-19, left x28-31
    for side_x in (16, 28):
        # y28: keep center 2px, clear outer
        dst[28, side_x, 3] = 0
        dst[28, side_x + 3, 3] = 0
        # y29: keep center 1-2px
        dst[29, side_x, 3] = 0
        dst[29, side_x + 1, 3] = 0
        dst[29, side_x + 3, 3] = 0
        # y30-31: clear
        for y in (30, 31):
            for x in range(side_x, side_x + 4):
                dst[y, x, 3] = 0


def main() -> None:
    path = copy_grid()
    im = Image.open(path)
    tiles = find_tiles(im)
    print("tiles", len(tiles))
    if len(tiles) < 18:
        raise SystemExit(f"expected 18 tiles, got {len(tiles)}")

    # save face crops
    faces_dir = CUBES / "chest_faces_v1"
    faces_dir.mkdir(exist_ok=True)
    named: dict[str, dict[str, Image.Image]] = {"BODY": {}, "L_PAULDRON": {}, "R_PAULDRON": {}}
    groups = ["BODY", "L_PAULDRON", "R_PAULDRON"]
    for i, box in enumerate(tiles[:18]):
        g = groups[i // 6]
        fname = FACE_ORDER[i % 6]
        crop = im.crop(box)
        crop.save(faces_dir / f"{g}_{fname}.png")
        named[g][fname] = crop
        print(f"  {g}_{fname}", box, crop.size)

    # build layer_1 from iron silhouette for helmet/boots/arms interim + body from faces
    iron = np.array(Image.open(VAN / "iron_layer_1.png").convert("RGBA"))
    out = np.zeros_like(iron)
    # keep non-body iron as dark indigo placeholder so full set still renders
    m = iron[:, :, 3] > 16
    out[m, :3] = (40, 32, 70)
    out[m, 3] = 255

    body = named["BODY"]
    for fname, (x, y, w, h) in BODY_UV.items():
        if fname in ("Top", "Bottom"):
            # skip caps — cutouts keep neck/waist open
            continue
        face = scale_face(body[fname], w, h)
        face[:, :, 3] = 255
        stamp(out, face, x, y, w, h)
        Image.fromarray(face, "RGBA").save(faces_dir / f"BODY_{fname}_{w}x{h}.png")

    apply_body_cutouts(out, iron)

    # Strip vanilla arm sleeves from layer — pauldrons replace shoulder mass.
    # Arm UV: tops (44,16)/(8,16) 4x4, sides (40-55,20-31) and boot-adjacent leftovers.
    for y in range(16, 32):
        for x in range(40, 56):
            out[y, x, 3] = 0
    # right/left arm tops only (keep foot tops at x8 if those are boots — iron uses 8,16 for feet)
    for y in range(16, 20):
        for x in range(44, 48):
            out[y, x, 3] = 0
        for x in range(48, 56):
            out[y, x, 3] = 0

    # Expand to 64×64: top = vanilla armor UV, bottom = pauldron cube nets for ModelPart texOffs.
    layer = np.zeros((64, 64, 4), np.uint8)
    layer[0:32, 0:64] = out

    # ModelPart UV for addBox(w=5,h=4,d=6) at texOffs(u,v):
    #   Top (u+d, v) 5×6; Bottom (u+d+w, v) 5×6
    #   Right (u, v+d) 6×4; Front (u+d, v+d) 5×4; Left (u+d+w, v+d) 6×4; Back (u+d+w+d, v+d) 5×4
    def stamp_pauldron_cube(dst: np.ndarray, faces: dict, u: int, v: int) -> None:
        d, w, h = 6, 5, 4
        layout = {
            "Top": (u + d, v, w, d),
            "Bottom": (u + d + w, v, w, d),
            "Right": (u, v + d, d, h),
            "Front": (u + d, v + d, w, h),
            "Left": (u + d + w, v + d, d, h),
            "Back": (u + d + w + d, v + d, w, h),
        }
        for fname, (px, py, fw, fh) in layout.items():
            f = scale_face(faces[fname], fw, fh)
            f[:, :, 3] = 255
            stamp(dst, f, px, py, fw, fh)

    stamp_pauldron_cube(layer, named["R_PAULDRON"], 0, 32)
    stamp_pauldron_cube(layer, named["L_PAULDRON"], 32, 32)

    OUT.mkdir(parents=True, exist_ok=True)
    Image.fromarray(layer, "RGBA").save(OUT / "phi_chitin_layer_1.png")
    Image.fromarray(layer, "RGBA").save(CUBES / "phi_chitin_layer_1_64.png")

    # Keep extras sheet as face reference
    atlas = layer[32:48, 0:64].copy()
    Image.fromarray(atlas, "RGBA").save(OUT / "phi_chitin_chest_extras.png")
    Image.fromarray(atlas, "RGBA").save(CUBES / "phi_chitin_chest_extras.png")

    FACE.mkdir(parents=True, exist_ok=True)
    prev = Image.new("RGBA", (64 * 3, 64 * 3), (18, 20, 28, 255))
    big = Image.fromarray(layer, "RGBA").resize((64 * 3, 64 * 3), Image.Resampling.NEAREST)
    prev.paste(big, (0, 0), big)
    prev.save(FACE / "phi_chitin_layer_1_chest_cube_v1_4x.png")
    print("wrote 64x64 layer_1 with pauldron cubes UV")


if __name__ == "__main__":
    main()
