# Essonite crystal art (Amethyst-style)

Vanilla Minecraft crystals are **not cubes**. They use:

- Texture: silhouette PNG (`amethyst_cluster.png`)
- Model: `minecraft:block/cross` (two crossed planes)

## Files to open in Aseprite

| File | Use |
|------|-----|
| **`essonite_crystal_edit.png`** | 16×16 edit canvas (copy of vanilla cluster) |
| **`essonite_crystal_edit_32.png`** | Same art ×2 for easier painting; downscale to 16 when exporting |
| `vanilla_amethyst_cluster.png` | Unchanged vanilla reference |
| `vanilla_*_amethyst_bud.png` | Growth stages reference |
| `models/vanilla_amethyst_cluster.json` | How the game places the texture |

## Aseprite tips

1. **Sprite → Color Mode → RGB Color**
2. Transparent background (not black fill — black was often opaque in old exports)
3. Keep the jagged “cluster” silhouette; recolor to Φ-blue / gold glow
4. Export final **16×16** as `essonite_crystal.png` for the mod (or keep 32×32 and ask to wire)

When art is ready, tell the agent to integrate — code will switch the block from cube to crystal model + cutout layer.
