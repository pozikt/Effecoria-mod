# Effecoria — Minecraft Mod

NeoForge mod for **Minecraft 1.21.1**, based on the [Effecoria universe](https://github.com/pozikt/Effecoria) (ЕТЭВ / Φ-field theory).

**Status:** Stage I — magic loop playable (7 schools, breathing/Orkanum, Φ harness, seals, primer). Funnel polish toward “recommend to a friend.”

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

### First hour (survival / creative)

1. **Resonance Focus** (Effecoria creative tab) → right-click → pick a school (7 schools, including Seals)
2. First join also opens school select if not initiated; you get a **Magic Primer**
3. HUD: **Ψ** (purple) and **Φ** (blue) bottom-left; breathing % after initiation
4. **Hold X** — Resonance Hub (pick spell / left node = breathing drill). **Hold R** — charge cast, release to fire
5. Hungry or drowning → Orkanum strain (weaker regen / soft cast power). Eat and surface
6. Craft **Essonite Dust** → **Phi Cell** / upgrade Focus; ore generates underground; cells also in structure chests
7. Seals school: look at a block, **G** — word editor
8. Testers: `/effecoria debug`, `/effecoria reschool <school>`, `/effecoria max [school]`
9. Creative: Φ ∞ / free casts when `creative_god_mode` is on (config)

Known issues: [docs/monetization/KNOWN_ISSUES.md](docs/monetization/KNOWN_ISSUES.md) · Funnel gate: [STAGE_I_FUNNEL.md](docs/monetization/STAGE_I_FUNNEL.md)

## Project layout

```
src/main/java/com/effecoria/
  core/formula/     FormulaEngine — all lore math in one place
  core/magic/       MagicSchool, SpellDefinition
  config/           BalanceConfig (runtime tuning)
  content/          Blocks, items, tabs

src/main/resources/data/effecoria/spells/   MVP spell JSON (phase 1 loader)
addons/school-codex/                        Paid DLC scaffold (drafts only)
docs/                                       Design, architecture, monetization
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
| [docs/monetization/](docs/monetization/README.md) | Free core + Boosty/Patreon + server + DLC playbook |

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
