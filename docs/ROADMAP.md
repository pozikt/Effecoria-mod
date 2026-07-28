# Roadmap — Effecoria Mod

Prioritizes **complete magic & essence gameplay** before worldgen, races, and technomagic.

Lore reference: [Effecoria encyclopedia](https://github.com/pozikt/Effecoria).

---

## Phase 0 — Foundation ✅

- NeoForge MDK, `FormulaEngine`, package layout, docs

## Phase 1 — Magic MVP ✅ (polish ongoing)

| Done | Item |
|------|------|
| ✅ | Ψ/Φ resources, HUD, regen tick |
| ✅ | Initiation via Resonance Focus (Elemental / Mental) |
| ✅ | 6 spells + cast pipeline + JSON loader |
| ✅ | Elemental rework: fireball, wind charge, water stream |
| ✅ | Creative god mode (`creative_god_mode` in config) |
| ✅ | Water stream extinguishes fire |

| Next in Phase 1 | Item |
|-----------------|------|
| ⬜ | Spell VFX/sound polish (mental school) |
| ⬜ | `sense_phi` world highlight (client) |
| ⬜ | Patchouli stub entries |

---

## Phase 2 — Magic Core (next major focus)

**Goal:** Full player magic loop — from school choice to progression — before heavy world content.

### 2a — Initiation & UI

| Item | Notes |
|------|-------|
| School selection screen | Replace shift+click ritual with proper menu |
| Spell book / hotbar UI | Replace blind X-cycle with selectable grid |
| Initiation ritual block (optional) | Essonite structure alternative to instant item |

### 2b — Essence & Φ harnessing

| Item | Lore hook |
|------|-----------|
| Essonite ore → dust → focus | Already placeholder ore exists |
| Phi Cell item | Portable Φ buffer (inventory charge) |
| Essence infusion | Crafting tie-in for spell unlocks |

### 2c — Breathing techniques (passives)

Orkanum / oxygen coupling — affects **Ψ regeneration rate**, not ambient Φ.

| Stat | Field | Formula hook |
|------|-------|--------------|
| Breathing mastery | `biologyQ` | `regenPsi = Ψ_soul × Φ × Q_biology × …` |

Planned mechanics:
- Unlock via research or trainer NPC
- Tiers: normal breath → resonant breath → void breath
- Bonus scales with air supply / hunger / meditation stance
- HUD indicator for active breathing mode

### 2d — Physical training (passives)

Soul conditioning — affects **max Ψ capacity** and spell power baseline.

| Stat | Field | Formula hook |
|------|-------|--------------|
| Training rank | `soulStrength` | `regenPsi`, `spellPower` |
| Conditioning | `maxPsi` | Hard cap on Ψ bar |

Planned mechanics:
- XP from sprinting, swimming, combat, block breaking
- Training milestones unlock higher `maxPsi`
- Diminishing returns to avoid grind wall

### 2e — Spell progression

| Item | Notes |
|------|-------|
| Research / essence cost per spell | Beyond starter trio |
| Frequency tuning minigame | Optional resonance bonus |
| Entropy management tutorial | Backlash as skill gate |

---

## Phase 3 — Materials & Anti-Magic

Deferred from old Phase 2 — **after** magic core feels complete.

| Material | Effect |
|----------|--------|
| Lead (ZNΦ) | Zero-Φ zones, block casting |
| Cold iron | Ψ conversion block, dampening |
| Void obsidian | Seal structures |
| Essonite blocks | Φ conductors, reactor parts |

Replace crude stone-enclosure ZNΦ hack with tag-based detection.

---

## Phase 4 — Races & Orkanum

| Item | Notes |
|------|-------|
| Race selection at initiation | Modifies `biologyQ`, `soulStrength` baselines |
| Race-specific passives | Tie into breathing / training trees |
| Flora consumables | Mandragora analogs, Φ-sensitive plants |

---

## Phase 5 — Necromancy & Liches

| Item | Notes |
|------|-------|
| External Ψ relay | Never self-frequency change |
| Phylacteries | `regenPsiLich` path |
| Kin curses, undead armies | Phase 4 lore |

---

## Phase 6 — Technomagic

| Item | Notes |
|------|-------|
| Phi reactors, mage towers | Multiblocks |
| Rune circuits | `technomagicPower()` |
| Essonite wiring | Φ distribution networks |

---

## Phase 7 — World

| Item | Notes |
|------|-------|
| Essonite worldgen | Overworld ore veins |
| Φ anomalies | Biome modifiers |
| Structures | TSE remnants, mage ruins |

---

## Resource model (clarification)

| Resource | Type | Player progression |
|----------|------|-------------------|
| **Φ** | Environmental flux | Read from world; boosted by location, time, materials |
| **Ψ** | Internal operator energy | Max cap ↑ training; regen ↑ breathing × ambient Φ |
| **Essence** | Crafting / unlock currency | Drops, infusion, research |

Breathing improves how fast Ψ refills **in a given Φ field**.  
Training raises how much Ψ you can store and channel.

---

## Config knobs (testing)

| Key | Default | Purpose |
|-----|---------|---------|
| `creative_god_mode` | `true` | Infinite Φ + free casts in creative |

See `docs/BALANCE.md` for full list.
