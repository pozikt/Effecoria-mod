# Roadmap — Effecoria Mod

Prioritizes **perfecting magic & energy** first, then world layers, technology, and finally cosmology (TSE → Ω).

Lore reference: [Effecoria encyclopedia](https://github.com/pozikt/Effecoria).

Monetization / release funnel (free core, support, server, DLC): [docs/monetization/](monetization/README.md). Stage I “recommend to a friend” gate: [STAGE_I_FUNNEL.md](monetization/STAGE_I_FUNNEL.md).

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

### Magic expands with each stage (policy)

Stage I ships a **complete cast loop** for all schools. Later stages do **not** freeze magic — each content pillar **widens the school that fits that pillar**. Direction of the update → direction of the spell/system expansion:

| When we ship… | School / system that grows | Why |
|---------------|----------------------------|-----|
| **II — Environment** | **Elemental** | Weather, biomes, flora/fauna, surface Φ anomalies give fire/ice/storm/steam real anchors |
| **III — Caves & ores** | *(as needed)* Material-tied workings, deep-Φ / low-light school hooks | New ores and underground Φ zones |
| **IV — Technology** | **Seals** | Reactors, towers, rune wiring, seal-automata — programmable Φ on blocks |
| **Items + accessory slot library** (Curios / Trinkets / equivalent) | **Corruption (curses)** | Wearable curse marks, blight brands, prey/bind on jewelry & relics — slots make permanent/semi-permanent curses readable |
| **V — TSE / VI — Ω** | Spatial + necromancy endgame (and cross-school cosmology) | Singularities, other-side geography |

Rule of thumb: **do not invent a huge school rewrite in Stage I “just because.”** Park deep seal automation until tech, deep curse equipment until accessory slots exist, and big elemental identity until the environment can feed it.

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
| **Seals** | Word pack: `vigil` (remote alarm) + `haustus` (standing Ψ siphon); conflicts/stacking done |
| **Anti-magic** | Lead (ZNΦ), cold iron — tag-based, replaces stone-box hack |
| **School depth** | Identity packs: Corruption, Spatial, Mental, Necromancy, Organic parasite |
| **Player-facing magic** | Teach the loop without wiki: Patchouli / in-game tips / hub clarity |

Optional inside I (only if it serves magic feel): initiation ritual block.

### Necromancy — Death Mark (shipped baseline)

Raise via Death Mark (not free summons). Thralls leash + LOS-only combat; gear preserved; no thrall loot farm. **Control budget** scales with breathing mastery (count + max single HP + total HP).

### Endgame hook (deferred) — Ender Dragon thrall

**Not Stage I implementation.** Design note for late necro / post–End exploration:

| Gate (fantasy) | Players already in netherite, End mapped for elytra, dragon beaten several times; mastery/control budget at true endgame |
| Behavior | While unbound to the rider: dragon **lives its own life** in the End (free roam AI — not leash thrall rules) |
| Call | On summoner signal: flies to the necromancer |
| Mount | Allows the summoner to **sit and steer** as a rideable mount |
| Balance | Unique exception to normal thrall leash/LOS; one dragon max; heavy mastery + story gates |

Track under Stage I polish only as **docs**; code when magic presentation + control budget feel final.

---

## Stage II — Environment

Surface world as Φ ecology.

| Item | Notes |
|------|-------|
| Biomes / biome modifiers | Φ density, day-night character |
| Flora | Φ-sensitive plants, consumables (mandragora analogs) |
| Fauna | Creatures tied to schools / Φ bands |
| Surface anomalies | Mild Φ storms, ZNΦ patches (content, not just anti-cast) |
| **Material sinks (early)** | Dust → emergency Ψ + Φ-fertilizer; Φ-glass / Φ-planks + fuel; full chitin armor; vitrified glass tools (sword/pick/axe/shovel); golem core → full Phi Cell |
| **Magic: Elemental expansion** | New spells / behaviors tied to weather, biomes, and surface Φ — school grows *because* the world can host it |

Races / Orkanum baselines fit naturally here (biologyQ hooks already exist).

---

## Stage III — Caves & ores

| Item | Notes |
|------|-------|
| Essonite & new ores | Real worldgen veins |
| Cave biomes / deep Φ | Low light, high entropy pockets |
| Underground structures | Small mage ruins, sealed chambers |
| **Magic (light touch)** | Material / deep-Φ hooks as ores unlock (not a full school rewrite) |

Materials that were “craft only” in Stage I get proper sources here.

---

## Stage IV — Technology

| Item | Notes |
|------|-------|
| Phi Cell → reactor tiers | Multiblocks, failure modes |
| Mage towers | Regional Φ, beacons |
| Rune circuits / wiring | `technomagicPower()` path |
| Seal-automata | Automated wards |
| **Magic: Seals expansion** | Deeper word grammar, networks, automation — seals scale with tech, not only with Stage I polish |

### Parallel track — Items & accessories → Corruption

When (or after) we add meaningful **items / relics** and integrate an **accessory-slot library** (Curios, Trinkets, or NeoForge equivalent):

| Item | Notes |
|------|-------|
| Accessory slots | Ring / amulet / charm slots for Effecoria gear |
| **Magic: Corruption (curses) expansion** | Curse marks, brands, prey/bind as wearable or slot-bound effects; readable persistence without stuffing the hotbar |

Corruption combat spells can still grow a little in Stage I; **equipment-scale curses** wait on slots + item identity.

---

## Stage V — TSE

| Item | Notes |
|------|-------|
| TSE sites / dungeons | Catalog + d100 generator as design input |
| Spatial magic risk | Long blink / portals ↔ TSE chance |
| Subspace hazards | Chaos Reefs, Ψ-ghosts, Ω-pockets — see [SUBSPACE.md](SUBSPACE.md) |
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
