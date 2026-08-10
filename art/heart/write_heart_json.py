"""Write Heart Reactor / Φ-bus models, loot, recipes, tags."""
from pathlib import Path

BASE = Path(__file__).resolve().parents[2] / "src" / "main" / "resources"


def w(rel: str, text: str) -> None:
    path = BASE / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text.strip() + "\n", encoding="utf-8")
    print(path.relative_to(BASE))


def main() -> None:
    for name in ["heart_reactor_core", "reactor_casing", "phi_bus"]:
        if name == "phi_bus":
            w(
                f"assets/effecoria/blockstates/{name}.json",
                """{
  "variants": {
    "powered=false": { "model": "effecoria:block/phi_bus" },
    "powered=true": { "model": "effecoria:block/phi_bus_on" }
  }
}""",
            )
            w(
                "assets/effecoria/models/block/phi_bus.json",
                """{
  "parent": "minecraft:block/cube_all",
  "textures": { "all": "effecoria:block/phi_bus" }
}""",
            )
            w(
                "assets/effecoria/models/block/phi_bus_on.json",
                """{
  "parent": "minecraft:block/cube_all",
  "textures": { "all": "effecoria:block/phi_bus" }
}""",
            )
        elif name == "heart_reactor_core":
            w(
                f"assets/effecoria/blockstates/{name}.json",
                """{
  "variants": {
    "facing=north,lit=false": { "model": "effecoria:block/heart_reactor_core" },
    "facing=south,lit=false": { "model": "effecoria:block/heart_reactor_core", "y": 180 },
    "facing=west,lit=false": { "model": "effecoria:block/heart_reactor_core", "y": 270 },
    "facing=east,lit=false": { "model": "effecoria:block/heart_reactor_core", "y": 90 },
    "facing=north,lit=true": { "model": "effecoria:block/heart_reactor_core_on" },
    "facing=south,lit=true": { "model": "effecoria:block/heart_reactor_core_on", "y": 180 },
    "facing=west,lit=true": { "model": "effecoria:block/heart_reactor_core_on", "y": 270 },
    "facing=east,lit=true": { "model": "effecoria:block/heart_reactor_core_on", "y": 90 }
  }
}""",
            )
            w(
                "assets/effecoria/models/block/heart_reactor_core.json",
                """{
  "parent": "minecraft:block/cube_all",
  "textures": { "all": "effecoria:block/heart_reactor_core" }
}""",
            )
            w(
                "assets/effecoria/models/block/heart_reactor_core_on.json",
                """{
  "parent": "minecraft:block/cube_all",
  "textures": { "all": "effecoria:block/heart_reactor_core" }
}""",
            )
        else:
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
  "parent": "effecoria:block/{name if name != 'heart_reactor_core' else 'heart_reactor_core'}"
}}""",
        )

    for name in ["heart_reactor_core", "reactor_casing", "phi_bus"]:
        w(
            f"data/effecoria/loot_table/blocks/{name}.json",
            f"""{{
  "type": "minecraft:block",
  "pools": [
    {{
      "bonus_rolls": 0.0,
      "entries": [{{ "type": "minecraft:item", "name": "effecoria:{name}" }}],
      "rolls": 1.0,
      "conditions": [{{ "condition": "minecraft:survives_explosion" }}]
    }}
  ]
}}""",
        )

    w(
        "data/effecoria/recipe/reactor_casing.json",
        """{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {
    "L": { "item": "effecoria:lead_ingot" },
    "P": { "item": "effecoria:phi_stone" },
    "D": { "item": "effecoria:essonite_dust" }
  },
  "pattern": ["LDL", "DPD", "LDL"],
  "result": { "count": 4, "id": "effecoria:reactor_casing" }
}""",
    )
    w(
        "data/effecoria/recipe/heart_reactor_core.json",
        """{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {
    "S": { "item": "effecoria:spark_reactor" },
    "C": { "item": "effecoria:reactor_casing" },
    "P": { "item": "effecoria:pure_essonite" },
    "M": { "item": "effecoria:mithril_wire" }
  },
  "pattern": ["CMC", "PSP", "CMC"],
  "result": { "count": 1, "id": "effecoria:heart_reactor_core" }
}""",
    )
    w(
        "data/effecoria/recipe/phi_bus.json",
        """{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {
    "M": { "item": "effecoria:mithril_wire" },
    "G": { "item": "effecoria:phi_glass" },
    "L": { "item": "effecoria:lead_nugget" }
  },
  "pattern": [" L ", "MGM", " L "],
  "result": { "count": 6, "id": "effecoria:phi_bus" }
}""",
    )

    w(
        "data/effecoria/technomagic/heart_reactor.json",
        """{
  "id": "effecoria:heart_reactor",
  "era": 4,
  "icon": "effecoria:heart_reactor_core",
  "status": "available",
  "requires": ["effecoria:spark_reactor"],
  "display_unlocks": ["effecoria:phi_bus"]
}""",
    )
    w(
        "data/effecoria/technomagic/phi_bus.json",
        """{
  "id": "effecoria:phi_bus",
  "era": 4,
  "icon": "effecoria:phi_bus",
  "status": "available",
  "requires": ["effecoria:spark_reactor", "effecoria:heart_reactor"],
  "display_unlocks": []
}""",
    )


if __name__ == "__main__":
    main()
