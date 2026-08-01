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
 *   <li>Reactive: {@code SENSE SPEC* ACTION NUMBER? MODIFIER* (TIME NUMBER)?}
 *       — sense emits a unit on event; then action fires (e.g. See Player Sound Five)</li>
 *   <li>Timed reactive: {@code See Hardness Five Time Ten} — action lasts N ticks</li>
 * </ul>
 * Multiple passive segments and reactive rules may be chained in one program.
 */
public final class SealProgramCompiler {
    public static final String TOKENS_TAG = "tokens";
    public static final String VERSION_TAG = "program_version";
    public static final int PROGRAM_VERSION = 2;

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
        return Mth.clamp(4 + Math.round(breathingMasteryRatio * 6f), 4, 12);
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
        enum Phase { IDLE, SENSE, ACTION, TIME }
        Phase phase = Phase.IDLE;

        for (SealWordDefinition word : words) {
            switch (word.kind()) {
                case SENSE -> {
                    openRule = newRule(word);
                    openAction = null;
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
                    if (openRule != null) {
                        ListTag actions = openRule.getList(ACTIONS, Tag.TAG_COMPOUND);
                        actions.add(openAction);
                        openRule.put(ACTIONS, actions);
                    } else {
                        passives.add(openAction);
                    }
                    phase = Phase.ACTION;
                }
                case NUMBER -> {
                    float value = word.numberValue() > 0f ? word.numberValue() : 1f;
                    if (phase == Phase.TIME) {
                        if (openRule == null) {
                            errors.add("orphan_number:" + word.id());
                        } else {
                            ListTag actions = openRule.getList(ACTIONS, Tag.TAG_COMPOUND);
                            if (actions.isEmpty()) {
                                errors.add("time_without_action:" + word.id());
                            } else {
                                CompoundTag last = actions.getCompound(actions.size() - 1);
                                last.putInt(DURATION_TICKS, Mth.clamp(Math.round(value), 1, 200));
                                openRule = null;
                                openAction = null;
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
                    } else if (openRule.getList(ACTIONS, Tag.TAG_COMPOUND).isEmpty()) {
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

        for (int i = 0; i < rules.size(); i++) {
            CompoundTag rule = rules.getCompound(i);
            if (rule.getList(ACTIONS, Tag.TAG_COMPOUND).isEmpty()) {
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

    private static CompoundTag newRule(SealWordDefinition sense) {
        CompoundTag rule = new CompoundTag();
        rule.putString(SENSE, sense.effect().isEmpty() ? sense.id().getPath() : sense.effect());
        rule.putFloat(RADIUS, 6f);
        rule.put(SPECS, new ListTag());
        rule.put(ACTIONS, new ListTag());
        return rule;
    }

    private static CompoundTag newAction(SealWordDefinition word) {
        CompoundTag action = new CompoundTag();
        String effect = word.effect().isEmpty() ? word.id().getPath() : word.effect();
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
            case "hurt" -> 2f;
            case "slow" -> 2f;
            case "push" -> 1f;
            case "glow", "light" -> 10f;
            case "sound" -> 1f;
            default -> 1f;
        };
    }

    private static void applyMagnitude(CompoundTag action, String effect, float magnitude) {
        float clamped = switch (effect) {
            case "hardness" -> Mth.clamp(magnitude, 1f, 10f);
            case "hurt" -> Mth.clamp(magnitude, 0.5f, 12f);
            case "slow" -> Mth.clamp(magnitude, 1f, 5f);
            case "push" -> Mth.clamp(magnitude, 0.5f, 5f);
            case "glow", "light" -> Mth.clamp(magnitude, 6f, 15f);
            case "sound" -> Mth.clamp(magnitude, 0.5f, 10f);
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
