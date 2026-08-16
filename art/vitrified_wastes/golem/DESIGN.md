# Vitrified Golem — design lock (unique pipeline v2)

## Lore source
- `docs/lore/DISCORD_VITRIFIED_WASTES.md`
- Hostile walking shard of a Φ-catastrophe

## Pipeline
1. Concept turnaround (5 views): `vitrified_golem_concept_turnaround_v2.png`
2. Segment bake: `SEGMENT_LOCK.md` (unique UV, **no vanilla islands**)
3. Build: `python scripts/build_vitrified_golem_unique_v2.py`
4. GeckoLib geo + opaque 128 atlas + existing anim bone names

## Silhouette
- Compact biped ~2.125 blocks (34 units)
- Head cube + **visor cube** + **two horn cubes**
- Torso + **protruding Φ-core** + pauldrons + **3 back spike cubes**
- Right arm + **blade shards**; left arm = **crystal cluster**
- Legs + **foot pads**

## Materials
- Opaque glass (α=255) — look via ultramarine + gold veins, not transparency
- Crystal accents: cyan/blue opaque
- Emissive: cyan Φ on visor/core

## Bones (anim-compatible)
`root`, `head`, `body`, `right_arm`, `left_arm`, `right_leg`, `left_leg`  
Extra cubes live on those bones (no new anim targets required).

## Status
- [x] lore
- [x] unique concept turnaround
- [x] SEGMENT_LOCK + unique UV
- [x] geo + atlas bake
- [ ] in-game verify
