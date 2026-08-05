# Versioning (Stage I alpha)

Public demo / Modrinth builds use **`mod_version` in `gradle.properties`**. Gradle names the jar `effecoria-<mod_version>.jar`.

## Rule of thumb

| When | Bump |
|------|------|
| New **public** jar (Demo cadence, Modrinth upload) | Patch: `0.2.x` → `0.2.(x+1)` |
| Same Minecraft line, pre-1.0 | Keep suffix `-alpha` |
| Only dev commits, no upload | Version optional (avoid drift) |

**One published version = one git snapshot.** If content shipped after a version bump (e.g. identity packs after `0.2.3`), bump again before upload (`0.2.4`).

## Release checklist

1. Set `mod_version` in `gradle.properties`
2. Add `docs/monetization/RELEASE_<version>.md` (changelog + Modrinth short text)
3. Update `docs/monetization/KNOWN_ISSUES.md` header
4. `.\scripts\package_demo.ps1` → `dist/`
5. Upload jar + paste changelog + KNOWN_ISSUES on Modrinth / Discord

Cross-ref: [STAGE_I_FUNNEL.md](STAGE_I_FUNNEL.md)
