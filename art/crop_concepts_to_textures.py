from __future__ import annotations

from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
BLOCK_OUT = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "block"
ITEM_OUT = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "item"
# GenerateImage stores outputs under the Cursor project assets folder.
# (See tool output paths.)
CONCEPTS = Path(r"C:\Users\2005t\.cursor\projects\c-Users-2005t-Effecoria-mod\assets")


def make_bg_transparent(img: Image.Image, white_thresh: int = 235) -> Image.Image:
    """Turn near-white/near-grey background into transparency so downscaled textures look clean."""
    if img.mode != "RGBA":
        img = img.convert("RGBA")
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            # 1) pure/near-white
            if r >= white_thresh and g >= white_thresh and b >= white_thresh:
                px[x, y] = (r, g, b, 0)
                continue
            # 2) near-grey gridlines: bright and low color variance
            mx = max(r, g, b)
            mn = min(r, g, b)
            if mx >= white_thresh - 10 and (mx - mn) <= 18 and (r + g + b) >= 650:
                px[x, y] = (r, g, b, 0)
    return img


def crop_grid(concept_path: Path, cols: int, rows: int) -> list[Image.Image]:
    img = Image.open(concept_path)
    img = make_bg_transparent(img)
    w, h = img.size
    cell_w = w // cols
    cell_h = h // rows

    cells: list[Image.Image] = []
    for r in range(rows):
        for c in range(cols):
            x0 = c * cell_w
            y0 = r * cell_h
            x1 = (c + 1) * cell_w if c < cols - 1 else w
            y1 = (r + 1) * cell_h if r < rows - 1 else h
            cells.append(img.crop((x0, y0, x1, y1)))
    return cells


def auto_crop_to_content(cell_img: Image.Image, pad: int = 1) -> Image.Image:
    """Crop transparent/background to the content's bounding box, then pad a bit."""
    if cell_img.mode != "RGBA":
        cell_img = cell_img.convert("RGBA")
    px = cell_img.load()
    w, h = cell_img.size
    min_x, min_y = w, h
    max_x, max_y = -1, -1
    for y in range(h):
        for x in range(w):
            if px[x, y][3] > 0:
                min_x = min(min_x, x)
                min_y = min(min_y, y)
                max_x = max(max_x, x)
                max_y = max(max_y, y)
    if max_x < 0:
        return cell_img
    min_x = max(0, min_x - pad)
    min_y = max(0, min_y - pad)
    max_x = min(w - 1, max_x + pad)
    max_y = min(h - 1, max_y + pad)
    # +1 because crop's end is exclusive
    return cell_img.crop((min_x, min_y, max_x + 1, max_y + 1))


def fit_to_16x16(cell_img: Image.Image) -> Image.Image:
    """Downscale with nearest-neighbor but keep aspect ratio and center on 16x16 canvas."""
    cell_img = auto_crop_to_content(cell_img, pad=1)
    cw, ch = cell_img.size
    if cw <= 0 or ch <= 0:
        out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        return out
    scale = min(16 / cw, 16 / ch)
    nw = max(1, int(round(cw * scale)))
    nh = max(1, int(round(ch * scale)))
    resized = cell_img.resize((nw, nh), resample=Image.NEAREST)
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    ox = (16 - nw) // 2
    oy = (16 - nh) // 2
    out.paste(resized, (ox, oy), resized)
    return out


def save_cell(cell_img: Image.Image, out_path: Path) -> None:
    out_img = fit_to_16x16(cell_img)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_img.save(out_path)


def main() -> None:
    # Crusher: 2 columns x 3 rows (6 cells, last is blank in prompt).
    crusher_sheet = CONCEPTS / "phi_crusher_concept_sheet.png"
    crusher_cells = crop_grid(crusher_sheet, cols=2, rows=3)

    # row0 col0: base side unlit -> phi_crusher_side.png
    # row0 col1: base top unlit  -> phi_crusher_top.png
    # row1 col0: base side lit   -> phi_crusher_side_on.png
    # row1 col1: hopper side     -> phi_crusher_hopper_side.png
    # row2 col0: hopper top      -> phi_crusher_hopper_top.png
    mapping = {
        (0, 0): BLOCK_OUT / "phi_crusher_side.png",
        (0, 1): BLOCK_OUT / "phi_crusher_top.png",
        (1, 0): BLOCK_OUT / "phi_crusher_side_on.png",
        (1, 1): BLOCK_OUT / "phi_crusher_hopper_side.png",
        (2, 0): BLOCK_OUT / "phi_crusher_hopper_top.png",
    }
    for (r, c), out_path in mapping.items():
        idx = r * 2 + c
        save_cell(crusher_cells[idx], out_path)

    # Item textures: use side views as proxy.
    save_cell(crusher_cells[0], ITEM_OUT / "phi_crusher.png")
    save_cell(crusher_cells[3], ITEM_OUT / "phi_crusher_hopper.png")

    # Bus: 2 columns x 1 row.
    bus_sheet = CONCEPTS / "phi_bus_concept_sheet.png"
    bus_cells = crop_grid(bus_sheet, cols=2, rows=1)
    save_cell(bus_cells[0], BLOCK_OUT / "phi_bus.png")
    save_cell(bus_cells[1], BLOCK_OUT / "phi_bus_on.png")
    save_cell(bus_cells[0], ITEM_OUT / "phi_bus.png")

    print("cropped concept sheets -> textures OK")


if __name__ == "__main__":
    main()

