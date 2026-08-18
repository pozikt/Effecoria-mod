package com.effecoria.core.seal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Parses the seal reality-programming language into per-cell program NBT.
 *
 * <pre>
 * dirt#north:
 *   glow = 5
 *   when step:
 *     sound = 5
 * </pre>
 */
public final class SealScriptParser {
    private static final Pattern HEADER =
            Pattern.compile("^([\\p{L}0-9_]+)(?:#([\\p{L}0-9_]+))?\\s*:\\s*$");
    private static final Pattern WHEN =
            Pattern.compile("^(?:when|когда|if|если)\\s+([\\p{L}0-9_]+)\\s*:\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASSIGN =
            Pattern.compile("^([\\p{L}0-9_]+)\\s*=\\s*([0-9]+(?:\\.[0-9]+)?)\\s*$", Pattern.CASE_INSENSITIVE);

    public record Stanza(
            SealSymbolTable.Member member, CompoundTag params, float psiCost, List<ResourceLocation> usedWords) {}

    public record Result(List<Stanza> stanzas, List<String> errors) {
        public boolean ok() {
            return errors.isEmpty() && !stanzas.isEmpty();
        }
    }

    private SealScriptParser() {}

    public static Result parse(String source, SealSymbolTable table) {
        List<String> errors = new ArrayList<>();
        if (source == null || source.isBlank()) {
            errors.add("empty");
            return new Result(List.of(), errors);
        }

        Map<String, StanzaBuilder> builders = new LinkedHashMap<>();
        StanzaBuilder current = null;
        boolean inWhen = false;
        String whenSpec = "";

        String[] lines = source.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i];
            String trimmed = raw.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("//")) {
                if (current != null) {
                    current.source.append(raw).append('\n');
                }
                continue;
            }
            if (SealScriptLexicon.isImport(trimmed)) {
                continue;
            }

            Matcher header = HEADER.matcher(trimmed);
            if (header.matches() && !SealScriptLexicon.isWhen(header.group(1))) {
                current = open(table, builders, header.group(1), header.group(2), errors, i + 1);
                inWhen = false;
                whenSpec = "";
                if (current != null) {
                    current.source.append(trimmed).append('\n');
                }
                continue;
            }

            Matcher when = WHEN.matcher(trimmed);
            if (when.matches()) {
                if (current == null) {
                    errors.add("when_without_block:" + (i + 1));
                    continue;
                }
                inWhen = true;
                whenSpec = SealScriptLexicon.canonicalSpec(when.group(1));
                current.source.append(trimmed).append('\n');
                continue;
            }

            Matcher assign = ASSIGN.matcher(trimmed);
            if (assign.matches()) {
                if (current == null) {
                    errors.add("assign_without_block:" + (i + 1));
                    continue;
                }
                String prop = SealScriptLexicon.canonicalProperty(assign.group(1));
                float value = Float.parseFloat(assign.group(2));
                addAssignment(current, prop, value, inWhen, whenSpec, errors, i + 1);
                current.source.append(trimmed).append('\n');
                continue;
            }

            errors.add("bad_line:" + (i + 1));
        }

        List<Stanza> stanzas = new ArrayList<>();
        for (StanzaBuilder builder : builders.values()) {
            if (builder.passives.isEmpty() && builder.rules.isEmpty()) {
                errors.add("empty_stanza:" + builder.member.symbol());
                continue;
            }
            CompoundTag params = SealProgramCompiler.assemble(
                    builder.passives, builder.rules, builder.source.toString().stripTrailing());
            stanzas.add(new Stanza(builder.member, params, builder.psiCost, List.copyOf(builder.usedWords)));
        }
        if (stanzas.isEmpty() && errors.isEmpty()) {
            errors.add("empty");
        }
        return new Result(stanzas, errors);
    }

    private static StanzaBuilder open(
            SealSymbolTable table,
            Map<String, StanzaBuilder> builders,
            String typeRaw,
            String aliasRaw,
            List<String> errors,
            int line) {
        String type = SealScriptLexicon.canonicalType(typeRaw);
        String alias = aliasRaw == null ? "" : SealScriptLexicon.sanitizeAlias(aliasRaw);
        String symbol = alias.isEmpty() ? type : type + "#" + alias;
        SealSymbolTable.Member member = table.lookup(symbol);
        if (member == null) {
            errors.add("unknown_block:" + symbol + ":" + line);
            return null;
        }
        if (member.conflict()) {
            errors.add("conflict:" + symbol + ":" + line);
            return null;
        }
        return builders.computeIfAbsent(member.symbol(), k -> new StanzaBuilder(member));
    }

    private static void addAssignment(
            StanzaBuilder builder,
            String prop,
            float value,
            boolean inWhen,
            String spec,
            List<String> errors,
            int line) {
        ResourceLocation word = wordForEffect(prop);
        if (word == null) {
            errors.add("unknown_property:" + prop + ":" + line);
            return;
        }
        CompoundTag action = SealProgramCompiler.createAction(prop, value);
        builder.noteWord(word);
        if (inWhen) {
            CompoundTag rule = newRule(spec);
            ListTag actions = rule.getList(SealProgramCompiler.ACTIONS, net.minecraft.nbt.Tag.TAG_COMPOUND);
            actions.add(action);
            rule.put(SealProgramCompiler.ACTIONS, actions);
            builder.rules.add(rule);
            ResourceLocation see = wordForEffect("see");
            if (see != null) {
                builder.noteWord(see);
            }
            ResourceLocation specWord = wordForEffect(spec);
            if (specWord != null) {
                builder.noteWord(specWord);
            }
        } else {
            builder.passives.add(action);
        }
    }

    private static CompoundTag newRule(String spec) {
        CompoundTag rule = new CompoundTag();
        rule.putString(SealProgramCompiler.SENSE, "see");
        rule.putFloat(SealProgramCompiler.RADIUS, 6f);
        ListTag specs = new ListTag();
        if (spec != null && !spec.isEmpty()) {
            specs.add(StringTag.valueOf(spec));
        }
        rule.put(SealProgramCompiler.SPECS, specs);
        rule.put(SealProgramCompiler.ACTIONS, new ListTag());
        return rule;
    }

    private static ResourceLocation wordForEffect(String effect) {
        if (effect == null || effect.isEmpty()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("effecoria", effect);
        if (SealWordRegistry.contains(id)) {
            return id;
        }
        for (SealWordDefinition word : SealWordRegistry.all().values()) {
            String key = word.effect().isEmpty() ? word.id().getPath() : word.effect();
            if (key.equalsIgnoreCase(effect) || word.id().getPath().equalsIgnoreCase(effect)) {
                return word.id();
            }
        }
        return null;
    }

    private static final class StanzaBuilder {
        private final SealSymbolTable.Member member;
        private final ListTag passives = new ListTag();
        private final ListTag rules = new ListTag();
        private final StringBuilder source = new StringBuilder();
        private final List<ResourceLocation> usedWords = new ArrayList<>();
        private float psiCost;

        private StanzaBuilder(SealSymbolTable.Member member) {
            this.member = member;
        }

        private void noteWord(ResourceLocation id) {
            if (!usedWords.contains(id)) {
                usedWords.add(id);
            }
            SealWordRegistry.get(id).ifPresent(word -> psiCost += word.psiCost());
        }
    }
}
