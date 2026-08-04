#!/usr/bin/env python3
"""Exact user visual-action prompts for Organic school spell icons."""
from __future__ import annotations

# id -> full prompt (user-authored)
ORGANIC_PROMPTS: dict[str, str] = {
    # Blood / flesh
    "blood_stasis": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A red blood droplet with a frozen ice crystal inside and 3 small stop symbols around it. "
        "Simple bold silhouette, flat pixel clusters. Palette: dark red, icy blue, white, crimson. "
        "Dark background, thin red border."
    ),
    "bio_strike": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A fleshy red tentacle with sharp bone spikes, striking forward with splatter pixels. "
        "Simple flat pixel art, sharp edges. Palette: crimson, pink, dark red, white. "
        "Dark background, thin red border."
    ),
    "biological_cleaving": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A massive organic cleaver made of chitin and muscle, slicing downward with blood splatter. "
        "Simple bold silhouette, flat colors. Palette: dark red, brown, pink, white. "
        "Dark background, thin crimson border."
    ),
    "muscle_spasm": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A twitching red muscle fiber with jagged electric pain lines and 3 small spasm marks. "
        "Simple silhouette, high contrast. Palette: deep red, dark purple, white, pink. "
        "Dark background, thin red border."
    ),
    "organic_necrosis": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A rotting patch of dead flesh with green-black decay spreading from the center, flaking off. "
        "Simple bold shapes, flat pixel art. Palette: dark green, black, brown, pale red. "
        "Dark background, thin green border."
    ),
    "scorched_earth": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A desolate patch of cracked black earth with small orange embers and 3 wilted plant pixels. "
        "Simple flat pixel art, sharp edges. Palette: black, dark brown, orange, dark gray. "
        "Dark background, thin orange border."
    ),
    "population_control": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A stylized scythe cutting through 4 small green creature silhouettes, thinning them out. "
        "Simple bold silhouette, flat colors. Palette: dark green, gray, red, black. "
        "Dark background, thin green border."
    ),
    # Roots / thorns
    "root_bind": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "4 twisted brown roots growing from the corners, wrapping around a trapped green silhouette in the center. "
        "Simple flat pixel art, sharp edges. Palette: brown, dark green, lime, dark brown. "
        "Dark background, thin brown border."
    ),
    "briar_surge": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A burst of thorny vines with sharp red spikes erupting upward from the ground, trapping a small figure. "
        "Simple bold silhouette, flat pixel clusters. Palette: dark green, brown, red, lime. "
        "Dark background, thin green border."
    ),
    "thorn_lash": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A long whip-like vine covered in 5 sharp thorns, lashing diagonally with motion lines. "
        "Simple silhouette, high contrast. Palette: green, dark brown, pale yellow, red. "
        "Dark background, thin green border."
    ),
    "poison_thorns": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "4 large purple-green poisonous spikes arranged in a cross, with toxic drips and a skull hazard symbol. "
        "Simple bold shapes, flat pixel art. Palette: purple, green, dark green, white. "
        "Dark background, thin purple border."
    ),
    "spore_storm": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A swirling tornado of green and yellow spores, with 7 small mushroom cap pixels in the storm. "
        "Simple flat pixel art, sharp edges. Palette: pale green, yellow, brown, dark green. "
        "Dark background, thin yellow border."
    ),
    "verdant_bloom": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A large glowing flower with 5 petals opening, emitting healing green sparkle pixels. "
        "Simple bold silhouette, flat colors. Palette: bright green, lime, white, pale yellow. "
        "Dark background, thin green border."
    ),
    "verdant_mend": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A green healing wave with 3 small leaf pixels and a glowing plus symbol in the center. "
        "Simple silhouette, flat pixel clusters. Palette: emerald green, white, lime, dark green. "
        "Dark background, thin green border."
    ),
    # Bone
    "bone_needle": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A thin sharp white bone needle pointing diagonally, with a small blood droplet at the tip. "
        "Simple bold silhouette, flat pixel art. Palette: white, pale gray, dark red, beige. "
        "Dark background, thin white border."
    ),
    "bone_spur": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "3 jagged white bone spikes erupting from the ground in a row, with crack lines around them. "
        "Simple flat pixel art, sharp edges. Palette: bone white, gray, beige, dark brown. "
        "Dark background, thin white border."
    ),
    "chitin_plates": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A segmented armor plate pattern of 6 dark brown chitin scales overlapping like beetle shell. "
        "Simple bold silhouette, flat colors. Palette: dark brown, black, pale brown, gray. "
        "Dark background, thin brown border."
    ),
    # Mutations / evolution
    "beast_form": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A humanoid silhouette with wolf-like features: claws, sharp ears, and fur lines, mid-transformation. "
        "Simple bold shapes, flat pixel art. Palette: brown, gray, dark red, white. "
        "Dark background, thin brown border."
    ),
    "full_transformation": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A feral beast silhouette on four legs, with glowing eyes, sharp teeth, and a thick mane of fur. "
        "Simple silhouette, high contrast. Palette: dark brown, black, bright yellow, gray. "
        "Dark background, thin yellow border."
    ),
    "bio_mimicry": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A figure blending into the background with chameleon-like color patches and 3 small camouflage patterns. "
        "Simple flat pixel art, sharp edges. Palette: green, brown, gray, beige. "
        "Dark background, thin green border."
    ),
    "evolutionary_leap": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "3 small creature silhouettes showing evolution stages: simple blob, fish, then humanoid, with upward arrows. "
        "Simple bold silhouette, flat colors. Palette: dark blue, green, brown, white. "
        "Dark background, thin white border."
    ),
    "gene_engineering": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A glowing DNA helix with 2 crossing strands and 4 colored gene segment pixels, plus a syringe. "
        "Simple silhouette, flat pixel clusters. Palette: bright blue, red, green, yellow, white. "
        "Dark background, thin blue border."
    ),
    "genetic_lock": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A padlock made of DNA strands, with 3 broken gene sequences and a cross mark. "
        "Simple bold shapes, flat pixel art. Palette: dark blue, gray, red, white. "
        "Dark background, thin red border."
    ),
    "biological_immortality": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A glowing golden infinity symbol made of cells dividing eternally, with a bright aura. "
        "Simple silhouette, flat colors. Palette: gold, white, bright yellow, pale blue. "
        "Dark background, thin gold border."
    ),
    "biological_singularity": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A condensed dense orb of organic matter with tendrils of life reaching out, pulling everything inward. "
        "Simple flat pixel art, sharp edges. Palette: dark red, pink, green, white. "
        "Dark background, thin white border."
    ),
    "cellular_dominion": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A network of 8 connected cell structures with a glowing nucleus, forming a web of control. "
        "Simple bold silhouette, flat pixel clusters. Palette: blue, green, purple, white. "
        "Dark background, thin blue border."
    ),
    "full_restructuring": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A body silhouette being rebuilt by 6 floating cell clusters, with reconstruction beams and 3 sparkle pixels. "
        "Simple silhouette, high contrast. Palette: green, blue, white, dark red. "
        "Dark background, thin green border."
    ),
    "organism_adaptation": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "4 environment icons around a creature: water drop, fire, air swirl, earth block, all glowing with adaptation. "
        "Simple bold shapes, flat pixel art. Palette: blue, orange, white, green, brown. "
        "Dark background, thin white border."
    ),
    # Healing / support
    "diagnostic_glimpse": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A glowing green eye with 3 floating health bars and status icon pixels around it. "
        "Simple flat pixel art, sharp edges. Palette: bright green, white, dark blue, gray. "
        "Dark background, thin green border."
    ),
    "life_sense": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A radar-like pulse with 5 small red life signal dots and a central glowing heart pixel. "
        "Simple bold silhouette, flat colors. Palette: red, green, white, dark blue. "
        "Dark background, thin red border."
    ),
    "pain_inhibitor": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A medical cross with 3 numbness wave lines radiating outward and a calm face pixel. "
        "Simple silhouette, flat pixel clusters. Palette: white, pale blue, green, gray. "
        "Dark background, thin white border."
    ),
    "sense_sharpening": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "An eye, an ear, and a nose arranged in a triangle, each glowing with golden energy lines. "
        "Simple bold shapes, flat pixel art. Palette: gold, white, brown, dark blue. "
        "Dark background, thin gold border."
    ),
    "soothing_sap": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A green leaf with 3 drops of golden sap falling into a small wound, healing it. "
        "Simple flat pixel art, sharp edges. Palette: bright green, gold, pale yellow, brown. "
        "Dark background, thin green border."
    ),
    "vital_infusion": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A glowing red energy stream flowing from a source into a figure, with a bright heart pulse effect. "
        "Simple bold silhouette, flat colors. Palette: crimson, pink, white, dark red. "
        "Dark background, thin red border."
    ),
    "vital_ward": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A curved organic shield made of living tissue and bone, with a glowing protective membrane. "
        "Simple silhouette, high contrast. Palette: dark green, brown, red, light blue. "
        "Dark background, thin green border."
    ),
    "vitality_pulse": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A radial pulse wave of green and red energy with a stylized heartbeat line through the center. "
        "Simple bold shapes, flat pixel art. Palette: green, red, white, dark blue. "
        "Dark background, thin green border."
    ),
    "limb_regeneration": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A missing limb being regrown with a green glow at the stump, new tissue forming outward. "
        "Simple flat pixel art, sharp edges. Palette: pale pink, green, white, dark red. "
        "Dark background, thin green border."
    ),
    "super_regeneration": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A figure glowing bright green with 6 rapid healing sparkle pixels and 4 regeneration plus signs. "
        "Simple bold silhouette, flat colors. Palette: bright green, white, dark green, yellow. "
        "Dark background, thin green border."
    ),
    "absolute_regeneration": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A blinding white-gold figure with 8 regeneration beams and a phoenix-like rebirth symbol. "
        "Simple silhouette, high contrast. Palette: white, gold, pale blue, green. "
        "Dark background, thin gold border."
    ),
    # Infection / attacks
    "acid_gland": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A swollen green gland with 3 drops of sizzling acid falling, each with a small fizz spark. "
        "Simple bold shapes, flat pixel art. Palette: bright green, yellow, dark green, white. "
        "Dark background, thin green border."
    ),
    "foreign_agent": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A hostile alien-looking cell with 3 tentacles, injecting itself into a healthy red blood cell. "
        "Simple flat pixel art, sharp edges. Palette: purple, pink, bright red, dark blue. "
        "Dark background, thin purple border."
    ),
    "biological_plague": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A green cloud of disease with 6 sick emoji pixels and 3 biohazard symbols scattered around. "
        "Simple bold silhouette, flat colors. Palette: sick green, dark green, yellow, black. "
        "Dark background, thin green border."
    ),
    "parasitic_infection": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A host figure with 3 parasitic worms burrowing out, and a small parasite egg cluster nearby. "
        "Simple silhouette, flat pixel clusters. Palette: dark red, pale pink, green, black. "
        "Dark background, thin red border."
    ),
    "immune_suppression": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A shield broken in half, with 4 small white blood cell pixels being suppressed by purple rays. "
        "Simple bold shapes, flat pixel art. Palette: white, purple, red, dark blue. "
        "Dark background, thin purple border."
    ),
    "metabolic_shock": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A stylized metabolism meter with 3 jagged spikes, a sick figure collapsing, and shockwave lines. "
        "Simple flat pixel art, sharp edges. Palette: orange, red, black, white. "
        "Dark background, thin red border."
    ),
    "bio_cataclysm": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A massive explosion of flesh and organic matter, with body parts, roots, and blood flying in all directions. "
        "Simple bold silhouette, flat colors. Palette: red, green, brown, pink, black. "
        "Dark background, thin red border."
    ),
    "bio_fission": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A single cell splitting into two identical cells, with a bright flash of energy in the center. "
        "Simple silhouette, high contrast. Palette: blue, green, purple, white. "
        "Dark background, thin blue border."
    ),
    "biological_field": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A large aura of green organic energy with floating life forms and biological particles. "
        "Simple bold shapes, flat pixel art. Palette: emerald green, pale green, white, dark blue. "
        "Dark background, thin green border."
    ),
    # Symbiosis / adaptation
    "symbiotic_graft": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "Two different organism halves joined together with a glowing graft line, sharing resources. "
        "Simple flat pixel art, sharp edges. Palette: green, brown, blue, gold. "
        "Dark background, thin gold border."
    ),
    "living_armor": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A full set of organic plate armor with breathing holes, made of chitin and hardened resin. "
        "Simple bold silhouette, flat colors. Palette: dark brown, dark green, beige, gray. "
        "Dark background, thin brown border."
    ),
    "life_creation": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "Two hands cupping a bright glowing orb of light, with a small creature silhouette forming inside. "
        "Simple silhouette, flat pixel clusters. Palette: bright gold, white, pale blue, green. "
        "Dark background, thin gold border."
    ),
    "metabolic_boost": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "A speedometer-like gauge with 5 metabolism arrows pointing up, and a glowing figure running. "
        "Simple bold shapes, flat pixel art. Palette: orange, red, yellow, white. "
        "Dark background, thin orange border."
    ),
    "adrenal_gift": (
        "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
        "An adrenal gland with 3 lightning bolts of energy, transferring to a figure with glowing speed lines. "
        "Simple flat pixel art, sharp edges. Palette: bright red, yellow, white, dark blue. "
        "Dark background, thin red border."
    ),
}
