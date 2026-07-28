# Contributing

Welcome. This guide assumes **Java knowledge** but **no prior Minecraft modding**.

## Setup

1. Install JDK 21
2. Clone repo, open in IntelliJ → Import Gradle project
3. Run `gradlew runClient`
4. Run `gradlew test` — verifies FormulaEngine without game

## Where to start (no NeoForge experience)

| Task | Difficulty | NeoForge needed? |
|------|------------|------------------|
| Edit spell JSON | Easy | No |
| Edit lang files (RU/EN) | Easy | No |
| Write FormulaEngine unit tests | Easy | No |
| Edit BALANCE.md / DESIGN.md | Easy | No |
| Add blocks/items from template | Medium | Some |
| Networking / attachments | Hard | Yes |

**Recommended first PR:** add or tweak a spell JSON + lang keys + unit test for a formula edge case.

## Branch workflow

```
main        ← stable, builds
develop     ← integration (create when team grows)
feature/*   ← your work
```

## Code rules

1. **All Φ/Ψ math** → `FormulaEngine` only
2. **No magic numbers** in gameplay logic → `BalanceConfig` or JSON
3. **Server authoritative** for casts and resources
4. Match existing package layout (see ARCHITECTURE.md)
5. UTF-8, Java 21, 4-space indent

## Adding a spell (checklist)

- [ ] JSON in `data/effecoria/spells/<school>/`
- [ ] Lang: `en_us.json` + `ru_ru.json`
- [ ] Effect type exists or new handler implemented
- [ ] Balance row in BALANCE.md if non-trivial
- [ ] Tested in multiplayer (two clients)

## Project communication

- **Design questions** → GitHub Issues with label `design`
- **Lore accuracy** → cross-check [Effecoria encyclopedia](https://github.com/pozikt/Effecoria)
- **Architecture changes** → discuss before large PRs

## Useful links

- [NeoForge 1.21.1 docs](https://docs.neoforged.net/docs/1.21.1/gettingstarted/)
- [NeoForge Discord](https://discord.neoforged.net/)
