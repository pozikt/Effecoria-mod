package com.effecoria.core.seal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Evaluates compiled seal programs: passive properties, sense pulses, timed overlays.
 */
public final class SealProgramRuntime {
    public static final String RT_SENSE = "sense_on";
    public static final String RT_TIMED = "timed";
    public static final String RT_UNTIL = "until";

    /** Event kinds a sense may match; empty set means "any". */
    public enum SenseEvent {
        APPROACH,
        STEP,
        HIT,
        USE,
        BREAK
    }

    private SealProgramRuntime() {}

    public static boolean isProgram(SealInstance seal) {
        return seal != null && seal.typeId().equals(SealTypes.PROGRAM) && seal.params() != null;
    }

    public static float effectiveHardness(SealInstance seal, long gameTime) {
        if (!isProgram(seal)) {
            return 0f;
        }
        migrateV1(seal.params());
        float best = maxPassive(seal.params(), "hardness");
        best = Math.max(best, maxTimed(seal.params(), "hardness", gameTime));
        if (best <= 0f && seal.params().contains(SealProgramCompiler.HARDNESS_MULT)) {
            best = seal.params().getFloat(SealProgramCompiler.HARDNESS_MULT);
        }
        return best;
    }

    public static int effectiveGlow(SealInstance seal, long gameTime) {
        if (!isProgram(seal)) {
            return 0;
        }
        migrateV1(seal.params());
        float best = maxPassive(seal.params(), "glow");
        best = Math.max(best, maxPassive(seal.params(), "light"));
        best = Math.max(best, maxTimed(seal.params(), "glow", gameTime));
        best = Math.max(best, maxTimed(seal.params(), "light", gameTime));
        if (best <= 0f && seal.params().contains(SealProgramCompiler.GLOW_LEVEL)) {
            best = seal.params().getInt(SealProgramCompiler.GLOW_LEVEL);
        }
        return Math.round(best);
    }

    public static float effectiveHurt(SealInstance seal, long gameTime) {
        if (!isProgram(seal)) {
            return 0f;
        }
        migrateV1(seal.params());
        float best = maxPassive(seal.params(), "hurt");
        best = Math.max(best, maxTimed(seal.params(), "hurt", gameTime));
        if (best <= 0f && seal.params().contains(SealProgramCompiler.HURT_DAMAGE)) {
            best = seal.params().getFloat(SealProgramCompiler.HURT_DAMAGE);
        }
        float acies = Math.max(maxPassive(seal.params(), "acies"), maxTimed(seal.params(), "acies", gameTime));
        if (acies > 0f && best > 0f) {
            best *= 1f + 0.25f * acies;
        } else if (acies > 0f) {
            best = acies;
        }
        return best;
    }

    public static int effectiveSlow(SealInstance seal, long gameTime) {
        if (!isProgram(seal)) {
            return 0;
        }
        migrateV1(seal.params());
        float best = maxPassive(seal.params(), "slow");
        best = Math.max(best, maxTimed(seal.params(), "slow", gameTime));
        if (best <= 0f && seal.params().contains(SealProgramCompiler.SLOW_AMP)) {
            best = seal.params().getInt(SealProgramCompiler.SLOW_AMP);
        }
        return Math.round(best);
    }

    public static float effectivePush(SealInstance seal, long gameTime) {
        if (!isProgram(seal)) {
            return 0f;
        }
        migrateV1(seal.params());
        float best = maxPassive(seal.params(), "push");
        best = Math.max(best, maxTimed(seal.params(), "push", gameTime));
        if (best <= 0f && seal.params().contains(SealProgramCompiler.PUSH_FORCE)) {
            best = seal.params().getFloat(SealProgramCompiler.PUSH_FORCE);
        }
        return best > 0f ? Mth.clamp(0.45f * best, 0.3f, 2.5f) : 0f;
    }

    public static boolean effectiveClausura(SealInstance seal, long gameTime) {
        return maxPassiveOrTimed(seal, "clausura", gameTime) > 0f;
    }

    public static boolean effectiveUmbra(SealInstance seal, long gameTime) {
        return maxPassiveOrTimed(seal, "umbra", gameTime) > 0f;
    }

    public static boolean effectiveServare(SealInstance seal, long gameTime) {
        return maxPassiveOrTimed(seal, "servare", gameTime) > 0f;
    }

    public static float effectiveCalor(SealInstance seal, long gameTime) {
        return maxPassiveOrTimed(seal, "calor", gameTime);
    }

    public static boolean effectiveAbnegatio(SealInstance seal, long gameTime) {
        return maxPassiveOrTimed(seal, "abnegatio", gameTime) > 0f;
    }

    private static float maxPassiveOrTimed(SealInstance seal, String effect, long gameTime) {
        if (!isProgram(seal)) {
            return 0f;
        }
        migrateV1(seal.params());
        return Math.max(maxPassive(seal.params(), effect), maxTimed(seal.params(), effect, gameTime));
    }

    /**
     * Tick reactive rules near a block. {@code event} is the concrete world event (or APPROACH for proximity).
     * Returns true if a pulse fired.
     */
    public static boolean pulse(
            ServerLevel level,
            BlockPos pos,
            SealInstance seal,
            long gameTime,
            SenseEvent event,
            LivingEntity subject) {
        if (!isProgram(seal)) {
            return false;
        }
        CompoundTag params = seal.params();
        migrateV1(params);
        if (!params.contains(SealProgramCompiler.RULES_TAG, Tag.TAG_LIST)) {
            return false;
        }

        CompoundTag rt = params.contains(SealProgramCompiler.RUNTIME_TAG)
                ? params.getCompound(SealProgramCompiler.RUNTIME_TAG)
                : new CompoundTag();
        purgeTimed(rt, gameTime);

        ListTag rules = params.getList(SealProgramCompiler.RULES_TAG, Tag.TAG_COMPOUND);
        boolean anyFired = false;
        for (int i = 0; i < rules.size(); i++) {
            CompoundTag rule = rules.getCompound(i);
            boolean matches = senseMatches(level, pos, rule, event, subject);
            String key = "s" + i;
            String timeKey = key + "_t";
            boolean wasOn = rt.getBoolean(key);
            if (!matches) {
                rt.putBoolean(key, false);
                continue;
            }
            boolean rising = !wasOn;
            boolean dwell =
                    event == SenseEvent.STEP && wasOn && gameTime - rt.getLong(timeKey) >= 15;
            if (rising || dwell) {
                fireRule(level, pos, seal, rule, gameTime, rt, key);
                rt.putLong(timeKey, gameTime);
                anyFired = true;
            }
            rt.putBoolean(key, true);
        }
        params.put(SealProgramCompiler.RUNTIME_TAG, rt);
        return anyFired;
    }

    /** Clears rising-edge latch so the next enter / step can fire again. */
    public static void clearSenseFlags(SealInstance seal) {
        if (!isProgram(seal)) {
            return;
        }
        CompoundTag params = seal.params();
        migrateV1(params);
        if (!params.contains(SealProgramCompiler.RULES_TAG, Tag.TAG_LIST)) {
            return;
        }
        int ruleCount = params.getList(SealProgramCompiler.RULES_TAG, Tag.TAG_COMPOUND).size();
        clearSenseFlags(params, ruleCount);
    }

    /** Continuous proximity scan for APPROACH senses (edge when someone enters radius). */
    public static void tickApproach(ServerLevel level, BlockPos pos, SealInstance seal, long gameTime) {
        if (!isProgram(seal)) {
            return;
        }
        CompoundTag params = seal.params();
        migrateV1(params);
        if (!params.contains(SealProgramCompiler.RULES_TAG, Tag.TAG_LIST)) {
            return;
        }
        ListTag rules = params.getList(SealProgramCompiler.RULES_TAG, Tag.TAG_COMPOUND);
        LivingEntity subject = null;
        boolean any = false;
        for (int i = 0; i < rules.size(); i++) {
            CompoundTag rule = rules.getCompound(i);
            Set<String> specs = readSpecs(rule);
            if (!specs.isEmpty() && !specs.contains("approach") && !specs.contains("player") && !specs.contains("mob")
                    && (specs.contains("step") || specs.contains("hit") || specs.contains("use") || specs.contains("break"))) {
                continue;
            }
            float radius = rule.contains(SealProgramCompiler.RADIUS) ? rule.getFloat(SealProgramCompiler.RADIUS) : 6f;
            AABB box = new AABB(pos).inflate(radius);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                if (entityMatchesSpecs(entity, specs)) {
                    subject = entity;
                    any = true;
                    break;
                }
            }
            if (any) {
                break;
            }
        }
        if (any) {
            pulse(level, pos, seal, gameTime, SenseEvent.APPROACH, subject);
        } else {
            clearApproachSenseFlags(params, rules);
        }
    }

    private static void clearApproachSenseFlags(CompoundTag params, ListTag rules) {
        CompoundTag rt = params.contains(SealProgramCompiler.RUNTIME_TAG)
                ? params.getCompound(SealProgramCompiler.RUNTIME_TAG)
                : new CompoundTag();
        for (int i = 0; i < rules.size(); i++) {
            Set<String> specs = readSpecs(rules.getCompound(i));
            if (eventAllowed(specs, SenseEvent.APPROACH)) {
                rt.putBoolean("s" + i, false);
            }
        }
        params.put(SealProgramCompiler.RUNTIME_TAG, rt);
    }

    private static void clearSenseFlags(CompoundTag params, int ruleCount) {
        CompoundTag rt = params.contains(SealProgramCompiler.RUNTIME_TAG)
                ? params.getCompound(SealProgramCompiler.RUNTIME_TAG)
                : new CompoundTag();
        for (int i = 0; i < ruleCount; i++) {
            rt.putBoolean("s" + i, false);
        }
        params.put(SealProgramCompiler.RUNTIME_TAG, rt);
    }

    private static boolean senseMatches(
            ServerLevel level, BlockPos pos, CompoundTag rule, SenseEvent event, LivingEntity subject) {
        Set<String> specs = readSpecs(rule);
        if (!eventAllowed(specs, event)) {
            return false;
        }
        if (subject != null && !entityMatchesSpecs(subject, specs)) {
            return false;
        }
        float radius = rule.contains(SealProgramCompiler.RADIUS) ? rule.getFloat(SealProgramCompiler.RADIUS) : 6f;
        if (event == SenseEvent.APPROACH) {
            return subject != null
                    && subject.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                    <= radius * radius;
        }
        return true;
    }

    private static boolean eventAllowed(Set<String> specs, SenseEvent event) {
        boolean hasEventFilter = specs.contains("approach")
                || specs.contains("step")
                || specs.contains("hit")
                || specs.contains("use")
                || specs.contains("break");
        if (!hasEventFilter) {
            return true;
        }
        return switch (event) {
            case APPROACH -> specs.contains("approach");
            case STEP -> specs.contains("step");
            case HIT -> specs.contains("hit");
            case USE -> specs.contains("use");
            case BREAK -> specs.contains("break");
        };
    }

    private static boolean entityMatchesSpecs(LivingEntity entity, Set<String> specs) {
        boolean wantPlayer = specs.contains("player");
        boolean wantMob = specs.contains("mob");
        if (!wantPlayer && !wantMob) {
            return true;
        }
        if (entity instanceof Player) {
            return wantPlayer;
        }
        return wantMob;
    }

    private static Set<String> readSpecs(CompoundTag rule) {
        Set<String> out = new HashSet<>();
        if (!rule.contains(SealProgramCompiler.SPECS, Tag.TAG_LIST)) {
            return out;
        }
        ListTag list = rule.getList(SealProgramCompiler.SPECS, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            out.add(list.getString(i));
        }
        return out;
    }

    private static void fireRule(
            ServerLevel level,
            BlockPos pos,
            SealInstance seal,
            CompoundTag rule,
            long gameTime,
            CompoundTag rt,
            String key) {
        int untilMax = rule.contains(SealProgramCompiler.UNTIL_MAX) ? rule.getInt(SealProgramCompiler.UNTIL_MAX) : 0;
        int fired = rt.getInt(key + "_fc");
        if (untilMax > 0 && fired >= untilMax) {
            return;
        }

        int countNeed = rule.contains(SealProgramCompiler.COUNT_NEED) ? rule.getInt(SealProgramCompiler.COUNT_NEED) : 0;
        int count = rt.getInt(key + "_cnt") + 1;
        rt.putInt(key + "_cnt", count);

        boolean ready = countNeed <= 0 || count >= countNeed;
        if (!ready) {
            fireActions(level, pos, seal, rule, gameTime, rt, SealProgramCompiler.ELSE_ACTIONS);
            return;
        }
        if (countNeed > 0) {
            rt.putInt(key + "_cnt", 0);
        }
        fireActions(level, pos, seal, rule, gameTime, rt, SealProgramCompiler.ACTIONS);
        rt.putInt(key + "_fc", fired + 1);
    }

    private static void fireActions(
            ServerLevel level,
            BlockPos pos,
            SealInstance seal,
            CompoundTag rule,
            long gameTime,
            CompoundTag rt,
            String listKey) {
        if (!rule.contains(listKey, Tag.TAG_LIST)) {
            return;
        }
        ListTag actions = rule.getList(listKey, Tag.TAG_COMPOUND);
        if (actions.isEmpty()) {
            return;
        }
        ListTag timed = rt.contains(RT_TIMED, Tag.TAG_LIST) ? rt.getList(RT_TIMED, Tag.TAG_COMPOUND) : new ListTag();
        for (int i = 0; i < actions.size(); i++) {
            CompoundTag action = actions.getCompound(i);
            int loops = action.contains(SealProgramCompiler.LOOP_REPEATS)
                    ? Mth.clamp(action.getInt(SealProgramCompiler.LOOP_REPEATS), 1, 10)
                    : 1;
            for (int r = 0; r < loops; r++) {
                dispatchAction(level, pos, seal, action, gameTime, timed);
            }
        }
        rt.put(RT_TIMED, timed);
    }

    private static void dispatchAction(
            ServerLevel level,
            BlockPos pos,
            SealInstance seal,
            CompoundTag action,
            long gameTime,
            ListTag timed) {
        String effect = action.getString(SealProgramCompiler.EFFECT);
        float mag = action.getFloat(SealProgramCompiler.MAGNITUDE);
        int dur = action.contains(SealProgramCompiler.DURATION_TICKS)
                ? action.getInt(SealProgramCompiler.DURATION_TICKS)
                : 0;

        if (dur > 0 && isOverlayEffect(effect)) {
            CompoundTag overlay = action.copy();
            overlay.putLong(RT_UNTIL, gameTime + dur);
            timed.add(overlay);
            return;
        }

        switch (effect) {
            case "sound" -> playSound(level, pos, action);
            case "hurt", "acies" -> SealProgramEffects.hurtOnce(level, pos, seal, mag);
            case "slow" -> SealProgramEffects.slowOnce(level, pos, seal, Math.round(mag));
            case "push" -> SealProgramEffects.pushOnce(level, pos, seal, Mth.clamp(0.45f * mag, 0.3f, 2.5f));
            case "hardness", "glow", "light", "clausura", "umbra", "servare", "abnegatio" -> {
                CompoundTag overlay = action.copy();
                overlay.putLong(RT_UNTIL, gameTime + (dur > 0 ? dur : 40));
                timed.add(overlay);
            }
            case "calor" -> {
                CompoundTag overlay = action.copy();
                overlay.putLong(RT_UNTIL, gameTime + (dur > 0 ? dur : 40));
                timed.add(overlay);
                SealProgramEffects.calorOnce(level, pos, seal, mag);
            }
            case "extrahere" -> SealProgramEffects.extrahereOnce(level, pos, seal, mag);
            case "haustus" -> SealProgramEffects.haustusOnce(level, pos, seal, mag);
            case "vigil" -> SealProgramEffects.vigilOnce(level, pos, seal, mag);
            case "imprimere" -> SealProgramEffects.imprimereOnce(level, pos, seal, mag);
            case "ordo" -> SealProgramEffects.ordoOnce(level, pos, seal, mag);
            case "absolutum" -> SealProgramEffects.absolutumOnce(level, pos, seal, mag);
            default -> {
            }
        }
    }

    private static boolean isOverlayEffect(String effect) {
        return "hardness".equals(effect)
                || "glow".equals(effect)
                || "light".equals(effect)
                || "hurt".equals(effect)
                || "acies".equals(effect)
                || "slow".equals(effect)
                || "push".equals(effect)
                || "clausura".equals(effect)
                || "umbra".equals(effect)
                || "servare".equals(effect)
                || "abnegatio".equals(effect)
                || "calor".equals(effect);
    }

    private static void playSound(ServerLevel level, BlockPos pos, CompoundTag action) {
        String id = action.contains(SealProgramCompiler.SOUND_EVENT)
                ? action.getString(SealProgramCompiler.SOUND_EVENT)
                : "minecraft:block.note_block.pling";
        float vol = action.contains(SealProgramCompiler.SOUND_VOLUME)
                ? action.getFloat(SealProgramCompiler.SOUND_VOLUME)
                : 0.55f;
        Optional<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.parse(id));
        sound.ifPresent(s -> level.playSound(null, pos, s, SoundSource.BLOCKS, vol, 1.0f));
    }

    private static float maxPassive(CompoundTag params, String effect) {
        float best = 0f;
        if (!params.contains(SealProgramCompiler.PASSIVES_TAG, Tag.TAG_LIST)) {
            return best;
        }
        ListTag list = params.getList(SealProgramCompiler.PASSIVES_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag a = list.getCompound(i);
            if (effect.equals(a.getString(SealProgramCompiler.EFFECT))) {
                best = Math.max(best, a.getFloat(SealProgramCompiler.MAGNITUDE));
            }
        }
        return best;
    }

    private static float maxTimed(CompoundTag params, String effect, long gameTime) {
        if (!params.contains(SealProgramCompiler.RUNTIME_TAG)) {
            return 0f;
        }
        CompoundTag rt = params.getCompound(SealProgramCompiler.RUNTIME_TAG);
        purgeTimed(rt, gameTime);
        params.put(SealProgramCompiler.RUNTIME_TAG, rt);
        if (!rt.contains(RT_TIMED, Tag.TAG_LIST)) {
            return 0f;
        }
        float best = 0f;
        ListTag timed = rt.getList(RT_TIMED, Tag.TAG_COMPOUND);
        for (int i = 0; i < timed.size(); i++) {
            CompoundTag a = timed.getCompound(i);
            if (effect.equals(a.getString(SealProgramCompiler.EFFECT)) && a.getLong(RT_UNTIL) > gameTime) {
                best = Math.max(best, a.getFloat(SealProgramCompiler.MAGNITUDE));
            }
        }
        return best;
    }

    private static void purgeTimed(CompoundTag rt, long gameTime) {
        if (!rt.contains(RT_TIMED, Tag.TAG_LIST)) {
            return;
        }
        ListTag timed = rt.getList(RT_TIMED, Tag.TAG_COMPOUND);
        ListTag next = new ListTag();
        for (int i = 0; i < timed.size(); i++) {
            CompoundTag a = timed.getCompound(i);
            if (a.getLong(RT_UNTIL) > gameTime) {
                next.add(a);
            }
        }
        rt.put(RT_TIMED, next);
    }

    /** One-shot convert of legacy flat v1 keys into passives so old blocks keep working. */
    public static void migrateV1(CompoundTag params) {
        if (params == null) {
            return;
        }
        if (params.contains(SealProgramCompiler.PASSIVES_TAG) || params.contains(SealProgramCompiler.RULES_TAG)) {
            if (!params.contains(SealProgramCompiler.VERSION_TAG)) {
                params.putInt(SealProgramCompiler.VERSION_TAG, SealProgramCompiler.PROGRAM_VERSION);
            }
            return;
        }
        ListTag passives = new ListTag();
        if (params.contains(SealProgramCompiler.HARDNESS_MULT)) {
            passives.add(flat("hardness", params.getFloat(SealProgramCompiler.HARDNESS_MULT)));
        }
        if (params.contains(SealProgramCompiler.GLOW_LEVEL)) {
            passives.add(flat("glow", params.getInt(SealProgramCompiler.GLOW_LEVEL)));
        }
        if (params.contains(SealProgramCompiler.HURT_DAMAGE)) {
            passives.add(flat("hurt", params.getFloat(SealProgramCompiler.HURT_DAMAGE)));
        }
        if (params.contains(SealProgramCompiler.SLOW_AMP)) {
            passives.add(flat("slow", params.getInt(SealProgramCompiler.SLOW_AMP)));
        }
        if (params.contains(SealProgramCompiler.PUSH_FORCE)) {
            passives.add(flat("push", params.getFloat(SealProgramCompiler.PUSH_FORCE) / 0.45f));
        }
        if (params.contains(SealProgramCompiler.SOUND_EVENT)) {
            CompoundTag sound = flat("sound", 1f);
            sound.putString(SealProgramCompiler.SOUND_EVENT, params.getString(SealProgramCompiler.SOUND_EVENT));
            passives.add(sound);
        }
        if (params.contains(SealProgramCompiler.SEE_RADIUS)) {
            CompoundTag rule = new CompoundTag();
            rule.putString(SealProgramCompiler.SENSE, "see");
            rule.putFloat(SealProgramCompiler.RADIUS, params.getFloat(SealProgramCompiler.SEE_RADIUS));
            rule.put(SealProgramCompiler.SPECS, new ListTag());
            ListTag actions = new ListTag();
            CompoundTag pulse = flat("glow", 8f);
            pulse.putInt(SealProgramCompiler.DURATION_TICKS, 40);
            actions.add(pulse);
            rule.put(SealProgramCompiler.ACTIONS, actions);
            ListTag rules = new ListTag();
            rules.add(rule);
            params.put(SealProgramCompiler.RULES_TAG, rules);
        }
        params.put(SealProgramCompiler.PASSIVES_TAG, passives);
        params.putInt(SealProgramCompiler.VERSION_TAG, SealProgramCompiler.PROGRAM_VERSION);
    }

    private static CompoundTag flat(String effect, float mag) {
        CompoundTag tag = new CompoundTag();
        tag.putString(SealProgramCompiler.EFFECT, effect);
        tag.putFloat(SealProgramCompiler.MAGNITUDE, mag);
        return tag;
    }
}
