# Stage I — Free funnel readiness

The free core is the sales funnel. Do **not** open Boosty donate, public server pay, or DLC until this checklist passes.

Cross-ref: [docs/ROADMAP.md](../ROADMAP.md) Stage I polish · [KNOWN_ISSUES.md](KNOWN_ISSUES.md)

## Definition of done (recommend to a friend)

A new player can, without a wiki:

1. Pick up Resonance Focus → choose a school ✅
2. See Ψ / Φ HUD and understand spend vs regen at a glance ✅
3. Open spell book (X), pick a spell, cast (R) ✅
4. Notice breathing / training matter (at least one onboarding tip) ✅
5. Place or inspect a seal without crashing or soft-locking ✅ *(Seals school; tip on first apply)*
6. Survive 30+ minutes of play without critical bugs ⬜ *needs public smoke*

If any step fails, Stage I is not a funnel yet.

## Checklist — Feel

| Item | Done? | Notes |
|------|-------|-------|
| Signature VFX on starter spells (all playable schools) | [x] | `CastPresentation` school themes |
| Basic SFX or distinct cast feedback | [x] | School-mapped vanilla SFX + action-bar block reasons |
| `sense_phi` / Φ feedback readable in world | [x] | Entity outline + block ZNΦ paint |
| No placeholder purple-black missing textures on player path | [x] | Focus / dust / Phi Cell / scroll / ores have textures |

## Checklist — Teaching the loop

| Item | Done? | Notes |
|------|-------|-------|
| In-game tip on first initiation | [x] | `FirstHourTips.INITIATED` + Magic Primer grant |
| Spell hub shows school + cost clearly | [x] | Hub hover + locked hints |
| Entropy / overcast explained once | [x] | Entropy tip + overcast cast message |
| Patchouli stub or equivalent guide book | [x] | Magic Primer (in-mod guide) |
| `/effecoria` debug not required for normal play | [x] | Keep for testers |

## Checklist — Progression baseline

| Item | Done? | Notes |
|------|-------|-------|
| Breathing mastery affects something the player feels | [x] | Cost, unlocks, Orkanum, soft cast power, necro budget |
| Path beyond starter trio documented or gated in UI | [x] | Essence gates + ghost nodes |
| Training / soul path does not soft-lock | [x] | Sprint/swim/meditate/drill + diminishing returns |
| Anti-magic (ZNΦ / cold iron) tagged or scheduled | [x] | `#effecoria:zero_flux` / `#effecoria:cold_iron` |

## Checklist — Stability

| Item | Done? | Notes |
|------|-------|-------|
| Clean client launch (NeoForge 1.21.1) | [ ] | Dev verified; needs each demo build |
| Dedicated server starts with mod | [ ] | Required before public server |
| Cast / school switch / seal place: no crash in 1h smoke test | [ ] | Run before Demo 4 |
| Known issues list published (Discord + Modrinth) | [~] | Draft: [KNOWN_ISSUES.md](KNOWN_ISSUES.md) — publish with next demo |
| Versioned builds (semver or `0.x.y`) | [x] | `0.2.2-alpha` in gradle.properties |

## Funnel gate (go / no-go)

**GO to Phase 1 (support pages)** when:

- All “Definition of done” items pass
- Feel + Teaching checklists ≥ 80% checked ✅ *(teaching/feel met in code)*
- Stability smoke tests pass ⬜
- At least **2–4 public demo builds** shipped with changelogs ⬜

**NO-GO** if players say “too early / too raw” as the dominant feedback.

## Public demo build cadence (pre-support)

| Build | Goal | Status |
|-------|------|--------|
| Demo 1 | Initiation + cast loop | Ready to package |
| Demo 2 | Breathing + training visible | Ready (Phase E) |
| Demo 3 | Seals + Φ harness + essonite | Ready |
| Demo 4 | Polish / bugfix; ask “would you recommend?” | After smoke + known-issues post |

After Demo 4 positive signal → [SUPPORT_TIERS.md](SUPPORT_TIERS.md).
