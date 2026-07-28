# Formulas

Lore equations from [Effecoria encyclopedia](https://github.com/pozikt/Effecoria) and their **in-game discretization**.

Implementation: `com.effecoria.core.formula.FormulaEngine`  
Tunable coefficients: `config/effecoria-common.toml` (via `BalanceConfig`)

---

## Ψ conversion (living mage)

**Lore:**

$$E_{\Psi} = \Psi_{soul} \times \int_{t_0}^{t_1} \left[ \Phi_{nature}(x^\mu) \cdot Q_{biology} \right] dt$$

**Game (per tick):**

```
ΔE_Ψ = Ψ_soul × Φ_local × Q_biology × Δt × psi_regen_scale
```

| Symbol | Source |
|--------|--------|
| Ψ_soul | Race + progression |
| Φ_local | PhiField at player position |
| Q_biology | Orkanum: hunger, oxygen, race |
| Δt | Usually 1 game tick |

---

## Lich conversion

**Lore:** \(Q_{biology} = 0\), use Φ_phyl (phylactery efficiency)

```
ΔE_Ψ = Ψ_soul × Φ_local × Φ_phyl × Δt × psi_regen_scale
```

---

## Spell power (individual cast)

**Lore:** coherent conversion through Ψ-operator

```
power = Ψ_current × Φ_local × resonance(ω_op, ω_spell) × spell_multiplier × spell_power_scale
```

**Resonance:**

```
resonance = max(0, 1 - |ω_op - ω_spell| / resonance_width_hz)
```

---

## Spell cost

```
cost = base_cost × (1 + low_phi_cost_factor × (1 - min(1, Φ_local)))
```

---

## Technomagic (phase 5)

**Lore:**

$$Effect \propto \Phi_{reactor} \cdot C_{circuit} \cdot K_{rune}$$

**Game:**

```
power = Φ_reactor × C_circuit × K_rune × techno_power_scale
```

---

## Entropy / b-component (backlash)

**Lore:** imaginary part of Ψ accumulates → Ω bleed

```
b_new = b_old + side_entropy × power_used × entropy_scale
```

When `b_new >= entropy_threshold` → backlash event (damage, debuff, spawn particles).

---

## Zero-Φ zones

When `Φ = 0` (lead chamber, ZNΦ):

- `regenPsi` returns 0
- `spellPower` returns 0
- `canCast` returns false

---

## Spectral purity (one magic type)

Attempting to cast a spell whose `school` ≠ player's `MagicAffinity`:

- Blocked in `FormulaEngine.canCast()`
- Future: second-school attempt triggers `PsiCollapseEvent` (lethal)

Nominal frequencies (Hz):

| School | Hz |
|--------|-----|
| Mental | 7.8 |
| Elemental | 22.3 |
| Organic | ~5 |
| Necromancy | 55 |
| Spatial | 120 |

---

## Changing balance

Edit `config/effecoria-common.toml` in the world save or default config folder.  
Do **not** hardcode numbers in spell JSON unless they are spell-specific (`base_cost`, `side_entropy`).
