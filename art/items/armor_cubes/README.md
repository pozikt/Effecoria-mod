# Armor entity pipeline (cube → atlas → game)

## Cycle
1. Generate **cube faces** for one piece (body / extras as separate cubes). Minecraft = axis-aligned cuboids only.
2. Read sheet → decide **which atlases / UV islands** (vanilla armor layers vs extra cubes for protrusions).
3. Scale faces into those UVs → write game textures → user reviews in-game.

## Chestplate v1 — atlas analysis

Sheet: `art/items/armor_cubes/chest_cube_grid_v1.png` (preferred) / `chest_cube_faces_v1.png` (cross nets, reference).

### Cubes observed
| Cube | Role | Vanilla player part? |
|------|------|----------------------|
| **BODY** | torso plate | Yes → `HumanoidModel.body` on chestplate |
| **LEFT PAULDRON** | protruding shoulder | **No** — not on vanilla armor mesh |
| **RIGHT PAULDRON** | protruding shoulder | **No** — needs extra `ModelPart` cubes |

### Atlases required
1. **`textures/models/armor/phi_chitin_layer_1.png`** (**64×64**)  
   - Rows 0–31: vanilla body/arms/helmet/boots UV (BODY faces + cutouts).  
   - Rows 32+: ModelPart cube nets for pauldrons  
     - Right `texOffs(0,32)` size 5×4×6  
     - Left `texOffs(32,32)` size 5×4×6  

2. **Custom model** `PhiChitinChestArmorModel` — extra body children `left_pauldron` / `right_pauldron`.  
   Registered via `ClientArmorExtensions` (`IClientItemExtensions` on the chestplate item).

## Status
- Grid sheet: `chest_cube_grid_v1.png`
- Face crops: `chest_faces_v1/`
- Game texture: `assets/effecoria/textures/models/armor/phi_chitin_layer_1.png` (64×64, pauldrons included)
- Cutouts: neck scoop + conical waist hem
- Pack: `scripts/pack_chest_cube_armor.py`
- Preview: `for_artist/phi_chitin_layer_1_chest_cube_v1_4x.png`
