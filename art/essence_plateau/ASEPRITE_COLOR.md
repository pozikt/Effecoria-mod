# Aseprite: серый цвет вместо ультрамарина

## Частая причина

Ванильные PNG из Minecraft часто в режиме **Grayscale (`L`)** или **Palette (`P`)**.  
Если открыть такой файл, Aseprite может создать спрайт в **Grayscale** — тогда любой цвет (даже ультрамарин `#120A8F`) при рисовании превращается в **оттенок серого**. Пипетка на ультрамарине в палитре показывает синий, а кисть всё равно серит — типичный признак.

Проверка: **Sprite → Color Mode** — должно быть **RGB Color**, не Grayscale и не Indexed.

## Что сделать

1. **Sprite → Color Mode → RGB Color** (если был Grayscale/Indexed — Aseprite переспросит; лучше новый файл).
2. Не рисовать на слое **Reference** / не заблокированном фоне с режимом Grayscale.
3. Открыть шаблон: `art/essence_plateau/phi_stone/phi_stone_32.png` (уже **RGBA**).
4. Внизу справа квадрат 6×6 — чистый ультрамарин; пипетка + кисть должны совпадать.
5. **File → Save As** → `phi_stone.aseprite` в той же папке.
6. Экспорт: **File → Export Sprite Sheet** или сохранить слой как PNG →  
   `python scripts/import_plateau_block.py phi_stone`

## Новый файл с нуля

1. **File → New** → 32×32, **Color Mode: RGB Color**, **Background: Transparent**.
2. Вставить hex ультрамарина в палитру (например `#1A237E` / `#120A8F`).
3. Рисовать на слое **Layer 1**, не Background.

## Импорт в мод

```powershell
python scripts\import_plateau_block.py phi_stone
```

Игра: **F3+T**.
