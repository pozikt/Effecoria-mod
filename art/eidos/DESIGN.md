# Eidos — design lock

## Lore source
- [`docs/STAGE_II_ESSENCE_PLATEAU.md`](../../docs/STAGE_II_ESSENCE_PLATEAU.md): rare passive Φ-field being; offer essonite crystal / pure essonite → buff or short portal hop.

## Critical shape lock
- Floating **legless** humanoid of beige energy + amber Φ glow.
- Head with single amber eye / Φ mark; torso with chest Φ; arms as wisps.
- Lower body tapers into a **particle trail** (glow cubes).
- **Two** orbital amber rings (broken into segment cubes) around midsection.
- No purple.

## Silhouette (Minecraft)
- Hitbox ~`0.7 × 1.4` ([`ModEntities`](../../src/main/java/com/effecoria/content/ModEntities.java)).
- Hovering; no gravity (existing AI).

## Palette (from concept)
| Role | RGB approx |
|------|------------|
| Body beige | `148,120,88` / `168,136,96` (darker mid for contrast) |
| Body dark | `88,68,48` |
| Φ glow / rings | bright amber `240,180,48` … |
| Eye | amber iris + black slit pupil (dedicated `eye` bone) |

## Template
- Topology: allay-like floating humanoid + custom rings/trail.
- Path: **GeckoLib**
- Atlas: **128×128**, opaque α=255 (cutout; no translucent holes)

## Status
- [x] lore / concept / SEGMENT_LOCK
- [x] geo / atlas / idle+gift
- [x] synced GIFTING
- [x] atlas paint pass (`scripts/bake_unique_fauna_atlases.py`)
- [ ] in-game playtest
