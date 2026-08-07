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
| **Φ-ядро** | −64–0 | Near-solid `essonite_block`, lethal Φ-radiation |

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

## Φ-fauna

GeckoLib mobs native to the plateau (spawn eggs in creative):

| Mob | Role | Notes |
|-----|------|-------|
| **Φ-larva** | Passive | Segmented glowing grub (GeckoLib); breeds on essonite dust; adults restore Ψ / charge Φ-cells; drops dust. Art: `art/phi_larva/` |
| **Crystal crab** | Neutral | Angers if essonite crystals/core are mined nearby; drops shards + Φ-chitin |
| **Eidos** | Rare passive | Offer essonite crystal / pure essonite → random buff or short-range portal hop |
| **Essence Wyvern** | Rare hostile apex | Classical wyvern (**no front legs**); flight + melee + plasma-breath stub; see `docs/ESSENCE_WYVERN.md` |

*(Twisted mage deferred; wyvern life-stages / dedicated drops later.)*

## Φ-ядро (core)

Lowest band under the plateau (`Y ≤ plateau_root_max_y`, default 0):

- Worldgen (`phi_root_core` / `PhiCoreFeature`): dense blobs of glowing `essonite_block` (~88%), sparse deepslate essonite veins, rare Φ-stone seams
- Gameplay: magic DPS + wither II / confusion / weakness every second without Φ protection (gold / focus / Φ-cell / essonite dust)
- Extreme Φ bonus for casting; unprotected mages also take Orkanum exhaustion

## Whispering Spire

Rare natural Φ-reactor peak (see `docs/WHISPERING_SPIRE.md`): essonite/void-obsidian cone, star-essonite caldera, vent plasma column, scaled Green/Yellow/Red/Black hazard zones. No fauna/temples yet.

## Gameplay

Column-aware: standing in caves under the peak still counts as plateau.
Φ-ядро (Y≤0): lethal radiation without protection.

## Dev

- **New world / new chunks** required after climate fix.
- `/locate biome effecoria:essence_plateau` → fly to coordinates → you should be on a mountain.
- Dig to bedrock under the peak: near-solid essonite core.
- Creative: place `phi_geyser` or wait for natural surface spawns.
- Creative: place `whispering_spire_vent` to test zones/column without hunting a rare feature.
