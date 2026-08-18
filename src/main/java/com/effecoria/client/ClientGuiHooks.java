package com.effecoria.client;

import com.effecoria.client.gui.GeneEditorScreen;
import com.effecoria.client.gui.MagicGuideScreen;
import com.effecoria.client.gui.RaceSelectScreen;
import com.effecoria.client.gui.SchoolSelectScreen;
import com.effecoria.client.gui.SealProgramScreen;
import com.effecoria.client.gui.SpellHubScreen;
import com.effecoria.client.gui.TechnomagicScreen;
import com.effecoria.core.progression.PrimerChapters;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientGuiHooks {
    private ClientGuiHooks() {}

    public static void openResonanceFocusScreen(Player player) {
        var data = PsiHelper.get(player);
        if (data.race().isEmpty()) {
            Minecraft.getInstance().setScreen(new RaceSelectScreen(false, !data.initiated()));
        } else if (!data.initiated()) {
            Minecraft.getInstance().setScreen(new SchoolSelectScreen());
        } else {
            Minecraft.getInstance().setScreen(new SpellHubScreen());
        }
    }

    public static void openSchoolSelect(boolean mandatory) {
        Minecraft.getInstance().setScreen(new SchoolSelectScreen(mandatory));
    }

    public static void openMagicGuide(Screen parent) {
        Minecraft.getInstance().setScreen(new MagicGuideScreen(parent));
    }

    public static void openMagicGuideChapter(PrimerChapters.Chapter chapter) {
        Minecraft.getInstance().setScreen(new MagicGuideScreen(chapter));
    }

    public static void openTechnomagic(Screen parent) {
        Minecraft.getInstance().setScreen(new TechnomagicScreen(parent));
    }

    public static boolean primerHasUnseenPages() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        return PrimerChapters.hasUnseen(PsiHelper.get(mc.player));
    }

    public static void openGeneEditor(
            int entityId,
            String targetName,
            java.util.List<String> current,
            java.util.List<String> unlocked,
            int maxSlots,
            boolean dnaLocked,
            boolean canLock) {
        Minecraft.getInstance()
                .setScreen(new GeneEditorScreen(
                        entityId, targetName, current, unlocked, maxSlots, dnaLocked, canLock));
    }

    public static void openSealEditor(
            net.minecraft.core.BlockPos anchor,
            int maxTargets,
            String source,
            java.util.List<com.effecoria.network.ModNetworking.SealEditorMember> members) {
        Minecraft.getInstance().setScreen(new SealProgramScreen(anchor, maxTargets, source, members));
    }
}
