package com.effecoria.core.loci;

import com.effecoria.EffecoriaMod;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Phoenix compiler: {@code WHEN} rules with optional {@code kind*} / {@code kind#name} before
 * {@code autonom} / {@code signal}, or {@code bus:}/{@code шина:} before {@code shed}/{@code feed}.
 * {@code soul_alive} allows SIGNAL/AUTONOM/ARM/DISARM/FEED.
 * Tokens are strings (RL words or address literals).
 */
public final class LexLociCompiler {
    public static final ResourceLocation WHEN = EffecoriaMod.id("when");
    public static final ResourceLocation SOUL_DEAD = EffecoriaMod.id("soul_dead");
    public static final ResourceLocation SOUL_GHOST = EffecoriaMod.id("soul_ghost");
    public static final ResourceLocation SOUL_ALIVE = EffecoriaMod.id("soul_alive");
    public static final ResourceLocation SHED = EffecoriaMod.id("shed");
    public static final ResourceLocation FEED = EffecoriaMod.id("feed");
    public static final ResourceLocation SIGNAL = EffecoriaMod.id("signal");
    public static final ResourceLocation AUTONOM = EffecoriaMod.id("autonom");
    public static final ResourceLocation BEACON = EffecoriaMod.id("beacon");
    public static final ResourceLocation ARM = EffecoriaMod.id("arm");
    public static final ResourceLocation DISARM = EffecoriaMod.id("disarm");

    public static final String WHEN_TOKEN = WHEN.toString();
    public static final String SOUL_DEAD_TOKEN = SOUL_DEAD.toString();
    public static final String SOUL_GHOST_TOKEN = SOUL_GHOST.toString();
    public static final String SOUL_ALIVE_TOKEN = SOUL_ALIVE.toString();
    public static final String SHED_TOKEN = SHED.toString();
    public static final String FEED_TOKEN = FEED.toString();
    public static final String SIGNAL_TOKEN = SIGNAL.toString();
    public static final String AUTONOM_TOKEN = AUTONOM.toString();
    public static final String BEACON_TOKEN = BEACON.toString();
    public static final String ARM_TOKEN = ARM.toString();
    public static final String DISARM_TOKEN = DISARM.toString();

    public static final int MAX_TOKENS = 16;
    public static final int MAX_RULES = 3;

    public record Actuation(LociActuator actuator, @Nullable LociAddress target) {}

    public record Rule(LociEvent event, List<Actuation> actuations) {
        public boolean has(LociActuator actuator) {
            for (Actuation a : actuations) {
                if (a.actuator() == actuator) {
                    return true;
                }
            }
            return false;
        }

        public EnumSet<LociActuator> actuatorSet() {
            EnumSet<LociActuator> set = EnumSet.noneOf(LociActuator.class);
            for (Actuation a : actuations) {
                set.add(a.actuator());
            }
            return set;
        }

        public List<Actuation> of(LociActuator actuator) {
            List<Actuation> out = new ArrayList<>();
            for (Actuation a : actuations) {
                if (a.actuator() == actuator) {
                    out.add(a);
                }
            }
            return out;
        }
    }

    public record CompileResult(List<Rule> rules, List<String> errors) {
        public boolean ok() {
            return errors.isEmpty() && !rules.isEmpty();
        }

        public List<Actuation> actuationsFor(LociEvent event) {
            for (Rule rule : rules) {
                if (rule.event() == event) {
                    return rule.actuations();
                }
            }
            return List.of();
        }

        public EnumSet<LociActuator> actuatorsFor(LociEvent event) {
            for (Rule rule : rules) {
                if (rule.event() == event) {
                    return rule.actuatorSet();
                }
            }
            return EnumSet.noneOf(LociActuator.class);
        }

        public boolean has(LociEvent event, LociActuator actuator) {
            return actuatorsFor(event).contains(actuator);
        }
    }

    private LexLociCompiler() {}

    public static List<String> defaultPhoenixTokens() {
        return List.of(WHEN_TOKEN, SOUL_DEAD_TOKEN, SHED_TOKEN, SIGNAL_TOKEN, AUTONOM_TOKEN);
    }

    public static List<String> palette() {
        return List.of(
                WHEN_TOKEN,
                SOUL_DEAD_TOKEN,
                SOUL_GHOST_TOKEN,
                SOUL_ALIVE_TOKEN,
                SHED_TOKEN,
                FEED_TOKEN,
                SIGNAL_TOKEN,
                AUTONOM_TOKEN,
                ARM_TOKEN,
                DISARM_TOKEN,
                BEACON_TOKEN);
    }

    /** Empty / null storage is the shipped Phoenix edict. */
    public static List<String> effectiveTokens(List<String> stored) {
        if (stored == null || stored.isEmpty()) {
            return defaultPhoenixTokens();
        }
        return List.copyOf(stored);
    }

    public static boolean isDefault(List<String> stored) {
        return stored == null || stored.isEmpty() || stored.equals(defaultPhoenixTokens());
    }

    public static CompileResult defaultProgram() {
        return new CompileResult(
                List.of(new Rule(
                        LociEvent.SOUL_DEAD,
                        List.of(
                                new Actuation(LociActuator.SHED, null),
                                new Actuation(LociActuator.SIGNAL, null),
                                new Actuation(LociActuator.AUTONOM, null)))),
                List.of());
    }

    public static CompileResult compile(List<String> stored) {
        return parse(effectiveTokens(stored), true);
    }

    public static boolean canAppend(List<String> draft, String token) {
        if (token == null || token.isEmpty() || draft == null || draft.size() >= MAX_TOKENS) {
            return false;
        }
        List<String> next = new ArrayList<>(draft);
        next.add(token);
        return parse(next, false).errors().isEmpty();
    }

    public static boolean isSense(String token) {
        return SOUL_DEAD_TOKEN.equals(token)
                || SOUL_GHOST_TOKEN.equals(token)
                || SOUL_ALIVE_TOKEN.equals(token);
    }

    public static boolean isSense(ResourceLocation id) {
        return id != null && isSense(id.toString());
    }

    public static boolean isAddressToken(String token) {
        return LociAddress.parse(token).isPresent();
    }

    @Nullable
    public static LociActuator actuatorOf(String token) {
        if (SHED_TOKEN.equals(token)) {
            return LociActuator.SHED;
        }
        if (FEED_TOKEN.equals(token)) {
            return LociActuator.FEED;
        }
        if (SIGNAL_TOKEN.equals(token)) {
            return LociActuator.SIGNAL;
        }
        if (AUTONOM_TOKEN.equals(token)) {
            return LociActuator.AUTONOM;
        }
        if (BEACON_TOKEN.equals(token)) {
            return LociActuator.BEACON;
        }
        if (ARM_TOKEN.equals(token)) {
            return LociActuator.ARM;
        }
        if (DISARM_TOKEN.equals(token)) {
            return LociActuator.DISARM;
        }
        return null;
    }

    @Nullable
    public static LociActuator actuatorOf(ResourceLocation id) {
        return id == null ? null : actuatorOf(id.toString());
    }

    @Nullable
    public static LociEvent eventOf(String token) {
        if (SOUL_DEAD_TOKEN.equals(token)) {
            return LociEvent.SOUL_DEAD;
        }
        if (SOUL_GHOST_TOKEN.equals(token)) {
            return LociEvent.SOUL_GHOST;
        }
        if (SOUL_ALIVE_TOKEN.equals(token)) {
            return LociEvent.SOUL_ALIVE;
        }
        return null;
    }

    @Nullable
    public static LociEvent eventOf(ResourceLocation id) {
        return id == null ? null : eventOf(id.toString());
    }

    public static String wordLabelKey(String token) {
        ResourceLocation id = ResourceLocation.tryParse(token);
        if (id == null) {
            return "loci_word.effecoria.unknown";
        }
        return wordLabelKey(id);
    }

    public static String wordLabelKey(ResourceLocation id) {
        return "loci_word.effecoria." + id.getPath();
    }

    private static CompileResult parse(List<String> tokens, boolean requireComplete) {
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
        List<Actuation> openActs = new ArrayList<>();
        LociAddress pendingAddr = null;

        for (String token : tokens) {
            if (WHEN_TOKEN.equals(token)) {
                if (pendingAddr != null) {
                    errors.add("dangling_addr");
                    break;
                }
                if (phase == Phase.ACTION && openEvent != null && !openActs.isEmpty()) {
                    rules.add(new Rule(openEvent, List.copyOf(openActs)));
                    openEvent = null;
                    openActs = new ArrayList<>();
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

            Optional<LociAddress> addrOpt = LociAddress.parse(token);
            if (addrOpt.isPresent()) {
                if (phase != Phase.ACTION || openEvent == null) {
                    errors.add("need_sense");
                    break;
                }
                if (pendingAddr != null) {
                    errors.add("dup_addr");
                    break;
                }
                pendingAddr = addrOpt.get();
                continue;
            }

            LociEvent event = eventOf(token);
            if (event != null) {
                if (pendingAddr != null) {
                    errors.add("dangling_addr");
                    break;
                }
                if (phase != Phase.SENSE) {
                    errors.add("need_when");
                    break;
                }
                if (!seen.add(event)) {
                    errors.add("dup_event");
                    break;
                }
                openEvent = event;
                openActs = new ArrayList<>();
                phase = Phase.ACTION;
                continue;
            }

            LociActuator actuator = actuatorOf(token);
            if (actuator != null) {
                if (phase != Phase.ACTION || openEvent == null) {
                    errors.add("need_sense");
                    break;
                }
                if (!actuatorAllowed(openEvent, actuator)) {
                    errors.add("bad_actuator");
                    break;
                }
                Optional<String> neededKind = LociAddress.kindForActuator(actuator);
                if (pendingAddr != null) {
                    if (neededKind.isEmpty() || !neededKind.get().equals(pendingAddr.kind())) {
                        errors.add("bad_target");
                        break;
                    }
                    openActs.add(new Actuation(actuator, pendingAddr));
                    pendingAddr = null;
                } else {
                    openActs.add(new Actuation(actuator, null));
                }
                continue;
            }

            errors.add("unknown:" + token);
            break;
        }

        if (pendingAddr != null) {
            if (requireComplete) {
                errors.add("dangling_addr");
            }
        } else if (openEvent != null) {
            if (openActs.isEmpty()) {
                if (requireComplete) {
                    errors.add("no_action");
                }
            } else {
                rules.add(new Rule(openEvent, List.copyOf(openActs)));
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

    private static boolean actuatorAllowed(LociEvent event, LociActuator actuator) {
        return switch (event) {
            case SOUL_DEAD -> actuator != LociActuator.BEACON;
            case SOUL_GHOST -> actuator == LociActuator.BEACON;
            case SOUL_ALIVE -> actuator == LociActuator.SIGNAL
                    || actuator == LociActuator.AUTONOM
                    || actuator == LociActuator.ARM
                    || actuator == LociActuator.DISARM
                    || actuator == LociActuator.FEED;
        };
    }
}
