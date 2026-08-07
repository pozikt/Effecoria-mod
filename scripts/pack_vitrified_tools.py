"""Pack vitrified glass tool icons: solid heads, iron silhouette mask (no chip-off parts).

Cycle: generate solid sketch → mask to vanilla iron outline → 16×16 game + previews.
"""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = Path(r"C:\Users\2005t\.cursor\projects\c-Users-2005t-Effecoria-mod\assets")
VAN = ROOT / "art/items/vanilla_refs"
TOOLS = ROOT / "art/items/tool_icons"
FACE = ROOT / "art/items/for_artist"
OUT = ROOT / "src/main/resources/assets/effecoria/textures/item"

# Generated solid sketches (no jagged shard silhouettes).
TOOLS_MAP: dict[str, tuple[str, str]] = {
    "vitrified_glass_sword": ("item_vitrified_sword_solid_v1.png", "iron_sword.png"),
    "vitrified_glass_pickaxe": ("item_vitrified_pickaxe_solid_v1.png", "iron_pickaxe.png"),
    "vitrified_glass_axe": ("item_vitrified_axe_solid_v1.png", "iron_axe.png"),
    "vitrified_glass_shovel": ("item_vitrified_shovel_solid_v1.png", "iron_shovel.png"),
}

# Deep fused glass + cyan edge + gold accent (darker than diamond blue).
LO = np.array((8, 10, 18), np.float32)
MID = np.array((22, 30, 55), np.float32)
HI = np.array((45, 85, 130), np.float32)
ACCENT = np.array((201, 162, 39), np.float32)
CYAN = np.array((70, 210, 230), np.float32)


def ensure_dirs() -> None:
    for p in (TOOLS, FACE, OUT):
        p.mkdir(parents=True, exist_ok=True)


def load_rgba(path: Path) -> np.ndarray:
    return np.array(Image.open(path).convert("RGBA"))


def punch_flat_bg(a: np.ndarray) -> np.ndarray:
    out = a.copy()
    alpha = out[:, :, 3]
    if (alpha < 200).mean() > 0.08:
        return out
    h, w = alpha.shape
    corners = [
        out[0, 0, :3].astype(int),
        out[0, w - 1, :3].astype(int),
        out[h - 1, 0, :3].astype(int),
        out[h - 1, w - 1, :3].astype(int),
    ]
    bg = np.median(np.stack(corners, 0), axis=0)
    dist = np.abs(out[:, :, :3].astype(int) - bg).sum(2)
    out[dist < 45, 3] = 0
    return out


def copy_sketch(asset_name: str) -> Path:
    src = ASSETS / asset_name
    dst = TOOLS / asset_name
    if not src.exists():
        raise FileNotFoundError(f"missing generated sketch: {src}")
    dst.write_bytes(src.read_bytes())
    return dst


def nearest16(path: Path) -> np.ndarray:
    a = punch_flat_bg(load_rgba(path))
    # Crop opaque content then nearest to 16×16 (preserves silhouette density).
    m = a[:, :, 3] > 40
    if m.any():
        ys, xs = np.where(m)
        a = a[int(ys.min()) : int(ys.max()) + 1, int(xs.min()) : int(xs.max()) + 1]
    return np.array(Image.fromarray(a, "RGBA").resize((16, 16), Image.Resampling.NEAREST))


def is_stick(rgb: np.ndarray, mask: np.ndarray) -> np.ndarray:
    """Vanilla wooden stick: brown-ish, R-dominant."""
    r, g, b = rgb[:, :, 0], rgb[:, :, 1], rgb[:, :, 2]
    return (
        mask
        & (r > b + 15)
        & (r > 60)
        & (r < 220)
        & (g < r)
        & (b < r - 10)
    )


def is_goldish(rgb: np.ndarray) -> np.ndarray:
    r, g, b = rgb[:, :, 0].astype(int), rgb[:, :, 1].astype(int), rgb[:, :, 2].astype(int)
    return (r > 150) & (g > 100) & (b < 130) & (r > b + 40)


def is_cyanish(rgb: np.ndarray) -> np.ndarray:
    r, g, b = rgb[:, :, 0].astype(int), rgb[:, :, 1].astype(int), rgb[:, :, 2].astype(int)
    return (b > 160) & (g > 120) & (r < 180)


def pack_one(name: str, sketch_file: str, iron_file: str) -> None:
    sketch_path = copy_sketch(sketch_file)
    sketch = nearest16(sketch_path)
    iron = load_rgba(VAN / iron_file)
    if iron.shape[0] != 16 or iron.shape[1] != 16:
        iron = np.array(Image.fromarray(iron, "RGBA").resize((16, 16), Image.Resampling.NEAREST))

    iron_a = iron[:, :, 3] > 16
    stick = is_stick(iron[:, :, :3], iron_a)
    head = iron_a & ~stick

    out = np.zeros((16, 16, 4), np.uint8)
    # Stick: keep vanilla wood pixels exactly (solid, readable).
    out[stick] = iron[stick]

    iron_lum = (0.3 * iron[:, :, 0] + 0.59 * iron[:, :, 1] + 0.11 * iron[:, :, 2]) / 255.0
    sk_a = sketch[:, :, 3] > 40

    # 4-neighbor edge of the head island — cyan glow lives here, not as solid fill.
    head_edge = np.zeros_like(head)
    for y in range(16):
        for x in range(16):
            if not head[y, x]:
                continue
            for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                yy, xx = y + dy, x + dx
                if yy < 0 or yy >= 16 or xx < 0 or xx >= 16 or not iron_a[yy, xx]:
                    head_edge[y, x] = True
                    break

    for y in range(16):
        for x in range(16):
            if not head[y, x]:
                continue
            t = float(iron_lum[y, x])
            # Bias toward dark glass — iron highlights become mid plate, not diamond-cyan fill.
            if t > 0.78:
                col = HI
            elif t > 0.45:
                col = MID
            else:
                col = LO

            if sk_a[y, x] and is_goldish(sketch[y : y + 1, x : x + 1]):
                col = ACCENT
            elif head_edge[y, x]:
                col = CYAN if t > 0.5 else HI
            elif t > 0.85 and (x * 3 + y * 5) % 17 == 0:
                col = ACCENT

            out[y, x, :3] = np.clip(col, 0, 255).astype(np.uint8)
            out[y, x, 3] = 255

    # CRITICAL: alpha == iron silhouette only — forbids chip-off sketch pixels.
    out[~iron_a, 3] = 0
    out[iron_a, 3] = 255

    # Save staging + game
    Image.fromarray(out, "RGBA").save(TOOLS / f"{name}_16x.png")
    Image.fromarray(out, "RGBA").save(OUT / f"{name}.png")
    Image.fromarray(out, "RGBA").save(FACE / f"{name}_16x.png")
    prev = Image.new("RGBA", (128, 128), (18, 20, 28, 255))
    big = Image.fromarray(out, "RGBA").resize((128, 128), Image.Resampling.NEAREST)
    prev.paste(big, (0, 0), big)
    prev.save(FACE / f"{name}_16x_8x.png")
    prev.save(TOOLS / f"{name}_16x_8x.png")

    # ASCII opacity check vs iron
    mismatch = int(((out[:, :, 3] > 16) != iron_a).sum())
    print(f"wrote {name} (alpha mismatch vs iron: {mismatch})")


def main() -> None:
    ensure_dirs()
    for name, (sketch, iron) in TOOLS_MAP.items():
        pack_one(name, sketch, iron)
    print("all vitrified tools packed")


if __name__ == "__main__":
    main()
