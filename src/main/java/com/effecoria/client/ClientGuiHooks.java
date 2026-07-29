package com.effecoria.client;

import com.effecoria.client.gui.SchoolSelectScreen;
import com.effecoria.client.gui.SpellHubScreen;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientGuiHooks {
    private ClientGuiHooks() {}

    public static void openResonanceFocusScreen(Player player) {
        if (!PsiHelper.get(player).initiated()) {
            Minecraft.getInstance().setScreen(new SchoolSelectScreen());
        } else {
            Minecraft.getInstance().setScreen(new SpellHubScreen());
        }
    }
}
