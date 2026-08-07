# Essence Wyvern — SEGMENT_LOCK (from user contour)

Source of truth: `art/essence_wyvern/concept_turnaround_contur.png`  
(human sketch of how to slice the turnaround — not a perfect tracing, but the method).

## Overlay legend (user pens)

| Color | Meaning |
|-------|---------|
| Red | Head volumes: skull / snout / jaw boxes |
| Lime | Individual cranial horns / frill spikes |
| Blue | Horizontal body rings: neck → torso → tail |
| Magenta | Leg volumes + each claw toe |

## Palette (concept — NO purple)

| Role | RGB |
|------|-----|
| Stone mid | `120,118,112` |
| Stone dark | `56,48,40` |
| Beige belly ribs | `104,88,72` |
| Claw | `28,24,18` |
| Gold tip / wing vein | `220,175,45` … `255,230,110` |
| Eye | yellow iris + sharp black slit pupil |

## Bone inventory (game bake)

Concept has ~12 blue rings on front (neck+torso) and ~15–20 on back (neck+tail).  
Minecraft bake keeps **counts & seams**, compresses only where bones would be too tiny:

| Group | Bones | Notes |
|-------|-------|-------|
| Head | `head`, `jaw` | Red boxes |
| Horns | `horn_0`…`horn_4` | Lime; parent `head` |
| Neck | `neck_0`…`neck_5` | 6 blue rings (front ~4 compressed with side density) |
| Torso | `body_0`, `body_1`, `body_2` | Chest → mid → hips |
| Crest | cube on each neck/body/tail bone | Gold tip |
| Wings | `left/right_wing` → `_mid` → `_tip` | Folded membranes |
| Leg | `*_leg` → `*_knee` → `*_shin` → `*_foot` | Magenta stacks |
| Claws | 4 cubes on each foot | 3 forward + dewclaw |
| Tail | `tail_0`…`tail_9` | Tapering blue rings |

## Method reminder

1. Read contour + concept.  
2. Lock this table.  
3. Build stacked ring cubes (not one chicken stub).  
4. Bake gray-beige atlas.  
5. User playtests.
