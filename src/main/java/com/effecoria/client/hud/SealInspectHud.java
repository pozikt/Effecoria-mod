package com.effecoria.client.hud;

import java.util.ArrayList;
import java.util.List;

import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.seal.ChunkSealData;
import com.effecoria.core.seal.SealInstance;
import com.effecoria.core.seal.SealLayer;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Crosshair readout of synced seal layers — builder feedback for chunk seal attachment. */
public final class SealInspectHud {
    private SealInspectHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) {
            return;
        }
        if (minecraft.screen != null) {
            return;
        }
        PlayerPsiData data = minecraft.player.getData(ModAttachments.PSI.get());
        if (!data.initiated()) {
            return;
        }
        HitResult hit = minecraft.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = blockHit.getBlockPos();
        Level level = minecraft.level;
        if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return;
        }
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkSealData seals = chunk.getData(ModAttachments.CHUNK_SEALS.get());
        List<SealInstance> layers = seals.getAll(pos);
        if (layers.isEmpty()) {
            return;
        }
        long gameTime = level.getGameTime();
        List<Component> names = new ArrayList<>();
        for (SealInstance seal : layers) {
            if (seal.isExpired(gameTime)) {
                continue;
            }
            names.add(displayName(seal.typeId()));
        }
        if (names.isEmpty()) {
            return;
        }

        Component joined = Component.empty();
        net.minecraft.network.chat.MutableComponent lineParts = Component.empty();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                lineParts.append(Component.literal(" · "));
            }
            lineParts.append(names.get(i));
        }
        joined = lineParts;
        boolean hasOffensive = layers.stream()
                .anyMatch(s -> !s.isExpired(gameTime) && SealLayer.isOffensive(s.typeId()));
        Component line = Component.translatable(
                hasOffensive ? "hud.effecoria.seal_inspect" : "hud.effecoria.seal_inspect_utility",
                joined);

        int width = minecraft.font.width(line);
        int x = (minecraft.getWindow().getGuiScaledWidth() - width) / 2;
        int y = minecraft.getWindow().getGuiScaledHeight() / 2 + 18;
        graphics.drawString(minecraft.font, line, x, y, 0xE8D4A8);
    }

    private static Component displayName(ResourceLocation typeId) {
        return Component.translatable("seal.effecoria." + typeId.getPath());
    }
}
