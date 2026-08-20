# Release notes — Effecoria `0.2.7-alpha` (Hotfix)

NeoForge **1.21.1** · jar: `effecoria-0.2.7-alpha.jar`

**Supersedes `0.2.6-alpha`.** Same content as Demo 5.1, with a critical load fix.

## Fix

- **World load hang at 0%** — seal glow light overlay no longer calls `Level.getChunkAt` from `getLightEmission`. That path could deadlock / stall during chunk lighting and world creation. Overlay now uses already-loaded chunks only (`getChunkNow` + attachment presence check).

## Discord short — RU

```md
Effecoria 0.2.7-alpha — Hotfix

• Исправлено вечное зависание создания/загрузки мира на 0%
• Причина: оверлей свечения печатей форсил загрузку чанков из light engine
• Контент тот же, что в 0.2.6-alpha — ставьте именно 0.2.7

NeoForge 1.21.1
```

## Discord short — EN

```md
Effecoria 0.2.7-alpha — Hotfix

• Fixed infinite world create/load hang stuck at 0%
• Cause: seal glow light overlay forced chunk loads from the light engine
• Same content as 0.2.6-alpha — use 0.2.7 instead

NeoForge 1.21.1
```

## Modrinth / GitHub — RU

```md
# Effecoria 0.2.7-alpha (Hotfix)

Критический фикс поверх `0.2.6-alpha`.

- Исправлено зависание загрузки мира на 0% из‑за seal light overlay
- Свечение печатей больше не форсит `getChunkAt` во время lighting/worldgen
- Игровой контент без изменений относительно `0.2.6-alpha`

`0.2.6-alpha` считать сломанным для создания мира — используйте этот билд.
```

## Modrinth / GitHub — EN

```md
# Effecoria 0.2.7-alpha (Hotfix)

Critical fix on top of `0.2.6-alpha`.

- Fixed world loading stuck at 0% caused by the seal light overlay
- Seal glow no longer force-loads chunks during lighting / worldgen
- Gameplay content unchanged from `0.2.6-alpha`

Treat `0.2.6-alpha` as broken for world creation — use this build.
```

## Package locally

```powershell
.\gradlew.bat packageForShare -x test
```

Artifact: `dist/effecoria-0.2.7-alpha.jar`
