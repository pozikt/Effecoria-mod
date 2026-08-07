# Eidos — SEGMENT_LOCK

Source: `art/eidos/concept_turnaround.png`

## Palette

| Role | RGB |
|------|-----|
| Body | `192,176,152` / `200,176,144` |
| Dark edge | `160,128,96` |
| Glow | `248,216,104` / `208,176,120` |
| Eye | `255,240,160` + dark rim |

## Bones

| Bone | Parent | Notes |
|------|--------|-------|
| `torso` | root | Main body ~5×8×3 @ y≈12–20 |
| `head` | torso | ~4.4 cube |
| `eye` | head | Protruding amber iris + black slit (~2.8³) |
| `arm_l` / `arm_r` | torso | Thin wisps |
| `trail_0`…`trail_2` | torso | Tapering glow trail downward |
| `ring_a` | torso | Outer orbital ring (4–6 segment cubes) |
| `ring_b` | torso | Inner / chest-level ring |
| `phi_chest` | torso | Small Φ emblem cube |

## Motion
- Idle: float bob on torso, rings spin opposite Y, trail sway, eye pulse via scale optional.
- Gift: rings expand, arms open, trail brightens (synced `GIFTING` flag — giftAnimTicks was server-only).
