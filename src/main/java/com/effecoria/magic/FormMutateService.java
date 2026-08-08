package com.effecoria.magic;

import com.effecoria.core.magic.SpellDefinition;
import com.effecoria.core.magic.SpellEffectEntry;
import com.effecoria.effect.elemental.SteamCloudService;
import com.effecoria.entity.RootCageEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Recast-mutate for lingering spell forms (steam veil, root bind).
 * Returns {@link Result#NONE} when no owned form exists so the normal cast proceeds.
 */
public final class FormMutateService {
    public static final float MUTATE_COST_FACTOR = 0.5f;
    public static final float MUTATE_ENTROPY_FACTOR = 0.6f;
    private static final double ROOT_SEARCH = 24.0;
    private static final int STEAM_REFRESH_TICKS = 80;
    private static final int ROOT_BONUS_LIFE = 40;

    public enum Result {
        /** No eligible form — proceed with normal spell cast. */
        NONE,
        /** Form mutated; skip SpellEffectExecutor.applyAll. */
        HANDLED,
        /** Form exists but mutate failed (e.g. too far); abort cast. */
        FAILED
    }

    private FormMutateService() {}

    public static Result tryMutate(ServerPlayer player, SpellDefinition spell, float chargeScale) {
        if (hasEffect(spell, "steam_veil") || "steam_veil".equals(spell.id().getPath())) {
            return mutateSteamVeil(player, chargeScale);
        }
        if (hasEffect(spell, "root_bind") || "root_bind".equals(spell.id().getPath())) {
            return mutateRootBind(player, spell, chargeScale);
        }
        return Result.NONE;
    }

    private static boolean hasEffect(SpellDefinition spell, String path) {
        for (SpellEffectEntry effect : spell.effects()) {
            if (path.equals(effect.type().getPath())) {
                return true;
            }
        }
        return false;
    }

    private static Result mutateSteamVeil(ServerPlayer player, float chargeScale) {
        ServerLevel level = player.serverLevel();
        if (!SteamCloudService.hasOwned(level, player.getUUID())) {
            return Result.NONE;
        }
        Vec3 from = player.position().add(0, 1.0, 0);
        SteamCloudService.SteamCloud nearest = SteamCloudService.findNearestOwned(level, player.getUUID(), from);
        if (nearest == null) {
            return Result.NONE;
        }
        if (!SteamCloudService.isInMutateRange(nearest, from)) {
            player.displayClientMessage(Component.translatable("message.effecoria.form_mutate_too_far"), true);
            return Result.FAILED;
        }
        SteamCloudService.Mode mode =
                player.isShiftKeyDown() ? SteamCloudService.Mode.FROST : SteamCloudService.Mode.SCALDING;
        int refresh = Math.round(STEAM_REFRESH_TICKS * (0.85f + 0.3f * chargeScale));
        if (!SteamCloudService.mutateOwned(level, player.getUUID(), from, mode, refresh)) {
            player.displayClientMessage(Component.translatable("message.effecoria.form_mutate_too_far"), true);
            return Result.FAILED;
        }
        level.playSound(
                null,
                player.blockPosition(),
                mode == SteamCloudService.Mode.FROST ? SoundEvents.GLASS_BREAK : SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS,
                0.85f,
                mode == SteamCloudService.Mode.FROST ? 1.4f : 0.65f);
        String key = mode == SteamCloudService.Mode.FROST
                ? "message.effecoria.form_mutate_steam_frost"
                : "message.effecoria.form_mutate_steam_scald";
        player.displayClientMessage(Component.translatable(key), true);
        return Result.HANDLED;
    }

    private static Result mutateRootBind(ServerPlayer player, SpellDefinition spell, float chargeScale) {
        ServerLevel level = player.serverLevel();
        Vec3 from = player.position();
        if (!RootCageEntity.hasOwnedNear(level, player.getUUID(), from, ROOT_SEARCH)) {
            return Result.NONE;
        }
        double range = resolveRootRange(spell);
        LivingEntity look = CastAim.raycastLiving(player, range);
        RootCageEntity cage = RootCageEntity.findOwnedNear(level, player.getUUID(), from, range, look);
        if (cage == null) {
            // Owned cage exists elsewhere but out of mutate range
            player.displayClientMessage(Component.translatable("message.effecoria.form_mutate_too_far"), true);
            return Result.FAILED;
        }
        int bonus = Math.round(ROOT_BONUS_LIFE * (0.85f + 0.4f * chargeScale));
        if (!cage.empower(bonus)) {
            player.displayClientMessage(Component.translatable("message.effecoria.form_mutate_failed"), true);
            return Result.FAILED;
        }
        player.displayClientMessage(Component.translatable("message.effecoria.form_mutate_root_venom"), true);
        return Result.HANDLED;
    }

    private static double resolveRootRange(SpellDefinition spell) {
        for (SpellEffectEntry effect : spell.effects()) {
            if ("root_bind".equals(effect.type().getPath()) && effect.params().has("range")) {
                return effect.params().get("range").getAsDouble() + 2.0;
            }
        }
        return 10.0;
    }
}
