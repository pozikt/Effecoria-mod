# Φ-earth / Φ-grass textures (Aseprite)

В игре **заглушки** (ванильные текстуры, без своих PNG в `textures/block/`):

| Блок | Модель | Что видно |
|------|--------|-----------|
| `phi_dirt` | весь блок = `dirt` | коричневая земля (серые пиксели в текстуре — камешки в ваниле) |
| `phi_grass` | сбоку/снизу `dirt`, сверху `grass_block_top` | земля + зелёная трава (цвет травы от биома) |
| `phi_stone` | весь блок = `stone` | **серый камень** — это заглушка для Φ-камня, не земля |

Если «всё серое» на плато — часто это **Φ-камень** (`phi_stone`) или боковая грань ванильной травы (раньше была `grass_block_side`: серая земля + зелёная полоска). Сейчас у Φ-травы бока = чистый `dirt`.

## Файлы для правки

| Файл | Назначение |
|------|------------|
| **`phi_earth_texture_32.png`** | Основная Φ-земля (32×32), открыть в Aseprite |
| `phi_grass_top_32.png` | Верх Φ-травы (заглушка = ванильный grass top) |
| `phi_grass_side_32.png` | Сбоку Φ-травы (заглушка = grass side) |
| `phi_grass_bottom_32.png` | Низ (заглушка = dirt) |
| `phi_grass_faces_sheet_32.png` | Лист 96×32: top \| side \| bottom для референса |

Ванильные 16×16 референсы: `vanilla_dirt_16.png`, `vanilla_grass_top_16.png`, `vanilla_grass_side_16.png`.

## Aseprite

1. **File → Open** → `phi_earth_texture_32.png` (или `phi_grass_faces_sheet_32.png`).
2. **Sprite → Sprite Size** — 32×32, если нужно отдельные файлы по граням.
3. Сохранить проект как `phi_earth_texture.aseprite` в этой папке.
После правки листа в Aseprite:

```powershell
python scripts/import_phi_grass_sheet.py
```

Экспортирует в `textures/block/` и подхватывается моделями `phi_grass` / `phi_dirt`.

## Генератор (устарел)

`scripts/gen_phi_grass_tex.py` — не используется, пока арт в Aseprite не готов.
