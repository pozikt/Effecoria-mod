# Vitrified Golem — artist swap

Rebuild from scratch (design lock + concept):

```bash
python scripts/build_vitrified_golem_from_scratch.py
```

See `DESIGN.md` and `concept_turnaround.png`.

In-game assets:
- `textures/entity/vitrified_golem.png` — 128×128 opaque atlas
- `geo/vitrified_golem.geo.json`
- `animations/vitrified_golem.animation.json`

Required bones: `root`, `head`, `body`, `right_arm`, `left_arm`, `right_leg`, `left_leg`

Animation ids:
`animation.vitrified_golem.{idle,walk,detect,attack_1,attack_2,rush,special,hurt,death}`
