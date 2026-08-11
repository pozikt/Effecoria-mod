"""Bake phi_cobble + purified_obsidian from concept solos."""
from __future__ import annotations

from collections import deque
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
CONCEPTS = Path(r"C:\Users\2005t\.cursor\projects\c-Users-2005t-Effecoria-mod\assets")
ART = ROOT / "art"
BLOCK = ROOT / "src/main/resources/assets/effecoria/textures/block"
PREVIEW = ROOT / "art/sink_redesign_preview"


def punch_black(img: Image.Image) -> Image.Image:
    img = img.convert("RGBA")
    w, h = img.size
    px = img.load()

    def bg(r: int, g: int, b: int, a: int) -> bool:
        return a < 8 or max(r, g, b) <= 12

    vis = [[False] * w for _ in range(h)]
    q: deque[tuple[int, int]] = deque()
    for x in range(w):
        q.append((x, 0))
        q.append((x, h - 1))
    for y in range(h):
        q.append((0, y))
        q.append((w - 1, y))
    while q:
        x, y = q.popleft()
        if not (0 <= x < w and 0 <= y < h) or vis[y][x]:
            continue
        vis[y][x] = True
        r, g, b, a = px[x, y]
        if not bg(r, g, b, a):
            continue
        px[x, y] = (0, 0, 0, 0)
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            q.append((nx, ny))
    return img


def to_face(path: Path) -> Image.Image:
    img = punch_black(Image.open(path))
    px = img.load()
    w, h = img.size
    x0, y0, x1, y1 = w, h, -1, -1
    for y in range(h):
        for x in range(w):
            if px[x, y][3] >= 20:
                x0, y0 = min(x0, x), min(y0, y)
                x1, y1 = max(x1, x), max(y1, y)
    cropped = img.crop((x0, y0, x1 + 1, y1 + 1)) if x1 >= 0 else img
    cw, ch = cropped.size
    side = min(cw, ch)
    cropped = cropped.crop(
        ((cw - side) // 2, (ch - side) // 2, (cw - side) // 2 + side, (ch - side) // 2 + side)
    )
    mid = cropped.resize((32, 32), resample=Image.Resampling.BOX)
    face = mid.resize((16, 16), resample=Image.Resampling.NEAREST)
    sp = face.load()
    out = Image.new("RGBA", (16, 16))
    op = out.load()
    samples = [sp[x, y][:3] for y in range(16) for x in range(16) if sp[x, y][3] > 100]
    samples.sort(key=lambda t: sum(t))
    fr, fg, fb = samples[len(samples) // 6] if samples else (10, 10, 18)
    for y in range(16):
        for x in range(16):
            r, g, b, a = sp[x, y]
            op[x, y] = (r, g, b, 255) if a >= 35 else (fr, fg, fb, 255)
    return out


def main() -> None:
    ART.mkdir(exist_ok=True)
    PREVIEW.mkdir(exist_ok=True)
    mapping = [
        ("sink_phi_cobble_solo.png", "phi_cobble.png"),
        ("sink_purified_obsidian_solo_v2.png", "purified_obsidian.png"),
    ]
    for src_name, out_name in mapping:
        src = CONCEPTS / src_name
        if not src.exists():
            # fallback names
            alt = CONCEPTS / src_name.replace("_v2", "")
            src = alt if alt.exists() else src
        ART.joinpath(src.name).write_bytes(src.read_bytes())
        face = to_face(src)
        face.save(BLOCK / out_name)
        stem = out_name.removesuffix(".png")
        face.resize((128, 128), Image.Resampling.NEAREST).save(PREVIEW / f"out_{stem}_x8.png")
        print("wrote", out_name)

    for sheet in (
        "sink_phi_cobble_purified_obsidian_sheet.png",
        "sink_purified_obsidian_solo.png",
    ):
        p = CONCEPTS / sheet
        if p.exists():
            ART.joinpath(sheet).write_bytes(p.read_bytes())
    print("phi_cobble + purified_obsidian OK")


if __name__ == "__main__":
    main()
