# Artifact craft (Era III)

Modular assembly of staves and jewelry, item seals for Seals school, Curios accessory slots.

## Player loop

1. **Shaft Lathe** (`shaft_lathe`) — material from `#effecoria:shaft_materials` + **length form** → `carved_shaft` (MED heat).
2. **Facet Cutter** (`facet_cutter`) — material from `#effecoria:focus_materials` + cut → `faceted_focus`.
3. **Phonemes** — `phi_phoneme_*` on shaft/focus (or essonite armor) before assemble.
4. **Artifact Assembler** — staff (shaft+focus) or jewelry (band+gem) → `modular_staff` / `assembled_*`.
5. **Seal Inscriber** — Seals school + known item seals → bind like enchantments; Strip removes them.
6. **Curios** — ring×2, amulet×1, charm×1 for jewelry. Open the **Curios** panel from the button next to your survival inventory (requires the Curios mod, bundled in dev via Gradle). Datapack path: `data/effecoria/curios/`.

## Shaft length

Forms are physical lengths (meters). Lathe UI cycles them shortest → longest:

| Form | Length | Notes |
|------|--------|--------|
| `wand` | 0.6 m | Short casting stick |
| `baton` | 1.0 m | Standard staff |
| `long_staff` | 1.4 m | Extended reach |
| `stature` | 1.8 m | About player height |

Longer shafts raise reach and slightly raise cast cost; material conductivity is independent.

Item models use scaled vanilla stick / blaze rod textures per form (`custom_model_data` 1–4). Regenerate PNGs with `scripts/gen_carved_shaft_textures.py` after changing length tables.

**Lathe & facet cutter GUI** use the vanilla stonecutter panel (176×166): input `(20,33)`, output `(143,33)`, **variant pool** as a 4-wide icon grid at `(52,14)` (click to select; invalid options dimmed when input material is wrong).

**Block art:** Generate `art/artifact_stations/face_sheet.png` (5×4 face atlas) and optional `concept_blocks.png` (isometric reference), then bake:

`python scripts/bake_artifact_station_textures.py`

Do **not** draw texels in Python — crops from the concept sheet and downscales to 16×16. Preview: `art/artifact_stations/baked_preview.png`. Shaft lathe & facet cutter are **half-height** benches; rotate with block `facing` like a stonecutter.

**Artifact assembler** uses the mortar panel; the same grid picks **staff / ring / amulet / charm**; craft slots at `(44,35)`, `(80,35)`, output `(134,35)`.

## Φ-conductivity

Each craft material has a datapack conductivity `0..1` under `data/effecoria/artifact/materials/`.

- Stamped onto carved parts and merged onto assembled gear (shaft 55% + focus 45%; jewelry 50/50).
- **Staff:** higher conductivity → lower cast cost, higher spell power.
- **Jewelry:** scales Curios Φ-shield bonus.

Examples: stick/planks ~0.2, copper/gold high, star essonite ~0.95, lead charm low (damper).

## Datapack

| Path | Content |
|------|---------|
| `data/effecoria/artifact/shaft_forms/` | Length profiles + reach/cost |
| `data/effecoria/artifact/materials/` | Item → Φ-conductivity |
| `data/effecoria/artifact/focus_cuts/` | Cuts + power/tier |
| `data/effecoria/artifact/assemble_recipes/` | staff/ring/amulet/charm |
| `data/effecoria/item_seals/` | Vanilla-analogue + Effecoria seals |

## Code

- `com.effecoria.core.artifact` — catalogs, NBT, `MaterialConductivity`, `StaffStats`
- Stations — `ArtifactStationBlock` + BE/menu/screen
- `ItemSealEvents` — combat/armor/mend hooks
- Discovery — `PlayerPsiData.knownItemSeals` (+ `item_seal_primer`)

Jewelry blanks: craft `jewelry_band` / `jewelry_gem`, then assemble. Prebuilt `gold_amulet`, `essonite_ring`, `star_amulet`, `phi_band`, `lead_charm` also wear in Curios.
