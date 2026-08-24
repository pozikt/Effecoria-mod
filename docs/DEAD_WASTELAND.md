# Dead Wasteland — Zero Φ-Flow Zone

**Мёртвая Пустошь** (`effecoria:dead_wasteland`): Φ_nature ≈ 0.

## Placement

TerraBlender region (weight **8**) replaces vanilla **`minecraft:desert`** in that
region slice — exact desert climate points, not a hand-rolled niche. That keeps
`/locate biome` on real sand flats (no under-bedrock phantoms) and leaves eroded
badlands alone. Shares desert replacement with **Vitrified Wastes** (also weight **8**),
so arid land splits between ash flats and flash-glass. Needs a **new world**.
See `docs/VITRIFIED_WASTES.md`.

`/locate biome effecoria:dead_wasteland` → then fly / `/tp` to **X Z** at surface
(ignore a low Y if chat shows one). Or explore arid inland until the bleached crust appears.

## Look

- Continuous crust: `parched_sand` (top ~2) → `ash_soil` → `parched_sandstone`
- Rare shallow ash washes (no gravel pits)
- Sparse dead bushes / rare ash trunks
- Gray fog; no rain; no lakes / water springs / fluid disks

## Dry hydrology

**Shipped inland-only.** `DeadWastelandHydrology.DRYING_ENABLED` is on:

- Worldgen `strip_wasteland_water` evaporates inland lakes (air); one pass per chunk
- Biome reads use the noise map (never `getBiome` during features — that crashed WorldGenRegion)
- Ocean shelves stay wet: desert climate keeps water tagged as wasteland far past the beach, so we also skip columns within 80 blocks of ocean / beach / river noise
- Runtime: reject buckets / fluid spread inland only; slow local strip near the player
- Never dries on chunk load (that OOM'd `/locate biome`)

Needs **new chunks** for the worldgen strip; standing in old soaked chunks still dries a small radius around the player.

## Gameplay

| Effect | Behavior |
|--------|----------|
| Φ sample | Forced `zeroFlux` |
| Spells | Blocked |
| Ψ regen | 0 |
| Φ-cell / essonite | Slow drain / rare bleed |
| Mages | Weakness → coma after config ticks |
| Non-mages | Apathy package |
