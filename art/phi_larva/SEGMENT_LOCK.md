# Φ-Larva — SEGMENT_LOCK

Source: `art/phi_larva/concept_turnaround.png`

## Palette

| Role | RGB |
|------|-----|
| Chitin | `184,152,104` / `176,144,96` |
| Dark rim | `144,112,72` |
| Belly | `200,176,128` |
| Glow | `248,216,104` |
| Eye / mandible | `20,16,12` |

## Bones

| Bone | Parent | Notes (approx W×H×D) |
|------|--------|----------------------|
| `seg_0` | root | Front ring ~5×4×3; belly + L/R glow + dorsal speck |
| `seg_1` | seg_0 | ~5.5×4.2×3 + glow |
| `seg_2` | seg_1 | Widest mid ~6×4.5×3 |
| `seg_3` | seg_2 | ~5.5×4.2×3 |
| `seg_4` | seg_3 | ~5×4×3 |
| `seg_5` | seg_4 | Rear ~4×3.5×3 + Φ mark cube |
| `head` | seg_0 | ~4.2×3.4×3.2 forward of seg_0 |
| `left_antenna` / `right_antenna` | head | Thin stalk + bulb |
| `mandible_l` / `mandible_r` | head | Tiny dark pincers |

## Seams
- 6 overlapping body rings (concept 6–7); bake **6** bones with depth overlap.
- Lateral glow **one pair per segment**.
- Dorsal glow strip as thin cube on each seg; rear Φ as painted face on `seg_5`.

## Motion
- Idle: soft body sway + glow pulse (scale on glow cubes via antenna tips / anim).
- Crawl: peristaltic Y/Z wave along `seg_0…5` + head bob.
