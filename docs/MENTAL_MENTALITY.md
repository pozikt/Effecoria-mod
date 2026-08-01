# Mental mentality

Offensive mental spells check {@link com.effecoria.effect.mental.MentalityService} before applying
debuffs or compulsions.

| Kind | Who | Rule |
|------|-----|------|
| **BEAST** | Animals, fish, spiders, creepers, slimes, ghasts, guardians, ambient fauna | Always afflicted; never break free |
| **HUMANOID** | Undead, villagers, illagers/raiders, piglins, hoglins, endermen, witches, players | Can **resist** on cast and **break out** over time |
| **CONSTRUCT** | Iron/snow golems, shulkers, vex, allay, blaze, warden, wither, dragon, necro thralls | Full immunity |

## Humanoid will

Will scales with max HP (`maxHealth / 20`, clamped). Higher caster breathing mastery lowers resist and breakout chances.

Breakout is rolled about once per second while afflicted; success clears compulsion and common mental potion effects.
