"""Recolor Φ-chitin armor layers + item icons → crystal / star essonite.

Does NOT modify phi_chitin_* sources. Icons get inventory padding (~3px).
"""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ARMOR = ROOT / "src/main/resources/assets/effecoria/textures/models/armor"
ITEM = ROOT / "src/main/resources/assets/effecoria/textures/item"
FACE = ROOT / "art/items/for_artist"

# Crystal ultramarine ramp (shadow → highlight)
CRYSTAL_PLATE = np.array(
    [
        [8, 18, 55],
        [15, 42, 110],
        [30, 70, 160],
        [55, 105, 200],
        [90, 145, 230],
    ],
    dtype=np.float32,
)
CRYSTAL_GOLD = np.array([212, 168, 75], dtype=np.float32)
CRYSTAL_GOLD_HI = np.array([232, 200, 120], dtype=np.float32)
CRYSTAL_GLOW = np.array([100, 180, 235], dtype=np.float32)  # Φ-fabric / cyan → soft blue glow

STAR_PLATE = np.array(
    [
        [140, 165, 200],
        [180, 205, 235],
        [210, 225, 245],
        [230, 240, 252],
        [245, 248, 255],
    ],
    dtype=np.float32,
)
STAR_GOLD = np.array([240, 200, 90], dtype=np.float32)
STAR_GOLD_HI = np.array([255, 230, 140], dtype=np.float32)
STAR_GLOW = np.array([180, 220, 255], dtype=np.float32)

PIECES = ("helmet", "chestplate", "leggings", "boots")


def is_gold(r: np.ndarray, g: np.ndarray, b: np.ndarray) -> np.ndarray:
    return (r > 140) & (g > 90) & (b < 140) & (r > b + 40)


def is_cyan(r: np.ndarray, g: np.ndarray, b: np.ndarray) -> np.ndarray:
    return (b > 150) & (g > 120) & (r < 180) & (b + g > r * 2.2)


def plate_ramp(lum: np.ndarray, ramp: np.ndarray) -> np.ndarray:
    """Map luminance 0..255 → ramp colors."""
    t = np.clip(lum / 255.0, 0, 1)
    idx = t * (len(ramp) - 1)
    i0 = np.floor(idx).astype(np.int32)
    i1 = np.minimum(i0 + 1, len(ramp) - 1)
    f = (idx - i0)[..., None]
    return ramp[i0] * (1 - f) + ramp[i1] * f


def recolor(arr: np.ndarray, plate: np.ndarray, gold: np.ndarray, gold_hi: np.ndarray, glow: np.ndarray) -> np.ndarray:
    out = arr.copy()
    a = out[:, :, 3] > 16
    if not a.any():
        return out
    rgb = out[:, :, :3].astype(np.float32)
    r, g, b = rgb[:, :, 0], rgb[:, :, 1], rgb[:, :, 2]
    lum = rgb.mean(axis=2)
    gold_m = is_gold(r, g, b) & a
    cyan_m = is_cyan(r, g, b) & a & ~gold_m
    plate_m = a & ~gold_m & ~cyan_m

    rgb[plate_m] = plate_ramp(lum[plate_m], plate)
    # gold: keep relative brightness
    if gold_m.any():
        t = np.clip((lum[gold_m] - 80) / 140.0, 0, 1)[:, None]
        rgb[gold_m] = gold * (1 - t) + gold_hi * t
    if cyan_m.any():
        t = np.clip(lum[cyan_m] / 220.0, 0.35, 1.0)[:, None]
        rgb[cyan_m] = glow * t

    out[:, :, :3] = np.clip(rgb, 0, 255).astype(np.uint8)
    return out


def save_preview(arr: np.ndarray, name: str, scale: int = 4) -> None:
    FACE.mkdir(parents=True, exist_ok=True)
    im = Image.fromarray(arr, "RGBA")
    big = im.resize((im.width * scale, im.height * scale), Image.Resampling.NEAREST)
    canvas = Image.new("RGBA", big.size, (18, 20, 28, 255))
    canvas.paste(big, (0, 0), big)
    canvas.save(FACE / name)


def main() -> None:
    sources = {
        "layer_1": ARMOR / "phi_chitin_layer_1.png",
        "layer_2": ARMOR / "phi_chitin_layer_2.png",
        "extras": ARMOR / "phi_chitin_chest_extras.png",
    }
    for key, path in sources.items():
        if not path.exists():
            raise SystemExit(f"missing {path}")

    crystal_opts = (CRYSTAL_PLATE, CRYSTAL_GOLD, CRYSTAL_GOLD_HI, CRYSTAL_GLOW)
    star_opts = (STAR_PLATE, STAR_GOLD, STAR_GOLD_HI, STAR_GLOW)

    mapping = [
        ("crystal_essonite_layer_1.png", sources["layer_1"], crystal_opts),
        ("crystal_essonite_layer_2.png", sources["layer_2"], crystal_opts),
        ("crystal_essonite_chest_extras.png", sources["extras"], crystal_opts),
        ("star_essonite_layer_1.png", sources["layer_1"], star_opts),
        ("star_essonite_layer_2.png", sources["layer_2"], star_opts),
        ("star_essonite_chest_extras.png", sources["extras"], star_opts),
    ]
    for out_name, src, opts in mapping:
        arr = recolor(np.array(Image.open(src).convert("RGBA")), *opts)
        Image.fromarray(arr, "RGBA").save(ARMOR / out_name)
        if "layer_1" in out_name:
            save_preview(arr, out_name.replace(".png", "_4x.png"), 4)
        print("wrote", out_name)

    for piece in PIECES:
        src = ITEM / f"phi_chitin_{piece}.png"
        base = np.array(Image.open(src).convert("RGBA"))
        for prefix, opts in (("crystal_essonite", crystal_opts), ("star_essonite", star_opts)):
            # Exact copy of chitin icon geometry (size + alpha); colors only.
            arr = recolor(base, *opts)
            if arr.shape != base.shape or not np.array_equal(arr[:, :, 3], base[:, :, 3]):
                raise SystemExit(f"icon geometry drift on {piece}")
            out = f"{prefix}_{piece}.png"
            Image.fromarray(arr, "RGBA").save(ITEM / out)
            save_preview(arr, out.replace(".png", "_8x.png"), 8)
            print(out, "1:1 from phi_chitin_" + piece)

    print("done (phi_chitin untouched)")


if __name__ == "__main__":
    main()
