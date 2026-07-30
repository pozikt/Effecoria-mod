import json
import os

base = "src/main/resources/data/effecoria/spells/corruption"
spells = [
    ("rot_touch", 0.5, 8, 0.05, 0.1, 0.08, "combat", [{"type": "effecoria:rot_touch", "damage_dice": "1d6", "hunger_ticks": 100, "range": 10}]),
    ("entropy_lash", 0.55, 9, 0.06, 0.12, 0.1, "combat", [{"type": "effecoria:entropy_lash", "damage_dice": "1d8", "poison_ticks": 60, "range": 11}]),
    ("plague_bolt", 0.6, 10, 0.06, 0.14, 0.12, "combat", [{"type": "effecoria:plague_bolt", "damage_dice": "2d6", "poison_ticks": 80, "weakness_ticks": 60, "range": 12}]),
    ("festering_wound", 0.65, 11, 0.07, 0.16, 0.14, "combat", [{"type": "effecoria:festering_wound", "damage_dice": "1d10", "wither_ticks": 100, "wither_amplifier": 1, "range": 11}]),
    ("miasma_cloak", 0.58, 10, 0.06, 0.14, 0.12, "utility", [{"type": "effecoria:miasma_cloak", "duration_ticks": 160, "radius": 4, "damage_per_second": 1.5}]),
    ("blight_surge", 0.7, 12, 0.07, 0.18, 0.16, "combat", [{"type": "effecoria:blight_surge", "damage_dice": "2d6", "radius": 7, "poison_ticks": 100}]),
    ("decay_bind", 0.68, 11, 0.07, 0.17, 0.15, "combat", [{"type": "effecoria:decay_bind", "root_ticks": 80, "glow_ticks": 100, "poison_ticks": 80, "range": 10}]),
    ("blight_field", 0.75, 14, 0.08, 0.2, 0.18, "combat", [{"type": "effecoria:blight_field", "duration_ticks": 220, "radius": 5.5, "damage_per_second": 2, "poison_amplifier": 1}]),
    ("entropy_aegis", 0.72, 13, 0.07, 0.19, 0.17, "utility", [{"type": "effecoria:entropy_aegis", "duration_ticks": 260, "resistance_amplifier": 1}]),
    ("tainted_leech", 0.78, 15, 0.08, 0.22, 0.2, "combat", [{"type": "effecoria:tainted_leech", "damage_dice": "2d8", "heal_ratio": 0.4, "range": 11}]),
    ("virulent_wave", 0.82, 16, 0.09, 0.24, 0.22, "combat", [{"type": "effecoria:virulent_wave", "radius": 8, "poison_ticks": 120, "poison_amplifier": 2}]),
    ("plague_crown", 0.88, 18, 0.09, 0.28, 0.26, "combat", [{"type": "effecoria:plague_crown", "duration_ticks": 140, "damage_dice": "3d6", "radius": 7, "poison_ticks": 100}]),
    ("omega_blight", 0.95, 22, 0.11, 0.35, 0.32, "combat", [{"type": "effecoria:omega_blight", "damage_dice": "4d8", "radius": 10, "field_ticks": 180, "field_dps": 3}]),
]
for sid, hz, cost, ent, phi, mast, cat, effects in spells:
    doc = {
        "id": f"effecoria:{sid}",
        "school": "corruption",
        "frequency_hz": hz,
        "base_cost": cost,
        "power_multiplier": 1.05,
        "side_entropy": ent,
        "min_phi": phi,
        "min_mastery": mast,
        "radial_category": cat,
        "effects": effects,
    }
    with open(os.path.join(base, sid + ".json"), "w", encoding="utf-8") as f:
        json.dump(doc, f, indent=2)
        f.write("\n")
print("wrote", len(spells))
