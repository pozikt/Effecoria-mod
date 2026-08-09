package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.world.OmegaScarService;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Ambient purple crack-smoke and dusk fog tint inside Ω-Scar. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientOmegaScarEffects {
    private ClientOmegaScarEffects() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null || minecraft.isPaused()) {
            return;
        }
        if (!OmegaScarService.isIn(level, player.position()) || player.tickCount % 2 != 0) {
            return;
        }
        RandomSource random = level.random;
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        // Black dust + purple miasma rising from cracks.
        for (int i = 0; i < 5; i++) {
            double px = x + (random.nextDouble() - 0.5) * 12.0;
            double pz = z + (random.nextDouble() - 0.5) * 12.0;
            double py = y + 0.1 + random.nextDouble() * 0.4;
            level.addParticle(
                    ModParticleTypes.CORRUPTION_MIASMA.get(),
                    px,
                    py,
                    pz,
                    (random.nextDouble() - 0.5) * 0.02,
                    0.04 + random.nextDouble() * 0.06,
                    (random.nextDouble() - 0.5) * 0.02);
        }
        if (player.tickCount % 6 == 0) {
            for (int i = 0; i < 3; i++) {
                level.addParticle(
                        ModParticleTypes.CORRUPTION_ENTROPY.get(),
                        x + (random.nextDouble() - 0.5) * 10.0,
                        y + 1.0 + random.nextDouble() * 3.0,
                        z + (random.nextDouble() - 0.5) * 10.0,
                        0.0,
                        0.01,
                        0.0);
            }
        }
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null || !OmegaScarService.isIn(level, player.position())) {
            return;
        }
        float night = level.isDay() ? 0.35f : 0.65f;
        event.setRed(Mth.lerp(night, event.getRed(), 0.06f));
        event.setGreen(Mth.lerp(night, event.getGreen(), 0.02f));
        event.setBlue(Mth.lerp(night, event.getBlue(), 0.10f));
    }
}
