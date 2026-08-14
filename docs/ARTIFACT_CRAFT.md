# Artifact craft (Era III)

Modular assembly of staves and jewelry, item seals for Seals school, Curios accessory slots.

**Shipped MVP** is below. **Reform (planned)** expands the same stations into full weapon families (melee + ranged) with material-composited textures — see [Reform — weapon families](#reform--weapon-families-planned). True freeform artifacts (spatial pocket, soulbind, self-aware) stay a later track.

## Player loop (shipped)

1. **Shaft Lathe** (`shaft_lathe`) — material from `#effecoria:shaft_materials` + **length form** → `carved_shaft` (MED heat).
2. **Facet Cutter** (`facet_cutter`) — material from `#effecoria:focus_materials` + cut → `faceted_focus`.
3. **Phonemes** — `phi_phoneme_*` on shaft/focus (or essonite armor) before assemble.
4. **Artifact Assembler** — staff (shaft+focus) or jewelry (band+gem) → `modular_staff` / `assembled_*`.
5. **Seal Inscriber** — Seals school + known item seals → bind like enchantments; Strip removes them.
6. **Curios** — ring×2, amulet×1, charm×1 for jewelry. Open the **Curios** panel from the button next to your survival inventory (requires the Curios mod, bundled in dev via Gradle). Datapack path: `data/effecoria/curios/`.

## Shaft length (shipped)

Forms are physical lengths (meters). Lathe UI cycles them shortest → longest:

| Form | Length | Notes |
|------|--------|--------|
| `wand` | 0.6 m | Short casting stick |
| `baton` | 1.0 m | Standard staff |
| `long_staff` | 1.4 m | Extended reach |
| `stature` | 1.8 m | About player height |

Longer shafts raise reach and slightly raise cast cost; material conductivity is independent.

**Reform note:** for melee weapon shafts, length also **raises melee cooldown** (trade-off with reach). Casting staves may keep cost-only penalty; datapack per family.

Item models use scaled vanilla stick / blaze rod textures per form (`custom_model_data` 1–4). Regenerate PNGs with `scripts/gen_carved_shaft_textures.py` after changing length tables.

**Lathe & facet cutter GUI** use the vanilla stonecutter panel (176×166): input `(20,33)`, output `(143,33)`, **variant pool** as a 4-wide icon grid at `(52,14)` (click to select; invalid options dimmed when input material is wrong).

**Block art:** Generate `art/artifact_stations/face_sheet.png` (5×4 face atlas) and optional `concept_blocks.png` (isometric reference), then bake:

`python scripts/bake_artifact_station_textures.py`

Do **not** draw texels in Python — crops from the concept sheet and downscales to 16×16. Preview: `art/artifact_stations/baked_preview.png`. Shaft lathe & facet cutter are **half-height** benches; rotate with block `facing` like a stonecutter.

**Artifact assembler** uses the mortar panel; the same grid picks **staff / ring / amulet / charm**; craft slots at `(44,35)`, `(80,35)`, output `(134,35)`. Reform expands the family grid (see below).

## Φ-conductivity

Each craft material has a datapack conductivity `0..1` under `data/effecoria/artifact/materials/`.

- Stamped onto carved parts and merged onto assembled gear (shaft 55% + focus 45%; jewelry 50/50).
- **Staff:** higher conductivity → lower cast cost, higher spell power.
- **Jewelry:** scales Curios Φ-shield bonus.

Examples: stick/planks ~0.2, copper/gold high, star essonite ~0.95, lead charm low (damper).

## Datapack

| Path | Content |
|------|---------|
| `data/effecoria/artifact/shaft_forms/` | Length profiles + reach/cost |
| `data/effecoria/artifact/materials/` | Item → Φ-conductivity |
| `data/effecoria/artifact/focus_cuts/` | Cuts + power/tier |
| `data/effecoria/artifact/assemble_recipes/` | staff/ring/amulet/charm |
| `data/effecoria/item_seals/` | Vanilla-analogue + Effecoria seals |

## Code

- `com.effecoria.core.artifact` — catalogs, NBT, `MaterialConductivity`, `StaffStats`
- Stations — `ArtifactStationBlock` + BE/menu/screen
- `ItemSealEvents` — combat/armor/mend hooks
- Discovery — `PlayerPsiData.knownItemSeals` (+ `item_seal_primer`)

Jewelry blanks: craft `jewelry_band` / `jewelry_gem`, then assemble. Prebuilt `gold_amulet`, `essonite_ring`, `star_amulet`, `phi_band`, `lead_charm` also wear in Curios.

---

## Reform — weapon families (planned)

**Goal:** same craft loop (carve parts → phonemes → assemble), but **full choice of body/shaft, tip, and (for guns) breech/magazine**, with **composited textures** from part materials. Diversify beyond staff + jewelry.

### Families (assembler modes)

| Family | Required parts | Optional | Role |
|--------|----------------|----------|------|
| **Staff / wand** (shipped) | древко + фокус | фонемы | каст |
| **Melee pole** | древко + **наконечник** | фонемы на лезвии/древке | ближний бой |
| **Ranged Φ-weapon** | **корпус** + **ствол/проводники** + **затвор** | **магазин**, фонемы на стволе | стрельба Φ-пулями / снарядами |
| **Jewelry** (shipped) | обод + камень | фонемы | Curios |
| **Artifact** (later) | форма + украшения + ядро | душа / печать хозяина | утилиты, не «просто статстик» |

Assembler UI: expand family grid beyond staff/ring/amulet/charm.

### Древко / корпус (полная свобода материала)

- Lathe (or body bench) принимает любой материал из расширенных тегов: эссенитовое дерево, мифрил, свинец, кости, Φ-сталь, …
- Игрок выбирает **форму длины** (и, для оружия, **профиль**: прямое древко, топорище, приклад+цевьё как «корпус» стрелкового).
- **Длина древка (melee):** ↑ дальность ближней атаки, ↑ кулдаун удара. Casting staff может оставить ↑ cost вместо кулдауна (datapack `family`).
- Итоговая текстура оружия = слои частей (корпус из эссенитового дерева + мифриловый ствол / лезвие).

### Наконечники (melee)

Отдельная деталь (cutter / smith die), не только «фокус»:

| Tip profile | Результат сборки |
|-------------|------------------|
| копьё / пика | укол, макс. reach за длину |
| топор / секира | рубящий, больше урон / меньше reach |
| алебарда | гибрид (datapack flags) |
| клинок / гладиус на коротком древке | меч-стафф |
| молот / булава | ударный, броня / stagger |

На **лезвие/наконечник** и на древко наносят фонемы (как сейчас на shaft/focus): например негативный эффект на попадание (`umbra`, яд, замедление — конкретный список расширяет `phi_phoneme_*` / item seals).

### Стрелковое оружие

Минимальный набор:

| Часть | Пример | Роль |
|-------|--------|------|
| Корпус | эссенитовое дерево | силуэт, вес, часть текстуры |
| Проводники / ствол | мифрил | канал Φ-выстрела, conductivity |
| Затвор | металл / механизм | слот **пули** (один выстрел или цикл) |
| Магазин (опц.) | — | запас пуль без перезарядки каждой вручную |
| Фонема на стволе | напр. «толчок» | поведение снаряда (отброс, пробитие, …) |

Пули — отдельные расходники (позже: материя / Φ-заряд / патрон с фокусом). Без магазина: затвор принимает одну пулю. С магазином: ёмкость из datapack материала магазина.

### Текстуры

- Не одна статичная иконка на «modular_gun».
- Слои: body material × tip/barrel material × optional trim; length/form масштабирует модель (как нынешние shaft CMD 1–4, расширить).
- Vanilla-recolor / crop from wood + metal bases where possible (workspace texture rule).

### Фонемы vs печати предмета

| Слой | Когда | Пример |
|------|--------|--------|
| Φ-фонема на детали | до сборки | ствол «толчок», лезвие «umbra» |
| Item seal (inscriber) | после сборки | как нынешние seals на готовом предмете |

Оба остаются; фонема «вшита» в геометрию части, печать — ритуал на готовом оружии/аксессуаре.

### Баланс-крючки (datapack)

| Параметр | Влияет |
|----------|--------|
| `length_m` | melee reach, melee cooldown (и/или cast cost) |
| `conductivity` | стоимость выстрела / сила каста |
| `tip_profile` | тип удара, множители |
| `breech` / `mag_cap` | скорострельность, ёмкость |
| phoneme ids | on-hit / on-shot эффекты |

---

## Artifacts — later track (not MVP reform)

Артефакты — **не** то же самое, что оружие: полная свобода действий, отдельный дизайн.

**Ближайший слой (после weapon families):**

- Широкий выбор **форм** и **украшений** с полезными свойствами (не только статы).
- Примеры свойств: **пространственный карман**, **привязка к владельцу** (soulbind / theft-proof).
- Украшения как слоты модификаторов на корпусе артефакта.

**Перспектива:**

- Артефакт или оружие с **самосознанием** (Ψ-отпечаток, диалог, условия верности) — после tower/imprinter maturity; не блокирует reform оружия.

Пока weapon-family reform не требует самосознания.

---

## Implementation sketch (when coding)

1. Datapack: `weapon_families/`, `tip_profiles/`, `gun_parts/` (body, barrel, breech, mag); extend `shaft_forms` with `melee_cooldown`.
2. Assembler: family picker + 2–4 part slots by family.
3. NBT: part materials + phonemes → merged stats + layered client model.
4. Combat hooks: melee reach/cooldown from shaft; projectile from breech + barrel phoneme.
5. Keep jewelry/staff recipes working; migrate `modular_staff` into family `staff`.
