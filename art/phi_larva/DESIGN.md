# Φ-Larva — design lock

## Lore source
- [`docs/STAGE_II_ESSENCE_PLATEAU.md`](../../docs/STAGE_II_ESSENCE_PLATEAU.md): passive crawler near essonite; breeds on dust; adults restore Ψ / charge Φ-cells; drops dust.

## Critical shape lock
- **Segmented grub/worm**, not a beetle — **no legs**.
- Soft pill-shaped rings (overlap), low to ground.
- Head: black dot eyes, short amber antennae with bulbs, tiny dark mandibles.
- Glow: lateral amber spots per segment, dorsal Φ-line, rear **Φ** mark.

## Silhouette (Minecraft)
- Hitbox stays ~`0.55 × 0.35` ([`ModEntities`](../../src/main/java/com/effecoria/content/ModEntities.java)).
- Model length ~10–12 units (~0.7 block), height ~4–5 units.

## Palette (from concept)
| Role | RGB approx |
|------|------------|
| Chitin mid | `184,152,104` |
| Chitin dark | `152,120,72` / `144,112,72` |
| Belly light | `200,176,128` |
| Glow amber | `248,216,104` … `248,208,96` |
| Eye / mandible | near-black |

No purple; no flat blue stripe stub.

## Template
- Topology: silverfish-style segment chain + custom head/antennae.
- Path: **GeckoLib**
- Atlas: **128×128**

## Status
- [x] lore
- [x] concept turnaround (`concept_turnaround.png`)
- [x] SEGMENT_LOCK
- [x] geo / atlas / idle+crawl via `scripts/build_phi_larva.py`
- [x] synced CRAWLING anim flag
- [ ] in-game playtest
