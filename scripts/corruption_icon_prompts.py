#!/usr/bin/env python3
"""Exact user visual-action prompts for Corruption school spell icons."""
from __future__ import annotations

# id -> full prompt (user-authored)
CORRUPTION_PROMPTS: dict[str, str] = {
    # Marks / seals
    "binding_seal": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A dark purple binding seal with 4 sharp angular runes forming a cage pattern, glowing with containment energy. "
        "Simple bold silhouette, flat pixel clusters. Palette: dark purple, bright purple, black, white. "
        "Dark background, thin purple border."
    ),
    "blight_brand": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A glowing brand mark of blight on a target's skin, with 3 infection lines spreading outward like a cursed tattoo. "
        "Simple flat pixel art, sharp edges. Palette: sick green, dark purple, black, pale yellow. "
        "Dark background, thin green border."
    ),
    "corrupt_mark": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A simple dark mark of corruption with 3 spreading veins, like a growing stain on the target. "
        "Simple bold silhouette, flat colors. Palette: dark purple, black, pale green, gray. "
        "Dark background, thin purple border."
    ),
    "prey_mark": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A glowing red curse mark above a terrified player's head, with 6 hostile eyes from surrounding mobs "
        "all locked onto them, and 4 targeting crosshairs pointing at the victim. "
        "Simple bold silhouette, flat pixel clusters. Palette: bright red, dark red, yellow, black, white. "
        "Dark background, thin red circular border."
    ),
    # Fields / auras
    "blight_field": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A ground area covered in glowing blight with 5 corrupted patches, sickly green fog, and decaying plants. "
        "Simple flat pixel art, sharp edges. Palette: dark green, pale green, brown, purple, black. "
        "Dark background, thin green border."
    ),
    "blight_pulse": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A radial pulse wave of blight energy, with 6 concentric rings of corruption spreading outward from the center. "
        "Simple bold silhouette, flat pixel clusters. Palette: dark purple, green, black, pale yellow. "
        "Dark background, thin purple border."
    ),
    "miasma_cloak": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A flowing cloak of toxic miasma wrapping around a figure, with 4 poison cloud wisps and a toxic aura. "
        "Simple bold shapes, flat pixel art. Palette: sick green, dark purple, black, pale gray. "
        "Dark background, thin green border."
    ),
    "entropy_aegis": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A defensive shield made of decaying entropy, with 4 disintegration rings and a pattern of crumbling matter. "
        "Simple flat pixel art, sharp edges. Palette: dark purple, gray, black, pale blue. "
        "Dark background, thin purple border."
    ),
    "plague_crown": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A spiked crown made of diseased tissue and bone, with 3 weeping sores and a green plague aura above it. "
        "Simple bold silhouette, flat colors. Palette: dark green, gold, dark red, purple. "
        "Dark background, thin green border."
    ),
    # Direct attacks
    "blight_surge": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A sudden burst of blight energy erupting upward, with 5 jagged corruption spikes and a splash of sickly green. "
        "Simple bold shapes, flat pixel art. Palette: bright green, dark purple, black, yellow. "
        "Dark background, thin green border."
    ),
    "decay_bind": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "3 rotting chains of decay wrapping around a trapped figure, with crumbling links and falling dust. "
        "Simple flat pixel art, sharp edges. Palette: dark brown, green, gray, black. "
        "Dark background, thin brown border."
    ),
    "entropy_lash": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A whip made of entropic energy, with 4 cracking trails and disintegration sparks, striking diagonally. "
        "Simple bold silhouette, flat colors. Palette: dark purple, green, black, white. "
        "Dark background, thin purple border."
    ),
    "festering_wound": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "An open infected wound with 3 pus drips, surrounded by green infection spreading and 2 maggot-like marks. "
        "Simple silhouette, high contrast. Palette: dark red, sick green, yellow, black, pale pink. "
        "Dark background, thin red border."
    ),
    "pestilence_wave": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A wave of pestilence spreading across the ground, with 6 infected spots and a green cloud of disease. "
        "Simple flat pixel art, sharp edges. Palette: dark green, pale green, brown, black. "
        "Dark background, thin green border."
    ),
    "plague_bolt": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A projectile made of concentrated plague, with a skull-shaped head and a trail of infected mist. "
        "Simple bold shapes, flat pixel art. Palette: sick green, dark purple, black, pale yellow. "
        "Dark background, thin green border."
    ),
    "rot_touch": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A rotting hand reaching forward, with 3 decaying fingers and green rot spreading from the touch. "
        "Simple flat pixel art, sharp edges. Palette: brown, green, pale gray, black. "
        "Dark background, thin brown border."
    ),
    "tainted_leech": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A corrupted leech creature with 3 suckers, draining life and spreading contamination with each feed. "
        "Simple bold silhouette, flat colors. Palette: dark purple, dark green, red, black. "
        "Dark background, thin purple border."
    ),
    "virulent_wave": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A highly contagious wave of virulent energy, with 7 infection particles and a violent green blast. "
        "Simple silhouette, high contrast. Palette: bright green, yellow, black, dark purple. "
        "Dark background, thin green border."
    ),
    # Ultimate
    "omega_blight": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A catastrophic blight explosion with 8 corruption spikes, a dark purple and green blast, and 3 skull death symbols. "
        "Simple bold shapes, flat pixel art. Palette: bright purple, sick green, black, white. "
        "Dark background, thin purple border."
    ),
}
