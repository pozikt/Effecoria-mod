"""Crop sink block face concept sheets -> 16x16 opaque block textures + cube models."""
from __future__ import annotations

import json
from collections import deque
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
BLOCK_OUT = ROOT / "src/main/resources/assets/effecoria/textures/block"
MODELS_BLOCK = ROOT / "src/main/resources/assets/effecoria/models/block"
MODELS_ITEM = ROOT / "src/main/resources/assets/effecoria/models/item"
CONCEPTS = Path(r"C:\Users\2005t\.cursor\projects\c-Users-2005t-Effecoria-mod\assets")
ART = ROOT / "art"
PREVIEW = ROOT / "art/sink_redesign_preview"


def flood_punch_bg(img: Image.Image) -> Image.Image:
    if img.mode != "RGBA":
        img = img.convert("RGBA")
    w, h = img.size
    px = img.load()

    def is_bg(r: int, g: int, b: int, a: int) -> bool:
        if a < 8:
            return True
        mx = max(r, g, b)
        chroma = mx - min(r, g, b)
        if mx <= 40 and chroma <= 16:
            return True
        if r >= 245 and g >= 245 and b >= 245:
            return True
        return False

    visited = [[False] * w for _ in range(h)]
    q: deque[tuple[int, int]] = deque()
    for x in range(w):
        q.append((x, 0))
        q.append((x, h - 1))
    for y in range(h):
        q.append((0, y))
        q.append((w - 1, y))
    while q:
        x, y = q.popleft()
        if not (0 <= x < w and 0 <= y < h) or visited[y][x]:
            continue
        visited[y][x] = True
        r, g, b, a = px[x, y]
        if not is_bg(r, g, b, a):
            continue
        px[x, y] = (r, g, b, 0)
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= nx < w and 0 <= ny < h and not visited[ny][nx]:
                nr, ng, nb, na = px[nx, ny]
                if is_bg(nr, ng, nb, na) or (
                    na > 0
                    and abs(nr - r) + abs(ng - g) + abs(nb - b) <= 24
                    and max(nr, ng, nb) <= 70
                ):
                    q.append((nx, ny))
    return img


def crop_grid(path: Path, cols: int, rows: int, inset: float = 0.04) -> list[Image.Image]:
    img = flood_punch_bg(Image.open(path))
    w, h = img.size
    cw, ch = w // cols, h // rows
    cells = []
    for r in range(rows):
        for c in range(cols):
            x0 = int(c * cw + cw * inset)
            y0 = int(r * ch + ch * inset)
            x1 = int((c + 1) * cw - cw * inset)
            y1 = int((r + 1) * ch - ch * inset)
            cells.append(img.crop((x0, y0, min(w, x1), min(h, y1))))
    return cells


def bbox(img: Image.Image, amin: int = 20) -> tuple[int, int, int, int] | None:
    px = img.load()
    w, h = img.size
    x0, y0, x1, y1 = w, h, -1, -1
    for y in range(h):
        for x in range(w):
            if px[x, y][3] >= amin:
                x0, y0 = min(x0, x), min(y0, y)
                x1, y1 = max(x1, x), max(y1, y)
    if x1 < 0:
        return None
    return x0, y0, x1 + 1, y1 + 1


def to_opaque_face(cell: Image.Image, size: int = 16) -> Image.Image:
    """Full-bleed opaque 16x16 block face."""
    cell = flood_punch_bg(cell.convert("RGBA"))
    box = bbox(cell)
    if not box:
        return Image.new("RGBA", (size, size), (80, 80, 85, 255))
    cropped = cell.crop(box)
    # Prefer near-square content; if isometric (tall diamond), take central square
    cw, ch = cropped.size
    side = min(cw, ch)
    if abs(cw - ch) > side * 0.25:
        # center square crop — helps isometric leftovers
        if cw > ch:
            x0 = (cw - side) // 2
            cropped = cropped.crop((x0, 0, x0 + side, ch))
        else:
            y0 = (ch - side) // 2
            # for isometric cubes, top face is upper half — bias upward a bit
            y0 = max(0, y0 - side // 8)
            cropped = cropped.crop((0, y0, cw, min(ch, y0 + side)))
            # re-square
            cw, ch = cropped.size
            side = min(cw, ch)
            cropped = cropped.crop(((cw - side) // 2, (ch - side) // 2, (cw - side) // 2 + side, (ch - side) // 2 + side))

    face = cropped.resize((size, size), resample=Image.Resampling.BOX)
    sp = face.load()
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    op = out.load()
    samples = [sp[x, y][:3] for y in range(size) for x in range(size) if sp[x, y][3] > 80]
    samples.sort(key=lambda t: sum(t))
    fr, fg, fb = samples[len(samples) // 3] if samples else (70, 70, 75)
    for y in range(size):
        for x in range(size):
            r, g, b, a = sp[x, y]
            if a >= 70:
                op[x, y] = (r, g, b, 255)
            else:
                op[x, y] = (fr, fg, fb, 255)
    return out


def write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def write_cube(name: str, top: str, side: str, bottom: str) -> None:
    write_json(
        MODELS_BLOCK / f"{name}.json",
        {
            "parent": "minecraft:block/cube",
            "textures": {
                "particle": side,
                "down": bottom,
                "up": top,
                "north": side,
                "south": side,
                "east": side,
                "west": side,
            },
        },
    )
    write_json(MODELS_ITEM / f"{name}.json", {"parent": f"effecoria:block/{name}"})


def main() -> None:
    PREVIEW.mkdir(parents=True, exist_ok=True)
    ART.mkdir(parents=True, exist_ok=True)

    def copy_art(name: str) -> Path:
        src = CONCEPTS / name
        dst = ART / name
        if src.exists():
            dst.write_bytes(src.read_bytes())
        return src if src.exists() else dst

    # Prefer dedicated solo flats (cleaner than grid cells).
    conc_top = to_opaque_face(Image.open(copy_art("sink_phi_concrete_top_solo.png")))
    conc_side = to_opaque_face(Image.open(copy_art("sink_phi_concrete_side_solo.png")))
    # Bottom: darker recolor of side
    conc_bottom = conc_side.copy()
    bp = conc_bottom.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = bp[x, y]
            bp[x, y] = (max(0, int(r * 0.72)), max(0, int(g * 0.72)), max(0, int(b * 0.75)), a)

    conc_top.save(BLOCK_OUT / "phi_concrete_top.png")
    conc_side.save(BLOCK_OUT / "phi_concrete_side.png")
    conc_bottom.save(BLOCK_OUT / "phi_concrete_bottom.png")
    conc_side.save(BLOCK_OUT / "phi_concrete.png")
    write_cube(
        "phi_concrete",
        "effecoria:block/phi_concrete_top",
        "effecoria:block/phi_concrete_side",
        "effecoria:block/phi_concrete_bottom",
    )

    anc_top = to_opaque_face(Image.open(copy_art("sink_omega_anchor_top_solo.png")))
    anc_side = to_opaque_face(Image.open(copy_art("sink_omega_anchor_side_solo.png")))
    anc_bottom = anc_side.copy()
    ap = anc_bottom.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = ap[x, y]
            # dim cyan pillars + keep dark body
            if b > r + 20 and g > r + 10:  # cyan-ish
                ap[x, y] = (max(0, r // 2), max(0, g // 2), max(0, b // 2), a)
            else:
                ap[x, y] = (max(0, int(r * 0.65)), max(0, int(g * 0.65)), max(0, int(b * 0.7)), a)

    anc_top.save(BLOCK_OUT / "omega_anchor_top.png")
    anc_side.save(BLOCK_OUT / "omega_anchor_side.png")
    anc_bottom.save(BLOCK_OUT / "omega_anchor_bottom.png")
    anc_top.save(BLOCK_OUT / "omega_anchor.png")
    write_cube(
        "omega_anchor",
        "effecoria:block/omega_anchor_top",
        "effecoria:block/omega_anchor_side",
        "effecoria:block/omega_anchor_bottom",
    )

    # Also keep multi-face sheets in art/ if present
    for name in ("sink_phi_concrete_faces_sheet.png", "sink_omega_anchor_faces_sheet.png"):
        copy_art(name)

    for n in (
        "phi_concrete_top",
        "phi_concrete_side",
        "phi_concrete_bottom",
        "omega_anchor_top",
        "omega_anchor_side",
        "omega_anchor_bottom",
    ):
        im = Image.open(BLOCK_OUT / f"{n}.png")
        im.resize((128, 128), Image.Resampling.NEAREST).save(PREVIEW / f"out_{n}_x8.png")

    print("sink block faces OK (solo)")


if __name__ == "__main__":
    main()
