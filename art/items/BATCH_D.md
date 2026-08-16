# Batch D — Star Reactor faces, cartography bottom, medical pack

## Pipeline

1. **Concept** — [`sketches/batch_d_star_medical_concept_atlas.png`](sketches/batch_d_star_medical_concept_atlas.png)
2. **Vanilla refs** — [`vanilla_refs_batch_d/`](vanilla_refs_batch_d/)
3. **Bake** — `python art/items/bake_batch_d_star_medical.py`
4. **Preview** — [`for_artist/batch_d_strip_8x.png`](for_artist/batch_d_strip_8x.png)

## Blocks

| Texture | Base |
|---------|------|
| `star_reactor_side` / `_on` | respawn_anchor + Φ conduits |
| `star_reactor_top` | beacon + star core |
| `phi_cartography_table_bottom` | cartography side3 → wood + Φ grid |

## Medical items (were borrowed potion/salve/flask icons)

| Item | Liquid |
|------|--------|
| `anti_phi_serum` | pale cyan, lead cork |
| `lung_rinse` | blue |
| `orkanumn_stimulant` | hot orange |
| `potion_omega_cleanse` | purple Ω |
| `omega_amputation_salve` | lavender cream |
| `essence_dew` | green-cyan + sparkles |

Models now point to `effecoria:item/<name>`.
