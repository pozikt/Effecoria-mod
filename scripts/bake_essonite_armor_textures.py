"""Generate crystal/star essonite armor textures (recolor of Φ-chitin) + item models."""

from pathlib import Path

from PIL import Image

ROOT = Path("src/main/resources/assets/effecoria")
ITEM = ROOT / "textures" / "item"
ARMOR = ROOT / "textures" / "models" / "armor"
MODELS = ROOT / "models" / "item"
ARMOR.mkdir(parents=True, exist_ok=True)


def remapped(img: Image.Image, fn) -> Image.Image:
    out = img.convert("RGBA")
    px = out.load()
    w, h = out.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            px[x, y] = fn(r, g, b, a)
    return out


def crystal_map(r, g, b, a):
    lum = (r + g + b) / 3
    if r > 140 and g > 110 and b < 100:
        return (min(255, r), min(255, int(g * 0.9)), max(40, int(b * 0.5)), a)
    return (
        int(min(255, lum * 0.55 + 40)),
        int(min(255, lum * 0.75 + 70)),
        int(min(255, lum * 0.95 + 90)),
        a,
    )


def star_map(r, g, b, a):
    lum = (r + g + b) / 3
    return (
        int(min(255, lum * 0.95 + 70)),
        int(min(255, lum * 0.85 + 50)),
        int(min(255, lum * 0.55 + 20)),
        a,
    )


def write_model(name: str) -> None:
    MODELS.joinpath(f"{name}.json").write_text(
        '{\n  "parent": "minecraft:item/generated",\n'
        f'  "textures": {{ "layer0": "effecoria:item/{name}" }}\n}}\n',
        encoding="utf-8",
    )


def main() -> None:
    base_layer = ARMOR / "phi_chitin_layer_1.png"
    base_layer2 = ARMOR / "phi_chitin_layer_2.png"
    pieces = {
        "helmet": ITEM / "phi_chitin_helmet.png",
        "chestplate": ITEM / "phi_chitin_chestplate.png",
        "leggings": ITEM / "phi_chitin_leggings.png",
        "boots": ITEM / "phi_chitin_boots.png",
    }

    for name, mapper in (("crystal_essonite", crystal_map), ("star_essonite", star_map)):
        remapped(Image.open(base_layer), mapper).save(ARMOR / f"{name}_layer_1.png")
        src2 = base_layer2 if base_layer2.exists() else base_layer
        remapped(Image.open(src2), mapper).save(ARMOR / f"{name}_layer_2.png")
        for piece, src in pieces.items():
            remapped(Image.open(src), mapper).save(ITEM / f"{name}_{piece}.png")
            write_model(f"{name}_{piece}")

    insert = Image.new("RGBA", (16, 16), (28, 22, 36, 255))
    px = insert.load()
    for i in range(16):
        px[i, 0] = (60, 50, 70, 255)
        px[i, 15] = (12, 10, 16, 255)
        px[0, i] = (60, 50, 70, 255)
        px[15, i] = (12, 10, 16, 255)
    insert.save(ITEM / "void_obsidian_insert.png")
    write_model("void_obsidian_insert")

    key = Image.new("RGBA", (16, 16), (212, 168, 75, 255))
    px = key.load()
    for y in range(4, 12):
        for x in range(6, 10):
            px[x, y] = (90, 70, 40, 255)
    key.save(ITEM / "psi_key.png")
    write_model("psi_key")

    paper_path = ITEM / "phi_paper.png"
    base_paper = Image.open(paper_path) if paper_path.exists() else Image.new("RGBA", (16, 16), (220, 210, 190, 255))
    colors = {
        "firmitas": (160, 150, 140, 255),
        "umbra": (70, 70, 90, 255),
        "abnegatio": (200, 200, 210, 255),
        "servare": (120, 180, 140, 255),
        "clausura": (180, 120, 80, 255),
    }
    for name, col in colors.items():
        out = base_paper.convert("RGBA").copy()
        px = out.load()
        w, h = out.size
        for y in range(h):
            for x in range(w):
                r, g, b, a = px[x, y]
                if a == 0:
                    continue
                if 4 <= x <= 11 and 4 <= y <= 11:
                    px[x, y] = col
        out.save(ITEM / f"phi_phoneme_{name}.png")
        write_model(f"phi_phoneme_{name}")

    print("ok")


if __name__ == "__main__":
    main()
