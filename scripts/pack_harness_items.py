"""Pack misc harness item icons: tilted sketch → clean bg/grids → nearest 16×16.

NO vanilla silhouette mask. Own shape after cleanup (filters, focus, cell).
"""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = Path(r"C:\Users\2005t\.cursor\projects\c-Users-2005t-Effecoria-mod\assets")
ART = ROOT / "art/items/harness_icons"
FACE = ROOT / "art/items/for_artist"
OUT = ROOT / "src/main/resources/assets/effecoria/textures/item"

ITEMS: dict[str, str] = {
    "gold_filter": "item_gold_filter_solid_v1.png",
    "lead_filter": "item_lead_filter_solid_v1.png",
    "resonance_focus": "item_resonance_focus_solid_v1.png",
    "phi_cell": "item_phi_cell_solid_v1.png",
    "essonite_dust": "item_essonite_dust_solid_v1.png",
    "phi_paper": "item_phi_paper_solid_v1.png",
    "phi_chitin": "item_phi_chitin_solid_v1.png",
    "breathing_scroll": "item_breathing_scroll_solid_v1.png",
    "magic_primer": "item_magic_primer_solid_v1.png",
}


def ensure_dirs() -> None:
    for p in (ART, FACE, OUT):
        p.mkdir(parents=True, exist_ok=True)


def load_rgba(path: Path) -> np.ndarray:
    return np.array(Image.open(path).convert("RGBA"))


def clean_background(a: np.ndarray) -> np.ndarray:
    """Punch flat bg, near-white, grey/gold/magenta transparency grids."""
    out = a.copy()
    rgb = out[:, :, :3].astype(np.float32)
    alpha = out[:, :, 3]
    h, w = alpha.shape

    # Corner-median flat background
    corners = np.stack(
        [
            out[0, 0, :3],
            out[0, w - 1, :3],
            out[h - 1, 0, :3],
            out[h - 1, w - 1, :3],
        ],
        axis=0,
    ).astype(np.float32)
    bg = np.median(corners, axis=0)
    dist = np.linalg.norm(rgb - bg, axis=2)

    near_white = (rgb[:, :, 0] > 225) & (rgb[:, :, 1] > 225) & (rgb[:, :, 2] > 225)
    # Grey checker / soft grey bg
    grey = (
        (np.abs(rgb[:, :, 0] - rgb[:, :, 1]) < 14)
        & (np.abs(rgb[:, :, 1] - rgb[:, :, 2]) < 14)
        & (rgb.mean(axis=2) > 160)
    )
    # Gold / yellow transparency mesh (common gen artifact)
    gold_grid = (
        (rgb[:, :, 0] > 170)
        & (rgb[:, :, 1] > 130)
        & (rgb[:, :, 2] < 120)
        & (rgb[:, :, 0] > rgb[:, :, 2] + 50)
        & (rgb.mean(axis=2) > 140)
    )
    # Magenta / hot-pink transparency markers
    magenta = (rgb[:, :, 0] > 180) & (rgb[:, :, 2] > 180) & (rgb[:, :, 1] < 120)
    # Near-black canvas when almost fully opaque (gen used solid dark bg)
    near_black = (rgb.mean(axis=2) < 22) & (alpha > 200)

    kill = (dist < 32) | near_white | grey | gold_grid | magenta
    # Only punch near-black if it dominates corners (true bg), not object cores
    if float(near_black.mean()) > 0.35:
        kill = kill | (near_black & (dist < 40))

    # If image was fully opaque, rely more on corner distance
    if (alpha < 200).mean() < 0.05:
        kill = (dist < 36) | near_white | grey | gold_grid | magenta

    out[kill, 3] = 0
    return out


def crop_square(a: np.ndarray) -> np.ndarray:
    m = a[:, :, 3] > 40
    if not m.any():
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


def to_16(a: np.ndarray) -> np.ndarray:
    tile = crop_square(a)
    out = np.array(Image.fromarray(tile, "RGBA").resize((16, 16), Image.Resampling.NEAREST))
    # Hard alpha — no soft fringe
    out[:, :, 3] = np.where(out[:, :, 3] > 96, 255, 0).astype(np.uint8)
    if (out[:, :, 3] > 0).sum() < 10:
        soft = np.array(Image.fromarray(tile, "RGBA").resize((16, 16), Image.Resampling.NEAREST))
        soft[:, :, 3] = np.where(soft[:, :, 3] > 40, 255, 0).astype(np.uint8)
        return soft
    # Second pass: kill leftover near-white / gold fringe at 16
    rgb = out[:, :, :3].astype(int)
    fringe = (
        ((rgb[:, :, 0] > 230) & (rgb[:, :, 1] > 230) & (rgb[:, :, 2] > 230))
        | ((rgb[:, :, 0] > 200) & (rgb[:, :, 1] > 160) & (rgb[:, :, 2] < 100) & (out[:, :, 3] > 0))
    )
    # Don't delete gold that is clearly interior accent: only isolated / edge-ish
    # Safer: only punch pure white fringe
    white = (rgb[:, :, 0] > 235) & (rgb[:, :, 1] > 235) & (rgb[:, :, 2] > 235)
    out[white, 3] = 0
    return out


def copy_sketch(asset_name: str) -> Path:
    src = ASSETS / asset_name
    dst = ART / asset_name
    if not src.exists():
        raise FileNotFoundError(f"missing sketch: {src}")
    dst.write_bytes(src.read_bytes())
    return dst


def save_all(name: str, arr: np.ndarray) -> None:
    Image.fromarray(arr, "RGBA").save(ART / f"{name}_16x.png")
    Image.fromarray(arr, "RGBA").save(OUT / f"{name}.png")
    Image.fromarray(arr, "RGBA").save(FACE / f"{name}_16x.png")
    prev = Image.new("RGBA", (128, 128), (18, 20, 28, 255))
    big = Image.fromarray(arr, "RGBA").resize((128, 128), Image.Resampling.NEAREST)
    prev.paste(big, (0, 0), big)
    prev.save(FACE / f"{name}_16x_8x.png")
    prev.save(ART / f"{name}_16x_8x.png")
    print(f"wrote {name} opaque={(arr[:, :, 3] > 0).sum()}")


def pack_one(name: str, sketch_file: str) -> None:
    path = copy_sketch(sketch_file)
    raw = load_rgba(path)
    cleaned = clean_background(raw)
    Image.fromarray(cleaned, "RGBA").save(ART / f"{name}_cleaned.png")
    out = to_16(cleaned)
    save_all(name, out)


def main() -> None:
    ensure_dirs()
    for name, sketch in ITEMS.items():
        pack_one(name, sketch)
    print("harness items packed (clean+scale, no vanilla mask)")


if __name__ == "__main__":
    main()
