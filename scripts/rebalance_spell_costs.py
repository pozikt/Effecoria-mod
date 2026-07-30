"""Rebalance spell base_cost for early-game tiers 0–3 (playable without wiki).

Tier is derived from school progression index in SpellProgression.java.
Spells outside progression keep mastery-based fallbacks.
"""
from __future__ import annotations

import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
SPELLS = ROOT / "src/main/resources/data/effecoria/spells"
PROGRESSION_JAVA = ROOT / "src/main/java/com/effecoria/core/psi/SpellProgression.java"

# Always-cheap utility / tutorial spells
CHEAP = {
    "sense_phi",
    "psychic_focus",
    "glow_seal",
    "beacon_seal",
    "death_sense",
    "life_sense",
    "diagnostic_glimpse",
    "phase_veil",
    "weak_breeze",
}


def parse_progression(path: pathlib.Path) -> dict[str, list[str]]:
    text = path.read_text(encoding="utf-8")
    schools: dict[str, list[str]] = {}
    for school, body in re.findall(
        r"case\s+(\w+)\s*->\s*List\.of\((.*?)\);",
        text,
        flags=re.S,
    ):
        if school == "default":
            continue
        ids = re.findall(r'id\("([a-z0-9_]+)"\)', body)
        schools[school.lower()] = ids
    return schools


def tier_for_index(index: int) -> int:
    if index < 0:
        return -1
    if index <= 2:
        return 0
    if index <= 6:
        return 1
    if index <= 11:
        return 2
    if index <= 17:
        return 3
    return 4


def cost_for_tier(tier: int, index_in_tier: int) -> int:
    # Slight ramp inside each tier so later spells cost a bit more.
    bumps = (0, 1, 2, 3, 4)
    bump = bumps[min(index_in_tier, len(bumps) - 1)]
    if tier <= 0:
        return 6 + bump  # 6–10
    if tier == 1:
        return 10 + bump  # 10–14
    if tier == 2:
        return 14 + bump  # 14–18
    if tier == 3:
        return 20 + min(bump, 3)  # 20–23
    return 28 + min(bump * 2, 10)  # 28–38 endgame


def cost_from_mastery(mastery: float) -> int:
    if mastery >= 0.8:
        return 28
    if mastery >= 0.65:
        return 22
    if mastery >= 0.5:
        return 18
    if mastery >= 0.25:
        return 14
    return 10


def main() -> None:
    progression = parse_progression(PROGRESSION_JAVA)
    index_by_id: dict[str, tuple[str, int]] = {}
    for school, ids in progression.items():
        for i, sid in enumerate(ids):
            index_by_id[sid] = (school, i)

    changed = 0
    for path in sorted(SPELLS.rglob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        sid = data["id"].split(":")[-1]
        mastery = float(data.get("min_mastery") or 0)
        old = data.get("base_cost")

        if sid in CHEAP:
            cost = 5 if mastery < 0.25 else 8
            tier_label = "cheap"
        elif sid in index_by_id:
            _school, idx = index_by_id[sid]
            tier = tier_for_index(idx)
            # position inside tier band
            starts = {0: 0, 1: 3, 2: 7, 3: 12, 4: 18}
            start = starts.get(tier, 18)
            cost = cost_for_tier(tier, idx - start)
            tier_label = f"t{tier}@{idx}"
        else:
            cost = cost_from_mastery(mastery)
            tier_label = "mastery"

        data["base_cost"] = cost
        path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        if old != cost:
            changed += 1
            print(f"{sid:28} {old:>5} -> {cost:<5} ({tier_label}, m={mastery})")
        else:
            print(f"{sid:28} {cost:>5} (unchanged, {tier_label})")

    print(f"\nUpdated {changed} spell costs.")


if __name__ == "__main__":
    main()
