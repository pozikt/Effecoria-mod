# Φ-Wyvern (Essence Wyvern) — design lock

## Lore source
- User bestiary brief (ETP): higher Φ-troph, lithophage, plasma breath
- Habitats: Essence Plateau, Whispering Spikes, crystal forests, Φ-mountains

## Critical shape lock
- **WYVERN, not dragon:** **no front legs**. Forelimbs = wing arms only.
- Segment source: `concept_turnaround_contur.png` → `SEGMENT_LOCK.md`
- Bones: `body_0/1/2`, `neck_0..5`, `head`+`jaw`+`horn_0..4`, wing×3×2, leg `thigh→knee→shin→foot`(+4 claws)×2, `tail_0..9`
- Crest = cube on each neck/body/tail ring (gold tip)
- Two **hind legs only** (digitigrade 4-bone); long multi-ring neck/tail; **3-bone wing chain**

## Silhouette (Minecraft scale — adult MVP)
- Lore size 15–30 m wingspan → **game adult ~ wingspan 8–10 blocks open, body ~2 blocks long torso + long neck/tail, standing ~3.4 blocks**
- **Horizontal predatory stance** (not upright chicken): chest forward, S-neck, long whip tail
- Massive wing sails visible even when folded (~2+ blocks tall membranes)
- Crest of essonite spines along back/tail
- Matte corundum scales; gold Φ-lines; gold eyes

## Materials / palette
- Base: matte stone-grey / beige rock (opaque α=255) — **no purple/ultramarine fill**
- Accent: gold Φ-veins on wings, gold crest tips; yellow eyes with black slit pupils
- Belly: beige rib plates on neck/torso rings

## Behavior stubs (MVP)
- Hostile territorial flyer on Essence Plateau (rare)
- Melee dive + placeholder plasma breath (particles + damage)
- Lithophagy flavor later (eat stone blocks) — stub only

## Template
- Topology: **phantom** (winged body, no forelegs) + **custom** neck/tail/hind legs
- Path: **GeckoLib**
- Atlas: **512×512** (many ring cubes from contour bake)

## Life stages (later)
- Hatchling / young / adult / ancient / Peak Dragon — **adult only** in this MVP

## Status
- [x] lore
- [x] concept turnaround
- [x] geo blockout (**v2 apex rebuild** — no chicken stub; huge wings, long neck/tail)
- [x] texture atlas (256)
- [x] animations
- [x] entity code
- [ ] in-game verify (re-check after v2)
