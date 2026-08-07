# Crystal Crab — SEGMENT_LOCK

Source: `art/crystal_crab/concept_turnaround.png`

## Palette

| Role | RGB |
|------|-----|
| Shell | `168,136,88` / `176,144,96` |
| Dark joint | `72,56,40` / `48,40,24` |
| Crystal | `208,168,112` / `248,216,104` |
| Eye | `20,16,12` |

## Bones

| Bone | Parent | Notes |
|------|--------|-------|
| `body` | root | Carapace ~12×5×10 + belly plate |
| `crystal_main` | body | Tall central amber shard |
| `crystal_l0`…`crystal_l2` / `crystal_r0`…`crystal_r2` | body | Side crown shards |
| `eye_l` / `eye_r` | body | Short stalks + dark eye bulb |
| `claw_l` / `claw_r` | body | Upper arm of pincer |
| `claw_l_tip` / `claw_r_tip` | claw_* | Lower jaw of pincer + claw crystal |
| `leg_l0`…`leg_l2` | body | Outer→inner left walk legs (upper) |
| `leg_l0_shin`… | leg_l* | Lower leg to ground |
| `leg_r0`…`leg_r2` + `_shin` | body | Mirror right |

## Motion
- Idle: slight claw open/close, crystal idle, eye bob.
- Walk: alternating left/right leg pairs (tripod-ish).
- Attack: both claws snap forward (synced ATTACK flag — client cannot see server-only ticks).
