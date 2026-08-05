# Release notes — Effecoria `0.2.4-alpha` (Demo 4)

NeoForge **1.21.1** · jar: `build/libs/effecoria-0.2.4-alpha.jar`

**Version bump:** `0.2.3-alpha` was tagged before identity packs landed; **0.2.4-alpha** is the jar that matches current `main` for public Demo 4.

## Highlights

- **School identity packs** — mid-game “what this school feels like” spells:
  - Corruption: `contagion_bloom`, `hunt_pulse`
  - Spatial: `phase_slip`
  - Mental: `mind_depress`, `mind_blank` (aggro wipe fix)
  - Necromancy: `thrall_focus`, `mark_reap`
  - Organic: `parasite_seed`, `spore_burst`
- **Seal words:** `vigil` (remote alarm), `haustus` (standing Ψ siphon)
- **Hydro slice** — block cutting behavior fix
- **Progression** — slower unlock pace, stricter order
- **Iron** — armor/tools isolate wearer from Φ flow

## Modrinth / Discord short changelog

```
0.2.4-alpha — Demo 4 (Stage I identity)
• Identity spells: Corruption, Spatial, Mental, Necro, Organic identity packs
• Seals: Vigil + Haustus
• Fix: Mind Blank aggro; hydro-slice cutting
• Iron blocks Phi on wearer; progression pacing
• NeoForge 1.21.1 — see KNOWN_ISSUES.md
```

## Smoke (dev, 2026-08-05)

| Check | Result |
|-------|--------|
| `gradlew build` | OK (re-run after version bump) |
| `gradlew runServer` → `Done` | OK |
| 1h manual cast/seal smoke | **Not run** — do before calling Demo 4 final |

## Package locally

```powershell
.\scripts\package_demo.ps1
```

## After publish

- Paste short changelog on Modrinth + Discord
- Link [KNOWN_ISSUES.md](KNOWN_ISSUES.md)
- Ask playtesters: “Would you recommend Effecoria to a friend after 30 minutes?”
