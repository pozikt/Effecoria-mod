# Vitrified glass tool icons

Canonical agent rule: `.cursor/rules/vitrified-tools-pipeline.mdc`.

## Cycle
1. Generate a **solid** orthographic item sketch (vanilla diagonal camera).
2. Mask to **iron** tool silhouette — alpha must match vanilla exactly.
3. Pack 16×16 → game + `for_artist` preview → user reviews in-game.
4. Next tool.

## No chip-off parts
- Head = fused black/cyan glass inside the iron outline only.
- No jagged shard tips, 1px teeth, floating flecks, or extra cubes outside the mask.
- Stick = vanilla wood pixels from the iron template.

## Set
| Item | Sketch asset | Iron mask |
|------|----------------|-----------|
| Sword | `item_vitrified_sword_solid_v1.png` | `iron_sword.png` |
| Pickaxe | `item_vitrified_pickaxe_solid_v1.png` | `iron_pickaxe.png` |
| Axe | `item_vitrified_axe_solid_v1.png` | `iron_axe.png` |
| Shovel | `item_vitrified_shovel_solid_v1.png` | `iron_shovel.png` |

Pack: `python scripts/pack_vitrified_tools.py`  
Staging: `art/items/tool_icons/`  
Game: `assets/effecoria/textures/item/vitrified_glass_*.png`
