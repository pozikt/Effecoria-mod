# Φ-alchemy art refs — mortar & cauldron

## Workflow (artist + agent)

1. Agent ships **placeholders** in the mod (function + rough model/UV).
2. Agent generates **sketches** under [`sketches/`](sketches/) for cutting into textures.
3. Artist paints final pixels and drops them into `src/main/resources/assets/effecoria/textures/...`.
4. For desk-scale props (mortar, flasks on table scale): **agree footprint/template first** (mortar ≈ lantern).

## Sketches (paint / cut from these)

| File | Use |
|------|-----|
| [`sketches/mortar_pestle_turnaround.png`](sketches/mortar_pestle_turnaround.png) | Front / side / top orthographic |
| [`sketches/mortar_pestle_item_icon.png`](sketches/mortar_pestle_item_icon.png) | Item icon silhouette |
| [`sketches/mortar_atlas_sketch.png`](sketches/mortar_atlas_sketch.png) | UV island layout (edit this) |

**Pack into game:** `python scripts/pack_mortar_atlas.py`  
Reads the edited sketch → writes:

- `textures/block/mortar_side.png`
- `textures/block/mortar_inner.png`
- `textures/block/mortar_rim.png`
- `textures/block/mortar_bottom.png`
- `textures/block/mortar_pestle.png`
- `textures/item/mortar_and_pestle.png`
- hollow multi-texture block model

Preview of last pack: [`mortar_template/faces/packed_preview_8x.png`](mortar_template/faces/packed_preview_8x.png)

Model UV target: [`mortar_template/`](mortar_template/) + `TEXTURE_UV.md`.

## Vanilla cauldron (how hollowness works)

Folder: [`vanilla_refs/textures/block/`](vanilla_refs/textures/block/)

| File | Role |
|------|------|
| `cauldron_side.png` | Outer walls |
| `cauldron_top.png` | Rim (top face of walls) |
| `cauldron_bottom.png` | Feet / underside |
| `cauldron_inner.png` | **Inside** floor + inward-facing wall bottoms |

Vanilla does **not** use one solid cube. Model [`vanilla_refs/models/block/cauldron.json`](vanilla_refs/models/block/cauldron.json):

1. Four thin wall boxes (thickness 2 px in block space)
2. One floor slab at y=3→4 with `#inside`
3. Feet cubes at the corners

Looking down, you see through the open top into `#inside`. That is the hollow look.

Also extracted: `textures/item/cauldron.png`, lantern block/item textures, `models/block/template_lantern.json`.

## Lantern size (target for mortar)

From `template_lantern.json` body:

- Footprint: **[5,0,5] → [11,7,11]** → **6×6** on XZ, height **7**
- Cap: **[6,7,6] → [10,9,10]**

Mortar template uses the same footprint (± rim), height ~6–7, **hollow bowl** (cauldron technique, lantern scale).

## Mortar template (paint here)

| Path | What |
|------|------|
| [`mortar_template/mortar_atlas_template.png`](mortar_template/mortar_atlas_template.png) | 16×16 atlas with marked UV regions |
| [`mortar_template/mortar_and_pestle.json`](mortar_template/mortar_and_pestle.json) | Hollow block model (lantern-sized) |
| [`mortar_template/TEXTURE_UV.md`](mortar_template/TEXTURE_UV.md) | UV map for the atlas |
| [`mortar_and_pestle_current.png`](mortar_and_pestle_current.png) | Current in-game flat cube texture (placeholder) |

When the atlas is painted, copy it to:

`src/main/resources/assets/effecoria/textures/block/mortar_and_pestle.png`

and keep the game model as the hollow JSON (already pointed at that texture).
