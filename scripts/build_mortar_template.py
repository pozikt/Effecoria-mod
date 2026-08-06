"""Build mortar UV atlas templates and install hollow lantern-sized model."""
from pathlib import Path
import shutil

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "art/phi_alchemy"
TMPL = ART / "mortar_template"
VAN = ART / "vanilla_refs/textures/block"
OUT_GAME = ROOT / "src/main/resources/assets/effecoria/textures/block"
OUT_MODEL = ROOT / "src/main/resources/assets/effecoria/models/block"


def phi_tint(im: Image.Image) -> Image.Image:
    im = im.convert("RGBA")
    out = []
    for r, g, b, a in im.getdata():
        if a == 0:
            out.append((0, 0, 0, 0))
            continue
        t = (0.3 * r + 0.59 * g + 0.11 * b) / 255
        out.append((int(30 + t * 90), int(50 + t * 130), int(90 + t * 150), a))
    res = Image.new("RGBA", im.size)
    res.putdata(out)
    return res


def main() -> None:
    TMPL.mkdir(parents=True, exist_ok=True)
    OUT_GAME.mkdir(parents=True, exist_ok=True)

    # UV guide atlas (16×16 + 16× upscale with labels)
    guide = Image.new("RGBA", (16, 16), (30, 30, 36, 255))
    d = ImageDraw.Draw(guide)
    d.rectangle([0, 0, 5, 5], fill=(40, 90, 140, 255), outline=(120, 200, 255, 255))
    d.rectangle([8, 0, 13, 5], fill=(90, 70, 40, 255), outline=(255, 210, 100, 255))
    d.rectangle([0, 7, 5, 12], fill=(50, 60, 90, 255), outline=(180, 200, 255, 255))
    d.rectangle([8, 7, 9, 12], fill=(120, 100, 70, 255), outline=(220, 190, 120, 255))
    d.rectangle([0, 13, 5, 15], fill=(25, 35, 55, 255), outline=(100, 120, 160, 255))
    guide.save(TMPL / "mortar_atlas_template.png")

    guide_big = guide.resize((256, 256), Image.NEAREST)
    db = ImageDraw.Draw(guide_big)
    for xy, text in [((8, 8), "SIDE"), ((136, 8), "INNER"), ((8, 120), "RIM"), ((136, 120), "PESTLE"), ((8, 216), "BOTTOM")]:
        db.text(xy, text, fill=(255, 255, 255, 230))
    guide_big.save(TMPL / "mortar_atlas_template_16x.png")

    side = phi_tint(Image.open(VAN / "cauldron_side.png"))
    inner = phi_tint(Image.open(VAN / "cauldron_inner.png"))
    top = phi_tint(Image.open(VAN / "cauldron_top.png"))
    bottom = phi_tint(Image.open(VAN / "cauldron_bottom.png"))
    lantern = Image.open(VAN / "lantern.png").convert("RGBA")

    atlas = Image.new("RGBA", (16, 16), (0, 0, 0, 0))

    def paste6(src: Image.Image, box, dest_xy):
        atlas.paste(src.crop(box).resize((6, 6), Image.NEAREST), dest_xy)

    paste6(side, (5, 5, 11, 11), (0, 0))
    paste6(inner, (5, 5, 11, 11), (8, 0))
    paste6(top, (5, 5, 11, 11), (0, 7))
    pest = phi_tint(lantern.crop((11, 1, 14, 12)).resize((2, 6), Image.NEAREST))
    atlas.paste(pest, (8, 7))
    bot = bottom.crop((5, 10, 11, 16)).resize((6, 3), Image.NEAREST)
    atlas.paste(bot, (0, 13))

    atlas.save(TMPL / "mortar_atlas_placeholder.png")
    atlas.save(OUT_GAME / "mortar_and_pestle.png")

    shutil.copy(TMPL / "mortar_and_pestle.json", OUT_MODEL / "mortar_and_pestle.json")

    flat = ART / "for_artist"
    flat.mkdir(exist_ok=True)
    for name in [
        "cauldron_side.png",
        "cauldron_top.png",
        "cauldron_bottom.png",
        "cauldron_inner.png",
        "lantern.png",
    ]:
        shutil.copy(VAN / name, flat / name)
    shutil.copy(ART / "vanilla_refs/textures/item/cauldron.png", flat / "cauldron_item.png")
    shutil.copy(ART / "vanilla_refs/textures/item/lantern.png", flat / "lantern_item.png")
    if (ART / "mortar_and_pestle_current.png").exists():
        shutil.copy(ART / "mortar_and_pestle_current.png", flat / "mortar_current_cube.png")
    shutil.copy(TMPL / "mortar_atlas_template_16x.png", flat / "mortar_atlas_template_16x.png")
    shutil.copy(TMPL / "mortar_atlas_placeholder.png", flat / "mortar_atlas_placeholder.png")

    print("ok", flat)


if __name__ == "__main__":
    main()
