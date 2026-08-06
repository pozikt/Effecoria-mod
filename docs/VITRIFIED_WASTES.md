# Vitrified Wastes — Residual Φ Flash Zone

**Стеклянная Пустошь** (`effecoria:vitrified_wastes`): пустыня, оплавленная Φ-вспышкой.
Чёрное стекло, ультрамариновые/золотые кристаллы, окаменевшие деревья. **Без мобов** (пока).

## Placement

TerraBlender region (weight **8**) replaces vanilla `minecraft:desert` in its slice,
alongside Dead Wasteland (also weight **8**). Together they split desert land roughly
50/50 between zero-Φ ash flats and flash-vitrified glass. Needs a **new world**.

`/locate biome effecoria:vitrified_wastes`

## Look

| Block | Role |
|-------|------|
| `vitrified_sand` | Surface dunes / Φ-barghans (slow fall + sink) |
| `vitrified_dirt` | Glass soil crust |
| `vitrified_stone` | Obsidian-hard fused bedrock crust |
| `vitrified_log` / `vitrified_branches` | Petrified lightning trees |
| `vitrified_geyser_crack` | Residual Φ fissure |
| Essonite crystals | Surface / crater rim |

Sky/fog: near-black indigo with blue-gold particle haze.

## Structures (features)

| Feature | Rarity | Notes |
|---------|--------|-------|
| `vitrified_tree` | common | Single petrified trunk |
| `vitrified_grove` | uncommon | 10–30 trees + crystal floor |
| `vitrified_crater` | uncommon | Glass bowl + central crack |
| `vitrified_mage_tower` | rare | Stone tower, essonite statue, chest |
| `vitrified_frozen_village` | very rare | Flash village husk + geyser plaza |

## Gameplay

| Effect | Behavior |
|--------|----------|
| Φ sample | Elevated (`vitrified_phi_bonus`, +storm bonus) |
| Spells | Allowed (unlike Dead Wasteland) |
| Radiation | Magic DPS without charged Φ-cell |
| Φ-storm | Rare; extra damage + particles |
| Protected mages | Mild Ψ regen while cell has charge |
| Quicksand | `vitrified_sand` slows / sinks / wall-damage |
| Branches | Light cactus-like contact damage |

Config keys under `BalanceConfig`: `vitrified_*`.

## Mobs

| Mob | Status |
|-----|--------|
| Vitrified Golem (`effecoria:vitrified_golem`) | Hostile — melee, rush, Φ-flash. Placeholder geo/texture (64×64); swap artist assets under `geo/`, `animations/`, `textures/entity/`. |

Spawn egg: creative tab. Natural spawn in biome (light rules via monster placement).

## Contrast with Dead Wasteland

| | Dead Wasteland | Vitrified Wastes |
|--|----------------|------------------|
| Φ | Forced zero | Elevated residual |
| Cast | Blocked | Allowed |
| Palette | Bleached ash | Black glass / cyan-gold |
| Hazard | Orkanum sleep | Radiation + storm |
