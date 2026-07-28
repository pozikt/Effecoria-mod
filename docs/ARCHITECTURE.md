# Architecture

Effecoria is built in **layers**. Content changes should not require core changes.

## Layer diagram

```
┌─────────────────────────────────────────┐
│  Content (JSON, datagen, textures)      │  ← most additions here
├─────────────────────────────────────────┤
│  API (SpellEffect, PhiModifier, …)      │  ← new behavior types
├─────────────────────────────────────────┤
│  Core (FormulaEngine, PhiField, Psi)    │  ← rarely touched
└─────────────────────────────────────────┘
```

## Packages

| Package | Responsibility | Change frequency |
|---------|----------------|------------------|
| `com.effecoria` | Mod entry | Rare |
| `com.effecoria.core.formula` | All physics math | Low |
| `com.effecoria.core.magic` | Schools, spell data types | Low |
| `com.effecoria.core.phi` | Φ-field grid (phase 1) | Medium |
| `com.effecoria.core.psi` | Player Ψ attachment (phase 1) | Medium |
| `com.effecoria.magic` | Cast pipeline, spell loader (phase 1) | Medium |
| `com.effecoria.effect` | Reusable effect types (phase 1) | Medium |
| `com.effecoria.content` | Blocks, items, entities | High |
| `com.effecoria.config` | Balance knobs | Low |
| `com.effecoria.network` | Client↔server packets (phase 1) | Medium |

## Adding content

### New spell (same behavior as existing effect)

1. Create `data/effecoria/spells/<school>/<name>.json`
2. Add lang keys under `assets/effecoria/lang/`
3. No Java required if `effects[].type` already exists

### New effect type (new behavior)

1. Add enum/constants in `effect/EffectTypes.java`
2. Implement `EffectExecutor` handler
3. Register in cast pipeline
4. Reference from JSON

### New material modifier

1. Add block/item tag: `data/effecoria/tags/blocks/phi_conductors.json`
2. Add row in `data/effecoria/material_modifiers.json` (phase 2)
3. `PhiModifier` reads tag automatically

### New race

1. JSON in `data/effecoria/races/<id>.json` (phase 3)
2. Fields: `soul_strength`, `biology_q`, `frequency_bias`, optional school weights

## Formula rule

**Never compute Ψ/Φ math inside spell or item classes.** Always call `FormulaEngine`.

## Multiplayer

All cast and resource changes are **server-authoritative**. Client sends intent; server validates via `FormulaEngine.canCast()` and syncs state.

## Testing strategy

| Layer | Test type |
|-------|-----------|
| FormulaEngine | JUnit (`src/test/java`) |
| Spell loading | Unit test with Codec (phase 1) |
| Cast pipeline | GameTest optional (phase 1+) |
| Integration | Manual multiplayer session |

## Dependencies (planned)

| Mod | Phase | Purpose |
|-----|-------|---------|
| Patchouli | 1 | In-game encyclopedia |
| JEI | 2+ | Recipes (optional) |
| GeckoLib | 4+ | Mob animations |

## Related repo

Lore source of truth: https://github.com/pozikt/Effecoria
