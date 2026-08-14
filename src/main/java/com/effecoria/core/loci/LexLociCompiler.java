package com.effecoria.core.loci;

import com.effecoria.EffecoriaMod;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * E1 Phoenix compiler: {@code WHEN} + {@code soul_dead} + at least one trigger
 * ({@code shed} / {@code signal} / {@code autonom}). Empty stored tokens mean the built-in edict.
 */
public final class LexLociCompiler {
    public static final ResourceLocation WHEN = EffecoriaMod.id("when");
    public static final ResourceLocation SOUL_DEAD = EffecoriaMod.id("soul_dead");
    public static final ResourceLocation SHED = EffecoriaMod.id("shed");
    public static final ResourceLocation SIGNAL = EffecoriaMod.id("signal");
    public static final ResourceLocation AUTONOM = EffecoriaMod.id("autonom");

    public static final int MAX_TOKENS = 5;

    public record CompileResult(LociEvent event, EnumSet<LociActuator> actuators, List<String> errors) {
        public boolean ok() {
            return errors.isEmpty() && event != null && !actuators.isEmpty();
        }
    }

    private LexLociCompiler() {}

    public static List<ResourceLocation> defaultPhoenixTokens() {
        return List.of(WHEN, SOUL_DEAD, SHED, SIGNAL, AUTONOM);
    }

    public static List<ResourceLocation> palette() {
        return defaultPhoenixTokens();
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
                LociEvent.SOUL_DEAD,
                EnumSet.of(LociActuator.SHED, LociActuator.SIGNAL, LociActuator.AUTONOM),
                List.of());
    }

    public static CompileResult compile(List<ResourceLocation> stored) {
        List<ResourceLocation> tokens = effectiveTokens(stored);
        List<String> errors = new ArrayList<>();
        if (tokens.size() > MAX_TOKENS) {
            errors.add("too_many");
            return new CompileResult(null, EnumSet.noneOf(LociActuator.class), errors);
        }
        if (tokens.size() < 3) {
            errors.add("too_short");
            return new CompileResult(null, EnumSet.noneOf(LociActuator.class), errors);
        }
        if (!WHEN.equals(tokens.get(0))) {
            errors.add("need_when");
        }
        if (!SOUL_DEAD.equals(tokens.get(1))) {
            errors.add("need_soul_dead");
        }
        EnumSet<LociActuator> actuators = EnumSet.noneOf(LociActuator.class);
        for (int i = 2; i < tokens.size(); i++) {
            ResourceLocation id = tokens.get(i);
            LociActuator actuator = actuatorOf(id);
            if (actuator == null) {
                errors.add("unknown:" + id);
                continue;
            }
            if (!actuators.add(actuator)) {
                errors.add("dup:" + id);
            }
        }
        if (actuators.isEmpty()) {
            errors.add("no_action");
        }
        if (!errors.isEmpty()) {
            return new CompileResult(null, EnumSet.noneOf(LociActuator.class), errors);
        }
        return new CompileResult(LociEvent.SOUL_DEAD, actuators, List.of());
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
        return null;
    }

    public static boolean canAppend(List<ResourceLocation> draft, ResourceLocation word) {
        if (word == null || draft == null) {
            return false;
        }
        if (draft.size() >= MAX_TOKENS) {
            return false;
        }
        if (WHEN.equals(word)) {
            return draft.isEmpty();
        }
        if (SOUL_DEAD.equals(word)) {
            return draft.size() == 1 && WHEN.equals(draft.get(0));
        }
        if (actuatorOf(word) == null) {
            return false;
        }
        if (draft.size() < 2 || !WHEN.equals(draft.get(0)) || !SOUL_DEAD.equals(draft.get(1))) {
            return false;
        }
        return !draft.contains(word);
    }

    public static String wordLabelKey(ResourceLocation id) {
        return "loci_word.effecoria." + id.getPath();
    }
}
