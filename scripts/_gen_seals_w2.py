import json
import os

base = "src/main/resources/data/effecoria/spells/seals"
spells = [
    ("shock_glyph", 0.45, 11, 0.05, 0.12, 0.1, "seals", [{"type": "effecoria:place_trap_seal", "duration_ticks": 1200, "range": 8, "trap_damage_mult": 1.8}]),
    ("ward_glyph", 0.48, 10, 0.04, 0.11, 0.09, "seals", [{"type": "effecoria:place_fortify_seal", "duration_ticks": 1800, "range": 9}]),
    ("repulsion_seal", 0.5, 12, 0.05, 0.13, 0.11, "seals", [{"type": "effecoria:place_repulse_seal", "duration_ticks": 900, "range": 8, "repulse_force": 0.9}]),
    ("anchor_fortify", 0.52, 14, 0.05, 0.15, 0.12, "seals", [{"type": "effecoria:place_fortify_seal", "duration_ticks": -1, "range": 8}]),
    ("permanent_glow", 0.46, 10, 0.03, 0.1, 0.08, "utility", [{"type": "effecoria:place_glow_seal", "duration_ticks": -1, "range": 10}]),
    ("snare_matrix", 0.54, 13, 0.06, 0.16, 0.13, "seals", [{"type": "effecoria:place_snare_seal", "duration_ticks": 1500, "range": 9, "slow_amplifier": 4}]),
    ("shock_trap", 0.58, 15, 0.06, 0.18, 0.15, "seals", [{"type": "effecoria:place_trap_seal", "duration_ticks": 2400, "range": 10, "trap_damage_mult": 2.4}]),
    ("omega_ward", 0.65, 18, 0.07, 0.22, 0.2, "seals", [
        {"type": "effecoria:place_fortify_seal", "duration_ticks": 4800, "range": 9},
    ]),
]
for sid, hz, cost, ent, phi, mast, cat, effects in spells:
    doc = {
        "id": f"effecoria:{sid}",
        "school": "seals",
        "frequency_hz": hz,
        "base_cost": cost,
        "power_multiplier": 1.0,
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
