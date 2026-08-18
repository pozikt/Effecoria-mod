package com.effecoria.core.seal;

import java.util.List;
import java.util.Locale;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** Reconstructs a seal script stanza from stored NBT or legacy token chips. */
public final class SealScriptPrinter {
    private SealScriptPrinter() {}

    public static String pretty(String symbol, CompoundTag params) {
        if (params != null && params.contains(SealProgramCompiler.SCRIPT_TAG, Tag.TAG_STRING)) {
            String stored = params.getString(SealProgramCompiler.SCRIPT_TAG);
            if (!stored.isBlank()) {
                return stored.endsWith("\n") ? stored : stored + "\n";
            }
        }
        StringBuilder out = new StringBuilder();
        out.append(symbol).append(":\n");
        boolean any = false;
        if (params != null) {
            ListTag passives = params.getList(SealProgramCompiler.PASSIVES_TAG, Tag.TAG_COMPOUND);
            for (int i = 0; i < passives.size(); i++) {
                CompoundTag action = passives.getCompound(i);
                String line = assignLine(action);
                if (!line.isEmpty()) {
                    out.append("  ").append(line).append('\n');
                    any = true;
                }
            }
            ListTag rules = params.getList(SealProgramCompiler.RULES_TAG, Tag.TAG_COMPOUND);
            for (int i = 0; i < rules.size(); i++) {
                CompoundTag rule = rules.getCompound(i);
                ListTag specs = rule.getList(SealProgramCompiler.SPECS, Tag.TAG_STRING);
                String spec = specs.isEmpty() ? "step" : specs.getString(0);
                out.append("  when ").append(spec).append(":\n");
                ListTag actions = rule.getList(SealProgramCompiler.ACTIONS, Tag.TAG_COMPOUND);
                for (int a = 0; a < actions.size(); a++) {
                    String line = assignLine(actions.getCompound(a));
                    if (!line.isEmpty()) {
                        out.append("    ").append(line).append('\n');
                        any = true;
                    }
                }
            }
        }
        if (!any) {
            List<ResourceLocation> tokens = SealProgramCompiler.readTokens(params);
            if (!tokens.isEmpty()) {
                return fromTokens(symbol, tokens);
            }
            return "";
        }
        return out.toString();
    }

    public static String fromTokens(String symbol, List<ResourceLocation> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        out.append(symbol).append(":\n");
        boolean inWhen = false;
        String pendingProp = null;
        for (ResourceLocation id : tokens) {
            var opt = SealWordRegistry.get(id);
            if (opt.isEmpty()) {
                continue;
            }
            SealWordDefinition word = opt.get();
            switch (word.kind()) {
                case SENSE, CONTROL -> {
                    inWhen = true;
                    pendingProp = null;
                }
                case SPEC -> {
                    String spec = word.effect().isEmpty() ? word.id().getPath() : word.effect();
                    out.append("  when ").append(spec).append(":\n");
                    inWhen = true;
                }
                case PROPERTY, TRIGGER -> {
                    pendingProp = word.effect().isEmpty() ? word.id().getPath() : word.effect();
                    pendingProp = switch (pendingProp) {
                        case "lux" -> "glow";
                        case "light" -> "glow";
                        case "firmitas" -> "hardness";
                        default -> pendingProp;
                    };
                }
                case NUMBER -> {
                    float value = word.numberValue() > 0f ? word.numberValue() : 1f;
                    if (pendingProp != null) {
                        String indent = inWhen ? "    " : "  ";
                        out.append(indent)
                                .append(pendingProp)
                                .append(" = ")
                                .append(trimNumber(value))
                                .append('\n');
                        pendingProp = null;
                    }
                }
                default -> {
                }
            }
        }
        if (pendingProp != null) {
            String indent = inWhen ? "    " : "  ";
            out.append(indent).append(pendingProp).append(" = 1\n");
        }
        return out.toString();
    }

    private static String assignLine(CompoundTag action) {
        String effect = action.getString(SealProgramCompiler.EFFECT);
        if (effect.isEmpty()) {
            return "";
        }
        if ("light".equals(effect) || "lux".equals(effect)) {
            effect = "glow";
        }
        if ("firmitas".equals(effect)) {
            effect = "hardness";
        }
        float mag = action.getFloat(SealProgramCompiler.MAGNITUDE);
        return effect + " = " + trimNumber(mag);
    }

    private static String trimNumber(float value) {
        if (Math.abs(value - Math.round(value)) < 0.01f) {
            return Integer.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
