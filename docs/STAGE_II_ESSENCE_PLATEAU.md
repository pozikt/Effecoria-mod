# Stage II — Essence Plateau

Rare high-mountain overworld biome (TerraBlender). Full vertical column (−64 → 320):

| Layer | Approx Y | Content |
|-------|----------|---------|
| **Sky** | ≥220 (islands 180–310) | Floating Φ-rock islands (`phi_sky_island`), extreme peaks |
| **Surface** | ~193–219 (+ highland tops) | Φ-grass / Φ-dirt, mist mood |
| **Crust** | ~129–192 (infusion 64–320) | Dense Φ-stone through the mountain mass |
| **Crystal caverns** | 0–128 (veins to 160) | Essonite veins, crystal clusters |
| **Φ-root** | −64–0 | Near-solid essonite, **lethal radiation** without protection |

Climate: high-slice + peak ridges, erosion 0–1, **surface+underground** depth so the whole column is this biome.

## Gameplay

- Surface: +Φ, +25% spell power, −25% cost, +regen, 80% gravity
- Caverns: extra Φ (`plateau_cave_phi_bonus`)
- Root (Y≤0): larger Φ + **magic DPS + wither** if unprotected
- Φ-soil spreads; vanilla crops fail on Φ-dirt

## Dev

- New chunks only. `/locate biome effecoria:essence_plateau`
- Requires TerraBlender NeoForge 1.21.1

## Art

See `art/essence_plateau/` and `art/phi_earth/`.
