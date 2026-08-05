"""Import art/phi_earth/phi_grass_faces_sheet_32.png into game textures (96x32: top | side | bottom)."""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SHEET = ROOT / "art/phi_earth/phi_grass_faces_sheet_32.png"
OUT = ROOT / "src/main/resources/assets/effecoria/textures/block"
TILE = 32


def main() -> None:
    im = Image.open(SHEET).convert("RGBA")
    if im.width < TILE * 3:
        raise SystemExit(f"Sheet must be at least {TILE * 3}px wide, got {im.width}")
    OUT.mkdir(parents=True, exist_ok=True)
    im.crop((0, 0, TILE, TILE)).save(OUT / "phi_grass_top.png")
    im.crop((TILE, 0, TILE * 2, TILE)).save(OUT / "phi_grass_side.png")
    im.crop((TILE * 2, 0, TILE * 3, TILE)).save(OUT / "phi_grass_bottom.png")
    im.crop((TILE * 2, 0, TILE * 3, TILE)).save(OUT / "phi_dirt.png")
    print("Imported phi grass/dirt textures from", SHEET)


if __name__ == "__main__":
    main()
