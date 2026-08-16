# Batch C — flat / low-detail blocks

## Pipeline

1. **Concept** — [`sketches/batch_c_blocks_concept_atlas.png`](sketches/batch_c_blocks_concept_atlas.png)
2. **Vanilla refs** — [`vanilla_refs_batch_c/`](vanilla_refs_batch_c/) (observer, crafter, blast furnace, lodestone, iron)
3. **Bake** — `python art/items/bake_batch_c_blocks.py`
4. **Preview** — [`for_artist/batch_c_strip_8x.png`](for_artist/batch_c_strip_8x.png)

## Replaced

| Block | Base |
|-------|------|
| `skiff_control_panel` | crafter_north + cyan HUD |
| `essence_lift_core` | blast_furnace_front + Φ column |
| `essence_thruster` | blast_furnace_side + violet nozzle |
| `tower_anchor` | lodestone_side + seal |
| `phi_bus` / `phi_bus_on` | procedural conduit stripe |
| `phi_coupler*` | iron_block + channel accent |
| `phi_matcher` / `_on` | observer_front + rings |
| `phi_beacon` | own texture (was `portal_gate_top`) |

Deferred: `star_reactor_*`, `phi_cartography_table_bottom` (Batch D).
