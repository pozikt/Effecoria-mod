# Seal word programming

Seals school programs blocks with a linear word scheme (key **G**).

## Grammar

Left → right pipeline:

1. **Passive** — `ACTION NUMBER? MODIFIER*` always on  
   Example: `Hardness Five` → block stays hard.
2. **Reactive** — `SENSE SPEC* ACTION NUMBER? MODIFIER* (Time NUMBER)?`  
   Sense emits a unit pulse on a matching event; then the action runs.  
   Example: `See Step Sound Five` → on step, play sound.  
   Example: `See Hardness Five Time Ten` → on sense, harden for 10 ticks.

Put always-on properties first; after `Time N` the rule closes so another passive may follow.

### Sense specs (subwords)

After `See`, optional filters:

| Word | Meaning |
|------|---------|
| Player / Mob | who |
| Approach | enter radius |
| Step | stand on block |
| Hit | left-click |
| Use | right-click |
| Break | block broken |

No specs → any of these events can pulse the sense. Number right after `See` (before an action) sets sense radius.

## Datapack

Words under `data/effecoria/seal_words/*.json` (`kind`: number, property, trigger, sense, spec, modifier, duration).

## Runtime

Compiled to `program_version: 2` with `passives` + `rules`. Rising-edge pulse fires actions; timed overlays live in `_rt.timed`.

Combat seal casting on **R** is deferred.
