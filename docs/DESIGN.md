# Design — Lore to Mechanics

Maps [Effecoria encyclopedia](https://github.com/pozikt/Effecoria) to Minecraft systems.

## Core identity

Magic is **not** a separate ruleset — it is Φ-field conversion through the Ψ-operator (soul) and biological Orkanum. Every mechanic should be explainable in-universe via Patchouli (later).

## MVP scope (Phase 1)

**Magic schools:** Elemental, Mental  
**Initiation:** `resonance_focus` ritual fixes one school forever  
**Resources:** Ψ bar (internal), Φ indicator (environmental)

### Elemental spells

| ID | Effect | Lore note |
|----|--------|-----------|
| `fire_burst` | AoE fire + ignite | Medium frequency band, thermal Φ dump |
| `wind_push` | Knockback | Kinetic channel |
| `stone_shield` | Absorption hearts | Structural field |

### Mental spells

| ID | Effect | Lore note |
|----|--------|-----------|
| `mental_push` | Telekinesis | Low-frequency Ψ actuator |
| `mental_sting` | Damage + slow | Cognitive noise (mandragora analog) |
| `sense_phi` | Highlight Φ gradient | Orkanum as detector |

## Systems deferred by phase

| Phase | Lore source | Game system |
|-------|-------------|-------------|
| 2 | materials-catalog | Tags, ZNΦ blocks, conductors |
| 3 | races.md | Race selection, Q_biology modifiers |
| 4 | liches.md, magic-system | Necromancy, phylacteries, kin curses |
| 5 | technomagic.md, phi-reactors.md | Multiblocks, Φ cells |
| 6 | flora.md, astrophysical-objects.md | Biomes, TSE structures |

## Hard rules (never break)

1. **One magic type per operator** after initiation
2. **No Ψ without Φ flux** (except stored E_Ψ)
3. **Lead / cold iron / void obsidian** must counter magic meaningfully
4. **Necromancy** always uses external Ψ relay (never self-frequency change)
5. **Lich** has Q_biology = 0; powered by phylactery

## Player onboarding (planned)

1. Find essonite → craft resonance focus
2. Perform initiation → choose school (UI or ritual structure)
3. Patchouli entry unlocks: "Spectral Purity Theorem"
4. First spell granted; others via research/loot

## Multiplayer PvP considerations

- Φ-field readable (sense_phi) but not exact enemy Ψ values
- Lead builds viable vs mages
- Backlash punishes spam casting

## Open design questions

Track in GitHub Issues:

- [ ] Initiation: GUI vs block ritual?
- [ ] Death on wrong-school cast attempt or gradual damage?
- [ ] Φ-field visible to all players or skill-gated?
