#!/usr/bin/env python3
"""Visual-action pixel prompts for Effecoria spell icons (64x64, round frame)."""
from __future__ import annotations

from elemental_icon_prompts import ELEMENTAL_PROMPTS

PREFIX = (
    "Pixel art, minecraft mod icon, 64x64 pixels, round frame. "
    "Simple bold silhouette, flat colors, sharp pixel edges, high contrast, "
    "no text, no letters, no anti-aliasing, no gradients. Dark background"
)

# Exact user examples (id -> full prompt)
EXAMPLE_PROMPTS: dict[str, str] = {
    "void_lance": (
        f"{PREFIX}, thin white circular border. "
        "A cracked purple void spear pointing diagonally down, surrounded by 3 white star-like spark pixels. "
        "Palette: dark violet, magenta, white."
    ),
    "festering_wound": (
        f"{PREFIX}, thin dark green circular border. "
        "An open red wound with 3 green dripping drops of pus below it. "
        "Simple silhouette, flat pixel clusters. Palette: dark red, sickly green, black."
    ),
    "cliff_urge": (
        f"{PREFIX}, thin blue circular border. "
        "A stylized yellow eye with a spiral inside, representing hypnotic pull. "
        "Simple bold pixels, flat shading. Palette: pale yellow, dark blue, white."
    ),
    "raise_skeleton": (
        f"{PREFIX}, thin white border. "
        "A stylized white skull with 2 crossbones behind it. "
        "Simple bold silhouette, flat pixel art, sharp edges. Palette: bone-white, gray, black."
    ),
    "root_bind": (
        f"{PREFIX}, thin green border. "
        "3 twisted brown roots forming a cage around a small green trapped pixel. "
        "Simple shapes, high contrast. Palette: brown, dark green, lime."
    ),
    "psi_ward": (
        f"{PREFIX}, thin cyan circular border. "
        "A cyan shield with a glowing magical plus sign in the center. "
        "Simple bold silhouette, flat pixel clusters. Palette: cyan, blue, white."
    ),
}

# school -> (border phrase, palette phrase)
SCHOOL_STYLE: dict[str, tuple[str, str]] = {
    "spatial": ("thin white circular border", "dark violet, magenta, white"),
    "corruption": ("thin dark green circular border", "dark red, sickly green, black"),
    "mental": ("thin blue circular border", "pale yellow, lavender, dark blue, white"),
    "elemental": ("thin orange circular border", "bright orange, yellow, dark red"),
    "necromancy": ("thin white circular border", "bone-white, gray, black"),
    "organic": ("thin green circular border", "brown, dark green, lime"),
    "common": ("thin cyan circular border", "cyan, blue, white"),
    "seals": ("thin gold circular border", "gold, amber, black"),
}

# Visual action scenes (not spell names) — id -> scene sentence
VISUAL_SCENES: dict[str, str] = {
    # common
    "psi_adrenaline": "A red lightning bolt through a small white circle, body-surge spark",
    "phi_glow": "An open hand with a cold cyan light orb hovering above the palm",
    "psi_charge": "A crystal flask being filled with rising cyan energy sparks",
    "psi_link": "Two small head silhouettes linked by a glowing cyan thread",
    "psi_ward": "A cyan shield with a glowing magical plus sign in the center",
    # examples already covered above
    "void_lance": "A cracked purple void spear pointing diagonally down, 3 white spark pixels",
    "festering_wound": "An open red wound with 3 green dripping drops of pus below it",
    "cliff_urge": "A stylized yellow eye with a spiral inside, hypnotic pull",
    "great_fireball": "A large orange fireball with 4 jagged flame pixels on top",
    "raise_skeleton": "A stylized white skull with 2 crossbones behind it",
    "root_bind": "3 twisted brown roots forming a cage around a small green trapped pixel",
    # elemental fire/ice/water/air samples
    "fire_burst": "A compact orange fireball with yellow core and 2 ember sparks",
    "ember_volley": "Three small orange ember projectiles flying diagonally up-right",
    "sear": "A skillet silhouette with a tiny flame under it",
    "ore_smelt": "A brown ore chunk with orange molten cracks",
    "ice_shard": "A sharp cyan ice crystal shard pointing up-right",
    "frost_bastion": "Three vertical ice pillars of different heights",
    "cryo_wave": "A crescent frost wave sweeping left to right",
    "water_stream": "A blue water jet blasting from left to right with foam pixels",
    "hydro_slice": "A crescent blue water blade slash",
    "wind_push": "Three white wind gust arcs pushing right",
    "tornado": "A grey-blue spinning funnel of wind",
    "plasma_bolt": "A violet-cyan plasma bolt streaking diagonally",
    "steam_jet": "A cone of grey-white steam with orange heat tint",
    "lightning_spear": "A jagged yellow-white lightning zigzag spear",
    # mental
    "mental_push": "Two concentric rings with force ticks, mind shove",
    "mental_sting": "A small purple psychic shard stabbing diagonally",
    "sense_phi": "An open eye scanning with soft cyan iris",
    "mind_lance": "A purple mind spear with fog trail",
    "psychic_focus": "A floating focus orb above a simple head outline",
    "mind_bolt": "A purple fog bolt with white core",
    "psychic_scream": "Concentric scream rings from a tiny mouth silhouette",
    "neural_lock": "A padlock over a crossed nerve line",
    "mind_terror": "A dark face silhouette with glowing red eyes of fear",
    "drown_urge": "A drowning head under a blue wave",
    "psychic_frenzy": "A jagged red rage skull spark",
    "mass_confusion": "Three swirling purple fog blobs",
    "mass_hysteria": "Many tiny panicked face dots in a circle",
    "telekinetic_crush": "A fist silhouette crushing a ring of force",
    "psychic_barrier": "A white-noise shield hexagon",
    "mind_probe": "An eye with a downward probe needle",
    "locus_echo": "A floating memory page beside a head silhouette",
    "mind_illusion": "A cracked mirror reflecting a false landscape",
    "mind_dominate": "Puppet strings over a tiny figure",
    "false_memory": "An open book with a crossed-out page",
    "dream_lock": "A sleeping face with lock over eyes",
    "hive_mind": "Several linked head nodes in a network",
    "psi_echo": "A hollow duplicate silhouette of a person",
    "total_veil": "A draped veil covering a landscape silhouette",
    # organic
    "vitality_pulse": "Two overlapping cells, red and pale green",
    "thorn_lash": "Spikes erupting from a ground line",
    "briar_surge": "Thorny green vines with purple buds",
    "verdant_mend": "A green heart with leaf veins",
    "diagnostic_glimpse": "A crosshair over a tiny body outline",
    "blood_stasis": "A red droplet frozen mid-drip",
    "life_sense": "Radar rings detecting a green life blip",
    "bio_strike": "A red muscle fist punch",
    "bone_needle": "A white bone needle projectile",
    "chitin_plates": "Layered brown chitin armor plates",
    "poison_thorns": "Green thorns dripping purple toxin",
    # necromancy
    "soul_drain": "A teal soul teardrop being siphoned into a dark hole",
    "wither_touch": "A black hand with sickly green fingertips",
    "shade_summon": "A purple shade silhouette rising from ground",
    "death_mark": "A skull mark with a targeting cross",
    "death_coil": "A spiraling green-black death coil",
    "bone_armor": "Ribcage armor silhouette",
    "army_of_dead": "Three small skulls in a row",
    # spatial
    "blink": "Two dots connected by a short teleport dash",
    "rift_yank": "A jagged spatial rift pulling a pixel inward",
    "phase_veil": "A translucent figure half-faded",
    "gravity_well": "Concentric rings sucking inward",
    "warp_bolt": "A blue warp projectile with star sparks",
    "far_blink": "A longer teleport dash with destination spark",
    # corruption
    "corrupt_mark": "A magenta crosshair brand with one green pixel",
    "blight_pulse": "Three expanding sickly green rings",
    "plague_bolt": "A green plague bolt with drip",
    "miasma_cloak": "Purple-green miasma cloud swirls",
    "binding_seal": "Dark green binding chains wrapping a brand seal",
    "blight_brand": "A sickly green brand stamp burning into flesh",
    "blight_field": "A circular field of rotting green spores",
    "blight_surge": "A surge wave of magenta-green blight",
    "decay_bind": "Rotting ropes binding a silhouette",
    "entropy_aegis": "A cracked shield leaking green entropy sparks",
    "entropy_lash": "A whip of black-green entropy cracking right",
    "omega_blight": "A huge blight skull wreathed in green flame",
    "pestilence_wave": "A horizontal wave of crawling pest pixels",
    "plague_crown": "A crown of green pustules over a dark brow",
    "rot_touch": "A black fingertip leaving green rot trails",
    "tainted_leech": "A leech silhouette sucking a red life drop",
    "virulent_wave": "Three virulent green arcs sweeping outward",
    # elemental more
    "absolute_zero": "A blue-white freeze burst crystallizing everything",
    "air_form": "A swirling humanoid outline made of wind streaks",
    "air_hand": "A translucent hand of compressed air gripping",
    "air_ionization": "Zigzag sparks ionizing a blue air column",
    "air_shroud": "A cloak of wind arcs wrapping a figure",
    "atmospheric_pressure": "Heavy pressure arrows crushing downward",
    "breath_bubble": "A round air bubble around a tiny head",
    "elemental_supremacy": "Four elemental orbs orbiting a bright core",
    "hurricane_storm": "A massive spiral storm with rain ticks",
    "hyper_cooling": "Frost rapidly coating a metal bar",
    "ice_prison": "An icy cage of vertical crystal bars",
    "ice_sheet": "A flat sheet of cracked ice spreading",
    "ion_storm": "Purple ion lightning raining from a cloud",
    "meteorological_cataclysm": "Storm, fire and ice colliding in one burst",
    "mirage": "A wavy heat-mirage silhouette doubling",
    "plasma_barrage": "Three violet plasma bolts fanning out",
    "quasar": "A bright core jetting twin energy beams",
    "shockwave": "Concentric force rings expanding from center",
    "sonic_lance": "A sharp sonic cone blasting right",
    "steam_flight": "Steam jets under a rising silhouette",
    "steam_veil": "A soft grey steam curtain",
    "thermonuclear_pulse": "A white-orange nuclear flash ring",
    "vacuum_cage": "An empty ring sucking air inward",
    "water_prison": "Water bars forming a spherical prison",
    "water_shield": "A curved blue water barrier",
    "water_shroud": "Cascading water sheets as a cloak",
    "weak_breeze": "Two soft wind ticks drifting right",
    # mental more
    "mental_fortress": "A brick fortress silhouette over a head outline",
    "omega_mind": "An enormous eye radiating purple thought rings",
    "psychic_amplify": "A signal triangle amplifying outward waves",
    "psychic_drain": "Purple energy siphoned from a head into a void",
    "psychic_storm": "Chaotic purple storm clouds over a mind",
    "synaptic_overload": "Crackling nerve web exploding with sparks",
    "thought_bomb": "A thought bubble about to burst with cracks",
    "thought_lance": "A thin purple thought spear thrusting",
    # necromancy more
    "bone_chill": "Frost forming on white bone knuckles",
    "bone_volley": "Three bone shards flying up-right",
    "corpse_burst": "A body outline exploding into bone bits",
    "curse_of_frailty": "A cracked heart under a dark curse mark",
    "dark_pact": "Two hands shaking over a black flame",
    "death_apotheosis": "A crowned skull rising in green flame",
    "death_gate": "A dark arched gate with soul wisps",
    "death_sense": "A skull radar ping detecting a life blip",
    "death_shadow": "A long shadow cast by a skull figure",
    "grave_bind": "Dirt hands pulling ankles into soil",
    "grave_field": "Tombstone silhouettes in green mist",
    "grave_leech": "A grave hole sucking a teal soul drop",
    "grave_whisper": "Skull whispering green sound waves",
    "haunting_visage": "A ghostly face with hollow glowing eyes",
    "lich_ascension": "A lich crown floating above a skull",
    "lich_ward": "Bone ward glyph shielding a skull",
    "life_tap": "A red life drop draining into black",
    "necrotic_aura": "Green necrotic rings around a figure",
    "necrotic_bolt": "A green-black necrotic bolt streak",
    "phantom_step": "A ghost footprint fading mid-dash",
    "phylactery_surge": "A gem phylactery pulsing teal light",
    "raise_zombie": "A rotting hand rising from cracked earth",
    "shade_brood": "Three small shade silhouettes clustered",
    "shade_swarm": "Many shade dots swirling as a swarm",
    "siphon_pulse": "A pulse ring siphoning teal energy inward",
    "soul_anchor": "A teal soul chained to an iron anchor",
    "soul_cataclysm": "Souls exploding outward from a dark core",
    "soul_reaper": "A scythe silhouette cutting a soul trail",
    "soul_shackle": "Teal soul wrapped in black shackles",
    "wither_wave": "A horizontal wither wave of black ash",
    # organic more
    "absolute_regeneration": "Flesh knitting closed with green sparkles",
    "acid_gland": "A gland sac dripping green acid drops",
    "adrenal_gift": "A heart pumping with red surge sparks",
    "beast_form": "A human silhouette morphing into a beast snout",
    "bio_cataclysm": "Cells erupting in a green-red explosion",
    "bio_fission": "One cell splitting into two",
    "bio_mimicry": "A camouflage silhouette matching a leaf",
    "biological_cleaving": "A cleaving slash through tissue layers",
    "biological_field": "A green bio-aura field circle",
    "biological_immortality": "An infinity loop made of cell strands",
    "biological_plague": "Spreading infection dots across a body map",
    "biological_singularity": "A dense green life core collapsing inward",
    "bone_spur": "A sharp bone spur erupting from skin",
    "cellular_dominion": "A crown over a controlled cell cluster",
    "evolutionary_leap": "A ladder of forms jumping upward",
    "foreign_agent": "A foreign cell invading a tissue grid",
    "full_restructuring": "Bones and muscle rearranging mid-form",
    "full_transformation": "A figure mid-metamorphosis into another shape",
    "genetic_lock": "A DNA helix locked with a padlock",
    "immune_suppression": "Immune shields fading with X marks",
    "life_creation": "A tiny embryo spark in green light",
    "limb_regeneration": "A regenerating arm bud with leaf veins",
    "living_armor": "Living bark plates covering a torso",
    "metabolic_boost": "A flame icon over a speeding heartbeat",
    "metabolic_shock": "A heart jolted by red-green shock lines",
    "muscle_spasm": "Twitching muscle fibers with jagged lines",
    "organic_necrosis": "Healthy green tissue turning black rot",
    "organism_adaptation": "A creature silhouette shifting traits",
    "pain_inhibitor": "A nerve line with a mute slash across it",
    "parasitic_infection": "Tiny parasites nesting in a body outline",
    "population_control": "Many dots with one larger controlling node",
    "scorched_earth": "Burned ground with dead plant stubs",
    "sense_sharpening": "An eye with sharpened focus ticks",
    "soothing_sap": "Amber sap dripping onto a wound",
    "spore_storm": "A cloud of green spores swirling",
    "super_regeneration": "Rapid healing rings over a cut",
    "symbiotic_graft": "Two tissues grafting together at a seam",
    "verdant_bloom": "A flower blooming from cracked soil",
    "vital_infusion": "Green vital fluid injecting into veins",
    "vital_ward": "A green organic shield of leaves",
    # seals
    "anchor_fortify": "A gold anchor stamp reinforcing a plate",
    "beacon_seal": "A glowing gold beacon pillar seal",
    "fortify_seal": "A gold fortify rune on a shield plate",
    "glow_seal": "A bright gold glow rune emitting light rays",
    "omega_ward": "A large gold omega ward glyph",
    "permanent_glow": "A lasting gold glow orb with seal ring",
    "repulsion_seal": "Gold arrows radiating from a seal center",
    "shock_glyph": "A gold lightning glyph etched in stone",
    "shock_trap": "A gold trap plate with lightning spark",
    "snare_glyph": "A gold snare rune with looped cords",
    "snare_matrix": "A grid of gold snare nodes linked",
    "trap_seal": "A gold hinged trap seal on the ground",
    "ward_glyph": "A gold protective ward glyph",
    # spatial more
    "absolute_fold": "Space folding like paper into a singularity",
    "dimensional_anchor": "A spatial hook pinning a rift shut",
    "fold_repulse": "Folded space snapping outward in a blast",
    "gravity_snare": "Gravity rings trapping a pixel in place",
    "rift_burst": "A purple rift exploding star sparks",
    "rift_slash": "A diagonal rift tear cutting space",
    "spatial_singularity": "A tiny black hole pulling star pixels",
    "spatial_surge": "A surge of warped space ripples",
    "spatial_ward": "A hexagonal ward against rift tears",
    "void_step": "A footstep vanishing into purple void",
    "warp_exchange": "Two positions swapping with warp trails",
}


def build_prompt(spell_id: str, school: str, scene: str | None = None) -> str:
    if spell_id in EXAMPLE_PROMPTS:
        return EXAMPLE_PROMPTS[spell_id]
    if spell_id in ELEMENTAL_PROMPTS:
        return ELEMENTAL_PROMPTS[spell_id]
    border, palette = SCHOOL_STYLE.get(school, ("thin white circular border", "white, gray, black"))
    action = scene or VISUAL_SCENES.get(spell_id) or f"A simple iconic symbol for {spell_id.replace('_', ' ')}"
    return (
        f"{PREFIX}, {border}. {action}. "
        f"Simple bold silhouette, flat pixel clusters. Palette: {palette}."
    )


def prompt_for(spell_id: str, school: str) -> str:
    return build_prompt(spell_id, school)
