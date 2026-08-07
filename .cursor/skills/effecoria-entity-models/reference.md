# Entity models — reference

## Transparency and holes

### Why the vitrified golem looked “full of holes”

1. **`RenderType.entityTranslucent`** (`VitrifiedGolemRenderer`) — every texel with alpha &lt; 255 shows what is **behind** that surface (other faces, empty air inside the hitbox, sky).
2. **“Glass” painted with transparency** — if limb faces use soft alpha or gray “glass” at 50%, you see **into the model** and **out the other side** → reads as Swiss cheese, not glass material.
3. **Unpainted UV / wrong UV** — geo cube still renders; texture samples **alpha 0** from atlas → invisible quads → gaps between boxes.
4. **Edge flood too aggressive** — `clean_face_alpha(..., silhouette=True)` on **non-silhouette** parts eats dark glass fill (`pack_vitrified_golem_texture.py` → `SILHOUETTE_PARTS` only blades).
5. **Semi-transparent antialiasing** from downscaling photos/AI art — must snap to **0 or 255** alpha.

### Rules for Effecoria entity albedo

| Intent | Alpha | Notes |
|--------|-------|--------|
| Solid body (stone, chitin, metal, **opaque glass**) | 255 everywhere on that face | Fake glass with **highlights + emissive**, not transparency |
| Cutout (blade shard, torn edge) | 0 outside silhouette | Flood from **image edges** only; keep interior opaque |
| Atlas padding | 0 | Never leave stray RGB on unused atlas cells |
| Emissive (Φ glow, eyes) | 255 | Use bright texels; optional `RenderLayer` later — still opaque base |

**Minecraft entity “transparency”** is almost always **cutout** (binary alpha), not true alpha blending, unless you accept see-through artifacts.

### Minecraft box UV (GeckoLib / Bedrock)

For a box of size `(w, h, d)` with origin UV `(u, v)`:

```
top    (u+d,     v)       w × d
bottom (u+d+w,   v)       w × d
right  (u,       v+d)     d × h   (+X)
front  (u+d,     v+d)     w × h   (−Z, “face”)
left   (u+d+w,   v+d)     d × h   (−X)
back   (u+2d+w,  v+d)     w × h   (+Z)
```

**1 unit cube size = 1 pixel** on each face (Effecoria golem script convention). Larger weapon? **Bigger cube** in geo + **more pixels** on that face (see `right_blade` 8×12×8).

Red corner marker on edit templates = **top** of side faces (orientation when painting).

## Repo paths

| Asset | Location |
|-------|----------|
| Game texture | `src/main/resources/assets/effecoria/textures/entity/` |
| Geo | `src/main/resources/assets/effecoria/geo/` |
| Animations | `src/main/resources/assets/effecoria/animations/` |
| Art workspace | `art/<region>/<creature>/` |
| Face kit | `art/.../faces/`, `nets/` |
| Pack script | `scripts/pack_vitrified_golem_texture.py` (clone for new mobs) |

## Concept → game scale

1. Measure concept height in pixels (e.g. character 512 px tall).
2. Target in-game height ≈ entity hitbox height in blocks (golem ~2.5–3 blocks).
3. Native face size = geo cube edge length in **pixels** (integer).
4. Edit at `×8` (or script `SCALE`) with **nearest** downscale only.
5. Re-read downscaled face at 100% zoom — no single-pixel holes along seams.

## Renderer choice

- **Opaque mob** → `RenderType.entityCutoutNoCull` or solid if no alpha.
- **Designed cutouts** (blades, wisps) → `entityTranslucent` + **binary** alpha discipline.
- Mixed: separate layers (body opaque, effect translucent) — only if worth the complexity.

## DESIGN.md template (per creature)

```markdown
# <Creature name>

## Lore source
- docs/...

## Silhouette
- ...

## Vanilla template
- Base: iron_golem (128) / zombie (64) / custom

## Atlas
- Size: 128×128
- Cutout parts: ...
- Opaque parts: ...

## Bones
- root, body, ...

## Status
- [ ] concept
- [ ] geo
- [ ] textures packed
- [ ] in-game verified
```
