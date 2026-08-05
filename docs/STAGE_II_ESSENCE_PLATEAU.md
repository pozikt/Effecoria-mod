# Stage II — Essence Plateau

Rare **jagged-peak mountain** (TerraBlender `PEAK_*` + `EROSION_0`, surface only).
`/locate biome` should land on a highland summit — not an orphan cave pocket.

Under that mountain, Φ generation follows the **surface column** to bedrock
(`effecoria:under_plateau_surface`), even when 3D underground biomes differ:

| Layer | Approx Y | Content |
|-------|----------|---------|
| **Sky** | ≥220 (islands 180–310) | Floating Φ-rock islands on the surface biome |
| **Surface** | peak highland | Φ-grass / Φ-dirt / Φ-stone crust, **Φ-geysers** |
| **Crust** | mid→high | Dense Φ-stone through the massif |
| **Crystal caverns** | 0–160 | Cave shells, veins, **essonite dripstone**, **druze**, **geodes**, **Φ-water lakes** |
| **Φ-root** | −64–0 | Near-solid essonite, lethal radiation |

## Crystal caverns (ETP)

- **Essonite spikes** (`essonite_pointed`) — dripstone-style stalactites/stalagmites; merged tips form Φ-columns. Drops shards; tips can yield pure essonite.
- **Druze** — multi-facing crystal buds/clusters on walls; silk touch keeps the cluster.
- **Essonite geodes** — rare hollow pockets lined with dripstone + crystals.
- **Φ-water lakes** — vanilla-style ellipsoid cave lakes (`LakeFeature` logic): carved into stone, filled with still Φ-water, Φ-stone walls. No shore crust. Spawns before dripstone; skips stalagmite forests; cliff edges waterfall or seal. Bucket places/fills; sneak-drink causes Φ-poison.

## Φ-geysers

Cyclic planetary cracks (`effecoria:phi_geyser`):

- **Dormant → precursor → eruption → cooldown** (moon-modulated dormant length)
- Near: mages get strong Ψ regen + slow Φ-cell charge; non-mages get nausea/hunger
- In plasma column: instant full Ψ, then Orkanum burn / exhaustion; non-mages die
- Touch crack: soul burn for mages
- Aftermath: essonite dust drops + `essonite_crust` puddles; rim is `void_obsidian`
- Manual trigger: right-click with Resonance Focus (initiated mage)

## Φ-fog

Atmospheric essonite mist (not water vapor), density 0–3:

| Density | Cause | Feel |
|---------|--------|------|
| Haze (1) | Plateau baseline | Soft ultramarine veil, +50% Ψ regen |
| Dense (2) | Valleys / near geysers | 12-block view, +100% regen, non-mage strain |
| Storm (3) | Thunder on plateau | Choking fog, Ψ drain + exhaustion |

Client: `ViewportEvent` fog tint + `phi_mist` / spark motes + movement wake trails.
Ω-fog (causality rupture) is reserved for future Ω-rifts.

## Φ-flora

- **Φ-turf** (`phi_grass`) — soil surface; **Φ-blades** (`phi_blades`) — glowing shoots, mycorrhizal spread
- **Φ-trees** — fancy trunk `phi_log` + indigo `phi_leaves`; sapling grows only on Φ-soil
- Drops: rare `phi_nut` / sapling from leaves; felling logs flash-alarms nearby players
- `phi_nut`: mages regain Ψ; non-mages get Φ-poison
- Art refs for leaf edit: `art/phi_flora/phi_leaves.png` vs `art/phi_flora/oak_leaves.png`

## Gameplay

Column-aware: standing in caves under the peak still counts as plateau.
Root (Y≤0): magic DPS + wither without protection.

## Dev

- **New world / new chunks** required after climate fix.
- `/locate biome effecoria:essence_plateau` → fly to coordinates → you should be on a mountain.
- Dig down: essonite-lined caves toward bedrock.
- Creative: place `phi_geyser` or wait for natural surface spawns.
