#!/usr/bin/env python3
"""Exact user visual-action prompts for Elemental school spell icons."""
from __future__ import annotations

# id -> full prompt (user-authored)
ELEMENTAL_PROMPTS: dict[str, str] = {
    # Fire / plasma
    "ember_volley": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "Three small orange fireballs flying in a spread pattern, each with a trailing flame pixel. "
        "Simple bold silhouette, flat pixel clusters. Palette: bright orange, yellow, dark red. "
        "Dark background, thin orange circular border."
    ),
    "fire_burst": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A single round fireball with 4 jagged flame spikes erupting outward. "
        "Simple silhouette, flat colors, sharp pixel edges. Palette: fiery orange, yellow core, dark red. "
        "Dark background, thin red-orange border."
    ),
    "great_fireball": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A massive fireball engulfing most of the circle, with large flame tongues and ember sparks around it. "
        "Simple bold silhouette, flat pixel clusters. Palette: bright yellow, intense orange, crimson, black. "
        "Dark background, thin orange border."
    ),
    "sear": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A raw steak with a small flame pixel above it, partially cooking. "
        "Simple silhouette, flat colors. Palette: brown, red, orange, yellow. "
        "Dark background, thin warm orange border."
    ),
    "ore_smelt": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A rough iron ore block with a melting golden drip and a small flame below it. "
        "Simple bold pixels, flat shading. Palette: gray, gold, orange, dark red. "
        "Dark background, thin orange border."
    ),
    "plasma_bolt": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A bright white-hot plasma bolt with electric blue and purple arcs around it, leaving a vapor trail. "
        "Simple silhouette, high contrast. Palette: white, cyan, magenta, dark blue. "
        "Dark background, thin white border."
    ),
    "plasma_barrage": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "Five small plasma pellets arranged in a burst, each glowing white with cyan edges. "
        "Simple bold shapes, flat pixel clusters. Palette: white, light blue, purple. "
        "Dark background, thin cyan border."
    ),
    "thermonuclear_pulse": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A blinding white circle in the center with intense orange and red concentric rings expanding outward. "
        "Simple flat pixel art, high contrast. Palette: white, yellow, orange, red. "
        "Dark background, thin white border."
    ),
    "quasar": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A blazing eye-like orb with fiery rings orbiting around it and small pixel debris scattered. "
        "Simple bold silhouette, flat colors. Palette: bright yellow, orange, black, dark red. "
        "Dark background, thin yellow border."
    ),
    # Ice
    "ice_shard": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A sharp diamond-shaped ice crystal pointing right, with a cold mist pixel below it. "
        "Simple silhouette, flat pixel clusters. Palette: ice-blue, white, light cyan. "
        "Dark background, thin cyan border."
    ),
    "ice_prison": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A stylized cage made of 4 ice pillars encasing a trapped dark blue silhouette. "
        "Simple bold shapes, flat colors. Palette: light blue, white, dark blue, gray. "
        "Dark background, thin blue border."
    ),
    "ice_sheet": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A flat horizontal ice patch with a jagged crack and a small sliding footprint. "
        "Simple silhouette, flat pixel art. Palette: pale blue, white, light gray. "
        "Dark background, thin pale blue border."
    ),
    "frost_bastion": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A sturdy ice wall made of 3 connected ice blocks with snow on top, defensive stance. "
        "Simple bold silhouette, flat pixel clusters. Palette: icy blue, white, light gray. "
        "Dark background, thin white border."
    ),
    "cryo_wave": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A crescent-shaped wave of frost and ice crystals spreading outward from the center. "
        "Simple flat pixel art, sharp edges. Palette: cyan, white, light blue. "
        "Dark background, thin cyan border."
    ),
    "hyper_cooling": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A bright blue freezing blast with snowflake pixels and jagged ice spikes radiating outward. "
        "Simple silhouette, high contrast. Palette: deep blue, cyan, white. "
        "Dark background, thin blue border."
    ),
    "absolute_zero": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A solid blue circle with a frozen skull-like face inside, surrounded by a ring of ice crystals. "
        "Simple bold pixels, flat colors. Palette: dark blue, ice blue, white. "
        "Dark background, thin white border."
    ),
    # Air
    "weak_breeze": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A gentle swirl of 3 white wind lines curving to the right, like a soft breeze. "
        "Simple silhouette, flat pixel clusters. Palette: light gray, white, pale blue. "
        "Dark background, thin gray border."
    ),
    "wind_push": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A dense white wind blast with 5 horizontal lines shooting forward, pushing effect. "
        "Simple bold shapes, flat colors. Palette: white, light gray, pale cyan. "
        "Dark background, thin white border."
    ),
    "air_hand": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "An open translucent hand made of swirling wind, reaching forward to grab. "
        "Simple silhouette, flat pixel art. Palette: pale white, light blue, gray. "
        "Dark background, thin pale blue border."
    ),
    "air_shroud": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A circular shield of swirling wind currents wrapping around a small humanoid pixel figure. "
        "Simple bold silhouette, flat colors. Palette: white, pale gray, light cyan. "
        "Dark background, thin white border."
    ),
    "air_form": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A semi-transparent ghost-like figure made of wind and small cloud puffs, floating upward. "
        "Simple silhouette, flat pixel clusters. Palette: white, light gray, pale blue. "
        "Dark background, thin gray border."
    ),
    "air_ionization": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A crackling field of small static sparks and electrical arcs between floating air particles. "
        "Simple bold shapes, flat colors. Palette: electric blue, white, purple. "
        "Dark background, thin blue border."
    ),
    "atmospheric_pressure": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A heavy downward arrow made of compressed air with dense cloud layers above it. "
        "Simple silhouette, high contrast. Palette: dark gray, white, pale blue. "
        "Dark background, thin white border."
    ),
    "breath_bubble": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A round transparent bubble with a small air pocket inside, surrounded by water pixel droplets. "
        "Simple flat pixel art, sharp edges. Palette: pale blue, white, dark blue. "
        "Dark background, thin cyan border."
    ),
    "hurricane_storm": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A swirling spiral of wind and debris with jagged white wind lines and a dark eye in the center. "
        "Simple bold silhouette, flat pixel clusters. Palette: white, gray, dark gray, pale blue. "
        "Dark background, thin white border."
    ),
    "tornado": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A narrow twisting funnel of wind with debris pixels flying at the bottom, touching the ground. "
        "Simple silhouette, flat colors. Palette: white, light gray, brown, gray. "
        "Dark background, thin white border."
    ),
    "sonic_lance": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A straight piercing shockwave with concentric sound rings and a bright white tip, cutting forward. "
        "Simple bold shapes, flat pixel art. Palette: white, pale blue, gray. "
        "Dark background, thin white border."
    ),
    "shockwave": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A powerful radial burst of white rings expanding from the center with a small center impact point. "
        "Simple silhouette, high contrast. Palette: white, light gray, dark gray. "
        "Dark background, thin white border."
    ),
    # Water / steam
    "water_stream": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A straight jet of blue water with 3 trailing droplets, shooting forward. "
        "Simple flat pixel art, bold silhouette. Palette: deep blue, light blue, white. "
        "Dark background, thin blue border."
    ),
    "water_shield": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A curved water barrier with ripples and a splash effect on the surface. "
        "Simple bold shapes, flat colors. Palette: blue, cyan, white. "
        "Dark background, thin cyan border."
    ),
    "water_shroud": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A flowing mantle of water wrapping around a small humanoid figure, with droplets falling off. "
        "Simple silhouette, flat pixel clusters. Palette: ocean blue, light cyan, white. "
        "Dark background, thin blue border."
    ),
    "steam_jet": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A cone of white steam with 3 small cloud puffs and a hint of orange heat at the base. "
        "Simple flat pixel art, sharp edges. Palette: white, light gray, pale orange. "
        "Dark background, thin white border."
    ),
    "steam_veil": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A dense wall of white steam clouds with a faint silhouette of a figure behind it. "
        "Simple bold silhouette, flat colors. Palette: white, light gray, pale blue. "
        "Dark background, thin gray border."
    ),
    "steam_flight": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A figure with two large steam jets coming from the feet, blasting upward with cloud trails. "
        "Simple silhouette, flat pixel clusters. Palette: white, light blue, gray. "
        "Dark background, thin white border."
    ),
    "hydro_slice": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A thin curved blade made of high-pressure water, cutting diagonally with splash particles. "
        "Simple bold shapes, flat pixel art. Palette: bright blue, cyan, white. "
        "Dark background, thin blue border."
    ),
    "mirage": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A distorted wavy square with heat shimmer, a faint palm tree pixel inside, and a small sun above. "
        "Simple silhouette, flat colors. Palette: pale yellow, orange, gray, light blue. "
        "Dark background, thin yellow border."
    ),
    # Not in user table — keep water-school style for the remaining elemental spell
    "water_prison": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A spherical cage of blue water bars trapping a dark silhouette inside. "
        "Simple bold shapes, flat colors. Palette: deep blue, cyan, white, dark blue. "
        "Dark background, thin blue border."
    ),
    # Lightning / mixed
    "lightning_spear": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A jagged zigzag lightning bolt shaped like a spear, striking downward with 2 small branch sparks. "
        "Simple bold silhouette, flat pixel clusters. Palette: bright yellow, electric blue, white. "
        "Dark background, thin yellow border."
    ),
    "ion_storm": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A chaotic field of 6 small spark pixels and ion trails, glowing with electric energy. "
        "Simple flat pixel art, high contrast. Palette: purple, cyan, white, dark blue. "
        "Dark background, thin purple border."
    ),
    "vacuum_cage": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A grid of 6 empty transparent walls with a trapped dark pixel inside, suffocating. "
        "Simple bold silhouette, flat colors. Palette: dark purple, gray, white, pale blue. "
        "Dark background, thin purple border."
    ),
    "meteorological_cataclysm": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A chaotic mix of a red sun, dark storm cloud, rain drops, and lightning bolt all in one circle. "
        "Simple bold shapes, flat pixel art. Palette: red, yellow, dark gray, blue, white. "
        "Dark background, thin white border."
    ),
    "elemental_supremacy": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "Four elemental symbols: a fire flame, a water drop, a wind swirl, and an ice crystal arranged in a diamond. "
        "Simple silhouette, flat pixel clusters. Palette: red, blue, white, cyan, yellow. "
        "Dark background, thin gold border."
    ),
}
