# Art brief

Phase 0 uses **vanilla placeholder models**. Replace before public release.

## Style direction

- Palette: deep purples (Φ / essonite), silver-white (conductors), cold blue-grey (cold iron)
- Readability at 16×16 — avoid noisy textures
- Particles: soft glow, not vanilla flame clones

## Priority asset list

| Asset | Size | Notes |
|-------|------|-------|
| essonite_ore | 16× block | Purple crystalline in stone |
| essonite_block | 16× | Charged crystal block |
| resonance_focus | 16× item | Handheld crystal / lens |
| phi_spark | 8×8 particle | Essenton visual |
| entropy_wisp | 8×8 particle | Ω / backlash (dark purple) |

## MVP spells — VFX notes

| Spell | Particle idea |
|-------|---------------|
| fire_burst | Orange core + purple Φ rim |
| wind_push | Horizontal streak, white-silver |
| stone_shield | Brown hex ring at feet |
| mental_push | Cyan spiral toward target |
| mental_sting | Brief purple flash on target head |
| sense_phi | World tint overlay (shader later) + floating motes |

## Tools

- **Blockbench** — item/block models, GeckoLib mobs (phase 4+)
- **Aseprite / LibreSprite** — pixel textures
- GenerateImage in Cursor — **concept art only**, not final sprites

## Placeholder policy

Until custom art lands, use distinct vanilla stand-ins (documented in PR).  
Current: amethyst_block (essonite), amethyst_shard (focus).

## File locations

```
assets/effecoria/textures/block/
assets/effecoria/textures/item/
assets/effecoria/textures/particle/
assets/effecoria/particles/*.json
```

## Who does what

| Role | Responsibility |
|------|----------------|
| Lore author | Approve visual fit |
| Programmer | Wire particles in Java |
| Artist | Final PNG + Blockbench |
