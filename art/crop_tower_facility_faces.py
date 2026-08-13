"""Crop tower-facility face concept sheets -> 16x16 textures + cube models."""
from __future__ import annotations

import json
from pathlib import Path

import numpy as np
from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "art" / "tower_facility"
BLOCK_OUT = ROOT / "src/main/resources/assets/effecoria/textures/block"
ITEM_OUT = ROOT / "src/main/resources/assets/effecoria/textures/item"
MODELS_BLOCK = ROOT / "src/main/resources/assets/effecoria/models/block"
PREVIEW = ART / "preview_crops"

# Cell order for 2×3 sheets labeled FRONT BACK LEFT / RIGHT TOP BOTTOM
LAYOUT_FBL_RTB = {"north": 0, "south": 1, "west": 2, "east": 3, "up": 4, "down": 5}


def smooth(s: np.ndarray, k: int = 7) -> np.ndarray:
    k = k | 1
    pad = np.pad(s, (k // 2, k // 2), mode="edge")
    return np.convolve(pad, np.ones(k) / k, mode="valid")


def border_peaks(score: np.ndarray, min_sep: int, thr_factor: float = 0.5) -> list[int]:
    thr = score.mean() + thr_factor * (score.max() - score.mean())
    idxs: list[int] = []
    i = 0
    n = len(score)
    while i < n:
        if score[i] >= thr:
            j = i
            while j + 1 < n and score[j + 1] >= thr * 0.65:
                j += 1
            idxs.append(i + int(np.argmax(score[i : j + 1])))
            i = max(j + 1, i + min_sep)
        else:
            i += 1
    return idxs


def intervals_from_peaks(peaks: list[int], limit: int, lo: int = 170, hi: int = 560) -> list[tuple[int, int]]:
    pts = sorted(set([0] + peaks + [limit - 1]))
    out: list[tuple[int, int]] = []
    for a, b in zip(pts, pts[1:]):
        if lo <= (b - a) <= hi:
            out.append((a, b))
    return out


def peak_face_squares(path: Path, cols: int = 3, rows: int = 2) -> list[Image.Image] | None:
    img = Image.open(path).convert("RGB")
    arr = np.asarray(img).astype(np.float32)
    h, w = arr.shape[:2]
    gray = arr.mean(axis=2)
    gy = np.abs(np.diff(gray, axis=0))
    gx = np.abs(np.diff(gray, axis=1))
    rp = [p for p in border_peaks(smooth(gy.mean(axis=1)), 55) if 30 < p < h - 15]
    cp = [p for p in border_peaks(smooth(gx.mean(axis=0)), 55) if 15 < p < w - 15]
    riv = sorted(intervals_from_peaks(rp, h), key=lambda t: -(t[1] - t[0]))[:rows]
    civ = sorted(intervals_from_peaks(cp, w), key=lambda t: -(t[1] - t[0]))[:cols]
    if len(riv) < rows or len(civ) < cols:
        return None
    riv = sorted(riv)
    civ = sorted(civ)
    faces: list[Image.Image] = []
    for ry0, ry1 in riv:
        for cx0, cx1 in civ:
            ix0 = cx0 + int((cx1 - cx0) * 0.07)
            ix1 = cx1 - int((cx1 - cx0) * 0.07)
            iy0 = ry0 + int((ry1 - ry0) * 0.05)
            iy1 = ry1 - int((ry1 - ry0) * 0.10)
            side = min(ix1 - ix0, iy1 - iy0)
            sx = ix0 + (ix1 - ix0 - side) // 2
            sy = iy0 + (iy1 - iy0 - side) // 2
            faces.append(img.crop((sx, sy, sx + side, sy + side)))
    return faces


def fixed_face_squares(
    path: Path,
    *,
    cols: int = 3,
    rows: int = 2,
    title_frac: float = 0.10,
    label_frac: float = 0.20,
) -> list[Image.Image]:
    img = Image.open(path).convert("RGB")
    w, h = img.size
    title = int(h * title_frac)
    body_h = h - title
    cw, ch = w // cols, body_h // rows
    faces: list[Image.Image] = []
    for r in range(rows):
        for c in range(cols):
            x0, y0 = c * cw, title + r * ch
            x1, y1 = x0 + cw, y0 + ch
            fx0 = int(x0 + cw * 0.08)
            fy0 = int(y0 + ch * 0.06)
            fx1 = int(x1 - cw * 0.08)
            fy1 = int(y1 - ch * label_frac)
            fw, fh = fx1 - fx0, fy1 - fy0
            side = min(fw, fh)
            sx = fx0 + (fw - side) // 2
            sy = fy0 + (fh - side) // 2
            faces.append(img.crop((sx, sy, sx + side, sy + side)))
    return faces


def to_16_opaque(face: Image.Image) -> Image.Image:
    out = face.convert("RGBA").resize((16, 16), resample=Image.Resampling.BOX)
    px = out.load()
    for y in range(16):
        for x in range(16):
            r, g, b, _ = px[x, y]
            px[x, y] = (r, g, b, 255)
    return out


def write_cube_model(block_id: str) -> None:
    model = {
        "parent": "minecraft:block/cube",
        "textures": {
            "particle": f"effecoria:block/{block_id}_front",
            "north": f"effecoria:block/{block_id}_front",
            "south": f"effecoria:block/{block_id}_back",
            "west": f"effecoria:block/{block_id}_left",
            "east": f"effecoria:block/{block_id}_right",
            "up": f"effecoria:block/{block_id}_top",
            "down": f"effecoria:block/{block_id}_bottom",
        },
    }
    (MODELS_BLOCK / f"{block_id}.json").write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")


def save_block(block_id: str, squares: list[Image.Image], layout: dict[str, int]) -> None:
    PREVIEW.mkdir(parents=True, exist_ok=True)
    role = {
        "front": layout["north"],
        "back": layout["south"],
        "left": layout["west"],
        "right": layout["east"],
        "top": layout["up"],
        "bottom": layout["down"],
    }
    for name, idx in role.items():
        face = to_16_opaque(squares[idx])
        face.save(BLOCK_OUT / f"{block_id}_{name}.png")
        face.resize((64, 64), Image.Resampling.NEAREST).save(PREVIEW / f"{block_id}_{name}_4x.png")
        if name == "front":
            face.save(BLOCK_OUT / f"{block_id}.png")
    write_cube_model(block_id)
    print(f"wrote {block_id}")


def crop_item_icon(path: Path) -> Image.Image:
    img = Image.open(path).convert("RGBA")
    w, h = img.size
    cell = img.crop((int(w * 0.25), int(h * 0.12), int(w * 0.75), int(h * 0.88)))
    arr = np.asarray(cell.convert("RGB")).astype(np.int16)
    r, g, b = arr[..., 0], arr[..., 1], arr[..., 2]
    # gray sheet bg
    grayish = (np.abs(r - g) < 18) & (np.abs(g - b) < 18) & (r > 70) & (r < 200)
    ys, xs = np.where(~grayish)
    if len(xs) < 50:
        return Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    x0, y0, x1, y1 = int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1
    pad = 4
    x0, y0 = max(0, x0 - pad), max(0, y0 - pad)
    x1, y1 = min(cell.width, x1 + pad), min(cell.height, y1 + pad)
    bw, bh = x1 - x0, y1 - y0
    side = min(bw, bh)
    sx = x0 + (bw - side) // 2
    sy = y0 + (bh - side) // 2
    cropped = cell.crop((sx, sy, sx + side, sy + side))
    px = cropped.load()
    for y in range(cropped.height):
        for x in range(cropped.width):
            rr, gg, bb, aa = px[x, y]
            if abs(rr - gg) < 16 and abs(gg - bb) < 16 and 70 < rr < 200:
                px[x, y] = (0, 0, 0, 0)
    out = cropped.resize((16, 16), resample=Image.Resampling.BOX)
    px = out.load()
    for y in range(16):
        for x in range(16):
            rr, gg, bb, aa = px[x, y]
            if aa < 35 or (abs(rr - gg) < 14 and abs(gg - bb) < 14 and 70 < rr < 190):
                px[x, y] = (0, 0, 0, 0)
    return out


def main() -> None:
    BLOCK_OUT.mkdir(parents=True, exist_ok=True)
    ITEM_OUT.mkdir(parents=True, exist_ok=True)
    PREVIEW.mkdir(parents=True, exist_ok=True)

    jobs = [
        ("foundation_amulet", "foundation_amulet_faces.png"),
        ("omega_damper", "omega_damper_faces.png"),
        ("phi_air_synth", "phi_air_synth_faces.png"),
        ("phi_water_purifier", "phi_water_purifier_faces.png"),
        ("tower_console", "tower_console_faces.png"),
        ("regen_chamber", "regen_chamber_core_faces.png"),
    ]
    for block_id, concept in jobs:
        path = ART / concept
        squares = peak_face_squares(path) or fixed_face_squares(path)
        if len(squares) != 6:
            squares = fixed_face_squares(path)
        save_block(block_id, squares, LAYOUT_FBL_RTB)

    # Hull 2×2
    hull_path = ART / "regen_capsule_hull_panels.png"
    hull_squares = peak_face_squares(hull_path, cols=2, rows=2) or fixed_face_squares(
        hull_path, cols=2, rows=2, title_frac=0.08, label_frac=0.18
    )
    names = ("side_wall", "floor", "rim_top", "corner_post")
    for name, sq in zip(names, hull_squares, strict=True):
        face = to_16_opaque(sq)
        face.save(BLOCK_OUT / f"regen_capsule_{name}.png")
        face.resize((64, 64), Image.Resampling.NEAREST).save(PREVIEW / f"regen_capsule_{name}_4x.png")
        if name == "side_wall":
            face.save(BLOCK_OUT / "regen_capsule_hull.png")
            face.resize((64, 64), Image.Resampling.NEAREST).save(PREVIEW / "regen_capsule_hull_4x.png")
    print("wrote regen_capsule hull")

    icon = crop_item_icon(ART / "psi_focus_face.png")
    icon.save(ITEM_OUT / "psi_focus.png")
    icon.resize((64, 64), Image.Resampling.NEAREST).save(PREVIEW / "psi_focus_4x.png")
    print("wrote psi_focus")


if __name__ == "__main__":
    main()
