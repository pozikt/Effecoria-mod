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

**Currently disabled** (`DeadWastelandHydrology.DRYING_ENABLED = false`). Water bodies stay.
Re-enable later: flip the flag and restore `neoforge/biome_modifier/wasteland_strip_water.json`.

Reserved design (when re-enabled): inland-only strip + border buffer so ocean edges do not trench.

## Gameplay

| Effect | Behavior |
|--------|----------|
| Φ sample | Forced `zeroFlux` |
| Spells | Blocked |
| Ψ regen | 0 |
| Φ-cell / essonite | Slow drain / rare bleed |
| Mages | Weakness → coma after config ticks |
| Non-mages | Apathy package |
