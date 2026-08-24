"""Vanilla-silhouette pass for catalyst / glue / key placeholders."""
from __future__ import annotations

import zipfile
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
JAR = ROOT / "build/moddev/artifacts/neoforge-21.1.242-client-extra-aka-minecraft-resources.jar"
VAN = ROOT / "art/items/vanilla_refs"
OUT = ROOT / "src/main/resources/assets/effecoria/textures/item"
FACE = ROOT / "art/items/for_artist"

EXTRACT = {
    "assets/minecraft/textures/block/torchflower.png": "torchflower.png",
    "assets/minecraft/textures/item/honey_bottle.png": "honey_bottle.png",
    "assets/minecraft/textures/item/glowstone_dust.png": "glowstone_dust.png",
    "assets/minecraft/textures/item/slime_ball.png": "slime_ball.png",
    "assets/minecraft/textures/item/trial_key.png": "trial_key.png",
    "assets/minecraft/textures/item/potion.png": "potion.png",
    "assets/minecraft/textures/item/potion_overlay.png": "potion_overlay.png",
}


def lum(r: int, g: int, b: int) -> float:
    return (0.3 * r + 0.59 * g + 0.11 * b) / 255.0


def lerp(a, b, t: float):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def sat(r: int, g: int, b: int) -> int:
    return max(r, g, b) - min(r, g, b)


def extract() -> None:
    VAN.mkdir(parents=True, exist_ok=True)
    if not JAR.exists():
        return
    with zipfile.ZipFile(JAR) as z:
        for src, dest in EXTRACT.items():
            if src in z.namelist():
                (VAN / dest).write_bytes(z.read(src))


def save_item(name: str, img: Image.Image) -> None:
    img = img.convert("RGBA")
    OUT.mkdir(parents=True, exist_ok=True)
    FACE.mkdir(parents=True, exist_ok=True)
    img.save(OUT / f"{name}.png")
    img.save(FACE / f"{name}_16x.png")
    prev = Image.new("RGBA", (128, 128), (18, 20, 28, 255))
    big = img.resize((128, 128), Image.Resampling.NEAREST)
    prev.paste(big, (0, 0), big)
    prev.save(FACE / f"{name}_16x_8x.png")
    print("wrote", name, (OUT / f"{name}.png").stat().st_size)


def map_pixels(src: Path, mapper) -> Image.Image:
    im = Image.open(src).convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a < 16:
                px[x, y] = (0, 0, 0, 0)
                continue
            nr, ng, nb = mapper(r, g, b)
            px[x, y] = (nr, ng, nb, 255)
    return im


def fireflower(r, g, b):
    t = lum(r, g, b)
    if g > r + 6 and g >= b:
        return lerp((12, 48, 36), (48, 140, 92), t)
    if r > 90 and r >= g:
        # hotter bloom + a little Φ magenta in midtones
        if 0.35 < t < 0.7:
            return lerp((180, 40, 90), (255, 170, 40), (t - 0.35) / 0.35)
        return lerp((90, 18, 20), (255, 240, 160), t)
    return lerp((20, 16, 28), (90, 70, 110), t)


def phi_nectar(r, g, b):
    t = lum(r, g, b)
    if sat(r, g, b) < 38 and max(r, g, b) > 70:
        # glass / cork — keep, slight cyan glint
        return (min(255, r), min(255, g + 8), min(255, b + 18))
    if t > 0.62:
        return lerp((180, 200, 80), (255, 240, 160), (t - 0.62) / 0.38)
    return lerp((12, 90, 110), (70, 210, 230), t / 0.62)


def omega_dust(r, g, b):
    t = lum(r, g, b)
    return lerp((18, 0, 36), (190, 90, 255), t)


def essence_glue(r, g, b):
    t = lum(r, g, b)
    return lerp((70, 18, 72), (255, 170, 230), t)


def psi_key(r, g, b):
    t = lum(r, g, b)
    # copper / rust shaft → steel; warm highlights → cyan bit
    if r > g + 20 and r > b + 10:
        return lerp((20, 70, 90), (90, 230, 240), t)
    return lerp((28, 32, 40), (190, 200, 210), t)


def bake_lonver() -> None:
    overlay = np.array(Image.open(VAN / "potion_overlay.png").convert("RGBA"))
    bottle = np.array(Image.open(VAN / "potion.png").convert("RGBA"))
    fill = np.zeros((16, 16, 3), np.float32)
    fill[:, :] = (132, 18, 48)
    out = np.zeros((16, 16, 4), np.uint8)
    ol = (0.3 * overlay[:, :, 0] + 0.59 * overlay[:, :, 1] + 0.11 * overlay[:, :, 2]) / 255.0
    for c in range(3):
        out[:, :, c] = np.clip(fill[:, :, c] * (0.45 + 0.7 * ol), 0, 255).astype(np.uint8)
    out[:, :, 3] = overlay[:, :, 3]
    ba = bottle[:, :, 3] > 32
    out[ba, :3] = bottle[ba, :3]
    out[ba, 3] = 255
    save_item("lonver_blood_vial", Image.fromarray(out, "RGBA"))


def main() -> None:
    extract()
    save_item("fireflower", map_pixels(VAN / "torchflower.png", fireflower))
    save_item("phi_nectar", map_pixels(VAN / "honey_bottle.png", phi_nectar))
    save_item("omega_dust", map_pixels(VAN / "glowstone_dust.png", omega_dust))
    save_item("essence_glue", map_pixels(VAN / "slime_ball.png", essence_glue))
    save_item("psi_key", map_pixels(VAN / "trial_key.png", psi_key))
    bake_lonver()


if __name__ == "__main__":
    main()
