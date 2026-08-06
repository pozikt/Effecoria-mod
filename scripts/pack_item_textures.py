"""Pack Effecoria item textures.

Default: vanilla silhouette masks + palette / liquid fill (readable MC icons).
Exceptions (original sketches only):
  phi_nut, essonite_shard, vitrified_glass_shard, pure_essonite, vitrified_golem_core
"""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
VAN = ROOT / "art/items/vanilla_refs"
SK = ROOT / "art/items/sketches"
FACE = ROOT / "art/items/for_artist"
OUT = ROOT / "src/main/resources/assets/effecoria/textures/item"
ASSETS = Path(r"C:\Users\2005t\.cursor\projects\c-Users-2005t-Effecoria-mod\assets")

ORIGINAL_SKETCHES: dict[str, str] = {
    "phi_nut": "item_phi_nut_sketch.png",
    "essonite_shard": "item_essonite_shard_sketch.png",
    "vitrified_glass_shard": "item_vitrified_glass_shard_sketch.png",
    "pure_essonite": "item_pure_essonite_sketch.png",
    "vitrified_golem_core": "item_golem_core_sketch.png",
}

PAL = {
    "tonic": ((20, 60, 140), (60, 140, 230), (120, 220, 255), (201, 162, 39)),
    "resonance": ((18, 24, 80), (50, 70, 180), (100, 180, 255), (230, 200, 80)),
    "stimulant": ((10, 80, 160), (40, 180, 255), (200, 245, 255), (255, 210, 60)),
    "purified": ((40, 120, 180), (80, 200, 230), (180, 240, 255), (220, 200, 120)),
    "raw_phi": ((20, 50, 120), (40, 100, 200), (70, 180, 230), (180, 150, 50)),
    "essonite": ((30, 40, 120), (60, 90, 200), (100, 180, 255), (201, 162, 39)),
    "vitrified": ((10, 12, 20), (30, 40, 80), (80, 160, 220), (201, 162, 39)),
    "chitin": ((20, 40, 90), (40, 70, 150), (90, 150, 220), (180, 160, 70)),
}


def ensure_dirs() -> None:
    for p in (SK, FACE, OUT, VAN):
        p.mkdir(parents=True, exist_ok=True)


def load_rgba(path: Path) -> np.ndarray:
    return np.array(Image.open(path).convert("RGBA"))


def save_item(name: str, arr: np.ndarray) -> None:
    img = Image.fromarray(arr, "RGBA")
    img.save(OUT / f"{name}.png")
    img.save(FACE / f"{name}_16x.png")
    prev = Image.new("RGBA", (128, 128), (18, 20, 28, 255))
    big = img.resize((128, 128), Image.Resampling.NEAREST)
    prev.paste(big, (0, 0), big)
    prev.save(FACE / f"{name}_16x_8x.png")
    print("wrote", name)


def copy_sketch(name: str) -> Path | None:
    src = ASSETS / name
    dst = SK / name
    if src.exists():
        dst.write_bytes(src.read_bytes())
        return dst
    return dst if dst.exists() else None


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
    bg = np.median(np.stack(corners, axis=0), axis=0).astype(np.float32)
    rgb = out[:, :, :3].astype(np.float32)
    dist = np.linalg.norm(rgb - bg, axis=2)
    near_white = (rgb[:, :, 0] > 230) & (rgb[:, :, 1] > 230) & (rgb[:, :, 2] > 230)
    grey = (
        (np.abs(rgb[:, :, 0] - rgb[:, :, 1]) < 12)
        & (np.abs(rgb[:, :, 1] - rgb[:, :, 2]) < 12)
        & (rgb.mean(axis=2) > 180)
    )
    out[(dist < 28) | near_white | grey, 3] = 0
    return out


def crop_content(im: Image.Image) -> Image.Image:
    a = punch_flat_bg(np.array(im.convert("RGBA")))
    alpha = a[:, :, 3]
    rgb = a[:, :, :3].astype(np.float32)
    lum = 0.3 * rgb[:, :, 0] + 0.59 * rgb[:, :, 1] + 0.11 * rgb[:, :, 2]
    mask = alpha > 40 if (alpha < 200).mean() > 0.08 else lum > 18
    ys, xs = np.where(mask)
    if len(ys) == 0:
        return Image.fromarray(a, "RGBA")
    y0, y1 = int(ys.min()), int(ys.max()) + 1
    x0, x1 = int(xs.min()), int(xs.max()) + 1
    side = max(y1 - y0, x1 - x0)
    pad = max(2, side // 32)
    side = side + pad * 2
    cy, cx = (y0 + y1) // 2, (x0 + x1) // 2
    half = side // 2
    y0s, x0s = max(0, cy - half), max(0, cx - half)
    y1s, x1s = min(a.shape[0], y0s + side), min(a.shape[1], x0s + side)
    y0s, x0s = max(0, y1s - side), max(0, x1s - side)
    return Image.fromarray(a, "RGBA").crop((x0s, y0s, x1s, y1s))


def nearest16_from_sketch(path: Path) -> np.ndarray:
    """Original item icon: crop + nearest 16, keep own silhouette."""
    tile = crop_content(Image.open(path))
    out = np.array(tile.resize((16, 16), Image.Resampling.NEAREST).convert("RGBA"))
    out[:, :, 3] = np.where(out[:, :, 3] > 96, 255, 0).astype(np.uint8)
    if (out[:, :, 3] > 0).sum() < 8:
        soft = np.array(tile.resize((16, 16), Image.Resampling.NEAREST).convert("RGBA"))
        soft[:, :, 3] = np.where(soft[:, :, 3] > 40, 255, 0).astype(np.uint8)
        return soft
    return out


def nearest16_fill(path: Path) -> np.ndarray:
    """Liquid/fill sketch for vanilla bottle/bucket masks (opaque fill tile)."""
    im = Image.open(path).convert("RGBA")
    a = np.array(im)
    alpha = a[:, :, 3]
    rgb = a[:, :, :3].astype(np.float32)
    lum = 0.3 * rgb[:, :, 0] + 0.59 * rgb[:, :, 1] + 0.11 * rgb[:, :, 2]
    mask = alpha > 40 if (alpha < 200).mean() > 0.1 else lum > 20
    ys, xs = np.where(mask)
    if len(ys) == 0:
        tile = im
    else:
        y0, y1 = int(ys.min()), int(ys.max()) + 1
        x0, x1 = int(xs.min()), int(xs.max()) + 1
        side = max(y1 - y0, x1 - x0)
        cy, cx = (y0 + y1) // 2, (x0 + x1) // 2
        half = side // 2
        y0s, x0s = max(0, cy - half), max(0, cx - half)
        y1s, x1s = min(a.shape[0], y0s + side), min(a.shape[1], x0s + side)
        y0s, x0s = max(0, y1s - side), max(0, x1s - side)
        tile = im.crop((x0s, y0s, x1s, y1s))
    return np.array(tile.resize((16, 16), Image.Resampling.NEAREST).convert("RGBA"))


def solid_fill(pal_key: str, seed: int = 0) -> np.ndarray:
    lo, mid, hi, accent = (np.array(c, np.uint8) for c in PAL[pal_key])
    rng = np.random.default_rng(seed)
    out = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            t = 0.35 + 0.35 * np.sin((x + y) * 0.7) + 0.15 * rng.random()
            if t < 0.4:
                col = lo
            elif t < 0.7:
                col = mid
            else:
                col = hi
            if rng.random() < 0.04:
                col = accent
            out[y, x, :3] = col
            out[y, x, 3] = 255
    return out


def bake_potion(name: str, fill: np.ndarray) -> None:
    overlay = load_rgba(VAN / "potion_overlay.png")
    bottle = load_rgba(VAN / "potion.png")
    fill = fill.copy()
    fill[:, :, 3] = 255

    out = np.zeros((16, 16, 4), np.uint8)
    ol = (0.3 * overlay[:, :, 0] + 0.59 * overlay[:, :, 1] + 0.11 * overlay[:, :, 2]) / 255.0
    for c in range(3):
        out[:, :, c] = np.clip(fill[:, :, c].astype(np.float32) * (0.45 + 0.7 * ol), 0, 255).astype(np.uint8)
    out[:, :, 3] = overlay[:, :, 3]

    ba = bottle[:, :, 3] > 32
    out[ba, :3] = bottle[ba, :3]
    out[ba, 3] = 255
    bright = ba & ((bottle[:, :, 0].astype(int) + bottle[:, :, 1] + bottle[:, :, 2]) > 420)
    out[bright, :3] = bottle[bright, :3]
    save_item(name, out)


def bake_empty_bottle(name: str) -> None:
    bottle = load_rgba(VAN / "glass_bottle.png")
    out = bottle.copy()
    out[:, :, 3] = np.where(out[:, :, 3] > 16, 255, 0)
    m = out[:, :, 3] > 0
    lum = out[:, :, 0].astype(int) + out[:, :, 1] + out[:, :, 2]
    hi = m & (lum > 400)
    out[hi, 0] = np.clip(out[hi, 0].astype(int) - 10, 0, 255)
    out[hi, 1] = np.clip(out[hi, 1].astype(int) + 8, 0, 255)
    out[hi, 2] = np.clip(out[hi, 2].astype(int) + 30, 0, 255)
    save_item(name, out)


def bake_bucket(name: str, fill: np.ndarray) -> None:
    wb = load_rgba(VAN / "water_bucket.png")
    empty = load_rgba(VAN / "bucket.png")
    diff = np.abs(wb[:, :, :3].astype(int) - empty[:, :, :3].astype(int)).sum(axis=2)
    water = (wb[:, :, 3] > 16) & (diff > 40)
    metal = (wb[:, :, 3] > 16) & ~water

    out = np.zeros_like(wb)
    wl = (0.3 * wb[:, :, 0] + 0.59 * wb[:, :, 1] + 0.11 * wb[:, :, 2]) / 255.0
    for y in range(16):
        for x in range(16):
            if water[y, x]:
                col = fill[y, x, :3].astype(np.float32) * (0.55 + 0.55 * wl[y, x])
                out[y, x, :3] = np.clip(col, 0, 255).astype(np.uint8)
                out[y, x, 3] = 255
            elif metal[y, x]:
                out[y, x] = wb[y, x]
    save_item(name, out)


def recolor_template(name: str, template: str, pal_key: str, keep_metal: bool = False) -> None:
    src = load_rgba(VAN / template)
    lo, mid, hi, accent = (np.array(c, np.float32) for c in PAL[pal_key])
    out = np.zeros_like(src)
    m = src[:, :, 3] > 16
    lum = (0.3 * src[:, :, 0] + 0.59 * src[:, :, 1] + 0.11 * src[:, :, 2]) / 255.0
    for y in range(16):
        for x in range(16):
            if not m[y, x]:
                continue
            t = float(lum[y, x])
            if keep_metal and src[y, x, 0] > 140 and abs(int(src[y, x, 0]) - int(src[y, x, 1])) < 25:
                g = int(0.7 * src[y, x, 0])
                out[y, x, :3] = (g, g, min(255, g + 20))
            elif t > 0.72:
                col = hi
            elif t > 0.42:
                col = mid + (hi - mid) * ((t - 0.42) / 0.3)
            else:
                col = lo + (mid - lo) * (t / 0.42)
            if (x * 7 + y * 3) % 41 == 0 and t > 0.35:
                col = accent
            out[y, x, :3] = np.clip(col, 0, 255).astype(np.uint8)
            out[y, x, 3] = 255
    save_item(name, out)


def recolor_tool(name: str, template: str, pal_key: str = "vitrified") -> None:
    src = load_rgba(VAN / template)
    lo, mid, hi, accent = (np.array(c, np.float32) for c in PAL[pal_key])
    out = np.zeros_like(src)
    m = src[:, :, 3] > 16
    rgb = src[:, :, :3].astype(int)
    stick = m & (rgb[:, :, 0] > rgb[:, :, 2] + 15) & (rgb[:, :, 0] > 60) & (rgb[:, :, 0] < 200) & (
        rgb[:, :, 1] < rgb[:, :, 0]
    )
    head = m & ~stick
    lum = (0.3 * src[:, :, 0] + 0.59 * src[:, :, 1] + 0.11 * src[:, :, 2]) / 255.0
    for y in range(16):
        for x in range(16):
            if stick[y, x]:
                out[y, x] = src[y, x]
            elif head[y, x]:
                t = float(lum[y, x])
                if t > 0.65:
                    col = hi
                elif t > 0.35:
                    col = mid
                else:
                    col = lo
                if (x + y) % 11 == 0:
                    col = accent
                out[y, x, :3] = col.astype(np.uint8)
                out[y, x, 3] = 255
    save_item(name, out)


def pack_original(name: str, sketch: str) -> None:
    path = copy_sketch(sketch)
    if path is None or not path.exists():
        raise FileNotFoundError(f"missing original sketch for {name}: {sketch}")
    save_item(name, nearest16_from_sketch(path))


def main() -> None:
    ensure_dirs()

    fills: dict[str, np.ndarray] = {}
    mapping = {
        "tonic": "liquid_phi_tonic_sketch.png",
        "resonance": "liquid_phi_resonance_sketch.png",
        "stimulant": "liquid_phi_stimulant_sketch.png",
        "purified": "liquid_purified_phi_water_sketch.png",
    }
    for key, fname in mapping.items():
        p = copy_sketch(fname)
        fills[key] = nearest16_fill(p) if p and p.exists() else solid_fill(key, hash(key) % 10_000)
        Image.fromarray(fills[key], "RGBA").save(SK / f"fill_{key}_16.png")
    fills["raw_phi"] = solid_fill("raw_phi", 11)

    # --- vanilla-mask items ---
    bake_empty_bottle("phi_flask")
    bake_potion("phi_flask_water", fills["purified"])
    bake_potion("potion_phi_tonic", fills["tonic"])
    bake_potion("potion_phi_resonance", fills["resonance"])
    bake_potion("potion_phi_stimulant", fills["stimulant"])

    bake_bucket("phi_water_bucket", fills["raw_phi"])
    bake_bucket("purified_phi_water_bucket", fills["purified"])

    recolor_template("phi_paper", "paper.png", "essonite")
    recolor_template("essonite_dust", "glowstone_dust.png", "essonite")
    recolor_template("phi_chitin", "netherite_scrap.png", "chitin")
    recolor_template("resonance_focus", "ender_eye.png", "resonance")
    recolor_template("phi_cell", "heart_of_the_sea.png", "tonic")
    recolor_template("gold_filter", "hopper.png", "essonite")
    recolor_template("lead_filter", "hopper.png", "vitrified")

    recolor_tool("vitrified_glass_sword", "iron_sword.png")
    recolor_tool("vitrified_glass_pickaxe", "iron_pickaxe.png")
    recolor_tool("vitrified_glass_axe", "iron_axe.png")
    recolor_tool("vitrified_glass_shovel", "iron_shovel.png")

    for piece, tmpl in [
        ("phi_chitin_helmet", "iron_helmet.png"),
        ("phi_chitin_chestplate", "iron_chestplate.png"),
        ("phi_chitin_leggings", "iron_leggings.png"),
        ("phi_chitin_boots", "iron_boots.png"),
    ]:
        recolor_template(piece, tmpl, "chitin")

    # --- original sketches only ---
    for item, sketch in ORIGINAL_SKETCHES.items():
        pack_original(item, sketch)

    print("item pack complete (vanilla masks + 5 originals)")


if __name__ == "__main__":
    main()
