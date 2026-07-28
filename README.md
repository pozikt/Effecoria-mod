# Effecoria — Minecraft Mod

NeoForge mod for **Minecraft 1.21.1**, based on the [Effecoria universe](https://github.com/pozikt/Effecoria) (ЕТЭВ / Φ-field theory).

**Status:** Phase 2 — magic UI, breathing passives, physical training foundation.

## Requirements

- **JDK 21** (Temurin recommended)
- IntelliJ IDEA or VS Code with Gradle support

## Quick start

```bash
git clone https://github.com/pozikt/Effecoria-mod.git
cd Effecoria-mod
./gradlew clean build -x test   # Windows: gradlew.bat clean build -x test
./gradlew runClient
```

First launch downloads NeoForge and Minecraft — expect several minutes.

### Phase 1 gameplay

1. Take **Resonance Focus** from creative tab Effecoria
2. **Right-click** Resonance Focus → school selection menu (Elemental / Mental / Organic / Necromancy)
3. HUD shows Ψ (purple) and Φ (blue) bars at bottom-left
4. **R** — cast selected spell | **X** — spell book
5. `/effecoria debug` — stats in chat
6. `/effecoria reschool elemental` — switch school without recreating world
7. **Creative mode:** Φ shows ∞, casts are free, Ψ stays full (toggle `creative_god_mode` in config)
8. `/effecoria cast effecoria:fire_burst` — direct cast (cheat sheet)

## Project layout

```
src/main/java/com/effecoria/
  core/formula/     FormulaEngine — all lore math in one place
  core/magic/       MagicSchool, SpellDefinition
  config/           BalanceConfig (runtime tuning)
  content/          Blocks, items, tabs

src/main/resources/data/effecoria/spells/   MVP spell JSON (phase 1 loader)
docs/                                       Design & architecture
src/test/java/                              Unit tests (optional, WIP)
```

## Documentation

| Doc | Purpose |
|-----|---------|
| [docs/ROADMAP.md](docs/ROADMAP.md) | Development phases & priorities |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Layers, packages, how to extend |
| [docs/FORMULAS.md](docs/FORMULAS.md) | Lore equations → game math |
| [docs/DESIGN.md](docs/DESIGN.md) | Lore → mechanics mapping |
| [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) | Guide for collaborators |
| [docs/BALANCE.md](docs/BALANCE.md) | Starting balance numbers |
| [docs/ART_BRIEF.md](docs/ART_BRIEF.md) | Texture/particle brief |

## Tests

Unit tests are optional for now — use `build -x test` (see Quick start).

## Roadmap

**Priority:** complete magic & essence systems before worldgen and races.

| Phase | Focus |
|-------|-------|
| **0** ✅ | MDK, FormulaEngine, placeholders, docs |
| **1** ✅ | Ψ/Φ, initiation, 6 spells, HUD, creative god mode |
| **2** (now) | **Magic core** — school UI, spell book, breathing, training |
| **3** | Materials (lead ZNΦ, cold iron, essonite mechanics) |
| **4** | Races & Orkanum |
| **5** | Necromancy & liches |
| **6** | Technomagic & multiblocks |
| **7** | Worldgen, flora, structures |

Full breakdown: [docs/ROADMAP.md](docs/ROADMAP.md)

## License

Code: All Rights Reserved (update when ready to open-source).  
Lore: [Effecoria encyclopedia](https://github.com/pozikt/Effecoria).
