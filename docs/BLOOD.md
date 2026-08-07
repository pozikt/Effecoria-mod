# Blood (Φ-носитель / Ψ-слепок)

In ETP lore, blood is liquid soul: hemoglobin iron conducts Φ; cellular DNA carries a Ψ imprint. This document records the full classification; **MVP code** only ships vials + alembic boosting.

## Classification

| Type | Source | Φ capacity | MVP item |
|------|--------|------------|----------|
| Common blood | Non-mages, animals | Low | `blood_vial` |
| Mage blood | Initiated mages / Orkanum bearers | High | `mage_blood_vial` |
| Ω-blood | Ω-natives, Eldritch, corruption victims | Negative | `omega_blood_vial` (stub) |
| Lonver blood | Lonvers | Extreme | *later* |
| Wyvern blood | Φ-wyverns | Very high | `wyvern_blood_vial` |

## MVP gameplay

1. Craft `blood_vial_empty` (Φ-flask + glass bottle).
2. Use on self → common or mage vial (hurts); use on living animal → common; wyvern loot / drain → wyvern vial.
3. Place blood vials in alembic optional slots → longer brew duration; mage/wyvern donor UUID anchors the potion (+25% duration when the donor drinks it).

## Deferred (lore only for now)

Medicine transfusion, Φ-ink, artifact binding, necromantic anchors, reactor coolant, vampires, blooddust explosives, kinship curses — see design brief / Stage III+.

Mirage `ModFluids.BLOOD` is a separate visual fluid and is **not** wired to vials.
