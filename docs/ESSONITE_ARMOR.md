# Essonite Armor (Φ Contour)

Essonite armor is a second Orkanum worn on the body: it stores ambient Φ, subsidizes casts, and unlocks combat actives.

## Tiers

| Tier | Pieces | Craft | Capacity / actives |
|------|--------|-------|--------------------|
| **Basic** | `phi_chitin_*` | Φ-chitin | Small charge · Φ-Flash only |
| **Crystal** | `crystal_essonite_*` | Pure essonite + leather | Mid charge · Flash, Crystal Skin, Wings, Ω-Block (3 inserts) |
| **Star** | `star_essonite_*` | Crystal piece + 2× star essonite | Max charge · all actives (1 insert for Ω) |

Mixed sets still share a charge pool. Wings / Crystal Skin / Ω-Block require a **crystal or star chestplate**.

## Charge

- Per-piece `ArmorPhiCharge` (0–1) in item CustomData.
- Regenerates from ambient Φ ([`PhiFieldService`](../src/main/java/com/effecoria/core/phi/PhiFieldService.java)); stops in ZNΦ / zero flux.
- **Piezo:** taking damage converts a fraction into charge.
- **Self-repair:** slow durability mend while pool ≥ threshold and Φ > 0.
- **Cold:** charged armor snuffs fire and softens fire damage (not freeze immunity).
- HUD pip: amber bar under Φ on the Ψ overlay.

## Passives

- **Φ-shielding:** bonus mental resist; chance to reject corruption curses (stronger while charged / higher tier).
- **Orkanum subsidy:** armor pool pays a tier fraction of spell Ψ cost ([`CastPipeline`](../src/main/java/com/effecoria/magic/CastPipeline.java)).
- **Φ-vision (helmet):** nearby Φ-fauna briefly glow.
- **Adaptive camo:** glow family stored on pieces by biome (emerald / gold / scar) — flavor + tooltip.

## Actives

| Key | Action |
|-----|--------|
| **Z** | Activate selected ability (sneak+Z toggles Umbra if inscribed) |
| **C** | Cycle ability: Flash → Crystal Skin → Wings → Ω-Block |

- **Φ-Flash:** dump pool → AoE knockback, slow, clears Ψ-wards.
- **Crystal Skin:** rooted + near-immune briefly (crystal+).
- **Essence Wings:** short steam/elytra-style flight (crystal+ chest).
- **Ω-Block:** consume void-obsidian inserts → temporary magic/wither/Ω immunity.

## Phonemes

Craft `phi_phoneme_*` scrolls; use with armor in the other hand.

| Phoneme | Effect |
|---------|--------|
| Firmitas | Faster self-repair |
| Umbra | Sneak+Z veil (invisibility) |
| Abnegatio | Next hit ignored (recharges from charge) |
| Servare | Slow heal while charged |
| Clausura | Cannot unequip without `psi_key` |

## Balance knobs

`BalanceConfig` keys under `essonite_armor_*` (regen, piezo, subsidy, pool Ψ worth, repair, flash, skin/wings/omega durations, insert costs, cooldown, fire reduction).

## Code map

- [`com.effecoria.armor`](../src/main/java/com/effecoria/armor/) — tier, data, service, item
- [`EssoniteArmorEvents`](../src/main/java/com/effecoria/event/EssoniteArmorEvents.java) — piezo, absorb, fire, Clausura
