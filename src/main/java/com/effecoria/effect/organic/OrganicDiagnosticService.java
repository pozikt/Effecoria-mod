package com.effecoria.effect.organic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Temporary diagnostic labels above diagnosed creatures. */
public final class OrganicDiagnosticService {
    private static final List<PendingRestore> PENDING = new ArrayList<>();

    private record PendingRestore(
            ResourceKey<Level> dimension,
            UUID entityId,
            Component previousName,
            boolean previousNameVisible,
            long expireAtGameTime) {}

    private OrganicDiagnosticService() {}

    public static void applyLabel(ServerLevel level, LivingEntity subject, int durationTicks) {
        clearLabel(subject);
        long expireAt = level.getGameTime() + Math.max(20, durationTicks);
        PENDING.add(new PendingRestore(
                level.dimension(),
                subject.getUUID(),
                subject.getCustomName(),
                subject.isCustomNameVisible(),
                expireAt));

        DiagnosticReadout readout = readoutFor(subject);
        MutableComponent label = Component.literal(
                        Math.round(subject.getHealth()) + "/" + Math.round(subject.getMaxHealth()))
                .withStyle(readout.color())
                .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable(readout.statusKey()).withStyle(readout.color()));
        if (!readout.flags().isEmpty()) {
            label.append(Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY));
            for (int i = 0; i < readout.flags().size(); i++) {
                if (i > 0) {
                    label.append(Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY));
                }
                label.append(Component.translatable(readout.flags().get(i)).withStyle(ChatFormatting.GRAY));
            }
        }

        subject.setCustomName(label);
        subject.setCustomNameVisible(true);
    }

    public static DiagnosticReadout readoutFor(LivingEntity subject) {
        float ratio = subject.getHealth() / Math.max(1f, subject.getMaxHealth());
        ChatFormatting color;
        String statusKey;
        if (ratio > 0.66f && !subject.isOnFire() && !hasSevereAffliction(subject)) {
            color = ChatFormatting.GREEN;
            statusKey = "message.effecoria.organic.diagnostic.status.healthy";
        } else if (ratio > 0.33f && subject.getHealth() > 2f) {
            color = ChatFormatting.YELLOW;
            statusKey = "message.effecoria.organic.diagnostic.status.ill";
        } else {
            color = ChatFormatting.RED;
            statusKey = "message.effecoria.organic.diagnostic.status.critical";
        }

        List<String> flags = new ArrayList<>();
        if (subject.isOnFire()) {
            flags.add("message.effecoria.organic.diagnostic.flag.burning");
        }
        if (subject.hasEffect(net.minecraft.world.effect.MobEffects.POISON)) {
            flags.add("message.effecoria.organic.diagnostic.flag.poison");
        }
        if (subject.hasEffect(net.minecraft.world.effect.MobEffects.WITHER)) {
            flags.add("message.effecoria.organic.diagnostic.flag.wither");
        }
        if (subject.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION)) {
            flags.add("message.effecoria.organic.diagnostic.flag.regen");
        }
        return new DiagnosticReadout(color, statusKey, flags, Math.round(ratio * 100f));
    }

    private static boolean hasSevereAffliction(LivingEntity subject) {
        return subject.hasEffect(net.minecraft.world.effect.MobEffects.POISON)
                || subject.hasEffect(net.minecraft.world.effect.MobEffects.WITHER)
                || subject.getAbsorptionAmount() <= 0f && subject.getHealth() < subject.getMaxHealth() * 0.4f;
    }

    public static void clearLabel(LivingEntity subject) {
        Iterator<PendingRestore> it = PENDING.iterator();
        while (it.hasNext()) {
            PendingRestore entry = it.next();
            if (entry.entityId().equals(subject.getUUID())) {
                restore(subject, entry);
                it.remove();
            }
        }
    }

    public static void tick(MinecraftServer server) {
        Iterator<PendingRestore> it = PENDING.iterator();
        while (it.hasNext()) {
            PendingRestore entry = it.next();
            ServerLevel level = server.getLevel(entry.dimension());
            if (level == null) {
                it.remove();
                continue;
            }
            if (level.getGameTime() < entry.expireAtGameTime()) {
                continue;
            }
            Entity entity = level.getEntity(entry.entityId());
            if (entity instanceof LivingEntity living) {
                restore(living, entry);
            }
            it.remove();
        }
    }

    private static void restore(LivingEntity living, PendingRestore entry) {
        living.setCustomName(entry.previousName());
        living.setCustomNameVisible(entry.previousNameVisible());
    }

    public record DiagnosticReadout(ChatFormatting color, String statusKey, List<String> flags, int healthPercent) {}
}
