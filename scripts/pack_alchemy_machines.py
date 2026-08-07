"""Pack alchemy machine art: mortar item (tilted clean) + burner/alembic block faces."""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = Path(r"C:\Users\2005t\.cursor\projects\c-Users-2005t-Effecoria-mod\assets")
ART = ROOT / "art/phi_alchemy/machine_icons"
FACE = ROOT / "art/items/for_artist"
OUT_ITEM = ROOT / "src/main/resources/assets/effecoria/textures/item"
OUT_BLOCK = ROOT / "src/main/resources/assets/effecoria/textures/block"

# Reuse harness cleaners conceptually (inline to keep script standalone).
ITEM_SKETCH = "item_mortar_pestle_solid_v1.png"
BLOCKS: dict[str, str] = {
    "essence_burner": "block_essence_burner_v1.png",
    "essence_burner_on": "block_essence_burner_on_v1.png",
    "essence_alembic": "block_essence_alembic_v1.png",
}


def ensure_dirs() -> None:
    for p in (ART, FACE, OUT_ITEM, OUT_BLOCK):
        p.mkdir(parents=True, exist_ok=True)


def load_rgba(path: Path) -> np.ndarray:
    return np.array(Image.open(path).convert("RGBA"))


def copy_asset(name: str) -> Path:
    src = ASSETS / name
    dst = ART / name
    if not src.exists():
        raise FileNotFoundError(src)
    dst.write_bytes(src.read_bytes())
    return dst


def clean_bg(a: np.ndarray) -> np.ndarray:
    out = a.copy()
    rgb = out[:, :, :3].astype(np.float32)
    h, w = out.shape[:2]
    corners = np.stack(
        [out[0, 0, :3], out[0, w - 1, :3], out[h - 1, 0, :3], out[h - 1, w - 1, :3]],
        0,
    ).astype(np.float32)
    bg = np.median(corners, axis=0)
    dist = np.linalg.norm(rgb - bg, axis=2)
    near_white = (rgb[:, :, 0] > 225) & (rgb[:, :, 1] > 225) & (rgb[:, :, 2] > 225)
    grey = (
        (np.abs(rgb[:, :, 0] - rgb[:, :, 1]) < 14)
        & (np.abs(rgb[:, :, 1] - rgb[:, :, 2]) < 14)
        & (rgb.mean(2) > 160)
    )
    gold_grid = (
        (rgb[:, :, 0] > 170)
        & (rgb[:, :, 1] > 130)
        & (rgb[:, :, 2] < 120)
        & (rgb[:, :, 0] > rgb[:, :, 2] + 50)
        & (rgb.mean(2) > 140)
    )
    magenta = (rgb[:, :, 0] > 180) & (rgb[:, :, 2] > 180) & (rgb[:, :, 1] < 120)
    kill = (dist < 32) | near_white | grey | gold_grid | magenta
    if (out[:, :, 3] < 200).mean() < 0.05:
        kill = (dist < 36) | near_white | grey | gold_grid | magenta
    out[kill, 3] = 0
    return out


def crop_content(a: np.ndarray) -> np.ndarray:
    m = a[:, :, 3] > 40
    if not m.any():
        # fully opaque block art — use whole image
        return a
    ys, xs = np.where(m)
    y0, y1 = int(ys.min()), int(ys.max()) + 1
    x0, x1 = int(xs.min()), int(xs.max()) + 1
    side = max(y1 - y0, x1 - x0)
    pad = max(2, side // 24)
    side = side + pad * 2
    cy, cx = (y0 + y1) // 2, (x0 + x1) // 2
    half = side // 2
    y0s = max(0, cy - half)
    x0s = max(0, cx - half)
    y1s = min(a.shape[0], y0s + side)
    x1s = min(a.shape[1], x0s + side)
    y0s, x0s = max(0, y1s - side), max(0, x1s - side)
    return a[y0s:y1s, x0s:x1s]


def to_item_16(a: np.ndarray) -> np.ndarray:
    tile = crop_content(clean_bg(a))
    out = np.array(Image.fromarray(tile, "RGBA").resize((16, 16), Image.Resampling.NEAREST))
    out[:, :, 3] = np.where(out[:, :, 3] > 96, 255, 0).astype(np.uint8)
    white = (out[:, :, 0] > 235) & (out[:, :, 1] > 235) & (out[:, :, 2] > 235)
    out[white, 3] = 0
    return out


def to_block_16(a: np.ndarray) -> np.ndarray:
    """Full opaque 16×16 for cube_all — keep full frame (block gens fill the square)."""
    # Light punch only near-white / magenta / gold grid, not dark indigo corners.
    out = a.copy()
    rgb = out[:, :, :3].astype(np.float32)
    near_white = (rgb[:, :, 0] > 230) & (rgb[:, :, 1] > 230) & (rgb[:, :, 2] > 230)
    magenta = (rgb[:, :, 0] > 180) & (rgb[:, :, 2] > 180) & (rgb[:, :, 1] < 120)
    out[near_white | magenta, 3] = 0
    # Resize whole canvas; if mostly opaque already, nearest full image.
    if (out[:, :, 3] > 40).mean() > 0.7:
        tile = out
    else:
        tile = crop_content(out)
    out16 = np.array(Image.fromarray(tile, "RGBA").resize((16, 16), Image.Resampling.NEAREST))
    fill = np.array((18, 22, 48, 255), np.uint8)
    for y in range(16):
        for x in range(16):
            if out16[y, x, 3] < 128:
                out16[y, x] = fill
            else:
                out16[y, x, 3] = 255
    return out16


def find_row_tiles(im: Image.Image, n: int = 5) -> list[tuple[int, int, int, int]]:
    a = np.array(im.convert("RGBA"))
    rgb = a[:, :, :3].astype(np.float32)
    lum = rgb.mean(2)
    sat = rgb.max(2) - rgb.min(2)
    mask = (lum > 18) & (lum < 220) & (sat > 8)
    col = mask.mean(0) > 0.02

    def runs(flag: np.ndarray) -> list[tuple[int, int]]:
        out = []
        i, m = 0, len(flag)
        while i < m:
            if flag[i]:
                j = i
                while j < m and flag[j]:
                    j += 1
                if j - i > 40:
                    out.append((i, j))
                i = j
            else:
                i += 1
        return out

    cols = [r for r in runs(col) if r[1] - r[0] > 80][:n]
    if len(cols) < n:
        xs = np.where(mask.any(0))[0]
        x0, x1 = int(xs.min()), int(xs.max()) + 1
        step = (x1 - x0) // n
        cols = [(x0 + i * step, x0 + (i + 1) * step) for i in range(n)]
    ys = np.where(mask.any(1))[0]
    y0, y1 = int(ys.min()), int(ys.max()) + 1
    tiles = []
    for cx0, cx1 in cols:
        cell = mask[y0:y1, cx0:cx1]
        if not cell.any():
            tiles.append((cx0, y0, cx1, y1))
            continue
        cy, cx = np.where(cell)
        tiles.append((cx0 + int(cx.min()), y0 + int(cy.min()), cx0 + int(cx.max()) + 1, y0 + int(cy.max()) + 1))
    return tiles


def pack_mortar_block_faces() -> None:
    path = copy_asset("block_mortar_faces_grid_v1.png")
    im = Image.open(path)
    tiles = find_row_tiles(im, 5)
    names = ["mortar_side", "mortar_inner", "mortar_rim", "mortar_bottom", "mortar_pestle"]
    if len(tiles) < 5:
        raise SystemExit(f"expected 5 mortar faces, got {len(tiles)}")
    for name, box in zip(names, tiles):
        crop = im.crop(box)
        arr = to_block_16(np.array(crop.convert("RGBA")))
        Image.fromarray(arr, "RGBA").save(OUT_BLOCK / f"{name}.png")
        Image.fromarray(arr, "RGBA").save(ART / f"{name}_16x.png")
        save_preview(name, arr, ART)
        print("wrote", name, box)


def save_preview(name: str, arr: np.ndarray, folder: Path) -> None:
    Image.fromarray(arr, "RGBA").save(folder / f"{name}.png")
    prev = Image.new("RGBA", (128, 128), (18, 20, 28, 255))
    big = Image.fromarray(arr, "RGBA").resize((128, 128), Image.Resampling.NEAREST)
    prev.paste(big, (0, 0), big)
    prev.save(FACE / f"{name}_16x_8x.png")
    prev.save(ART / f"{name}_16x_8x.png")


def pack_mortar_item() -> None:
    raw = load_rgba(copy_asset(ITEM_SKETCH))
    out = to_item_16(raw)
    Image.fromarray(out, "RGBA").save(OUT_ITEM / "mortar_and_pestle.png")
    Image.fromarray(out, "RGBA").save(ART / "mortar_and_pestle_16x.png")
    # also keep legacy block item-looking sheet path used elsewhere
    Image.fromarray(out, "RGBA").save(OUT_BLOCK / "mortar_and_pestle.png")
    save_preview("mortar_and_pestle", out, ART)
    print("wrote mortar_and_pestle item opaque", int((out[:, :, 3] > 0).sum()))


def pack_blocks() -> None:
    for name, sketch in BLOCKS.items():
        raw = load_rgba(copy_asset(sketch))
        out = to_block_16(raw)
        Image.fromarray(out, "RGBA").save(OUT_BLOCK / f"{name}.png")
        Image.fromarray(out, "RGBA").save(ART / f"{name}_16x.png")
        save_preview(name, out, ART)
        print("wrote block", name)


def main() -> None:
    ensure_dirs()
    pack_mortar_item()
    pack_mortar_block_faces()
    pack_blocks()
    print("alchemy machines packed")


if __name__ == "__main__":
    main()
