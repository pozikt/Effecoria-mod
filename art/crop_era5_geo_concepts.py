"""Crop Era V geo concept sheets -> 16x16 textures."""
from __future__ import annotations

import json
from collections import deque
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
CONCEPTS = Path(r"C:\Users\2005t\.cursor\projects\c-Users-2005t-Effecoria-mod\assets")
ART = ROOT / "art"
BLOCK = ROOT / "src/main/resources/assets/effecoria/textures/block"
ITEM = ROOT / "src/main/resources/assets/effecoria/textures/item"
MODELS_B = ROOT / "src/main/resources/assets/effecoria/models/block"
MODELS_I = ROOT / "src/main/resources/assets/effecoria/models/item"
STATES = ROOT / "src/main/resources/assets/effecoria/blockstates"
PREVIEW = ROOT / "art/era5_preview"


def punch(img: Image.Image) -> Image.Image:
    img = img.convert("RGBA")
    w, h = img.size
    px = img.load()

    def bg(r, g, b, a):
        return a < 8 or max(r, g, b) <= 18

    vis = [[False] * w for _ in range(h)]
    q: deque[tuple[int, int]] = deque()
    for x in range(w):
        q.append((x, 0))
        q.append((x, h - 1))
    for y in range(h):
        q.append((0, y))
        q.append((w - 1, y))
    while q:
        x, y = q.popleft()
        if not (0 <= x < w and 0 <= y < h) or vis[y][x]:
            continue
        vis[y][x] = True
        r, g, b, a = px[x, y]
        if not bg(r, g, b, a):
            continue
        px[x, y] = (0, 0, 0, 0)
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            q.append((nx, ny))
    return img


def crop_grid(path: Path, cols: int, rows: int, inset: float = 0.05) -> list[Image.Image]:
    img = punch(Image.open(path))
    w, h = img.size
    cw, ch = w // cols, h // rows
    out = []
    for r in range(rows):
        for c in range(cols):
            x0 = int(c * cw + cw * inset)
            y0 = int(r * ch + ch * inset)
            x1 = int((c + 1) * cw - cw * inset)
            y1 = int((r + 1) * ch - ch * inset)
            out.append(img.crop((x0, y0, min(w, x1), min(h, y1))))
    return out


def bbox(img: Image.Image, amin: int = 20):
    px = img.load()
    w, h = img.size
    x0, y0, x1, y1 = w, h, -1, -1
    for y in range(h):
        for x in range(w):
            if px[x, y][3] >= amin:
                x0, y0 = min(x0, x), min(y0, y)
                x1, y1 = max(x1, x), max(y1, y)
    return None if x1 < 0 else (x0, y0, x1 + 1, y1 + 1)


def to_face(cell: Image.Image, opaque: bool = True) -> Image.Image:
    cell = punch(cell.convert("RGBA"))
    box = bbox(cell)
    if not box:
        return Image.new("RGBA", (16, 16), (40, 40, 48, 255 if opaque else 0))
    cropped = cell.crop(box)
    cw, ch = cropped.size
    side = min(cw, ch)
    cropped = cropped.crop(((cw - side) // 2, (ch - side) // 2, (cw - side) // 2 + side, (ch - side) // 2 + side))
    mid = cropped.resize((32, 32), Image.Resampling.BOX)
    face = mid.resize((16, 16), Image.Resampling.NEAREST)
    sp = face.load()
    out = Image.new("RGBA", (16, 16))
    op = out.load()
    samples = [sp[x, y][:3] for y in range(16) for x in range(16) if sp[x, y][3] > 100]
    samples.sort(key=lambda t: sum(t))
    fr, fg, fb = samples[len(samples) // 5] if samples else (30, 32, 38)
    for y in range(16):
        for x in range(16):
            r, g, b, a = sp[x, y]
            if a >= 40:
                op[x, y] = (r, g, b, 255)
            elif opaque:
                op[x, y] = (fr, fg, fb, 255)
            else:
                op[x, y] = (0, 0, 0, 0)
    return out


def to_item(cell: Image.Image) -> Image.Image:
    return to_face(cell, opaque=False)


def write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def cube(name: str, tex: str) -> None:
    write_json(STATES / f"{name}.json", {"variants": {"": {"model": f"effecoria:block/{name}"}}})
    write_json(MODELS_B / f"{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": tex}})
    write_json(MODELS_I / f"{name}.json", {"parent": f"effecoria:block/{name}"})


def cube_orient(name: str, side: str, top: str, front: str | None = None) -> None:
    write_json(
        STATES / f"{name}.json",
        {
            "variants": {
                "facing=north": {"model": f"effecoria:block/{name}"},
                "facing=south": {"model": f"effecoria:block/{name}", "y": 180},
                "facing=west": {"model": f"effecoria:block/{name}", "y": 270},
                "facing=east": {"model": f"effecoria:block/{name}", "y": 90},
            }
        },
    )
    textures = {"particle": side, "down": top, "up": top, "north": front or side, "south": side, "east": side, "west": side}
    write_json(MODELS_B / f"{name}.json", {"parent": "minecraft:block/orientable", "textures": {
        "top": top, "front": front or side, "side": side
    }})
    write_json(MODELS_I / f"{name}.json", {"parent": f"effecoria:block/{name}"})


def item_gen(name: str) -> None:
    write_json(MODELS_I / f"{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"effecoria:item/{name}"}})


def save(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    PREVIEW.mkdir(exist_ok=True)
    img.resize((128, 128), Image.Resampling.NEAREST).save(PREVIEW / f"{path.stem}_x8.png")


def main() -> None:
    ART.mkdir(exist_ok=True)
    for name in (
        "era5_geo_well_faces_sheet.png",
        "era5_climate_array_faces_sheet.png",
        "era5_portal_gate_faces_sheet.png",
        "era5_geo_items_sheet.png",
    ):
        src = CONCEPTS / name
        if src.exists():
            (ART / name).write_bytes(src.read_bytes())

    # Geo well 2x2: side, side_on, top, casing
    g = crop_grid(CONCEPTS / "era5_geo_well_faces_sheet.png", 2, 2)
    save(to_face(g[0]), BLOCK / "geo_well_side.png")
    save(to_face(g[1]), BLOCK / "geo_well_side_on.png")
    save(to_face(g[2]), BLOCK / "geo_well_top.png")
    save(to_face(g[3]), BLOCK / "geo_casing.png")
    # core uses orientable; also copy side as hull BER faces later
    write_json(
        STATES / "geo_well_core.json",
        {
            "variants": {
                "facing=north,lit=false": {"model": "effecoria:block/geo_well_core"},
                "facing=south,lit=false": {"model": "effecoria:block/geo_well_core", "y": 180},
                "facing=west,lit=false": {"model": "effecoria:block/geo_well_core", "y": 270},
                "facing=east,lit=false": {"model": "effecoria:block/geo_well_core", "y": 90},
                "facing=north,lit=true": {"model": "effecoria:block/geo_well_core_on"},
                "facing=south,lit=true": {"model": "effecoria:block/geo_well_core_on", "y": 180},
                "facing=west,lit=true": {"model": "effecoria:block/geo_well_core_on", "y": 270},
                "facing=east,lit=true": {"model": "effecoria:block/geo_well_core_on", "y": 90},
            }
        },
    )
    write_json(
        MODELS_B / "geo_well_core.json",
        {
            "parent": "minecraft:block/orientable",
            "textures": {
                "top": "effecoria:block/geo_well_top",
                "front": "effecoria:block/geo_well_side",
                "side": "effecoria:block/geo_well_side",
            },
        },
    )
    write_json(
        MODELS_B / "geo_well_core_on.json",
        {
            "parent": "minecraft:block/orientable",
            "textures": {
                "top": "effecoria:block/geo_well_top",
                "front": "effecoria:block/geo_well_side_on",
                "side": "effecoria:block/geo_well_side_on",
            },
        },
    )
    write_json(MODELS_I / "geo_well_core.json", {"parent": "effecoria:block/geo_well_core"})
    cube("geo_casing", "effecoria:block/geo_casing")
    write_json(STATES / "geo_well_part.json", {"variants": {"": {"model": "effecoria:block/geo_well_part"}}})
    write_json(
        MODELS_B / "geo_well_part.json",
        {"parent": "minecraft:block/cube_all", "textures": {"all": "effecoria:block/geo_well_side"}},
    )

    # Climate 2x2: side, side_on, top, front
    c = crop_grid(CONCEPTS / "era5_climate_array_faces_sheet.png", 2, 2)
    save(to_face(c[0]), BLOCK / "climate_array_side.png")
    save(to_face(c[1]), BLOCK / "climate_array_side_on.png")
    save(to_face(c[2]), BLOCK / "climate_array_top.png")
    save(to_face(c[3]), BLOCK / "climate_array_front.png")
    write_json(
        STATES / "climate_array.json",
        {
            "variants": {
                "facing=north,lit=false": {"model": "effecoria:block/climate_array"},
                "facing=south,lit=false": {"model": "effecoria:block/climate_array", "y": 180},
                "facing=west,lit=false": {"model": "effecoria:block/climate_array", "y": 270},
                "facing=east,lit=false": {"model": "effecoria:block/climate_array", "y": 90},
                "facing=north,lit=true": {"model": "effecoria:block/climate_array_on"},
                "facing=south,lit=true": {"model": "effecoria:block/climate_array_on", "y": 180},
                "facing=west,lit=true": {"model": "effecoria:block/climate_array_on", "y": 270},
                "facing=east,lit=true": {"model": "effecoria:block/climate_array_on", "y": 90},
            }
        },
    )
    write_json(
        MODELS_B / "climate_array.json",
        {
            "parent": "minecraft:block/orientable",
            "textures": {
                "top": "effecoria:block/climate_array_top",
                "front": "effecoria:block/climate_array_front",
                "side": "effecoria:block/climate_array_side",
            },
        },
    )
    write_json(
        MODELS_B / "climate_array_on.json",
        {
            "parent": "minecraft:block/orientable",
            "textures": {
                "top": "effecoria:block/climate_array_top",
                "front": "effecoria:block/climate_array_front",
                "side": "effecoria:block/climate_array_side_on",
            },
        },
    )
    write_json(MODELS_I / "climate_array.json", {"parent": "effecoria:block/climate_array"})

    # Portal 2x2: frame side, frame top, active, inactive
    p = crop_grid(CONCEPTS / "era5_portal_gate_faces_sheet.png", 2, 2)
    save(to_face(p[0]), BLOCK / "portal_gate_side.png")
    save(to_face(p[1]), BLOCK / "portal_gate_top.png")
    save(to_face(p[2]), BLOCK / "portal_gate_active.png")
    save(to_face(p[3]), BLOCK / "portal_gate_inactive.png")
    write_json(
        STATES / "portal_gate.json",
        {
            "variants": {
                "facing=north,active=false": {"model": "effecoria:block/portal_gate"},
                "facing=south,active=false": {"model": "effecoria:block/portal_gate", "y": 180},
                "facing=west,active=false": {"model": "effecoria:block/portal_gate", "y": 270},
                "facing=east,active=false": {"model": "effecoria:block/portal_gate", "y": 90},
                "facing=north,active=true": {"model": "effecoria:block/portal_gate_active"},
                "facing=south,active=true": {"model": "effecoria:block/portal_gate_active", "y": 180},
                "facing=west,active=true": {"model": "effecoria:block/portal_gate_active", "y": 270},
                "facing=east,active=true": {"model": "effecoria:block/portal_gate_active", "y": 90},
            }
        },
    )
    write_json(
        MODELS_B / "portal_gate.json",
        {
            "parent": "minecraft:block/orientable",
            "textures": {
                "top": "effecoria:block/portal_gate_top",
                "front": "effecoria:block/portal_gate_inactive",
                "side": "effecoria:block/portal_gate_side",
            },
        },
    )
    write_json(
        MODELS_B / "portal_gate_active.json",
        {
            "parent": "minecraft:block/orientable",
            "textures": {
                "top": "effecoria:block/portal_gate_top",
                "front": "effecoria:block/portal_gate_active",
                "side": "effecoria:block/portal_gate_side",
            },
        },
    )
    write_json(MODELS_I / "portal_gate.json", {"parent": "effecoria:block/portal_gate"})

    # Items 2x2
    it = crop_grid(CONCEPTS / "era5_geo_items_sheet.png", 2, 2, inset=0.08)
    save(to_item(it[0]), ITEM / "deep_phi_catalyst.png")
    save(to_item(it[1]), ITEM / "geo_well_core.png")
    save(to_item(it[2]), ITEM / "climate_array.png")
    save(to_item(it[3]), ITEM / "portal_gate.png")
    item_gen("deep_phi_catalyst")
    # Prefer block parents for machine items when placed as BlockItem; keep item icons as fallback
    write_json(MODELS_I / "deep_phi_catalyst.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "effecoria:item/deep_phi_catalyst"}})

    print("era5 textures OK")


if __name__ == "__main__":
    main()
