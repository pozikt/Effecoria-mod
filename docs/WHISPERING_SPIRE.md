# Whispering Spire (Шепчущий Пик)

Natural Φ-reactor landmark on the **Essence Plateau**: layered essonite / void-obsidian cone, star-essonite caldera, and a continuous Φ-plasma column from the vent.

## Placement rules

- Rejects sites near biome borders (plateau must hold in a ~16-block ring; only checks chunks available during worldgen).
- Rejects steep footprint relief (skirts would float off cliffs).
- Prefers local high ground; sinks the cone 8 blocks into rock and fills foundation under the footprint.
- Generation uses a **hollow shell** (not solid fill) and skips out-of-height positions to avoid integrated-server watchdog / crash.

## Anatomy (generation)

Truncated cone tapers to a **rim** (not a needle tip). A **bowl** is carved into the summit; a **throat shaft** of void obsidian drops several blocks into the massif; the vent sits at the bottom of that throat so the plasma column rises *out of* the mountain.

## In-game (MVP without fauna/temples)

- **Worldgen:** rare `whispering_spire` feature (`rarity_filter` 220) on `#effecoria:is_essence_plateau`.
- **Vent BE:** registers the site, ambient beacon/amethyst audio, plasma + spark column particles.
- **Zones** (horizontal distance from vent, configurable in `BalanceConfig`):

| Zone | Default radius | Effect |
|------|----------------|--------|
| Green | ≤96 | Mild Φ bonus; mages regen Ψ; non-mages slight euphoria (Speed) |
| Yellow | ≤48 | Burn / exhaustion without Φ protection; whispers for initiates |
| Red | ≤24 | Heavy magic DPS + wither/blind without protection |
| Black | ≤8 | Near-lethal soul-burn; protection only softens |

- **Whisper:** initiated mages get periodic action-bar Φ-phoneme stubs (seal-lore hooks; no Logos unlock yet).
- **Loot:** slope chest with `star_essonite`, pure essonite, Φ-cell, dust, void obsidian.
- **Materials:** `star_essonite_block`, `star_essonite` item, `whispering_spire_vent`.

## API

```java
WhisperingSpireService.zoneAt(level, pos)
WhisperingSpireService.phiBonus(level, pos)  // hooked into PhiFieldService.sample
WhisperingSpireService.nearestVent(level, pos)
```

## Deferred

- Φ-wyverns, ice worms, temples/monasteries
- Custom ModSounds (infrasound / dedicated whisper)
- Seal Logos unlock from phonemes
- True planetary-scale distances (km fiction stays lore; radii are Minecraft-scaled)
