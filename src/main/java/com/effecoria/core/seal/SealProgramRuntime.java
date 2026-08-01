package com.effecoria.core.seal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
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

    /**
     * Tick reactive rules near a block. {@code event} is the concrete world event (or APPROACH for proximity).
     * Returns true if a rising-edge pulse fired.
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
            boolean wasOn = rt.getBoolean(key);
            rt.putBoolean(key, matches);
            if (matches && !wasOn) {
                fireActions(level, pos, seal, rule, gameTime, rt);
                anyFired = true;
            } else if (!matches) {
                rt.putBoolean(key, false);
            }
        }
        params.put(SealProgramCompiler.RUNTIME_TAG, rt);
        return anyFired;
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
                // Pure interaction specs — approach tick does not arm them.
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
            clearSenseFlags(params, rules.size());
        }
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

    private static void fireActions(
            ServerLevel level, BlockPos pos, SealInstance seal, CompoundTag rule, long gameTime, CompoundTag rt) {
        ListTag actions = rule.getList(SealProgramCompiler.ACTIONS, Tag.TAG_COMPOUND);
        ListTag timed = rt.contains(RT_TIMED, Tag.TAG_LIST) ? rt.getList(RT_TIMED, Tag.TAG_COMPOUND) : new ListTag();
        for (int i = 0; i < actions.size(); i++) {
            CompoundTag action = actions.getCompound(i);
            String effect = action.getString(SealProgramCompiler.EFFECT);
            float mag = action.getFloat(SealProgramCompiler.MAGNITUDE);
            int dur = action.contains(SealProgramCompiler.DURATION_TICKS)
                    ? action.getInt(SealProgramCompiler.DURATION_TICKS)
                    : 0;

            if (dur > 0 && ("hardness".equals(effect) || "glow".equals(effect) || "light".equals(effect)
                    || "hurt".equals(effect) || "slow".equals(effect) || "push".equals(effect))) {
                CompoundTag overlay = action.copy();
                overlay.putLong(RT_UNTIL, gameTime + dur);
                timed.add(overlay);
                continue;
            }

            switch (effect) {
                case "sound" -> playSound(level, pos, action);
                case "hurt" -> SealProgramEffects.hurtOnce(level, pos, seal, mag);
                case "slow" -> SealProgramEffects.slowOnce(level, pos, seal, Math.round(mag));
                case "push" -> SealProgramEffects.pushOnce(level, pos, seal, Mth.clamp(0.45f * mag, 0.3f, 2.5f));
                case "hardness", "glow", "light" -> {
                    // Instant without duration: brief pulse overlay (20 ticks).
                    CompoundTag overlay = action.copy();
                    overlay.putLong(RT_UNTIL, gameTime + 40);
                    timed.add(overlay);
                }
                default -> {
                }
            }
        }
        rt.put(RT_TIMED, timed);
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
