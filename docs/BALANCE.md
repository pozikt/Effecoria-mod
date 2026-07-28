# Balance (starting values)

All globals can be overridden in `config/effecoria-common.toml`.  
Spell-specific values live in JSON under `data/effecoria/spells/`.

## Global config defaults

| Key | Default | Notes |
|-----|---------|-------|
| psi_regen_scale | 0.05 | Tune after playtesting |
| spell_power_scale | 1.0 | |
| low_phi_cost_factor | 0.5 | Extra cost in weak Φ |
| entropy_scale | 0.02 | Backlash buildup |
| entropy_threshold | 1.0 | Trigger backlash |
| resonance_width_hz | 5.0 | Off-frequency forgiveness |

## Human baseline (Phase 1 target)

| Stat | Value |
|------|-------|
| Ψ_soul | 1.0 |
| Starting E_Ψ | 50 |
| Max E_Ψ | 100 |
| Q_biology | 0.6 |
| Φ default (overworld surface) | 1.0 |

## Spell costs (JSON)

| Spell | base_cost | side_entropy |
|-------|-----------|--------------|
| sense_phi | 5 | 0.02 |
| mental_sting | 8 | 0.06 |
| mental_push | 12 | 0.08 |
| wind_push | 10 | 0.05 |
| fire_burst | 14 | 0.10 |
| water_stream | 12 | 0.06 |

## Φ-field sampling (Phase 1 plan)

| Factor | Multiplier |
|--------|------------|
| Surface overworld | 1.0 |
| Deep underground | 0.7 |
| Nether | 1.2 |
| End | 0.5 |
| Night (overworld) | 1.0 |
| Day (sun visible) | 1.1 |
| Inside lead tag radius | 0.0 (ZNΦ) |

## Playtest goals

- Full Ψ pool lasts ~20–30 casts of medium spells
- Regen from empty to full: ~2–3 minutes in normal Φ
- Backlash triggers after ~10–15 spam casts without pause

Adjust after Phase 1 internal test.
