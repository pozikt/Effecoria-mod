"""Bake textures + JSON assets for byproduct sinks."""
from __future__ import annotations

import json
import zipfile
from pathlib import Path

from PIL import Image, ImageEnhance, ImageOps

ROOT = Path(__file__).resolve().parents[1]
JAR = Path.home() / ".gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar"
ASSETS = ROOT / "src/main/resources/assets/effecoria"
DATA = ROOT / "src/main/resources/data/effecoria"
TEX_BLOCK = ASSETS / "textures/block"
TEX_ITEM = ASSETS / "textures/item"
TEX_ARMOR = ASSETS / "textures/models/armor"
MODELS_BLOCK = ASSETS / "models/block"
MODELS_ITEM = ASSETS / "models/item"
BLOCKSTATES = ASSETS / "blockstates"
RECIPE = DATA / "recipe"
LOOT = DATA / "loot_table/blocks"


def extract(path: str) -> Image.Image:
    with zipfile.ZipFile(JAR) as zf:
        with zf.open(path) as f:
            return Image.open(f).convert("RGBA")


def recolor(img: Image.Image, tint=(90, 160, 190), sat=0.85, bright=1.05) -> Image.Image:
    r, g, b, a = img.split()
    rgb = Image.merge("RGB", (r, g, b))
    # grayscale luminosity then tint
    gray = ImageOps.grayscale(rgb)
    gray = ImageEnhance.Brightness(gray).enhance(bright)
    gray = ImageEnhance.Contrast(gray).enhance(1.1)
    out = Image.new("RGBA", img.size)
    gp = gray.load()
    op = out.load()
    tr, tg, tb = tint
    for y in range(img.size[1]):
        for x in range(img.size[0]):
            aa = a.getpixel((x, y))
            if aa == 0:
                op[x, y] = (0, 0, 0, 0)
                continue
            v = gp[x, y] / 255.0
            # mix gray with tint
            nr = int(min(255, (v * (1 - sat) + (tr / 255.0) * v * sat) * 255))
            ng = int(min(255, (v * (1 - sat) + (tg / 255.0) * v * sat) * 255))
            nb = int(min(255, (v * (1 - sat) + (tb / 255.0) * v * sat) * 255))
            # better: scale tint by luminosity
            nr = int(min(255, tr * v * 1.15))
            ng = int(min(255, tg * v * 1.15))
            nb = int(min(255, tb * v * 1.15))
            op[x, y] = (nr, ng, nb, aa)
    return out


def write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def cube_all(name: str, tex: str) -> None:
    write_json(BLOCKSTATES / f"{name}.json", {"variants": {"": {"model": f"effecoria:block/{name}"}}})
    write_json(
        MODELS_BLOCK / f"{name}.json",
        {"parent": "minecraft:block/cube_all", "textures": {"all": tex}},
    )
    write_json(MODELS_ITEM / f"{name}.json", {"parent": f"effecoria:block/{name}"})


def item_generated(name: str) -> None:
    write_json(
        MODELS_ITEM / f"{name}.json",
        {"parent": "minecraft:item/generated", "textures": {"layer0": f"effecoria:item/{name}"}},
    )


def handheld(name: str) -> None:
    write_json(
        MODELS_ITEM / f"{name}.json",
        {"parent": "minecraft:item/handheld", "textures": {"layer0": f"effecoria:item/{name}"}},
    )


def loot_simple(name: str) -> None:
    write_json(
        LOOT / f"{name}.json",
        {
            "type": "minecraft:block",
            "pools": [
                {
                    "rolls": 1,
                    "entries": [{"type": "minecraft:item", "name": f"effecoria:{name}"}],
                    "conditions": [{"condition": "minecraft:survives_explosion"}],
                }
            ],
        },
    )


def shaped(path: str, pattern: list[str], key: dict, result: str, count: int = 1) -> None:
    write_json(
        RECIPE / f"{path}.json",
        {
            "type": "minecraft:crafting_shaped",
            "pattern": pattern,
            "key": key,
            "result": {"id": result, "count": count},
        },
    )


def shapeless(path: str, ingredients: list, result: str, count: int = 1) -> None:
    write_json(
        RECIPE / f"{path}.json",
        {
            "type": "minecraft:crafting_shapeless",
            "ingredients": ingredients,
            "result": {"id": result, "count": count},
        },
    )


def main() -> None:
    for d in (TEX_BLOCK, TEX_ITEM, TEX_ARMOR, MODELS_BLOCK, MODELS_ITEM, BLOCKSTATES, RECIPE, LOOT):
        d.mkdir(parents=True, exist_ok=True)

    # --- textures ---
    concrete = recolor(extract("assets/minecraft/textures/block/light_gray_concrete.png"), (70, 140, 170))
    concrete.save(TEX_BLOCK / "phi_concrete.png")

    anchor = recolor(extract("assets/minecraft/textures/block/obsidian.png"), (40, 20, 70), sat=0.9, bright=0.9)
    # add pale core
    px = anchor.load()
    for y in range(5, 11):
        for x in range(5, 11):
            r, g, b, a = px[x, y]
            px[x, y] = (min(255, r + 40), min(255, g + 20), min(255, b + 60), a)
    anchor.save(TEX_BLOCK / "omega_anchor.png")

    white = extract("assets/minecraft/textures/item/paper.png")
    recolor(white, (100, 180, 200)).save(TEX_ITEM / "phi_cloth.png")
    recolor(extract("assets/minecraft/textures/item/string.png"), (80, 160, 180)).save(TEX_ITEM / "phi_rope.png")
    recolor(extract("assets/minecraft/textures/item/iron_nugget.png"), (90, 50, 130)).save(
        TEX_ITEM / "omega_filter.png"
    )

    # armor layers
    for layer, name in (("layer_1", "phi_steel_layer_1.png"), ("layer_2", "phi_steel_layer_2.png")):
        src = extract(f"assets/minecraft/textures/models/armor/iron_{layer}.png")
        recolor(src, (95, 155, 185), bright=1.1).save(TEX_ARMOR / name)

    # gear items
    gear = {
        "phi_steel_helmet": "iron_helmet",
        "phi_steel_chestplate": "iron_chestplate",
        "phi_steel_leggings": "iron_leggings",
        "phi_steel_boots": "iron_boots",
        "phi_steel_sword": "iron_sword",
        "phi_steel_pickaxe": "iron_pickaxe",
        "phi_steel_axe": "iron_axe",
        "phi_steel_shovel": "iron_shovel",
        "phi_steel_hoe": "iron_hoe",
    }
    for out_name, van in gear.items():
        img = recolor(extract(f"assets/minecraft/textures/item/{van}.png"), (95, 155, 185), bright=1.08)
        img.save(TEX_ITEM / f"{out_name}.png")

    print("textures ok")

    # --- models / blockstates ---
    cube_all("phi_concrete", "effecoria:block/phi_concrete")
    cube_all("omega_anchor", "effecoria:block/omega_anchor")
    loot_simple("phi_concrete")
    loot_simple("omega_anchor")

    for n in ("phi_cloth", "phi_rope", "omega_filter"):
        item_generated(n)
    for n in gear:
        if "sword" in n or "pickaxe" in n or "axe" in n or "shovel" in n or "hoe" in n:
            handheld(n)
        else:
            item_generated(n)

    # --- recipes ---
    shaped(
        "phi_concrete",
        ["GG", "GC"],
        {"G": {"item": "effecoria:phi_stone_grit"}, "C": {"item": "minecraft:clay_ball"}},
        "effecoria:phi_concrete",
        4,
    )
    shapeless(
        "bone_meal_from_bone_grit",
        [{"item": "effecoria:bone_grit"}, {"item": "effecoria:bone_grit"}],
        "minecraft:bone_meal",
        1,
    )
    shaped(
        "phi_cloth",
        ["FF", "FF"],
        {"F": {"item": "effecoria:phi_fiber"}},
        "effecoria:phi_cloth",
        1,
    )
    shaped(
        "phi_rope",
        ["F", "F", "F"],
        {"F": {"item": "effecoria:phi_fiber"}},
        "effecoria:phi_rope",
        1,
    )
    shaped(
        "lead_from_phi_rope",
        ["RR ", "RS ", "  S"],
        {"R": {"item": "effecoria:phi_rope"}, "S": {"item": "minecraft:slime_ball"}},
        "minecraft:lead",
        2,
    )
    shaped(
        "lead_cloak_from_cloth",
        ["LLL", "LCL", "L L"],
        {"L": {"item": "effecoria:lead_ingot"}, "C": {"item": "effecoria:phi_cloth"}},
        "effecoria:lead_cloak",
        1,
    )
    shapeless(
        "omega_filter",
        [{"item": "effecoria:obsidian_grit"}, {"item": "effecoria:lead_foil"}],
        "effecoria:omega_filter",
        1,
    )
    shaped(
        "omega_dust_from_nuggets",
        ["NNN", "NNN", "NNN"],
        {"N": {"item": "effecoria:omega_nugget"}},
        "effecoria:omega_dust",
        1,
    )
    shaped(
        "omega_anchor",
        [" P ", "PSP", " P "],
        {"P": {"item": "effecoria:purified_obsidian"}, "S": {"item": "effecoria:pure_essonite"}},
        "effecoria:omega_anchor",
        1,
    )

    # phi steel gear
    for piece, pattern in {
        "phi_steel_helmet": ["III", "I I"],
        "phi_steel_chestplate": ["I I", "III", "III"],
        "phi_steel_leggings": ["III", "I I", "I I"],
        "phi_steel_boots": ["I I", "I I"],
    }.items():
        shaped(piece, pattern, {"I": {"item": "effecoria:phi_steel_ingot"}}, f"effecoria:{piece}")

    for tool, pattern in {
        "phi_steel_sword": ["I", "I", "#"],
        "phi_steel_pickaxe": ["III", " # ", " # "],
        "phi_steel_axe": ["II", "I#", " #"],
        "phi_steel_shovel": ["I", "#", "#"],
        "phi_steel_hoe": ["II", " #", " #"],
    }.items():
        shaped(
            tool,
            pattern,
            {"I": {"item": "effecoria:phi_steel_ingot"}, "#": {"item": "minecraft:stick"}},
            f"effecoria:{tool}",
        )

    print("recipes/models ok")


if __name__ == "__main__":
    main()
