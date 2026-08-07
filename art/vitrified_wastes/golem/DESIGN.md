# Vitrified Golem — design lock

## Lore source
- `docs/lore/DISCORD_VITRIFIED_WASTES.md`
- Hostile walking shard of a Φ-catastrophe (black glass + ultramarine/gold)

## Silhouette (locked)
- Compact biped, **~2.2 blocks tall** (smaller than iron golem, **not** a uniform 88% shrink)
- Broad torso of fused glass plates
- Head: short block with **one bright Φ-core / eye** on the face
- Arms: thick glass clubs, **proportional** to torso (not iron-golem 30-tall arms)
- Legs: solid pillars that **reach the ground** (no floating / no sink)
- Optional: jagged glass shards on shoulders/elbows (opaque cutouts later)

## Materials
- Opaque black / indigo glass (alpha 255 everywhere on body)
- Accent: cyan Φ glow (eyes/core), gold flecks (vine-like cracks)
- **No** soft transparency — glass is painted, not see-through

## Size class
- Hitbox: ~0.9 × 2.2
- Atlas: 128×128
- Template topology: iron-golem biped bone names, **custom integer box sizes**

## Vanilla template
- Topology: iron golem (`head`, `body`, `right_arm`, `left_arm`, `right_leg`, `left_leg`)
- Animations: iron golem walk/attack math, **scaled angles** for shorter limbs
- Not: copy iron UV with float-scaled cubes (that broke proportions/UVs)

## Concept read (turnaround)
- Compact biped; torso reads as main mass; arms thick but not floor-length clubs
- Legs solid pillars to ground line; head block with cyan face core
- Palette: opaque indigo glass + gold flecks + cyan eye

## Box blockout (integer, feet y=0)
| Part | Size | Notes |
|------|------|-------|
| legs | 4×14×4 | hip pivot y=14 |
| body | 10×12×6 + shoulder 12×3×8 | y=14…26 |
| head | 8×8×8 + eye plate | y=26…34 |
| arms | 4×16×4 | shoulder pivot y=24 |

Total height **34 units ≈ 2.125 blocks**. Hitbox `0.9 × 2.15`.

## Status
- [x] lore
- [x] concept turnaround
- [x] geo blockout
- [x] texture atlas
- [x] animations
- [ ] in-game verify
