# Effecoria School Codex (addon scaffold)

Paid content pack for the free [Effecoria](../../README.md) core.  
**Status:** scaffold / drafts only — do not sell until gates in [docs/monetization/DLC_SCHOOL_CODEX.md](../../docs/monetization/DLC_SCHOOL_CODEX.md) pass.

## Planned mod id

`effecoria_school_codex`

## Dependency

- Minecraft 1.21.1 + NeoForge (same as core)
- Required: `effecoria` (same major.minor line)

## Content drafts

| Path | Purpose |
|------|---------|
| `content-draft/spells/` | Extra spell JSON per school |
| `content-draft/seals/` | Extra seal words |
| `STORE_PAGE.md` | Boosty/Gumroad copy |

Drafts are **not** loaded by the game until a real NeoForge subproject is wired.

## Implementation outline (later)

1. Add Gradle subproject or separate repo depending on core artifact.
2. Copy approved drafts into `src/main/resources/data/effecoria_school_codex/...` (or merge into `effecoria` namespace if the loader expects one pack — prefer separate namespace + core registry scan if supported).
3. Add lang keys RU/EN.
4. Smoke-test: core alone works; core+codex loads extras; codex alone fails gracefully.
5. Distribute paid jar via Boosty shop; keep core on Modrinth.

## License

All Rights Reserved (same as core until stated otherwise). Paid distribution does not transfer copyright to buyers — license grant for personal use only (state on store page).
