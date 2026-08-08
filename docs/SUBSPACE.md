# Subspace / hyperspace — future content notes

Living doc for `effecoria:subspace` after the voyage MVP (`subspace_voyage`, 1∶100 scale).

## Portal rules (shipped)

- Entry/exit portals are a **two-block-tall puncture** (void oval + ragged rim), oriented to the caster's facing.
- Opening a gate still requires the Spatial spell `subspace_voyage`.
- When an overworld **ENTRY** opens, a twin **EXIT** is placed at a **session-unique** hyperspace yard (`subspaceAnchor(sessionId)`) and linked back to that entry so arrivals always see a return gate. A new voyage never reuses the previous yard.
- A second cast in hyperspace opens an optional **far EXIT** (1∶100 map) without removing the yard twin.
- Entry portals stay open while passengers travel; **when the Spatial host walks through an exit, the whole gate network closes** and remaining travelers are returned to the origin.
- Multiple Spatial mages may run **independent** sessions at once; a passenger Spatial mage can place an *additional* exit without deleting the host’s (until the host exits).
- Party members of one session share the same hyperspace rendezvous (the host's session yard).

## Floor / Φ-vision (shipped)

- Hyperspace stands on translucent **`phi_veil`** (not End stone). Leftover End stone under landings is migrated on entry.
- Client Φ-landscape: ultramarine fog ocean, star-hills, planet motes, black-hole spirals, TSE knots, Ω glitch snow (`SubspacePhiLandscapeClient`).

## Must-add later: cosmological anomalies & hazards

Hyperspace is **not** an empty corridor. Add navigation and survival threats:

| Phenomenon | Intent |
|------------|--------|
| **Chaos Reefs** (Φ-junk islands) | Conglomerates of glassed bones, swords, petrified trees; drift on Φ-currents; collision = death |
| **Ψ-limb ghosts** | Severed limbs keep a Ψ imprint → short pain/fear projections; mental hit for voyagers |
| **Ω-matter pockets** | Contaminated dumps open micro-tears; causality glitches around them |
| Broader **cosmological anomalies** | Φ-storms, current shears, false horizons, TSE-adjacent singularities |

Tie spatial risk (long blink / portals / voyage distance) into Stage V TSE when ready ([ROADMAP.md](ROADMAP.md)).

---

## Matter fate when left in the Φ-sublayer

Scaffolding: `SubspaceMatterService` (exile classification + physical dump yard) + **`SubspaceEssentializationService`** (slow block convert in hyperspace, persisted ages).

### Shipped essentialization (playable rates)

Recast/exile dumps and nearby watched cells age in `effecoria:subspace`. Defaults (config `subspace_*_ticks`):

| Source | Becomes | Default age |
|--------|---------|-------------|
| Dirt / grass / soft organics | `phi_dirt` / `phi_grass` / `phi_blades` | ~2 min |
| Logs / planks / leaves | `phi_log` / `phi_planks` / `phi_leaves` | ~4 min |
| Stone family | `phi_stone` → `essonite_ore` | ~10 min → ~20 min |
| Sand / quartz | `essonite_crust` → ore | ~5 min → ore stage |
| Glass | `phi_glass` | ~5 min |
| Water / ice | `phi_water` → blue ice (Φ-hydrate stand-in) | ~3 min → ×2 |
| Obsidian | `void_obsidian` | ~7.5 min |
| Amethyst buds/cluster | essonite buds/crystal | organic-ish |
| Gold | spit back to overworld (“falling gold”) | ~5 min |
| Iron | stable for now (no Φ-iron block yet) | — |
| Essonite family | slow bud growth / ore→block→star | chance pulse |

Tune via `BalanceConfig` / `effecoria-server.toml`. Lore timescales (months–millennia) remain the fiction; config compresses them.

**Speed control:** `randomTickSpeed` does **not** affect this (aging uses game time, not block random ticks). Use:

- `/gamerule subspaceEssentializeSpeed <n>` — `0` pause, `1` default, `100` ≈ 100× faster
- `/effecoria subspaceSpeed [n]` — same rule (op)

### Organic (limbs, grass, trees) — lore stages

1. **Φ-conservation** (hours–days) — sterilized, no rot; looks fresh/green.
2. **Essentialization** (months–years) — tissue → essento-ceramic (glass-gold-blue), veins/bones visible.
3. **Φ-diffusion** (centuries+) — structure dissolves into Φ background (Φ-echo glow).

### Inorganic

- **Φ-conductors** (silver, mythril…): stable forever.
- **Φ-insulators** (gold, lead…): Φ-bubble → eventual spit-back to realspace (“falling gold”) — **gold spit shipped**.
- **Iron/steel**: essentialize ~100× slower than organics (no dedicated Φ-iron block yet).
- **Silicates**: low-grade essonite over millennia → mineable “star essonite” on reefs (deadly trade) — **ore / star path started**.
- **Artifacts with Φ-phonemes**: mutated / dangerous variants.

### Culture / economy (later hooks)

- Falling-gold weather events
- Chaos-reef mining
- Burial rites that intentionally exile bodies as “eternal stars”

---

## Shipped related spells

| Spell | Role |
|-------|------|
| `subspace_voyage` | Travel corridor |
| `rift_excise` | Exile a small realspace volume into hyperspace (physical dump beside the host voyage landing; queue for future reefs) |
