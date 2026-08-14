package com.effecoria.core.loci;

import com.effecoria.EffecoriaMod;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Phoenix compiler: one or two {@code WHEN} rules.
 * {@code soul_dead} drives shed / signal / autonom; {@code soul_ghost} may add {@code beacon}.
 * Empty stored tokens mean the built-in death edict.
 */
public final class LexLociCompiler {
    public static final ResourceLocation WHEN = EffecoriaMod.id("when");
    public static final ResourceLocation SOUL_DEAD = EffecoriaMod.id("soul_dead");
    public static final ResourceLocation SOUL_GHOST = EffecoriaMod.id("soul_ghost");
    public static final ResourceLocation SHED = EffecoriaMod.id("shed");
    public static final ResourceLocation SIGNAL = EffecoriaMod.id("signal");
    public static final ResourceLocation AUTONOM = EffecoriaMod.id("autonom");
    public static final ResourceLocation BEACON = EffecoriaMod.id("beacon");

    public static final int MAX_TOKENS = 8;
    public static final int MAX_RULES = 2;

    public record Rule(LociEvent event, EnumSet<LociActuator> actuators) {}

    public record CompileResult(List<Rule> rules, List<String> errors) {
        public boolean ok() {
            return errors.isEmpty() && !rules.isEmpty();
        }

        public EnumSet<LociActuator> actuatorsFor(LociEvent event) {
            for (Rule rule : rules) {
                if (rule.event() == event) {
                    return rule.actuators();
                }
            }
            return EnumSet.noneOf(LociActuator.class);
        }
    }

    private LexLociCompiler() {}

    public static List<ResourceLocation> defaultPhoenixTokens() {
        return List.of(WHEN, SOUL_DEAD, SHED, SIGNAL, AUTONOM);
    }

    public static List<ResourceLocation> palette() {
        return List.of(WHEN, SOUL_DEAD, SOUL_GHOST, SHED, SIGNAL, AUTONOM, BEACON);
    }

    /** Empty / null storage is the shipped Phoenix edict. */
    public static List<ResourceLocation> effectiveTokens(List<ResourceLocation> stored) {
        if (stored == null || stored.isEmpty()) {
            return defaultPhoenixTokens();
        }
        return List.copyOf(stored);
    }

    public static boolean isDefault(List<ResourceLocation> stored) {
        return stored == null || stored.isEmpty() || stored.equals(defaultPhoenixTokens());
    }

    public static CompileResult defaultProgram() {
        return new CompileResult(
                List.of(new Rule(
                        LociEvent.SOUL_DEAD,
                        EnumSet.of(LociActuator.SHED, LociActuator.SIGNAL, LociActuator.AUTONOM))),
                List.of());
    }

    public static CompileResult compile(List<ResourceLocation> stored) {
        return parse(effectiveTokens(stored), true);
    }

    public static boolean canAppend(List<ResourceLocation> draft, ResourceLocation word) {
        if (word == null || draft == null || draft.size() >= MAX_TOKENS) {
            return false;
        }
        List<ResourceLocation> next = new ArrayList<>(draft);
        next.add(word);
        CompileResult prefix = parse(next, false);
        return prefix.errors().isEmpty();
    }

    public static boolean isSense(ResourceLocation id) {
        return SOUL_DEAD.equals(id) || SOUL_GHOST.equals(id);
    }

    public static LociActuator actuatorOf(ResourceLocation id) {
        if (SHED.equals(id)) {
            return LociActuator.SHED;
        }
        if (SIGNAL.equals(id)) {
            return LociActuator.SIGNAL;
        }
        if (AUTONOM.equals(id)) {
            return LociActuator.AUTONOM;
        }
        if (BEACON.equals(id)) {
            return LociActuator.BEACON;
        }
        return null;
    }

    public static LociEvent eventOf(ResourceLocation id) {
        if (SOUL_DEAD.equals(id)) {
            return LociEvent.SOUL_DEAD;
        }
        if (SOUL_GHOST.equals(id)) {
            return LociEvent.SOUL_GHOST;
        }
        return null;
    }

    public static String wordLabelKey(ResourceLocation id) {
        return "loci_word.effecoria." + id.getPath();
    }

    private static CompileResult parse(List<ResourceLocation> tokens, boolean requireComplete) {
        List<String> errors = new ArrayList<>();
        if (tokens.size() > MAX_TOKENS) {
            errors.add("too_many");
            return new CompileResult(List.of(), errors);
        }
        List<Rule> rules = new ArrayList<>();
        EnumSet<LociEvent> seen = EnumSet.noneOf(LociEvent.class);
        enum Phase {
            IDLE,
            SENSE,
            ACTION
        }
        Phase phase = Phase.IDLE;
        LociEvent openEvent = null;
        EnumSet<LociActuator> openActs = EnumSet.noneOf(LociActuator.class);

        for (ResourceLocation id : tokens) {
            if (WHEN.equals(id)) {
                if (phase == Phase.ACTION && openEvent != null && !openActs.isEmpty()) {
                    rules.add(new Rule(openEvent, EnumSet.copyOf(openActs)));
                    openEvent = null;
                    openActs = EnumSet.noneOf(LociActuator.class);
                    phase = Phase.IDLE;
                }
                if (phase != Phase.IDLE) {
                    errors.add("bad_when");
                    break;
                }
                if (rules.size() >= MAX_RULES) {
                    errors.add("too_many_rules");
                    break;
                }
                phase = Phase.SENSE;
                continue;
            }
            LociEvent event = eventOf(id);
            if (event != null) {
                if (phase != Phase.SENSE) {
                    errors.add("need_when");
                    break;
                }
                if (!seen.add(event)) {
                    errors.add("dup_event");
                    break;
                }
                openEvent = event;
                openActs = EnumSet.noneOf(LociActuator.class);
                phase = Phase.ACTION;
                continue;
            }
            LociActuator actuator = actuatorOf(id);
            if (actuator != null) {
                if (phase != Phase.ACTION || openEvent == null) {
                    errors.add("need_sense");
                    break;
                }
                if (!openActs.add(actuator)) {
                    errors.add("dup:" + id);
                    break;
                }
                continue;
            }
            errors.add("unknown:" + id);
            break;
        }

        if (openEvent != null) {
            if (openActs.isEmpty()) {
                if (requireComplete) {
                    errors.add("no_action");
                }
            } else {
                rules.add(new Rule(openEvent, EnumSet.copyOf(openActs)));
            }
        } else if (phase == Phase.SENSE && requireComplete) {
            errors.add("need_sense");
        }

        if (requireComplete && rules.isEmpty() && errors.isEmpty()) {
            errors.add("empty");
        }
        if (!errors.isEmpty()) {
            return new CompileResult(List.of(), errors);
        }
        return new CompileResult(List.copyOf(rules), List.of());
    }
}
