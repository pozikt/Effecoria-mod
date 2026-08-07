---
name: effecoria-entity-models
description: >-
  Creates Effecoria mob models and entity textures from lore/bestiary through
  concept turnarounds, vanilla template choice, and in-game UV atlases (GeckoLib
  or Java). Explains alpha cutouts and hole artifacts. Use when the user asks
  for entity/mob models, GeckoLib geo, entity textures, bestiary creatures,
  vitrified golem-style pipelines, or fixing transparent holes in mob skins.
---

# Effecoria entity model pipeline

## Pipeline (author workflow)

1. Take a creature from the **bestiary / lore** (project docs). If there is a description, generate the creature from **strict orthographic views** (all sides and standard angles). If there is **no** description, **ask the user** before generating.
2. After generating concept art, **read the images** and decide which **vanilla mob UV/layout templates** can be used as a base. If the creature is unusual, design **custom box layouts** (still document UV like vanilla).
3. **Downscale and bake** into game textures: 1 block unit = 1 texel per face on that cube (GeckoLib/Effecoria convention). Use nearest-neighbor. Run pack scripts when they exist.

Always prefer **recolor/adapt vanilla silhouettes** for items/blocks; for entities, prefer **vanilla humanoid/quadruped UV math** before inventing new atlases.

## Step 0 — Lore lookup (before art)

Search in order:

| Source | Path |
|--------|------|
| Lore notes | `docs/lore/*.md` |
| Biome / feature docs | `docs/VITRIFIED_WASTES.md`, `docs/WHISPERING_SPIRE.md`, `docs/STAGE_*.md` |
| In-game strings | `assets/effecoria/lang/en_us.json`, `ru_ru.json` (hints, spawn egg names) |

Extract: silhouette, materials (glass, chitin, bone), size class, limbs, glow parts, hostility, signature weapon.

**No usable description?** Ask the user (size, materials, mood, 1–2 signature features). Do not guess a full bestiary entry.

## Step 1 — Strict concept sheet (GenerateImage)

Generate a **single reference sheet** or a small set with **fixed camera rules**:

- **Required:** front, back, left, right (orthographic, same scale, same stance).
- **Recommended:** top (plan), 3/4 front-left, detail callout (face / weapon / core).
- **Background:** flat neutral or transparent; **no** busy scenery.
- **Style:** pixel-art readable at 64–128 px tall in-game; state palette (base + 1 emissive accent).

Save under `art/<feature>/<creature>/` with names like `concept_turnaround.png`, `concept_sheet.png`.

After generation, **open/read the image** and write a 5–10 line **design lock** (proportions, what is opaque vs cutout, emissive zones).

## Step 2 — Pick vanilla templates

See [vanilla-templates.md](vanilla-templates.md). Match **topology**, not theme:

- Biped humanoid → iron golem / zombie / piglin UV habits
- Biped + weapon cube → iron golem + extra cubes (Effecoria: vitrified golem)
- Quadruped → wolf / cow / spider segments
- Flying small → allay / bee / phantom
- Amorphous / boss → warden / guardian custom atlases

Record choice in `art/<creature>/DESIGN.md`: template mob, atlas size (64 vs 128), GeckoLib vs Java model.

## Step 3 — Implementation path in this repo

| Path | When | Output |
|------|------|--------|
| **GeckoLib** (preferred for animated bosses) | Multi-bone attacks, custom anims | `geo/*.geo.json`, `animations/*.animation.json`, `textures/entity/*.png` |
| **Java `Model` + `LayerDefinition`** | Simple mobs, one texture | `client/model/*Model.java`, `textures/entity/*.png` |

**Existing gold standard:** vitrified golem — `art/vitrified_wastes/golem/`, `scripts/pack_vitrified_golem_texture.py`, `VitrifiedGolemRenderer` uses `RenderType.entityTranslucent`.

Workflow for new GeckoLib mob:

1. Blockout `*.geo.json` (bones + cube sizes). Sync `description.texture_width/height` with atlas.
2. `--init` face templates → paint `faces/*` OR paint `nets/*` → `--from-nets`.
3. `python scripts/pack_vitrified_golem_texture.py` (copy/adapt script for new `PARTS` map).
4. Register entity, `GeoEntityRenderer`, animation names in code.

## Step 4 — Alpha and “holes” (mandatory read)

Before marking a mob done, apply [reference.md — Transparency and holes](reference.md#transparency-and-holes).

Summary:

- **Holes in the mesh** ≠ **holes in the texture**. Translucent render + accidental alpha on “solid” limbs shows **empty interior** and **back faces** → looks pierced.
- **Every texel on a cube face** that should look solid must be **alpha 255**. Reserve alpha 0 for intentional cutouts only (blade edges, cracks), and flood from edges in pack script.
- **Never** leave UV rectangles empty in the atlas (alpha 0) for cubes that still exist in geo — game samples “nothing” there.
- Semi-transparent pixels (1–127 alpha) → **binarize** to 0 or 255 for Minecraft entities.

When user reports “дырки”, inspect: renderer `RenderType`, atlas paste gaps, `clean_face_alpha` silhouette flags, geo cube sizes vs UV sizes.

## Step 5 — Validation checklist

```
- [ ] Lore/design lock written
- [ ] Concept sheet matches locked proportions
- [ ] Template + atlas size documented
- [ ] geo UV matches pack script PARTS (or documented map)
- [ ] pack script run; no MISSING faces in log
- [ ] In-game: no random holes at rest pose; cutouts only where designed
- [ ] Animations don’t pull cubes apart exposing unstitched UV
```

## Utility commands

```bash
# Golem (adapt PARTS for other mobs)
python scripts/pack_vitrified_golem_texture.py --init
python scripts/pack_vitrified_golem_texture.py --from-nets
python scripts/pack_vitrified_golem_texture.py
```

Extract vanilla entity UV reference from client jar when needed:

`assets/minecraft/textures/entity/<mob>.png` → compare in `art/reference/vanilla/`.

## Additional resources

- [reference.md](reference.md) — UV layout, alpha rules, file paths
- [vanilla-templates.md](vanilla-templates.md) — mob → template mapping
- `art/vitrified_wastes/golem/TEXTURE_EDIT.md` — face naming (RU)
