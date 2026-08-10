# Spark Reactor («Искра») textures

1. Concept sheets generated into this folder (`concept_sheet.png`, `*_ref.png`).
2. `bake_textures.py` downsamples AI refs and paints crisp **16×16** faces into
   `assets/effecoria/textures/block/spark_reactor_*.png`.

Regen:

```bash
python art/phi_alchemy/spark_reactor/bake_textures.py
```

Faces: `front` (hatch + gem), `side` (fins), `top` (mithril contact), `bottom`, plus `_on` lit variants.
