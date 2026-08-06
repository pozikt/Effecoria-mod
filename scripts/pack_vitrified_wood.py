"""Pack vitrified log/branches sketches -> 16x16 game textures."""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "art/vitrified_wastes"
SK = ART / "sketches"
FACE = ART / "for_artist"
OUT = ROOT / "src/main/resources/assets/effecoria/textures/block"
ASSETS = Path(r"C:\Users\2005t\.cursor\projects\c-Users-2005t-Effecoria-mod\assets")


def copy_sketch(name: str) -> Path:
    src = ASSETS / name
    dst = SK / name
    if src.exists():
        dst.write_bytes(src.read_bytes())
    return dst


def crop_tile(im: Image.Image) -> Image.Image:
    a = np.array(im.convert("RGBA"))
    rgb = a[:, :, :3].astype(np.float32)
    alpha = a[:, :, 3]
    lum = 0.3 * rgb[:, :, 0] + 0.59 * rgb[:, :, 1] + 0.11 * rgb[:, :, 2]
    # Prefer alpha content for transparent sketches; else luminance variance.
    if (alpha < 200).mean() > 0.15:
        mask = alpha > 32
        ys, xs = np.where(mask)
        if len(ys) == 0:
            return im
        y0, y1 = int(ys.min()), int(ys.max()) + 1
        x0, x1 = int(xs.min()), int(xs.max()) + 1
        # pad a bit toward square
        h, w = y1 - y0, x1 - x0
        side = max(h, w, 8)
        cy, cx = (y0 + y1) // 2, (x0 + x1) // 2
        half = (side + 1) // 2
        y0s = max(0, cy - half)
        x0s = max(0, cx - half)
        y1s = min(a.shape[0], y0s + side)
        x1s = min(a.shape[1], x0s + side)
        y0s = max(0, y1s - side)
        x0s = max(0, x1s - side)
        return im.crop((x0s, y0s, x1s, y1s))

    row_std = lum.std(axis=1)
    col_std = lum.std(axis=0)
    thr_r = max(8.0, float(np.percentile(row_std, 35)))
    thr_c = max(8.0, float(np.percentile(col_std, 35)))
    rows = np.where(row_std >= thr_r)[0]
    cols = np.where(col_std >= thr_c)[0]
    y0, y1 = int(rows[0]), int(rows[-1]) + 1
    x0, x1 = int(cols[0]), int(cols[-1]) + 1
    h, w = y1 - y0, x1 - x0
    side = max(h, w)
    cy, cx = (y0 + y1) // 2, (x0 + x1) // 2
    half = side // 2
    y0s = max(0, cy - half)
    x0s = max(0, cx - half)
    y1s = min(a.shape[0], y0s + side)
    x1s = min(a.shape[1], x0s + side)
    y0s = max(0, y1s - side)
    x0s = max(0, x1s - side)
    return im.crop((x0s, y0s, x1s, y1s))


def to16_opaque(im: Image.Image) -> Image.Image:
    tile = crop_tile(im).resize((16, 16), Image.Resampling.NEAREST).convert("RGBA")
    a = np.array(tile)
    a[:, :, 3] = 255
    return Image.fromarray(a, "RGBA")


def to16_cross(im: Image.Image, bush_ref: Path | None = None) -> Image.Image:
    """Keep alpha. If sketch is opaque square, punch bg via corner color + optional bush mask."""
    tile = crop_tile(im).resize((16, 16), Image.Resampling.NEAREST).convert("RGBA")
    a = np.array(tile)
    # If almost fully opaque, key out near-corner / flat bg.
    if (a[:, :, 3] > 200).mean() > 0.92:
        corners = np.stack([a[0, 0, :3], a[0, -1, :3], a[-1, 0, :3], a[-1, -1, :3]]).astype(int)
        bg = corners.mean(axis=0)
        diff = np.abs(a[:, :, :3].astype(int) - bg).sum(axis=2)
        # also treat very bright / very uniform as bg if present
        transparent = diff < 45
        a[transparent, 3] = 0
        a[~transparent, 3] = 255
    else:
        a[:, :, 3] = np.where(a[:, :, 3] > 96, 255, 0)

    # Prefer bush silhouette if sketch wrongly became a log-top (circular dense fill).
    opaque_frac = (a[:, :, 3] > 0).mean()
    if bush_ref and bush_ref.exists() and opaque_frac > 0.55:
        bush = np.array(Image.open(bush_ref).convert("RGBA").resize((16, 16), Image.Resampling.NEAREST))
        bush_m = bush[:, :, 3] > 128
        # recolor bush silhouette with sketch colors sampled from opaque pixels
        colors = a[a[:, :, 3] > 0][:, :3]
        if len(colors) == 0:
            colors = np.array([[20, 24, 40], [40, 60, 120], [94, 200, 255]], dtype=np.uint8)
        out = np.zeros((16, 16, 4), dtype=np.uint8)
        # map bush luminance to palette
        bl = (0.3 * bush[:, :, 0] + 0.59 * bush[:, :, 1] + 0.11 * bush[:, :, 2]).astype(float)
        # sort palette by brightness
        pal = colors[np.random.default_rng(0).choice(len(colors), size=min(64, len(colors)), replace=False)]
        pal_l = 0.3 * pal[:, 0] + 0.59 * pal[:, 1] + 0.11 * pal[:, 2]
        order = np.argsort(pal_l)
        pal = pal[order]
        lo, mid, hi = pal[0], pal[len(pal) // 2], pal[-1]
        for y in range(16):
            for x in range(16):
                if not bush_m[y, x]:
                    continue
                t = bl[y, x] / 255.0
                if t < 0.35:
                    col = lo
                elif t < 0.7:
                    col = mid
                else:
                    col = hi
                # rare cyan/gold on tips
                if bush[y, x, 1] > 180 and (x + y) % 7 == 0:
                    col = np.array([94, 200, 255], dtype=np.uint8)
                elif (x * 3 + y * 5) % 29 == 0:
                    col = np.array([201, 162, 39], dtype=np.uint8)
                out[y, x, :3] = col
                out[y, x, 3] = 255
        return Image.fromarray(out, "RGBA")

    return Image.fromarray(a, "RGBA")


def save(name: str, img: Image.Image) -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    FACE.mkdir(parents=True, exist_ok=True)
    img.save(OUT / f"{name}.png")
    img.save(FACE / f"{name}_16x.png")
    img.resize((128, 128), Image.Resampling.NEAREST).save(FACE / f"{name}_16x_8x.png")
    print("wrote", name, "opaque_frac", float(np.mean(np.array(img)[:, :, 3] > 0)))


def main() -> None:
    side_p = copy_sketch("vitrified_log_side_sketch.png")
    top_p = copy_sketch("vitrified_log_top_sketch.png")
    br_p = copy_sketch("vitrified_branches_sketch.png")

    save("vitrified_log", to16_opaque(Image.open(side_p)))
    save("vitrified_log_top", to16_opaque(Image.open(top_p)))
    bush = ART / "vanilla_refs/dead_bush.png"
    save("vitrified_branches", to16_cross(Image.open(br_p), bush))


if __name__ == "__main__":
    main()
