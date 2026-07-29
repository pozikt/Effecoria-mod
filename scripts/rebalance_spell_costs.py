import json
import pathlib

root = pathlib.Path(__file__).resolve().parents[1] / "src/main/resources/data/effecoria/spells"
cheap = {"sense_phi", "psychic_focus", "glow_seal", "beacon_seal"}

for path in sorted(root.rglob("*.json")):
    data = json.loads(path.read_text(encoding="utf-8"))
    sid = data["id"].split(":")[-1]
    mastery = float(data.get("min_mastery") or 0)
    if sid in cheap:
        cost = 12
    elif mastery >= 0.8:
        cost = 20
    elif mastery >= 0.65:
        cost = 17
    elif mastery >= 0.25:
        cost = 15
    else:
        cost = 15
    data["base_cost"] = cost
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(data["id"], cost, mastery)
