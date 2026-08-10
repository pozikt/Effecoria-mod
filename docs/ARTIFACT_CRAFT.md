# Artifact craft (Era III)

Modular assembly of staves and jewelry, item seals for Seals school, Curios accessory slots.

## Player loop

1. **Shaft Lathe** (`shaft_lathe`) — material from `#effecoria:shaft_materials` + form → `carved_shaft` (MED heat).
2. **Facet Cutter** (`facet_cutter`) — material from `#effecoria:focus_materials` + cut → `faceted_focus`.
3. **Phonemes** — `phi_phoneme_*` on shaft/focus (or essonite armor) before assemble.
4. **Artifact Assembler** — staff (shaft+focus) or jewelry (band+gem) → `modular_staff` / `assembled_*`.
5. **Seal Inscriber** — Seals school + known item seals → bind like enchantments; Strip removes them.
6. **Curios** — ring×2, amulet×1, charm×1 for jewelry.

## Datapack

| Path | Content |
|------|---------|
| `data/effecoria/artifact/shaft_forms/` | Forms + stats |
| `data/effecoria/artifact/focus_cuts/` | Cuts + power/tier |
| `data/effecoria/artifact/assemble_recipes/` | staff/ring/amulet/charm |
| `data/effecoria/item_seals/` | Vanilla-analogue + Effecoria seals |

## Code

- `com.effecoria.core.artifact` — catalogs, NBT, Curios helpers, `StaffStats`
- Stations — `ArtifactStationBlock` + BE/menu/screen
- `ItemSealEvents` — combat/armor/mend hooks
- Discovery — `PlayerPsiData.knownItemSeals` (+ `item_seal_primer`)

Jewelry blanks: craft `jewelry_band` / `jewelry_gem`, then assemble. Prebuilt `gold_amulet`, `essonite_ring`, `star_amulet`, `phi_band`, `lead_charm` also wear in Curios.
