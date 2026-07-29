#!/usr/bin/env python3
"""Generate placeholder spell icons and particle textures for Effecoria."""
from __future__ import annotations

import math
import json
import shutil
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
OUT_GUI = ROOT / "src/main/resources/assets/effecoria/textures/gui/sprites/spells"
OUT_PARTICLE = ROOT / "src/main/resources/assets/effecoria/textures/particle"
OUT_ENTITY = ROOT / "src/main/resources/assets/effecoria/textures/entity"
OUT_PARTICLES_JSON = ROOT / "src/main/resources/assets/effecoria/particles"
CURSOR_ASSETS = Path(r"C:\Users\2005t\.cursor\projects\c-Users-2005t-Effecoria\assets")

ICON = 64
PARTICLE = 16

SCHOOLS = {
    "mental": ((120, 220, 255), (80, 160, 255), (40, 80, 160)),
    "elemental": ((255, 180, 60), (255, 110, 40), (180, 60, 20)),
    "organic": ((120, 255, 100), (60, 200, 70), (30, 120, 40)),
    "necromancy": ((100, 255, 140), (40, 160, 80), (20, 80, 50)),
    "spatial": ((140, 210, 255), (80, 140, 255), (50, 80, 180)),
    "corruption": ((220, 120, 255), (140, 60, 160), (80, 30, 90)),
    "seals": ((255, 220, 100), (220, 170, 60), (140, 100, 30)),
}

SPELL_SCHOOL = {
    "mental_push": "mental", "mental_sting": "mental", "sense_phi": "mental",
    "mind_lance": "mental", "psychic_focus": "mental",
    "fire_burst": "elemental", "wind_push": "elemental", "water_stream": "elemental",
    "steam_jet": "elemental", "steam_veil": "elemental", "ember_volley": "elemental",
    "ice_shard": "elemental", "frost_bastion": "elemental", "plasma_bolt": "elemental",
    "hydro_slice": "elemental",
    "vitality_pulse": "organic", "thorn_lash": "organic", "root_bind": "organic",
    "briar_surge": "organic", "verdant_mend": "organic",
    "soul_drain": "necromancy", "wither_touch": "necromancy", "shade_summon": "necromancy",
    "grave_leech": "necromancy", "shade_swarm": "necromancy",
    "blink": "spatial", "rift_yank": "spatial", "phase_veil": "spatial",
    "void_step": "spatial", "gravity_well": "spatial",
    "corrupt_mark": "corruption", "binding_seal": "corruption", "blight_pulse": "corruption",
    "blight_brand": "corruption", "pestilence_wave": "corruption",
    "trap_seal": "seals", "fortify_seal": "seals", "glow_seal": "seals",
    "snare_glyph": "seals", "beacon_seal": "seals",
}


def lerp(a: int, b: int, t: float) -> int:
    return int(a + (b - a) * t)


def with_alpha(color, alpha: int):
    return color[:3] + (alpha,)


def draw_disc(draw: ImageDraw.ImageDraw, cx: int, cy: int, r: int, fill, outline=None, width=2):
    draw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=fill, outline=outline, width=width)


def apply_circle_mask(img: Image.Image) -> Image.Image:
    """Clip icon to a circle for hub constellation nodes."""
    mask = Image.new("L", (ICON, ICON), 0)
    md = ImageDraw.Draw(mask)
    md.ellipse((1, 1, ICON - 2, ICON - 2), fill=255)
    out = Image.new("RGBA", (ICON, ICON), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def base_icon(school: str) -> Image.Image:
    """High-contrast tile: dark fill, bright school rim, bold glyph on top."""
    accent, _, rim = SCHOOLS[school]
    bg = (14, 14, 22, 255)
    img = Image.new("RGBA", (ICON, ICON), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = ICON // 2, ICON // 2
    draw_disc(d, cx, cy, 30, accent + (255,))
    draw_disc(d, cx, cy, 26, bg)
    draw_disc(d, cx, cy, 26, None, outline=rim + (255,), width=3)
    draw_disc(d, cx, cy, 22, (22, 22, 32, 255))
    return img, d, cx, cy


def icon_arrow(d, cx, cy, color):
    d.polygon([(cx + 14, cy), (cx - 10, cy - 10), (cx - 10, cy + 10)], fill=color)


def icon_spike(d, cx, cy, color):
    d.polygon([(cx, cy - 16), (cx - 8, cy + 12), (cx + 8, cy + 12)], fill=color)


def icon_eye(d, cx, cy, color):
    draw_disc(d, cx, cy, 10, color)
    draw_disc(d, cx, cy, 4, (20, 20, 30, 255))


def icon_lance(d, cx, cy, color):
    d.line([(cx - 16, cy + 12), (cx + 16, cy - 12)], fill=color, width=4)
    d.polygon([(cx + 16, cy - 12), (cx + 8, cy - 8), (cx + 12, cy - 4)], fill=color)


def icon_focus(d, cx, cy, color):
    draw_disc(d, cx, cy, 8, color)
    d.ellipse((cx - 14, cy - 18, cx + 14, cy - 4), outline=color, width=2)


def icon_fire(d, cx, cy, color):
    d.polygon([(cx, cy - 16), (cx + 10, cy + 4), (cx, cy + 14), (cx - 10, cy + 4)], fill=color)


def icon_wind(d, cx, cy, color):
    for i in range(3):
        y = cy - 6 + i * 6
        d.arc((cx - 14 + i * 2, y - 4, cx + 6 + i * 2, y + 8), 300, 120, fill=color, width=2)


def icon_water(d, cx, cy, color):
    d.polygon([(cx, cy - 12), (cx + 10, cy + 8), (cx - 10, cy + 8)], fill=color)
    d.line([(cx - 14, cy + 4), (cx + 14, cy + 4)], fill=color, width=2)


def icon_steam(d, cx, cy, color):
    for ox in (-6, 0, 6):
        d.arc((cx + ox - 4, cy - 10, cx + ox + 4, cy + 2), 200, 340, fill=color, width=2)


def icon_embers(d, cx, cy, color):
    for ox, oy in [(-8, 4), (0, -6), (8, 4)]:
        draw_disc(d, cx + ox, cy + oy, 4, color)


def icon_heart(d, cx, cy, color):
    d.polygon([(cx, cy + 10), (cx - 12, cy - 2), (cx, cy - 10), (cx + 12, cy - 2)], fill=color)


def icon_thorns(d, cx, cy, color):
    for ang in range(0, 360, 60):
        rad = math.radians(ang)
        x2 = cx + int(math.cos(rad) * 14)
        y2 = cy + int(math.sin(rad) * 14)
        d.line([(cx, cy), (x2, y2)], fill=color, width=3)


def icon_roots(d, cx, cy, color):
    d.line([(cx, cy - 8), (cx, cy + 12)], fill=color, width=3)
    d.line([(cx, cy + 4), (cx - 10, cy + 12)], fill=color, width=2)
    d.line([(cx, cy + 4), (cx + 10, cy + 12)], fill=color, width=2)


def icon_briar(d, cx, cy, color):
    icon_thorns(d, cx, cy, color)
    draw_disc(d, cx, cy, 5, (40, 80, 40, 255))


def icon_leaf(d, cx, cy, color):
    d.polygon([(cx, cy - 12), (cx + 10, cy), (cx, cy + 12), (cx - 10, cy)], fill=color)


def icon_soul(d, cx, cy, color):
    draw_disc(d, cx, cy - 4, 8, color)
    d.polygon([(cx - 8, cy), (cx + 8, cy), (cx, cy + 14)], fill=color)


def icon_skull(d, cx, cy, color):
    draw_disc(d, cx, cy - 2, 10, color)
    d.rectangle((cx - 8, cy + 4, cx + 8, cy + 12), fill=color)


def icon_shade(d, cx, cy, color):
    d.polygon([(cx, cy - 14), (cx + 12, cy + 12), (cx - 12, cy + 12)], fill=with_alpha(color, 200))


def icon_shades(d, cx, cy, color):
    for ox in (-8, 0, 8):
        d.polygon([(cx + ox, cy - 10), (cx + ox + 6, cy + 8), (cx + ox - 6, cy + 8)], fill=color)


def icon_blink(d, cx, cy, color):
    draw_disc(d, cx, cy, 12, None, outline=color, width=2)
    draw_disc(d, cx - 4, cy, 3, color)
    draw_disc(d, cx + 6, cy, 3, color)


def icon_rift(d, cx, cy, color):
    d.arc((cx - 14, cy - 14, cx + 14, cy + 14), 45, 270, fill=color, width=3)


def icon_veil(d, cx, cy, color):
    d.ellipse((cx - 12, cy - 16, cx + 12, cy + 10), outline=color, width=2)
    d.line([(cx - 8, cy + 6), (cx + 8, cy + 6)], fill=color, width=2)


def icon_void(d, cx, cy, color):
    draw_disc(d, cx, cy, 10, (10, 10, 20, 255))
    draw_disc(d, cx, cy, 10, None, outline=color, width=2)


def icon_well(d, cx, cy, color):
    for r in (14, 10, 6):
        draw_disc(d, cx, cy, r, None, outline=color, width=1)


def icon_mark(d, cx, cy, color):
    d.line([(cx, cy - 14), (cx, cy + 14)], fill=color, width=3)
    d.line([(cx - 10, cy - 6), (cx + 10, cy - 6)], fill=color, width=3)


def icon_chain(d, cx, cy, color):
    draw_disc(d, cx - 6, cy, 6, None, outline=color, width=2)
    draw_disc(d, cx + 6, cy, 6, None, outline=color, width=2)


def icon_pulse(d, cx, cy, color):
    pts = []
    for x in range(-14, 15, 2):
        y = cy + int(6 * math.sin(x / 3))
        pts.append((cx + x, y))
    d.line(pts, fill=color, width=2)


def icon_wave(d, cx, cy, color):
    d.arc((cx - 16, cy - 8, cx, cy + 8), 300, 60, fill=color, width=2)
    d.arc((cx - 8, cy - 8, cx + 8, cy + 8), 300, 60, fill=color, width=2)


def icon_trap(d, cx, cy, color):
    d.polygon([(cx, cy - 12), (cx + 12, cy + 10), (cx - 12, cy + 10)], outline=color, width=2)


def icon_shield(d, cx, cy, color):
    d.polygon([(cx, cy - 14), (cx + 12, cy - 4), (cx + 8, cy + 12), (cx - 8, cy + 12), (cx - 12, cy - 4)], outline=color, width=2)


def icon_glow(d, cx, cy, color):
    draw_disc(d, cx, cy, 6, color)
    for r in (10, 14):
        draw_disc(d, cx, cy, r, None, outline=with_alpha(color, 120), width=1)


def icon_snare(d, cx, cy, color):
    draw_disc(d, cx, cy, 12, None, outline=color, width=2)
    d.line([(cx - 8, cy - 8), (cx + 8, cy + 8)], fill=color, width=2)
    d.line([(cx + 8, cy - 8), (cx - 8, cy + 8)], fill=color, width=2)


def icon_beacon(d, cx, cy, color):
    d.polygon([(cx, cy - 14), (cx + 6, cy + 10), (cx - 6, cy + 10)], fill=color)
    d.line([(cx, cy - 14), (cx, cy + 16)], fill=(255, 255, 200, 200), width=2)


def icon_hydro_slice(d, cx, cy, color):
    d.line([(cx - 14, cy + 2), (cx + 14, cy + 2)], fill=color, width=3)
    d.polygon([(cx, cy - 10), (cx + 8, cy + 6), (cx - 8, cy + 6)], fill=(120, 200, 255, 200))


def icon_ice(d, cx, cy, color):
    d.polygon([(cx, cy - 14), (cx + 6, cy - 2), (cx + 10, cy + 10), (cx, cy + 6), (cx - 10, cy + 10), (cx - 6, cy - 2)], fill=color)


def icon_frost_wall(d, cx, cy, color):
    icon_shield(d, cx, cy, color)
    d.line([(cx - 8, cy - 6), (cx + 8, cy + 6)], fill=(220, 240, 255, 200), width=2)


def icon_plasma(d, cx, cy, color):
    icon_fire(d, cx, cy, (255, 140, 255, 255))
    draw_disc(d, cx, cy, 4, (180, 100, 255, 180))


def icon_steam_veil(d, cx, cy, color):
    icon_steam(d, cx, cy, color)
    d.ellipse((cx - 14, cy - 16, cx + 14, cy + 4), outline=color, width=2)


DRAWERS = {
    "mental_push": icon_arrow,
    "mental_sting": icon_spike,
    "sense_phi": icon_eye,
    "mind_lance": icon_lance,
    "psychic_focus": icon_focus,
    "fire_burst": icon_fire,
    "wind_push": icon_wind,
    "water_stream": icon_water,
    "steam_jet": icon_steam,
    "steam_veil": icon_steam_veil,
    "ember_volley": icon_embers,
    "ice_shard": icon_ice,
    "frost_bastion": icon_frost_wall,
    "plasma_bolt": icon_plasma,
    "hydro_slice": icon_hydro_slice,
    "vitality_pulse": icon_heart,
    "thorn_lash": icon_thorns,
    "root_bind": icon_roots,
    "briar_surge": icon_briar,
    "verdant_mend": icon_leaf,
    "soul_drain": icon_soul,
    "wither_touch": icon_skull,
    "shade_summon": icon_shade,
    "grave_leech": icon_soul,
    "shade_swarm": icon_shades,
    "blink": icon_blink,
    "rift_yank": icon_rift,
    "phase_veil": icon_veil,
    "void_step": icon_void,
    "gravity_well": icon_well,
    "corrupt_mark": icon_mark,
    "binding_seal": icon_chain,
    "blight_pulse": icon_pulse,
    "blight_brand": icon_mark,
    "pestilence_wave": icon_wave,
    "trap_seal": icon_trap,
    "fortify_seal": icon_shield,
    "glow_seal": icon_glow,
    "snare_glyph": icon_snare,
    "beacon_seal": icon_beacon,
}


def make_icon(spell: str) -> Image.Image:
    school = SPELL_SCHOOL[spell]
    sym = (255, 255, 255, 255)
    img, d, cx, cy = base_icon(school)
    DRAWERS[spell](d, cx, cy, sym)
    return apply_circle_mask(img)


def make_particle(name: str, inner, outer) -> Image.Image:
    """Legacy orb — kept for entity fallback."""
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx = cy = PARTICLE // 2
    for i in range(6, 0, -1):
        t = i / 6
        col = (
            lerp(outer[0], inner[0], t),
            lerp(outer[1], inner[1], t),
            lerp(outer[2], inner[2], t),
            255,
        )
        draw_disc(d, cx, cy, i, col)
    return img


def save_particle_png(name: str, img: Image.Image):
    img.save(OUT_PARTICLE / f"{name}.png")


def write_particle_json(particle_id: str, textures: list[str]):
    OUT_PARTICLES_JSON.mkdir(parents=True, exist_ok=True)
    (OUT_PARTICLES_JSON / f"{particle_id}.json").write_text(
        json.dumps({"textures": textures}, indent=2) + "\n",
        encoding="utf-8",
    )


def save_particle(name: str, img: Image.Image):
    save_particle_png(name, img)
    write_particle_json(name, [f"effecoria:{name}"])


def particle_water_drop(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2 - 1
    tilt = variant * 2 - 2
    d.polygon(
        [(cx + tilt, cy - 5), (cx + 4 + tilt, cy + 2), (cx + tilt, cy + 6), (cx - 4 + tilt, cy + 2)],
        fill=(80, 170, 255, 230),
    )
    d.ellipse((cx - 3 + tilt, cy - 2, cx + 3 + tilt, cy + 4), fill=(140, 210, 255, 200))
    return img


def particle_water_splash() -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2 + 2
    d.arc((cx - 7, cy - 5, cx + 7, cy + 3), 200, 340, fill=(100, 190, 255, 220), width=2)
    for ang in (250, 270, 290):
        rad = math.radians(ang)
        x2 = cx + int(math.cos(rad) * 6)
        y2 = cy + int(math.sin(rad) * 4)
        d.line([(cx, cy), (x2, y2)], fill=(160, 220, 255, 180), width=1)
    return img


def particle_water_wave() -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    d.arc((cx - 8, cy - 2, cx + 8, cy + 6), 190, 350, fill=(90, 160, 240, 200), width=2)
    d.arc((cx - 6, cy, cx + 6, cy + 8), 190, 350, fill=(120, 190, 255, 140), width=1)
    return img


def particle_phi_flame() -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2 + 2
    d.polygon([(cx, cy - 7), (cx + 5, cy + 2), (cx, cy + 5), (cx - 5, cy + 2)], fill=(255, 180, 60, 240))
    d.polygon([(cx, cy - 5), (cx + 3, cy + 1), (cx, cy + 3), (cx - 3, cy + 1)], fill=(255, 240, 120, 220))
    d.ellipse((cx - 4, cy + 2, cx + 4, cy + 6), outline=(170, 70, 210, 180), width=1)
    return img


def particle_phi_gust() -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for i in range(3):
        y = 4 + i * 4
        d.line([(2, y), (PARTICLE - 2 - i, y)], fill=(220, 235, 255, 200 - i * 30), width=2)
    return img


def particle_mental_fog(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2 + variant - 1, PARTICLE // 2
    for r, a in ((7, 50), (5, 80), (3, 120)):
        draw_disc(d, cx, cy, r, (150, 120, 255, a))
    draw_disc(d, cx, cy, 2, (200, 180, 255, 90))
    return img


def particle_organic_leaf(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    hue = variant * 15
    fill = (60 + hue, 170 - hue // 2, 70, 230)
    d.polygon([(cx, cy - 6), (cx + 5, cy), (cx, cy + 6), (cx - 5, cy)], fill=fill)
    d.line([(cx, cy - 5), (cx, cy + 5)], fill=(40, 110, 50, 200), width=1)
    return img


def particle_organic_root() -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    d.line([(cx, cy - 6), (cx, cy + 6)], fill=(80, 55, 30, 240), width=2)
    d.line([(cx, cy + 1), (cx - 5, cy + 6)], fill=(90, 60, 35, 220), width=2)
    d.line([(cx, cy + 1), (cx + 5, cy + 6)], fill=(90, 60, 35, 220), width=2)
    return img


def particle_organic_fog() -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    for r, a in ((7, 45), (5, 70), (3, 100)):
        draw_disc(d, cx, cy, r, (80, 200, 90, a))
    return img


def particle_necro_shadow(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2 + variant - 1, PARTICLE // 2 + 1
    d.polygon(
        [(cx, cy - 5), (cx + 6, cy + 4), (cx + 2, cy + 6), (cx - 2, cy + 6), (cx - 6, cy + 4)],
        fill=(15, 10, 25, 220),
    )
    d.polygon([(cx, cy - 3), (cx + 3, cy + 2), (cx - 3, cy + 2)], fill=(40, 30, 55, 180))
    return img


def particle_necro_fog() -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    for r, a in ((7, 60), (5, 90), (3, 120)):
        draw_disc(d, cx, cy, r, (10, 8, 18, a))
    return img


def particle_spatial_rift(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    shift = variant * 2
    d.polygon(
        [(cx - 2 + shift, cy - 7), (cx + 4, cy - 1), (cx + 1, cy + 7), (cx - 5, cy + 2)],
        fill=(120, 160, 255, 200),
    )
    d.polygon(
        [(cx + 2 - shift, cy - 6), (cx + 6, cy), (cx + 3, cy + 6), (cx - 2, cy + 1)],
        fill=(180, 120, 255, 160),
    )
    return img


def particle_spatial_warp() -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    d.line([(cx - 7, cy - 3), (cx + 7, cy + 3)], fill=(140, 100, 255, 200), width=2)
    d.line([(cx - 7, cy + 3), (cx + 7, cy - 3)], fill=(100, 180, 255, 160), width=1)
    d.ellipse((cx - 4, cy - 4, cx + 4, cy + 4), outline=(200, 160, 255, 120), width=1)
    return img


def particle_corruption_poison() -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2 - 1
    d.polygon([(cx, cy - 5), (cx + 3, cy + 1), (cx, cy + 6), (cx - 3, cy + 1)], fill=(90, 220, 60, 230))
    d.ellipse((cx - 2, cy, cx + 2, cy + 4), fill=(140, 255, 100, 180))
    return img


def particle_corruption_blood() -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    draw_disc(d, cx - 2, cy - 1, 2, (180, 20, 30, 230))
    draw_disc(d, cx + 2, cy, 2, (140, 10, 20, 210))
    draw_disc(d, cx, cy + 3, 1, (200, 40, 50, 200))
    return img


def particle_corruption_rune(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    col = (160, 60, 180, 220) if variant == 0 else (100, 200, 70, 220)
    draw_disc(d, cx, cy, 5, None, outline=col, width=2)
    d.line([(cx, cy - 4), (cx, cy + 4)], fill=col, width=1)
    d.line([(cx - 4, cy), (cx + 4, cy)], fill=col, width=1)
    if variant == 1:
        d.line([(cx - 3, cy - 3), (cx + 3, cy + 3)], fill=col, width=1)
    return img


def particle_seal_glyph(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    gold = (240, 200, 90, 230)
    purple = (170, 120, 255, 200)
    if variant == 0:
        draw_disc(d, cx, cy, 6, None, outline=gold, width=2)
        d.polygon([(cx, cy - 4), (cx + 3, cy + 3), (cx - 3, cy + 3)], outline=purple, width=1)
    elif variant == 1:
        d.rectangle((cx - 5, cy - 5, cx + 5, cy + 5), outline=gold, width=2)
        d.line([(cx - 4, cy), (cx + 4, cy)], fill=purple, width=1)
        d.line([(cx, cy - 4), (cx, cy + 4)], fill=purple, width=1)
    else:
        for ang in (0, 120, 240):
            rad = math.radians(ang)
            x2 = cx + int(math.cos(rad) * 5)
            y2 = cy + int(math.sin(rad) * 5)
            d.line([(cx, cy), (x2, y2)], fill=gold, width=2)
        draw_disc(d, cx, cy, 2, purple)
    return img


def particle_seal_spark() -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    for ang in range(0, 360, 90):
        rad = math.radians(ang)
        x2 = cx + int(math.cos(rad) * 5)
        y2 = cy + int(math.sin(rad) * 5)
        d.line([(cx, cy), (x2, y2)], fill=(255, 230, 140, 230), width=1)
    draw_disc(d, cx, cy, 2, (255, 250, 200, 240))
    return img


def particle_phi_spark() -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    d.line([(cx - 4, cy), (cx + 4, cy)], fill=(255, 240, 180, 230), width=1)
    d.line([(cx, cy - 4), (cx, cy + 4)], fill=(200, 140, 255, 230), width=1)
    draw_disc(d, cx, cy, 2, (255, 255, 255, 200))
    return img


def particle_steam_fog() -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    for r, a in ((7, 40), (5, 70), (3, 110)):
        draw_disc(d, cx, cy, r, (210, 220, 230, a))
    return img


def particle_ice_crystal() -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    d.polygon([(cx, cy - 6), (cx + 4, cy + 5), (cx - 4, cy + 5)], fill=(180, 230, 255, 230))
    d.line([(cx, cy - 5), (cx, cy + 4)], fill=(240, 250, 255, 200), width=1)
    return img


def generate_school_particles():
    """Unique sprites + JSON for each magic-school particle type."""
    # Water — drops, splashes, waves (multi-frame sprite sets)
    drop_paths = []
    for i in range(3):
        name = f"water_drop_{i}"
        save_particle_png(name, particle_water_drop(i))
        drop_paths.append(f"effecoria:{name}")
    write_particle_json("water_drop", drop_paths)

    save_particle("water_splash", particle_water_splash())
    save_particle("water_wave", particle_water_wave())

    save_particle("phi_flame", particle_phi_flame())
    save_particle("phi_gust", particle_phi_gust())
    save_particle("phi_spark", particle_phi_spark())

    fog_paths = []
    for i in range(3):
        name = f"mental_fog_{i}"
        save_particle_png(name, particle_mental_fog(i))
        fog_paths.append(f"effecoria:{name}")
    write_particle_json("mental_fog", fog_paths)

    leaf_paths = []
    for i in range(3):
        name = f"organic_leaf_{i}"
        save_particle_png(name, particle_organic_leaf(i))
        leaf_paths.append(f"effecoria:{name}")
    write_particle_json("organic_leaf", leaf_paths)
    save_particle("organic_root", particle_organic_root())
    save_particle("organic_fog", particle_organic_fog())

    shadow_paths = []
    for i in range(3):
        name = f"necro_shadow_{i}"
        save_particle_png(name, particle_necro_shadow(i))
        shadow_paths.append(f"effecoria:{name}")
    write_particle_json("necro_shadow", shadow_paths)
    save_particle("necro_fog", particle_necro_fog())

    rift_paths = []
    for i in range(3):
        name = f"spatial_rift_{i}"
        save_particle_png(name, particle_spatial_rift(i))
        rift_paths.append(f"effecoria:{name}")
    write_particle_json("spatial_rift", rift_paths)
    save_particle("spatial_warp", particle_spatial_warp())

    save_particle("corruption_poison", particle_corruption_poison())
    save_particle("corruption_blood", particle_corruption_blood())
    rune_paths = []
    for i in range(2):
        name = f"corruption_rune_{i}"
        save_particle_png(name, particle_corruption_rune(i))
        rune_paths.append(f"effecoria:{name}")
    write_particle_json("corruption_rune", rune_paths)

    glyph_paths = []
    for i in range(3):
        name = f"seal_glyph_{i}"
        save_particle_png(name, particle_seal_glyph(i))
        glyph_paths.append(f"effecoria:{name}")
    write_particle_json("seal_glyph", glyph_paths)
    save_particle("seal_spark", particle_seal_spark())

    save_particle("steam_fog", particle_steam_fog())
    save_particle("ice_crystal", particle_ice_crystal())


def main():
    OUT_GUI.mkdir(parents=True, exist_ok=True)
    OUT_PARTICLE.mkdir(parents=True, exist_ok=True)
    OUT_ENTITY.mkdir(parents=True, exist_ok=True)

    for spell in SPELL_SCHOOL:
        make_icon(spell).save(OUT_GUI / f"{spell}.png")

    generate_school_particles()

    src_mega = CURSOR_ASSETS / "mega_fireball.png"
    if src_mega.exists():
        shutil.copy2(src_mega, OUT_ENTITY / "mega_fireball.png")
    else:
        make_particle("mega_fireball", (255, 160, 60), (160, 70, 200)).resize((128, 128), Image.Resampling.NEAREST).save(OUT_ENTITY / "mega_fireball.png")

    print(f"Generated {len(SPELL_SCHOOL)} spell icons and school-specific particles.")

if __name__ == "__main__":
    main()
