# Vitrified Golem — SEGMENT_LOCK (unique pipeline v2)

Concept: `vitrified_golem_concept_turnaround_v2.png`  
Lore: walking shard of Φ-flash — opaque black/ultramarine glass, gold veins, cyan Φ core.  
**No vanilla UV islands.** Atlas 128×128, 1 unit = 1 texel.

## Palette

| Role | RGB | Notes |
|------|-----|-------|
| glass deep | 10,14,32 | body fill |
| glass mid | 18,28,58 | panels |
| glass rich | 28,40,110 | ultramarine |
| edge | 4,6,16 | rims |
| gold hi / mid / lo | 255,230,120 / 240,195,55 / 180,120,28 | Φ cracks |
| cyan core / eye | 210,250,255 / 20,200,245 | emissive (opaque α=255) |
| crystal | 80,160,220 | left arm / back spikes / horns |

## Bone inventory (anim-compatible names)

| Bone | Parent | Pivot | Cubes (W×H×D) | Notes |
|------|--------|-------|---------------|-------|
| root | — | 0,0,0 | — | |
| head | root | 0,26,0 | head 8×8×8; visor 4×3×1; horn_l 2×3×2; horn_r 2×3×2 | horns = geometry |
| body | root | 0,20,0 | torso 10×12×6; core 4×4×3; pauldron_l 4×3×5; pauldron_r 4×3×5; spike1–3 | core protrudes from chest |
| right_arm | root | −7,24,0 | arm 4×14×4; blade_a 3×8×2; blade_b 2×6×2 | shard blade |
| left_arm | root | 7,24,0 | shard0 3×6×3; shard1 3×8×3; shard2 2×5×2 | crystal arm cluster |
| right_leg | root | −2.5,14,0 | leg 4×12×4; foot 5×2×5 | |
| left_leg | root | 2.5,14,0 | leg 4×12×4; foot 5×2×5 | |

Total height ≈ 34 units (~2.125 blocks).

## UV atlas map (unique packing, no overlap)

| Region | UV (u,v) | Box size | Footprint (2d+2w)×(d+h) |
|--------|----------|----------|-------------------------|
| head | 0,0 | 8×8×8 | 32×16 |
| visor | 32,0 | 4×3×1 | 10×4 |
| horn_l | 48,0 | 2×3×2 | 8×5 |
| horn_r | 56,0 | 2×3×2 | 8×5 |
| torso | 0,20 | 10×12×6 | 32×18 |
| core | 34,20 | 4×4×3 | 14×7 |
| pauldron_l | 34,30 | 4×3×5 | 18×8 |
| pauldron_r | 54,30 | 4×3×5 | 18×8 |
| spike1 | 74,0 | 3×6×2 | 10×8 |
| spike2 | 86,0 | 3×7×2 | 10×9 |
| spike3 | 98,0 | 2×5×2 | 8×7 |
| right_arm | 0,42 | 4×14×4 | 16×18 |
| blade_a | 18,42 | 3×8×2 | 10×10 |
| blade_b | 30,42 | 2×6×2 | 8×8 |
| left shard0 | 40,42 | 3×6×3 | 12×9 |
| left shard1 | 54,42 | 3×8×3 | 12×11 |
| left shard2 | 68,42 | 2×5×2 | 8×7 |
| right_leg | 80,42 | 4×12×4 | 16×16 |
| right_foot | 98,42 | 5×2×5 | 20×7 |
| left_leg | 0,64 | 4×12×4 | 16×16 |
| left_foot | 18,64 | 5×2×5 | 20×7 |

## Rules
- Every volumetric detail is a cube — never painted fake nose/horn on a face.
- All solid faces α=255 (opaque glass / crystal look via color, not transparency).
- Crystal / blade faces may use cutout only if silhouette flood is edge-only; default opaque.
