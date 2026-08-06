# Стеклянная земля (`vitrified_dirt`) — редизайн

## Роль в биоме
Корка **оплавленной почвы**: когда-то dirt, после Φ-вспышки — чёрное стекло с землистыми комками. Это **не** гладкий обсидиан и не песок дюн (`vitrified_sand`).

## Палитра
| Роль | HEX (ориентир) |
|------|----------------|
| База / тень | `#0A0C12` … `#1A1E28` |
| Ультрамариновое стекло | `#1A2A6A` … `#2A4A9A` |
| Циановый блик | `#5EC8FF` (редко) |
| Золотая Φ-искра | `#C9A227` (очень редко) |

## Силуэт
База: ванильный [`dirt.png`](../vanilla_refs/dirt.png) — **оставить комковатость земли**, только перекрасить материал в стекло-почву.

## Файлы
| Файл | Зачем |
|------|--------|
| [`sketches/vitrified_dirt_sketch.png`](../sketches/vitrified_dirt_sketch.png) | Эскиз направления |
| [`for_artist/vitrified_dirt_16x.png`](../for_artist/vitrified_dirt_16x.png) | Рабочий 16×16 (сейчас плейсхолдер в моде) |
| [`for_artist/vitrified_dirt_16x_8x.png`](../for_artist/vitrified_dirt_16x_8x.png) | ×8 для пиксель-арта |
| [`blocks/dirt/vitrified_dirt_old.png`](../blocks/dirt/vitrified_dirt_old.png) | Старая текстура |

## Как править
1. Открой `vitrified_dirt_16x.png` (или эскиз → сведи к 16×16 nearest).
2. Сохрани поверх того же файла **или** положи готовую `vitrified_dirt.png` сюда.
3. Напиши «своди» — упакую в `assets/effecoria/textures/block/vitrified_dirt.png`.

Дальше по очереди: песок → камень → бревно/ветви → трещина.
