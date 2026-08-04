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

# Legacy rim tuples — icon art uses spell_icon_art.SCHOOL_PAL (visual-action palettes)
SCHOOLS = {
    "mental": ((80, 140, 255), (255, 240, 120), (30, 50, 120)),
    "elemental": ((255, 150, 40), (255, 230, 90), (140, 50, 15)),
    "organic": ((70, 200, 80), (160, 255, 100), (30, 90, 35)),
    "necromancy": ((230, 230, 220), (255, 255, 255), (90, 90, 85)),
    "spatial": ((240, 240, 245), (255, 255, 255), (90, 70, 140)),
    "corruption": ((50, 120, 45), (120, 200, 70), (25, 55, 22)),
    "seals": ((255, 210, 80), (255, 245, 180), (100, 70, 20)),
    "common": ((70, 220, 230), (200, 255, 255), (30, 90, 100)),
}

SCHOOL_CORE = {
    "mental": (20, 28, 55),
    "elemental": (40, 18, 10),
    "organic": (18, 40, 18),
    "necromancy": (22, 22, 24),
    "spatial": (28, 16, 48),
    "corruption": (36, 10, 12),
    "seals": (40, 30, 12),
    "common": (14, 28, 32),
}

SPELL_SCHOOL = {
    "mental_push": "mental", "mental_sting": "mental", "sense_phi": "mental",
    "mind_lance": "mental", "psychic_focus": "mental",
    "mind_bolt": "mental", "psychic_scream": "mental", "thought_lance": "mental",
    "neural_lock": "mental", "telekinetic_crush": "mental", "mass_confusion": "mental",
    "psychic_barrier": "mental", "mind_probe": "mental", "locus_echo": "mental", "synaptic_overload": "mental",
    "psychic_drain": "mental", "mental_fortress": "mental", "thought_bomb": "mental",
    "psychic_storm": "mental", "psychic_amplify": "mental", "omega_mind": "mental",
    "mind_terror": "mental", "cliff_urge": "mental", "drown_urge": "mental", "psychic_frenzy": "mental", "mass_hysteria": "mental",
    "fire_burst": "elemental", "sear": "elemental", "ore_smelt": "elemental", "wind_push": "elemental", "water_stream": "elemental",
    "psi_adrenaline": "common", "phi_glow": "common", "psi_charge": "common", "psi_link": "common", "psi_ward": "common",
    "steam_jet": "elemental", "steam_veil": "elemental", "ember_volley": "elemental",
    "ice_shard": "elemental", "frost_bastion": "elemental", "plasma_bolt": "elemental",
    "hydro_slice": "elemental", "great_fireball": "elemental", "steam_flight": "elemental",
    "air_hand": "elemental", "water_prison": "elemental", "vacuum_cage": "elemental",
    "water_shield": "elemental", "shockwave": "elemental", "ice_sheet": "elemental",
    "breath_bubble": "elemental", "sonic_lance": "elemental", "ice_prison": "elemental",
    "air_ionization": "elemental", "mirage": "elemental", "tornado": "elemental", "ion_storm": "elemental",
    "weak_breeze": "elemental", "hyper_cooling": "elemental",
    "lightning_spear": "elemental", "water_shroud": "elemental", "air_shroud": "elemental",
    "atmospheric_pressure": "elemental", "cryo_wave": "elemental", "air_form": "elemental",
    "hurricane_storm": "elemental", "elemental_supremacy": "elemental", "thermonuclear_pulse": "elemental",
    "absolute_zero": "elemental", "meteorological_cataclysm": "elemental", "quasar": "elemental",
    "plasma_barrage": "elemental",
    "vitality_pulse": "organic", "thorn_lash": "organic", "root_bind": "organic",
    "briar_surge": "organic", "verdant_mend": "organic",
    "diagnostic_glimpse": "organic", "blood_stasis": "organic", "life_sense": "organic",
    "bio_strike": "organic", "bone_needle": "organic", "foreign_agent": "organic",
    "muscle_spasm": "organic", "chitin_plates": "organic", "acid_gland": "organic",
    "parasitic_infection": "organic", "metabolic_shock": "organic", "biological_field": "organic",
    "bone_spur": "organic", "sense_sharpening": "organic", "pain_inhibitor": "organic",
    "poison_thorns": "organic", "bio_mimicry": "organic", "organism_adaptation": "organic",
    "immune_suppression": "organic", "metabolic_boost": "organic", "organic_necrosis": "organic",
    "full_restructuring": "organic", "scorched_earth": "organic", "bio_fission": "organic",
    "super_regeneration": "organic", "population_control": "organic", "biological_plague": "organic",
    "living_armor": "organic", "beast_form": "organic", "bio_cataclysm": "organic",
    "absolute_regeneration": "organic", "cellular_dominion": "organic", "evolutionary_leap": "organic",
    "symbiotic_graft": "organic", "limb_regeneration": "organic", "verdant_bloom": "organic",
    "genetic_lock": "organic", "biological_cleaving": "organic", "full_transformation": "organic",
    "spore_storm": "organic", "biological_singularity": "organic", "life_creation": "organic",
    "biological_immortality": "organic",
    "vital_infusion": "organic", "soothing_sap": "organic", "vital_ward": "organic", "adrenal_gift": "organic",
    "soul_drain": "necromancy", "wither_touch": "necromancy", "shade_summon": "necromancy",
    "grave_leech": "necromancy", "shade_swarm": "necromancy",
    "bone_chill": "necromancy", "death_sense": "necromancy", "grave_whisper": "necromancy",
    "siphon_pulse": "necromancy", "bone_armor": "necromancy", "life_tap": "necromancy",
    "wither_wave": "necromancy", "dark_pact": "necromancy", "soul_shackle": "necromancy",
    "phantom_step": "necromancy", "grave_field": "necromancy", "raise_skeleton": "necromancy",
    "shade_brood": "necromancy", "lich_ward": "necromancy", "death_coil": "necromancy",
    "soul_cataclysm": "necromancy", "death_apotheosis": "necromancy",
    "necrotic_bolt": "necromancy", "grave_bind": "necromancy", "curse_of_frailty": "necromancy",
    "haunting_visage": "necromancy", "corpse_burst": "necromancy", "raise_zombie": "necromancy",
    "bone_volley": "necromancy", "necrotic_aura": "necromancy", "soul_anchor": "necromancy",
    "army_of_dead": "necromancy", "death_gate": "necromancy", "soul_reaper": "necromancy",
    "phylactery_surge": "necromancy", "lich_ascension": "necromancy",
    "death_mark": "necromancy", "death_shadow": "necromancy",
    "blink": "spatial", "rift_yank": "spatial", "phase_veil": "spatial",
    "void_step": "spatial", "gravity_well": "spatial",
    "warp_bolt": "spatial", "spatial_ward": "spatial", "fold_repulse": "spatial",
    "rift_slash": "spatial", "gravity_snare": "spatial", "dimensional_anchor": "spatial",
    "far_blink": "spatial", "warp_exchange": "spatial", "spatial_surge": "spatial",
    "void_lance": "spatial", "rift_burst": "spatial", "spatial_singularity": "spatial",
    "absolute_fold": "spatial",
    "corrupt_mark": "corruption", "binding_seal": "corruption", "blight_pulse": "corruption",
    "blight_brand": "corruption", "pestilence_wave": "corruption",
    "rot_touch": "corruption", "entropy_lash": "corruption", "plague_bolt": "corruption",
    "festering_wound": "corruption", "miasma_cloak": "corruption", "blight_surge": "corruption",
    "decay_bind": "corruption", "blight_field": "corruption", "entropy_aegis": "corruption",
    "tainted_leech": "corruption", "virulent_wave": "corruption", "plague_crown": "corruption",
    "omega_blight": "corruption",
    "trap_seal": "seals", "fortify_seal": "seals", "glow_seal": "seals",
    "snare_glyph": "seals", "beacon_seal": "seals",
    "shock_glyph": "seals", "ward_glyph": "seals", "repulsion_seal": "seals",
    "anchor_fortify": "seals", "permanent_glow": "seals", "snare_matrix": "seals",
    "shock_trap": "seals", "omega_ward": "seals",
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
    """Circular LoL-style plate: school glow rim, beveled frame, tinted core."""
    accent, highlight, rim = SCHOOLS[school]
    core = SCHOOL_CORE[school]
    img = Image.new("RGBA", (ICON, ICON), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = ICON // 2, ICON // 2

    # Soft outer halo
    draw_disc(d, cx, cy, 31, with_alpha(accent, 90))
    # Bright school rim
    draw_disc(d, cx, cy, 29, accent + (255,))
    # Deep metal under-rim
    draw_disc(d, cx, cy, 26, rim + (255,))
    # Inner well
    draw_disc(d, cx, cy, 23, (10, 10, 14, 255))
    draw_disc(d, cx, cy, 21, core + (255,))
    # Bevel highlights (top-left light / bottom-right dark)
    d.arc((cx - 27, cy - 27, cx + 27, cy + 27), 200, 340, fill=highlight + (230,), width=2)
    d.arc((cx - 27, cy - 27, cx + 27, cy + 27), 20, 160, fill=with_alpha(rim, 220), width=2)
    # Common arts: extra crimson inner ring on steel rim
    if school == "common":
        d.ellipse((cx - 24, cy - 24, cx + 24, cy + 24), outline=highlight + (255,), width=2)
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


# --- Mental association icons (hardcoded family colors) ---

def icon_mental_force(d, cx, cy, color):
    ring = (170, 140, 255, 230)
    core = (220, 200, 255, 255)
    for r in (14, 9, 5):
        d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=ring, width=2)
    draw_disc(d, cx, cy, 3, core)
    d.line([(cx - 12, cy), (cx - 16, cy - 4)], fill=ring, width=2)
    d.line([(cx + 12, cy), (cx + 16, cy + 4)], fill=ring, width=2)


def icon_mental_sting(d, cx, cy, color):
    shard = (190, 160, 255, 255)
    tip = (240, 230, 255, 255)
    d.polygon([(cx + 12, cy - 12), (cx - 2, cy - 2), (cx + 2, cy + 2), (cx - 10, cy + 12), (cx - 4, cy + 2), (cx + 2, cy - 4)], fill=shard)
    d.line([(cx - 6, cy + 6), (cx + 8, cy - 8)], fill=tip, width=1)


def icon_mental_eye(d, cx, cy, color):
    iris = (120, 200, 255, 255)
    sclera = (220, 235, 255, 255)
    pupil = (30, 20, 50, 255)
    d.ellipse((cx - 14, cy - 8, cx + 14, cy + 8), fill=sclera)
    draw_disc(d, cx, cy, 6, iris)
    draw_disc(d, cx, cy, 3, pupil)
    draw_disc(d, cx + 2, cy - 2, 1, (255, 255, 255, 230))


def icon_mental_lance(d, cx, cy, color):
    shaft = (160, 130, 255, 255)
    tip = (230, 220, 255, 255)
    d.line([(cx - 14, cy + 12), (cx + 12, cy - 12)], fill=shaft, width=3)
    d.polygon([(cx + 12, cy - 12), (cx + 4, cy - 10), (cx + 10, cy - 4)], fill=tip)
    draw_disc(d, cx - 2, cy + 2, 2, (200, 180, 255, 220))


def icon_mental_focus(d, cx, cy, color):
    ring = (150, 180, 255, 255)
    core = (220, 200, 255, 255)
    draw_disc(d, cx, cy + 2, 6, core)
    d.ellipse((cx - 12, cy - 14, cx + 12, cy - 2), outline=ring, width=2)
    d.line([(cx, cy - 2), (cx, cy + 8)], fill=ring, width=2)


def icon_mental_bolt(d, cx, cy, color):
    bolt = (180, 150, 255, 255)
    d.polygon([(cx - 2, cy - 14), (cx + 6, cy - 2), (cx + 1, cy - 1), (cx + 8, cy + 14), (cx - 2, cy + 2), (cx + 2, cy + 0)], fill=bolt)
    draw_disc(d, cx, cy, 2, (255, 255, 255, 220))


def icon_psychic_scream(d, cx, cy, color):
    ring = (190, 160, 255, 220)
    for r in (6, 10, 14):
        d.ellipse((cx - r, cy - r // 2 - 2, cx + r, cy + r // 2 + 2), outline=ring, width=2)
    draw_disc(d, cx, cy, 3, (240, 230, 255, 255))


def icon_neural_lock(d, cx, cy, color):
    glow = (80, 220, 255, 255)
    lock = (160, 140, 220, 255)
    d.line([(cx, cy - 12), (cx, cy + 12)], fill=glow, width=2)
    d.line([(cx - 12, cy), (cx + 12, cy)], fill=glow, width=2)
    d.rectangle((cx - 5, cy - 2, cx + 5, cy + 6), fill=lock)
    d.arc((cx - 5, cy - 8, cx + 5, cy + 2), 180, 0, fill=lock, width=2)


def icon_telekinetic_crush(d, cx, cy, color):
    fist = (170, 140, 255, 255)
    draw_disc(d, cx, cy, 8, fist)
    d.ellipse((cx - 14, cy - 14, cx + 14, cy + 14), outline=(200, 180, 255, 180), width=2)
    for ang in (45, 135, 225, 315):
        rad = math.radians(ang)
        x2 = cx + int(round(math.cos(rad) * 12))
        y2 = cy + int(round(math.sin(rad) * 12))
        d.line([(cx, cy), (x2, y2)], fill=(220, 200, 255, 200), width=2)


def icon_mass_confusion(d, cx, cy, color):
    fog = (160, 130, 230, 200)
    for ox, oy, r in ((-6, -4, 6), (6, 2, 5), (0, 6, 4), (-2, -8, 3)):
        draw_disc(d, cx + ox, cy + oy, r, fog)
    draw_disc(d, cx, cy, 2, (230, 210, 255, 220))


def icon_psychic_barrier(d, cx, cy, color):
    ward = (150, 170, 255, 230)
    d.polygon([(cx, cy - 14), (cx + 12, cy - 4), (cx + 8, cy + 12), (cx - 8, cy + 12), (cx - 12, cy - 4)], fill=ward)
    draw_disc(d, cx, cy, 4, (220, 230, 255, 230))
    d.line([(cx - 4, cy), (cx + 4, cy)], fill=(80, 100, 180, 200), width=1)


def icon_mind_probe(d, cx, cy, color):
    icon_mental_eye(d, cx, cy - 2, color)
    d.line([(cx, cy + 6), (cx, cy + 14)], fill=(160, 200, 255, 230), width=2)
    draw_disc(d, cx, cy + 14, 2, (200, 230, 255, 255))


def icon_locus_echo(d, cx, cy, color):
    # Memory reading: open mind + swirling recall fragments
    head = (150, 120, 220, 255)
    mist = (200, 170, 255, 200)
    page = (220, 230, 255, 240)
    ink = (80, 60, 140, 255)
    # head / skull silhouette
    draw_disc(d, cx - 2, cy - 2, 9, head)
    d.polygon([(cx - 8, cy + 4), (cx + 4, cy + 4), (cx + 2, cy + 12), (cx - 6, cy + 12)], fill=head)
    # memory page / fragment floating out
    d.polygon([(cx + 2, cy - 10), (cx + 14, cy - 12), (cx + 13, cy + 4), (cx + 1, cy + 6)], fill=page)
    d.line([(cx + 5, cy - 6), (cx + 11, cy - 7)], fill=ink, width=1)
    d.line([(cx + 4, cy - 2), (cx + 10, cy - 3)], fill=ink, width=1)
    d.line([(cx + 4, cy + 2), (cx + 9, cy + 1)], fill=ink, width=1)
    # recall wisps
    draw_disc(d, cx - 10, cy - 8, 2, mist)
    draw_disc(d, cx + 8, cy + 8, 2, mist)
    draw_disc(d, cx - 4, cy + 10, 1, (230, 210, 255, 220))


def icon_synaptic_overload(d, cx, cy, color):
    glow = (90, 230, 255, 255)
    for i in range(5):
        ang = math.radians(i * 72)
        x2 = cx + int(round(math.cos(ang) * 12))
        y2 = cy + int(round(math.sin(ang) * 12))
        d.line([(cx, cy), (x2, y2)], fill=glow, width=2)
        draw_disc(d, x2, y2, 2, (220, 255, 255, 255))
    draw_disc(d, cx, cy, 3, (255, 255, 255, 255))


def icon_psychic_drain(d, cx, cy, color):
    siphon = (140, 80, 200, 255)
    d.arc((cx - 12, cy - 10, cx + 4, cy + 10), 200, 40, fill=siphon, width=3)
    d.arc((cx - 4, cy - 8, cx + 12, cy + 8), 20, 200, fill=(180, 120, 230, 230), width=3)
    draw_disc(d, cx + 8, cy - 4, 3, (230, 200, 255, 255))
    draw_disc(d, cx - 8, cy + 4, 2, (100, 50, 140, 230))


def icon_mental_fortress(d, cx, cy, color):
    stone = (140, 150, 220, 255)
    d.rectangle((cx - 10, cy - 4, cx + 10, cy + 12), fill=stone)
    d.polygon([(cx - 12, cy - 4), (cx, cy - 14), (cx + 12, cy - 4)], fill=(170, 180, 240, 255))
    draw_disc(d, cx, cy + 2, 3, (220, 230, 255, 230))


def icon_thought_bomb(d, cx, cy, color):
    core = (180, 140, 255, 255)
    draw_disc(d, cx, cy, 7, core)
    draw_disc(d, cx, cy, 3, (255, 255, 255, 240))
    for ang in range(0, 360, 45):
        rad = math.radians(ang)
        x2 = cx + int(round(math.cos(rad) * 13))
        y2 = cy + int(round(math.sin(rad) * 13))
        draw_disc(d, x2, y2, 2, (200, 170, 255, 220))


def icon_psychic_storm(d, cx, cy, color):
    fog = (150, 120, 230, 200)
    draw_disc(d, cx, cy, 10, fog)
    icon_mental_bolt(d, cx - 4, cy, color)
    icon_mental_bolt(d, cx + 5, cy + 2, color)


def icon_omega_mind(d, cx, cy, color):
    icon_mental_eye(d, cx, cy, color)
    d.ellipse((cx - 14, cy - 14, cx + 14, cy + 14), outline=(180, 160, 255, 200), width=2)
    for ang in (0, 120, 240):
        rad = math.radians(ang)
        draw_disc(d, cx + int(round(math.cos(rad) * 12)), cy + int(round(math.sin(rad) * 12)), 2, (200, 220, 255, 230))


def icon_mind_terror(d, cx, cy, color):
    fear = (120, 40, 90, 255)
    d.polygon([(cx, cy - 12), (cx + 10, cy + 2), (cx + 6, cy + 12), (cx - 6, cy + 12), (cx - 10, cy + 2)], fill=fear)
    draw_disc(d, cx - 3, cy - 2, 2, (220, 80, 100, 255))
    draw_disc(d, cx + 3, cy - 2, 2, (220, 80, 100, 255))
    d.arc((cx - 5, cy + 4, cx + 5, cy + 12), 200, 340, fill=(40, 10, 30, 255), width=2)


def icon_cliff_urge(d, cx, cy, color):
    fear = (140, 60, 110, 255)
    d.polygon([(cx - 12, cy + 10), (cx - 4, cy - 12), (cx + 4, cy - 4), (cx + 12, cy + 10)], fill=fear)
    d.line([(cx - 8, cy + 4), (cx + 8, cy + 4)], fill=(220, 180, 200, 200), width=1)
    draw_disc(d, cx, cy - 2, 2, (255, 200, 220, 230))


def icon_drown_urge(d, cx, cy, color):
    fear = (80, 50, 140, 255)
    d.ellipse((cx - 10, cy - 8, cx + 10, cy + 10), fill=fear)
    d.arc((cx - 12, cy + 4, cx + 12, cy + 14), 200, 340, fill=(140, 180, 255, 200), width=2)
    draw_disc(d, cx - 3, cy - 2, 2, (200, 160, 255, 220))


def icon_psychic_frenzy(d, cx, cy, color):
    rage = (200, 60, 100, 255)
    d.polygon([(cx - 10, cy + 8), (cx - 4, cy - 12), (cx + 2, cy - 2), (cx + 10, cy + 10)], fill=rage)
    d.polygon([(cx + 8, cy - 8), (cx + 2, cy + 10), (cx - 2, cy + 2)], fill=(240, 120, 140, 230))
    draw_disc(d, cx, cy, 2, (255, 220, 230, 230))


def icon_mass_hysteria(d, cx, cy, color):
    icon_mass_confusion(d, cx, cy, color)
    icon_mind_terror(d, cx + 4, cy - 4, color)


def icon_psychic_amplify(d, cx, cy, color):
    icon_mental_focus(d, cx, cy, color)
    d.ellipse((cx - 14, cy - 14, cx + 14, cy + 14), outline=(160, 200, 255, 180), width=1)



def icon_fire(d, cx, cy, color):
    flame = (255, 120, 40, 255)
    core = (255, 220, 100, 255)
    d.polygon([(cx, cy - 16), (cx + 11, cy + 2), (cx + 4, cy + 14), (cx - 4, cy + 14), (cx - 11, cy + 2)], fill=flame)
    d.polygon([(cx, cy - 8), (cx + 5, cy + 4), (cx, cy + 10), (cx - 5, cy + 4)], fill=core)
    draw_disc(d, cx - 8, cy + 10, 2, (255, 160, 60, 220))
    draw_disc(d, cx + 9, cy + 8, 2, (255, 160, 60, 220))


def icon_wind(d, cx, cy, color):
    gust = (190, 220, 255, 255)
    for i, (oy, span) in enumerate(((-8, 14), (-1, 16), (6, 12))):
        y = cy + oy
        d.arc((cx - span, y - 5, cx + span - 4, y + 7), 200, 340, fill=gust, width=3)
        d.line([(cx + span - 6, y), (cx + span, y - 3)], fill=gust, width=2)


def icon_water(d, cx, cy, color):
    drop = (70, 160, 255, 255)
    highlight = (200, 235, 255, 220)
    d.polygon([(cx, cy - 14), (cx + 9, cy - 2), (cx + 8, cy + 6), (cx, cy + 12), (cx - 8, cy + 6), (cx - 9, cy - 2)], fill=drop)
    draw_disc(d, cx - 3, cy - 2, 2, highlight)


def icon_steam(d, cx, cy, color):
    steam = (210, 225, 235, 230)
    for ox, oy, r in ((-8, 2, 5), (0, -6, 6), (8, 0, 5), (-2, 8, 4)):
        draw_disc(d, cx + ox, cy + oy, r, steam)
    draw_disc(d, cx, cy - 2, 2, (255, 255, 255, 180))


def icon_embers(d, cx, cy, color):
    for ox, oy, r, c in (
        (-9, 6, 3, (255, 90, 30, 255)),
        (0, -8, 4, (255, 160, 40, 255)),
        (9, 4, 3, (255, 110, 30, 255)),
        (-4, -1, 2, (255, 220, 100, 240)),
        (5, 10, 2, (255, 180, 60, 240)),
    ):
        draw_disc(d, cx + ox, cy + oy, r, c)


def icon_ice(d, cx, cy, color):
    crystal = (160, 220, 255, 255)
    edge = (230, 250, 255, 255)
    d.polygon([(cx, cy - 15), (cx + 9, cy - 2), (cx + 5, cy + 12), (cx - 5, cy + 12), (cx - 9, cy - 2)], fill=crystal)
    d.line([(cx, cy - 12), (cx, cy + 10)], fill=edge, width=2)
    d.line([(cx - 7, cy), (cx + 7, cy)], fill=edge, width=1)
    d.line([(cx - 5, cy - 6), (cx + 5, cy + 6)], fill=edge, width=1)


def icon_plasma(d, cx, cy, color):
    # Distinct from fire: violet core + cyan rim, not a purple flame
    rim = (120, 220, 255, 255)
    body = (180, 90, 255, 255)
    core = (255, 255, 255, 255)
    draw_disc(d, cx, cy, 11, rim)
    draw_disc(d, cx, cy, 8, body)
    draw_disc(d, cx, cy, 3, core)
    for angle in (20, 110, 200, 290):
        rad = math.radians(angle)
        x2 = cx + int(round(math.cos(rad) * 14))
        y2 = cy + int(round(math.sin(rad) * 14))
        d.line([(cx, cy), (x2, y2)], fill=rim, width=2)


def icon_lightning(d, cx, cy, color):
    # Classic zigzag bolt — readable at 32px
    bolt = (210, 235, 255, 255)
    core = (255, 255, 255, 240)
    d.polygon([
        (cx + 2, cy - 15), (cx + 8, cy - 2), (cx + 3, cy - 2),
        (cx + 7, cy + 15), (cx - 2, cy + 1), (cx + 2, cy + 1),
        (cx - 6, cy - 15),
    ], fill=bolt)
    d.line([(cx + 1, cy - 12), (cx + 4, cy - 2), (cx + 0, cy + 0), (cx + 3, cy + 12)], fill=core, width=1)


def icon_vacuum(d, cx, cy, color):
    void = (25, 15, 40, 255)
    rim = (90, 70, 140, 255)
    draw_disc(d, cx, cy, 12, rim)
    draw_disc(d, cx, cy, 8, void)
    draw_disc(d, cx, cy, 3, (10, 5, 20, 255))
    for angle in range(0, 360, 60):
        rad = math.radians(angle)
        x1 = cx + int(round(math.cos(rad) * 14))
        y1 = cy + int(round(math.sin(rad) * 14))
        x2 = cx + int(round(math.cos(rad) * 9))
        y2 = cy + int(round(math.sin(rad) * 9))
        d.line([(x1, y1), (x2, y2)], fill=rim, width=2)


def icon_sonic(d, cx, cy, color):
    ring = (180, 210, 255, 230)
    for r in (6, 10, 14):
        d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=ring, width=2)
    draw_disc(d, cx, cy, 3, (240, 250, 255, 255))


def icon_hydro_slice(d, cx, cy, color):
    # Crescent water blade + spray — cut, not drop+bar
    blade = (90, 180, 255, 255)
    edge = (220, 245, 255, 255)
    spray = (140, 210, 255, 230)
    d.pieslice((cx - 14, cy - 14, cx + 14, cy + 14), 200, 20, fill=blade)
    d.pieslice((cx - 7, cy - 7, cx + 7, cy + 7), 200, 20, fill=(20, 30, 50, 255))
    d.arc((cx - 13, cy - 13, cx + 13, cy + 13), 205, 15, fill=edge, width=2)
    for ox, oy, r in ((10, -6, 2), (12, 2, 2), (8, 8, 1), (-10, 10, 2)):
        draw_disc(d, cx + ox, cy + oy, r, spray)


def icon_frost_wall(d, cx, cy, color):
    ice = (150, 210, 255, 255)
    for ox in (-10, -2, 6):
        d.rectangle((cx + ox, cy - 12, cx + ox + 6, cy + 12), fill=ice)
        d.line([(cx + ox + 3, cy - 10), (cx + ox + 3, cy + 10)], fill=(230, 250, 255, 200), width=1)


def icon_great_fire(d, cx, cy, color):
    icon_fire(d, cx, cy, color)
    d.ellipse((cx - 15, cy - 15, cx + 15, cy + 15), outline=(255, 180, 60, 200), width=2)
    draw_disc(d, cx + 10, cy - 8, 2, (255, 220, 100, 230))


def icon_steam_flight(d, cx, cy, color):
    icon_steam(d, cx, cy + 2, color)
    wing = (200, 230, 255, 220)
    d.arc((cx - 16, cy - 10, cx - 2, cy + 6), 200, 20, fill=wing, width=3)
    d.arc((cx + 2, cy - 10, cx + 16, cy + 6), 160, 340, fill=wing, width=3)


def icon_air_hand(d, cx, cy, color):
    palm = (200, 225, 255, 240)
    draw_disc(d, cx, cy + 2, 7, palm)
    for ox in (-8, -3, 3, 8):
        d.line([(cx + ox // 2, cy - 2), (cx + ox, cy - 14)], fill=palm, width=3)
    icon_wind(d, cx, cy + 8, color)


def icon_water_prison(d, cx, cy, color):
    cage = (60, 140, 230, 230)
    d.ellipse((cx - 13, cy - 14, cx + 13, cy + 14), outline=cage, width=3)
    icon_water(d, cx, cy, color)


def icon_vacuum_cage(d, cx, cy, color):
    icon_vacuum(d, cx, cy, color)
    d.ellipse((cx - 15, cy - 15, cx + 15, cy + 15), outline=(110, 90, 160, 180), width=1)


def icon_water_shield(d, cx, cy, color):
    shield = (80, 170, 255, 230)
    d.polygon([(cx, cy - 14), (cx + 12, cy - 4), (cx + 8, cy + 12), (cx - 8, cy + 12), (cx - 12, cy - 4)], fill=shield)
    draw_disc(d, cx, cy, 4, (200, 235, 255, 220))


def icon_shockwave(d, cx, cy, color):
    icon_sonic(d, cx, cy, color)
    gust = (170, 200, 240, 200)
    for ang in (0, 90, 180, 270):
        rad = math.radians(ang)
        x2 = cx + int(round(math.cos(rad) * 15))
        y2 = cy + int(round(math.sin(rad) * 15))
        d.line([(cx, cy), (x2, y2)], fill=gust, width=2)


def icon_ice_sheet(d, cx, cy, color):
    sheet = (170, 225, 255, 240)
    d.polygon([(cx - 14, cy + 4), (cx - 6, cy - 10), (cx + 10, cy - 6), (cx + 14, cy + 8), (cx - 8, cy + 12)], fill=sheet)
    d.line([(cx - 8, cy), (cx + 8, cy + 2)], fill=(230, 250, 255, 220), width=1)
    icon_ice(d, cx, cy - 2, color)


def icon_breath_bubble(d, cx, cy, color):
    bubble = (150, 220, 255, 200)
    d.ellipse((cx - 12, cy - 12, cx + 12, cy + 12), outline=bubble, width=3)
    draw_disc(d, cx - 3, cy - 4, 3, (230, 250, 255, 200))
    icon_water(d, cx + 2, cy + 2, color)


def icon_sonic_lance(d, cx, cy, color):
    icon_lance(d, cx, cy, (200, 230, 255, 255))
    icon_sonic(d, cx - 4, cy + 4, color)


def icon_ice_prison(d, cx, cy, color):
    bars = (150, 210, 255, 255)
    for ox in (-10, -2, 6):
        d.rectangle((cx + ox, cy - 12, cx + ox + 5, cy + 12), fill=bars)
    icon_ice(d, cx, cy, color)


def icon_air_ionization(d, cx, cy, color):
    icon_lightning(d, cx, cy, color)
    d.ellipse((cx - 13, cy - 13, cx + 13, cy + 13), outline=(160, 220, 255, 180), width=2)


def icon_mirage(d, cx, cy, color):
    haze = (255, 200, 120, 180)
    for oy in (-8, 0, 8):
        d.arc((cx - 12, cy + oy - 4, cx + 12, cy + oy + 6), 200, 340, fill=haze, width=2)
    icon_steam(d, cx, cy - 2, (230, 230, 240, 200))


def icon_tornado(d, cx, cy, color):
    funnel = (180, 210, 240, 255)
    edge = (220, 240, 255, 230)
    d.polygon([
        (cx - 12, cy - 12), (cx + 12, cy - 12),
        (cx + 4, cy + 12), (cx - 4, cy + 12),
    ], fill=funnel)
    d.line([(cx - 6, cy - 4), (cx + 8, cy - 4)], fill=edge, width=1)
    d.line([(cx - 4, cy + 2), (cx + 5, cy + 2)], fill=edge, width=1)
    d.line([(cx - 2, cy + 8), (cx + 3, cy + 8)], fill=edge, width=1)
    draw_disc(d, cx, cy - 14, 2, edge)


def icon_ion_storm(d, cx, cy, color):
    icon_lightning(d, cx - 4, cy, color)
    icon_lightning(d, cx + 6, cy + 2, color)
    draw_disc(d, cx, cy - 10, 3, (220, 240, 255, 230))


def icon_weak_breeze(d, cx, cy, color):
    soft = (200, 225, 255, 200)
    for i in range(2):
        y = cy - 2 + i * 8
        d.arc((cx - 12, y - 4, cx + 10, y + 6), 210, 330, fill=soft, width=2)


def icon_hyper_cooling(d, cx, cy, color):
    icon_ice(d, cx, cy, color)
    d.ellipse((cx - 13, cy - 13, cx + 13, cy + 13), outline=(160, 210, 255, 220), width=2)
    draw_disc(d, cx, cy, 3, (230, 245, 255, 230))


def icon_lightning_spear(d, cx, cy, color):
    tip = (230, 245, 255, 255)
    d.polygon([(cx - 3, cy + 4), (cx + 3, cy + 4), (cx, cy + 14)], fill=tip)
    icon_lightning(d, cx, cy - 2, color)


def icon_water_shroud(d, cx, cy, color):
    d.ellipse((cx - 13, cy - 14, cx + 13, cy + 12), outline=(100, 180, 255, 200), width=2)
    icon_water(d, cx, cy, color)


def icon_air_shroud(d, cx, cy, color):
    d.ellipse((cx - 13, cy - 14, cx + 13, cy + 12), outline=(200, 220, 255, 200), width=2)
    icon_wind(d, cx, cy, color)


def icon_atmospheric_pressure(d, cx, cy, color):
    icon_shockwave(d, cx, cy, color)
    draw_disc(d, cx, cy, 5, (70, 80, 100, 230))


def icon_cryo_wave(d, cx, cy, color):
    wave = (150, 220, 255, 230)
    d.polygon([(cx - 14, cy + 10), (cx - 4, cy - 8), (cx + 6, cy + 2), (cx + 14, cy - 6), (cx + 14, cy + 12)], fill=wave)
    icon_ice(d, cx - 2, cy, color)


def icon_air_form(d, cx, cy, color):
    icon_wind(d, cx, cy, color)
    d.ellipse((cx - 10, cy - 12, cx + 10, cy + 8), outline=(255, 255, 255, 160), width=1)
    draw_disc(d, cx, cy - 2, 4, (220, 235, 255, 180))


def icon_hurricane_storm(d, cx, cy, color):
    icon_tornado(d, cx, cy, color)
    d.ellipse((cx - 15, cy - 15, cx + 15, cy + 15), outline=(160, 190, 230, 180), width=2)
    icon_lightning(d, cx + 8, cy - 8, color)


def icon_elemental_supremacy(d, cx, cy, color):
    icon_fire(d, cx - 7, cy - 2, color)
    icon_ice(d, cx + 7, cy + 2, color)
    icon_wind(d, cx, cy - 8, color)


def icon_thermonuclear_pulse(d, cx, cy, color):
    icon_plasma(d, cx, cy, color)
    icon_embers(d, cx, cy, color)
    d.ellipse((cx - 15, cy - 15, cx + 15, cy + 15), outline=(255, 220, 120, 220), width=2)


def icon_absolute_zero(d, cx, cy, color):
    icon_ice_prison(d, cx, cy, color)
    draw_disc(d, cx, cy, 4, (240, 250, 255, 240))
    d.ellipse((cx - 14, cy - 14, cx + 14, cy + 14), outline=(180, 230, 255, 200), width=1)


def icon_meteorological_cataclysm(d, cx, cy, color):
    icon_hurricane_storm(d, cx, cy, color)
    icon_embers(d, cx - 6, cy + 6, color)


def icon_quasar(d, cx, cy, color):
    draw_disc(d, cx, cy, 4, (255, 240, 220, 255))
    d.ellipse((cx - 9, cy - 9, cx + 9, cy + 9), outline=(200, 140, 255, 230), width=2)
    d.ellipse((cx - 14, cy - 14, cx + 14, cy + 14), outline=(120, 80, 200, 180), width=2)
    for angle in (0, 72, 144, 216, 288):
        rad = math.radians(angle)
        x2 = cx + int(round(math.cos(rad) * 15))
        y2 = cy + int(round(math.sin(rad) * 15))
        draw_disc(d, x2, y2, 2, (180, 160, 255, 200))


def icon_plasma_barrage(d, cx, cy, color):
    for ox, oy in ((-8, -5), (0, 0), (8, 5), (-4, 7), (5, -8)):
        draw_disc(d, cx + ox, cy + oy, 3, (180, 100, 255, 240))
        draw_disc(d, cx + ox, cy + oy, 1, (255, 255, 255, 230))


def icon_steam_veil(d, cx, cy, color):
    icon_steam(d, cx, cy, color)
    d.ellipse((cx - 14, cy - 16, cx + 14, cy + 6), outline=(200, 220, 235, 180), width=2)


def icon_heart(d, cx, cy, color):
    d.polygon([(cx, cy + 10), (cx - 12, cy - 2), (cx, cy - 10), (cx + 12, cy - 2)], fill=color)


def icon_thorns(d, cx, cy, color):
    for ang in (-50, -15, 20, 55):
        rad = math.radians(ang - 90)
        x2 = cx + int(math.cos(rad) * 16)
        y2 = cy + int(math.sin(rad) * 16)
        d.line([(cx, cy + 6), (x2, y2)], fill=color, width=3)
        # barb
        d.line([(x2, y2), (x2 - 3, y2 + 4)], fill=with_alpha(color, 200), width=2)


def icon_roots(d, cx, cy, color):
    # Trunk stub
    d.line([(cx, cy - 6), (cx, cy + 2)], fill=color, width=3)
    # Branching downward tendrils (reads as roots, not a jaw/claw)
    paths = [
        [(cx, cy + 2), (cx - 4, cy + 8), (cx - 11, cy + 14)],
        [(cx, cy + 2), (cx + 1, cy + 10), (cx + 2, cy + 16)],
        [(cx, cy + 2), (cx + 5, cy + 8), (cx + 12, cy + 13)],
        [(cx - 2, cy + 6), (cx - 8, cy + 11)],
        [(cx + 3, cy + 7), (cx + 9, cy + 12)],
    ]
    for pts in paths:
        d.line(pts, fill=color, width=2)
    soil = with_alpha((90, 70, 40), 220)
    d.ellipse((cx - 10, cy + 12, cx + 10, cy + 18), fill=soil)


def icon_briar(d, cx, cy, color):
    icon_roots(d, cx, cy, color)
    tip = with_alpha((180, 255, 120), 255)
    for ox, oy in ((-8, 4), (9, 3), (0, -2)):
        d.line([(cx + ox, cy + oy), (cx + ox - 2, cy + oy - 6)], fill=tip, width=2)


def icon_thorn_lash(d, cx, cy, color):
    # Ground line of erupting spikes
    d.line([(cx - 14, cy + 10), (cx + 14, cy + 10)], fill=with_alpha((70, 50, 30), 220), width=2)
    for i, h in enumerate((8, 14, 11, 16, 9)):
        x = cx - 10 + i * 5
        d.line([(x, cy + 10), (x, cy + 10 - h)], fill=color, width=2)
        d.line([(x, cy + 10 - h), (x + 3, cy + 10 - h + 4)], fill=with_alpha(color, 200), width=1)


def icon_leaf(d, cx, cy, color):
    d.polygon([(cx, cy - 14), (cx + 11, cy - 2), (cx + 4, cy + 12), (cx - 4, cy + 12), (cx - 11, cy - 2)], fill=color)
    d.line([(cx, cy - 10), (cx, cy + 10)], fill=with_alpha((30, 90, 40), 220), width=1)


def icon_sap(d, cx, cy, color):
    amber = (220, 160, 40, 255)
    d.ellipse((cx - 6, cy - 2, cx + 6, cy + 12), fill=amber)
    d.ellipse((cx - 3, cy - 10, cx + 3, cy - 2), fill=amber)
    draw_disc(d, cx - 2, cy + 2, 2, (255, 220, 120, 200))


def icon_cells(d, cx, cy, color):
    # Red cell + pale stabilizer cell
    draw_disc(d, cx - 5, cy, 7, (200, 50, 65, 255))
    draw_disc(d, cx - 5, cy, 3, (140, 30, 45, 255))
    draw_disc(d, cx + 6, cy - 2, 6, (230, 235, 245, 240))
    draw_disc(d, cx + 5, cy - 2, 3, (90, 110, 180, 255))


def icon_virus(d, cx, cy, color):
    core = (170, 70, 200, 255)
    spike = (220, 150, 240, 255)
    draw_disc(d, cx, cy, 8, core)
    draw_disc(d, cx, cy, 3, (240, 210, 255, 255))
    for angle in range(0, 360, 45):
        rad = math.radians(angle)
        x2 = cx + int(round(math.cos(rad) * 14))
        y2 = cy + int(round(math.sin(rad) * 14))
        d.line([(cx, cy), (x2, y2)], fill=spike, width=2)
        draw_disc(d, x2, y2, 2, spike)


def icon_parasite(d, cx, cy, color):
    body = (190, 160, 50, 255)
    d.arc((cx - 14, cy - 10, cx + 6, cy + 12), 200, 40, fill=body, width=4)
    d.arc((cx - 4, cy - 8, cx + 14, cy + 10), 20, 200, fill=body, width=4)
    draw_disc(d, cx + 10, cy - 2, 3, (230, 200, 80, 255))


def icon_bone(d, cx, cy, color):
    bone = (235, 225, 200, 255)
    d.rectangle((cx - 3, cy - 12, cx + 3, cy + 12), fill=bone)
    draw_disc(d, cx - 5, cy - 12, 4, bone)
    draw_disc(d, cx + 5, cy - 12, 4, bone)
    draw_disc(d, cx - 5, cy + 12, 4, bone)
    draw_disc(d, cx + 5, cy + 12, 4, bone)


def icon_chitin(d, cx, cy, color):
    shell = (110, 90, 45, 255)
    d.polygon([(cx, cy - 14), (cx + 12, cy - 2), (cx + 8, cy + 12), (cx - 8, cy + 12), (cx - 12, cy - 2)], fill=shell)
    d.line([(cx, cy - 10), (cx, cy + 10)], fill=(60, 45, 25, 255), width=2)


def icon_muscle(d, cx, cy, color):
    fiber = (180, 45, 60, 255)
    for i, oy in enumerate((-8, -2, 4, 10)):
        d.arc((cx - 14, cy + oy - 6, cx + 14, cy + oy + 6), 200, 340, fill=fiber, width=3)


def icon_nerve(d, cx, cy, color):
    glow = (80, 220, 255, 255)
    d.line([(cx, cy - 14), (cx, cy + 14)], fill=glow, width=2)
    d.line([(cx - 12, cy), (cx + 12, cy)], fill=glow, width=2)
    d.line([(cx - 10, cy - 10), (cx + 10, cy + 10)], fill=glow, width=1)
    draw_disc(d, cx, cy, 4, (230, 255, 255, 255))


def icon_dna(d, cx, cy, color):
    a = (50, 170, 220, 255)
    b = (220, 80, 130, 255)
    for i in range(-12, 13, 2):
        ox = int(round(math.sin(i * 0.45) * 8))
        draw_disc(d, cx + ox, cy + i, 2, a)
        draw_disc(d, cx - ox, cy + i, 2, b)
        if i % 4 == 0:
            d.line([(cx + ox, cy + i), (cx - ox, cy + i)], fill=(180, 200, 230, 180), width=1)


def icon_spore(d, cx, cy, color):
    for ox, oy, r in ((0, 0, 5), (-8, -4, 3), (8, -3, 3), (-5, 7, 2), (6, 6, 2), (0, -9, 2)):
        draw_disc(d, cx + ox, cy + oy, r, with_alpha(color, 200))



# --- Necromancy family accents (hardcoded; ignore school green) ---
# bone: ivory (230,220,190) + teal (80,200,160)
# soul: cyan-green (120,255,200) + void (20,40,50)
# wither: black/purple (40,20,50) + sickly (180,255,100)
# grave: moss (90,110,80) + tomb gray (140,140,130)
# shade: violet-black (60,40,80) + pale (200,190,220)
# bind: iron (160,160,170) + soul green (80,220,150)

def icon_death_sense(d, cx, cy, color):
    soul = (120, 255, 200, 255)
    void = (20, 40, 50, 255)
    draw_disc(d, cx, cy, 10, void)
    draw_disc(d, cx, cy, 10, None, outline=soul, width=2)
    draw_disc(d, cx, cy, 4, soul)
    draw_disc(d, cx + 2, cy - 2, 1, (255, 255, 255, 220))


def icon_bone_chill(d, cx, cy, color):
    ivory = (230, 220, 190, 255)
    teal = (80, 200, 160, 230)
    d.rectangle((cx - 3, cy - 10, cx + 3, cy + 8), fill=ivory)
    draw_disc(d, cx - 5, cy - 10, 4, ivory)
    draw_disc(d, cx + 5, cy - 10, 4, ivory)
    draw_disc(d, cx - 5, cy + 8, 4, ivory)
    draw_disc(d, cx + 5, cy + 8, 4, ivory)
    for ox, oy in ((-8, -4), (8, 2), (0, -14)):
        draw_disc(d, cx + ox, cy + oy, 2, teal)


def icon_necrotic_bolt(d, cx, cy, color):
    ivory = (230, 220, 190, 255)
    teal = (80, 200, 160, 255)
    d.polygon([(cx - 2, cy - 14), (cx + 6, cy - 2), (cx + 1, cy), (cx + 7, cy + 14), (cx - 2, cy + 2), (cx + 2, cy)], fill=ivory)
    draw_disc(d, cx, cy - 2, 2, teal)


def icon_soul_drain(d, cx, cy, color):
    soul = (120, 255, 200, 255)
    void = (20, 40, 50, 255)
    draw_disc(d, cx, cy - 4, 8, soul)
    d.polygon([(cx - 8, cy), (cx + 8, cy), (cx, cy + 14)], fill=soul)
    draw_disc(d, cx, cy - 2, 3, void)
    d.line([(cx + 10, cy - 8), (cx + 16, cy - 14)], fill=soul, width=2)


def icon_wither_touch(d, cx, cy, color):
    dark = (40, 20, 50, 255)
    sickly = (180, 255, 100, 255)
    d.polygon([(cx - 10, cy + 8), (cx - 4, cy - 12), (cx + 2, cy - 2), (cx + 10, cy + 10)], fill=dark)
    draw_disc(d, cx + 2, cy, 3, sickly)
    draw_disc(d, cx - 4, cy + 4, 2, (120, 180, 60, 220))


def icon_death_mark(d, cx, cy, color):
    dark = (40, 20, 50, 255)
    sickly = (180, 255, 100, 230)
    d.ellipse((cx - 11, cy - 11, cx + 11, cy + 11), outline=dark, width=3)
    d.line([(cx, cy - 9), (cx, cy + 9)], fill=sickly, width=2)
    d.line([(cx - 9, cy), (cx + 9, cy)], fill=sickly, width=2)
    draw_disc(d, cx + 8, cy - 8, 2, sickly)


def icon_grave_whisper(d, cx, cy, color):
    moss = (90, 110, 80, 255)
    tomb = (140, 140, 130, 255)
    d.rectangle((cx - 8, cy - 4, cx + 8, cy + 12), fill=tomb)
    d.ellipse((cx - 8, cy - 12, cx + 8, cy), fill=tomb)
    draw_disc(d, cx - 3, cy - 4, 2, moss)
    draw_disc(d, cx + 4, cy + 2, 2, moss)
    d.arc((cx - 14, cy - 10, cx + 6, cy + 6), 200, 40, fill=(180, 200, 160, 180), width=2)


def icon_curse_of_frailty(d, cx, cy, color):
    dark = (40, 20, 50, 255)
    sickly = (180, 255, 100, 255)
    d.line([(cx - 10, cy - 10), (cx + 10, cy + 10)], fill=dark, width=3)
    d.line([(cx + 10, cy - 10), (cx - 10, cy + 10)], fill=dark, width=3)
    draw_disc(d, cx, cy, 4, sickly)
    draw_disc(d, cx, cy, 2, dark)


def icon_siphon_pulse(d, cx, cy, color):
    soul = (120, 255, 200, 255)
    void = (20, 40, 50, 255)
    for r in (6, 11, 15):
        d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=soul, width=2)
    draw_disc(d, cx, cy, 4, void)
    draw_disc(d, cx, cy, 2, soul)


def icon_bone_armor(d, cx, cy, color):
    ivory = (230, 220, 190, 255)
    teal = (80, 200, 160, 220)
    d.polygon([(cx, cy - 14), (cx + 11, cy - 4), (cx + 8, cy + 12), (cx - 8, cy + 12), (cx - 11, cy - 4)], fill=ivory)
    d.line([(cx - 4, cy - 2), (cx - 4, cy + 8)], fill=teal, width=2)
    d.line([(cx + 4, cy - 2), (cx + 4, cy + 8)], fill=teal, width=2)
    d.line([(cx, cy - 8), (cx, cy + 6)], fill=teal, width=1)


def icon_phantom_step(d, cx, cy, color):
    shade = (60, 40, 80, 200)
    pale = (200, 190, 220, 220)
    d.ellipse((cx - 12, cy - 16, cx + 12, cy + 10), outline=pale, width=2)
    d.polygon([(cx - 6, cy - 4), (cx + 8, cy), (cx - 4, cy + 10)], fill=shade)
    draw_disc(d, cx + 6, cy - 8, 2, pale)


def icon_grave_bind(d, cx, cy, color):
    iron = (160, 160, 170, 255)
    green = (80, 220, 150, 230)
    moss = (90, 110, 80, 200)
    for oy in (-8, 0, 8):
        d.ellipse((cx - 6, cy + oy - 4, cx + 6, cy + oy + 4), outline=iron, width=2)
    d.line([(cx, cy - 10), (cx, cy + 10)], fill=green, width=2)
    draw_disc(d, cx + 8, cy - 8, 2, moss)


def icon_life_tap(d, cx, cy, color):
    soul = (120, 255, 200, 255)
    void = (20, 40, 50, 255)
    d.polygon([(cx, cy - 6), (cx + 10, cy), (cx, cy + 12), (cx - 10, cy)], fill=soul)
    draw_disc(d, cx, cy, 3, void)
    d.line([(cx - 12, cy - 10), (cx - 4, cy - 2)], fill=soul, width=2)
    d.line([(cx + 12, cy - 10), (cx + 4, cy - 2)], fill=soul, width=2)


def icon_haunting_visage(d, cx, cy, color):
    shade = (60, 40, 80, 240)
    pale = (200, 190, 220, 255)
    draw_disc(d, cx, cy - 2, 11, shade)
    draw_disc(d, cx - 4, cy - 4, 3, pale)
    draw_disc(d, cx + 4, cy - 4, 3, pale)
    draw_disc(d, cx - 4, cy - 4, 1, (20, 10, 30, 255))
    draw_disc(d, cx + 4, cy - 4, 1, (20, 10, 30, 255))
    d.arc((cx - 6, cy + 2, cx + 6, cy + 10), 20, 160, fill=pale, width=2)


def icon_soul_shackle(d, cx, cy, color):
    iron = (160, 160, 170, 255)
    green = (80, 220, 150, 255)
    draw_disc(d, cx - 6, cy, 6, None, outline=iron, width=2)
    draw_disc(d, cx + 6, cy, 6, None, outline=iron, width=2)
    draw_disc(d, cx, cy, 3, green)
    d.line([(cx - 12, cy - 8), (cx + 12, cy + 8)], fill=green, width=1)


def icon_bone_volley(d, cx, cy, color):
    ivory = (230, 220, 190, 255)
    teal = (80, 200, 160, 220)
    for i, (ox, oy) in enumerate(((-6, 4), (0, 0), (6, -4))):
        d.polygon(
            [(cx + ox + 8, cy + oy), (cx + ox - 6, cy + oy - 4), (cx + ox - 6, cy + oy + 4)],
            fill=ivory if i != 1 else teal,
        )


def icon_wither_wave(d, cx, cy, color):
    dark = (40, 20, 50, 255)
    sickly = (180, 255, 100, 230)
    d.arc((cx - 16, cy - 8, cx, cy + 8), 300, 60, fill=dark, width=3)
    d.arc((cx - 8, cy - 8, cx + 8, cy + 8), 300, 60, fill=sickly, width=2)
    d.arc((cx, cy - 8, cx + 16, cy + 8), 300, 60, fill=dark, width=2)
    draw_disc(d, cx + 4, cy - 2, 2, sickly)


def icon_necrotic_aura(d, cx, cy, color):
    ivory = (230, 220, 190, 200)
    teal = (80, 200, 160, 230)
    dark = (40, 20, 50, 180)
    for r in (6, 11, 15):
        d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=teal if r != 11 else dark, width=2)
    draw_disc(d, cx, cy, 4, ivory)


def icon_dark_pact(d, cx, cy, color):
    dark = (40, 20, 50, 255)
    sickly = (180, 255, 100, 255)
    soul = (120, 255, 200, 200)
    d.line([(cx, cy - 14), (cx, cy + 14)], fill=dark, width=3)
    d.line([(cx - 10, cy - 6), (cx + 10, cy - 6)], fill=dark, width=3)
    draw_disc(d, cx - 8, cy + 6, 3, sickly)
    draw_disc(d, cx + 8, cy + 6, 3, soul)


def icon_grave_leech(d, cx, cy, color):
    moss = (90, 110, 80, 255)
    tomb = (140, 140, 130, 255)
    d.arc((cx - 12, cy - 8, cx + 4, cy + 10), 200, 40, fill=tomb, width=4)
    d.arc((cx - 2, cy - 6, cx + 12, cy + 8), 20, 200, fill=moss, width=3)
    draw_disc(d, cx + 8, cy - 2, 3, (120, 255, 200, 220))


def icon_corpse_burst(d, cx, cy, color):
    moss = (90, 110, 80, 255)
    tomb = (140, 140, 130, 255)
    draw_disc(d, cx, cy, 6, tomb)
    for ang in range(0, 360, 45):
        rad = math.radians(ang)
        x2 = cx + int(math.cos(rad) * 13)
        y2 = cy + int(math.sin(rad) * 13)
        d.line([(cx, cy), (x2, y2)], fill=moss, width=2)
        draw_disc(d, x2, y2, 2, (180, 200, 140, 220))


def icon_grave_field(d, cx, cy, color):
    moss = (90, 110, 80, 200)
    tomb = (140, 140, 130, 255)
    for ox, oy, r in ((-6, -2, 7), (6, 2, 6), (0, -8, 5), (-2, 8, 4)):
        draw_disc(d, cx + ox, cy + oy, r, moss)
    d.rectangle((cx - 4, cy - 2, cx + 4, cy + 10), fill=tomb)
    d.ellipse((cx - 4, cy - 8, cx + 4, cy), fill=tomb)


def icon_soul_anchor(d, cx, cy, color):
    iron = (160, 160, 170, 255)
    green = (80, 220, 150, 255)
    void = (20, 40, 50, 255)
    d.ellipse((cx - 10, cy - 10, cx + 10, cy + 10), outline=iron, width=2)
    d.line([(cx, cy - 12), (cx, cy + 14)], fill=green, width=2)
    draw_disc(d, cx, cy, 4, void)
    draw_disc(d, cx, cy, 2, green)


def icon_lich_ward(d, cx, cy, color):
    ivory = (230, 220, 190, 255)
    teal = (80, 200, 160, 255)
    soul = (120, 255, 200, 200)
    d.polygon([(cx, cy - 14), (cx + 12, cy - 4), (cx + 8, cy + 12), (cx - 8, cy + 12), (cx - 12, cy - 4)], outline=ivory, width=2)
    draw_disc(d, cx, cy - 2, 5, soul)
    draw_disc(d, cx, cy - 2, 2, teal)


def icon_death_gate(d, cx, cy, color):
    shade = (60, 40, 80, 255)
    pale = (200, 190, 220, 255)
    draw_disc(d, cx, cy, 12, None, outline=pale, width=2)
    draw_disc(d, cx, cy, 8, shade)
    d.arc((cx - 6, cy - 10, cx + 6, cy + 10), 270, 90, fill=pale, width=2)
    draw_disc(d, cx - 4, cy, 2, pale)
    draw_disc(d, cx + 5, cy, 2, (120, 255, 200, 200))


def icon_soul_reaper(d, cx, cy, color):
    soul = (120, 255, 200, 255)
    void = (20, 40, 50, 255)
    d.line([(cx - 4, cy + 12), (cx + 2, cy - 14)], fill=void, width=3)
    d.polygon([(cx + 2, cy - 14), (cx + 14, cy - 6), (cx + 4, cy - 4)], fill=soul)
    d.polygon([(cx + 2, cy - 14), (cx + 12, cy - 14), (cx + 4, cy - 8)], fill=void)
    draw_disc(d, cx - 2, cy + 4, 2, soul)


def icon_death_coil(d, cx, cy, color):
    shade = (60, 40, 80, 255)
    pale = (200, 190, 220, 230)
    sickly = (180, 255, 100, 200)
    d.arc((cx - 12, cy - 12, cx + 12, cy + 12), 30, 300, fill=shade, width=3)
    d.arc((cx - 8, cy - 8, cx + 8, cy + 8), 60, 280, fill=pale, width=2)
    draw_disc(d, cx + 6, cy - 6, 3, sickly)


def icon_phylactery_surge(d, cx, cy, color):
    soul = (120, 255, 200, 255)
    void = (20, 40, 50, 255)
    ivory = (230, 220, 190, 220)
    d.rectangle((cx - 7, cy - 4, cx + 7, cy + 10), outline=ivory, width=2)
    draw_disc(d, cx, cy, 5, soul)
    draw_disc(d, cx, cy, 2, void)
    for ox, oy in ((-10, -8), (10, -6), (0, -14)):
        draw_disc(d, cx + ox, cy + oy, 2, soul)


def icon_soul_cataclysm(d, cx, cy, color):
    soul = (120, 255, 200, 255)
    void = (20, 40, 50, 255)
    draw_disc(d, cx, cy, 5, void)
    for ang in range(0, 360, 40):
        rad = math.radians(ang)
        x2 = cx + int(math.cos(rad) * 14)
        y2 = cy + int(math.sin(rad) * 14)
        d.line([(cx, cy), (x2, y2)], fill=soul, width=2)
        draw_disc(d, x2, y2, 2, soul)
    draw_disc(d, cx, cy, 3, soul)


def icon_death_apotheosis(d, cx, cy, color):
    soul = (120, 255, 200, 255)
    void = (20, 40, 50, 255)
    pale = (200, 190, 220, 200)
    draw_disc(d, cx, cy - 2, 8, soul)
    d.polygon([(cx - 8, cy), (cx + 8, cy), (cx, cy + 14)], fill=soul)
    draw_disc(d, cx, cy - 2, 3, void)
    d.ellipse((cx - 14, cy - 14, cx + 14, cy + 14), outline=pale, width=2)
    draw_disc(d, cx, cy - 14, 2, soul)


def icon_death_shadow(d, cx, cy, color):
    shade = (60, 40, 80, 240)
    pale = (200, 190, 220, 200)
    d.polygon([(cx, cy - 14), (cx + 12, cy + 12), (cx + 4, cy + 10), (cx - 4, cy + 10), (cx - 12, cy + 12)], fill=shade)
    d.polygon([(cx, cy - 8), (cx + 6, cy + 4), (cx - 6, cy + 4)], fill=pale)
    draw_disc(d, cx - 3, cy - 2, 1, (255, 255, 255, 220))
    draw_disc(d, cx + 3, cy - 2, 1, (255, 255, 255, 220))


def icon_lich_ascension(d, cx, cy, color):
    ivory = (230, 220, 190, 255)
    teal = (80, 200, 160, 255)
    soul = (120, 255, 200, 255)
    draw_disc(d, cx, cy - 2, 10, ivory)
    d.rectangle((cx - 8, cy + 4, cx + 8, cy + 12), fill=ivory)
    draw_disc(d, cx - 4, cy - 4, 2, (20, 40, 50, 255))
    draw_disc(d, cx + 4, cy - 4, 2, (20, 40, 50, 255))
    d.line([(cx - 3, cy + 2), (cx + 3, cy + 2)], fill=(20, 40, 50, 255), width=1)
    for ox, oy in ((-12, -10), (12, -8), (0, -16)):
        draw_disc(d, cx + ox, cy + oy, 2, soul if ox == 0 else teal)


# Legacy / removed-summon fallbacks (keep DRAWERS resolvable)
def icon_shade(d, cx, cy, color):
    icon_death_shadow(d, cx, cy, color)


def icon_shades(d, cx, cy, color):
    pale = (200, 190, 220, 220)
    shade = (60, 40, 80, 220)
    for ox in (-8, 0, 8):
        d.polygon([(cx + ox, cy - 10), (cx + ox + 6, cy + 8), (cx + ox - 6, cy + 8)], fill=shade if ox else pale)


def icon_skull(d, cx, cy, color):
    ivory = (230, 220, 190, 255)
    draw_disc(d, cx, cy - 2, 10, ivory)
    d.rectangle((cx - 8, cy + 4, cx + 8, cy + 12), fill=ivory)
    draw_disc(d, cx - 4, cy - 4, 2, (20, 40, 50, 255))
    draw_disc(d, cx + 4, cy - 4, 2, (20, 40, 50, 255))


def icon_soul(d, cx, cy, color):
    icon_soul_drain(d, cx, cy, color)


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


def icon_corrupt_mark(d, cx, cy, color):
    rune = (160, 60, 180, 255)
    venom = (90, 220, 60, 220)
    d.ellipse((cx - 11, cy - 11, cx + 11, cy + 11), outline=rune, width=2)
    d.line([(cx, cy - 9), (cx, cy + 9)], fill=rune, width=2)
    d.line([(cx - 9, cy), (cx + 9, cy)], fill=rune, width=2)
    draw_disc(d, cx + 8, cy - 8, 2, venom)


def icon_binding_seal(d, cx, cy, color):
    link = (140, 80, 170, 255)
    for oy in (-8, 0, 8):
        d.ellipse((cx - 6, cy + oy - 4, cx + 6, cy + oy + 4), outline=link, width=2)
    d.line([(cx, cy - 10), (cx, cy + 10)], fill=(100, 50, 130, 230), width=2)


def icon_blight_pulse(d, cx, cy, color):
    poison = (80, 200, 50, 230)
    for r in (6, 11, 15):
        d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=poison, width=2)
    draw_disc(d, cx, cy, 3, (140, 255, 90, 255))


def icon_rot_touch(d, cx, cy, color):
    rot = (110, 90, 40, 255)
    blood = (160, 30, 40, 240)
    d.polygon([(cx - 10, cy + 8), (cx - 4, cy - 12), (cx + 2, cy - 2), (cx + 10, cy + 10)], fill=rot)
    draw_disc(d, cx + 4, cy + 2, 3, blood)
    draw_disc(d, cx - 6, cy + 4, 2, (90, 140, 40, 220))


def icon_entropy_lash(d, cx, cy, color):
    ash = (180, 100, 200, 255)
    d.line([(cx - 12, cy + 10), (cx + 12, cy - 12)], fill=ash, width=3)
    for ox, oy in ((-6, 4), (0, -2), (6, -8), (10, -4)):
        draw_disc(d, cx + ox, cy + oy, 2, (120, 60, 140, 230))


def icon_plague_bolt(d, cx, cy, color):
    bolt = (100, 210, 60, 255)
    d.polygon([(cx - 2, cy - 14), (cx + 6, cy - 2), (cx + 1, cy), (cx + 7, cy + 14), (cx - 2, cy + 2), (cx + 2, cy)], fill=bolt)
    draw_disc(d, cx, cy, 2, (200, 255, 140, 240))


def icon_festering_wound(d, cx, cy, color):
    flesh = (160, 50, 60, 255)
    d.ellipse((cx - 10, cy - 8, cx + 10, cy + 10), fill=flesh)
    d.polygon([(cx - 4, cy - 2), (cx + 6, cy - 6), (cx + 2, cy + 6)], fill=(40, 15, 20, 255))
    draw_disc(d, cx - 5, cy + 4, 2, (90, 160, 40, 230))


def icon_miasma_cloak(d, cx, cy, color):
    fog = (100, 70, 130, 200)
    for ox, oy, r in ((-6, -2, 7), (6, 2, 6), (0, -8, 5), (-2, 8, 4)):
        draw_disc(d, cx + ox, cy + oy, r, fog)
    draw_disc(d, cx, cy, 2, (140, 220, 80, 200))


def icon_blight_brand(d, cx, cy, color):
    icon_corrupt_mark(d, cx, cy, color)
    d.ellipse((cx - 14, cy - 14, cx + 14, cy + 14), outline=(90, 200, 50, 180), width=1)


def icon_blight_surge(d, cx, cy, color):
    icon_blight_pulse(d, cx, cy, color)
    draw_disc(d, cx - 10, cy - 6, 2, (140, 255, 90, 220))
    draw_disc(d, cx + 10, cy + 4, 2, (140, 255, 90, 220))


def icon_decay_bind(d, cx, cy, color):
    icon_binding_seal(d, cx, cy, color)
    draw_disc(d, cx + 8, cy - 8, 2, (90, 200, 50, 230))


def icon_pestilence_wave(d, cx, cy, color):
    wave = (90, 190, 50, 230)
    d.polygon([(cx - 14, cy + 8), (cx - 4, cy - 10), (cx + 6, cy + 0), (cx + 14, cy - 8), (cx + 14, cy + 12)], fill=wave)
    draw_disc(d, cx, cy, 3, (160, 255, 100, 240))


def icon_blight_field(d, cx, cy, color):
    icon_miasma_cloak(d, cx, cy, color)
    d.ellipse((cx - 13, cy - 13, cx + 13, cy + 13), outline=(120, 80, 160, 180), width=1)


def icon_entropy_aegis(d, cx, cy, color):
    shield = (140, 70, 170, 240)
    d.polygon([(cx, cy - 14), (cx + 11, cy - 4), (cx + 8, cy + 12), (cx - 8, cy + 12), (cx - 11, cy - 4)], fill=shield)
    for ox, oy in ((-4, -2), (4, 2), (0, 6)):
        draw_disc(d, cx + ox, cy + oy, 2, (200, 120, 220, 220))


def icon_tainted_leech(d, cx, cy, color):
    body = (140, 40, 50, 255)
    d.arc((cx - 12, cy - 8, cx + 4, cy + 10), 200, 40, fill=body, width=4)
    d.arc((cx - 2, cy - 6, cx + 12, cy + 8), 20, 200, fill=(100, 180, 50, 230), width=3)
    draw_disc(d, cx + 8, cy - 2, 3, (200, 60, 70, 255))


def icon_virulent_wave(d, cx, cy, color):
    icon_pestilence_wave(d, cx, cy, color)
    draw_disc(d, cx - 8, cy - 4, 2, (180, 255, 120, 230))
    draw_disc(d, cx + 6, cy + 6, 2, (180, 255, 120, 230))


def icon_plague_crown(d, cx, cy, color):
    crown = (120, 60, 150, 255)
    d.polygon([(cx - 12, cy + 2), (cx - 8, cy - 10), (cx - 2, cy), (cx + 4, cy - 12), (cx + 10, cy - 2), (cx + 12, cy + 4)], fill=crown)
    draw_disc(d, cx, cy - 2, 3, (90, 220, 60, 240))


def icon_omega_blight(d, cx, cy, color):
    icon_blight_pulse(d, cx, cy, color)
    icon_miasma_cloak(d, cx, cy + 2, color)
    d.ellipse((cx - 14, cy - 14, cx + 14, cy + 14), outline=(160, 60, 180, 200), width=2)


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


# --- Common arts (grey-red frame; warm steel glyphs) ---

def icon_psi_adrenaline(d, cx, cy, color):
    bolt = (230, 95, 75, 255)
    ring = (255, 185, 130, 230)
    d.polygon([(cx, cy - 14), (cx + 7, cy - 1), (cx + 2, cy - 1), (cx + 9, cy + 14), (cx - 4, cy + 1), (cx + 1, cy + 1)], fill=bolt)
    d.ellipse((cx - 10, cy - 10, cx + 10, cy + 10), outline=ring, width=2)


def icon_phi_glow(d, cx, cy, color):
    for r, a in ((12, 70), (8, 130), (5, 200)):
        draw_disc(d, cx, cy, r, (200, 220, 255, a))
    draw_disc(d, cx, cy, 3, (255, 255, 255, 255))
    d.ellipse((cx - 14, cy - 14, cx + 14, cy + 14), outline=(180, 90, 90, 180), width=1)


def icon_psi_charge(d, cx, cy, color):
    core = (90, 255, 210, 230)
    spark = (255, 220, 140, 255)
    draw_disc(d, cx, cy, 7, core)
    draw_disc(d, cx, cy, 3, (240, 255, 250, 255))
    for ang in (20, 90, 160, 230, 300):
        rad = math.radians(ang)
        x2 = cx + int(math.cos(rad) * 13)
        y2 = cy + int(math.sin(rad) * 13)
        d.line([(cx, cy), (x2, y2)], fill=spark, width=2)


def icon_psi_link(d, cx, cy, color):
    left = (210, 120, 140, 255)
    right = (160, 180, 220, 255)
    draw_disc(d, cx - 8, cy, 6, left)
    draw_disc(d, cx + 8, cy, 6, right)
    d.arc((cx - 10, cy - 8, cx + 10, cy + 8), 200, 340, fill=(230, 180, 160, 230), width=2)
    draw_disc(d, cx, cy - 2, 2, (255, 230, 210, 255))


def icon_psi_ward(d, cx, cy, color):
    plate = (170, 190, 195, 230)
    rim = (200, 90, 85, 255)
    d.polygon([(cx, cy - 14), (cx + 11, cy - 4), (cx + 8, cy + 12), (cx - 8, cy + 12), (cx - 11, cy - 4)], fill=plate)
    d.polygon([(cx, cy - 14), (cx + 11, cy - 4), (cx + 8, cy + 12), (cx - 8, cy + 12), (cx - 11, cy - 4)], outline=rim)
    draw_disc(d, cx, cy, 3, (230, 240, 245, 255))


def icon_sear(d, cx, cy, color):
    pan = (150, 150, 160, 255)
    d.ellipse((cx - 12, cy + 2, cx + 12, cy + 14), fill=pan)
    d.line([(cx + 10, cy + 8), (cx + 16, cy + 4)], fill=pan, width=2)
    icon_fire(d, cx, cy - 4, color)


def icon_ore_smelt(d, cx, cy, color):
    ore = (90, 100, 120, 255)
    d.polygon([(cx - 10, cy + 2), (cx - 4, cy - 8), (cx + 6, cy - 6), (cx + 10, cy + 4), (cx, cy + 10)], fill=ore)
    draw_disc(d, cx - 2, cy, 2, (180, 190, 210, 220))
    d.polygon([(cx + 2, cy - 12), (cx + 8, cy - 2), (cx - 2, cy - 2)], fill=(255, 160, 50, 255))


DRAWERS = {
    "mental_push": icon_mental_force,
    "mental_sting": icon_mental_sting,
    "sense_phi": icon_mental_eye,
    "mind_lance": icon_mental_lance,
    "psychic_focus": icon_mental_focus,
    "mind_bolt": icon_mental_bolt,
    "psychic_scream": icon_psychic_scream,
    "thought_lance": icon_mental_lance,
    "neural_lock": icon_neural_lock,
    "telekinetic_crush": icon_telekinetic_crush,
    "mass_confusion": icon_mass_confusion,
    "psychic_barrier": icon_psychic_barrier,
    "mind_probe": icon_mind_probe,
    "locus_echo": icon_locus_echo,
    "synaptic_overload": icon_synaptic_overload,
    "psychic_drain": icon_psychic_drain,
    "mental_fortress": icon_mental_fortress,
    "thought_bomb": icon_thought_bomb,
    "psychic_storm": icon_psychic_storm,
    "mass_hysteria": icon_mass_hysteria,
    "psychic_amplify": icon_psychic_amplify,
    "omega_mind": icon_omega_mind,
    "mind_terror": icon_mind_terror,
    "cliff_urge": icon_cliff_urge,
    "drown_urge": icon_drown_urge,
    "psychic_frenzy": icon_psychic_frenzy,
    "psi_adrenaline": icon_psi_adrenaline,
    "phi_glow": icon_phi_glow,
    "psi_charge": icon_psi_charge,
    "psi_link": icon_psi_link,
    "psi_ward": icon_psi_ward,
    "fire_burst": icon_fire,
    "sear": icon_sear,
    "ore_smelt": icon_ore_smelt,
    "wind_push": icon_wind,
    "water_stream": icon_water,
    "steam_jet": icon_steam,
    "steam_veil": icon_steam_veil,
    "ember_volley": icon_embers,
    "ice_shard": icon_ice,
    "frost_bastion": icon_frost_wall,
    "plasma_bolt": icon_plasma,
    "hydro_slice": icon_hydro_slice,
    "great_fireball": icon_great_fire,
    "steam_flight": icon_steam_flight,
    "air_hand": icon_air_hand,
    "water_prison": icon_water_prison,
    "vacuum_cage": icon_vacuum_cage,
    "water_shield": icon_water_shield,
    "shockwave": icon_shockwave,
    "ice_sheet": icon_ice_sheet,
    "breath_bubble": icon_breath_bubble,
    "sonic_lance": icon_sonic_lance,
    "ice_prison": icon_ice_prison,
    "air_ionization": icon_air_ionization,
    "mirage": icon_mirage,
    "tornado": icon_tornado,
    "ion_storm": icon_ion_storm,
    "weak_breeze": icon_weak_breeze,
    "hyper_cooling": icon_hyper_cooling,
    "lightning_spear": icon_lightning_spear,
    "water_shroud": icon_water_shroud,
    "air_shroud": icon_air_shroud,
    "atmospheric_pressure": icon_atmospheric_pressure,
    "cryo_wave": icon_cryo_wave,
    "air_form": icon_air_form,
    "hurricane_storm": icon_hurricane_storm,
    "elemental_supremacy": icon_elemental_supremacy,
    "thermonuclear_pulse": icon_thermonuclear_pulse,
    "absolute_zero": icon_absolute_zero,
    "meteorological_cataclysm": icon_meteorological_cataclysm,
    "quasar": icon_quasar,
    "plasma_barrage": icon_plasma_barrage,
    "vitality_pulse": icon_cells,
    "thorn_lash": icon_thorn_lash,
    "root_bind": icon_roots,
    "briar_surge": icon_briar,
    "verdant_mend": icon_cells,
    "diagnostic_glimpse": icon_nerve,
    "blood_stasis": icon_cells,
    "life_sense": icon_nerve,
    "bio_strike": icon_muscle,
    "bone_needle": icon_bone,
    "foreign_agent": icon_virus,
    "muscle_spasm": icon_muscle,
    "chitin_plates": icon_chitin,
    "acid_gland": icon_sap,
    "parasitic_infection": icon_parasite,
    "metabolic_shock": icon_nerve,
    "biological_field": icon_cells,
    "bone_spur": icon_bone,
    "sense_sharpening": icon_nerve,
    "pain_inhibitor": icon_nerve,
    "poison_thorns": icon_thorns,
    "bio_mimicry": icon_veil,
    "organism_adaptation": icon_dna,
    "immune_suppression": icon_virus,
    "metabolic_boost": icon_nerve,
    "organic_necrosis": icon_sap,
    "full_restructuring": icon_dna,
    "scorched_earth": icon_briar,
    "bio_fission": icon_dna,
    "super_regeneration": icon_cells,
    "population_control": icon_virus,
    "biological_plague": icon_spore,
    "living_armor": icon_chitin,
    "beast_form": icon_muscle,
    "bio_cataclysm": icon_meteorological_cataclysm,
    "absolute_regeneration": icon_cells,
    "cellular_dominion": icon_dna,
    "evolutionary_leap": icon_dna,
    "symbiotic_graft": icon_cells,
    "limb_regeneration": icon_cells,
    "verdant_bloom": icon_briar,
    "genetic_lock": icon_dna,
    "vital_infusion": icon_cells,
    "soothing_sap": icon_cells,
    "vital_ward": icon_cells,
    "adrenal_gift": icon_nerve,
    "spore_storm": icon_spore,
    "biological_cleaving": icon_muscle,
    "full_transformation": icon_dna,
    "biological_singularity": icon_dna,
    "life_creation": icon_cells,
    "biological_immortality": icon_cells,
    "soul_drain": icon_soul_drain,
    "wither_touch": icon_wither_touch,
    "shade_summon": icon_shade,
    "grave_leech": icon_grave_leech,
    "shade_swarm": icon_shades,
    "bone_chill": icon_bone_chill,
    "death_sense": icon_death_sense,
    "grave_whisper": icon_grave_whisper,
    "siphon_pulse": icon_siphon_pulse,
    "bone_armor": icon_bone_armor,
    "life_tap": icon_life_tap,
    "wither_wave": icon_wither_wave,
    "dark_pact": icon_dark_pact,
    "soul_shackle": icon_soul_shackle,
    "phantom_step": icon_phantom_step,
    "grave_field": icon_grave_field,
    "raise_skeleton": icon_skull,
    "shade_brood": icon_shades,
    "lich_ward": icon_lich_ward,
    "death_coil": icon_death_coil,
    "soul_cataclysm": icon_soul_cataclysm,
    "death_apotheosis": icon_death_apotheosis,
    "necrotic_bolt": icon_necrotic_bolt,
    "grave_bind": icon_grave_bind,
    "curse_of_frailty": icon_curse_of_frailty,
    "haunting_visage": icon_haunting_visage,
    "corpse_burst": icon_corpse_burst,
    "raise_zombie": icon_skull,
    "bone_volley": icon_bone_volley,
    "necrotic_aura": icon_necrotic_aura,
    "soul_anchor": icon_soul_anchor,
    "army_of_dead": icon_shades,
    "death_gate": icon_death_gate,
    "soul_reaper": icon_soul_reaper,
    "phylactery_surge": icon_phylactery_surge,
    "lich_ascension": icon_lich_ascension,
    "death_mark": icon_death_mark,
    "death_shadow": icon_death_shadow,
    "blink": icon_blink,
    "rift_yank": icon_rift,
    "phase_veil": icon_veil,
    "void_step": icon_void,
    "gravity_well": icon_well,
    "warp_bolt": icon_lance,
    "spatial_ward": icon_shield,
    "fold_repulse": icon_arrow,
    "rift_slash": icon_lance,
    "gravity_snare": icon_well,
    "dimensional_anchor": icon_chain,
    "far_blink": icon_blink,
    "warp_exchange": icon_rift,
    "spatial_surge": icon_wave,
    "void_lance": icon_lance,
    "rift_burst": icon_pulse,
    "spatial_singularity": icon_well,
    "absolute_fold": icon_void,
    "corrupt_mark": icon_corrupt_mark,
    "binding_seal": icon_binding_seal,
    "blight_pulse": icon_blight_pulse,
    "blight_brand": icon_blight_brand,
    "pestilence_wave": icon_pestilence_wave,
    "rot_touch": icon_rot_touch,
    "entropy_lash": icon_entropy_lash,
    "plague_bolt": icon_plague_bolt,
    "festering_wound": icon_festering_wound,
    "miasma_cloak": icon_miasma_cloak,
    "blight_surge": icon_blight_surge,
    "decay_bind": icon_decay_bind,
    "blight_field": icon_blight_field,
    "entropy_aegis": icon_entropy_aegis,
    "tainted_leech": icon_tainted_leech,
    "virulent_wave": icon_virulent_wave,
    "plague_crown": icon_plague_crown,
    "omega_blight": icon_omega_blight,
    "trap_seal": icon_trap,
    "fortify_seal": icon_shield,
    "glow_seal": icon_glow,
    "snare_glyph": icon_snare,
    "beacon_seal": icon_beacon,
    "shock_glyph": icon_trap,
    "ward_glyph": icon_frost_wall,
    "repulsion_seal": icon_wind,
    "anchor_fortify": icon_shield,
    "permanent_glow": icon_glow,
    "snare_matrix": icon_snare,
    "shock_trap": icon_trap,
    "omega_ward": icon_frost_wall,
}


def make_icon(spell: str) -> Image.Image:
    import spell_icon_art
    return spell_icon_art.make_icon(spell, SPELL_SCHOOL[spell])


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


def particle_mental_shard(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    shard = (190, 160, 255, 255)
    if variant % 2 == 0:
        d.polygon([(12, 2), (10, 7), (14, 9), (4, 14), (6, 8), (3, 6)], fill=shard)
    else:
        d.polygon([(3, 3), (8, 5), (7, 9), (13, 13), (9, 10), (10, 5)], fill=shard)
    draw_disc(d, 8, 8, 1, (255, 255, 255, 230))
    return img


def particle_mental_force(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    ring = (170, 140, 255, 180 - variant * 20)
    r = 6 - variant
    d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=ring, width=2)
    draw_disc(d, cx, cy, 1, (230, 210, 255, 200))
    return img


def particle_mental_synapse(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    glow = (90, 230, 255, 240)
    core = (230, 255, 255, 255)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    d.line([(cx, 2), (cx, 14)], fill=glow, width=1)
    d.line([(2, cy), (14, cy)], fill=glow, width=1)
    if variant % 2 == 0:
        d.line([(3, 3), (13, 13)], fill=glow, width=1)
    else:
        d.line([(13, 3), (3, 13)], fill=glow, width=1)
    draw_disc(d, cx, cy, 2, core)
    return img


def particle_mental_ward(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    ward = (150, 170, 255, 220)
    d.polygon([(cx, 2), (cx + 5, cy - 1), (cx + 3, 13), (cx - 3, 13), (cx - 5, cy - 1)], outline=ward)
    if variant % 2 == 0:
        draw_disc(d, cx, cy, 2, (220, 230, 255, 230))
    return img


def particle_mental_fear(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    fear = (120, 40, 90, 220) if variant % 2 == 0 else (90, 30, 70, 200)
    draw_disc(d, cx, cy, 5, fear)
    draw_disc(d, cx - 2, cy - 1, 1, (220, 80, 100, 230))
    draw_disc(d, cx + 2, cy - 1, 1, (220, 80, 100, 230))
    return img


def particle_mental_sense(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    d.ellipse((cx - 6, cy - 4, cx + 6, cy + 4), fill=(210, 230, 255, 200))
    draw_disc(d, cx, cy, 3, (100, 190, 255, 240))
    draw_disc(d, cx, cy, 1, (30, 20, 50, 255))
    if variant % 2 == 0:
        draw_disc(d, cx + 1, cy - 1, 1, (255, 255, 255, 230))
    return img


def particle_organic_leaf(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    hue = variant * 15
    fill = (60 + hue, 170 - hue // 2, 70, 230)
    tip = (cx, cy - 6 - variant)
    right = (cx + 5 + variant // 2, cy)
    bottom = (cx, cy + 6)
    left = (cx - 5 - variant // 2, cy)
    d.polygon([tip, right, bottom, left], fill=fill)
    d.line([(cx, cy - 5), (cx, cy + 5)], fill=(40, 110, 50, 200), width=1)
    return img


def particle_organic_root(variant: int = 0) -> Image.Image:
    """Winding root tendril — not a Y-claw/jaw silhouette."""
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    bark = (92, 62, 32, 245)
    moss = (70, 110, 45, 200)
    # S-curve or hook depending on variant
    if variant == 0:
        pts = [(8, 1), (7, 4), (9, 7), (6, 10), (8, 13), (5, 15)]
    elif variant == 1:
        pts = [(7, 1), (9, 4), (8, 7), (10, 10), (7, 13), (9, 15)]
    else:
        pts = [(8, 2), (6, 5), (8, 8), (5, 11), (7, 14)]
        d.line([(8, 8), (11, 11), (12, 14)], fill=bark, width=2)
    d.line(pts, fill=bark, width=2)
    # tiny side sprout
    mid = pts[len(pts) // 2]
    d.line([mid, (mid[0] - 3, mid[1] + 2)], fill=moss, width=1)
    return img


def particle_organic_fog() -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    for r, a in ((7, 45), (5, 70), (3, 100)):
        draw_disc(d, cx, cy, r, (80, 200, 90, a))
    return img


def particle_organic_spore(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx = PARTICLE // 2 + (variant - 1)
    cy = PARTICLE // 2
    fill = (160, 210, 90, 220) if variant % 2 == 0 else (120, 180, 70, 200)
    draw_disc(d, cx, cy, 3 + variant % 2, fill)
    draw_disc(d, cx - 1, cy - 1, 1, (230, 255, 180, 180))
    return img


def particle_organic_thorn() -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    green = (50, 140, 55, 245)
    tip = (90, 200, 80, 255)
    d.polygon([(8, 1), (10, 8), (8, 15), (6, 8)], fill=green)
    d.line([(8, 1), (8, 15)], fill=tip, width=1)
    d.line([(8, 6), (11, 9)], fill=green, width=1)
    return img


def particle_organic_sap() -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    amber = (210, 150, 35, 230)
    d.ellipse((5, 4, 11, 14), fill=amber)
    d.ellipse((6, 1, 10, 5), fill=amber)
    draw_disc(d, 7, 7, 1, (255, 230, 140, 200))
    return img


def particle_organic_blood_cell(variant: int = 0) -> Image.Image:
    """Biconcave erythrocyte disc."""
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2 + (variant - 1), PARTICLE // 2
    rim = (180, 40, 55, 240)
    body = (210, 55, 70, 220)
    center = (140, 25, 40, 200)
    draw_disc(d, cx, cy, 5, rim)
    draw_disc(d, cx, cy, 4, body)
    draw_disc(d, cx, cy, 2, center)
    draw_disc(d, cx - 1, cy - 1, 1, (255, 140, 150, 160))
    return img


def particle_organic_white_cell(variant: int = 0) -> Image.Image:
    """Leukocyte / stabilizer cell with nucleus."""
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2 + (variant % 2)
    membrane = (230, 235, 245, 210)
    nucleus = (90, 110, 180, 230)
    granule = (200, 210, 230, 180)
    draw_disc(d, cx, cy, 6, membrane)
    draw_disc(d, cx - 1, cy, 3, nucleus)
    draw_disc(d, cx + 2, cy - 2, 1, granule)
    draw_disc(d, cx + 1, cy + 2, 1, granule)
    return img


def particle_organic_virus(variant: int = 0) -> Image.Image:
    """Spiked virion."""
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    core = (170, 70, 200, 235) if variant % 2 == 0 else (140, 50, 180, 230)
    spike = (210, 140, 230, 240)
    draw_disc(d, cx, cy, 4, core)
    draw_disc(d, cx, cy, 2, (240, 200, 255, 200))
    for angle in range(0, 360, 45 + variant * 5):
        rad = math.radians(angle)
        x2 = cx + int(round(math.cos(rad) * 7))
        y2 = cy + int(round(math.sin(rad) * 7))
        d.line([(cx, cy), (x2, y2)], fill=spike, width=1)
        draw_disc(d, x2, y2, 1, spike)
    return img


def particle_organic_parasite(variant: int = 0) -> Image.Image:
    """Curved larva / worm."""
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    body = (180, 150, 60, 240)
    dark = (120, 90, 30, 220)
    if variant == 0:
        pts = [(2, 8), (5, 5), (9, 6), (12, 9), (14, 12)]
    else:
        pts = [(1, 10), (4, 7), (8, 8), (11, 6), (14, 8)]
    d.line(pts, fill=body, width=3)
    d.line(pts, fill=dark, width=1)
    head = pts[-1]
    draw_disc(d, head[0], head[1], 2, (220, 190, 80, 255))
    return img


def particle_organic_bone(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    bone = (235, 225, 200, 255)
    shade = (190, 175, 150, 230)
    if variant % 2 == 0:
        d.polygon([(8, 1), (10, 7), (8, 15), (6, 7)], fill=bone)
        draw_disc(d, 8, 2, 2, bone)
        draw_disc(d, 8, 14, 2, bone)
    else:
        d.polygon([(3, 4), (13, 6), (14, 10), (4, 12)], fill=bone)
        d.line([(4, 6), (12, 9)], fill=shade, width=1)
    return img


def particle_organic_chitin(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    shell = (110, 80, 45, 245) if variant % 2 == 0 else (90, 120, 55, 235)
    edge = (60, 45, 25, 220)
    d.polygon([(8, 2), (14, 7), (12, 14), (4, 14), (2, 7)], fill=shell)
    d.line([(8, 3), (8, 13)], fill=edge, width=1)
    d.line([(5, 8), (11, 8)], fill=edge, width=1)
    return img


def particle_organic_muscle(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    fiber = (170, 40, 55, 245)
    strand = (220, 80, 90, 220)
    y0 = 3 + variant
    d.line([(3, y0), (13, y0 + 4)], fill=fiber, width=2)
    d.line([(4, y0 + 3), (12, y0 + 7)], fill=strand, width=1)
    d.line([(5, y0 + 6), (11, y0 + 9)], fill=fiber, width=1)
    return img


def particle_organic_nerve(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    glow = (90, 230, 255, 230)
    core = (220, 255, 255, 255)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    d.line([(cx, 1), (cx, 15)], fill=glow, width=1)
    d.line([(1, cy), (15, cy)], fill=glow, width=1)
    if variant % 2 == 0:
        d.line([(3, 3), (13, 13)], fill=glow, width=1)
    else:
        d.line([(13, 3), (3, 13)], fill=glow, width=1)
    draw_disc(d, cx, cy, 2, core)
    return img


def particle_organic_dna(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    a = (60, 180, 220, 240)
    b = (220, 90, 140, 240)
    # two helix strands
    for i in range(0, 15, 2):
        ox = int(round(math.sin((i + variant) * 0.7) * 4))
        d.point((8 + ox, i + 1), fill=a)
        d.point((8 - ox, i + 1), fill=b)
        if i % 4 == 0:
            d.line([(8 + ox, i + 1), (8 - ox, i + 1)], fill=(180, 200, 220, 160), width=1)
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


def particle_necro_bone(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    ivory = (230, 220, 190, 240) if variant % 2 == 0 else (210, 200, 170, 230)
    teal = (80, 200, 160, 200)
    if variant % 2 == 0:
        d.polygon([(5, 4), (11, 3), (12, 8), (8, 12), (4, 9)], fill=ivory)
        draw_disc(d, 7, 7, 1, teal)
    else:
        d.polygon([(4, 6), (10, 4), (13, 9), (9, 13), (3, 10)], fill=ivory)
        d.line([(6, 6), (10, 10)], fill=teal, width=1)
    return img


def particle_necro_soul(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx = PARTICLE // 2
    cy = PARTICLE // 2 - (1 if variant % 2 == 0 else 0)
    soul = (120, 255, 200, 230) if variant % 2 == 0 else (100, 230, 180, 220)
    void = (20, 40, 50, 200)
    draw_disc(d, cx, cy - 1, 4, soul)
    d.polygon([(cx - 3, cy + 1), (cx + 3, cy + 1), (cx, cy + 6)], fill=soul)
    draw_disc(d, cx, cy - 1, 1, void)
    return img


def particle_necro_wither(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    dark = (40, 20, 50, 230)
    sickly = (180, 255, 100, 220)
    if variant % 2 == 0:
        d.point((5, 5), fill=dark)
        d.point((8, 4), fill=sickly)
        d.point((7, 8), fill=dark)
        d.point((10, 9), fill=sickly)
        d.point((4, 10), fill=(60, 40, 70, 200))
    else:
        d.line([(5, 6), (11, 10)], fill=dark, width=1)
        draw_disc(d, 8, 7, 1, sickly)
        d.point((6, 10), fill=sickly)
    return img


def particle_necro_grave(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    moss = (90, 110, 80, 240) if variant % 2 == 0 else (100, 120, 70, 230)
    tomb = (140, 140, 130, 220)
    d.polygon([(4, 6), (9, 4), (12, 9), (8, 13), (3, 10)], fill=moss)
    draw_disc(d, 7, 8, 1, tomb)
    if variant % 2 == 1:
        draw_disc(d, 10, 6, 1, (110, 130, 90, 200))
    return img


def particle_necro_shade(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2 + variant - 1, PARTICLE // 2 + 1
    shade = (60, 40, 80, 200) if variant % 2 == 0 else (50, 30, 70, 180)
    pale = (200, 190, 220, 160)
    d.polygon(
        [(cx, cy - 5), (cx + 5, cy + 4), (cx + 2, cy + 6), (cx - 2, cy + 6), (cx - 5, cy + 4)],
        fill=shade,
    )
    d.polygon([(cx, cy - 2), (cx + 2, cy + 2), (cx - 2, cy + 2)], fill=pale)
    return img


def particle_necro_bind(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    iron = (160, 160, 170, 240)
    green = (80, 220, 150, 200)
    if variant % 2 == 0:
        d.ellipse((3, 5, 10, 12), outline=iron, width=2)
        draw_disc(d, 6, 8, 1, green)
    else:
        d.ellipse((6, 4, 13, 11), outline=iron, width=2)
        d.line([(8, 5), (11, 10)], fill=green, width=1)
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


def particle_corruption_rot(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rot = (110, 90, 35, 240) if variant % 2 == 0 else (80, 110, 40, 230)
    d.polygon([(4, 5), (9, 3), (12, 8), (8, 13), (3, 10)], fill=rot)
    draw_disc(d, 7, 8, 1, (60, 50, 20, 200))
    return img


def particle_corruption_miasma(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2 + variant - 1, PARTICLE // 2
    for r, a in ((7, 45), (5, 75), (3, 110)):
        draw_disc(d, cx, cy, r, (90, 50, 120, a))
    draw_disc(d, cx, cy, 2, (120, 180, 70, 80))
    return img


def particle_corruption_entropy(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    ash = (170, 90, 200, 230)
    if variant % 2 == 0:
        d.point((6, 5), fill=ash)
        d.point((9, 7), fill=ash)
        d.point((7, 10), fill=(120, 50, 150, 220))
        d.point((11, 9), fill=ash)
    else:
        d.line([(5, 6), (11, 10)], fill=ash, width=1)
        draw_disc(d, 8, 8, 1, (220, 160, 255, 200))
    return img


def particle_corruption_bind(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    link = (140, 80, 180, 240)
    if variant % 2 == 0:
        d.ellipse((3, 5, 10, 12), outline=link, width=2)
    else:
        d.ellipse((6, 4, 13, 11), outline=link, width=2)
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


def particle_elemental_ember(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2 + (variant - 1), PARTICLE // 2
    draw_disc(d, cx, cy, 3, (255, 120, 40, 230))
    draw_disc(d, cx, cy, 1, (255, 220, 120, 240))
    return img


def particle_elemental_plasma(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    core = (200, 120, 255, 240) if variant % 2 == 0 else (120, 220, 255, 240)
    draw_disc(d, cx, cy, 5, core)
    draw_disc(d, cx, cy, 2, (255, 255, 255, 230))
    return img


def particle_elemental_spark(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    bolt = (180, 230, 255, 255)
    if variant % 2 == 0:
        d.line([(3, 2), (7, 7), (5, 9), (12, 14)], fill=bolt, width=2)
    else:
        d.line([(12, 2), (8, 6), (10, 9), (4, 14)], fill=bolt, width=2)
    draw_disc(d, 8, 8, 1, (255, 255, 255, 255))
    return img


def particle_elemental_vacuum(variant: int = 0) -> Image.Image:
    img = Image.new("RGBA", (PARTICLE, PARTICLE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = PARTICLE // 2, PARTICLE // 2
    for r, a in ((7, 50), (5, 90), (3, 140)):
        draw_disc(d, cx, cy, r, (20, 10, 35, a))
    draw_disc(d, cx, cy, 1, (60, 40, 90, 180))
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

    ember_paths = []
    for i in range(3):
        name = f"elemental_ember_{i}"
        save_particle_png(name, particle_elemental_ember(i))
        ember_paths.append(f"effecoria:{name}")
    write_particle_json("elemental_ember", ember_paths)

    plasma_paths = []
    for i in range(2):
        name = f"elemental_plasma_{i}"
        save_particle_png(name, particle_elemental_plasma(i))
        plasma_paths.append(f"effecoria:{name}")
    write_particle_json("elemental_plasma", plasma_paths)

    spark_paths = []
    for i in range(2):
        name = f"elemental_spark_{i}"
        save_particle_png(name, particle_elemental_spark(i))
        spark_paths.append(f"effecoria:{name}")
    write_particle_json("elemental_spark", spark_paths)

    vacuum_paths = []
    for i in range(2):
        name = f"elemental_vacuum_{i}"
        save_particle_png(name, particle_elemental_vacuum(i))
        vacuum_paths.append(f"effecoria:{name}")
    write_particle_json("elemental_vacuum", vacuum_paths)

    fog_paths = []
    for i in range(3):
        name = f"mental_fog_{i}"
        save_particle_png(name, particle_mental_fog(i))
        fog_paths.append(f"effecoria:{name}")
    write_particle_json("mental_fog", fog_paths)

    shard_paths = []
    for i in range(2):
        name = f"mental_shard_{i}"
        save_particle_png(name, particle_mental_shard(i))
        shard_paths.append(f"effecoria:{name}")
    write_particle_json("mental_shard", shard_paths)

    force_paths = []
    for i in range(2):
        name = f"mental_force_{i}"
        save_particle_png(name, particle_mental_force(i))
        force_paths.append(f"effecoria:{name}")
    write_particle_json("mental_force", force_paths)

    synapse_paths = []
    for i in range(2):
        name = f"mental_synapse_{i}"
        save_particle_png(name, particle_mental_synapse(i))
        synapse_paths.append(f"effecoria:{name}")
    write_particle_json("mental_synapse", synapse_paths)

    ward_paths = []
    for i in range(2):
        name = f"mental_ward_{i}"
        save_particle_png(name, particle_mental_ward(i))
        ward_paths.append(f"effecoria:{name}")
    write_particle_json("mental_ward", ward_paths)

    fear_paths = []
    for i in range(2):
        name = f"mental_fear_{i}"
        save_particle_png(name, particle_mental_fear(i))
        fear_paths.append(f"effecoria:{name}")
    write_particle_json("mental_fear", fear_paths)

    sense_paths = []
    for i in range(2):
        name = f"mental_sense_{i}"
        save_particle_png(name, particle_mental_sense(i))
        sense_paths.append(f"effecoria:{name}")
    write_particle_json("mental_sense", sense_paths)

    leaf_paths = []
    for i in range(3):
        name = f"organic_leaf_{i}"
        save_particle_png(name, particle_organic_leaf(i))
        leaf_paths.append(f"effecoria:{name}")
    write_particle_json("organic_leaf", leaf_paths)

    root_paths = []
    for i in range(3):
        name = f"organic_root_{i}"
        save_particle_png(name, particle_organic_root(i))
        root_paths.append(f"effecoria:{name}")
    write_particle_json("organic_root", root_paths)
    # Keep legacy single-file for any old refs
    save_particle_png("organic_root", particle_organic_root(0))

    save_particle("organic_fog", particle_organic_fog())

    spore_paths = []
    for i in range(3):
        name = f"organic_spore_{i}"
        save_particle_png(name, particle_organic_spore(i))
        spore_paths.append(f"effecoria:{name}")
    write_particle_json("organic_spore", spore_paths)
    save_particle_png("organic_spore", particle_organic_spore(0))

    save_particle("organic_thorn", particle_organic_thorn())
    save_particle("organic_sap", particle_organic_sap())

    blood_paths = []
    for i in range(3):
        name = f"organic_blood_cell_{i}"
        save_particle_png(name, particle_organic_blood_cell(i))
        blood_paths.append(f"effecoria:{name}")
    write_particle_json("organic_blood_cell", blood_paths)

    white_paths = []
    for i in range(2):
        name = f"organic_white_cell_{i}"
        save_particle_png(name, particle_organic_white_cell(i))
        white_paths.append(f"effecoria:{name}")
    write_particle_json("organic_white_cell", white_paths)

    virus_paths = []
    for i in range(3):
        name = f"organic_virus_{i}"
        save_particle_png(name, particle_organic_virus(i))
        virus_paths.append(f"effecoria:{name}")
    write_particle_json("organic_virus", virus_paths)

    parasite_paths = []
    for i in range(2):
        name = f"organic_parasite_{i}"
        save_particle_png(name, particle_organic_parasite(i))
        parasite_paths.append(f"effecoria:{name}")
    write_particle_json("organic_parasite", parasite_paths)

    bone_paths = []
    for i in range(2):
        name = f"organic_bone_{i}"
        save_particle_png(name, particle_organic_bone(i))
        bone_paths.append(f"effecoria:{name}")
    write_particle_json("organic_bone", bone_paths)

    chitin_paths = []
    for i in range(2):
        name = f"organic_chitin_{i}"
        save_particle_png(name, particle_organic_chitin(i))
        chitin_paths.append(f"effecoria:{name}")
    write_particle_json("organic_chitin", chitin_paths)

    muscle_paths = []
    for i in range(2):
        name = f"organic_muscle_{i}"
        save_particle_png(name, particle_organic_muscle(i))
        muscle_paths.append(f"effecoria:{name}")
    write_particle_json("organic_muscle", muscle_paths)

    nerve_paths = []
    for i in range(2):
        name = f"organic_nerve_{i}"
        save_particle_png(name, particle_organic_nerve(i))
        nerve_paths.append(f"effecoria:{name}")
    write_particle_json("organic_nerve", nerve_paths)

    dna_paths = []
    for i in range(3):
        name = f"organic_dna_{i}"
        save_particle_png(name, particle_organic_dna(i))
        dna_paths.append(f"effecoria:{name}")
    write_particle_json("organic_dna", dna_paths)

    shadow_paths = []
    for i in range(3):
        name = f"necro_shadow_{i}"
        save_particle_png(name, particle_necro_shadow(i))
        shadow_paths.append(f"effecoria:{name}")
    write_particle_json("necro_shadow", shadow_paths)
    save_particle("necro_fog", particle_necro_fog())

    bone_paths = []
    for i in range(2):
        name = f"necro_bone_{i}"
        save_particle_png(name, particle_necro_bone(i))
        bone_paths.append(f"effecoria:{name}")
    write_particle_json("necro_bone", bone_paths)

    soul_paths = []
    for i in range(2):
        name = f"necro_soul_{i}"
        save_particle_png(name, particle_necro_soul(i))
        soul_paths.append(f"effecoria:{name}")
    write_particle_json("necro_soul", soul_paths)

    wither_paths = []
    for i in range(2):
        name = f"necro_wither_{i}"
        save_particle_png(name, particle_necro_wither(i))
        wither_paths.append(f"effecoria:{name}")
    write_particle_json("necro_wither", wither_paths)

    grave_paths = []
    for i in range(2):
        name = f"necro_grave_{i}"
        save_particle_png(name, particle_necro_grave(i))
        grave_paths.append(f"effecoria:{name}")
    write_particle_json("necro_grave", grave_paths)

    shade_paths = []
    for i in range(2):
        name = f"necro_shade_{i}"
        save_particle_png(name, particle_necro_shade(i))
        shade_paths.append(f"effecoria:{name}")
    write_particle_json("necro_shade", shade_paths)

    necro_bind_paths = []
    for i in range(2):
        name = f"necro_bind_{i}"
        save_particle_png(name, particle_necro_bind(i))
        necro_bind_paths.append(f"effecoria:{name}")
    write_particle_json("necro_bind", necro_bind_paths)

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

    rot_paths = []
    for i in range(2):
        name = f"corruption_rot_{i}"
        save_particle_png(name, particle_corruption_rot(i))
        rot_paths.append(f"effecoria:{name}")
    write_particle_json("corruption_rot", rot_paths)

    miasma_paths = []
    for i in range(3):
        name = f"corruption_miasma_{i}"
        save_particle_png(name, particle_corruption_miasma(i))
        miasma_paths.append(f"effecoria:{name}")
    write_particle_json("corruption_miasma", miasma_paths)

    entropy_paths = []
    for i in range(2):
        name = f"corruption_entropy_{i}"
        save_particle_png(name, particle_corruption_entropy(i))
        entropy_paths.append(f"effecoria:{name}")
    write_particle_json("corruption_entropy", entropy_paths)

    bind_paths = []
    for i in range(2):
        name = f"corruption_bind_{i}"
        save_particle_png(name, particle_corruption_bind(i))
        bind_paths.append(f"effecoria:{name}")
    write_particle_json("corruption_bind", bind_paths)

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
