package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.PhiSample;
import com.effecoria.core.phi.PhiFieldService;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Client visuals while phi-sense is active — motes on Φ gradients and living targets. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientPhiSenseEffects {
    private static final double ENTITY_RADIUS = 32;
    private static final int BLOCK_RING = 5;

    private ClientPhiSenseEffects() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        if (!PsiHelper.get(minecraft.player).isPhiSenseActive(minecraft.level.getGameTime())) {
            return;
        }
        if (minecraft.player.tickCount % 4 != 0) {
            return;
        }
        Level level = minecraft.level;
        Vec3 eye = minecraft.player.getEyePosition();
        highlightLiving(level, minecraft.player, eye);
        if (minecraft.player.tickCount % 12 == 0) {
            sampleBlockGradients(level, minecraft.player, eye);
        }
    }

    private static void highlightLiving(Level level, net.minecraft.world.entity.player.Player viewer, Vec3 eye) {
        AABB box = viewer.getBoundingBox().inflate(ENTITY_RADIUS);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == viewer) {
                continue;
            }
            if (entity.distanceToSqr(viewer) > ENTITY_RADIUS * ENTITY_RADIUS) {
                continue;
            }
            Vec3 at = entity.getEyePosition();
            PhiSample sample = PhiFieldService.sample(level, at, viewer);
            if (sample.zeroFlux()) {
                level.addParticle(ModParticleTypes.CORRUPTION_RUNE.get(), at.x, at.y, at.z, 0, 0.02, 0);
            } else if (sample.value() >= 1.05f) {
                level.addParticle(ModParticleTypes.PHI_SPARK.get(), at.x, at.y, at.z, 0, 0.03, 0);
                level.addParticle(ModParticleTypes.SPATIAL_WARP.get(), at.x, at.y - 0.2, at.z, 0, 0.01, 0);
            } else if (sample.value() <= 0.55f) {
                level.addParticle(ModParticleTypes.NECRO_SHADOW.get(), at.x, at.y, at.z, 0, 0.015, 0);
            } else {
                level.addParticle(ModParticleTypes.PHI_SPARK.get(), at.x, at.y, at.z, 0, 0.015, 0);
            }
        }
    }

    private static void sampleBlockGradients(Level level, net.minecraft.world.entity.player.Player viewer, Vec3 eye) {
        BlockPos center = BlockPos.containing(eye);
        for (int dx = -BLOCK_RING; dx <= BLOCK_RING; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -BLOCK_RING; dz <= BLOCK_RING; dz++) {
                    if ((dx * dx + dz * dz) > BLOCK_RING * BLOCK_RING) {
                        continue;
                    }
                    BlockPos pos = center.offset(dx, dy, dz);
                    PhiSample sample = PhiFieldService.sample(level, Vec3.atCenterOf(pos), viewer);
                    if (sample.zeroFlux()) {
                        continue;
                    }
                    if (sample.value() < 0.45f || sample.value() > 1.15f) {
                        double x = pos.getX() + 0.5;
                        double y = pos.getY() + 0.55;
                        double z = pos.getZ() + 0.5;
                        level.addParticle(ModParticleTypes.PHI_SPARK.get(), x, y, z, 0, 0.008, 0);
                    }
                }
            }
        }
    }
}
