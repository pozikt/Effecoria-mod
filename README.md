# Effecoria — Minecraft Mod

NeoForge mod for **Minecraft 1.21.1**, based on the [Effecoria universe](https://github.com/pozikt/Effecoria) (ЕТЭВ / Φ-field theory).

**Status:** Phase 0 — skeleton, formula engine, data templates.

## Requirements

- **JDK 21** (Temurin recommended)
- IntelliJ IDEA or VS Code with Gradle support

## Quick start

```bash
git clone https://github.com/pozikt/Effecoria-mod.git
cd Effecoria-mod
./gradlew runClient    # Windows: gradlew.bat runClient
```

First launch downloads NeoForge and Minecraft — expect several minutes.

## Project layout

```
src/main/java/com/effecoria/
  core/formula/     FormulaEngine — all lore math in one place
  core/magic/       MagicSchool, SpellDefinition
  config/           BalanceConfig (runtime tuning)
  content/          Blocks, items, tabs

src/main/resources/data/effecoria/spells/   MVP spell JSON (phase 1 loader)
docs/                                       Design & architecture
src/test/java/                              Unit tests (no Minecraft needed)
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

```bash
./gradlew test
```

FormulaEngine tests run **without** launching Minecraft.

## Roadmap

| Phase | Focus |
|-------|-------|
| **0** (now) | MDK, FormulaEngine, placeholders, docs |
| **1** | Ψ/Φ resources, initiation, 6 MVP spells, cast pipeline |
| **2** | Materials (lead, cold iron, essonite mechanics) |
| **3** | Races & Orkanum |
| **4** | Necromancy & liches |
| **5** | Technomagic & multiblocks |
| **6** | Worldgen, flora, structures |

## License

Code: All Rights Reserved (update when ready to open-source).  
Lore: [Effecoria encyclopedia](https://github.com/pozikt/Effecoria).
