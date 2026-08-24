"""Party F: essonite/omega crystals, dripstone tips, split machine bottoms, ores."""
from __future__ import annotations

import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
JAR = ROOT / "build/moddev/artifacts/neoforge-21.1.242-client-extra-aka-minecraft-resources.jar"
OUT_B = ROOT / "src/main/resources/assets/effecoria/textures/block"
OUT_I = ROOT / "src/main/resources/assets/effecoria/textures/item"

ESS = ((12, 22, 70), (28, 70, 160), (70, 150, 220), (200, 230, 255))
GOLD = (210, 175, 55)
OMEGA = ((48, 8, 40), (140, 20, 80), (210, 50, 90), (255, 180, 210))
LEAD = ((28, 32, 38), (70, 78, 88), (120, 128, 138), (190, 196, 204))
MITHRIL = ((40, 70, 90), (90, 140, 165), (160, 210, 230), (230, 250, 255))
BASALT = ((22, 22, 24), (52, 54, 58), (92, 94, 98), (150, 152, 156))
TUFF = ((55, 52, 48), (95, 92, 82), (140, 136, 118), (190, 185, 160))


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


def jar_img(z: zipfile.ZipFile, folder: str, name: str) -> Image.Image:
    return Image.open(BytesIO(z.read(f"assets/minecraft/textures/{folder}/{name}")))


def recolor(im: Image.Image, pal, *, gold: bool = False, keep_alpha: bool = True) -> Image.Image:
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a < 8:
                px[x, y] = (0, 0, 0, 0)
                continue
            t = lum(r, g, b)
            rgb = shade(pal, t)
            if gold and t > 0.42 and ((x * 7 + y * 13) % 23 == 0):
                rgb = GOLD
            elif gold and t > 0.58 and ((x * 5 + y * 11) % 29 == 0):
                rgb = (170, 155, 80)
            px[x, y] = (*rgb, a if keep_alpha else 255)
    return im


def save_b(name: str, img: Image.Image) -> None:
    OUT_B.mkdir(parents=True, exist_ok=True)
    img.convert("RGBA").save(OUT_B / f"{name}.png")
    print("block", name, (OUT_B / f"{name}.png").stat().st_size)


def save_i(name: str, img: Image.Image) -> None:
    OUT_I.mkdir(parents=True, exist_ok=True)
    img.convert("RGBA").save(OUT_I / f"{name}.png")
    print("item", name, (OUT_I / f"{name}.png").stat().st_size)


def overlay_ore(stone: Image.Image, ore: Image.Image, stone_pal, gem_pal) -> Image.Image:
    """Keep stone luminance; map the ore's bright/saturated specks to gems."""
    stone = recolor(stone, stone_pal, keep_alpha=False)
    ore = ore.convert("RGBA")
    out = stone.convert("RGBA")
    sp, op, gp = stone.load(), out.load(), ore.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = gp[x, y]
            sat = max(r, g, b) - min(r, g, b)
            t = lum(r, g, b)
            if sat > 28 or t > 0.55:
                op[x, y] = (*shade(gem_pal, t), 255)
            else:
                op[x, y] = sp[x, y]
    return out


def main() -> None:
    if not JAR.exists():
        raise SystemExit(f"missing jar: {JAR}")
    with zipfile.ZipFile(JAR) as z:
        crystals = [
            ("amethyst_cluster.png", "essonite_crystal.png", ESS),
            ("small_amethyst_bud.png", "essonite_crystal_bud_small.png", ESS),
            ("medium_amethyst_bud.png", "essonite_crystal_bud_medium.png", ESS),
            ("large_amethyst_bud.png", "essonite_crystal_bud_large.png", ESS),
            ("amethyst_cluster.png", "omega_crystal.png", OMEGA),
            ("small_amethyst_bud.png", "omega_crystal_bud_small.png", OMEGA),
            ("medium_amethyst_bud.png", "omega_crystal_bud_medium.png", OMEGA),
            ("large_amethyst_bud.png", "omega_crystal_bud_large.png", OMEGA),
        ]
        for src, dst, pal in crystals:
            save_b(dst.replace(".png", ""), recolor(jar_img(z, "block", src), pal))

        drip = [
            "pointed_dripstone_up_tip.png",
            "pointed_dripstone_up_tip_merge.png",
            "pointed_dripstone_up_frustum.png",
            "pointed_dripstone_up_middle.png",
            "pointed_dripstone_up_base.png",
            "pointed_dripstone_down_tip.png",
            "pointed_dripstone_down_tip_merge.png",
            "pointed_dripstone_down_frustum.png",
            "pointed_dripstone_down_middle.png",
            "pointed_dripstone_down_base.png",
        ]
        for name in drip:
            dst = name.replace("pointed_dripstone", "essonite_pointed").replace(".png", "")
            save_b(dst, recolor(jar_img(z, "block", name), ESS, gold=True))
        save_i("essonite_pointed", recolor(jar_img(z, "item", "pointed_dripstone.png"), ESS, gold=True))

        bottoms = {
            "artifact_assembler_bottom": ("smithing_table_bottom.png", (70, 78, 90)),
            "clay_crucible_bottom": ("packed_mud.png", (140, 100, 70)),
            "facet_cutter_bottom": ("stonecutter_bottom.png", (90, 95, 105)),
            "mortar_bottom": ("furnace_top.png", (100, 95, 85)),
            "seal_inscriber_bottom": ("chiseled_deepslate.png", (40, 50, 70)),
            "shaft_lathe_bottom": ("stripped_spruce_log.png", (95, 70, 40)),
        }
        pal_from_tint = lambda tint: (
            tuple(max(8, c - 50) for c in tint),
            tint,
            tuple(min(255, c + 40) for c in tint),
            tuple(min(255, c + 90) for c in tint),
        )
        for dst, (src, tint) in bottoms.items():
            save_b(dst, recolor(jar_img(z, "block", src), pal_from_tint(tint), keep_alpha=False))

        save_b("phi_crusher_hopper_top", recolor(jar_img(z, "block", "hopper_top.png"), LEAD, keep_alpha=False))
        save_b("lead_block", recolor(jar_img(z, "block", "iron_block.png"), LEAD, keep_alpha=False))
        save_b("mithril_block", recolor(jar_img(z, "block", "iron_block.png"), MITHRIL, keep_alpha=False))

        save_b(
            "basalt_essonite_ore",
            overlay_ore(jar_img(z, "block", "basalt_side.png"), jar_img(z, "block", "nether_quartz_ore.png"), BASALT, ESS),
        )
        save_b(
            "tuff_essonite_ore",
            overlay_ore(jar_img(z, "block", "tuff.png"), jar_img(z, "block", "emerald_ore.png"), TUFF, ESS),
        )


if __name__ == "__main__":
    main()
