# Magic — what to build next (Stage I focus)

Aligns with [ROADMAP.md](ROADMAP.md) Stage I (“magic feels finished”) before biomes/ores/TSE.

## Where we are

| Area | Status |
|------|--------|
| 7 schools, JSON spells, radial cast | ~190 spells, progression lists per school |
| Ψ/Φ, breathing, exhaustion, whiff | In play |
| School VFX (particles) | Baseline; spatial trails, phi-sense motes added |
| Block seals | trap / fortify / glow / snare / repulse |
| Corruption curses | Contagion by EntityType + cure tags/AI — see [CORRUPTION.md](CORRUPTION.md) |
| Spatial singularity | Veil black-hole lens at cast focus — see [SPATIAL_FX.md](SPATIAL_FX.md) |
| **Lich ascension** | **Disabled** — out of progression; needs phylactery + mage tower (Stage IV) |

Spell count is no longer the bottleneck. Next work is **systems, feel, and gates**.

---

## Recommended order (phases)

### Phase A — Feel & readability (1–2 weeks of dev)

Goal: casting feels intentional and readable in survival.

1. **Per-school VFX pass** — consistent cast wind-up, impact, and AoE rings. ✅ *`CastPresentation`*
2. **SFX pass** — map schools to a small set of vanilla sounds (no new assets). ✅ *school themes in `CastPresentation`*
3. **`sense_phi` v2** — entity outline (high/low Φ / ZNΦ) + motes. ✅ *outline shipped in `ClientPhiSenseOutline`*
4. **Cast feedback** — specific blocked/whiff reasons on action bar. ✅ *`CastBlockReason` / `CastPipeline`*
5. **Balance sweep** — `rebalance_spell_costs.py` + playtest tiers 0–3 per school (not endgame numbers). ✅ *progression-index tiers*

**Exit:** a new player can learn one school to ~tier 3 without wiki.

---

### Phase B — Progression that isn’t “all spells day one” (core Stage I)

Goal: mastery and essence matter; radial isn’t overcrowded at start.

1. **Essence & research unlocks** — spells beyond starter band require essence + optional `unlock_essence_cost` in JSON (see `SpellUnlockService`, config `spell_starter_count`). ✅ *baseline shipped*
2. **Entropy tutorial** — first backlash event, HUD hint, decay over time. ✅ *`EntropyService` + HUD bar*
3. **Training XP** — meditation, spell success streaks, breathing drill (+ sprint/swim). ✅ *`ProgressionService`*
4. **Radial UX** — show next locked spells as ghost nodes + unlock hint on hover. ✅ *`SpellHubLayout` / `SpellUnlockService.hintFor`* (favorites optional later)

**Exit:** progression list is a *goal tree*, not an instant full wheel.

---

### Phase C — Φ harness (items, no worldgen yet)

Goal: bridge lore formulas to items players can craft.

1. **Essonite dust → Resonance Focus** craft (or upgrade path). ✅ *smelt ore → dust → focus; sneak-upgrade tiers*
2. **Phi Cell** — portable Φ buffer; drains on cast in low-Φ zones; craft-only recipe. ✅ *`PhiHarness` / `PhiCellItem`*
3. **Focus tiers** — small bonuses to resonance or cost floor (data in item NBT). ✅ *tiers 0–3 via CustomData*

**Exit:** low-Φ caves are playable without creative Φ.

---

### Phase D — Anti-magic & seals polish

Goal: ZNΦ and wards are gameplay, not lore text.

1. **Lead (ZNΦ) tag** — blocks or items reduce Φ_sample in radius; blocks cast in chambers.
   - **Deferred (box):** Stage II+ environment / materials — not while Stage I is pure magic loop.
2. **Cold iron** tag — optional school-specific debuff (necro/corruption first).
   - **Deferred** with D1 (world tags / materials).
3. **Seal stacking rules** — one offensive + fortify and/or glow per block. ✅ *see `SealLayer` / `SealPlaceResult`*
4. **Seal conflicts** — repulse vs trap priority; chunk sync messages. ✅ *`SealLayer.offensivePriority` (repulse > snare > trap > program); place feedback names old→new + layer list; `SealInspectHud` reads synced chunk seals*

**Exit:** builders can make a safe ritual room and a trapped corridor.

---

### Phase E — Breathing & body (Orkanum) ✅

1. Air / hunger coupling to `biologyQ` via `BiologyService.bodyFactor` → `PsiHelper.toContext` → Ψ regen. ✅
2. Soft Orkanum spell-power scale (`biology_spell_power_*` config). ✅
3. Hub breathing trainer + scroll + fatigue; server hit rate-limit. ✅
4. HUD: always show breathing after initiation; Orkanum strain / fatigue / drill bonus. ✅
5. First-hour tip `BREATHING` on hub open. ✅
6. Race baselines — `BiologyService.applyRaceBaseline` hook ready for Stage II.

---

### Phase F — Deferred “epic” magic (do **not** rush)

Tie to **Stage IV** technology / structures / late End progression:

| Feature | Depends on |
|---------|------------|
| **True lich** (`lich_ascension`, `regenPsiLich`, phylactery efficiency) | Phylactery item/block, soul anchor, tower Φ |
| **TSE risk on long blink** | TSE sites (Stage V) |
| **Ender Dragon thrall** | Endgame mastery + special AI (see below) |
| **Ω backlash** | Ω dimension (Stage VI) |

Code for lich state remains in `PlayerPsiData` / `FormulaEngine.regenPsiLich` for later wiring.

#### Ender Dragon thrall (design only — do not code in Stage I sprint)

**Fantasy gate:** very late game — End already explored for elytra, dragon defeated multiple times, players in netherite; necro control budget / mastery at endgame tier (far above normal thrall caps).

**Behavior when captured:**

1. **Autonomy** — unlike normal Death Mark thralls, the dragon is **not** leashed beside the caster. It keeps End-dragon-like free movement / “lives its own life.”
2. **Call** — on summoner signal (spell, key, or item TBD), it pathfinds / flies to the necromancer.
3. **Mount** — owner can mount and steer (ridable); dismount returns it toward autonomous End behavior (or linger nearby — TBD in implementation).
4. **Limits** — single dragon; no loot farm; does not use standard thrall LOS/leash combat AI.

Ship only after Stage I magic **presentation** and thrall budget feel solid.

---

## Optional parallel tracks (pick one when A–B stable)

- **Patchouli** — one page per school + cast loop + entropy + Death Mark / control budget.
- **Initiation ritual** — multiblock or block sequence instead of instant school select.
- **Spell variants** — overcast shipped; hold-to-charge: hold cast key (R), release to fire (power/cost ramp via `CAST_CHARGE_*`).

---

## Backlog / UX (noted)

### First join — school selection (TODO) → ✅ shipped

When a player **first appears in the world** and is not initiated, client opens **`SchoolSelectScreen(mandatory)`** (`ClientFirstJoinSchoolPrompt`). ESC cannot dismiss until a school is chosen.

### Magic presentation (after schools feel solid)

Guides / Patchouli / first-hour teaching wait until **each school has a satisfying combat identity**. Until then, prioritize making schools actually good (mental compulsion wave started: terror / cliff / frenzy).

---

## Deferred to later roadmap stages (do not start in Stage I magic pass)

| Item | Why deferred |
|------|----------------|
| **D1 Lead / ZNΦ world tags** | Needs environment, ores, structures — Stage II–III |
| **D2 Cold iron** | Same — material tags / world content |
| First-join school menu | ✅ shipped (`ClientFirstJoinSchoolPrompt`) |
| Lich re-enable | Stage IV tower / phylactery |
| **Ender Dragon thrall AI / mount** | Endgame fantasy; after magic presentation pass |

---

## Suggested next sprint (concrete)

If choosing a single slice for the next implementation session:

1. Phase **B1**: essence-gated spell unlocks — ✅ done.
2. Phase **A3**: phi-sense entity outline — ✅ done.
3. Phase **D3**: seal stacking on one block — ✅ done.
4. Phase **A4**: cast feedback — ✅ done.
5. **First-join school menu** — ✅ done.
6. Phase **B4** radial locked hints — ✅ done.
7. Test helper: `/effecoria max [school]` (op) — max stats + unlock all school spells — ✅ done.
8. Phase **B2** entropy tutorial — ✅ done.
9. Phase **A1/A2** VFX/SFX polish — ✅ done.
10. Phase **B3** training XP sources — ✅ done.
11. Phase **A5** balance sweep — ✅ done.
12. Phase **C** Φ harness (dust / focus / Phi Cell / tiers) — ✅ done.
13. Death Mark thralls + mastery control budget + additive Φ stacking — ✅ baseline shipped.
14. Phase **D4** seal conflicts + inspect HUD — ✅ done.
15. Magic **presentation** (Primer guide + first-hour tips + hub/HUD clarity) — ✅ done.
16. Phase **E** breathing/body (air/hunger → Orkanum, soft cast scale, HUD, train rate-limit, tip) — ✅ done.
17. Stage I **exit polish** (tip sequencing, seal tip, funnel/known-issues docs) — ✅ done.
18. **Next:** package Demo 1–3 builds + dedicated-server smoke; or intentional Stage II environment.

---

## Out of scope for Stage I

- New spell waves purely for count (schools are wide enough).
- Worldgen ores / biomes (Stage II–III).
- Mage tower multiblock (Stage IV) — **including re-enabling lich ascension**.
- Ender Dragon thrall / rideable dragon AI.

Update this file when a phase ships or priorities change.
