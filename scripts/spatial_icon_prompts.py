#!/usr/bin/env python3
"""Exact user visual-action prompts for Spatial school spell icons."""
from __future__ import annotations

# id -> full prompt (user-authored)
SPATIAL_PROMPTS: dict[str, str] = {
    # Teleport / travel
    "blink": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A small humanoid figure mid-teleport with blue afterimage trails and a flash of light, blinking forward. "
        "Simple bold silhouette, flat pixel clusters. Palette: bright blue, cyan, white, dark purple. "
        "Dark background, thin blue border."
    ),
    "far_blink": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "An open swirling portal with a dark starry void inside and 3 floating purple runes around it, long-range travel. "
        "Simple flat pixel art, sharp edges. Palette: deep purple, magenta, blue, white. "
        "Dark background, thin purple border."
    ),
    "void_step": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A small floating dark bag with 9 glowing slot pixels arranged in a 3x3 grid, like inventory storage. "
        "Simple bold silhouette, flat colors. Palette: dark gray, purple, cyan, white. "
        "Dark background, thin purple border."
    ),
    "subspace_voyage": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A miniature bubble world inside a circular frame, with tiny stars, a planet, and floating rocks in a private dimension. "
        "Simple silhouette, flat pixel clusters. Palette: deep blue, purple, white, cyan. "
        "Dark background, thin white border."
    ),
    # Combat
    "rift_slash": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A jagged black crack cutting diagonally across the circle, with white void light bleeding out of the edges. "
        "Simple bold silhouette, high contrast. Palette: black, white, dark purple, bright cyan. "
        "Dark background, thin white border."
    ),
    "void_lance": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A spear made of pure darkness and stars, with a bright white tip, surrounded by 3 small spatial cracks. "
        "Simple silhouette, flat pixel art. Palette: dark purple, black, white, pale blue. "
        "Dark background, thin white border."
    ),
    "warp_exchange": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "Two humanoid pixel figures swapping places, connected by a curved purple line and two glow flashes. "
        "Simple bold shapes, flat colors. Palette: purple, cyan, white, dark blue. "
        "Dark background, thin purple border."
    ),
    "rift_burst": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "An explosive eruption of jagged black shards and white light, bursting outward from the center in all directions. "
        "Simple flat pixel art, sharp edges. Palette: black, white, dark purple, gray. "
        "Dark background, thin white border."
    ),
    "gravity_well": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A deep dark gravity pit with 3 spiral rings of debris and a crushed enemy pixel being pulled into the center. "
        "Simple bold silhouette, flat pixel clusters. Palette: dark blue, purple, gray, white. "
        "Dark background, thin purple border."
    ),
    "fold_repulse": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A powerful radial push blast of purple energy, with 6 outward arrows and 3 small figure pixels being flung away. "
        "Simple silhouette, high contrast. Palette: bright purple, cyan, white, dark blue. "
        "Dark background, thin purple border."
    ),
    "rift_yank": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A spatial hook arm of purple energy reaching forward and grabbing a small enemy pixel, pulling it toward the user. "
        "Simple bold shapes, flat colors. Palette: purple, magenta, white, dark blue. "
        "Dark background, thin magenta border."
    ),
    "spatial_surge": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A chaotic blast of warped space with 5 jagged purple arcs and small debris blocks flying outward. "
        "Simple flat pixel art, sharp edges. Palette: bright purple, blue, white, dark gray. "
        "Dark background, thin purple border."
    ),
    # Defense / utility
    "absolute_fold": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A glowing egg-shaped cocoon of folded space, with a faint safe humanoid silhouette inside, shielded from damage. "
        "Simple bold silhouette, flat pixel clusters. Palette: pale blue, cyan, white, dark purple. "
        "Dark background, thin cyan border."
    ),
    "phase_veil": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A semi-transparent phantom-like figure with blurred edges and wavy distortion lines, phasing out of danger. "
        "Simple flat pixel art, sharp edges. Palette: white, pale purple, light blue, gray. "
        "Dark background, thin white border."
    ),
    "spatial_ward": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A curved lens of warped space bending an incoming arrow pixel sideways, with faint distortion rings. "
        "Simple bold silhouette, flat colors. Palette: light blue, cyan, gray, white. "
        "Dark background, thin cyan border."
    ),
    "dimensional_anchor": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A broken pocket watch with a glowing purple time loop symbol, and a figure stuck repeating the same movement. "
        "Simple silhouette, flat pixel clusters. Palette: dark purple, gold, cyan, white. "
        "Dark background, thin gold border."
    ),
    "rift_excise": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A clean square cut out of reality, with a block of matter being sucked into a black void dimension. "
        "Simple bold shapes, flat pixel art. Palette: black, purple, white, pale blue. "
        "Dark background, thin purple border."
    ),
    # Sense / gravity
    "warp_bolt": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A glowing magic eye with 3 concentric rings and a purple radar pulse, scanning the terrain ahead. "
        "Simple flat pixel art, sharp edges. Palette: cyan, purple, white, dark blue. "
        "Dark background, thin cyan border."
    ),
    "gravity_snare": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A stylized humanoid figure walking sideways on a wall, with reversed gravity arrows pointing to the side. "
        "Simple bold silhouette, flat colors. Palette: dark purple, cyan, white, gray. "
        "Dark background, thin purple border."
    ),
    "spatial_singularity": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A blinding white point with 5 concentric colorful rings (purple, blue, cyan, white) radiating outward like a pulsar. "
        "Simple silhouette, high contrast. Palette: white, bright cyan, magenta, dark blue. "
        "Dark background, thin white border."
    ),
}
