# Mortar atlas UV (16×16) — lantern-scale bowl

Same idea as vanilla `lantern.png`: one sheet, several UV islands.

```
 0        6   8        14  16
 +--------+---+--------+--+ 0
 | SIDE   |   | INNER  |  |     rows 0–6  (height of bowl wall)
 | N/S/E/W|   | floor  |  |
 | 6×6    |   | 6×6    |  |
 +--------+---+--------+--+ 7
 | RIM TOP 6×6| PESTLE |  |     row 7–12
 |            | 2×6    |  |
 +------------+--------+--+ 13
 | BOTTOM 6×6 | unused |  |     row 13–15
 +------------+--------+--+ 16
```

| Region | UV [u0,v0 → u1,v1] | Used by model faces |
|--------|--------------------|---------------------|
| Side | 0,0 → 6,6 | Outer N/S/E/W of bowl |
| Inner | 8,0 → 14,6 | Inner floor + inward wall faces |
| Rim | 0,7 → 6,13 | Top of wall rim |
| Pestle | 8,7 → 10,13 | Thin pestle stick |
| Bottom | 0,13 → 6,16 | Underside of bowl |

Paint opaque pixels only where the model samples; leave unused cells transparent or dark grey.
