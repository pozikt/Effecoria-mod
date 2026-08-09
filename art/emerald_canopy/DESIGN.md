# Emerald Canopy — block textures

## Pipeline (crop → scale, not recolor)

1. Concept sheet: [`concept_block_sheet.png`](concept_block_sheet.png) (6 panels).
2. Bake script crops each panel fragment, then **BOX-downscales to 16×16**:
   ```
   python scripts/bake_emerald_canopy_textures.py
   ```
3. Debug crops land in `crops_16/` (`panel_N_full.png` + final 16²).

## Panel → block

| Panel | Crop use |
|-------|----------|
| 0 top-left | `ancient_essence_wood.png` (bark side) |
| 1 top-mid | `ancient_essence_wood_top.png` (rings) |
| 2 top-right | `golden_bark.png` |
| 3 bottom-left | `golden_bark_top.png` |
| 4 bottom-mid | `phi_snare_vine.png` (checker → alpha, then scale) |
| 5 bottom-right | moss accent (crop only) |

Vanilla jar refs under `vanilla_refs/` are unused by this bake path (kept for optional compare).
