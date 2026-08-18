package com.effecoria.core.seal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.effecoria.core.glue.EssenceGlueData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Glue component → named cells for the seal script editor.
 *
 * <p>One block of a type becomes {@code dirt}. Several of the same type without aliases
 * are a conflict until the player sets {@code dirt#north}.
 */
public final class SealSymbolTable {
    public record Member(BlockPos pos, String typeKey, String alias, boolean conflict) {
        public String symbol() {
            if (!alias.isEmpty()) {
                return typeKey + "#" + alias;
            }
            return typeKey;
        }
    }

    private final BlockPos anchor;
    private final List<Member> members;
    private final Map<String, Member> bySymbol;

    private SealSymbolTable(BlockPos anchor, List<Member> members, Map<String, Member> bySymbol) {
        this.anchor = anchor;
        this.members = members;
        this.bySymbol = bySymbol;
    }

    public BlockPos anchor() {
        return anchor;
    }

    public List<Member> members() {
        return members;
    }

    public Member lookup(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String key = SealScriptLexicon.normalizeSymbol(raw);
        Member direct = bySymbol.get(key);
        if (direct != null) {
            return direct;
        }
        int hash = key.indexOf('#');
        String type = hash < 0 ? key : key.substring(0, hash);
        String alias = hash < 0 ? "" : key.substring(hash + 1);
        type = SealScriptLexicon.canonicalType(type);
        String rebuilt = alias.isEmpty() ? type : type + "#" + alias;
        return bySymbol.get(rebuilt);
    }

    public static SealSymbolTable build(ServerLevel level, BlockPos anchor) {
        return build(level, anchor, Map.of());
    }

    public static SealSymbolTable build(ServerLevel level, BlockPos anchor, Map<BlockPos, String> extraAliases) {
        EssenceGlueData glue = EssenceGlueData.get(level);
        Set<BlockPos> component = glue.component(anchor);
        List<BlockPos> cells = new ArrayList<>();
        if (component.isEmpty()) {
            cells.add(anchor.immutable());
        } else {
            cells.addAll(component);
            cells.sort(BlockPos::compareTo);
        }

        List<String> typeKeys = new ArrayList<>(cells.size());
        for (BlockPos pos : cells) {
            typeKeys.add(typeKey(level.getBlockState(pos)));
        }

        List<Member> draft = new ArrayList<>();
        Map<String, Integer> symbolCounts = new HashMap<>();
        for (int i = 0; i < cells.size(); i++) {
            BlockPos pos = cells.get(i).immutable();
            String type = typeKeys.get(i);
            String alias = extraAliases.getOrDefault(pos, glue.alias(pos));
            if (alias == null) {
                alias = "";
            }
            Member member = new Member(pos, type, alias, false);
            draft.add(member);
            symbolCounts.merge(member.symbol(), 1, Integer::sum);
        }

        List<Member> members = new ArrayList<>();
        Map<String, Member> bySymbol = new HashMap<>();
        for (Member raw : draft) {
            boolean conflict = symbolCounts.getOrDefault(raw.symbol(), 0) > 1;
            Member member = new Member(raw.pos(), raw.typeKey(), raw.alias(), conflict);
            members.add(member);
            bySymbol.putIfAbsent(member.symbol(), member);
        }
        return new SealSymbolTable(anchor.immutable(), List.copyOf(members), Map.copyOf(bySymbol));
    }

    public static String typeKey(BlockState state) {
        return typeKey(state.getBlock());
    }

    public static String typeKey(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null) {
            return "block";
        }
        return id.getPath().toLowerCase(Locale.ROOT);
    }

    public static String existingSource(ServerLevel level, SealSymbolTable table) {
        StringBuilder out = new StringBuilder();
        for (Member member : table.members()) {
            if (member.conflict()) {
                continue;
            }
            CompoundTag params = null;
            for (SealInstance seal : SealService.getAll(level, member.pos())) {
                if (SealProgramRuntime.isProgram(seal) && seal.params() != null) {
                    params = seal.params();
                    break;
                }
            }
            if (params == null) {
                continue;
            }
            String stanza = SealScriptPrinter.pretty(member.symbol(), params);
            if (stanza.isBlank()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append('\n');
            }
            out.append(stanza);
            if (stanza.charAt(stanza.length() - 1) != '\n') {
                out.append('\n');
            }
        }
        return out.toString();
    }
}
