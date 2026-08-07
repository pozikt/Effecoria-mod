# Effecoria School Codex (addon scaffold)

Optional content pack for the [Effecoria](../../README.md) core.  
**Status:** scaffold / drafts only — not wired into the game yet.

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

Drafts are **not** loaded by the game until a real NeoForge subproject is wired.

## Implementation outline (later)

1. Add Gradle subproject or separate repo depending on core artifact.
2. Copy approved drafts into `src/main/resources/data/effecoria_school_codex/...` (or merge into `effecoria` namespace if the loader expects one pack — prefer separate namespace + core registry scan if supported).
3. Add lang keys RU/EN.
4. Smoke-test: core alone works; core+codex loads extras; codex alone fails gracefully.

## License

All Rights Reserved (same as core until stated otherwise).
