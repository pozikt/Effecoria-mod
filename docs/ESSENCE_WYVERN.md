# Essence Wyvern (Φ-Wyvern)

Classical **wyvern** apex flyer — **no front legs**; wings are the forelimbs.
Adult MVP only (hatchling → Peak Dragon later).

## Identity

| | |
|--|--|
| ID | `effecoria:essence_wyvern` |
| Role | Higher Φ-troph / lithophage / territorial flyer |
| Habitats (lore) | Essence Plateau, Whispering Spikes, crystal forests, Φ-mountains |
| Threat | S-class vs unprepared mages |

## Anatomy lock

- Bones: `root`, `body`, `neck`, `head`, `left_wing`→`mid`→`tip` (×2), `left_leg`, `right_leg`, `tail`
- Crest = body/tail cubes (essonite spines), not separate limbs
- Hitbox MVP: **3.2 × 3.4**; visual wingspan open ~10 blocks; 3-bone wing flap (ref: Tiny Dragons / Antarchy)

## Pipeline assets

| Asset | Path |
|-------|------|
| Design lock | `art/essence_wyvern/DESIGN.md` |
| Concept turnaround | `art/essence_wyvern/concept_turnaround.png` |
| Builder | `scripts/build_essence_wyvern.py` |
| Geo / anim / atlas | `assets/effecoria/geo|animations|textures/entity/essence_wyvern.*` |
| Render | `EssenceWyvernRenderer` — `entityCutoutNoCull` |

## MVP combat / behavior

- **Grounded by default** (gravity + stroll); does **not** perpetual-hover
- Sit/rest: crouch on hind legs, **wing knuckles as props**
- Takeoff to hunt (mid/far targets), soar patrol, land when calm
- Dive melee + plasma breath (prefers airborne)
- Lithophagy flavor later (eat stone blocks) — stub only

## Later

- Dedicated drop items (bone, scale, crest, orkanum)
- Lithophagy (eat stone / essonite ore)
- Life stages; Lonver pact flavor
- Whispering Spire / Φ-mountain spawns
