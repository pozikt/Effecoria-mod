# Batch B — jewelry / charms (wrong-icon fix)

## Pipeline

1. **Concept** — [`sketches/batch_b_jewelry_concept_atlas.png`](sketches/batch_b_jewelry_concept_atlas.png)
2. **Vanilla refs** — [`vanilla_refs_batch_b/`](vanilla_refs_batch_b/) (gold/iron nuggets, amethyst, …)
3. **Bake** — `python art/items/bake_batch_b_jewelry.py`
4. **In-game** — unique `textures/item/*.png` + models point to `effecoria:item/<name>`
5. **Preview** — [`for_artist/batch_b_strip_8x.png`](for_artist/batch_b_strip_8x.png)

## Items

| Item | Was | Now |
|------|-----|-----|
| `jewelry_band` | gold_nugget | gold blank band |
| `assembled_ring` | gold_nugget | gold ring |
| `essonite_ring` | pure_essonite | gold ring + Φ gem |
| `jewelry_gem` | essonite_shard | cut gem |
| `assembled_charm` | lead_filter | hanging charm |
| `lead_charm` | lead_filter | dull lead charm |
| `assembled_amulet` | gold_amulet | gold pendant |
| `star_amulet` | star_essonite | star-core amulet |
| `phi_band` | phi_cell | cyan Φ bracelet |
| `faceted_focus` | resonance_focus | hex lens |
| `essentocyte_kit` | lead_filter | lead case + vial |
