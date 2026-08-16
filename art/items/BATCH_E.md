# Batch E — fabricators, energy cores, artillery

## Pipeline

1. **Concept** — [`sketches/batch_e_fabricator_energy_artillery_concept.png`](sketches/batch_e_fabricator_energy_artillery_concept.png)
2. **Vanilla refs** — [`vanilla_refs_batch_e/`](vanilla_refs_batch_e/)
3. **Bake** — `python art/items/bake_batch_e_fabricator_energy_artillery.py`
4. **Preview** — [`for_artist/batch_e_strip_8x.png`](for_artist/batch_e_strip_8x.png)

## Fabricators I–III

Each tier has its **own concept** (not a shared atlas tile):

| Tier | Concept | Front identity |
|------|---------|----------------|
| I | [`sketches/phi_fabricator_i_concept.png`](sketches/phi_fabricator_i_concept.png) | scan bar + single crystal bay + Φ |
| II | [`sketches/phi_fabricator_ii_concept.png`](sketches/phi_fabricator_ii_concept.png) | dual crystals + teal mithril + center Φ |
| III | [`sketches/phi_fabricator_iii_concept.png`](sketches/phi_fabricator_iii_concept.png) | magenta Φ core + corner nodes + III plaque |

Bake: crop face → 64 BOX → 16 BOX + contrast; `_on` boosts emissives; side/top match tier accents.

## Reactor casing

**Kept** the previous plating design — Batch E bake no longer overwrites `reactor_casing`.

## Artillery

| Texture | Notes |
|---------|-------|
| `phi_artillery_base` | pedestal + yaw ring |
| `phi_beam_lens` / `_on` | tinted glass lens + glow |

Already decent (skipped): `phi_accumulator`, Geo Well, most Spark front/side, Star hull BER atlases.
