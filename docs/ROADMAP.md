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

Do not treat Stage I polish as a hard gate anymore: **Stage II (environment + materials) is active** while the magic loop stays playable. Deep school rewrites still wait on the pillars below.

### Magic expands with each stage (policy)

Stage I ships a **complete cast loop** for all schools. Later stages do **not** freeze magic — each content pillar **widens the school that fits that pillar**. Direction of the update → direction of the spell/system expansion:

| When we ship… | School / system that grows | Why |
|---------------|----------------------------|-----|
| **II — Environment** | **Elemental** | Weather, biomes, flora/fauna, surface Φ anomalies give fire/ice/storm/steam real anchors |
| **III — Caves & ores** | *(as needed)* Material-tied workings, deep-Φ / low-light school hooks | New ores and underground Φ zones |
| **IV — Technology** | **Seals** | Reactors, towers, rune wiring, seal-automata — programmable Φ on blocks |
| **Items + accessory slot library** (Curios) | **Corruption (curses)** | Wearable curse marks — slots exist; curse gear still Stage IV+ |
| **V — TSE / VI — Ω** | Spatial + necromancy endgame (and cross-school cosmology) | Singularities, other-side geography |

Rule of thumb: **do not invent a huge school rewrite in Stage I “just because.”** Park deep seal automation until tech, deep curse equipment until accessory slots exist, and big elemental identity until the environment can feed it.

---

## Stage I — Magic & energy (shipped loop)

**Goal:** Full player magic loop — school → progression → cast → entropy. **Loop is playable;** remaining rows are polish, not a blocker for Stage II.

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
| **Φ harness** | Essonite dust → focus / **Phi Cell** — craft loop shipped; world sources expand under Stage II materials |
| **Breathing** | Air/hunger coupling; optional trainer/modes |
| **Training** | Broader XP sources; diminishing returns |
| **Seals** | Word pack: `vigil` (remote alarm) + `haustus` (standing Ψ siphon); conflicts/stacking done |
| **Anti-magic** | Lead (ZNΦ), cold iron — tag-based; deeper world presence with II/III |
| **School depth** | Identity packs: Corruption, Spatial, Mental, Necromancy, Organic parasite |
| **Player-facing magic** | Primer + tips + hub shipped. **Planned:** Consciousness Matrix (graph of knowns, blurred unknowns, puzzle paths) — [MAGIC_PLAN.md](MAGIC_PLAN.md#consciousness-matrix-planned--replaces-magic-primer) |

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

## Stage II — Environment (**current**)

Surface world as Φ ecology. **Also owns early material sinks** (README phases **3 + 7** run together here).

### In progress / shipping now

| Item | Notes |
|------|-------|
| Biomes / biome modifiers | Vitrified Wastes, Essence Plateau (and related Φ biomes) |
| Flora | Φ logs/leaves/saplings, blades, plateau & wastes plants |
| Structures / encounters | Golem & biome structures; more ruins as content settles |
| **Materials (early sinks)** | Essonite dust/shards/pure → focus; Φ-chitin set (item icons + chest pauldrons on player); vitrified glass tools; alchemy (mortar, burner, alembic, potions); Φ-water filtration |
| Texture / armor pipeline | Cube-face → atlas → entity layers (`art/items/armor_cubes/`) |

### Still planned in II

| Item | Notes |
|------|-------|
| Fauna | Creatures tied to schools / Φ bands (beyond current mobs) |
| Surface anomalies | Mild Φ storms, **ZNΦ mute patches shipped** (`znphi_crust` disks on Overworld surface; Dead Wasteland still biome-scale mute) |
| **Magic: Elemental expansion** | Environment matter bond Wave 2 shipped (water/ice + lava + sand/ash); further combos later — see [MAGIC_PLAN.md § Environmental matter casting](MAGIC_PLAN.md#environmental-matter-casting-future--elemental-pro) |

Races / Orkanum baselines — shipped MVP (see [docs/RACES.md](RACES.md)).

---

## Stage III — Caves & ores

| Item | Notes |
|------|-------|
| Essonite / lead / mithril veins | **Shipped** — lead + essonite: Overworld (essonite also Nether); mithril: rich on Essence Plateau + sparse Overworld deep veins |
| Cave biomes / deep Φ | **Shipped MVP** — `deep_phi_pocket` (replaces dripstone caves in a TerraBlender slice): low light, entropy drip, deep-Φ ambient + sparse essonite crystal decoration |
| Underground structures | Small mage ruins, sealed chambers — still planned |
| **Magic (light touch)** | Material / deep-Φ hooks as ores unlock (not a full school rewrite) |

Materials that were “craft only” in Stage I get proper sources here.

---

## Stage IV — Technology

| Item | Notes |
|------|-------|
| **Technomagic catalog + Era I–III** | **In progress / shipped scaffold** — see [TECHNOMAGIC.md](TECHNOMAGIC.md): free craft, discovery UI, Era I–II machines, Era III imprinter/construct/telegraph |
| **Φ-fabricator I–III** | **Shipped** — datapack recipes + memory-crystal scan; Class IV later — [TECHNOMAGIC.md](TECHNOMAGIC.md#era-ivvi-φ-fabricator-classes-iiii) |
| **Star Reactor + Φ-artillery** | **Shipped MVP** — 5×5×5 Star hub + manual thermal beam; other beam modes later — [TECHNOMAGIC.md](TECHNOMAGIC.md#era-vi-star-reactor-5x5x5) |
| Phi Cell → reactor tiers | Multiblocks, failure modes (Era III–V catalog `planned`) |
| Mage towers | Regional Φ, beacons; **Soulbound Conclave (multi-owner)** — [planned](SOULBOUND_CONCLAVE.md); **Lex Loci** tower edicts + Φ-channels — [planned](LEX_LOCI.md) / [PHI_FLOW_LAWS.md](PHI_FLOW_LAWS.md) |
| Rune circuits / wiring | `technomagicPower()` path → Essential Flow Solver (frequency channels, ΔQ, I_Ω) |
| Seal-automata | Automated wards; turret autonomy when Ψ-computer busy |
| **Magic: Seals expansion** | Deeper word grammar, networks, automation — seals scale with tech, not only with Stage I polish |

### Parallel track — Items & accessories → Corruption

When (or after) we add meaningful **items / relics** and integrate an **accessory-slot library** (Curios, Trinkets, or NeoForge equivalent):

| Item | Notes |
|------|-------|
| Accessory slots | **Shipped (Curios)** — ring×2 / amulet / charm; see [ARTIFACT_CRAFT.md](ARTIFACT_CRAFT.md) |
| Modular artifacts | **Shipped (Era III)** — lathe / cutter / assembler / item seals. **Reform planned** — weapon families (melee tips, Φ-guns, composited textures); freeform artifacts later — [ARTIFACT_CRAFT.md](ARTIFACT_CRAFT.md#reform--weapon-families-planned) |
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
| `creative_god_mode` | `true` | Infinite Φ + free casts in creative (ZNΦ mute still applies) |

See `docs/BALANCE.md` for the full list.
