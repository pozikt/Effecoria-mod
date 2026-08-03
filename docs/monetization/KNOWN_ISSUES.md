# Known issues — Effecoria (Stage I)

Honest list for Discord / Modrinth release notes. Update when shipping a demo build.

## Current (0.2.x-alpha)

| Issue | Severity | Notes |
|-------|----------|-------|
| Essonite / Phi Cell loot only in **new** chunks & **new** structure chests | Low | Old worlds need exploration beyond already-generated regions |
| Magic Primer uses vanilla writable-book look | Low | Content is custom; cover art optional |
| Dedicated-server smoke not formally signed off | Med | Run `runServer` before public hosted play |
| Overcast has cast feedback but no dedicated first-hour tip | Low | Primer Orkanum chapter covers it |
| Race Orkanum baselines not content yet | Low | Hook `BiologyService.applyRaceBaseline` ready for Stage II |
| Lich ascension / dragon thrall intentionally disabled | Info | Stage IV+ |

## How to report

Prefer a short repro: school, spell id, world type, log snippet from `run/logs/latest.log`.

Template: [DISCORD_BUG_TEMPLATE.md](DISCORD_BUG_TEMPLATE.md) when publishing community channels.
