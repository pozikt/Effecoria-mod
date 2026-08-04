#!/usr/bin/env python3
"""Dump all Effecoria spells with RU names and descriptions."""
from __future__ import annotations

import json
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LANG = json.loads((ROOT / "src/main/resources/assets/effecoria/lang/ru_ru.json").read_text(encoding="utf-8"))
SPELLS = ROOT / "src/main/resources/data/effecoria/spells"
OUT = ROOT / "tmp_spell_catalog.json"


def main():
    by_school: dict[str, list] = defaultdict(list)
    missing = []
    for f in sorted(SPELLS.rglob("*.json")):
        data = json.loads(f.read_text(encoding="utf-8"))
        sid = data.get("id", f"effecoria:{f.stem}").split(":")[-1]
        school = data.get("school", f.parent.name)
        name = LANG.get(f"spell.effecoria.{sid}", sid)
        desc = LANG.get(f"spell.effecoria.{sid}.desc") or LANG.get(f"spell.effecoria.{sid}.description") or ""
        if not desc:
            missing.append(sid)
            # Infer from effects as fallback note
            effects = [e.get("type", "").split(":")[-1] for e in data.get("effects", [])]
            desc = f"(эффект: {', '.join(effects) or '—'})"
        by_school[school].append(
            {
                "id": sid,
                "name": name,
                "desc": desc,
                "cost": data.get("base_cost"),
                "hz": data.get("frequency_hz"),
                "category": data.get("radial_category", ""),
            }
        )

    payload = {
        "total": sum(len(v) for v in by_school.values()),
        "missing_desc": missing,
        "schools": {k: v for k, v in sorted(by_school.items())},
    }
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {OUT} total={payload['total']} missing_desc={len(missing)}")
    for sch, items in payload["schools"].items():
        print(f"\n=== {sch.upper()} ({len(items)}) ===")
        for it in items:
            print(f"- {it['name']} (`{it['id']}`): {it['desc']}")


if __name__ == "__main__":
    main()
