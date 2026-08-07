# Vanilla mob templates (1.21)

Use as **UV layout and proportion** references. Extract textures from the Minecraft client jar when painting.

| Template | Texture (typical) | Use when |
|----------|-------------------|----------|
| **Iron Golem** | 128×128, large head/body/arms/legs | Tall biped, wide torso, hammer fists, boss scale |
| **Zombie / Husk / Drowned** | 64×64 humanoid | Standard biped, simpler than golem |
| **Piglin / Zombified Piglin** | 64×64 | Biped with ears/snout offset (adapt head cube) |
| **Skeleton / Stray** | 64×64 | Thin biped, separate limbs |
| **Warden** | 128×128 | Custom tall non-human, heavy limbs |
| **Blaze** | small multi-part | Floating rods around core |
| **Guardian / Elder** | custom | Single eye, bulky body |
| **Spider / Cave Spider** | 64×64 | 8 legs, segmented body |
| **Wolf / Cat / Fox** | 64×64 | Quadruped horizontal torso |
| **Cow / Pig** | 64×64 | Farm quadruped |
| **Bee / Allay / Vex** | small 64×64 | Tiny flying, wings as extra cubes |
| **Phantom** | 64×64 | Wide wings, flat body |
| **Slime / Magma Cube** | layered sizes | Single cube + inner layer |
| **Shulker** | 64×64 | Box + peek head |
| **Wither / Wither Skeleton** | 64×64 / 64×32 | Multi-head or dark skeleton variant |

## Effecoria mappings (done / planned)

| Creature | Template basis | Notes |
|----------|----------------|-------|
| **Vitrified Golem** | Iron golem–like biped + extra weapon cubes | 128 atlas, GeckoLib, opaque body + cutout blades |
| **Essence Wyvern** | Phantom wings + custom neck/tail/hind legs | **No front legs**; 128 atlas; `scripts/build_essence_wyvern.py` |
| *(future bestiary)* | Pick from table above | Document in `art/.../DESIGN.md` |

## Unusual silhouettes

If no row fits:

1. Decompose into **axis-aligned boxes** (GeckoLib) or **single composite model** (Java).
2. Draw a **paper doll** front/side with cube bounds labeled (WxHxD).
3. Assign one UV island per box; avoid rotating UV islands — rotate **bones** instead.
4. Keep atlas ≤ 256×256 unless many parts; 128×128 is default for one boss.

## Strict turnaround prompts (GenerateImage)

Include in every concept prompt:

- Orthographic projection, no perspective distortion
- Same scale across views, feet on ground line
- T-pose or neutral stance unless lore requires otherwise
- Pixel-art friendly, clear separation of limbs for cube blockout
- List materials from lore verbatim

Example opening:

> Effecoria mod creature concept sheet, orthographic views only: front, back, left, right, top. [CREATURE from lore]. Materials: []. Palette: []. Neutral gray background. No text labels. Pixel-art readable.
