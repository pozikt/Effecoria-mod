# Spatial FX (Veil)

Successful Spatial casts may broadcast an S2C pulse to players within **48 blocks**. Effects are **world-anchored** distortion posts — Spatial school does **not** use particles.

## Buckets

| Bucket | Spells | Effect |
|--------|--------|--------|
| **Singularity** | `gravity_snare`, `gravity_well` / `gravity_field`, `spatial_singularity`, `subspace_voyage`, `rift_excise` | Black-hole Veil post (`effecoria:singularity`) |
| **Dimensional cut** | see modes below | Slash Veil post (`effecoria:spatial_cut`) |
| **Ripple** | `blink`, `far_blink`, `void_step`, `warp_exchange`, `absolute_fold` | Concentric space ripple (`effecoria:spatial_ripple`) — not a black hole |
| **None** | `phase_veil`, `spatial_ward`, `dimensional_anchor` | Sounds only |

## Cut modes

| Mode | Spells | Geometry |
|------|--------|----------|
| **LINE** | `warp_bolt`, `void_lance`, `fold_repulse`, `spatial_surge`, `rift_yank` | Seam from A→B |
| **AROUND** | `rift_slash`, `rift_burst` | Two thin seams; angles randomized per cast |

## Ripple (jumps)

Concentric UV waves around the blink/exchange point: space flexes and settles (~1 s). No horizon / accretion disk.

## Dependencies

- **Veil** `4.2.1` (NeoForge) — jar-in-jar + required CLIENT dep in `neoforge.mods.toml`

## Compatibility

- **Iris/Oculus shaderpack active**: posts skipped (no crash)
- Whiffs do not trigger heavy posts
