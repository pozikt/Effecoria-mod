#!/usr/bin/env python3
"""Generate spell-catalog.canvas.tsx with embedded spell data."""
from __future__ import annotations

import json
from pathlib import Path

DATA = Path(r"C:\Users\2005t\.cursor\projects\c-Users-2005t-Effecoria-mod\canvases\_spell_data.json")
OUT = Path(r"C:\Users\2005t\.cursor\projects\c-Users-2005t-Effecoria-mod\canvases\spell-catalog.canvas.tsx")

data = json.loads(DATA.read_text(encoding="utf-8"))
rows_js = json.dumps(data["rows"], ensure_ascii=False)
counts_js = json.dumps(data["counts"], ensure_ascii=False)

canvas = f"""import {{
  Card,
  CardBody,
  CardHeader,
  Divider,
  H1,
  H2,
  Pill,
  Row,
  Select,
  Spacer,
  Stack,
  Stat,
  Table,
  Text,
  TextInput,
  useCanvasState,
  useHostTheme,
}} from "cursor/canvas";

type SpellRow = {{
  school: string;
  schoolKey: string;
  name: string;
  id: string;
  desc: string;
  cost: number | null;
  hz: number | null;
  category: string;
}};

const COUNTS: Record<string, number> = {counts_js};

const SPELLS: SpellRow[] = {rows_js};

const SCHOOL_OPTIONS = [
  {{ value: "all", label: "Все школы" }},
  ...Object.keys(COUNTS).map((s) => ({{ value: s, label: `${{s}} (${{COUNTS[s]}})` }})),
];

export default function SpellCatalogCanvas() {{
  useHostTheme();
  const [school, setSchool] = useCanvasState("school", "all");
  const [query, setQuery] = useCanvasState("query", "");

  const q = query.trim().toLowerCase();
  const filtered = SPELLS.filter((s) => {{
    if (school !== "all" && s.school !== school) return false;
    if (!q) return true;
    return (
      s.name.toLowerCase().includes(q) ||
      s.id.toLowerCase().includes(q) ||
      s.desc.toLowerCase().includes(q)
    );
  }});

  return (
    <Stack gap={{20}} style={{{{ padding: 20, maxWidth: 1100 }}}}>
      <Stack gap={{6}}>
        <H1>Каталог заклинаний Effecoria</H1>
        <Text tone="secondary">
          Все {{SPELLS.length}} спеллов из data/effecoria/spells с описаниями из ru_ru.json
        </Text>
      </Stack>

      <Row gap={{12}} style={{{{ flexWrap: "wrap" }}}}>
        <Stat value={{String(SPELLS.length)}} label="Всего" />
        {{Object.entries(COUNTS).map(([name, n]) => (
          <Stat key={{name}} value={{String(n)}} label={{name}} />
        ))}}
      </Row>

      <Card>
        <CardHeader trailing={{<Pill tone="info">показано {{filtered.length}}</Pill>}}>
          Фильтр
        </CardHeader>
        <CardBody>
          <Row gap={{12}} style={{{{ flexWrap: "wrap", alignItems: "flex-end" }}}}>
            <Stack gap={{4}} style={{{{ minWidth: 200 }}}}>
              <Text size="small" tone="secondary">
                Школа
              </Text>
              <Select value={{school}} onChange={{setSchool}} options={{SCHOOL_OPTIONS}} />
            </Stack>
            <Stack gap={{4}} style={{{{ flex: 1, minWidth: 220 }}}}>
              <Text size="small" tone="secondary">
                Поиск
              </Text>
              <TextInput
                value={{query}}
                onChange={{setQuery}}
                placeholder="имя, id или описание"
              />
            </Stack>
          </Row>
        </CardBody>
      </Card>

      <Divider />

      <H2>{{school === "all" ? "Все школы" : school}}</H2>

      <Table
        stickyHeader
        columns={{[
          {{ id: "school", header: "Школа", width: 130, sortable: true }},
          {{ id: "name", header: "Название", width: 180, sortable: true }},
          {{ id: "id", header: "ID", width: 150, sortable: true }},
          {{ id: "cost", header: "Ψ", width: 56, align: "right" as const, sortable: true }},
          {{ id: "hz", header: "Гц", width: 64, align: "right" as const, sortable: true }},
          {{ id: "desc", header: "Что делает", sortable: true }},
        ]}}
        rows={{filtered.map((s) => ({{
          key: s.id,
          cells: [s.school, s.name, s.id, s.cost ?? "—", s.hz ?? "—", s.desc],
        }}))}}
      />

      <Spacer />
      <Text size="small" tone="tertiary">
        Источник: src/main/resources/data/effecoria/spells + assets/effecoria/lang/ru_ru.json
      </Text>
    </Stack>
  );
}}
"""

OUT.write_text(canvas, encoding="utf-8")
print(f"Wrote {OUT} ({OUT.stat().st_size} bytes)")
