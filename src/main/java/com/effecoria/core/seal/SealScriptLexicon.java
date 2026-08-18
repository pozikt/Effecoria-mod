package com.effecoria.core.seal;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Canonical names and RU/EN aliases for the seal script language. */
public final class SealScriptLexicon {
    private static final Map<String, String> TYPES = new HashMap<>();
    private static final Map<String, String> PROPERTIES = new HashMap<>();
    private static final Map<String, String> SPECS = new HashMap<>();

    static {
        type("dirt", "земля", "земляной", "грунт");
        type("stone", "камень");
        type("cobblestone", "булыжник");
        type("oak_log", "дуб", "бревно");
        type("oak_planks", "доски");
        type("glass", "стекло");
        type("iron_block", "железо");
        type("gold_block", "золото");
        type("sand", "песок");
        type("gravel", "гравий");
        type("netherrack", "незерак");
        type("obsidian", "обсидиан");

        prop("glow", "светимость", "свет", "light", "lux");
        prop("hardness", "твёрдость", "твердость", "крепкость", "firmitas");
        prop("sound", "звук");
        prop("hurt", "боль", "acies");
        prop("slow", "замедление");
        prop("push", "толчок");
        prop("calor", "жар", "огонь");
        prop("clausura", "замок");
        prop("umbra", "тень");
        prop("servare", "запрет");
        prop("extrahere", "вытягивание");
        prop("haustus", "глоток");
        prop("vigil", "страж");
        prop("imprimere", "отпечаток");
        prop("ordo", "порядок");
        prop("abnegatio", "фаза");
        prop("absolutum", "абсолют");

        spec("step", "шаг");
        spec("hit", "удар");
        spec("use", "использование", "пкм");
        spec("break", "слом", "разрушение");
        spec("approach", "приближение");
        spec("player", "игрок");
        spec("mob", "моб");
    }

    private SealScriptLexicon() {}

    public static String normalizeSymbol(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        int hash = s.indexOf('#');
        if (hash < 0) {
            return canonicalType(s);
        }
        return canonicalType(s.substring(0, hash)) + "#" + sanitizeAlias(s.substring(hash + 1));
    }

    public static String canonicalType(String raw) {
        String key = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        return TYPES.getOrDefault(key, key);
    }

    public static String canonicalProperty(String raw) {
        String key = raw.trim().toLowerCase(Locale.ROOT);
        return PROPERTIES.getOrDefault(key, key);
    }

    public static String canonicalSpec(String raw) {
        String key = raw.trim().toLowerCase(Locale.ROOT);
        return SPECS.getOrDefault(key, key);
    }

    public static boolean isWhen(String raw) {
        String key = raw.trim().toLowerCase(Locale.ROOT);
        return key.equals("when") || key.equals("когда") || key.equals("if") || key.equals("если");
    }

    public static boolean isImport(String line) {
        String key = line.trim().toLowerCase(Locale.ROOT);
        return key.equals("import glue")
                || key.equals("импорт клей")
                || key.equals("импорт glue")
                || key.startsWith("import ")
                || key.startsWith("импорт ");
    }

    public static String sanitizeAlias(String raw) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length() && out.length() < 24; i++) {
            char c = raw.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                out.append(Character.toLowerCase(c));
            }
        }
        return out.toString();
    }

    private static void type(String canonical, String... aliases) {
        TYPES.put(canonical, canonical);
        for (String alias : aliases) {
            TYPES.put(alias.toLowerCase(Locale.ROOT), canonical);
        }
    }

    private static void prop(String canonical, String... aliases) {
        PROPERTIES.put(canonical, canonical);
        for (String alias : aliases) {
            PROPERTIES.put(alias.toLowerCase(Locale.ROOT), canonical);
        }
    }

    private static void spec(String canonical, String... aliases) {
        SPECS.put(canonical, canonical);
        for (String alias : aliases) {
            SPECS.put(alias.toLowerCase(Locale.ROOT), canonical);
        }
    }
}
