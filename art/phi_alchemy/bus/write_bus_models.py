"""Bake continuous phi-bus cable textures + multipart wire models."""
from __future__ import annotations

import json
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[3]
TEX = ROOT / "src/main/resources/assets/effecoria/textures/block"
MODELS = ROOT / "src/main/resources/assets/effecoria/models/block"
BLOCKSTATES = ROOT / "src/main/resources/assets/effecoria/blockstates"

# Wire cross-section in model units
A, B = 6, 10


def cable_texture(powered: bool) -> Image.Image:
    """16x16 texture that looks like a cable sheath when wrapped on a thin rod."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    # Dark metal sheath with a bright Φ core stripe down the middle.
    sheath = (38, 42, 48, 255) if not powered else (48, 40, 28, 255)
    sheath_hi = (62, 68, 78, 255) if not powered else (90, 70, 40, 255)
    sheath_lo = (22, 24, 28, 255) if not powered else (28, 22, 16, 255)
    core = (120, 210, 255, 255) if not powered else (255, 200, 80, 255)
    core_hi = (200, 240, 255, 255) if not powered else (255, 240, 160, 255)

    for y in range(16):
        for x in range(16):
            # Vertical cable motif: edges dark, mid bright core
            if x <= 1 or x >= 14:
                px[x, y] = sheath_lo
            elif x in (2, 13):
                px[x, y] = sheath
            elif x in (3, 12):
                px[x, y] = sheath_hi
            elif x in (7, 8):
                px[x, y] = core_hi if (y % 4) != 0 else core
            elif x in (6, 9):
                px[x, y] = core
            else:
                px[x, y] = sheath
            # subtle banding along length
            if y % 5 == 0 and 3 <= x <= 12 and x not in (6, 7, 8, 9):
                r, g, b, a = px[x, y]
                px[x, y] = (max(0, r - 12), max(0, g - 12), max(0, b - 12), a)
    return img


def faces(tex: str = "#cable") -> dict:
    # Map so the core stripe runs along the arm length on long faces.
    return {
        "north": {"uv": [6, 0, 10, 16], "texture": tex},
        "south": {"uv": [6, 0, 10, 16], "texture": tex},
        "west": {"uv": [0, 0, 16, 4], "texture": tex, "rotation": 90},
        "east": {"uv": [0, 0, 16, 4], "texture": tex, "rotation": 90},
        "up": {"uv": [0, 0, 16, 4], "texture": tex},
        "down": {"uv": [0, 0, 16, 4], "texture": tex},
    }


def arm_north_model(texture: str) -> dict:
    """Half-arm toward NORTH (z=0..10). y-rotations cover E/S/W."""
    return {
        "textures": {"cable": texture, "particle": texture},
        "elements": [{"from": [A, A, 0], "to": [B, B, 10], "faces": faces()}],
    }


def arm_up_model(texture: str) -> dict:
    """Half-arm toward UP (y=6..16)."""
    return {
        "textures": {"cable": texture, "particle": texture},
        "elements": [
            {
                "from": [A, A, A],
                "to": [B, 16, B],
                "faces": {
                    "north": {"uv": [0, 0, 4, 16], "texture": "#cable"},
                    "south": {"uv": [0, 0, 4, 16], "texture": "#cable"},
                    "west": {"uv": [0, 0, 4, 16], "texture": "#cable"},
                    "east": {"uv": [0, 0, 4, 16], "texture": "#cable"},
                    "up": {"uv": [6, 6, 10, 10], "texture": "#cable"},
                    "down": {"uv": [6, 6, 10, 10], "texture": "#cable"},
                },
            }
        ],
    }


def arm_down_model(texture: str) -> dict:
    """Half-arm toward DOWN (y=0..10)."""
    return {
        "textures": {"cable": texture, "particle": texture},
        "elements": [
            {
                "from": [A, 0, A],
                "to": [B, B, B],
                "faces": {
                    "north": {"uv": [0, 0, 4, 16], "texture": "#cable"},
                    "south": {"uv": [0, 0, 4, 16], "texture": "#cable"},
                    "west": {"uv": [0, 0, 4, 16], "texture": "#cable"},
                    "east": {"uv": [0, 0, 4, 16], "texture": "#cable"},
                    "up": {"uv": [6, 6, 10, 10], "texture": "#cable"},
                    "down": {"uv": [6, 6, 10, 10], "texture": "#cable"},
                },
            }
        ],
    }


def core_model(texture: str) -> dict:
    return {
        "textures": {"cable": texture, "particle": texture},
        "elements": [
            {
                "from": [A, A, A],
                "to": [B, B, B],
                "faces": {
                    side: {"uv": [6, 6, 10, 10], "texture": "#cable"}
                    for side in ("north", "south", "west", "east", "up", "down")
                },
            }
        ],
    }


def inventory_model(texture: str) -> dict:
    """Item / fallback: small + cross so it reads as a wire segment."""
    return {
        "textures": {"cable": texture, "particle": texture},
        "elements": [
            {"from": [A, A, 0], "to": [B, B, 16], "faces": faces()},
            {"from": [0, A, A], "to": [16, B, B], "faces": faces()},
        ],
    }


def multipart_blockstate() -> dict:
    def part(when: dict, model: str, **rot):
        apply = {"model": model}
        apply.update(rot)
        return {"when": when, "apply": apply}

    parts = []
    for powered, suffix in ((False, ""), (True, "_on")):
        p = str(powered).lower()
        core = f"effecoria:block/phi_bus_core{suffix}"
        arm = f"effecoria:block/phi_bus_arm{suffix}"
        arm_u = f"effecoria:block/phi_bus_arm_up{suffix}"
        arm_d = f"effecoria:block/phi_bus_arm_down{suffix}"
        parts.append(part({"powered": p}, core))
        parts.append(part({"powered": p, "north": "true"}, arm))
        parts.append(part({"powered": p, "east": "true"}, arm, y=90))
        parts.append(part({"powered": p, "south": "true"}, arm, y=180))
        parts.append(part({"powered": p, "west": "true"}, arm, y=270))
        parts.append(part({"powered": p, "up": "true"}, arm_u))
        parts.append(part({"powered": p, "down": "true"}, arm_d))
    return {"multipart": parts}


def main() -> None:
    TEX.mkdir(parents=True, exist_ok=True)
    MODELS.mkdir(parents=True, exist_ok=True)

    cable_texture(False).save(TEX / "phi_bus.png")
    cable_texture(True).save(TEX / "phi_bus_on.png")
    print("textures ok")

    models = {
        "phi_bus_core.json": core_model("effecoria:block/phi_bus"),
        "phi_bus_core_on.json": core_model("effecoria:block/phi_bus_on"),
        "phi_bus_arm.json": arm_north_model("effecoria:block/phi_bus"),
        "phi_bus_arm_on.json": arm_north_model("effecoria:block/phi_bus_on"),
        "phi_bus_arm_up.json": arm_up_model("effecoria:block/phi_bus"),
        "phi_bus_arm_up_on.json": arm_up_model("effecoria:block/phi_bus_on"),
        "phi_bus_arm_down.json": arm_down_model("effecoria:block/phi_bus"),
        "phi_bus_arm_down_on.json": arm_down_model("effecoria:block/phi_bus_on"),
        "phi_bus.json": inventory_model("effecoria:block/phi_bus"),
        "phi_bus_on.json": inventory_model("effecoria:block/phi_bus_on"),
    }
    for name, data in models.items():
        (MODELS / name).write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
        print("model", name)

    (BLOCKSTATES / "phi_bus.json").write_text(
        json.dumps(multipart_blockstate(), indent=2) + "\n", encoding="utf-8"
    )
    print("blockstate ok")

    # Remove obsolete shape-variant models
    obsolete = [
        "phi_bus_single",
        "phi_bus_single_on",
        "phi_bus_end_n",
        "phi_bus_end_n_on",
        "phi_bus_straight_ns",
        "phi_bus_straight_ns_on",
        "phi_bus_corner_ne",
        "phi_bus_corner_ne_on",
        "phi_bus_t_missing_s",
        "phi_bus_t_missing_s_on",
        "phi_bus_cross",
        "phi_bus_cross_on",
    ]
    for stem in obsolete:
        p = MODELS / f"{stem}.json"
        if p.exists():
            p.unlink()
            print("removed", p.name)


if __name__ == "__main__":
    main()
