"""Regenerate only seal-school GUI icons."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

import generate_spell_assets as g  # noqa: E402
import spell_icon_art as s  # noqa: E402

OUT = ROOT / "src/main/resources/assets/effecoria/textures/gui/sprites/spells"


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for spell, school in g.SPELL_SCHOOL.items():
        if school != "seals":
            continue
        path = OUT / f"{spell}.png"
        s.make_icon(spell, school).save(path)
        print(f"{spell:20} {path.stat().st_size:5} B")


if __name__ == "__main__":
    main()
