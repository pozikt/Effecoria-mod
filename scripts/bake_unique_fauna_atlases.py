"""Bake opaque GeckoLib entity atlases for Ω-Scar + Crystal Forest fauna."""
from __future__ import annotations

import json
import random
import shutil
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "effecoria"
GEN = Path(r"C:\Users\2005t\.cursor\projects\c-Users-2005t-Effecoria-mod\assets")


def isize(v: float) -> int:
    return max(1, int(round(v)))


def darken(c: tuple[int, int, int], amount: int = 28) -> tuple[int, int, int]:
    return tuple(max(0, x - amount) for x in c)  # type: ignore[return-value]


def lighten(c: tuple[int, int, int], amount: int = 24) -> tuple[int, int, int]:
    return tuple(min(255, x + amount) for x in c)  # type: ignore[return-value]


def mix(a: tuple[int, int, int], b: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))  # type: ignore[return-value]


def fill_rect(
    img: Image.Image,
    x: int,
    y: int,
    w: int,
    h: int,
    palette: list[tuple[int, int, int]],
    rng: random.Random,
    *,
    edge: bool = True,
) -> None:
    if w <= 0 or h <= 0:
        return
    px = img.load()
    for j in range(h):
        for i in range(w):
            c = palette[rng.randrange(len(palette))]
            if edge and (i == 0 or j == 0 or i == w - 1 or j == h - 1):
                c = darken(c, 22)
            elif edge and (i == 1 or j == 1 or i == w - 2 or j == h - 2) and min(w, h) > 3:
                if rng.random() < 0.45:
                    c = darken(c, 10)
            px[x + i, y + j] = (*c, 255)


def paint_face(
    img: Image.Image,
    x: int,
    y: int,
    w: int,
    h: int,
    color: tuple[int, int, int],
) -> None:
    draw = ImageDraw.Draw(img)
    draw.rectangle([x, y, x + w - 1, y + h - 1], fill=(*color, 255))


def face_rects(u: int, v: int, w: float, h: float, d: float) -> dict[str, tuple[int, int, int, int]]:
    iw, ih, id_ = isize(w), isize(h), isize(d)
    return {
        "T": (u + id_, v, iw, id_),
        "Bo": (u + id_ + iw, v, iw, id_),
        "R": (u, v + id_, id_, ih),
        "F": (u + id_, v + id_, iw, ih),
        "L": (u + id_ + iw, v + id_, id_, ih),
        "Ba": (u + id_ + iw + id_, v + id_, iw, ih),
    }


def paint_cube(
    img: Image.Image,
    u: int,
    v: int,
    w: float,
    h: float,
    d: float,
    palette: list[tuple[int, int, int]],
    rng: random.Random,
    *,
    face_overrides: dict[str, list[tuple[int, int, int]]] | None = None,
) -> dict[str, tuple[int, int, int, int]]:
    faces = face_rects(u, v, w, h, d)
    for name, (x, y, fw, fh) in faces.items():
        pal = (face_overrides or {}).get(name, palette)
        fill_rect(img, x, y, fw, fh, pal, rng)
    return faces


def parse_cubes(geo_path: Path) -> list[tuple[str, int, int, float, float, float]]:
    data = json.loads(geo_path.read_text(encoding="utf-8"))
    geo = data["minecraft:geometry"][0]
    out: list[tuple[str, int, int, float, float, float]] = []
    for bone in geo["bones"]:
        name = bone["name"]
        for c in bone.get("cubes") or []:
            uv = c.get("uv")
            size = c.get("size")
            if not uv or not size:
                continue
            out.append((name, int(uv[0]), int(uv[1]), float(size[0]), float(size[1]), float(size[2])))
    return out


def save_atlas(img: Image.Image, art_dir: Path, name: str) -> None:
    art_dir.mkdir(parents=True, exist_ok=True)
    path = art_dir / f"{name}_atlas.png"
    img.save(path, optimize=True)
    img.resize((img.width * 4, img.height * 4), Image.Resampling.NEAREST).save(
        art_dir / f"{name}_atlas_4x.png", optimize=True
    )
    dest = ASSETS / "textures" / "entity" / f"{name}.png"
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(path, dest)
    print("wrote", path.relative_to(ROOT), "->", dest.relative_to(ROOT), path.stat().st_size)


# --- palettes ---
SHADE = {
    "body": [(18, 14, 24), (28, 20, 36), (12, 10, 16)],
    "cloak": [(14, 12, 20), (24, 18, 32), (36, 28, 48)],
    "purple": [(58, 36, 80), (78, 52, 110), (42, 26, 58)],
    "eye": (232, 210, 74),
    "eye_rim": (20, 16, 12),
}
WORM = {
    "chitin": [(26, 18, 24), (36, 28, 40), (20, 14, 18)],
    "seam": [(74, 48, 96), (90, 62, 120), (58, 36, 78)],
    "bone": [(216, 200, 168), (200, 184, 152), (176, 160, 128)],
    "eye": (232, 210, 74),
}
MINK = {
    "fur": [(26, 20, 24), (36, 28, 34), (18, 14, 18), (44, 34, 42)],
    "belly": [(74, 48, 80), (90, 58, 96), (58, 36, 64)],
    "bone": [(200, 184, 160), (176, 160, 136)],
    "ear": [(90, 58, 96), (120, 88, 112)],
    "eye": (232, 210, 74),
}
CRAB = {
    "shell": [(168, 136, 88), (176, 144, 96), (148, 120, 76), (128, 104, 64)],
    "dark": [(72, 56, 40), (48, 40, 24), (88, 72, 48)],
    "crystal": [(208, 168, 112), (248, 216, 104), (200, 152, 80), (184, 136, 72)],
    "eye": (20, 16, 12),
}
EIDOS = {
    "body": [(168, 136, 96), (148, 120, 88), (184, 152, 112), (128, 104, 72)],
    "dark": [(88, 68, 48), (72, 56, 40)],
    "glow": [(240, 180, 48), (248, 216, 104), (208, 160, 64)],
    "eye": (255, 240, 160),
    "pupil": (24, 16, 8),
}


def bake_omega_shade() -> None:
    geo = ASSETS / "geo" / "omega_shade.geo.json"
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    rng = random.Random(0x0A11)
    cubes = parse_cubes(geo)
    for name, u, v, w, h, d in cubes:
        if name == "head":
            faces = paint_cube(img, u, v, w, h, d, SHADE["body"], rng)
            fx, fy, fw, fh = faces["F"]
            # slit eyes
            ey = fy + max(1, fh // 3)
            paint_face(img, fx + max(1, fw // 5), ey, 1, 1, SHADE["eye"])
            paint_face(img, fx + fw - max(2, fw // 5) - 1, ey, 1, 1, SHADE["eye"])
            paint_face(img, fx + max(1, fw // 5) - 1, ey, 1, 1, SHADE["eye_rim"])
            paint_face(img, fx + fw - max(2, fw // 5), ey, 1, 1, SHADE["eye_rim"])
        elif name.startswith("cloak"):
            faces = paint_cube(img, u, v, w, h, d, SHADE["cloak"], rng)
            # ragged violet edge on outer long face
            fx, fy, fw, fh = faces["F"]
            for j in range(0, fh, 2):
                paint_face(img, fx, fy + j, 1, 1, SHADE["purple"][j % len(SHADE["purple"])])
        elif name in ("left_leg", "right_leg"):
            # painted as torn hem strips
            paint_cube(img, u, v, w, h, d, SHADE["cloak"], rng)
        elif name == "body":
            faces = paint_cube(img, u, v, w, h, d, SHADE["body"], rng)
            fx, fy, fw, fh = faces["F"]
            # bruised ribcage lines
            for row in range(2, fh - 1, 2):
                paint_face(img, fx + 1, fy + row, max(1, fw - 2), 1, SHADE["purple"][0])
            bx, by, bw, bh = faces["Ba"]
            paint_face(img, bx + bw // 2, by + 2, 1, max(1, bh - 3), SHADE["purple"][1])
        else:
            paint_cube(img, u, v, w, h, d, SHADE["body"], rng)
    save_atlas(img, ROOT / "art" / "scar" / "omega_shade", "omega_shade")


def bake_omega_worm() -> None:
    geo = ASSETS / "geo" / "omega_worm.geo.json"
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    rng = random.Random(0x0B0E)
    cubes = parse_cubes(geo)
    # body cubes in order: head-seg, mid, tail at u 0,20,40
    for name, u, v, w, h, d in cubes:
        if name == "body":
            faces = paint_cube(img, u, v, w, h, d, WORM["chitin"], rng)
            # violet seam band on front of each segment
            fx, fy, fw, fh = faces["F"]
            paint_face(img, fx, fy + fh // 2, fw, 1, WORM["seam"][0])
            tx, ty, tw, th = faces["T"]
            paint_face(img, tx + 1, ty + th // 2, max(1, tw - 2), 1, WORM["seam"][1])
        elif name == "head" and w >= 3:
            faces = paint_cube(img, u, v, w, h, d, WORM["chitin"], rng)
            fx, fy, fw, fh = faces["F"]
            paint_face(img, fx + 1, fy + 1, 1, 1, WORM["eye"])
            paint_face(img, fx + fw - 2, fy + 1, 1, 1, WORM["eye"])
        else:
            # jaws / stub legs
            pal = WORM["bone"] if w <= 2 and h <= 2 else WORM["chitin"]
            paint_cube(img, u, v, w, h, d, pal, rng)
    save_atlas(img, ROOT / "art" / "scar" / "omega_worm", "omega_worm")


def bake_rotfang_mink() -> None:
    geo = ASSETS / "geo" / "rotfang_mink.geo.json"
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    rng = random.Random(0xFA16)
    cubes = parse_cubes(geo)
    for idx, (name, u, v, w, h, d) in enumerate(cubes):
        if name == "body":
            faces = paint_cube(img, u, v, w, h, d, MINK["fur"], rng)
            bx, by, bw, bh = faces["Bo"]
            fill_rect(img, bx, by, bw, bh, MINK["belly"], rng)
            fx, fy, fw, fh = faces["F"]
            fill_rect(img, fx + 1, fy + fh - 2, max(1, fw - 2), 1, MINK["belly"], rng)
        elif name == "head" and w >= 4:
            faces = paint_cube(img, u, v, w, h, d, MINK["fur"], rng)
            fx, fy, fw, fh = faces["F"]
            paint_face(img, fx + 1, fy + 1, 1, 1, MINK["eye"])
            paint_face(img, fx + fw - 2, fy + 1, 1, 1, MINK["eye"])
        elif name == "head" and w == 2 and h == 2:
            # snout
            faces = paint_cube(img, u, v, w, h, d, MINK["fur"], rng)
            fx, fy, fw, fh = faces["F"]
            paint_face(img, fx, fy + fh - 1, 1, 1, MINK["bone"][0])
            paint_face(img, fx + fw - 1, fy + fh - 1, 1, 1, MINK["bone"][0])
        elif name == "head" and w == 1:
            paint_cube(img, u, v, w, h, d, MINK["ear"], rng)
        elif name == "tail":
            paint_cube(img, u, v, w, h, d, MINK["fur"] if idx % 2 == 0 else MINK["belly"], rng)
        else:
            paint_cube(img, u, v, w, h, d, MINK["fur"], rng)
    save_atlas(img, ROOT / "art" / "scar" / "rotfang_mink", "rotfang_mink")


def bake_crystal_crab() -> None:
    geo = ASSETS / "geo" / "crystal_crab.geo.json"
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    rng = random.Random(0xC2AB)
    for name, u, v, w, h, d in parse_cubes(geo):
        if name.startswith("crystal"):
            faces = paint_cube(img, u, v, w, h, d, CRAB["crystal"], rng)
            fx, fy, fw, fh = faces["F"]
            paint_face(img, fx + fw // 2, fy + 1, 1, max(1, fh - 2), lighten(CRAB["crystal"][1], 20))
        elif name.startswith("eye"):
            if h >= 1.5 and w < 1.5:
                paint_cube(img, u, v, w, h, d, CRAB["dark"], rng)
            else:
                faces = paint_cube(img, u, v, w, h, d, CRAB["dark"], rng)
                fx, fy, fw, fh = faces["F"]
                paint_face(img, fx + max(0, fw // 2), fy + max(0, fh // 2), 1, 1, CRAB["eye"])
        elif "claw" in name or "leg" in name:
            faces = paint_cube(img, u, v, w, h, d, CRAB["shell"], rng)
            if "tip" in name or name.endswith("claw_l") or name.endswith("claw_r"):
                # darker joints
                fx, fy, fw, fh = faces["F"]
                paint_face(img, fx, fy + fh - 1, fw, 1, CRAB["dark"][0])
            if "claw" in name and "tip" not in name and w > 2:
                # small crystal bump on claw top
                tx, ty, tw, th = faces["T"]
                paint_face(img, tx + tw // 2, ty + th // 2, 1, 1, CRAB["crystal"][0])
        else:
            faces = paint_cube(img, u, v, w, h, d, CRAB["shell"], rng)
            if name == "body" and w >= 10:
                tx, ty, tw, th = faces["T"]
                # carapace plate seams
                for x in range(2, tw - 1, 3):
                    paint_face(img, tx + x, ty + 1, 1, max(1, th - 2), CRAB["dark"][0])
    art = ROOT / "art" / "crystal_crab"
    art.mkdir(parents=True, exist_ok=True)
    img.save(art / "crystal_crab.png", optimize=True)
    img.save(art / "crystal_crab_atlas_natural.png", optimize=True)
    dest = ASSETS / "textures" / "entity" / "crystal_crab.png"
    shutil.copy2(art / "crystal_crab.png", dest)
    print("wrote crystal_crab", dest.stat().st_size)


def paint_phi(img: Image.Image, x: int, y: int, w: int, h: int, color: tuple[int, int, int]) -> None:
    """Simple Φ mark: circle + vertical bar."""
    if w < 3 or h < 3:
        paint_face(img, x + w // 2, y + h // 2, 1, 1, color)
        return
    cx, cy = x + w // 2, y + h // 2
    r = max(1, min(w, h) // 2 - 1)
    draw = ImageDraw.Draw(img)
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=(*color, 255))
    draw.line([cx, cy - r - 1, cx, cy + r + 1], fill=(*color, 255))


def bake_eidos() -> None:
    geo = ASSETS / "geo" / "eidos.geo.json"
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    rng = random.Random(0xE1D0)
    for name, u, v, w, h, d in parse_cubes(geo):
        if name.startswith("ring") or name.startswith("trail") or name == "phi_chest":
            faces = paint_cube(img, u, v, w, h, d, EIDOS["glow"], rng)
            if name == "phi_chest":
                fx, fy, fw, fh = faces["F"]
                paint_phi(img, fx, fy, fw, fh, lighten(EIDOS["glow"][0], 30))
        elif name == "eye":
            faces = paint_cube(img, u, v, w, h, d, EIDOS["glow"], rng)
            fx, fy, fw, fh = faces["F"]
            fill_rect(img, fx, fy, fw, fh, [EIDOS["eye"]], rng, edge=False)
            paint_face(img, fx + fw // 2, fy + 1, 1, max(1, fh - 2), EIDOS["pupil"])
        elif name == "torso":
            faces = paint_cube(img, u, v, w, h, d, EIDOS["body"], rng)
            if h >= 6:
                fx, fy, fw, fh = faces["F"]
                paint_phi(img, fx + 1, fy + 2, max(1, fw - 2), max(1, fh // 2), EIDOS["glow"][0])
                bx, by, bw, bh = faces["Ba"]
                paint_phi(img, bx + 1, by + 2, max(1, bw - 2), max(1, bh // 2), EIDOS["glow"][1])
                # glow spine line
                paint_face(img, fx + fw // 2, fy + 1, 1, max(1, fh - 2), EIDOS["glow"][0])
        elif name.startswith("arm"):
            paint_cube(img, u, v, w, h, d, EIDOS["body"], rng)
        else:
            paint_cube(img, u, v, w, h, d, EIDOS["body"], rng)
    art = ROOT / "art" / "eidos"
    art.mkdir(parents=True, exist_ok=True)
    img.save(art / "eidos.png", optimize=True)
    img.save(art / "eidos_atlas_natural.png", optimize=True)
    dest = ASSETS / "textures" / "entity" / "eidos.png"
    shutil.copy2(art / "eidos.png", dest)
    print("wrote eidos", dest.stat().st_size)


def copy_concepts() -> None:
    mapping = {
        "omega_shade_concept_turnaround.png": ROOT / "art" / "scar" / "omega_shade" / "concept_turnaround.png",
        "omega_worm_concept_turnaround.png": ROOT / "art" / "scar" / "omega_worm" / "concept_turnaround.png",
        "rotfang_mink_concept_turnaround.png": ROOT / "art" / "scar" / "rotfang_mink" / "concept_turnaround.png",
    }
    for src_name, dest in mapping.items():
        src = GEN / src_name
        if not src.exists():
            print("missing concept", src)
            continue
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dest)
        print("concept", dest.relative_to(ROOT), dest.stat().st_size)


def main() -> None:
    copy_concepts()
    bake_omega_shade()
    bake_omega_worm()
    bake_rotfang_mink()
    bake_crystal_crab()
    bake_eidos()


if __name__ == "__main__":
    main()
