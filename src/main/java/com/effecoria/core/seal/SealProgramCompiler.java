package com.effecoria.core.seal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Compiles a linear word sequence into runtime params on a {@link SealInstance}.
 *
 * <p>Grammar (left → right):
 * <ul>
 *   <li>Passive: {@code ACTION NUMBER? MODIFIER*} — always on (e.g. Hardness Five)</li>
 *   <li>Reactive: {@code SENSE SPEC* ACTION NUMBER? MODIFIER* (TIME NUMBER)?}</li>
 *   <li>Control: {@code COUNT|LOOP|UNTIL NUMBER}, {@code IF|WHEN} as sense opener, {@code ELSE} alt branch</li>
 * </ul>
 */
public final class SealProgramCompiler {
    public static final String TOKENS_TAG = "tokens";
    public static final String VERSION_TAG = "program_version";
    public static final int PROGRAM_VERSION = 3;

    public static final String PASSIVES_TAG = "passives";
    public static final String RULES_TAG = "rules";
    public static final String RUNTIME_TAG = "_rt";

    public static final String EFFECT = "e";
    public static final String MAGNITUDE = "m";
    public static final String SOUND_EVENT = "sound_event";
    public static final String SOUND_VOLUME = "sound_vol";
    public static final String DURATION_TICKS = "dur";
    public static final String SENSE = "sense";
    public static final String SPECS = "specs";
    public static final String RADIUS = "radius";
    public static final String ACTIONS = "actions";
    public static final String ELSE_ACTIONS = "else_actions";
    public static final String COUNT_NEED = "count_need";
    public static final String UNTIL_MAX = "until_max";
    public static final String LOOP_REPEATS = "loop";

    /** @deprecated v1 flat keys — still read for migration of old inscriptions */
    @Deprecated public static final String HARDNESS_MULT = "hardness_mult";
    /** @deprecated v1 */
    @Deprecated public static final String HURT_DAMAGE = "hurt_damage";
    /** @deprecated v1 */
    @Deprecated public static final String SLOW_AMP = "slow_amp";
    /** @deprecated v1 */
    @Deprecated public static final String PUSH_FORCE = "push_force";
    /** @deprecated v1 */
    @Deprecated public static final String GLOW_LEVEL = "glow_level";
    /** @deprecated v1 */
    @Deprecated public static final String SOUND_PERIOD = "sound_period";
    /** @deprecated v1 */
    @Deprecated public static final String SEE_RADIUS = "see_radius";

    private SealProgramCompiler() {}

    public record CompileResult(CompoundTag params, float totalPsiCost, List<String> errors) {
        public boolean ok() {
            return errors.isEmpty();
        }
    }

    public static int maxTokens(float breathingMasteryRatio) {
        return Mth.clamp(8 + Math.round(breathingMasteryRatio * 8f), 8, 16);
    }

    public static CompileResult compile(List<ResourceLocation> tokenIds) {
        List<String> errors = new ArrayList<>();
        if (tokenIds == null || tokenIds.isEmpty()) {
            errors.add("empty");
            return new CompileResult(new CompoundTag(), 0f, errors);
        }

        List<SealWordDefinition> words = new ArrayList<>();
        float cost = 0f;
        for (ResourceLocation id : tokenIds) {
            Optional<SealWordDefinition> opt = SealWordRegistry.get(id);
            if (opt.isEmpty()) {
                errors.add("unknown:" + id);
                continue;
            }
            SealWordDefinition word = opt.get();
            words.add(word);
            cost += word.psiCost();
        }
        if (!errors.isEmpty()) {
            return new CompileResult(new CompoundTag(), cost, errors);
        }

        CompoundTag params = new CompoundTag();
        params.putInt(VERSION_TAG, PROGRAM_VERSION);
        ListTag tokens = new ListTag();
        for (ResourceLocation id : tokenIds) {
            tokens.add(StringTag.valueOf(id.toString()));
        }
        params.put(TOKENS_TAG, tokens);

        ListTag passives = new ListTag();
        ListTag rules = new ListTag();

        CompoundTag openRule = null;
        CompoundTag openAction = null;
        boolean elseBranch = false;
        int pendingLoop = 0;
        enum ExpectNum { NONE, LOOP, COUNT, UNTIL }
        ExpectNum expectNum = ExpectNum.NONE;
        enum Phase { IDLE, SENSE, ACTION, TIME }
        Phase phase = Phase.IDLE;

        for (SealWordDefinition word : words) {
            switch (word.kind()) {
                case CONTROL -> {
                    String ctrl = controlKey(word);
                    switch (ctrl) {
                        case "loop", "repeat" -> expectNum = ExpectNum.LOOP;
                        case "count" -> {
                            if (openRule == null) {
                                errors.add("count_without_sense:" + word.id());
                            } else {
                                expectNum = ExpectNum.COUNT;
                            }
                        }
                        case "until" -> {
                            if (openRule == null) {
                                errors.add("until_without_sense:" + word.id());
                            } else {
                                expectNum = ExpectNum.UNTIL;
                            }
                        }
                        case "if", "when" -> {
                            openRule = newRule("see");
                            openAction = null;
                            elseBranch = false;
                            phase = Phase.SENSE;
                            rules.add(openRule);
                        }
                        case "else" -> {
                            if (openRule == null) {
                                errors.add("else_without_rule:" + word.id());
                            } else {
                                elseBranch = true;
                                openAction = null;
                                phase = Phase.ACTION;
                            }
                        }
                        default -> errors.add("unknown_control:" + word.id());
                    }
                }
                case SENSE -> {
                    openRule = newRule(word);
                    openAction = null;
                    elseBranch = false;
                    phase = Phase.SENSE;
                    rules.add(openRule);
                }
                case SPEC -> {
                    if (openRule == null) {
                        errors.add("orphan_spec:" + word.id());
                        break;
                    }
                    if (phase != Phase.SENSE) {
                        errors.add("spec_after_action:" + word.id());
                        break;
                    }
                    ListTag specs = openRule.getList(SPECS, Tag.TAG_STRING);
                    specs.add(StringTag.valueOf(word.effect().isEmpty() ? word.id().getPath() : word.effect()));
                    openRule.put(SPECS, specs);
                }
                case PROPERTY, TRIGGER -> {
                    openAction = newAction(word);
                    if (pendingLoop > 0) {
                        openAction.putInt(LOOP_REPEATS, pendingLoop);
                        pendingLoop = 0;
                    }
                    if (openRule != null) {
                        String listKey = elseBranch ? ELSE_ACTIONS : ACTIONS;
                        ListTag actions = openRule.contains(listKey, Tag.TAG_LIST)
                                ? openRule.getList(listKey, Tag.TAG_COMPOUND)
                                : new ListTag();
                        actions.add(openAction);
                        openRule.put(listKey, actions);
                    } else {
                        passives.add(openAction);
                    }
                    phase = Phase.ACTION;
                }
                case NUMBER -> {
                    float value = word.numberValue() > 0f ? word.numberValue() : 1f;
                    if (expectNum != ExpectNum.NONE) {
                        int n = Mth.clamp(Math.round(value), 1, 20);
                        switch (expectNum) {
                            case LOOP -> pendingLoop = n;
                            case COUNT -> {
                                if (openRule != null) {
                                    openRule.putInt(COUNT_NEED, n);
                                }
                            }
                            case UNTIL -> {
                                if (openRule != null) {
                                    openRule.putInt(UNTIL_MAX, n);
                                }
                            }
                            default -> {
                            }
                        }
                        expectNum = ExpectNum.NONE;
                        break;
                    }
                    if (phase == Phase.TIME) {
                        if (openRule == null) {
                            errors.add("orphan_number:" + word.id());
                        } else {
                            ListTag actions = actionListForOpen(openRule, elseBranch);
                            if (actions.isEmpty()) {
                                errors.add("time_without_action:" + word.id());
                            } else {
                                CompoundTag last = actions.getCompound(actions.size() - 1);
                                last.putInt(DURATION_TICKS, Mth.clamp(Math.round(value), 1, 200));
                                openRule = null;
                                openAction = null;
                                elseBranch = false;
                                phase = Phase.IDLE;
                            }
                        }
                    } else if (phase == Phase.SENSE && openRule != null && openAction == null) {
                        openRule.putFloat(RADIUS, Mth.clamp(value, 2f, 16f));
                    } else if (openAction != null) {
                        applyMagnitude(openAction, openAction.getString(EFFECT), value);
                    } else {
                        errors.add("orphan_number:" + word.id());
                    }
                }
                case DURATION -> {
                    if (openRule == null) {
                        errors.add("time_without_sense:" + word.id());
                    } else if (actionListForOpen(openRule, elseBranch).isEmpty()) {
                        errors.add("time_without_action:" + word.id());
                    } else {
                        phase = Phase.TIME;
                    }
                }
                case MODIFIER -> {
                    if (openAction == null) {
                        errors.add("orphan_modifier:" + word.id());
                    } else {
                        applyModifier(openAction, word);
                    }
                }
            }
        }

        if (expectNum != ExpectNum.NONE) {
            errors.add("control_without_number");
        }
        if (pendingLoop > 0) {
            errors.add("loop_without_action");
        }

        for (int i = 0; i < rules.size(); i++) {
            CompoundTag rule = rules.getCompound(i);
            boolean hasMain = rule.contains(ACTIONS, Tag.TAG_LIST)
                    && !rule.getList(ACTIONS, Tag.TAG_COMPOUND).isEmpty();
            boolean hasElse = rule.contains(ELSE_ACTIONS, Tag.TAG_LIST)
                    && !rule.getList(ELSE_ACTIONS, Tag.TAG_COMPOUND).isEmpty();
            if (!hasMain && !hasElse) {
                errors.add("sense_without_action");
            }
        }

        params.put(PASSIVES_TAG, passives);
        params.put(RULES_TAG, rules);

        if (errors.isEmpty() && passives.isEmpty() && rules.isEmpty()) {
            errors.add("no_effect");
        }
        return new CompileResult(params, cost, errors);
    }

    public static List<ResourceLocation> readTokens(CompoundTag params) {
        List<ResourceLocation> out = new ArrayList<>();
        if (params == null || !params.contains(TOKENS_TAG, Tag.TAG_LIST)) {
            return out;
        }
        ListTag list = params.getList(TOKENS_TAG, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            out.add(ResourceLocation.parse(list.getString(i)));
        }
        return out;
    }

    private static String controlKey(SealWordDefinition word) {
        if (!word.effect().isEmpty()) {
            return word.effect();
        }
        return word.id().getPath();
    }

    private static ListTag actionListForOpen(CompoundTag rule, boolean elseBranch) {
        String key = elseBranch ? ELSE_ACTIONS : ACTIONS;
        if (!rule.contains(key, Tag.TAG_LIST)) {
            return new ListTag();
        }
        return rule.getList(key, Tag.TAG_COMPOUND);
    }

    private static CompoundTag newRule(SealWordDefinition sense) {
        return newRule(sense.effect().isEmpty() ? sense.id().getPath() : sense.effect());
    }

    private static CompoundTag newRule(String sense) {
        CompoundTag rule = new CompoundTag();
        rule.putString(SENSE, sense);
        rule.putFloat(RADIUS, 6f);
        rule.put(SPECS, new ListTag());
        rule.put(ACTIONS, new ListTag());
        return rule;
    }

    private static CompoundTag newAction(SealWordDefinition word) {
        CompoundTag action = new CompoundTag();
        String effect = word.effect().isEmpty() ? word.id().getPath() : word.effect();
        // Aliases into existing runtime keys where helpful.
        effect = switch (effect) {
            case "lux" -> "light";
            case "firmitas" -> "hardness";
            default -> effect;
        };
        action.putString(EFFECT, effect);
        applyMagnitude(action, effect, defaultMagnitude(effect));
        if (word.soundEvent() != null) {
            action.putString(SOUND_EVENT, word.soundEvent().toString());
        }
        if ("sound".equals(effect) && !action.contains(SOUND_EVENT)) {
            action.putString(SOUND_EVENT, "minecraft:block.note_block.pling");
        }
        action.putFloat(SOUND_VOLUME, 0.55f);
        return action;
    }

    private static float defaultMagnitude(String effect) {
        return switch (effect) {
            case "hardness" -> 2f;
            case "hurt", "acies" -> 2f;
            case "slow" -> 2f;
            case "push" -> 1f;
            case "glow", "light" -> 10f;
            case "sound" -> 1f;
            case "calor" -> 2f;
            case "clausura", "umbra", "servare" -> 1f;
            case "extrahere" -> 8f;
            case "haustus" -> 6f;
            case "vigil" -> 1f;
            case "imprimere" -> 1f;
            case "ordo" -> 4f;
            case "abnegatio" -> 1f;
            case "absolutum" -> 8f;
            default -> 1f;
        };
    }

    private static void applyMagnitude(CompoundTag action, String effect, float magnitude) {
        float clamped = switch (effect) {
            case "hardness" -> Mth.clamp(magnitude, 1f, 10f);
            case "hurt", "acies" -> Mth.clamp(magnitude, 0.5f, 12f);
            case "slow" -> Mth.clamp(magnitude, 1f, 5f);
            case "push" -> Mth.clamp(magnitude, 0.5f, 5f);
            case "glow", "light" -> Mth.clamp(magnitude, 6f, 15f);
            case "sound" -> Mth.clamp(magnitude, 0.5f, 10f);
            case "calor" -> Mth.clamp(magnitude, 1f, 10f);
            case "extrahere", "haustus", "ordo", "absolutum" -> Mth.clamp(magnitude, 1f, 16f);
            default -> Mth.clamp(magnitude, 0.5f, 16f);
        };
        action.putFloat(MAGNITUDE, clamped);
    }

    private static void applyModifier(CompoundTag action, SealWordDefinition modifier) {
        String effect = action.getString(EFFECT);
        String mod = modifier.effect().isEmpty() ? modifier.id().getPath() : modifier.effect();
        if ("sound".equals(effect)) {
            if (modifier.soundEvent() != null) {
                action.putString(SOUND_EVENT, modifier.soundEvent().toString());
            } else if ("soft".equals(mod)) {
                action.putFloat(SOUND_VOLUME, 0.25f);
            } else if ("loud".equals(mod)) {
                action.putFloat(SOUND_VOLUME, 1.2f);
            }
        }
    }
}
