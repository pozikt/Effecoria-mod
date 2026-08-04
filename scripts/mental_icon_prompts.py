#!/usr/bin/env python3
"""Exact user visual-action prompts for Mental school spell icons."""
from __future__ import annotations

# id -> full prompt (user-authored)
MENTAL_PROMPTS: dict[str, str] = {
    # Direct attacks
    "mind_bolt": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A bolt of psychic energy with a jagged zigzag pattern, electric blue and white, striking forward. "
        "Simple bold silhouette, flat pixel clusters. Palette: bright blue, white, purple, dark blue. "
        "Dark background, thin blue border."
    ),
    "mind_lance": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A sharp spear made of focused mental energy, with a glowing tip and 3 psychic wave rings trailing behind. "
        "Simple flat pixel art, sharp edges. Palette: pale yellow, white, cyan, dark purple. "
        "Dark background, thin yellow border."
    ),
    "thought_lance": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A dense lance of pure thought, with 5 brainwave ripple rings and a bright white piercing tip. "
        "Simple bold silhouette, flat colors. Palette: white, pale blue, gold, gray. "
        "Dark background, thin white border."
    ),
    "mental_sting": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A small sharp needle of psychic energy with a buzzing aura and 3 confusion lines around the target. "
        "Simple silhouette, high contrast. Palette: cyan, purple, white, dark blue. "
        "Dark background, thin cyan border."
    ),
    "psychic_scream": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "An open screaming mouth with 6 concentric sound wave rings radiating outward, distorting the air. "
        "Simple bold shapes, flat pixel art. Palette: purple, white, dark red, pale blue. "
        "Dark background, thin purple border."
    ),
    "psychic_frenzy": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A figure with 3 swirling chaotic spiral eyes, surrounded by 8 jagged madness lines and a purple aura. "
        "Simple flat pixel art, sharp edges. Palette: bright purple, yellow, white, dark red. "
        "Dark background, thin purple border."
    ),
    "synaptic_overload": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A brain with 6 overloading neural sparks, jagged electrical lines, and 3 overload warning symbols. "
        "Simple bold silhouette, flat pixel clusters. Palette: electric blue, yellow, white, dark purple. "
        "Dark background, thin blue border."
    ),
    "telekinetic_crush": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A crushed figure with a heavy purple psychic weight pressing down, surrounded by 4 doubt wave lines. "
        "Simple silhouette, high contrast. Palette: dark purple, gray, white, pale blue. "
        "Dark background, thin purple border."
    ),
    "thought_bomb": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A massive explosion of mental energy, with 7 debris thought fragments and a bright white blast wave. "
        "Simple bold shapes, flat pixel art. Palette: white, yellow, purple, cyan. "
        "Dark background, thin white border."
    ),
    "psychic_storm": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A swirling vortex of mental chaos, with 5 brain fragments and 8 confusion spiral lines in the storm. "
        "Simple flat pixel art, sharp edges. Palette: dark purple, cyan, white, gray. "
        "Dark background, thin purple border."
    ),
    "mass_confusion": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "4 small confused figures with question marks above their heads, surrounded by wavy distortion lines. "
        "Simple bold silhouette, flat colors. Palette: pale blue, yellow, gray, white. "
        "Dark background, thin blue border."
    ),
    "mass_hysteria": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "5 small figures all panicking in different directions, with 6 jagged hysteria lines and a red chaos aura. "
        "Simple silhouette, flat pixel clusters. Palette: red, orange, purple, white. "
        "Dark background, thin red border."
    ),
    "neural_lock": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A nerve ending with 3 frozen stop symbols and a lock icon, blocking neural signals. "
        "Simple bold shapes, flat pixel art. Palette: dark blue, cyan, red, white. "
        "Dark background, thin blue border."
    ),
    # Fear / suggestion
    "cliff_urge": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A figure standing at the edge of a cliff, leaning forward with 3 hypnotic spiral eyes pulling them down. "
        "Simple bold silhouette, flat pixel clusters. Palette: dark blue, pale yellow, gray, white. "
        "Dark background, thin yellow border."
    ),
    "drown_urge": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A figure being pulled underwater by 3 ghostly hands, with bubbles and a dark deep blue abyss below. "
        "Simple flat pixel art, sharp edges. Palette: dark blue, pale blue, white, gray. "
        "Dark background, thin blue border."
    ),
    "mind_terror": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A terrified figure with wide eyes, 3 screaming face symbols, and 4 fleeing footprints behind them. "
        "Simple bold silhouette, flat colors. Palette: dark red, purple, white, gray. "
        "Dark background, thin red border."
    ),
    "mind_illusion": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A distorted fake environment with 3 floating red eyes, a blood lake, and a skull cloud in a dreamlike haze. "
        "Simple silhouette, high contrast. Palette: red, black, white, dark gray. "
        "Dark background, thin red border."
    ),
    "total_veil": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A completely fake reality with 5 impossible geometric shapes, floating islands, and a twisted sun. "
        "Simple bold shapes, flat pixel art. Palette: purple, pink, cyan, gold, white. "
        "Dark background, thin white border."
    ),
    "false_memory": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A memory bubble with 3 fake happy scenes overwriting a dark real memory, with a brain edit icon. "
        "Simple flat pixel art, sharp edges. Palette: gold, pale blue, gray, white. "
        "Dark background, thin gold border."
    ),
    "psi_whisper": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A simple whisper wave going into an ear, with a gentle push arrow and 3 soft pulse lines. "
        "Simple bold silhouette, flat colors. Palette: pale blue, white, light purple, gray. "
        "Dark background, thin blue border."
    ),
    # Defense
    "psychic_barrier": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A white noise wall of static with 4 jamming wave lines, blocking a scanning beam from entering. "
        "Simple flat pixel art, sharp edges. Palette: white, gray, pale blue, dark blue. "
        "Dark background, thin white border."
    ),
    "mental_fortress": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A solid castle made of mental blocks, with 4 reinforcing pillars and a glowing crest on the wall. "
        "Simple bold silhouette, flat pixel clusters. Palette: gray, blue, gold, white. "
        "Dark background, thin gold border."
    ),
    "psychic_focus": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A third eye on the forehead glowing brightly, with 3 concentration lines and a calm meditating figure. "
        "Simple bold shapes, flat pixel art. Palette: gold, white, pale blue, purple. "
        "Dark background, thin gold border."
    ),
    "psychic_amplify": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A magnifying glass over a brain, with 3 upward amplification arrows and a glowing aura. "
        "Simple flat pixel art, sharp edges. Palette: bright yellow, blue, white, purple. "
        "Dark background, thin yellow border."
    ),
    # Scan
    "sense_phi": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A radar pulse of blue emotion waves, with 3 small heart icons at different distances being scanned. "
        "Simple bold silhouette, flat colors. Palette: cyan, pink, white, dark blue. "
        "Dark background, thin cyan border."
    ),
    "mind_probe": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A data probe with a glowing tip entering a head, extracting 3 floating data crystal icons (HP, AI, equipment). "
        "Simple silhouette, flat pixel clusters. Palette: green, blue, white, gold. "
        "Dark background, thin green border."
    ),
    "locus_echo": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A glowing memory fragment from a location, with 3 blurred coordinate numbers and a ghostly scene of the past. "
        "Simple flat pixel art, sharp edges. Palette: pale blue, purple, white, gray. "
        "Dark background, thin blue border."
    ),
    "psi_echo": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A glowing blue psychic decoy of a humanoid figure, with 3 taunt lines drawing enemy aggression away. "
        "Simple bold silhouette, flat colors. Palette: bright blue, cyan, white, dark blue. "
        "Dark background, thin cyan border."
    ),
    # Control
    "mind_dominate": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A glowing puppet string going from a caster's hand to a controlled figure, with 3 domination rings around the target's head. "
        "Simple flat pixel art, sharp edges. Palette: purple, pink, white, dark blue. "
        "Dark background, thin purple border."
    ),
    "hive_mind": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "6 small heads connected by glowing blue psychic lines to a central hive brain, forming a network. "
        "Simple bold silhouette, flat pixel clusters. Palette: blue, cyan, white, purple. "
        "Dark background, thin cyan border."
    ),
    "dream_lock": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A sleeping figure with 3 heavy chains holding them down, and a dark dream shadow looming above. "
        "Simple bold shapes, flat pixel art. Palette: dark purple, gray, white, pale blue. "
        "Dark background, thin purple border."
    ),
    "mental_push": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A psychic push wave shoving a figure backward, with 3 motion lines and a recoil effect. "
        "Simple flat pixel art, sharp edges. Palette: pale blue, white, gray, dark blue. "
        "Dark background, thin blue border."
    ),
    # Lethal
    "omega_mind": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A supermassive psychic overload with 10 brainwave rings, a bright white blast, and 3 skull icons of mental exhaustion. "
        "Simple bold silhouette, flat colors. Palette: white, bright yellow, purple, dark red. "
        "Dark background, thin white border."
    ),
    "psychic_drain": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A vampire-like psychic leech attached to a brain, siphoning energy with 3 drain beams returning to the caster. "
        "Simple silhouette, high contrast. Palette: dark purple, red, white, cyan. "
        "Dark background, thin red border."
    ),
}
