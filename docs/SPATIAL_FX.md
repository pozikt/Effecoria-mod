# Spatial FX (Veil)

Successful Spatial casts may broadcast an S2C pulse to players within **48 blocks**. Effects are **world-anchored**: each nearby client projects the same world points into their view, so observers see the seam/well where the spell actually is — not a private HUD flash on the caster only.

## Buckets

| Bucket | Spells | Effect |
|--------|--------|--------|
| **Singularity** | `gravity_snare`, `gravity_well` / `gravity_field`, `spatial_singularity`, `absolute_fold`, `subspace_voyage`, `rift_excise` | Black-hole Veil post (`effecoria:singularity`) at focus |
| **Dimensional cut** | see modes below | Slash Veil post (`effecoria:spatial_cut`) + world particle arcs |
| **None** | `blink`, `far_blink`, `phase_veil`, `spatial_ward`, `warp_exchange`, `dimensional_anchor`, `void_step` | School particles only |

Routing: `SpatialVfx` (server). Singularity from `CastPresentation`; **cuts fire from hit methods** with correct geometry.

## Cut modes

| Mode | Spells | Geometry |
|------|--------|----------|
| **LINE** | `warp_bolt`, `void_lance`, `fold_repulse`, `spatial_surge`, `rift_yank` | Seam from A→B (caster eyes → target, or yank path) |
| **AROUND** | `rift_slash`, `rift_burst` | Radial Judgement cuts through the target |

Payload: `SpatialCutFxPayload(from, to, intensity, slashCount, mode)`.

## Singularity

Veil post `effecoria:singularity`: world-anchored lens (CenterUV / depth), ~1–2 s pulse.

## Dimensional cuts

Veil post `effecoria:spatial_cut`:

- Projects `from`/`to` (or focus for AROUND) every frame — same world seam for every viewer
- Depth-gated UV shear + cyan/white edge along the cut
- Brief strike (~8–16 ticks), not a gravity well

Plus dense world arcs (`SPATIAL_RIFT` / `SPATIAL_WARP` / end-rod) along the segment or radial stack — visible even when Iris skips the post.

## Dependencies

- **Veil** `4.2.1` (NeoForge) — jar-in-jar + required CLIENT dep in `neoforge.mods.toml`

## Compatibility

- **Sodium**: expected OK with Veil
- **Iris/Oculus shaderpack active**: post skipped; **world particles still play** for all nearby players
- **OptiFine**: not used on NeoForge

Whiffs do not trigger heavy posts.
