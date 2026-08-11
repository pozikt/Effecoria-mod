package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.disease.DiseaseService;
import com.effecoria.core.disease.PhiDisease;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Periodically bleeds entropy and eases Ω-sickness near the anchor. */
public final class OmegaAnchorBlockEntity extends BlockEntity {
    public static final int RADIUS = 4;
    private static final int PERIOD = 40;

    public OmegaAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OMEGA_ANCHOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OmegaAnchorBlockEntity be) {
        if (!(level instanceof ServerLevel server) || server.getGameTime() % PERIOD != 0L) {
            return;
        }
        AABB box = new AABB(pos).inflate(RADIUS);
        for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, box)) {
            PlayerPsiData data = PsiHelper.get(player);
            float before = data.entropyB();
            if (before > 0f) {
                data.setEntropyB(Math.max(0f, before - 0.03f));
                PsiHelper.set(player, data);
                player.syncData(ModAttachments.PSI.get());
            }
            // Soften stage-1 Ω sickness only (does not wipe rot).
            var profile = DiseaseService.get(player);
            if (profile.has(PhiDisease.OMEGA_SICKNESS) && profile.get(PhiDisease.OMEGA_SICKNESS).stage() <= 1) {
                DiseaseService.cure(player, PhiDisease.OMEGA_SICKNESS);
            }
        }
        if (server.getGameTime() % (PERIOD * 4) == 0L) {
            server.sendParticles(
                    ParticleTypes.END_ROD,
                    pos.getX() + 0.5,
                    pos.getY() + 1.1,
                    pos.getZ() + 0.5,
                    3,
                    0.2,
                    0.15,
                    0.2,
                    0.0);
        }
    }
}
