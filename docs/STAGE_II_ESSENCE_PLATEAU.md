# Stage II — Essence Plateau

Rare **jagged-peak mountain** (TerraBlender `PEAK_*` + `EROSION_0`, surface only).
`/locate biome` should land on a highland summit — not an orphan cave pocket.

Under that mountain, Φ generation follows the **surface column** to bedrock
(`effecoria:under_plateau_surface`), even when 3D underground biomes differ:

| Layer | Approx Y | Content |
|-------|----------|---------|
| **Sky** | ≥220 (islands 180–310) | Floating Φ-rock islands on the surface biome |
| **Surface** | peak highland | Φ-grass / Φ-dirt / Φ-stone crust |
| **Crust** | mid→high | Dense Φ-stone through the massif |
| **Crystal caverns** | 0–160 | Cave shells of essonite + veins + crystal buds |
| **Φ-root** | −64–0 | Near-solid essonite, lethal radiation |

## Gameplay

Column-aware: standing in caves under the peak still counts as plateau.
Root (Y≤0): magic DPS + wither without protection.

## Dev

- **New world / new chunks** required after climate fix.
- `/locate biome effecoria:essence_plateau` → fly to coordinates → you should be on a mountain.
- Dig down: essonite-lined caves toward bedrock.
