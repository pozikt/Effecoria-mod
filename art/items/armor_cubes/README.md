# Armor entity pipeline (cube → atlas → game)

Canonical reference for Φ-chitin (and future sets). Agent rule: `.cursor/rules/phi-chitin-armor-pipeline.mdc`.

## Cycle
1. Generate **cube faces** for one piece (body / extras as separate cubes). Minecraft = axis-aligned cuboids only.
2. Read sheet → decide **which atlases / UV islands** (vanilla armor layers vs extra cubes for protrusions).
3. Scale faces into those UVs → write game textures → **user reviews in-game**.
4. Polish accents (cyan eyes, gold from item icon) only after silhouette is approved.

## Lessons learned (do not regress)
- **Protrusions** (pauldrons, etc.) are not on the vanilla armor mesh → extra `ModelPart` cubes + UV in the lower half of a **64×64** `layer_1`.
- **`layer_1` at 64×64** requires custom armor models with `texSize(64,64)` for **helmet, chest, and boots**. Default 64×32 sampling maps boot UV into the pauldron band → looks like a waist plate.
- **Leggings** stay on `layer_2` (64×32). Iron paints only a short **belt** strip (`y≈27..31`) and leg sides with **open ankles** for boots — stamp the *top* of waist art into the belt, and *lower* boot art into `y=26..31` on `layer_1`.
- Helmet: do **not** apply iron’s full face opening to Front (deletes cheeks). Thin eyes; gold can be painted from the item icon if the cube grid has none.
- Pack scripts must **preserve** other islands on shared atlases (helmet/boots edit `layer_1` without wiping chest/pauldrons).

## Layers overview
| Slot | Atlas | Model |
|------|--------|--------|
| Helmet | `layer_1` head UV | `PhiChitinHelmetArmorModel` 64×64 |
| Chestplate | `layer_1` body + pauldron nets y≥32 | `PhiChitinChestArmorModel` 64×64 |
| Leggings | `layer_2` leg + belt | default (64×32 OK) |
| Boots | `layer_1` foot UV | `PhiChitinBootsArmorModel` 64×64 |

Client registration: `ClientArmorExtensions` (`IClientItemExtensions` + layer definitions).

## Pack scripts
| Piece | Script | Grid |
|-------|--------|------|
| Chest | `scripts/pack_chest_cube_armor.py` | `chest_cube_grid_v1.png` |
| Helmet | `scripts/pack_helmet_cube_armor.py` | `helmet_cube_grid_v1.png` |
| Leggings | `scripts/pack_leggings_cube_armor.py` | `leggings_cube_grid_v1.png` |
| Boots | `scripts/pack_boots_cube_armor.py` | `boots_cube_grid_v1.png` |

Crops live under `art/items/armor_cubes/*_faces_v1/`. Previews under `art/items/for_artist/`.

---

## Chestplate v1

Sheet: `chest_cube_grid_v1.png` (preferred) / `chest_cube_faces_v1.png` (cross nets, reference).

| Cube | Role | Vanilla part? |
|------|------|----------------|
| **BODY** | torso plate | Yes → body |
| **LEFT/RIGHT PAULDRON** | shoulder | **No** → extra cubes `texOffs(0,32)` / `(32,32)` size 5×4×6 |

Cutouts: neck scoop + conical waist hem. Preview: `for_artist/phi_chitin_layer_1_chest_cube_v1_4x.png`.

## Helmet v1

| Cube | Role |
|------|------|
| **HEAD** | shell on head UV 8×8 faces |

Cutouts: cheeks kept; thin cyan eyes; gold from item icon v2. Preview: `phi_chitin_helmet_uv_v1_8x.png`.

## Leggings v1

2×6 grid: **RIGHT LEG** + **BODY/WAIST**. Left leg mirrors.

- Leg: Top `(4,16) 4×4`; sides 4×12; bottom clear for boots
- Waist: mask to iron belt rows; use top of BODY face art

Preview: `phi_chitin_leggings_uv_v1_8x.png`.

## Boots v1

1×6 **RIGHT FOOT** grid.

- Sole `(8,16) 4×4`; sides only `y=26..31` (lower ankle/toe crop)
- Must use `PhiChitinBootsArmorModel` (64×64)

Preview: `phi_chitin_boots_uv_v1_10x.png`.
