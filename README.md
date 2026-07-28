# Effecoria — Minecraft Mod

NeoForge mod for **Minecraft 1.21.1**, based on the [Effecoria universe](https://github.com/pozikt/Effecoria) (ЕТЭВ / Φ-field theory).

**Status:** Phase 1 — Ψ/Φ resources, initiation, spell casting (6 spells), HUD.

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
2. **Right-click** → Elemental school | **Shift+Right-click** → Mental school
3. HUD shows Ψ (purple) and Φ (blue) bars at bottom-left
4. **R** — cast selected spell | **X** — cycle spell
5. `/effecoria debug` — stats in chat
6. `/effecoria cast effecoria:fire_burst` — direct cast (cheat sheet)

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
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Layers, packages, how to extend |
| [docs/FORMULAS.md](docs/FORMULAS.md) | Lore equations → game math |
| [docs/DESIGN.md](docs/DESIGN.md) | Lore → mechanics mapping |
| [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) | Guide for collaborators |
| [docs/BALANCE.md](docs/BALANCE.md) | Starting balance numbers |
| [docs/ART_BRIEF.md](docs/ART_BRIEF.md) | Texture/particle brief |

## Tests

Unit tests are optional for now — use `build -x test` (see Quick start).

## Roadmap

| Phase | Focus |
|-------|-------|
| **0** | MDK, FormulaEngine, placeholders, docs |
| **1** (now) | Ψ/Φ resources, initiation, 6 MVP spells, cast pipeline |
| **2** | Materials (lead, cold iron, essonite mechanics) |
| **3** | Races & Orkanum |
| **4** | Necromancy & liches |
| **5** | Technomagic & multiblocks |
| **6** | Worldgen, flora, structures |

## License

Code: All Rights Reserved (update when ready to open-source).  
Lore: [Effecoria encyclopedia](https://github.com/pozikt/Effecoria).
