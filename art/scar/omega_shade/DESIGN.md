# Omega Shade — design lock

## Lore
Ω-Scar parasite (`OmegaShadeEntity`): vex-like flight, latches and drains Ψ. No Φ glyph.

## Template
- Topology: **Vex / Allay** flying humanoid + ragged cloak cubes
- Path: **GeckoLib** (`geo/omega_shade.geo.json`)
- Atlas: **64×64**, opaque α=255, `entityCutoutNoCull`

## Shape lock
- Cube head, two sick-yellow pin-eyes, blank face (no mouth)
- Skinny torso with bruised-purple rib lines
- Bilateral cloak panels as torn wings
- Geo still has leftover `left_leg` / `right_leg` cubes — painted as hem rags, not feet

## Palette
| Role | RGB |
|------|-----|
| Void flesh | `18,14,24` / `28,20,36` |
| Cloak | `14,12,20` / `36,28,48` |
| Ω seam | `58,36,80` / `78,52,110` |
| Eye | `232,210,74` |

## Status
- [x] concept turnaround
- [x] SEGMENT_LOCK
- [x] atlas bake (`scripts/bake_unique_fauna_atlases.py`)
- [ ] in-game playtest
