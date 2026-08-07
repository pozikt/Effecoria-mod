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
2. After generating (or receiving) concept art, **read the images** — then run **Step 1b segment bake** before inventing boxes.
3. Decide which **vanilla mob UV/layout templates** can be used as a base. If unusual, custom box layouts (still document UV like vanilla).
4. **Downscale and bake** into game textures: 1 model unit = 1 texel per face on that cube. Nearest-neighbor. Pack scripts when they exist.

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

After generation, **open/read the image** and write a short **design lock** (proportions, opaque vs cutout, emissive zones).

### Step 1b — Segment / pixel bake (mandatory before geo)

Turn the 2D turnaround into a **segment inventory**, then into Minecraft boxes. Do not invent a chicken stub and “paint later.”

1. **Read the image** (tool read). Prefer a user **contour overlay** (`*_contur.png` / `*_contour.png`) when present — colored pens mark seams the way a human slices the silhouette. Sample dominant RGB if color is contested — match concept, not a default fantasy purple.
2. **Trace seams** on each view (or follow the user’s pens): where head ends / neck starts; each neck ring; torso plates; wing folds; thigh → knee → shin → foot; claw count. Rebuild geo from that inventory, not from a chicken stub.
3. Write `art/<creature>/SEGMENT_LOCK.md` with:
   - Palette table (roles → RGB)
   - Bone list with approximate box sizes (W×H×D in model units)
   - Crest / spines / claws counts and which parent bone owns them
   - Notes on bent joints (digitigrade, S-neck, etc.)
4. **Only then** build `geo` bones (one bone or cube per locked segment) and bake the atlas from that map.
5. Hand off to the user for **in-game testing** before polish pass.

Example (Essence Wyvern): neck is stacked rings with belly ribs; spine gold-tipped spikes on every segment; hind leg = thigh + knee + shin + foot + **4** talons — see `art/essence_wyvern/SEGMENT_LOCK.md`.

## Step 2 — Pick vanilla templates

See [vanilla-templates.md](vanilla-templates.md). Match **topology**, not theme:

- Biped humanoid → iron golem / zombie / piglin UV habits
- Biped + weapon cube → iron golem + extra cubes (Effecoria: vitrified golem)
- Quadruped → wolf / cow / spider segments
- Flying small → allay / bee / phantom
- Winged biped / wyvern → phantom wing idea + **custom** multi-bone neck/legs (never glue wings as one cube forever)

Record choice in `art/<creature>/DESIGN.md`: template mob, atlas size (64 vs 128/256), GeckoLib vs Java model.

## Step 3 — Implementation path in this repo

| Path | When | Output |
|------|------|--------|
| **GeckoLib** (preferred for animated bosses) | Multi-bone attacks, custom anims | `geo/*.geo.json`, `animations/*.animation.json`, `textures/entity/*.png` |
| **Java `Model` + `LayerDefinition`** | Simple mobs, one texture | `client/model/*Model.java`, `textures/entity/*.png` |

**Existing gold standard:** vitrified golem — `art/vitrified_wastes/golem/`, `scripts/pack_vitrified_golem_texture.py`, cutout renderer for solid atlas.

Workflow for new GeckoLib mob:

1. `SEGMENT_LOCK.md` from Step 1b.
2. Blockout `*.geo.json` (bones + cube sizes). Sync `description.texture_width/height` with atlas.
3. Paint/bake atlas from locked palette (script or nets). **Opaque α=255** on solid parts.
4. Anim stubs matching bone names.
5. Register entity, `GeoEntityRenderer`, animation names in code → **user tests in-game**.

## Step 4 — Alpha and “holes” (mandatory read)

Before marking a mob done, apply [reference.md — Transparency and holes](reference.md#transparency-and-holes).

Summary:

- **Holes in the mesh** ≠ **holes in the texture**. Translucent render + accidental alpha on “solid” limbs shows **empty interior**.
- **Every texel on a cube face** that should look solid must be **alpha 255**.
- **Never** leave UV rectangles empty for cubes that exist in geo.
- Semi-transparent pixels → **binarize** to 0 or 255.

When user reports “дырки”, inspect: renderer `RenderType`, atlas paste gaps, geo cube sizes vs UV sizes.

## Step 5 — Validation checklist

```
- [ ] Lore/design lock written
- [ ] SEGMENT_LOCK matches concept seams (counts, palette)
- [ ] Concept sheet matches locked proportions / colors
- [ ] Template + atlas size documented
- [ ] geo UV matches pack script PARTS (or documented map)
- [ ] pack/build script run; no MISSING faces in log
- [ ] In-game: no random holes; colors match concept (not default purple)
- [ ] Animations don’t pull cubes apart exposing unstitched UV
- [ ] User playtest feedback incorporated
```

## Utility commands

```bash
python scripts/pack_vitrified_golem_texture.py --init
python scripts/pack_vitrified_golem_texture.py --from-nets
python scripts/pack_vitrified_golem_texture.py
python scripts/build_essence_wyvern.py
```

## Additional resources

- [reference.md](reference.md) — UV layout, alpha rules, file paths
- [vanilla-templates.md](vanilla-templates.md) — mob → template mapping
- `art/essence_wyvern/SEGMENT_LOCK.md` — segment-bake example
