# Emerald Canopy (Изумрудный Свод / Sea of Crowns)

Giant Φ-forest biome — evolution of Crystal Forest under stable high-Φ mycorrhiza.

## Locate

- TerraBlender replaces the **full jungle climate family** → `effecoria:emerald_canopy`:
  - `jungle`, `sparse_jungle`, `bamboo_jungle`, `mangrove_swamp`
- Region weight **12** (wider contiguous sheets than Crystal Forest)
- `/locate biome effecoria:emerald_canopy` (new chunks / new world required)

## Gameplay

| Feature | Behavior |
|---------|----------|
| Gravity | ×0.8 (`emerald_canopy_gravity_mult`) |
| Φ field | Strong bonus (`emerald_canopy_phi_bonus`, default 0.65) |
| Fog / weather | Dense Φ-mist; essence rain; storms screened by canopy |
| Dew | Higher harvest chance from Φ foliage |
| Forest Mind | Logging raises local anger → mental noise → ent aggro on harmers |

## Worldgen

- **Giant trees** (`EmeraldCanopyTreeFeature`): height ~40–64, rare emergents ~72–88; buttresses; `phi_log` / `ancient_essence_wood` / `golden_bark`; multi-tier `phi_leaves`
- **Understory**: tall blades, moss, snare vines

## Resources

- `ancient_essence_wood` — hard canopy timber (+ rare `ancient_heartwood`)
- `golden_bark` — alchemical concentrate (shapeless → essonite dust)
- `giant_phi_nut` — full Ψ restore for initiated
- `essence_dew` — canopy harvest bonus

## Fauna

Reuse: Φ-larva, eidos, crystal crab, essence wyvern  
New: Φ-Ent, Φ-Lemur, Wailer Bat, Glass Worm (vanilla-model placeholders)

## Hazards

- Fall from canopy tiers
- `phi_snare_vine` — slows / pulls / damages (worse for mages)
- Forest Mind when over-harvesting
