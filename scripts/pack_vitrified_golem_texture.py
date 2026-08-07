"""
Build Vitrified Golem edit kit + pack faces into game atlas.

Run:
  python scripts/pack_vitrified_golem_texture.py --from-nets   # nets/ -> faces/ -> atlas
  python scripts/pack_vitrified_golem_texture.py               # pack existing faces/ only
  python scripts/pack_vitrified_golem_texture.py --init        # create missing face templates
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "art" / "vitrified_wastes" / "golem"
FACES = ART / "faces"
GAME_TEX = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "entity" / "vitrified_golem.png"
GEO = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "geo" / "vitrified_golem.geo.json"
ART_GEO = ART / "vitrified_golem.geo.json"
ART_ANIM = ART / "vitrified_golem.animation.json"

ATLAS = 128
SCALE = 8  # edit previews

# Minecraft box UV for size (w,h,d) at (u,v):
#   top    (u+d,     v)     w×d
#   bottom (u+d+w,   v)     w×d
#   right  (u,       v+d)   d×h   (+X)
#   front  (u+d,     v+d)   w×h   (-Z)  = лицо
#   left   (u+d+w,   v+d)   d×h   (-X)
#   back   (u+2*d+w, v+d)   w×h   (+Z)  = затылок


def box_faces(u: int, v: int, w: int, h: int, d: int) -> dict[str, tuple[int, int, int, int]]:
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }


# name -> (u, v, w, h, d, bone, cube_index, colors base/accent)
PARTS: dict[str, dict] = {
    "head": {
        "uv": (0, 0),
        "size": (7, 7, 7),
        "bone": "head",
        "cube": 0,
        "title_ru": "Голова",
        "base": (18, 18, 36),
        "accent": (0, 210, 255),
    },
    "body": {
        "uv": (32, 0),
        "size": (8, 10, 5),
        "bone": "body",
        "cube": 0,
        "title_ru": "Торс",
        "base": (22, 33, 62),
        "accent": (255, 215, 0),
    },
    "shoulder": {
        "uv": (64, 0),
        "size": (9, 3, 6),
        "bone": "body",
        "cube": 1,
        "title_ru": "Плечи / панцирь",
        "base": (15, 20, 40),
        "accent": (200, 170, 40),
    },
    "heart": {
        "uv": (96, 0),
        "size": (3, 3, 2),
        "bone": "heart",
        "cube": 0,
        "title_ru": "Сердцевина",
        "base": (30, 60, 180),
        "accent": (0, 210, 255),
    },
    "eye": {
        "uv": (112, 0),
        "size": (3, 3, 2),
        "bone": "eye",
        "cube": 0,
        "title_ru": "Глаз (кристалл)",
        "base": (0, 180, 220),
        "accent": (255, 255, 255),
    },
    "right_arm": {
        "uv": (0, 20),
        "size": (3, 14, 3),
        "bone": "right_arm",
        "cube": 0,
        "title_ru": "Правая рука",
        "base": (12, 12, 28),
        "accent": (80, 90, 120),
    },
    "left_arm": {
        "uv": (16, 20),
        "size": (3, 14, 3),
        "bone": "left_arm",
        "cube": 0,
        "title_ru": "Левая рука",
        "base": (12, 12, 28),
        "accent": (80, 90, 120),
    },
    "right_blade": {
        "uv": (64, 66),
        "size": (8, 12, 8),
        "bone": "right_arm",
        "cube": 1,
        "title_ru": "Правое лезвие локтя",
        "base": (8, 8, 18),
        "accent": (0, 160, 200),
        "edit_scale": 8,
    },
    "left_blade": {
        "uv": (64, 88),
        "size": (8, 12, 8),
        "bone": "left_arm",
        "cube": 1,
        "title_ru": "Левое лезвие локтя",
        "base": (8, 8, 18),
        "accent": (0, 160, 200),
        "edit_scale": 8,
    },
    "right_leg": {
        "uv": (0, 48),
        "size": (3, 18, 3),
        "bone": "right_leg",
        "cube": 0,
        "title_ru": "Правая нога",
        "base": (14, 14, 30),
        "accent": (60, 70, 100),
    },
    "left_leg": {
        "uv": (16, 48),
        "size": (3, 18, 3),
        "bone": "left_leg",
        "cube": 0,
        "title_ru": "Левая нога",
        "base": (14, 14, 30),
        "accent": (60, 70, 100),
    },
    "right_foot": {
        "uv": (32, 48),
        "size": (4, 2, 4),
        "bone": "right_leg",
        "cube": 1,
        "title_ru": "Правая ступня",
        "base": (10, 10, 22),
        "accent": (90, 80, 40),
    },
    "left_foot": {
        "uv": (48, 48),
        "size": (4, 2, 4),
        "bone": "left_leg",
        "cube": 1,
        "title_ru": "Левая ступня",
        "base": (10, 10, 22),
        "accent": (90, 80, 40),
    },
}

FACE_LABELS_RU = {
    "front": "лицо",
    "back": "затылок",
    "left": "бок_L",
    "right": "бок_R",
    "top": "верхушка",
    "bottom": "низ",
}

# Parts where outer near-black padding should become alpha (silhouettes).
# Only blades get edge flood → alpha (irregular glass shards).
# Arms/legs/body are full black-glass plates — do not eat their fill.
SILHOUETTE_PARTS = {
    "right_blade",
    "left_blade",
}


def face_dims(w: int, h: int, d: int) -> dict[str, tuple[int, int]]:
    return {
        "top": (w, d),
        "bottom": (w, d),
        "right": (d, h),
        "front": (w, h),
        "left": (d, h),
        "back": (w, h),
    }


def net_layout(w: int, h: int, d: int) -> dict[str, tuple[int, int, int, int]]:
    """Return face -> (x, y, fw, fh) in net pixel coords (SCALE applied)."""
    pad = 2
    cell_w = max(w, d) + pad
    cell_h = max(h, d) + pad
    dims = face_dims(w, h, d)
    cells = {
        "top": (1, 0),
        "left": (0, 1),
        "front": (1, 1),
        "right": (2, 1),
        "back": (3, 1),
        "bottom": (1, 2),
    }
    out = {}
    for face, (cx, cy) in cells.items():
        fw, fh = dims[face]
        x = (cx * cell_w + 4) * SCALE
        y = (cy * cell_h + 24) * SCALE
        out[face] = (x, y, fw * SCALE, fh * SCALE)
    return out


def _luma(px: tuple[int, int, int, int]) -> float:
    r, g, b, a = px
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def clean_face_alpha(im: Image.Image, *, silhouette: bool) -> Image.Image:
    """Preserve artist alpha; key canvas leftovers; optional edge flood for silhouettes."""
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    canvas = (12, 12, 20)

    # 1) Exact / near canvas background -> transparent
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            if abs(r - canvas[0]) <= 4 and abs(g - canvas[1]) <= 4 and abs(b - canvas[2]) <= 4:
                px[x, y] = (0, 0, 0, 0)

    if not silhouette:
        # Snap soft alpha to hard cutout (Minecraft-friendly)
        for y in range(h):
            for x in range(w):
                r, g, b, a = px[x, y]
                if 0 < a < 128:
                    px[x, y] = (0, 0, 0, 0)
                elif a >= 128 and a < 255:
                    px[x, y] = (r, g, b, 255)
        return im

    # 2) Flood from edges: near-black, low-chroma padding -> alpha
    from collections import deque

    visited = [[False] * w for _ in range(h)]
    q: deque[tuple[int, int]] = deque()

    def is_pad(x: int, y: int) -> bool:
        r, g, b, a = px[x, y]
        if a < 16:
            return True
        if _luma((r, g, b, a)) > 22:
            return False
        # keep gold / cyan emissive even if dark-ish neighbors
        if b > r + 25 or (r > 80 and g > 40 and b < 60):
            return False
        # low chroma black/indigo glass padding at borders
        return max(r, g, b) - min(r, g, b) < 28

    for x in range(w):
        q.append((x, 0))
        q.append((x, h - 1))
    for y in range(h):
        q.append((0, y))
        q.append((w - 1, y))

    kill: list[tuple[int, int]] = []
    while q:
        x, y = q.popleft()
        if x < 0 or y < 0 or x >= w or y >= h or visited[y][x]:
            continue
        visited[y][x] = True
        if not is_pad(x, y):
            continue
        kill.append((x, y))
        q.extend(((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)))

    for x, y in kill:
        px[x, y] = (0, 0, 0, 0)

    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if 0 < a < 128:
                px[x, y] = (0, 0, 0, 0)
            elif a >= 128 and a < 255:
                px[x, y] = (r, g, b, 255)
    return im


def solidify_plate_face(im: Image.Image, base: tuple[int, int, int]) -> Image.Image:
    """Fill interior holes on limb/torso plates — opaque glass, not see-through texels."""
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    for _ in range(w * h + 4):
        changed = False
        for y in range(h):
            for x in range(w):
                if px[x, y][3] > 0:
                    continue
                rs = gs = bs = n = 0
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h and px[nx, ny][3] > 0:
                        r, g, b, _a = px[nx, ny]
                        rs += r
                        gs += g
                        bs += b
                        n += 1
                if n:
                    px[x, y] = (rs // n, gs // n, bs // n, 255)
                    changed = True
        if not changed:
            break
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                px[x, y] = (*base, 255)
            elif a < 255:
                px[x, y] = (r, g, b, 255)
    return im


def repair_atlas_plates(atlas: Image.Image) -> Image.Image:
    """Solidify every non-silhouette part face in a packed 128×128 atlas."""
    atlas = atlas.convert("RGBA").copy()
    for part, meta in PARTS.items():
        if part in SILHOUETTE_PARTS:
            continue
        u, v = meta["uv"]
        w, h, d = meta["size"]
        base = meta["base"]
        for _face, (fu, fv, fw, fh) in box_faces(u, v, w, h, d).items():
            crop = atlas.crop((fu, fv, fu + fw, fv + fh))
            fixed = solidify_plate_face(crop, base)
            atlas.paste(fixed, (fu, fv), fixed)
    return atlas


def extract_faces_from_atlas(atlas: Image.Image) -> None:
    """Write faces/* from a packed atlas (for re-edit after repair)."""
    FACES.mkdir(parents=True, exist_ok=True)
    for part, meta in PARTS.items():
        u, v = meta["uv"]
        w, h, d = meta["size"]
        for face, (fu, fv, fw, fh) in box_faces(u, v, w, h, d).items():
            crop = atlas.crop((fu, fv, fu + fw, fv + fh))
            crop.save(face_path(part, face, edit=False))


def unpack_nets_to_faces(*, backup: bool = True) -> int:
    """Extract native faces from artist-edited nets/ into faces/."""
    import shutil
    from datetime import datetime

    nets = ART / "nets"
    if not nets.exists():
        raise SystemExit("nets/ folder missing")

    if backup and FACES.exists() and any(FACES.glob("*.png")):
        stamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        bak = ART / f"faces_backup_before_unpack_{stamp}"
        shutil.copytree(FACES, bak)
        print(f"Backup faces -> {bak}")

    FACES.mkdir(parents=True, exist_ok=True)
    count = 0
    for part, meta in PARTS.items():
        net_path = nets / f"{part}_net.png"
        if not net_path.exists():
            print(f"skip missing net: {part}")
            continue
        net = Image.open(net_path).convert("RGBA")
        w, h, d = meta["size"]
        layout = net_layout(w, h, d)
        edit_scale = meta.get("edit_scale", SCALE)
        silhouette = part in SILHOUETTE_PARTS
        for face, (x, y, bw, bh) in layout.items():
            crop = net.crop((x, y, x + bw, y + bh))
            fw, fh = face_dims(w, h, d)[face]
            native = crop.resize((fw, fh), Image.NEAREST)
            native = clean_face_alpha(native, silhouette=silhouette)
            native.save(face_path(part, face, edit=False))
            # refresh edit preview from cleaned native
            big = native.resize((fw * edit_scale, fh * edit_scale), Image.NEAREST)
            sheet = Image.new("RGBA", (big.width, big.height + 18), (20, 20, 28, 255))
            sheet.paste(big, (0, 18), big)
            draw = ImageDraw.Draw(sheet)
            draw.text(
                (2, 2),
                f"{meta['title_ru']} — {FACE_LABELS_RU[face]} ({fw}x{fh})",
                fill=(220, 220, 230),
            )
            sheet.save(face_path(part, face, edit=True))
            count += 1
        print(f"unpacked {part}")
    print(f"Unpacked {count} faces from nets")
    return count


def face_path(part: str, face: str, edit: bool = False) -> Path:
    suffix = "_edit" if edit else ""
    return FACES / f"{part}__{face}{suffix}.png"


def make_template_face(w: int, h: int, base: tuple, accent: tuple, label: str) -> Image.Image:
    im = Image.new("RGBA", (w, h), (*base, 255))
    d = ImageDraw.Draw(im)
    # border + crosshair so orientation is obvious
    d.rectangle([0, 0, w - 1, h - 1], outline=(*accent, 255))
    if w > 2 and h > 2:
        d.point([(1, 1), (w - 2, 1)], fill=(*accent, 255))
        # top-left marker = "up" for side faces
        d.rectangle([0, 0, max(0, min(1, w - 1)), max(0, min(1, h - 1))], fill=(255, 80, 80, 255))
    return im


def init_faces(only: list[str] | None = None, *, force: bool = False) -> None:
    """Create missing face templates. Never overwrite existing artwork unless force=True."""
    import shutil
    from datetime import datetime

    FACES.mkdir(parents=True, exist_ok=True)
    if force:
        stamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        bak = ART / f"faces_backup_before_init_{stamp}"
        if FACES.exists() and any(FACES.glob("*.png")):
            shutil.copytree(FACES, bak)
            print(f"Backup before --init --force -> {bak}")

    written = 0
    skipped = 0
    for part, meta in PARTS.items():
        if only is not None and part not in only:
            continue
        u, v = meta["uv"]
        w, h, d = meta["size"]
        faces = box_faces(u, v, w, h, d)
        edit_scale = meta.get("edit_scale", SCALE)
        for face, (fu, fv, fw, fh) in faces.items():
            native_path = face_path(part, face, edit=False)
            edit_path = face_path(part, face, edit=True)
            if not force and native_path.exists():
                skipped += 1
                continue
            label = f"{part}/{FACE_LABELS_RU[face]}"
            native = make_template_face(fw, fh, meta["base"], meta["accent"], label)
            native.save(native_path)
            big = native.resize((fw * edit_scale, fh * edit_scale), Image.NEAREST)
            # caption strip
            sheet = Image.new("RGBA", (big.width, big.height + 18), (20, 20, 28, 255))
            sheet.paste(big, (0, 18))
            draw = ImageDraw.Draw(sheet)
            draw.text(
                (2, 2),
                f"{meta['title_ru']} — {FACE_LABELS_RU[face]} ({fw}x{fh} native, x{edit_scale} edit)",
                fill=(220, 220, 230),
            )
            sheet.save(edit_path)
            written += 1
    print(f"Wrote {written} templates, skipped {skipped} existing -> {FACES}")


def pack_atlas() -> Image.Image:
    atlas = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))
    missing = []
    for part, meta in PARTS.items():
        u, v = meta["uv"]
        w, h, d = meta["size"]
        edit_scale = meta.get("edit_scale", SCALE)
        faces = box_faces(u, v, w, h, d)
        for face, (fu, fv, fw, fh) in faces.items():
            # Prefer native; if only edit exists, downscale
            native = face_path(part, face, edit=False)
            edit = face_path(part, face, edit=True)
            if native.exists():
                im = Image.open(native).convert("RGBA")
            elif edit.exists():
                im = Image.open(edit).convert("RGBA")
                # strip caption if present
                if im.height == fh * edit_scale + 18:
                    im = im.crop((0, 18, im.width, im.height))
                elif im.height > fh + 18:
                    im = im.crop((0, 18, im.width, im.height))
                im = im.resize((fw, fh), Image.NEAREST)
            else:
                missing.append(f"{part}/{face}")
                continue
            if im.size != (fw, fh):
                im = im.resize((fw, fh), Image.NEAREST)
            if part not in SILHOUETTE_PARTS:
                im = solidify_plate_face(im, meta["base"])
            atlas.paste(im, (fu, fv), im)
    if missing:
        print("MISSING faces:", ", ".join(missing))
    return atlas


def write_part_nets(*, force: bool = False) -> None:
    """Classic cross unfold per part: top / left-front-right-back / bottom.

    Never overwrites an existing net unless force=True (artist may paint on nets).
    """
    nets = ART / "nets"
    nets.mkdir(parents=True, exist_ok=True)
    for part, meta in PARTS.items():
        out = nets / f"{part}_net.png"
        if out.exists() and not force:
            print(f"skip existing net (keep artwork): {out.name}")
            continue
        faces = {}
        for face in FACE_LABELS_RU:
            p = face_path(part, face, edit=False)
            if not p.exists():
                continue
            faces[face] = Image.open(p).convert("RGBA")
        if len(faces) < 6:
            continue
        w, h, d = meta["size"]
        pad = 2
        cell_w = max(w, d) + pad
        cell_h = max(h, d) + pad
        canvas_w = cell_w * 4 + 8
        canvas_h = cell_h * 3 + 40
        canvas = Image.new("RGBA", (canvas_w * SCALE, canvas_h * SCALE), (12, 12, 20, 255))
        draw = ImageDraw.Draw(canvas)

        def paste(face: str, cx: int, cy: int) -> None:
            im = faces[face]
            big = im.resize((im.width * SCALE, im.height * SCALE), Image.NEAREST)
            x = (cx * cell_w + 4) * SCALE
            y = (cy * cell_h + 24) * SCALE
            canvas.paste(big, (x, y), big)
            draw.text((x, y - 14), FACE_LABELS_RU[face], fill=(220, 230, 255))

        paste("top", 1, 0)
        paste("left", 0, 1)
        paste("front", 1, 1)
        paste("right", 2, 1)
        paste("back", 3, 1)
        paste("bottom", 1, 2)
        draw.text((8, 4), f"{meta['title_ru']} ({part}) — развёртка", fill=(255, 215, 0))
        canvas.save(out)
        print(f"wrote {out.name}")
    print(f"Nets folder: {nets}")


def write_labeled_guide(atlas: Image.Image) -> None:
    guide = atlas.resize((ATLAS * 4, ATLAS * 4), Image.NEAREST).convert("RGBA")
    overlay = Image.new("RGBA", guide.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    for part, meta in PARTS.items():
        u, v = meta["uv"]
        w, h, d = meta["size"]
        # box footprint
        bw, bh = 2 * (w + d), d + h
        x0, y0 = u * 4, v * 4
        x1, y1 = (u + bw) * 4 - 1, (v + bh) * 4 - 1
        draw.rectangle([x0, y0, x1, y1], outline=(255, 215, 0, 200))
        draw.text((x0 + 2, y0 + 2), meta["title_ru"], fill=(255, 255, 255, 255))
        faces = box_faces(u, v, w, h, d)
        for face, (fu, fv, fw, fh) in faces.items():
            draw.rectangle(
                [fu * 4, fv * 4, (fu + fw) * 4 - 1, (fv + fh) * 4 - 1],
                outline=(0, 210, 255, 160),
            )
            draw.text((fu * 4 + 1, fv * 4 + 1), FACE_LABELS_RU[face][:3], fill=(180, 230, 255, 220))
    out = Image.alpha_composite(guide, overlay)
    out.save(ART / "uv_guide_labeled.png")
    print("Wrote", ART / "uv_guide_labeled.png")


def update_geo_uv() -> None:
    geo = json.loads(GEO.read_text(encoding="utf-8"))
    geom = geo["minecraft:geometry"][0]
    geom["description"]["texture_width"] = ATLAS
    geom["description"]["texture_height"] = ATLAS
    bones = {b["name"]: b for b in geom["bones"]}

    # Normalize eye to integer cube for clean UV (keep visual size close)
    eye_bone = bones["eye"]
    eye_bone["cubes"][0]["size"] = [3, 3, 2]
    eye_bone["cubes"][0]["origin"] = [-1.5, 30.2, -4.5]

    for part, meta in PARTS.items():
        bone = bones.get(meta["bone"])
        if bone is None:
            continue
        cubes = bone.get("cubes") or []
        idx = meta["cube"]
        if idx >= len(cubes):
            continue
        cube = cubes[idx]
        u, v = meta["uv"]
        cube["uv"] = [u, v]

    # Explicit size sync where UV assumes integers (already are)
    GEO.write_text(json.dumps(geo, indent=2), encoding="utf-8")
    ART_GEO.write_text(json.dumps(geo, indent=2), encoding="utf-8")
    print("Updated geo UV -> 128x128 layout")


def write_readme() -> None:
    lines = [
        "# Vitrified Golem — текстурный кит",
        "",
        "Рисовать удобнее по **отдельным граням**, потом собрать атлас скриптом.",
        "",
        "## Как править",
        "1. Открой файлы в `faces/`.",
        "2. Для удобства есть `*__*_edit.png` (×8 + подпись).",
        "3. Итоговая грань для сборки — файл **без** `_edit` (нативный размер).",
        "4. После правок: `python scripts/pack_vitrified_golem_texture.py`",
        "",
        "## Имена граней",
        "| EN | RU |",
        "|----|----|",
        "| front | лицо |",
        "| back | затылок |",
        "| left | бок слева (−X) |",
        "| right | бок справа (+X) |",
        "| top | верхушка |",
        "| bottom | низ |",
        "",
        "## Части",
    ]
    for part, meta in PARTS.items():
        w, h, d = meta["size"]
        u, v = meta["uv"]
        lines.append(f"- **{part}** — {meta['title_ru']} ({w}×{h}×{d}), UV ({u},{v})")
    lines += [
        "",
        "## Файлы",
        "- `faces/*` — отдельные грани",
        "- `uv_guide_labeled.png` — карта атласа 128×128 ×4",
        "- `vitrified_golem.png` — собранный атлас (копия игрового)",
        "",
        "## UV и размер грани",
        "В Minecraft/GeckoLib **1 unit куба = 1 пиксель** на соответствующей грани.",
        "Чтобы лезвие было крупнее — увеличен куб `8×12×8` (лицо **8×12** px, в `_edit` **64×96**).",
        "",
        "Красная точка в углу грани = верх/ориентир.",
    ]
    (ART / "TEXTURE_EDIT.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--init", action="store_true", help="Create missing face templates (never overwrites)")
    ap.add_argument(
        "--force",
        action="store_true",
        help="With --init: overwrite existing faces (auto-backup first)",
    )
    ap.add_argument(
        "--from-nets",
        action="store_true",
        help="Unpack artist nets/ into faces/, clean alpha, then pack atlas",
    )
    ap.add_argument(
        "--repair-atlas",
        action="store_true",
        help="Fill holes on solid body faces in the current game atlas, re-export faces/",
    )
    ap.add_argument(
        "--only",
        type=str,
        default="",
        help="Comma-separated part names (e.g. right_blade,left_blade)",
    )
    args = ap.parse_args()
    only = [p.strip() for p in args.only.split(",") if p.strip()] or None

    FACES.mkdir(parents=True, exist_ok=True)
    update_geo_uv()
    if args.repair_atlas:
        if not GAME_TEX.exists():
            raise SystemExit(f"Missing game atlas: {GAME_TEX}")
        atlas = repair_atlas_plates(Image.open(GAME_TEX))
        atlas.save(GAME_TEX)
        atlas.save(ART / "vitrified_golem.png")
        extract_faces_from_atlas(atlas)
        write_labeled_guide(atlas)
        print("Repaired solid faces ->", GAME_TEX)
        return
    if args.from_nets:
        unpack_nets_to_faces(backup=True)
    elif args.init:
        if args.force:
            init_faces(only, force=True)
        else:
            init_faces(only, force=False)
    elif only is None and not any(FACES.glob("*__front.png")):
        init_faces(None, force=False)

    atlas = pack_atlas()
    atlas.save(GAME_TEX)
    atlas.save(ART / "vitrified_golem.png")
    # Never regenerate nets after unpack — would wipe artist work
    write_part_nets(force=False)
    write_labeled_guide(atlas)
    write_readme()
    print("Packed ->", GAME_TEX)


if __name__ == "__main__":
    main()
