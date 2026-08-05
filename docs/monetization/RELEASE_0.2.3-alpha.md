# Release notes — Effecoria `0.2.3-alpha` (superseded)

**Use [RELEASE_0.2.4-alpha.md](RELEASE_0.2.4-alpha.md)** for Demo 4 — version was bumped after identity packs shipped on `main`.

NeoForge **1.21.1** · jar: `build/libs/effecoria-0.2.3-alpha.jar`

## Highlights

- **School identity packs** — mid-game “what this school feels like” spells across schools:
  - Corruption: `contagion_bloom`, `hunt_pulse`
  - Spatial: `phase_slip`
  - Mental: `mind_depress`, `mind_blank` (aggro wipe fix)
  - Necromancy: `thrall_focus`, `mark_reap`
  - Organic: `parasite_seed`, `spore_burst`
- **Seal words:** `vigil` (remote alarm), `haustus` (standing Ψ siphon)
- **Hydro slice** — block cutting behavior fix
- **Progression** — slower unlock pace, stricter order (see `f65204f`)
- **Iron** — armor/tools isolate wearer from Φ flow

## Modrinth / Discord short changelog

```
0.2.3-alpha — Stage I identity + polish
• Identity spells: Corruption contagion/hunt, Spatial phase slip, Mental depress/blank, Necro thrall focus/mark reap, Organic parasite/spore burst
• Seals: Vigil + Haustus word pack
• Fix: Mind Blank holds mobs off aggro; hydro-slice cutting
• Iron blocks Phi on wearer; progression pacing tweaks
• NeoForge 1.21.1 — see KNOWN_ISSUES.md
```

## Smoke (dev, 2026-08-05)

| Check | Result |
|-------|--------|
| `gradlew build` | OK |
| `gradlew runServer` → `Done` | OK (~3s after world prep) |
| 1h manual cast/seal smoke | **Not run** — do before public Demo 4 |

## Package locally

```powershell
.\scripts\package_demo.ps1
```

Copies jar + this file into `dist/` for upload.

## After publish

- Paste short changelog on Modrinth + Discord
- Link [KNOWN_ISSUES.md](KNOWN_ISSUES.md)
- Ask playtesters: “Would you recommend Effecoria to a friend after 30 minutes?”
