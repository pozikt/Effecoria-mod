# Crystal Crab — design lock

## Lore source
- [`docs/STAGE_II_ESSENCE_PLATEAU.md`](../../docs/STAGE_II_ESSENCE_PLATEAU.md): neutral; angers if essonite crystals/core mined nearby; drops shards + Φ-chitin.

## Critical shape lock
- Wide low **golem-crab**: stone-beige carapace + amber essonite crystals on back/claws.
- **2** big front claws (pincers), **3** walking legs per side (6 total).
- Short eyestalks with dark eyes.
- No purple / no blue stub palette.

## Silhouette (Minecraft)
- Hitbox ~`1.1 × 0.75` ([`ModEntities`](../../src/main/java/com/effecoria/content/ModEntities.java)).
- Model ~16–18u wide, ~10–12u long, feet at y=0.

## Palette (from concept)
| Role | RGB approx |
|------|------------|
| Shell mid | `168,136,88` / `176,144,96` |
| Shell dark | `88,72,48` / `48,40,24` |
| Crystal amber | `200,168,112` … `208,168,112` |
| Eye | near-black |

## Template
- Topology: spider/crab (body + bilateral legs + claws) custom boxes.
- Path: **GeckoLib**
- Atlas: **128×128**

## Status
- [x] lore
- [x] concept turnaround
- [x] SEGMENT_LOCK
- [x] geo / atlas / anims
- [x] synced WALKING + ATTACK
- [ ] in-game playtest
