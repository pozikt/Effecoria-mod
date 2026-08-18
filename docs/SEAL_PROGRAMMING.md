# Seal word programming

Seals school programs **matter** with a typed overlay on named cells (key **Y**). Lex Loci (tower edicts) stays separate.

## Glue symbols

Look at a block and press **Y**. If it is Φ-glued, the editor imports the whole component.

- One dirt in the bundle → `dirt` / `земля`
- Several dirt blocks → conflict until renamed `dirt#north` (list + alias field, Enter)
- A lone unglued block is still one symbol by type

Click a name in the list to insert it. World outlines: blue = imported cells, red = unnamed duplicates, green = selected.

## Language

```
dirt#north:
  glow = 5
  hardness = 8
  when step:
    sound = 5
```

Russian aliases work (`светимость`, `твёрдость`, `когда шаг`, `звук`). `import glue` is optional.

Properties are **seal overlays**, not vanilla BlockState. `glow = 5` on dirt does not replace dirt and does not place a `minecraft:light` block — light comes from the inscription.

Numbers are literals. Known overlay keys: glow/light, hardness, sound, hurt, slow, push, plus existing seal-word actions. Sense specs after `when`: step, hit, use, break, approach, player, mob.

Tab inserts an autocomplete suggestion. Save/Load slots store the **source text**.

## Limits

Distinct inscribed cells scale with breathing mastery (4–12). Ψ cost is the sum of used words across cells.

## Runtime

Compiled to `program_version: 3` with `passives` + `rules` (same evaluator as before). Timed overlays live in `_rt.timed`. Erase: editor button or Shift + empty-hand RMB (clears the imported component).

Legacy chip sequences still apply if sent, and can be pretty-printed when loaded from old save slots.

Combat seal casting on **R** is deferred.
