"""Create RGB 32x32 phi_stone template with ultramarine swatch for Aseprite."""
from pathlib import Path

from PIL import Image

out = Path(__file__).resolve().parents[1] / "art/essence_plateau/phi_stone/phi_stone_32.png"
ref = out.parent / "vanilla_stone_16.png"

if ref.is_file():
    base = Image.open(ref).convert("RGBA").resize((32, 32), Image.Resampling.NEAREST)
else:
    base = Image.new("RGBA", (32, 32), (125, 125, 125, 255))

# RGB ultramarine swatch (bottom-right 6x6) — pipette test area
ultra = (18, 10, 143, 255)  # #120A8F approx
px = base.load()
for y in range(26, 32):
    for x in range(26, 32):
        px[x, y] = ultra

out.parent.mkdir(parents=True, exist_ok=True)
base.save(out)
print("Wrote RGB template:", out, base.mode)
