"""Party D: vanilla-water Φ fluids, star-reactor faces, cyan glass."""
from __future__ import annotations

import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
JAR = ROOT / "build/moddev/artifacts/neoforge-21.1.242-client-extra-aka-minecraft-resources.jar"
OUT = ROOT / "src/main/resources/assets/effecoria/textures/block"

PHI = ((8, 40, 52), (24, 92, 108), (48, 168, 184), (180, 240, 245))
PURIFIED = ((12, 55, 58), (36, 140, 130), (90, 220, 190), (230, 255, 240))
STAR = ((18, 22, 38), (40, 52, 78), (90, 120, 150), (200, 230, 245))
GLASS = ((36, 88, 102), (64, 150, 168), (140, 215, 225), (240, 255, 255))


def lum(r: int, g: int, b: int) -> float:
    return (0.3 * r + 0.59 * g + 0.11 * b) / 255.0


def lerp(a, b, t: float):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def shade(pal, t: float):
    lo, a, b, hi = pal
    t = max(0.0, min(1.0, t))
    if t < 1.0 / 3.0:
        return lerp(lo, a, t * 3.0)
    if t < 2.0 / 3.0:
        return lerp(a, b, (t - 1.0 / 3.0) * 3.0)
    return lerp(b, hi, (t - 2.0 / 3.0) * 3.0)


def jar_bytes(z: zipfile.ZipFile, name: str) -> bytes:
    return z.read(f"assets/minecraft/textures/block/{name}")


def recolor_rgba(im: Image.Image, pal, keep_alpha: bool = True) -> Image.Image:
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a < 8:
                px[x, y] = (0, 0, 0, 0)
                continue
            rgb = shade(pal, lum(r, g, b))
            if pal is GLASS:
                rgb = shade(pal, 0.22 + 0.78 * lum(r, g, b))
            px[x, y] = (*rgb, a if keep_alpha else 255)
    return im


def paint_star_side(base: Image.Image, on: bool) -> Image.Image:
    img = recolor_rgba(base, STAR, keep_alpha=False)
    px = img.load()
    core = PHI if not on else PURIFIED

    def mix(x: int, y: int, t: float, amt: float) -> None:
        r, g, b, _a = px[x, y]
        rgb = shade(core, t)
        px[x, y] = (
            int(r * (1 - amt) + rgb[0] * amt),
            int(g * (1 - amt) + rgb[1] * amt),
            int(b * (1 - amt) + rgb[2] * amt),
            255,
        )

    amt = 0.72 if on else 0.55
    for y in range(16):
        mix(2, y, 0.5 + 0.08 * (y % 3), amt)
        mix(3, y, 0.62 + 0.08 * ((y + 1) % 3), amt)
        mix(12, y, 0.62 + 0.08 * ((y + 1) % 3), amt)
        mix(13, y, 0.5 + 0.08 * (y % 3), amt)
        if 5 <= y <= 10:
            mix(7, y, 0.75 if on else 0.5, amt)
            mix(8, y, 0.85 if on else 0.58, amt)
    if on:
        px[7, 4] = (*shade(core, 1.0), 255)
        px[8, 4] = (*shade(core, 1.0), 255)
    return img


def paint_star_top(beacon: Image.Image) -> Image.Image:
    img = recolor_rgba(beacon, STAR, keep_alpha=False)
    px = img.load()
    for y in range(5, 11):
        for x in range(5, 11):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 2.4:
                px[x, y] = (*shade(PHI, 0.95 - 0.12 * d), 255)
            elif d < 3.6:
                px[x, y] = (*shade(STAR, 0.7), 255)
    px[7, 7] = (255, 255, 255, 255)
    px[8, 7] = (220, 250, 255, 255)
    return img


def main() -> None:
    if not JAR.exists():
        raise SystemExit(f"missing jar: {JAR}")
    OUT.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(JAR) as z:
        still = Image.open(BytesIO(jar_bytes(z, "water_still.png")))
        flow = Image.open(BytesIO(jar_bytes(z, "water_flow.png")))
        overlay = Image.open(BytesIO(jar_bytes(z, "water_overlay.png")))
        glass = Image.open(BytesIO(jar_bytes(z, "glass.png")))
        side = Image.open(BytesIO(jar_bytes(z, "respawn_anchor_side0.png")))
        beacon = Image.open(BytesIO(jar_bytes(z, "beacon.png")))
        still_meta = jar_bytes(z, "water_still.png.mcmeta")
        flow_meta = jar_bytes(z, "water_flow.png.mcmeta")

        pairs = [
            ("phi_water_still", still, PHI, still_meta),
            ("phi_water_flow", flow, PHI, flow_meta),
            ("purified_phi_water_still", still, PURIFIED, still_meta),
            ("purified_phi_water_flow", flow, PURIFIED, flow_meta),
        ]
        for name, src, pal, meta in pairs:
            out = recolor_rgba(src, pal, keep_alpha=False)
            out.save(OUT / f"{name}.png")
            (OUT / f"{name}.png.mcmeta").write_bytes(meta)
            print("fluid", name, out.size, (OUT / f"{name}.png").stat().st_size)

        ov_phi = recolor_rgba(overlay, PHI, keep_alpha=True)
        ov_phi.save(OUT / "phi_water_overlay.png")
        ov_pur = recolor_rgba(overlay, PURIFIED, keep_alpha=True)
        ov_pur.save(OUT / "purified_phi_water_overlay.png")

        g = recolor_rgba(glass, GLASS, keep_alpha=True)
        g.save(OUT / "phi_glass.png")
        print("glass", (OUT / "phi_glass.png").stat().st_size)

        paint_star_side(side, False).save(OUT / "star_reactor_side.png")
        paint_star_side(side, True).save(OUT / "star_reactor_side_on.png")
        paint_star_top(beacon).save(OUT / "star_reactor_top.png")
        print("star faces written")


if __name__ == "__main__":
    main()
