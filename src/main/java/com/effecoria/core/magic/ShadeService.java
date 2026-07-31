package com.effecoria.core.magic;

import com.effecoria.effect.necromancy.NecroSummonService;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Vex;

/** Player-summoned vex shades — permanent thralls via {@link NecroSummonService}. */
public final class ShadeService {
    public static final String OWNER_TAG = NecroSummonService.OWNER_TAG;
    public static final String TARGET_TAG = NecroSummonService.TARGET_TAG;

    private ShadeService() {}

    public static void registerShade(Vex shade, ServerPlayer owner, LivingEntity target) {
        NecroSummonService.register(shade, owner, target);
    }

    /** Covered by {@link NecroSummonService#tick}. */
    public static void tick(ServerPlayer owner) {}
}
