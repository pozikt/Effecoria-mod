# Essence Plateau — пути для арта блоков

Биом: `effecoria:essence_plateau`. Размер грани в игре: **16×16** (можно рисовать **32×32** и downscale при импорте).

## Уже в игре (Φ-земля / Φ-трава)

| Что | Рисовать / лист | Импорт | Текстура в моде |
|-----|-----------------|--------|-----------------|
| Φ-трава (3 грани) | `art/phi_earth/phi_grass_faces_sheet_32.png` | `python scripts/import_phi_grass_sheet.py` | `textures/block/phi_grass_top.png`, `phi_grass_side.png`, `phi_grass_bottom.png` |
| Φ-земля | правая плитка на том же листе | тот же скрипт | `textures/block/phi_dirt.png` |

Модели: `src/main/resources/assets/effecoria/models/block/phi_grass.json`, `phi_dirt.json`.

---

## Остальные блоки биома (нужен арт)

### Φ-камень (`phi_stone`)

| | Путь |
|---|------|
| **Рисовать здесь** | `art/essence_plateau/phi_stone/phi_stone_32.png` (одна грань, `cube_all`) |
| **Референс ванилы** | `art/essence_plateau/phi_stone/vanilla_stone_16.png` (держим в **RGBA**, не Grayscale) |
| **Шаблон RGB** | `art/essence_plateau/phi_stone/phi_stone_32.png` (угол 6×6 — ультрамарин для проверки пипетки) |
| **Экспорт в мод** | `src/main/resources/assets/effecoria/textures/block/phi_stone.png` |
| **Модель** | `src/main/resources/assets/effecoria/models/block/phi_stone.json` |
| **Сейчас в игре** | заглушка `minecraft:block/stone` |

После экспорта PNG обновить `phi_stone.json`:

```json
"textures": { "all": "effecoria:block/phi_stone" }
```

Импорт (после положить файл в `art/...`):

```powershell
python scripts/import_plateau_block.py phi_stone
```

---

### Эссенитовый кристалл (`essonite_crystal`) — **не куб**, а кластер как аметист

В ваниле кристалл — модель `cross` + PNG силуэта (прозрачный фон), не `cube_all`.

| | Путь |
|---|------|
| **Рисовать (16×16)** | `art/essence_plateau/essonite_crystal/essonite_crystal_edit.png` |
| **Рисовать (32×32, удобнее)** | `art/essence_plateau/essonite_crystal/essonite_crystal_edit_32.png` |
| **Эталон ванилы** | `art/essence_plateau/essonite_crystal/vanilla_amethyst_cluster.png` |
| **Буд-этапы (референс)** | `vanilla_large/medium/small_amethyst_bud.png` |
| **Модель-референс** | `art/essence_plateau/essonite_crystal/models/vanilla_amethyst_cluster.json` |
| **Экспорт в мод (после арта)** | `src/main/resources/assets/effecoria/textures/block/essonite_crystal.png` |

В Aseprite: **RGB Color**, прозрачный фон; силуэт как у аметиста.  
После готовности арта — скажи, подключим `cross` + cutout (сейчас в игре ещё куб-заглушка).

---

### Эссенитовая руда в камне (`essonite_ore`) — worldgen на плато

| | Путь |
|---|------|
| **Рисовать здесь** | `art/essence_plateau/essonite_ore/essonite_ore_32.png` |
| **Экспорт в мод** | `src/main/resources/assets/effecoria/textures/block/essonite_ore.png` |
| **Модель** | `src/main/resources/assets/effecoria/models/block/essonite_ore.json` |

Импорт:

```powershell
python scripts/import_plateau_block.py essonite_ore
```

(Остальные варианты руды — `deepslate_essonite_ore`, `granite_essonite_ore`, … — в `textures/block/` с тем же префиксом; для плато важнее базовый `essonite_ore`.)

---

## Сводка папок `art/`

```
art/
  phi_earth/
    phi_grass_faces_sheet_32.png    ← Φ-трава + Φ-земля (лист)
    README.md
  essence_plateau/
    README.md                       ← этот файл
    phi_stone/
      phi_stone_32.png              ← рисовать
    essonite_crystal/
      essonite_crystal_32.png       ← рисовать
    essonite_ore/
      essonite_ore_32.png           ← рисовать (опционально)
```

## Lang / id в игре

| Блок | id |
|------|-----|
| Φ-земля | `effecoria:phi_dirt` |
| Φ-трава | `effecoria:phi_grass` |
| Φ-камень | `effecoria:phi_stone` |
| Кристалл | `effecoria:essonite_crystal` |
| Руда | `effecoria:essonite_ore` |

После любых текстур: **F3+T** в клиенте или пересборка jar.
