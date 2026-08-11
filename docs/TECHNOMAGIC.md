# Technomagic tree

Craft stays **free**. Operating Era N machines requires completing **all available** catalog nodes of eras 1..N−1 (craft their icon items to discover them). Machines also consume prior-era resources (Φ-heat, charged cells, Φ-flux slugs).

See also [ROADMAP.md](ROADMAP.md) Stage IV.

## Progression rules

| Rule | Effect |
|------|--------|
| Free craft | Recipes are never recipe-gated |
| Era complete | Every `available` node in that era is discovered |
| Operate Era N | Eras 1..N−1 must be complete (creative mode bypasses) |
| Resource chain | Era II needs Era I heat; Era III needs MED burner + Φ-cell; Era IV Spark needs flux slug; Heart needs pure essonite / flux priming + coolant; Forge needs star/pure essonite or charged cell |

## Eras

| Era | Theme | Status |
|-----|--------|--------|
| I — Hearth | Torch, campfire heat, crucible, mortar | **Playable** |
| II — Workshop | Burner, Φ-furnace, glass/flasks, alembic, filters, cell, focus | **Playable** |
| III — Imprint | Ψ-imprinter, golem / telegraph, artifact craft, seals, jewelry | **Playable** |
| IV — Reactors | Spark, Heart 3×3×3, Φ-bus, Forge 3×4×3, **Φ-turrets** | **Playable** |
| V — Geo | Geo wells, climate array, portal gate | Catalog `planned` |

## Era IV Heart multiblock (3×3×3)

Around `heart_reactor_core`:

| Position | Block |
|----------|--------|
| 8 corners | `void_obsidian` |
| 12 edges | `reactor_casing` |
| 6 face centers | `phi_glass` |
| Center | `heart_reactor_core` |

When the shell is valid, it **assembles**: shell cells become invisible `heart_reactor_part`s and the core BER draws one solid textured 3×3×3. Click any part/core to open the fuel GUI. Breaking a part or the core dismantles and restores the materials (broken cell drops its original block).

Prime with **1× pure essonite** or **4× phi_flux_slug**, then START. Ambient Φ while formed+running (`PhiPower` radius 8; hull parts are Φ-transparent for LOS). Place ice/water/Φ-water just outside the shell to cool. Adjacent `phi_bus` on the **hull** (or Spark) energizes the cable (BFS to controller; hop attenuation); bus outlets are `PhiPowerProvider` radius 1 — place the last bus **adjacent to the machine** (turret mount).

## Era IV Forge multiblock (3×4×3)

One layer taller than Heart. Core at relative `(0,0,0)`; shell `dx,dz ∈ {-1,0,1}`, `dy ∈ {-1,0,1,2}`:

| Cell | Block |
|------|--------|
| Vertical corner pillars | `void_obsidian` |
| Floor / roof (non-corners) | `lead_block` |
| Mid-layer side windows | `phi_glass` |
| Center | `forge_reactor_core` |

Assembles into invisible `forge_reactor_part`s + BER 3×4×3 golden hull. Deep hum while lit.

**Modes:** ENERGY (Φ radius 32 + bus), SMELT, SYNTH, CLEANSE. Fuels: star essonite / pure essonite / charged Φ-cell (≥85%). Catalysts: Lonver blood vial, Φ-nectar, fireflower. Coolant outside hull; Ω meter scrams at 25%, burst at 50%.

## Era IV Φ-turrets

Two-block assembly: shared **`turret_mount`** (half-slab, attaches to floor / wall / ceiling) + type **barrel**. Φ-power connects to the mount only (wireless in Heart radius, or last `phi_bus` touching the mount). When a barrel sits on the outward face, both become `FORMED`; BER draws a fixed plate + rotating yoke/barrel that aims at hostiles.

Drain via `PhiPower.consumeTick(load)` at the mount (Spark/Heart/Forge; bus drains the injector).

| Barrel | Power cost / shot | Ammo | Catalog |
|--------|-------------------|------|---------|
| Plasma | 20 | — | mount |
| Kinetic | 40 | mithril bolt / nuggets | mount |
| Mental | 30 | — | mount |
| Spatial | 200 (needs factor ≥1.5) | — | mount + Forge |
| Omega | 120 | Ω-dust / tainted / shard | mount + Forge |

Arm in mount GUI. Targets hostiles with LOS from the barrel cell. Overheat after sustained fire.

## Flow

```mermaid
flowchart LR
  spark[spark_reactor] --> heart[heart_reactor_core]
  casing[reactor_casing] --> heart
  heart --> forge[forge_reactor_core]
  heart --> bus[phi_bus]
  forge --> bus
  spark --> bus
  bus --> machines[adjacent_machines]
  heart --> turrets[phi_turrets]
  forge --> turrets
  bus --> turrets
```

## Data

- Catalog: `data/effecoria/technomagic/*.json`
- Code: `HeartMultiblock`, `ForgeMultiblock`, `*ReactorBlockEntity`, `PhiBusNetwork`, `PhiPowerHubs`, `TurretMountBlock`, `PhiTurretBlock` (barrel), `PhiTurretBlockEntity`, `TurretAssembly`, `TurretKind`
- `PhiPower` local scan radius 8; Forge hubs via `PhiPowerHubs` up to 32; `drainFuel` on providers
- Gates: `TechnomagicGates` / `TechnomagicProgress`
