# DLC — School Codex (first paid addon)

**Ship only after** strong reception of the free core ([STAGE_I_FUNNEL.md](STAGE_I_FUNNEL.md) + community “would recommend” without “too raw”).

Separate jar: `effecoria_school_codex` depending on `effecoria`. Core remains playable without this addon.

## Product promise

| Free core | School Codex (paid) |
|-----------|---------------------|
| Starter loop per school | Extra spells / seal words per school |
| Baseline VFX | Optional VFX packs / alternate cast skins |
| Self-sufficient | Depth & fantasy, not mandatory power |

**Migration policy (locked):** Codex stays paid. After **6–12 months**, 1–2 simplified spells or words may move into free core; Codex keeps the rest + new season content. Announce the policy on the store page.

## Content scope (v1)

Per playable school (except `none`):

- +2–4 spells (JSON) using **existing** effect types where possible
- +2–3 seal words if Seals school / programming benefits
- Optional particle/skin overrides (cosmetic)

Hard cap: Codex must not be required to “finish” Stage I progression.

## Reception gate (before coding the jar)

- [ ] Free core on Modrinth with changelog history
- [ ] Dominant feedback is not “early / broken”
- [ ] Support tiers live ≥ 1 month OR clear one-time demand for a pack
- [ ] Store page (Boosty shop / Gumroad) draft ready
- [ ] НПД receipts process ready for digital goods

## Technical layout (scaffold)

Repo path: [`addons/school-codex/`](../../addons/school-codex/).

```
addons/school-codex/
  README.md                 — build & distribution notes
  content-draft/
    spells/<school>/*.json  — draft spell defs (not yet wired)
    seals/words/*.json      — draft seal words
  STORE_PAGE.md             — RU/EN product copy
```

When implementing for real:

1. Prefer a **Gradle subproject** or sibling repo that depends on the published/core `effecoria` jar.
2. Mod id: `effecoria_school_codex`
3. Load only additional datapack namespaces / optional resource packs
4. Soft-depend: if core missing → clear log error, no crash loop
5. Do **not** publish the paid jar on Modrinth as the main file; link “requires Effecoria”

See [docs/ARCHITECTURE.md](../ARCHITECTURE.md) for adding spells via JSON.

## Pricing (guide)

| Region | One-time |
|--------|----------|
| RU | ~299–599 ₽ |
| EN | ~$4–8 |

Discount for Operator+ supporters optional (code or Discord role).

## Lore Season alternative

If School Codex feels too early, ship **Lore Season 1** instead: 4–8 week quest/boss/artifacts pack with the same jar rules and no P2W. Same gates apply.

## Do not

- Gate basic initiation or starter trio behind Codex
- Buff paid spells above free endgame without free counters
- Bundle server P2W with the DLC
