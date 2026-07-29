# Roadmap — Effecoria Mod

Prioritizes **perfecting magic & energy** first, then world layers, technology, and finally cosmology (TSE → Ω).

Lore reference: [Effecoria encyclopedia](https://github.com/pozikt/Effecoria).

---

## Stage order (locked)

| Stage | Theme | Goal |
|-------|--------|------|
| **I** | Magic & energy | Complete, polished cast loop — feel “done to play” |
| **II** | Environment | Biomes, flora, fauna, surface Φ anomalies |
| **III** | Caves & ores | Underground, new ores, deep Φ zones |
| **IV** | Technology | Phi Cells → reactors, towers, rune networks |
| **V** | TSE | Topological singularities as content & risk |
| **VI** | Ω-space | Analog of Hell — dimension + inhabitants (after TSE) |

Do not start II+ until Stage I is intentionally “finished enough.”

---

## Stage I — Magic & energy (current)

**Goal:** Full player magic loop — school → progression → cast → entropy — before heavy world content.

### Done

| Item | Notes |
|------|-------|
| Ψ/Φ resources, HUD, regen | Day/night Φ via solar clock; regen + breathing % |
| Initiation + school select | Resonance Focus; 7 schools |
| Cast pipeline + JSON spells | 21 starter spells (3 per school) |
| Spatial / Corruption / Seals | Block seals (trap, fortify, glow + real light) |
| Breathing mastery | Continuous 0–1; scroll + calm meditation |
| Training → soul / max Ψ | Sprint/swim milestones |
| Mastery (breathing × essence) | Cost/power slight adjust |
| Whiff economy | 25% Ψ on miss target/block |
| Creative god mode | Config |
| Spell book + hold-X radial | Favorites + Movement/Combat/Utility/Seals rings |
| Admin `/effecoria set` | Playtest knobs |

### Still in Stage I (polish → “perfect”)

| Area | Items |
|------|-------|
| **Feel** | Spell VFX/SFX; `sense_phi` world highlight; Patchouli stubs |
| **Progression** | Essence / research unlocks beyond starter trio; entropy tutorial |
| **Φ harness** | Essonite dust → focus craft; **Phi Cell** (portable Φ buffer) — items first, not worldgen |
| **Breathing** | Air/hunger coupling; optional trainer/modes |
| **Training** | Broader XP sources; diminishing returns |
| **Seals** | More types, conflicts/stacking rules |
| **Anti-magic** | Lead (ZNΦ), cold iron — tag-based, replaces stone-box hack |
| **School depth** | Extra spells per school; necromancy endgame hooks (lich later if needed) |

Optional inside I (only if it serves magic feel): initiation ritual block.

---

## Stage II — Environment

Surface world as Φ ecology.

| Item | Notes |
|------|-------|
| Biomes / biome modifiers | Φ density, day-night character |
| Flora | Φ-sensitive plants, consumables (mandragora analogs) |
| Fauna | Creatures tied to schools / Φ bands |
| Surface anomalies | Mild Φ storms, ZNΦ patches (content, not just anti-cast) |

Races / Orkanum baselines fit naturally here (biologyQ hooks already exist).

---

## Stage III — Caves & ores

| Item | Notes |
|------|-------|
| Essonite & new ores | Real worldgen veins |
| Cave biomes / deep Φ | Low light, high entropy pockets |
| Underground structures | Small mage ruins, sealed chambers |

Materials that were “craft only” in Stage I get proper sources here.

---

## Stage IV — Technology

| Item | Notes |
|------|-------|
| Phi Cell → reactor tiers | Multiblocks, failure modes |
| Mage towers | Regional Φ, beacons |
| Rune circuits / wiring | `technomagicPower()` path |
| Seal-automata | Automated wards |

---

## Stage V — TSE

| Item | Notes |
|------|-------|
| TSE sites / dungeons | Catalog + d100 generator as design input |
| Spatial magic risk | Long blink / portals ↔ TSE chance |
| ΦR / chronal hooks | Optional ambient effects |

TSE is a **gateway narrative** toward Ω (lore: TSE as Φ sink / Ω interface).

---

## Stage VI — Ω-space (3+1 complement)

Lore: conservation forbids annihilating the imaginary Ψ component \(ib\) in ordinary **(3+1)** spacetime; it drains into **Ω-space** (metric signature (++−−)) — the dissipative “Hell” of Effecoria.

| Item | Notes |
|------|-------|
| Ω dimension | Custom dimension; not a reskin of the Nether |
| Gates / leaks | Via TSE exits, reactor Ω-drain failures, high-entropy backlash |
| Inhabitants | Forms born from materialized fear/pain/exhaust (encyclopedia) |
| Gameplay | Entropy dumps, psych-acoustic hazards, late-game expeditions |

**Explicitly last** among major content pillars — needs solid magic, world, tech, and TSE first.

---

## Resource model

| Resource | Type | Player progression |
|----------|------|-------------------|
| **Φ** | Environmental flux | World + items (Phi Cell); location/time/materials |
| **Ψ** | Internal operator energy | Cap ↑ training; regen ↑ breathing × Φ |
| **Essence** | Unlock / craft currency | Drops, infusion, research |
| **b / entropy** | Cast side-product | Backlash; eventually Ω drain |

Breathing = how fast Ψ refills **in a given Φ field**.  
Training = how much Ψ you can store and channel.

---

## Config knobs (testing)

| Key | Default | Purpose |
|-----|---------|---------|
| `creative_god_mode` | `true` | Infinite Φ + free casts in creative |

See `docs/BALANCE.md` for the full list.
