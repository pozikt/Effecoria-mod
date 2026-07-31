# Spatial singularity FX (Veil)

Successful Spatial casts broadcast a short S2C pulse to players within **48 blocks** of the cast focus.

The client activates Veil post-pipeline `effecoria:singularity`:

- World-anchored **black-hole lens** (gravitational bend, photon ring, accretion, event horizon)
- Depth-aware soft gate so foreground geometry is less distorted
- Rise–fall pulse ~1–2 s from cast power

## Dependencies

- **Veil** `4.2.1` (NeoForge) — jar-in-jar + required CLIENT dep in `neoforge.mods.toml`

## Compatibility

- **Sodium**: expected OK with Veil
- **Iris/Oculus shaderpack active**: pipeline is skipped (no crash); particles from `CastPresentation` remain
- **OptiFine**: not used on NeoForge

Whiffs do not trigger the effect.
