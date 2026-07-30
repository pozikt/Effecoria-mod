import json
import os

base = "src/main/resources/data/effecoria/spells/mental"
spells = [
    ("mind_bolt", 8.0, 6, 0.05, 0.12, 0.1, "combat", [{"type": "effecoria:mind_bolt", "damage_dice": "1d8"}]),
    ("psychic_scream", 9.0, 8, 0.06, 0.14, 0.12, "combat", [{"type": "effecoria:psychic_scream", "damage_dice": "1d6", "radius": 6, "confusion_ticks": 80}]),
    ("thought_lance", 11.0, 12, 0.07, 0.18, 0.2, "combat", [{"type": "effecoria:thought_lance", "damage_dice": "2d8", "slow_ticks": 60}]),
    ("neural_lock", 10.0, 10, 0.06, 0.16, 0.18, "combat", [{"type": "effecoria:neural_lock", "duration_ticks": 100}]),
    ("telekinetic_crush", 11.5, 11, 0.07, 0.17, 0.2, "combat", [{"type": "effecoria:telekinetic_crush", "damage_dice": "2d6", "lift_force": 0.65}]),
    ("mass_confusion", 12.0, 12, 0.07, 0.2, 0.22, "combat", [{"type": "effecoria:mass_confusion", "radius": 8, "confusion_ticks": 120}]),
    ("psychic_barrier", 9.5, 9, 0.05, 0.15, 0.15, "utility", [{"type": "effecoria:psychic_barrier", "duration_ticks": 200}]),
    ("mind_probe", 10.5, 8, 0.05, 0.16, 0.16, "utility", [{"type": "effecoria:mind_probe", "duration_ticks": 160}]),
    ("synaptic_overload", 13.0, 14, 0.08, 0.24, 0.28, "combat", [{"type": "effecoria:synaptic_overload", "damage_dice": "2d8", "confusion_ticks": 100}]),
    ("psychic_drain", 14.0, 15, 0.08, 0.26, 0.3, "combat", [{"type": "effecoria:psychic_drain", "damage_dice": "2d6", "psi_ratio": 0.2}]),
    ("mental_fortress", 16.0, 18, 0.08, 0.3, 0.35, "utility", [{"type": "effecoria:mental_fortress", "duration_ticks": 400}]),
    ("thought_bomb", 18.0, 22, 0.09, 0.35, 0.45, "combat", [{"type": "effecoria:thought_bomb", "damage_dice": "3d10", "radius": 5}]),
    ("psychic_storm", 20.0, 24, 0.1, 0.38, 0.5, "combat", [{"type": "effecoria:psychic_storm", "damage_dice": "3d6", "radius": 7, "pulses": 3}]),
    ("psychic_amplify", 8.5, 10, 0.04, 0.14, 0.12, "utility", [{"type": "effecoria:psychic_amplify", "duration_ticks": 400}]),
    ("omega_mind", 22.0, 28, 0.11, 0.42, 0.55, "utility", [{"type": "effecoria:omega_mind", "duration_ticks": 200, "phi_sense_ticks": 320}]),
]
for sid, hz, cost, ent, phi, mast, cat, effects in spells:
    doc = {
        "id": f"effecoria:{sid}",
        "school": "mental",
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
