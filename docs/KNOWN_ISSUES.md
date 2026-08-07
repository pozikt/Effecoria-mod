# Known issues — Effecoria (Stage I)

Honest list for Discord / Modrinth release notes. Update when shipping a demo build.

## Current (`0.2.4-alpha`)

| Issue | Severity | Notes |
|-------|----------|-------|
| Essonite / Phi Cell loot only in **new** chunks & **new** structure chests | Low | Old worlds need exploration beyond already-generated regions |
| Magic Primer uses vanilla writable-book look | Low | Content is custom; cover art optional |
| Overcast has cast feedback but no dedicated first-hour tip | Low | Primer Orkanum chapter covers it |
| Race Orkanum baselines not content yet | Low | Hook `BiologyService.applyRaceBaseline` ready for Stage II |
| Lich ascension / dragon thrall intentionally disabled | Info | Stage IV+ |
| Identity spell icons — some reuse placeholders (e.g. organic/necro clones) | Low | Art pass later |
| Full 1h cast/seal/school smoke not automated | Info | Run before calling Demo 4 “final” |

## Recent fixes (0.2.3–0.2.4)

- **Mind Blank** — mobs stay de-aggroed (retarget block + spell combat)
- **Hydro slice** — cutting blocks along slash

## How to report

Prefer a short repro: school, spell id, world type, log snippet from `run/logs/latest.log`.

Template: [DISCORD_BUG_TEMPLATE.md](DISCORD_BUG_TEMPLATE.md) when publishing community channels.
