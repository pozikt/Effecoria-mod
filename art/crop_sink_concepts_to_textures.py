"""Crop sink redesign concept sheets into crisp Minecraft textures."""
from __future__ import annotations

from collections import deque
from pathlib import Path

from PIL import Image, ImageEnhance, ImageOps


ROOT = Path(__file__).resolve().parents[1]
BLOCK_OUT = ROOT / "src/main/resources/assets/effecoria/textures/block"
ITEM_OUT = ROOT / "src/main/resources/assets/effecoria/textures/item"
ARMOR_OUT = ROOT / "src/main/resources/assets/effecoria/textures/models/armor"
CONCEPTS = Path(r"C:\Users\2005t\.cursor\projects\c-Users-2005t-Effecoria-mod\assets")
ART_PREVIEW = ROOT / "art/sink_redesign_preview"
VANILLA_ARMOR = ROOT / "art/vanilla_armor_ref"


def flood_punch_bg(img: Image.Image, tol: int = 28) -> Image.Image:
    """Punch only background connected to image edges (keeps grey metal)."""
    if img.mode != "RGBA":
        img = img.convert("RGBA")
    w, h = img.size
    px = img.load()

    def is_bg(r: int, g: int, b: int, a: int) -> bool:
        if a < 8:
            return True
        mx, mn = max(r, g, b), min(r, g, b)
        chroma = mx - mn
        # charcoal / near-black sheet only (do NOT punch mid-grey metal)
        if mx <= 52 and chroma <= 18:
            return True
        # near white
        if r >= 240 and g >= 240 and b >= 240:
            return True
        return False

    visited = [[False] * w for _ in range(h)]
    q: deque[tuple[int, int]] = deque()
    for x in range(w):
        q.append((x, 0))
        q.append((x, h - 1))
    for y in range(h):
        q.append((0, y))
        q.append((w - 1, y))

    while q:
        x, y = q.popleft()
        if x < 0 or y < 0 or x >= w or y >= h or visited[y][x]:
            continue
        visited[y][x] = True
        r, g, b, a = px[x, y]
        if not is_bg(r, g, b, a):
            # allow slight bleed into near-bg neighbors of already-punched? skip non-bg seeds
            # but we only enqueue from edges initially; for flood continue only if bg
            continue
        px[x, y] = (r, g, b, 0)
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= nx < w and 0 <= ny < h and not visited[ny][nx]:
                nr, ng, nb, na = px[nx, ny]
                if is_bg(nr, ng, nb, na) or (
                    na > 0
                    and abs(nr - r) + abs(ng - g) + abs(nb - b) <= tol
                    and max(nr, ng, nb) <= 140
                    and (max(nr, ng, nb) - min(nr, ng, nb)) <= 24
                ):
                    q.append((nx, ny))
    return img


def punch_cell_frame(img: Image.Image, margin: float = 0.08) -> Image.Image:
    """Remove inventory-slot style grey frames near cell borders."""
    if img.mode != "RGBA":
        img = img.convert("RGBA")
    w, h = img.size
    mx = int(w * margin)
    my = int(h * margin)
    px = img.load()
    for y in range(h):
        for x in range(w):
            if x >= mx and x < w - mx and y >= my and y < h - my:
                continue
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            chroma = max(r, g, b) - min(r, g, b)
            lum = max(r, g, b)
            if chroma <= 18 and lum <= 150:
                px[x, y] = (r, g, b, 0)
    return img


def crop_grid(path: Path, cols: int, rows: int, inset: float = 0.06) -> list[Image.Image]:
    img = flood_punch_bg(Image.open(path))
    w, h = img.size
    cw, ch = w // cols, h // rows
    cells: list[Image.Image] = []
    for r in range(rows):
        for c in range(cols):
            x0 = int(c * cw + cw * inset)
            y0 = int(r * ch + ch * inset)
            x1 = int((c + 1) * cw - cw * inset)
            y1 = int((r + 1) * ch - ch * inset)
            if c == cols - 1:
                x1 = min(w, x1)
            if r == rows - 1:
                y1 = min(h, y1)
            cells.append(punch_cell_frame(img.crop((x0, y0, x1, y1))))
    return cells


def bbox(img: Image.Image, alpha_min: int = 16) -> tuple[int, int, int, int] | None:
    px = img.load()
    w, h = img.size
    min_x, min_y, max_x, max_y = w, h, -1, -1
    for y in range(h):
        for x in range(w):
            if px[x, y][3] >= alpha_min:
                min_x = min(min_x, x)
                min_y = min(min_y, y)
                max_x = max(max_x, x)
                max_y = max(max_y, y)
    if max_x < 0:
        return None
    return min_x, min_y, max_x + 1, max_y + 1


def to_16(cell: Image.Image, opaque: bool = False) -> Image.Image:
    cell = cell.convert("RGBA")
    # second pass punch on cell edges
    cell = flood_punch_bg(cell, tol=22)
    box = bbox(cell)
    if not box:
        return Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    x0, y0, x1, y1 = box
    pad = 2
    x0 = max(0, x0 - pad)
    y0 = max(0, y0 - pad)
    x1 = min(cell.size[0], x1 + pad)
    y1 = min(cell.size[1], y1 + pad)
    cropped = cell.crop((x0, y0, x1, y1))

    # BOX downscale keeps silhouette when going hi-res -> 16
    cw, ch = cropped.size
    scale = min(16 / cw, 16 / ch)
    nw = max(1, int(round(cw * scale)))
    nh = max(1, int(round(ch * scale)))
    # first box to ~32 then nearest to 16 for crisp pixels when large
    if max(cw, ch) > 48:
        mid_w = max(nw * 2, nw)
        mid_h = max(nh * 2, nh)
        mid = cropped.resize((mid_w, mid_h), resample=Image.Resampling.BOX)
        resized = mid.resize((nw, nh), resample=Image.Resampling.NEAREST)
    else:
        resized = cropped.resize((nw, nh), resample=Image.Resampling.NEAREST)

    # harden alpha
    sp = resized.load()
    hard = Image.new("RGBA", (nw, nh), (0, 0, 0, 0))
    hp = hard.load()
    for y in range(nh):
        for x in range(nw):
            r, g, b, a = sp[x, y]
            if a >= 96:
                hp[x, y] = (r, g, b, 255)

    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    ox = (16 - nw) // 2
    oy = (16 - nh) // 2
    out.paste(hard, (ox, oy), hard)

    if opaque:
        px = out.load()
        # sample opaque pixels for filler (prefer darker)
        samples = []
        for y in range(16):
            for x in range(16):
                r, g, b, a = px[x, y]
                if a == 255:
                    samples.append((r, g, b))
        if samples:
            samples.sort(key=lambda t: t[0] + t[1] + t[2])
            fr, fg, fb = samples[len(samples) // 4]
            for y in range(16):
                for x in range(16):
                    if px[x, y][3] < 255:
                        px[x, y] = (fr, fg, fb, 255)
    return out


def save_item(cell: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    to_16(cell, opaque=False).save(path)


def save_block_face(cell: Image.Image, path: Path) -> None:
    """Force full-bleed opaque 16x16 face (for cube_all)."""
    path.parent.mkdir(parents=True, exist_ok=True)
    cell = cell.convert("RGBA")
    cell = flood_punch_bg(cell, tol=18)
    box = bbox(cell)
    if not box:
        Image.new("RGBA", (16, 16), (80, 80, 80, 255)).save(path)
        return
    cropped = cell.crop(box)
    # stretch to square face
    face = cropped.resize((16, 16), resample=Image.Resampling.BOX)
    sp = face.load()
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    op = out.load()
    # filler from median-ish
    cols = [sp[x, y][:3] for y in range(16) for x in range(16) if sp[x, y][3] > 100]
    cols.sort(key=lambda t: sum(t))
    fr, fg, fb = cols[len(cols) // 2] if cols else (90, 90, 95)
    for y in range(16):
        for x in range(16):
            r, g, b, a = sp[x, y]
            if a >= 80:
                op[x, y] = (r, g, b, 255)
            else:
                op[x, y] = (fr, fg, fb, 255)
    out.save(path)


def bake_phi_steel_armor_layers() -> None:
    """Recolor iron UV to steel-grey + sparse cyan accents (concept palette)."""
    ARMOR_OUT.mkdir(parents=True, exist_ok=True)
    cyan = (48, 210, 220)
    for layer, out_name in (("layer_1", "phi_steel_layer_1.png"), ("layer_2", "phi_steel_layer_2.png")):
        src = Image.open(VANILLA_ARMOR / f"iron_{layer}.png").convert("RGBA")
        r, g, b, a = src.split()
        gray = ImageOps.grayscale(Image.merge("RGB", (r, g, b)))
        gray = ImageEnhance.Contrast(gray).enhance(1.12)
        out = Image.new("RGBA", src.size)
        gp = gray.load()
        ap = a.load()
        op = out.load()
        for y in range(src.size[1]):
            for x in range(src.size[0]):
                aa = ap[x, y]
                if aa == 0:
                    op[x, y] = (0, 0, 0, 0)
                    continue
                v = gp[x, y] / 255.0
                # cool steel-grey (not cyan-primary)
                nr = int(min(255, 55 + 145 * v))
                ng = int(min(255, 62 + 150 * v))
                nb = int(min(255, 72 + 155 * v))
                op[x, y] = (nr, ng, nb, aa)

        # only brightest rim pixels get cyan edge glow
        for y in range(out.size[1]):
            for x in range(out.size[0]):
                r0, g0, b0, aa = op[x, y]
                if aa == 0:
                    continue
                if r0 + g0 + b0 >= 480:
                    op[x, y] = (
                        min(255, int(r0 * 0.45 + cyan[0] * 0.55)),
                        min(255, int(g0 * 0.35 + cyan[1] * 0.65)),
                        min(255, int(b0 * 0.35 + cyan[2] * 0.65)),
                        aa,
                    )
        if layer == "layer_1":
            for y in range(20, 31):
                for x in range(20, 28):
                    if op[x, y][3] == 0:
                        continue
                    if x in (23, 24):
                        op[x, y] = (*cyan, 255)
                    if y == 22 and x in (21, 22, 25, 26):
                        op[x, y] = (*cyan, 255)
                    if y == 28 and x in (21, 22, 25, 26):
                        op[x, y] = (*cyan, 255)
            # boot sole glow strip (approx feet region)
            for x in range(8, 16):
                if op[x, 29][3]:
                    op[x, 29] = (*cyan, 255)
                if op[x + 32, 29][3]:
                    op[x + 32, 29] = (*cyan, 255)
        else:
            for x in range(16, 24):
                if op[x, 16][3]:
                    op[x, 16] = (*cyan, 255)
                if op[x + 16, 16][3]:
                    op[x + 16, 16] = (*cyan, 255)

        out.save(ARMOR_OUT / out_name)
        out.save(ART_PREVIEW / out_name)


def main() -> None:
    ART_PREVIEW.mkdir(parents=True, exist_ok=True)

    # byproducts 3x3
    cells = crop_grid(CONCEPTS / "sink_byproducts_concept_sheet.png", 3, 3, inset=0.05)
    for cell, name in zip(
        cells,
        [
            "phi_stone_grit",
            "bone_grit",
            "phi_bone_paste",
            "phi_wood_shavings",
            "phi_fiber",
            "obsidian_grit",
            "omega_nugget",
            "soul_shard",
            "omega_waste",
        ],
    ):
        save_item(cell, ITEM_OUT / f"{name}.png")

    # crafted items (top row of crafted sheet) — 3x2
    cells = crop_grid(CONCEPTS / "sink_crafted_concept_sheet.png", 3, 2, inset=0.07)
    save_item(cells[0], ITEM_OUT / "phi_cloth.png")
    save_item(cells[1], ITEM_OUT / "phi_rope.png")
    save_item(cells[3], ITEM_OUT / "phi_steel_ingot.png")
    # dedicated solo icon (sheet crop loses grey metal rim)
    solo = CONCEPTS / "sink_omega_filter_solo.png"
    if solo.exists():
        save_item(Image.open(solo), ITEM_OUT / "omega_filter.png")
    else:
        save_item(cells[2], ITEM_OUT / "omega_filter.png")

    # flat block faces sheet (prefer over isometric cells)
    faces = CONCEPTS / "sink_block_faces_flat_sheet.png"
    if faces.exists():
        fcells = crop_grid(faces, 2, 1, inset=0.04)
        save_block_face(fcells[0], BLOCK_OUT / "phi_concrete.png")
        save_block_face(fcells[1], BLOCK_OUT / "omega_anchor.png")
    else:
        save_block_face(cells[4], BLOCK_OUT / "phi_concrete.png")
        save_block_face(cells[5], BLOCK_OUT / "omega_anchor.png")

    # armor items 2x2
    cells = crop_grid(CONCEPTS / "sink_phi_steel_armor_items_sheet.png", 2, 2, inset=0.08)
    save_item(cells[0], ITEM_OUT / "phi_steel_helmet.png")
    save_item(cells[1], ITEM_OUT / "phi_steel_chestplate.png")
    save_item(cells[2], ITEM_OUT / "phi_steel_leggings.png")
    save_item(cells[3], ITEM_OUT / "phi_steel_boots.png")

    # tools 3x2
    cells = crop_grid(CONCEPTS / "sink_phi_steel_tools_sheet.png", 3, 2, inset=0.08)
    save_item(cells[0], ITEM_OUT / "phi_steel_sword.png")
    save_item(cells[1], ITEM_OUT / "phi_steel_pickaxe.png")
    save_item(cells[2], ITEM_OUT / "phi_steel_axe.png")
    save_item(cells[3], ITEM_OUT / "phi_steel_shovel.png")
    save_item(cells[4], ITEM_OUT / "phi_steel_hoe.png")

    bake_phi_steel_armor_layers()

    # previews x8
    for folder, names in (
        (ITEM_OUT, [
            "phi_stone_grit", "bone_grit", "phi_bone_paste", "phi_wood_shavings", "phi_fiber",
            "obsidian_grit", "omega_nugget", "soul_shard", "omega_waste",
            "phi_cloth", "phi_rope", "omega_filter", "phi_steel_ingot",
            "phi_steel_helmet", "phi_steel_chestplate", "phi_steel_leggings", "phi_steel_boots",
            "phi_steel_sword", "phi_steel_pickaxe", "phi_steel_axe", "phi_steel_shovel", "phi_steel_hoe",
        ]),
        (BLOCK_OUT, ["phi_concrete", "omega_anchor"]),
    ):
        for n in names:
            im = Image.open(folder / f"{n}.png")
            im.resize((128, 128), Image.Resampling.NEAREST).save(ART_PREVIEW / f"out_{n}_x8.png")

    print("sink concept -> textures OK (v2)")


if __name__ == "__main__":
    main()
