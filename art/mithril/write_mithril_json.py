"""Write mithril blockstates, models, loot, tags, worldgen, and recipes."""
from pathlib import Path

BASE = Path(__file__).resolve().parents[2] / "src" / "main" / "resources"


def w(rel: str, text: str) -> None:
    path = BASE / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text.strip() + "\n", encoding="utf-8")
    print("wrote", path.relative_to(BASE.parent.parent.parent))


def main() -> None:
    for name in ["mithril_ore", "deepslate_mithril_ore", "mithril_block"]:
        w(
            f"assets/effecoria/blockstates/{name}.json",
            f"""{{
  "variants": {{
    "": {{ "model": "effecoria:block/{name}" }}
  }}
}}""",
        )
        w(
            f"assets/effecoria/models/block/{name}.json",
            f"""{{
  "parent": "minecraft:block/cube_all",
  "textures": {{ "all": "effecoria:block/{name}" }}
}}""",
        )
        w(
            f"assets/effecoria/models/item/{name}.json",
            f"""{{
  "parent": "effecoria:block/{name}"
}}""",
        )

    for n in [
        "raw_mithril",
        "mithril_ingot",
        "mithril_nugget",
        "mithril_wire",
        "mithril_helmet",
        "mithril_chestplate",
        "mithril_leggings",
        "mithril_boots",
    ]:
        w(
            f"assets/effecoria/models/item/{n}.json",
            f"""{{
  "parent": "minecraft:item/generated",
  "textures": {{ "layer0": "effecoria:item/{n}" }}
}}""",
        )

    for n in ["mithril_sword", "mithril_pickaxe", "mithril_axe", "mithril_shovel", "mithril_hoe"]:
        w(
            f"assets/effecoria/models/item/{n}.json",
            f"""{{
  "parent": "minecraft:item/handheld",
  "textures": {{ "layer0": "effecoria:item/{n}" }}
}}""",
        )

    ore_loot = """{{
  "type": "minecraft:block",
  "pools": [
    {{
      "bonus_rolls": 0.0,
      "entries": [
        {{
          "type": "minecraft:alternatives",
          "children": [
            {{
              "type": "minecraft:item",
              "conditions": [
                {{
                  "condition": "minecraft:match_tool",
                  "predicate": {{
                    "predicates": {{
                      "minecraft:enchantments": [
                        {{
                          "enchantments": "minecraft:silk_touch",
                          "levels": {{ "min": 1 }}
                        }}
                      ]
                    }}
                  }}
                }}
              ],
              "name": "effecoria:{ore}"
            }},
            {{
              "type": "minecraft:item",
              "functions": [
                {{
                  "enchantment": "minecraft:fortune",
                  "formula": "minecraft:ore_drops",
                  "function": "minecraft:apply_bonus"
                }},
                {{ "function": "minecraft:explosion_decay" }}
              ],
              "name": "effecoria:raw_mithril"
            }}
          ]
        }}
      ],
      "rolls": 1.0
    }}
  ]
}}"""
    for ore in ["mithril_ore", "deepslate_mithril_ore"]:
        w(f"data/effecoria/loot_table/blocks/{ore}.json", ore_loot.format(ore=ore))

    w(
        "data/effecoria/loot_table/blocks/mithril_block.json",
        """{
  "type": "minecraft:block",
  "pools": [
    {
      "bonus_rolls": 0.0,
      "entries": [
        { "type": "minecraft:item", "name": "effecoria:mithril_block" }
      ],
      "rolls": 1.0,
      "conditions": [
        { "condition": "minecraft:survives_explosion" }
      ]
    }
  ],
  "random_sequence": "effecoria:blocks/mithril_block"
}""",
    )

    w(
        "data/effecoria/tags/block/mithril_ores.json",
        """{
  "replace": false,
  "values": [
    "effecoria:mithril_ore",
    "effecoria:deepslate_mithril_ore"
  ]
}""",
    )
    w(
        "data/effecoria/tags/item/phi_conductors.json",
        """{
  "replace": false,
  "values": [
    "effecoria:mithril_ingot",
    "effecoria:mithril_wire",
    "effecoria:mithril_nugget",
    "effecoria:mithril_block"
  ]
}""",
    )

    w(
        "data/effecoria/worldgen/configured_feature/ore_mithril.json",
        """{
  "type": "minecraft:ore",
  "config": {
    "size": 5,
    "discard_chance_on_air_exposure": 0.35,
    "targets": [
      {
        "target": {
          "predicate_type": "minecraft:tag_match",
          "tag": "minecraft:stone_ore_replaceables"
        },
        "state": { "Name": "effecoria:mithril_ore" }
      },
      {
        "target": {
          "predicate_type": "minecraft:tag_match",
          "tag": "minecraft:deepslate_ore_replaceables"
        },
        "state": { "Name": "effecoria:deepslate_mithril_ore" }
      }
    ]
  }
}""",
    )
    w(
        "data/effecoria/worldgen/placed_feature/ore_mithril.json",
        """{
  "feature": "effecoria:ore_mithril",
  "placement": [
    { "type": "minecraft:count", "count": 3 },
    { "type": "minecraft:in_square" },
    {
      "type": "minecraft:height_range",
      "height": {
        "type": "minecraft:trapezoid",
        "min_inclusive": { "absolute": -32 },
        "max_inclusive": { "absolute": 80 }
      }
    },
    { "type": "minecraft:biome" }
  ]
}""",
    )
    w(
        "data/effecoria/neoforge/biome_modifier/plateau_mithril_ore.json",
        """{
  "type": "neoforge:add_features",
  "biomes": "#effecoria:is_essence_plateau",
  "features": ["effecoria:ore_mithril"],
  "step": "underground_ores"
}""",
    )

    recipes = {
        "mithril_block.json": """{
  "type": "minecraft:crafting_shaped",
  "category": "building",
  "key": { "#": { "item": "effecoria:mithril_ingot" } },
  "pattern": ["###", "###", "###"],
  "result": { "count": 1, "id": "effecoria:mithril_block" }
}""",
        "mithril_ingot_from_block.json": """{
  "type": "minecraft:crafting_shapeless",
  "category": "misc",
  "ingredients": [{ "item": "effecoria:mithril_block" }],
  "result": { "count": 9, "id": "effecoria:mithril_ingot" }
}""",
        "mithril_nugget.json": """{
  "type": "minecraft:crafting_shapeless",
  "category": "misc",
  "ingredients": [{ "item": "effecoria:mithril_ingot" }],
  "result": { "count": 9, "id": "effecoria:mithril_nugget" }
}""",
        "mithril_ingot_from_nuggets.json": """{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": { "#": { "item": "effecoria:mithril_nugget" } },
  "pattern": ["###", "###", "###"],
  "result": { "count": 1, "id": "effecoria:mithril_ingot" }
}""",
        "mithril_wire.json": """{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": { "M": { "item": "effecoria:mithril_ingot" } },
  "pattern": ["M", "M"],
  "result": { "count": 4, "id": "effecoria:mithril_wire" }
}""",
        "mithril_helmet.json": """{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "key": { "M": { "item": "effecoria:mithril_ingot" } },
  "pattern": ["MMM", "M M"],
  "result": { "count": 1, "id": "effecoria:mithril_helmet" }
}""",
        "mithril_chestplate.json": """{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "key": { "M": { "item": "effecoria:mithril_ingot" } },
  "pattern": ["M M", "MMM", "MMM"],
  "result": { "count": 1, "id": "effecoria:mithril_chestplate" }
}""",
        "mithril_leggings.json": """{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "key": { "M": { "item": "effecoria:mithril_ingot" } },
  "pattern": ["MMM", "M M", "M M"],
  "result": { "count": 1, "id": "effecoria:mithril_leggings" }
}""",
        "mithril_boots.json": """{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "key": { "M": { "item": "effecoria:mithril_ingot" } },
  "pattern": ["M M", "M M"],
  "result": { "count": 1, "id": "effecoria:mithril_boots" }
}""",
        "mithril_sword.json": """{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "key": {
    "M": { "item": "effecoria:mithril_ingot" },
    "#": { "item": "minecraft:stick" }
  },
  "pattern": ["M", "M", "#"],
  "result": { "count": 1, "id": "effecoria:mithril_sword" }
}""",
        "mithril_pickaxe.json": """{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "key": {
    "M": { "item": "effecoria:mithril_ingot" },
    "#": { "item": "minecraft:stick" }
  },
  "pattern": ["MMM", " # ", " # "],
  "result": { "count": 1, "id": "effecoria:mithril_pickaxe" }
}""",
        "mithril_axe.json": """{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "key": {
    "M": { "item": "effecoria:mithril_ingot" },
    "#": { "item": "minecraft:stick" }
  },
  "pattern": ["MM", "M#", " #"],
  "result": { "count": 1, "id": "effecoria:mithril_axe" }
}""",
        "mithril_shovel.json": """{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "key": {
    "M": { "item": "effecoria:mithril_ingot" },
    "#": { "item": "minecraft:stick" }
  },
  "pattern": ["M", "#", "#"],
  "result": { "count": 1, "id": "effecoria:mithril_shovel" }
}""",
        "mithril_hoe.json": """{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "key": {
    "M": { "item": "effecoria:mithril_ingot" },
    "#": { "item": "minecraft:stick" }
  },
  "pattern": ["MM", " #", " #"],
  "result": { "count": 1, "id": "effecoria:mithril_hoe" }
}""",
    }
    for name, body in recipes.items():
        w(f"data/effecoria/recipe/{name}", body)


if __name__ == "__main__":
    main()
