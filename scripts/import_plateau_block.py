"""Copy art/essence_plateau/<name>/<name>_32.png into textures/block/<name>.png and wire model if needed."""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "art/essence_plateau"
OUT = ROOT / "src/main/resources/assets/effecoria/textures/block"
MODELS = ROOT / "src/main/resources/assets/effecoria/models/block"

WIRE = {
    "phi_stone": ("phi_stone.json", "effecoria:block/phi_stone"),
    "essonite_crystal": ("essonite_crystal.json", "effecoria:block/essonite_crystal"),
    "essonite_ore": ("essonite_ore.json", "effecoria:block/essonite_ore"),
}


def main() -> None:
    if len(sys.argv) < 2:
        print("Usage: python scripts/import_plateau_block.py <phi_stone|essonite_crystal|essonite_ore>")
        raise SystemExit(1)
    name = sys.argv[1]
    src = ART / name / f"{name}_32.png"
    if not src.is_file():
        raise SystemExit(f"Missing {src}")
    OUT.mkdir(parents=True, exist_ok=True)
    im = Image.open(src).convert("RGBA")
    if im.size != (32, 32):
        im = im.resize((32, 32), Image.Resampling.NEAREST)
    dest = OUT / f"{name}.png"
    im.save(dest)
    print("Wrote", dest)
    if name in WIRE:
        model_name, tex = WIRE[name]
        model_path = MODELS / model_name
        text = model_path.read_text(encoding="utf-8")
        if "minecraft:block/" in text and name == "phi_stone":
            model_path.write_text(
                """{
  "parent": "minecraft:block/cube_all",
  "textures": {
    "all": "%s"
  }
}
"""
                % tex,
                encoding="utf-8",
            )
            print("Updated model", model_path)
        elif name == "essonite_crystal":
            model_path.write_text(
                """{
  "parent": "minecraft:block/cube_all",
  "textures": {
    "all": "%s"
  }
}
"""
                % tex,
                encoding="utf-8",
            )
            print("Updated model", model_path)


if __name__ == "__main__":
    main()
