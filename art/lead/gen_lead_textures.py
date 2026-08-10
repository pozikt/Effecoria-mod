"""Recolor vanilla iron textures into dull blue-grey lead for Effecoria."""
from __future__ import annotations

from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
REF = Path(__file__).resolve().parent / "vanilla_refs"
BLOCK = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "block"
ITEM = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "item"
BLOCK.mkdir(parents=True, exist_ok=True)
ITEM.mkdir(parents=True, exist_ok=True)


def to_lead(im: Image.Image, ore_vein: bool = False) -> Image.Image:
    """Keep stone/deepslate host; remap metallic / warm iron pixels to lead blue-grey."""
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            lum = (r + g + b) / 3.0
            # Host rock: leave mostly alone if cool/neutral and mid-dark (stone/deepslate)
            sat = max(r, g, b) - min(r, g, b)
            # Vanilla iron ore veins are warm beige; keep cool stone/deepslate host.
            is_warm_vein = r > g + 5 and r > b + 15 and lum > 85 and sat > 20
            is_bright_metal = lum > 150 and sat < 45 and r > 120
            is_ingot_metal = not ore_vein
            if ore_vein and not (is_warm_vein or is_bright_metal):
                continue
            if is_warm_vein or is_bright_metal or is_ingot_metal:
                t = max(0.0, min(1.0, (lum - 40) / 180.0))
                nr = int(52 + 68 * t)
                ng = int(56 + 70 * t)
                nb = int(64 + 82 * t)
                px[x, y] = (nr, ng, nb, a)
    return im


def save(path: Path, im: Image.Image):
    im.save(path)
    print("wrote", path.relative_to(ROOT), im.size)


def main():
    mapping = [
        ("iron_ore.png", BLOCK / "lead_ore.png", True),
        ("deepslate_iron_ore.png", BLOCK / "deepslate_lead_ore.png", True),
        ("iron_block.png", BLOCK / "lead_block.png", False),
        ("iron_ingot.png", ITEM / "lead_ingot.png", False),
        ("iron_nugget.png", ITEM / "lead_nugget.png", False),
        ("raw_iron.png", ITEM / "raw_lead.png", False),
    ]
    for src_name, dest, ore in mapping:
        src = REF / src_name
        if not src.exists():
            raise SystemExit(f"missing {src}")
        save(dest, to_lead(Image.open(src), ore_vein=ore))


if __name__ == "__main__":
    main()
