# Φ-alchemy machines (MVP)

GUI machines for Stage II village alchemy: **mortar**, **essence burner**, and **essence alembic**, linked by a shared heat bus.

## How to play

1. **Mortar** — open GUI, put essonite ore/crystal/shard/pure in the input.
   - **Manual:** hold a stick (tag `effecoria:pestles`) near the mortar (or with GUI open). Slow grind, ~70% dust / 30% cobble waste; rare gold nugget byproduct.
   - **Auto:** put a charged Φ-cell in the drive slot (or hold one nearby). Faster grind, ~90% purity; drains a little cell charge per craft.
   - Hoppers: insert from top/sides into input; extract outputs from below.

2. **Burner** — open GUI, put essonite dust in the fuel slot. Optional gold ingot catalyst (`effecoria:burner_catalysts`).
   - Buttons set temperature: **LOW / MED / HIGH**. Higher temp drains fuel faster and paints brighter particles.
   - **HIGH without catalyst** overheats after ~10s → cooldown (no heat until cooled).
   - Shift-right-click with dust still quick-feeds fuel.
   - Radiates `HeatLevel` to adjacent blocks (sides + above the burner).

3. **Alembic** — place next to (or above) a lit burner. GUI slots: water flask | reagent1–3 | output.
   - Reagent1: dust → tonic, shard → resonance, pure → stimulant.
   - Needs neighbor heat ≠ NONE. Heat level scales potion duration (LOW 85%, MED 100%, HIGH 120%) via stack NBT.
   - Reagent2/3 reserved (`effecoria:alembic_reagent_optional`, empty in MVP).

## Heat API

```java
com.effecoria.core.alchemy.HeatLevel   // NONE | LOW | MEDIUM | HIGH
com.effecoria.core.alchemy.PhiHeatSource // heatLevel(), consumeHeatTick()
com.effecoria.core.alchemy.PhiHeat.getNeighborHeat(level, pos)
com.effecoria.core.alchemy.PhiHeat.consumeNeighborHeat(server, pos)
```

Future furnaces / crystallizers should implement `PhiHeatSource` or only consume via `PhiHeat`.

## Extension tags

| Tag | Role |
|-----|------|
| `effecoria:pestles` | Manual mortar driver |
| `effecoria:mortar_inputs` | Extra grindables (also hard-coded essonite set) |
| `effecoria:burner_catalysts` | Prevents HIGH overheat |
| `effecoria:alembic_water` | Brew base |
| `effecoria:alembic_reagent_power` | Primary reagent (matrix still code-backed for MVP potions) |
| `effecoria:alembic_reagent_optional` | Future flora / distillate slots |

## GUI

Panels are **256×256** PNGs with content in the top-left **176×166** (vanilla `AbstractContainerScreen` + `GuiGraphics.blit` contract). Slot menu coords = top-left of the 16×16 item icon; frames painted at `(x-1, y-1)`. Player inventory is fixed at vanilla `y=84` / hotbar `y=142`. Regenerate: `python scripts/gen_alchemy_gui.py`.

## Deferred (not MVP)

Void/mithril mortar tiers, Φ-bus, flora reagents, distillate, Ω-pollen, Φ-furnace, crystallizer, alchemy skill, dual-potion mixing.
