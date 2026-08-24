# Known issues — Effecoria (Stage II)

Honest list for Discord / GitHub release notes. Update when shipping a demo build.

## Current (`0.2.5-alpha` + races WIP)

| Issue | Severity | Notes |
|-------|----------|-------|
| New biomes / structures need **new chunks** (or a new world) | Medium | Already-explored regions stay old terrain |
| Ω-Scar / Crystal Forest / Emerald Canopy fauna — first custom atlas pass | Low | Playtest in-game; Φ-larva / wyvern already had unique atlases |
| Races: **no player model morph** (Origins-style visuals later) | Info | Passives + Orkanum baseline only |
| Vampire sun / blood drink is rough MVP | Low | Helmet blocks sun; vials restore Ψ |
| Magic Primer cover is a vanilla-book recolor | Low | Consciousness Matrix successor is planned — see MAGIC_PLAN.md |
| Identity / school spell icons — some placeholders | Low | Art pass later |
| Lich ascension / dragon thrall intentionally disabled | Info | Stage IV+ |
| Full automated cast/biome smoke not in CI | Info | Manual locate + walk recommended |

## How to report

Prefer a short repro: school, spell id, race, biome, world type, log snippet from `logs/latest.log`.
