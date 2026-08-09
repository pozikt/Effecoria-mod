# Races — Effecoria MVP

Player races set the permanent **Orkanum baseline** (`biologyQ`) and 1–2 soft passives. No player model morph in MVP.

Source lore: [Effecoria encyclopedia races](https://github.com/pozikt/Effecoria/blob/main/encyclopedia/04-biology/races.md).

## Flow

1. First join (or legacy save without race) → **Race select** (mandatory).
2. Then **School select** (deferrable via Resonance Focus).
3. Ops: `/effecoria race` · `/effecoria race <id>` · `/effecoria rerace <id>`.

## Baselines & traits

| Race | Orkanum | Traits |
|------|---------|--------|
| Human | 0.60 | +5% breathing mastery gain |
| Orc | 0.85 | +10% max HP; −10% cast entropy & exhaustion |
| Elf | 0.75 | −8% spell cost; longer Φ-sense |
| Dwarf | 0.70 | mining / toughness; Seals −10% cost |
| Varanagi | 0.90 | less hunger on cast; heal while still |
| Dryad | 0.95 | Organic +12% / other −5%; forest hunger ease |
| Lonver | 1.05 | +15 max Ψ; better low-Φ regen; slower exhaustion decay |
| Harpy | 0.70 | Sprint + 3 jumps → elytra glide; space flaps climb (hunger); −50% fall damage |
| Vampire | 0.35 | ×0.5 Ψ regen; drink blood vials; sun burn without helmet |

Baselines are tunable in `effecoria-common.toml` (`race_baseline_*`).

## Out of scope

Custom skins/models, hard school locks, Lonver blood item, full vampire society.
