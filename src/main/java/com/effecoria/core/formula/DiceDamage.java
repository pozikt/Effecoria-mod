package com.effecoria.core.formula;

import com.google.gson.JsonObject;

import net.minecraft.util.Mth;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts D&amp;D-style dice notation from spell JSON into Minecraft hit-point damage.
 *
 * <pre>
 * expected = n × (faces + 1) / 2  (+ optional bonus / alt dice)
 * mcBase   = expected × 0.5
 * final    = mcBase × (0.75 + power / 100)
 * </pre>
 */
public final class DiceDamage {
    private static final Pattern DICE = Pattern.compile("(\\d+)d(\\d+)", Pattern.CASE_INSENSITIVE);

    private DiceDamage() {}

    /** Expected value of a single expression like {@code 3d8} or {@code 1d4+1d4}. */
    public static float expected(String expression) {
        if (expression == null || expression.isBlank()) {
            return 0f;
        }
        float total = 0f;
        Matcher matcher = DICE.matcher(expression.toLowerCase(Locale.ROOT));
        boolean found = false;
        while (matcher.find()) {
            found = true;
            int n = Integer.parseInt(matcher.group(1));
            int faces = Integer.parseInt(matcher.group(2));
            total += n * (faces + 1) / 2f;
        }
        return found ? total : 0f;
    }

    public static float toMinecraftBase(float expected) {
        return expected * 0.5f;
    }

    public static float scaleWithPower(float mcBase, float power) {
        return mcBase * (0.75f + power / 100f);
    }

    public static float fromDice(String expression, float power) {
        return scaleWithPower(toMinecraftBase(expected(expression)), power);
    }

    /**
     * Prefers {@code damage_dice} (+ optional {@code damage_dice_alt}, {@code damage_bonus});
     * falls back to legacy flat {@code damage} × power/50.
     */
    public static float fromParams(JsonObject params, float power, float legacyDefault) {
        float diceExpected = 0f;
        if (params.has("damage_dice")) {
            diceExpected += expected(params.get("damage_dice").getAsString());
        }
        if (params.has("damage_dice_alt")) {
            diceExpected += expected(params.get("damage_dice_alt").getAsString());
        }
        if (params.has("damage_bonus")) {
            diceExpected += params.get("damage_bonus").getAsFloat();
        }
        if (diceExpected > 0f) {
            return clampDamage(scaleWithPower(toMinecraftBase(diceExpected), power));
        }
        float flat = params.has("damage") ? params.get("damage").getAsFloat() : legacyDefault;
        return clampDamage(flat * (power / 50f));
    }

    /**
     * Per-second DoT. Dice keyed as per D&amp;D round (~6s), converted to MC / second.
     */
    public static float perSecondFromParams(JsonObject params, float power, float legacyDefault) {
        if (params.has("damage_dice_per_round")) {
            float perRound = fromDice(params.get("damage_dice_per_round").getAsString(), power);
            return Math.max(0.05f, perRound / 6f);
        }
        if (params.has("damage_per_second")) {
            return params.get("damage_per_second").getAsFloat() * (0.75f + power / 100f);
        }
        return legacyDefault * (0.75f + power / 100f);
    }

    public static float clampDamage(float damage) {
        return Mth.clamp(damage, 0.25f, 80f);
    }
}
