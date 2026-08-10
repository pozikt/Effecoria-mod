"""Bake technomagic machine 16x16 textures in Spark Reactor visual language.

Shared style: lead frame, corner bolts, cooling fins, cyan mithril accents.
Does NOT touch spark_reactor_* files.
"""
from __future__ import annotations

from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
ART = ROOT / "art" / "phi_alchemy" / "tech_machines"
OUT = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "block"
ART.mkdir(parents=True, exist_ok=True)
OUT.mkdir(parents=True, exist_ok=True)

# Shared palette (matches spark_reactor bake)
LEAD0 = (38, 40, 44, 255)
LEAD1 = (58, 62, 68, 255)
LEAD2 = (78, 84, 92, 255)
LEAD3 = (102, 110, 120, 255)
LEAD4 = (130, 138, 148, 255)
EDGE = (28, 30, 34, 255)
SLOT = (22, 24, 28, 255)
SLOT_LIP = (48, 52, 58, 255)
BOLT = (150, 156, 164, 255)
GEM_OFF = (42, 58, 78, 255)
GEM_ON = (70, 190, 255, 255)
GEM_CORE = (200, 240, 255, 255)
CYAN = (55, 150, 220, 255)
CYAN_DIM = (70, 100, 130, 255)
CYAN_HOT = (140, 220, 255, 255)
CLAY = (110, 88, 72, 255)
CLAY_D = (86, 68, 56, 255)
GLASS = (90, 150, 180, 180)


def blank(c=LEAD2):
    return Image.new("RGBA", (16, 16), c)


def paint_frame(px, bolts=True):
    for i in range(16):
        px[i, 0] = EDGE
        px[i, 15] = EDGE
        px[0, i] = EDGE
        px[15, i] = EDGE
    if bolts:
        for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
            px[x, y] = BOLT


def paint_fins(px, x0=1, x1=15, y0=1, y1=15, shift=0):
    for y in range(y0, y1):
        for x in range(x0, x1):
            band = (x + shift) % 3
            px[x, y] = LEAD1 if band == 0 else LEAD3 if band == 1 else LEAD2
            if y in (2, 8, 13):
                r, g, b, a = px[x, y]
                px[x, y] = (max(0, r - 12), max(0, g - 12), max(0, b - 10), a)


def paint_plate(px, x0=1, x1=15, y0=1, y1=15):
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = LEAD2 if (x + y) % 2 == 0 else LEAD1


def put_rect(px, x0, y0, x1, y1, color):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= x < 16 and 0 <= y < 16:
                px[x, y] = color


def put_px(px, x, y, color):
    if 0 <= x < 16 and 0 <= y < 16:
        px[x, y] = color


def disc(px, cx, cy, r, color, hollow=None):
    for y in range(16):
        for x in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= r and (hollow is None or d >= hollow):
                px[x, y] = color


def save(name: str, img: Image.Image):
    # never overwrite reactor
    if name.startswith("spark_reactor"):
        raise RuntimeError("refusing to write spark_reactor textures")
    img.save(OUT / name)
    img.resize((96, 96), Image.Resampling.NEAREST).save(ART / f"preview_{name}")
    print("wrote", name)


# ---- Era I/II machines ----

def essence_burner(lit: bool) -> Image.Image:
    img = blank()
    px = img.load()
    paint_fins(px)
    paint_frame(px)
    # heat ports
    put_rect(px, 4, 4, 6, 6, CYAN_HOT if lit else SLOT)
    put_rect(px, 9, 4, 11, 6, CYAN_HOT if lit else SLOT)
    put_rect(px, 6, 9, 9, 12, SLOT)
    put_rect(px, 6, 9, 9, 9, SLOT_LIP)
    if lit:
        put_px(px, 5, 5, GEM_CORE)
        put_px(px, 10, 5, GEM_CORE)
        put_rect(px, 7, 10, 8, 11, CYAN)
    return img


def essence_alembic() -> Image.Image:
    img = blank()
    px = img.load()
    paint_fins(px, shift=1)
    paint_frame(px)
    # distill window
    put_rect(px, 5, 4, 10, 10, (50, 90, 120, 255))
    put_rect(px, 6, 5, 9, 9, CYAN_DIM)
    put_px(px, 7, 6, CYAN_HOT)
    put_px(px, 8, 7, GEM_CORE)
    # spout hint
    put_rect(px, 11, 7, 13, 8, LEAD4)
    return img


def clay_crucible_side() -> Image.Image:
    img = blank(CLAY_D)
    px = img.load()
    for y in range(1, 15):
        for x in range(1, 15):
            px[x, y] = CLAY if (x + y) % 3 else CLAY_D
    paint_frame(px)
    # lead band
    put_rect(px, 1, 3, 14, 4, LEAD3)
    put_rect(px, 1, 11, 14, 12, LEAD2)
    put_rect(px, 6, 6, 9, 9, SLOT)
    return img


def clay_crucible_top() -> Image.Image:
    img = blank(LEAD2)
    px = img.load()
    paint_plate(px)
    paint_frame(px)
    disc(px, 7.5, 7.5, 5.2, CLAY_D)
    disc(px, 7.5, 7.5, 3.5, SLOT)
    disc(px, 7.5, 7.5, 5.2, LEAD3, hollow=4.6)
    return img


def phi_furnace_front() -> Image.Image:
    img = blank()
    px = img.load()
    paint_fins(px)
    paint_frame(px)
    # door
    put_rect(px, 4, 3, 11, 12, SLOT_LIP)
    put_rect(px, 5, 4, 10, 11, SLOT)
    # peephole
    put_rect(px, 7, 6, 8, 7, CYAN)
    put_px(px, 7, 6, GEM_CORE)
    put_px(px, 12, 3, GEM_OFF)
    return img


def phi_furnace_side() -> Image.Image:
    img = blank()
    px = img.load()
    paint_fins(px, shift=1)
    paint_frame(px)
    put_rect(px, 2, 6, 3, 10, CYAN_DIM)
    return img


def phi_furnace_top() -> Image.Image:
    img = blank()
    px = img.load()
    paint_plate(px)
    paint_frame(px)
    disc(px, 7.5, 7.5, 3.2, CYAN_DIM)
    disc(px, 7.5, 7.5, 1.5, CYAN)
    put_px(px, 7, 7, GEM_CORE)
    put_px(px, 8, 7, GEM_CORE)
    return img


def psi_imprinter_front() -> Image.Image:
    img = blank()
    px = img.load()
    paint_fins(px)
    paint_frame(px)
    # press plate
    put_rect(px, 4, 4, 11, 10, LEAD1)
    put_rect(px, 5, 5, 10, 9, CYAN_DIM)
    # rune cross
    put_rect(px, 7, 5, 8, 9, CYAN_HOT)
    put_rect(px, 5, 7, 10, 7, CYAN_HOT)
    put_px(px, 7, 7, GEM_CORE)
    # ram
    put_rect(px, 6, 2, 9, 3, LEAD4)
    return img


def psi_imprinter_side() -> Image.Image:
    img = blank()
    px = img.load()
    paint_fins(px, shift=2)
    paint_frame(px)
    put_rect(px, 12, 4, 13, 8, GEM_OFF)
    return img


def psi_imprinter_top() -> Image.Image:
    img = blank()
    px = img.load()
    paint_plate(px)
    paint_frame(px)
    put_rect(px, 4, 4, 11, 11, LEAD1)
    put_rect(px, 6, 6, 9, 9, CYAN_DIM)
    put_px(px, 7, 7, GEM_CORE)
    put_px(px, 8, 7, CYAN_HOT)
    return img


def phi_telegraph_side() -> Image.Image:
    img = blank()
    px = img.load()
    paint_fins(px)
    paint_frame(px)
    # signal strip
    put_rect(px, 6, 2, 9, 13, SLOT_LIP)
    for y in (3, 5, 7, 9, 11):
        put_rect(px, 7, y, 8, y, CYAN_HOT if y % 4 == 3 else CYAN_DIM)
    put_px(px, 7, 7, GEM_CORE)
    return img


def phi_telegraph_end() -> Image.Image:
    img = blank()
    px = img.load()
    paint_plate(px)
    paint_frame(px)
    disc(px, 7.5, 7.5, 4.5, LEAD1)
    disc(px, 7.5, 7.5, 3.0, CYAN_DIM)
    disc(px, 7.5, 7.5, 1.4, CYAN_HOT)
    put_px(px, 7, 7, GEM_CORE)
    put_px(px, 8, 7, GEM_CORE)
    return img


def artifact_assembler_front() -> Image.Image:
    img = blank()
    px = img.load()
    paint_fins(px)
    paint_frame(px)
    # bench window
    put_rect(px, 3, 5, 12, 12, SLOT)
    put_rect(px, 4, 6, 11, 11, LEAD1)
    # guide rails
    put_rect(px, 4, 7, 11, 7, CYAN_DIM)
    put_rect(px, 4, 10, 11, 10, CYAN_DIM)
    put_px(px, 7, 8, CYAN_HOT)
    put_px(px, 8, 9, GEM_CORE)
    return img


def artifact_assembler_side() -> Image.Image:
    img = blank()
    px = img.load()
    paint_fins(px, shift=1)
    paint_frame(px)
    put_rect(px, 5, 8, 10, 12, SLOT_LIP)
    return img


def artifact_assembler_top() -> Image.Image:
    img = blank()
    px = img.load()
    paint_plate(px)
    paint_frame(px)
    put_rect(px, 3, 3, 12, 12, LEAD1)
    put_rect(px, 4, 4, 11, 11, SLOT)
    # cross guides
    put_rect(px, 7, 4, 8, 11, CYAN_DIM)
    put_rect(px, 4, 7, 11, 8, CYAN_DIM)
    put_px(px, 7, 7, GEM_CORE)
    put_px(px, 8, 8, CYAN_HOT)
    return img


def artifact_assembler_bottom() -> Image.Image:
    img = blank(LEAD1)
    px = img.load()
    paint_plate(px)
    paint_frame(px)
    return img


def seal_inscriber_side() -> Image.Image:
    img = blank()
    px = img.load()
    paint_fins(px)
    paint_frame(px)
    put_rect(px, 5, 6, 10, 12, SLOT)
    put_rect(px, 6, 7, 9, 10, LEAD1)
    put_px(px, 7, 8, CYAN)
    put_px(px, 8, 8, GEM_CORE)
    return img


def seal_inscriber_top() -> Image.Image:
    img = blank()
    px = img.load()
    paint_plate(px)
    paint_frame(px)
    disc(px, 7.5, 7.5, 4.5, LEAD1)
    disc(px, 7.5, 7.5, 3.0, CYAN_DIM)
    disc(px, 7.5, 7.5, 1.2, CYAN_HOT)
    put_px(px, 7, 7, GEM_CORE)
    return img


def seal_inscriber_bottom() -> Image.Image:
    return artifact_assembler_bottom()


def shaft_lathe_side() -> Image.Image:
    img = blank()
    px = img.load()
    # lower half used by model UV y=8..16
    paint_fins(px)
    paint_frame(px)
    put_rect(px, 2, 9, 13, 13, SLOT)
    put_rect(px, 3, 10, 12, 12, LEAD1)
    put_px(px, 7, 11, CYAN_HOT)
    put_px(px, 8, 11, GEM_CORE)
    return img


def shaft_lathe_top() -> Image.Image:
    img = blank()
    px = img.load()
    paint_plate(px)
    paint_frame(px)
    put_rect(px, 2, 6, 13, 9, SLOT)
    put_rect(px, 2, 7, 13, 8, CYAN_DIM)
    put_px(px, 7, 7, GEM_CORE)
    put_px(px, 8, 7, CYAN_HOT)
    return img


def shaft_lathe_bottom() -> Image.Image:
    return artifact_assembler_bottom()


def shaft_lathe_saw() -> Image.Image:
    img = blank((0, 0, 0, 0))
    px = img.load()
    put_rect(px, 1, 4, 14, 11, LEAD3)
    put_rect(px, 1, 6, 14, 9, LEAD4)
    put_rect(px, 1, 7, 14, 8, CYAN_DIM)
    for x in range(2, 14, 2):
        put_px(px, x, 5, BOLT)
        put_px(px, x, 10, BOLT)
    return img


def facet_cutter_side() -> Image.Image:
    img = blank()
    px = img.load()
    paint_fins(px, shift=1)
    paint_frame(px)
    disc(px, 7.5, 11, 3.0, SLOT_LIP)
    disc(px, 7.5, 11, 2.0, CYAN_DIM)
    put_px(px, 7, 11, GEM_CORE)
    return img


def facet_cutter_top() -> Image.Image:
    img = blank()
    px = img.load()
    paint_plate(px)
    paint_frame(px)
    disc(px, 7.5, 7.5, 4.0, LEAD1)
    disc(px, 7.5, 7.5, 3.0, CYAN_DIM, hollow=2.2)
    put_rect(px, 7, 4, 8, 11, CYAN_HOT)
    put_rect(px, 4, 7, 11, 8, CYAN_HOT)
    put_px(px, 7, 7, GEM_CORE)
    return img


def facet_cutter_bottom() -> Image.Image:
    return artifact_assembler_bottom()


def facet_cutter_disc() -> Image.Image:
    img = blank((0, 0, 0, 0))
    px = img.load()
    # 8x8 used region
    for y in range(8):
        for x in range(8):
            d = ((x - 3.5) ** 2 + (y - 3.5) ** 2) ** 0.5
            if d <= 3.6:
                px[x, y] = LEAD3 if d > 2.4 else (CYAN_HOT if d > 1.0 else GEM_CORE)
            elif d <= 3.9:
                px[x, y] = EDGE
    return img


def facet_cutter_arm() -> Image.Image:
    img = blank()
    px = img.load()
    paint_plate(px)
    paint_frame(px, bolts=False)
    put_rect(px, 4, 2, 11, 13, LEAD3)
    put_rect(px, 6, 3, 9, 12, CYAN_DIM)
    return img


def facet_cutter_pivot() -> Image.Image:
    img = blank(LEAD3)
    px = img.load()
    put_rect(px, 0, 0, 7, 0, EDGE)
    put_rect(px, 0, 0, 0, 7, EDGE)
    put_px(px, 3, 3, CYAN_HOT)
    put_px(px, 4, 3, GEM_CORE)
    return img


def mortar_side() -> Image.Image:
    img = blank()
    px = img.load()
    paint_fins(px)
    paint_frame(px)
    put_rect(px, 3, 8, 12, 14, LEAD1)
    return img


def mortar_inner() -> Image.Image:
    img = blank(SLOT)
    px = img.load()
    paint_frame(px, bolts=False)
    put_rect(px, 2, 2, 13, 13, SLOT)
    put_rect(px, 4, 4, 11, 11, LEAD0)
    put_px(px, 7, 7, CYAN_DIM)
    put_px(px, 8, 8, GEM_OFF)
    return img


def mortar_rim() -> Image.Image:
    img = blank(LEAD3)
    px = img.load()
    paint_frame(px)
    put_rect(px, 2, 2, 13, 13, LEAD4)
    put_rect(px, 4, 4, 11, 11, LEAD2)
    return img


def mortar_bottom() -> Image.Image:
    return artifact_assembler_bottom()


def mortar_pestle() -> Image.Image:
    img = blank((0, 0, 0, 0))
    px = img.load()
    put_rect(px, 6, 1, 9, 14, LEAD3)
    put_rect(px, 7, 1, 8, 14, LEAD4)
    put_rect(px, 6, 12, 9, 14, CYAN_DIM)
    put_px(px, 7, 13, GEM_CORE)
    put_px(px, 8, 13, CYAN_HOT)
    return img


def phi_torch() -> Image.Image:
    # template_torch uses vertical torch strip
    img = blank((0, 0, 0, 0))
    px = img.load()
    put_rect(px, 7, 5, 8, 14, LEAD3)
    put_rect(px, 6, 8, 9, 9, LEAD4)
    # cyan flame
    put_px(px, 7, 4, CYAN_DIM)
    put_px(px, 8, 4, CYAN)
    put_px(px, 7, 3, CYAN_HOT)
    put_px(px, 8, 3, GEM_CORE)
    put_px(px, 7, 2, GEM_CORE)
    put_px(px, 6, 3, CYAN_DIM)
    put_px(px, 9, 3, CYAN_DIM)
    return img


def phi_campfire_top(lit: bool = True) -> Image.Image:
    img = blank()
    px = img.load()
    paint_plate(px)
    paint_frame(px)
    # grate
    for i in range(3, 13, 2):
        put_rect(px, i, 3, i, 12, EDGE)
        put_rect(px, 3, i, 12, i, EDGE)
    if lit:
        put_rect(px, 6, 6, 9, 9, CYAN)
        put_px(px, 7, 7, GEM_CORE)
        put_px(px, 8, 7, CYAN_HOT)
        put_px(px, 7, 8, CYAN_HOT)
    else:
        put_rect(px, 6, 6, 9, 9, SLOT)
    return img


def phi_campfire_side() -> Image.Image:
    img = blank()
    px = img.load()
    paint_fins(px, shift=2)
    paint_frame(px)
    put_rect(px, 4, 10, 11, 13, SLOT)
    put_rect(px, 6, 11, 9, 12, CYAN_DIM)
    return img


def main():
    jobs = [
        ("essence_burner.png", essence_burner(False)),
        ("essence_burner_on.png", essence_burner(True)),
        ("essence_alembic.png", essence_alembic()),
        ("clay_crucible_side.png", clay_crucible_side()),
        ("clay_crucible_top.png", clay_crucible_top()),
        ("clay_crucible_bottom.png", artifact_assembler_bottom()),
        ("phi_furnace_front.png", phi_furnace_front()),
        ("phi_furnace_side.png", phi_furnace_side()),
        ("phi_furnace_top.png", phi_furnace_top()),
        ("psi_imprinter_front.png", psi_imprinter_front()),
        ("psi_imprinter_side.png", psi_imprinter_side()),
        ("psi_imprinter_top.png", psi_imprinter_top()),
        ("phi_telegraph_side.png", phi_telegraph_side()),
        ("phi_telegraph_end.png", phi_telegraph_end()),
        ("artifact_assembler_front.png", artifact_assembler_front()),
        ("artifact_assembler_side.png", artifact_assembler_side()),
        ("artifact_assembler_top.png", artifact_assembler_top()),
        ("artifact_assembler_bottom.png", artifact_assembler_bottom()),
        ("seal_inscriber_side.png", seal_inscriber_side()),
        ("seal_inscriber_top.png", seal_inscriber_top()),
        ("seal_inscriber_bottom.png", seal_inscriber_bottom()),
        ("shaft_lathe_side.png", shaft_lathe_side()),
        ("shaft_lathe_top.png", shaft_lathe_top()),
        ("shaft_lathe_bottom.png", shaft_lathe_bottom()),
        ("shaft_lathe_saw.png", shaft_lathe_saw()),
        ("facet_cutter_side.png", facet_cutter_side()),
        ("facet_cutter_top.png", facet_cutter_top()),
        ("facet_cutter_bottom.png", facet_cutter_bottom()),
        ("facet_cutter_disc.png", facet_cutter_disc()),
        ("facet_cutter_arm.png", facet_cutter_arm()),
        ("facet_cutter_pivot.png", facet_cutter_pivot()),
        ("mortar_side.png", mortar_side()),
        ("mortar_inner.png", mortar_inner()),
        ("mortar_rim.png", mortar_rim()),
        ("mortar_bottom.png", mortar_bottom()),
        ("mortar_pestle.png", mortar_pestle()),
        ("phi_torch.png", phi_torch()),
        ("phi_campfire_top.png", phi_campfire_top(True)),
        ("phi_campfire_side.png", phi_campfire_side()),
    ]
    for name, img in jobs:
        save(name, img)

    # contact sheet of key fronts
    names = [
        "essence_burner.png",
        "essence_burner_on.png",
        "essence_alembic.png",
        "phi_furnace_front.png",
        "psi_imprinter_front.png",
        "phi_telegraph_side.png",
        "artifact_assembler_front.png",
        "seal_inscriber_side.png",
        "shaft_lathe_top.png",
        "facet_cutter_top.png",
        "mortar_side.png",
        "clay_crucible_side.png",
    ]
    sheet = Image.new("RGBA", (17 * len(names) + 2, 18), (0, 0, 0, 255))
    for i, name in enumerate(names):
        sheet.paste(Image.open(OUT / name), (2 + i * 17, 1))
    sheet.resize((sheet.width * 6, sheet.height * 6), Image.Resampling.NEAREST).save(
        ART / "tech_faces_sheet_6x.png"
    )
    print("sheet ->", ART / "tech_faces_sheet_6x.png")


if __name__ == "__main__":
    main()
