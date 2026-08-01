# Subspace / hyperspace — future content notes

Living doc for `effecoria:subspace` after the voyage MVP (`subspace_voyage`, 1∶100 scale).

## Portal rules (shipped)

- Entry/exit portals are a **two-block-tall puncture** (void oval + ragged rim), oriented to the caster's facing.
- Opening a gate still requires the Spatial spell `subspace_voyage`.
- Entry portals stay open while passengers travel; **when the Spatial host walks through an exit, the whole gate network closes** and remaining travelers are returned to the origin.
- Multiple Spatial mages may run **independent** sessions at once; a passenger Spatial mage can place an *additional* exit without deleting the host’s (until the host exits).
- Party members of one session share the same hyperspace rendezvous anchor.

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

Scaffolding: `SubspaceMatterService` (exile classification + physical dump yard next to `subspaceAnchor(host)` / active voyage entry). Full simulation of reefs / ghosts / spit-back comes later.

### Organic (limbs, grass, trees)

1. **Φ-conservation** (hours–days) — sterilized, no rot; looks fresh/green.
2. **Essentialization** (months–years) — tissue → essento-ceramic (glass-gold-blue), veins/bones visible.
3. **Φ-diffusion** (centuries+) — structure dissolves into Φ background (Φ-echo glow).

### Inorganic

- **Φ-conductors** (silver, mythril…): stable forever.
- **Φ-insulators** (gold, lead…): Φ-bubble → eventual spit-back to realspace (“falling gold”).
- **Iron/steel**: essentialize ~100× slower than organics.
- **Silicates**: low-grade essonite over millennia → mineable “star essonite” on reefs (deadly trade).
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
