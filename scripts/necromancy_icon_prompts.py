#!/usr/bin/env python3
"""Exact user visual-action prompts for Necromancy school spell icons."""
from __future__ import annotations

# id -> full prompt (user-authored)
NECROMANCY_PROMPTS: dict[str, str] = {
    # Summon
    "raise_skeleton": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A simple skeleton minion with a bow and empty eye sockets, standing in a defensive pose. "
        "Simple bold silhouette, flat pixel clusters. Palette: bone white, dark gray, black, pale yellow. "
        "Dark background, thin white border."
    ),
    "raise_zombie": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A rotting zombie with green skin, torn clothes, and empty glowing eyes, lurching forward. "
        "Simple flat pixel art, sharp edges. Palette: sick green, dark green, brown, pale gray. "
        "Dark background, thin green border."
    ),
    "shade_summon": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A translucent purple spirit with a wispy tail, bound by a glowing pact chain to a summoner's hand. "
        "Simple bold silhouette, flat colors. Palette: dark purple, pale blue, cyan, white. "
        "Dark background, thin purple border."
    ),
    "shade_swarm": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "5 small shadowy figures with glowing red eyes, swarming together like a dark cloud of spirits. "
        "Simple silhouette, high contrast. Palette: black, dark purple, red, gray. "
        "Dark background, thin red border."
    ),
    "shade_brood": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A dark nest of 4 shadow creatures hatching from dark eggs, with small glowing eyes and claw marks. "
        "Simple bold shapes, flat pixel art. Palette: dark purple, black, red, pale gray. "
        "Dark background, thin purple border."
    ),
    "army_of_dead": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "6 skeleton and zombie silhouettes marching in formation, with a dark banner and 3 death symbols. "
        "Simple flat pixel art, sharp edges. Palette: bone white, dark green, black, gray. "
        "Dark background, thin white border."
    ),
    "thrall_focus": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "One pale undead thrall with a bone-white targeting reticle over its head and a command arrow. "
        "Simple bold silhouette, flat pixel clusters. Palette: bone white, olive gray, black, pale green. "
        "Dark background, thin white border."
    ),
    "dark_pact": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A demonic contract written in blood, with 3 glowing Ω symbols, a soul payment icon, and a dark handshake. "
        "Simple bold silhouette, flat colors. Palette: dark red, black, orange, gold. "
        "Dark background, thin red border."
    ),
    "lich_ascension": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A figure transforming into a lich with glowing eyes, holding a phylactery, with a dark soul container floating above. "
        "Simple silhouette, flat pixel clusters. Palette: dark purple, black, green, gold. "
        "Dark background, thin purple border."
    ),
    # Bone / necrotic attacks
    "bone_volley": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A spread of 5 sharp white bones flying diagonally, each with a small splinter trail. "
        "Simple bold shapes, flat pixel art. Palette: bone white, gray, pale yellow, dark brown. "
        "Dark background, thin white border."
    ),
    "bone_armor": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A full set of bone plate armor with a skull helmet, ribcage chestplate, and shoulder spikes. "
        "Simple flat pixel art, sharp edges. Palette: white, pale gray, dark gray, beige. "
        "Dark background, thin white border."
    ),
    "bone_chill": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A frozen bone with 3 ice crystals growing from it, surrounded by a chilling blue-white mist. "
        "Simple bold silhouette, flat colors. Palette: pale blue, white, gray, dark blue. "
        "Dark background, thin blue border."
    ),
    "corpse_burst": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A corpse exploding with green necrotic energy, body parts and bone fragments flying in all directions. "
        "Simple silhouette, high contrast. Palette: dark green, pale green, red, brown, white. "
        "Dark background, thin green border."
    ),
    "death_coil": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A coiling serpent made of necrotic energy, with a skull-shaped head and a trailing smoke tail. "
        "Simple bold shapes, flat pixel art. Palette: dark purple, green, black, white. "
        "Dark background, thin purple border."
    ),
    "grave_bind": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "3 heavy grave chains made of black iron, wrapping around a trapped figure with a tombstone anchor. "
        "Simple flat pixel art, sharp edges. Palette: dark gray, black, pale blue, white. "
        "Dark background, thin gray border."
    ),
    "grave_field": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "4 grave mounds with wooden crosses, surrounded by a dark green blight aura and rising spirits. "
        "Simple bold silhouette, flat colors. Palette: dark green, brown, gray, pale purple. "
        "Dark background, thin green border."
    ),
    "necrotic_bolt": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A dark green necrotic bolt with a skull-like projectile head and a trailing black smoke path. "
        "Simple silhouette, flat pixel clusters. Palette: sick green, black, dark gray, white. "
        "Dark background, thin green border."
    ),
    "necrotic_aura": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A dark green aura around a figure, with 4 skull symbols fading in and out, draining life. "
        "Simple bold shapes, flat pixel art. Palette: dark green, pale green, black, gray. "
        "Dark background, thin green border."
    ),
    "wither_touch": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A withered gray hand reaching forward, with 3 decay cracks spreading from the touch point. "
        "Simple flat pixel art, sharp edges. Palette: pale gray, dark gray, green, black. "
        "Dark background, thin gray border."
    ),
    "wither_wave": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A radial wave of gray desiccation spreading outward, with 5 withering plants turning brown. "
        "Simple bold silhouette, flat colors. Palette: pale gray, brown, dark gray, green. "
        "Dark background, thin gray border."
    ),
    "curse_of_frailty": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A cracked glass figure with 3 curse symbols above it, and a breaking bone icon at the bottom. "
        "Simple silhouette, high contrast. Palette: pale blue, gray, white, dark purple. "
        "Dark background, thin blue border."
    ),
    # Ghosts / souls
    "death_shadow": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A dark shadow silhouette of a player, drifting toward a location with 3 ghostly trails behind it. "
        "Simple bold shapes, flat pixel art. Palette: black, dark purple, pale gray, white. "
        "Dark background, thin purple border."
    ),
    "haunting_visage": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A terrifying ghost face with hollow eyes, an open screaming mouth, and 3 wispy body trails below. "
        "Simple flat pixel art, sharp edges. Palette: pale white, dark gray, blue, purple. "
        "Dark background, thin white border."
    ),
    "phantom_step": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A ghostly footstep with 3 fade trails, moving forward with a spectral afterimage of a figure. "
        "Simple bold silhouette, flat colors. Palette: pale blue, white, gray, dark purple. "
        "Dark background, thin blue border."
    ),
    "grave_whisper": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A ghostly whisper wave coming from a grave, with 3 spirit fragments and a faint echo symbol. "
        "Simple silhouette, flat pixel clusters. Palette: pale blue, gray, white, dark purple. "
        "Dark background, thin blue border."
    ),
    "soul_shackle": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A bound spirit with 3 interrogation chains, 2 memory extraction beams, and a truth-telling glow. "
        "Simple bold shapes, flat pixel art. Palette: dark purple, green, white, gold. "
        "Dark background, thin purple border."
    ),
    "soul_reaper": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A hooded reaper figure with a glowing scythe, harvesting a soul from a peaceful rest symbol. "
        "Simple flat pixel art, sharp edges. Palette: black, dark purple, pale green, white. "
        "Dark background, thin white border."
    ),
    "soul_cataclysm": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "3 souls being fused into one unstable puppet figure, with surgery tools and unstable sparks. "
        "Simple bold silhouette, flat colors. Palette: purple, green, red, white, black. "
        "Dark background, thin purple border."
    ),
    # Death
    "death_mark": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A glowing skull mark on a target's chest, with 3 countdown lines and a death timer pixel. "
        "Simple bold shapes, flat pixel art. Palette: dark red, black, white, pale yellow. "
        "Dark background, thin red border."
    ),
    "mark_reap": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A curved bone scythe cutting through a marked pale skull with a red death-rune and cream soul sparks. "
        "Simple flat pixel art, sharp edges. Palette: bone white, cream, dark red, black. "
        "Dark background, thin white border."
    ),
    "death_gate": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A dark portal made of bones and skulls, with 3 death energy lines and a figure leaping through. "
        "Simple flat pixel art, sharp edges. Palette: dark purple, black, white, pale blue. "
        "Dark background, thin purple border."
    ),
    "death_apotheosis": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A divine apotheosis of death with 5 skull symbols, a dark crown, and an aura of absolute mortality. "
        "Simple bold silhouette, flat colors. Palette: black, white, dark red, gold. "
        "Dark background, thin gold border."
    ),
    "life_tap": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A siphon beam draining red life energy from a target into the caster, with 3 health transfer symbols. "
        "Simple silhouette, high contrast. Palette: bright red, dark red, white, black. "
        "Dark background, thin red border."
    ),
    "soul_drain": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A dark hand gripping a glowing blue soul, pulling it out of a struggling body with 3 extraction lines. "
        "Simple bold shapes, flat pixel art. Palette: dark purple, blue, white, gray. "
        "Dark background, thin blue border."
    ),
    "grave_leech": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A leech-like creature made of grave dirt and bones, attached to a target, draining life with 3 red lines. "
        "Simple flat pixel art, sharp edges. Palette: dark brown, green, red, gray. "
        "Dark background, thin red border."
    ),
    "siphon_pulse": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A radial pulse wave that drains life from nearby targets, with 5 small red energy streams returning. "
        "Simple bold silhouette, flat colors. Palette: dark red, purple, black, white. "
        "Dark background, thin red border."
    ),
    # Defense / utility
    "death_sense": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A radar pulse that detects death echoes, with 3 fading ghost markers and a dark compass. "
        "Simple bold shapes, flat pixel art. Palette: dark purple, pale blue, white, gray. "
        "Dark background, thin purple border."
    ),
    "lich_ward": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A sentinel guardian made of bone and shadow, standing watch with a glowing sentry eye and 3 ward runes. "
        "Simple flat pixel art, sharp edges. Palette: dark gray, white, purple, green. "
        "Dark background, thin purple border."
    ),
    "phylactery_surge": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A glowing phylactery container pulsing with power, surrounded by 3 drain rings and a surge blast. "
        "Simple bold silhouette, flat colors. Palette: gold, dark purple, green, white. "
        "Dark background, thin gold border."
    ),
    "soul_anchor": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A heavy anchor made of soul energy, with 3 grounding chains and a stable rune symbol. "
        "Simple silhouette, flat pixel clusters. Palette: dark blue, white, gray, pale purple. "
        "Dark background, thin blue border."
    ),
}
