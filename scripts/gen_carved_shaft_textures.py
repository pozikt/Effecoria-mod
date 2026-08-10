"""Scale vanilla stick into length-profile shaft item textures (see art/stick_vanilla_ref.png)."""
from __future__ import annotations

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
STICK_SRC = ROOT / "art" / "stick_vanilla_ref.png"
ROD_SRC = ROOT / "art" / "blaze_rod_vanilla_ref.png"
OUT_SHAFT = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "item" / "carved_shaft"
OUT_STAFF = ROOT / "src" / "main" / "resources" / "assets" / "effecoria" / "textures" / "item" / "modular_staff"

# Physical lengths from shaft_forms datapack; wand is the shortest visual baseline.
PROFILES = {
    "wand": 0.6,
    "baton": 1.0,
    "long_staff": 1.4,
    "stature": 1.8,
}
BASE_M = 0.6
BASE_H = 16


def scale_stick(stick: Image.Image, meters: float) -> Image.Image:
    h = max(BASE_H, int(round(BASE_H * meters / BASE_M)))
    return stick.resize((16, h), Image.Resampling.NEAREST)


def main() -> None:
    stick = Image.open(STICK_SRC).convert("RGBA")
    rod = Image.open(ROD_SRC).convert("RGBA")
    OUT_SHAFT.mkdir(parents=True, exist_ok=True)
    OUT_STAFF.mkdir(parents=True, exist_ok=True)
    for name, meters in PROFILES.items():
        scale_stick(stick, meters).save(OUT_SHAFT / f"{name}.png")
        scale_stick(rod, meters).save(OUT_STAFF / f"{name}.png")
    print(f"Wrote {len(PROFILES)} profiles to {OUT_SHAFT} and {OUT_STAFF}")


if __name__ == "__main__":
    main()
