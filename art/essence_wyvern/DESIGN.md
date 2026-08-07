# Φ-Wyvern (Essence Wyvern) — design lock

## Lore source
- User bestiary brief (ETP): higher Φ-troph, lithophage, plasma breath
- Habitats: Essence Plateau, Whispering Spikes, crystal forests, Φ-mountains

## Critical shape lock
- **WYVERN, not dragon:** **no front legs**. Forelimbs = wing arms only.
- Bones: `root`, `body`, `neck`, `head`, wing chain ×2, **leg chain** `left_leg`→`shin`→`foot` ×2, `tail`
- Crest spines are body/tail cubes (no separate crest bone in MVP)
- Two powerful **hind legs only** (digitigrade, Tiny Dragons-style 3-bone); long neck/tail; **3-bone wing chain**

## Silhouette (Minecraft scale — adult MVP)
- Lore size 15–30 m wingspan → **game adult ~ wingspan 8–10 blocks open, body ~2 blocks long torso + long neck/tail, standing ~3.4 blocks**
- **Horizontal predatory stance** (not upright chicken): chest forward, S-neck, long whip tail
- Massive wing sails visible even when folded (~2+ blocks tall membranes)
- Crest of essonite spines along back/tail
- Matte corundum scales; gold Φ-lines; gold eyes

## Materials / palette
- Base: matte stone-grey / mountain rock (opaque α=255)
- Accent calm: subtle ultramarine undertone
- Accent combat: gold Φ-veins, cyan-white plasma in mouth
- Crest spines: brightest gold/cyan

## Behavior stubs (MVP)
- Hostile territorial flyer on Essence Plateau (rare)
- Melee dive + placeholder plasma breath (particles + damage)
- Lithophagy flavor later (eat stone blocks) — stub only

## Template
- Topology: **phantom** (winged body, no forelegs) + **custom** neck/tail/hind legs
- Path: **GeckoLib**
- Atlas: **256×256** (large wing islands)

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
