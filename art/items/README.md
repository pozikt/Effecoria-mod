# Item textures

Скрипт: `scripts/pack_item_textures.py`

## По умолчанию
Ванильный силуэт + заливка/палитра (колбы, вёдра, пыль, броня, инструменты…).

## Оригинальные эскизы (без ванильной маски)
- `phi_nut`
- `essonite_shard`
- `vitrified_glass_shard`
- `pure_essonite`
- `vitrified_golem_core`

## Перепак
```powershell
python scripts/pack_item_textures.py
```

## Паттерны генерации (в работе)

Итерации по `phi_chitin_helmet` (v1 keeper, v2 отклонена):

1. **Ракурс** = как у ванильного аналога. Шлем: строго фронт на зрителя, L/R симметрия; не ¾, не профиль.
2. **Силуэт/ширина**: ближе к ванильному шлему — достаточно широкий купол; узкий «спартанский» профиль хуже.
3. **Палитра**: мягкий indigo / тёмно-фиолетовый + cyan глаза; не перенасыщенный фиолет и не жирное золото.
4. **Мелкий декор (Φ-линии, кольцо, скулы)**: на 16×16 часто теряется — пока можно игнорировать.
5. **Фон эскиза**: после пака — прозрачный (`alpha` 0/255); шахматка/белый с AI вырезаются.

Keepers:
- шлем: `sketches/item_chitin_helmet_v1.png` → `for_artist/phi_chitin_helmet_v1_16x_8x.png`
- нагрудник: `sketches/item_chitin_chest_v1.png` → `for_artist/phi_chitin_chestplate_v1_16x_8x.png`
- поножи: `sketches/item_chitin_legs_v1.png` → `for_artist/phi_chitin_leggings_v1_16x_8x.png`
- ботинки: `sketches/item_chitin_boots_v1.png` → `for_artist/phi_chitin_boots_v1_16x_8x.png`

## Броня на игроке (entity layers)
Не иконки, а UV на модели: `textures/models/armor/phi_chitin_layer_1.png` (+ `_layer_2` для поножей).  
Скрипт: `scripts/pack_phi_chitin_armor.py` — recolor ванильного iron UV в палитру сета + bake иконок v1.  
Превью: `for_artist/phi_chitin_layer_1_4x.png`, `phi_chitin_layer_2_4x.png`.

