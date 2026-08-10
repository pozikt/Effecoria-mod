"""Recolor vanilla iron (and essonite armor layers) into pale silver-cyan mithril."""
from __future__ import annotations

from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
REF = Path(__file__).resolve().parent / "vanilla_refs"
BLOCK = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "block"
ITEM = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "item"
ARMOR = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "models" / "armor"
BLOCK.mkdir(parents=True, exist_ok=True)
ITEM.mkdir(parents=True, exist_ok=True)
ARMOR.mkdir(parents=True, exist_ok=True)


def to_mithril(im: Image.Image, ore_vein: bool = False) -> Image.Image:
    """Keep stone/deepslate host; remap metallic pixels to silver with cyan Φ tint."""
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            lum = (r + g + b) / 3.0
            sat = max(r, g, b) - min(r, g, b)
            is_warm_vein = r > g + 5 and r > b + 15 and lum > 85 and sat > 20
            is_bright_metal = lum > 140 and sat < 55
            is_ingot_metal = not ore_vein
            if ore_vein and not (is_warm_vein or is_bright_metal):
                continue
            if is_warm_vein or is_bright_metal or is_ingot_metal:
                t = max(0.0, min(1.0, (lum - 30) / 200.0))
                # Pale silver with cool cyan bias
                nr = int(150 + 90 * t)
                ng = int(165 + 85 * t)
                nb = int(185 + 70 * t)
                # Soft cyan highlight on brighter pixels
                if t > 0.65:
                    ng = min(255, ng + 8)
                    nb = min(255, nb + 18)
                px[x, y] = (nr, ng, nb, a)
    return im


def to_mithril_armor_layer(im: Image.Image) -> Image.Image:
    """Remap existing 64x64 armor atlas toward silver-cyan (keep alpha)."""
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a < 8:
                continue
            lum = (r + g + b) / 3.0
            t = max(0.0, min(1.0, (lum - 20) / 200.0))
            nr = int(140 + 95 * t)
            ng = int(158 + 88 * t)
            nb = int(178 + 72 * t)
            if t > 0.55:
                ng = min(255, ng + 10)
                nb = min(255, nb + 22)
            px[x, y] = (nr, ng, nb, a)
    return im


def save(path: Path, im: Image.Image) -> None:
    im.save(path)
    print("wrote", path.relative_to(ROOT), im.size)


def main() -> None:
    mapping = [
        ("iron_ore.png", BLOCK / "mithril_ore.png", True),
        ("deepslate_iron_ore.png", BLOCK / "deepslate_mithril_ore.png", True),
        ("iron_block.png", BLOCK / "mithril_block.png", False),
        ("iron_ingot.png", ITEM / "mithril_ingot.png", False),
        ("iron_nugget.png", ITEM / "mithril_nugget.png", False),
        ("raw_iron.png", ITEM / "raw_mithril.png", False),
        ("iron_nugget.png", ITEM / "mithril_wire.png", False),
        ("iron_sword.png", ITEM / "mithril_sword.png", False),
        ("iron_pickaxe.png", ITEM / "mithril_pickaxe.png", False),
        ("iron_axe.png", ITEM / "mithril_axe.png", False),
        ("iron_shovel.png", ITEM / "mithril_shovel.png", False),
        ("iron_hoe.png", ITEM / "mithril_hoe.png", False),
        ("iron_helmet.png", ITEM / "mithril_helmet.png", False),
        ("iron_chestplate.png", ITEM / "mithril_chestplate.png", False),
        ("iron_leggings.png", ITEM / "mithril_leggings.png", False),
        ("iron_boots.png", ITEM / "mithril_boots.png", False),
    ]
    for src_name, dest, ore in mapping:
        src = REF / src_name
        if not src.exists():
            raise SystemExit(f"missing {src}")
        save(dest, to_mithril(Image.open(src), ore_vein=ore))

    # 1.21.1 still uses 64x64 models/armor layers — recolor essonite atlas UV.
    base1 = ROOT / "src/main/resources/assets/effecoria/textures/models/armor/crystal_essonite_layer_1.png"
    base2 = ROOT / "src/main/resources/assets/effecoria/textures/models/armor/crystal_essonite_layer_2.png"
    save(ARMOR / "mithril_layer_1.png", to_mithril_armor_layer(Image.open(base1)))
    save(ARMOR / "mithril_layer_2.png", to_mithril_armor_layer(Image.open(base2)))


if __name__ == "__main__":
    main()
