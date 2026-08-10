# Φ-Diseases

Playable pathologies of the Orkanum, Ψ-operator, DNA antenna, and Φ-trophic infections.

## Architecture

- Attachment: `effecoria:disease_profile` (`DiseaseProfile`) — **survives death** (`copyOnDeath`)
- Tick: every 10 server ticks via `DiseaseService.tick`
- Truth source: profile stages; vanilla mob effects are presentation only
- Ops: `/effecoria disease list|infect|cure|clear|clear_on_death`

### Death behaviour

By default diseases persist through death. Enable automatic wipe with:

```
/effecoria disease clear_on_death true
```

Force wipe anytime:

```
/effecoria disease clear
```

## Catalog

| Id | Cause (game) | Cure |
|----|--------------|------|
| `essence_burn` | Backlash, high Φ-radiation | Φ resistance / lead isolation; stage 3 scars Orkanum |
| `orkanumn_atrophy` | Long stay in Dead Wasteland | `orkanumn_stimulant` (stage 3 only improves) |
| `essentocytosis` | Prolonged radiation | `essentocyte_kit` (hurts) |
| `omega_sickness` | High entropy / Ω-Scar / Ω-blood | `potion_omega_cleanse` |
| `soul_dissonance` | Mental seize conflict | `psi_resonator_therapy` |
| `ghost_echo` | Death Mark / subspace | `psi_resonator_therapy` |
| `mage_barrenness` | Admin / rare | Blocks initiation; admin clear only |
| `curse_rot` | Corruption curse | Clears when curse is cured |
| `dust_lung` | Mining essonite / inhaling dust | `lung_rinse`; lead filter prevents |
| `omega_rot` | Ω-wound escalation / Ω mobs | `omega_amputation_salve` or cleanse |
| `crystal_fever` | Crystal Crab bite | `anti_phi_serum`; self-resolves; immunity |

## Balance

Knobs live under `disease_*` in `BalanceConfig`.
