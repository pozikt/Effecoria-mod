package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.effect.necromancy.DeathMarkService;
import com.effecoria.effect.organic.OrganicDiagnosticService;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;

/** Hides sensitive name tags (organic diagnostics, foreign death marks) from the wrong viewers. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientNameTagEvents {
    private ClientNameTagEvents() {}

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        Entity entity = event.getEntity();
        CompoundTag data = entity.getPersistentData();

        if (data.getBoolean(OrganicDiagnosticService.DIAG_FLAG_TAG)
                && PsiHelper.get(player).school() != MagicSchool.ORGANIC) {
            event.setCanRender(TriState.FALSE);
            return;
        }

        if (DeathMarkService.isWorldMark(entity)) {
            if (!data.hasUUID(DeathMarkService.MARK_OWNER_TAG)
                    || !player.getUUID().equals(data.getUUID(DeathMarkService.MARK_OWNER_TAG))) {
                event.setCanRender(TriState.FALSE);
            }
        }
    }
}
