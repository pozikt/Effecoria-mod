# Corruption curses

Targeted / pulse corruption debuffs go through `CorruptionCurseService` as **curse packages**.

## Contagion

- Same `EntityType` only (zombie → zombies).
- Radius: `contagion_chunks × 16` (0 / 1 / 2 by progression band).
- Peers receive the package with **no further cascade**.
- Players get effects but never contagion or seek-AI.

## Cure (individual)

Standing on or picking up a tagged cure:

| Tier | Tags |
|------|------|
| COMMON | `effecoria:corruption_cure_common` (flowers, saplings, logs/planks, dirt/grass, cobble, water/kelp) |
| RARE | `effecoria:corruption_cure_rare` (clay, mossy stone, ores, amethyst, raw ore blocks) |

COMMON curses also accept RARE materials. Only the cured mob is cleansed.

## Soft DoT

High-tier packages may set `soft_dot_per_second`. Damage ticks every second via magic damage — **not** infinite vanilla Poison.

## Seek AI

Cursed `PathfinderMob`s path to a **visible** cure block/item (~14 blocks) only when `getTarget() == null`. Pickup loot may be temporarily enabled for the curse and restored on clear.
