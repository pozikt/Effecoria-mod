package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.client.gui.SpellHubScreen;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.network.ModNetworking;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientInputEvents {
    private static final long HOLD_MS = 150L;

    private static boolean spellBookKeyWasDown;
    private static long spellBookHoldStartMs;
    private static boolean radialOpenedThisHold;

    private ClientInputEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            resetHoldState();
            return;
        }

        // KeyConflictContext.IN_GAME makes KeyMapping.isDown() false while any Screen is open,
        // which would immediately close the radial. Poll the raw bound key instead.
        boolean down = isSpellBookKeyPhysicallyDown(minecraft);

        if (minecraft.screen instanceof SpellHubScreen hub) {
            if (!down) {
                hub.completeSelection();
                resetHoldState();
            } else {
                spellBookKeyWasDown = true;
            }
            return;
        }

        while (KeyBindings.CAST_SPELL.consumeClick()) {
            if (minecraft.screen != null) {
                break;
            }
            PlayerPsiData data = minecraft.player.getData(ModAttachments.PSI.get());
            int index = data.initiated() ? data.selectedSpellIndex() : -1;
            PacketDistributor.sendToServer(new ModNetworking.CastSpellPayload(index));
        }

        if (minecraft.screen != null) {
            resetHoldState();
            return;
        }

        PlayerPsiData data = minecraft.player.getData(ModAttachments.PSI.get());

        if (down && !spellBookKeyWasDown) {
            spellBookHoldStartMs = System.currentTimeMillis();
            radialOpenedThisHold = false;
        }

        if (down && data.initiated() && !radialOpenedThisHold) {
            if (System.currentTimeMillis() - spellBookHoldStartMs >= HOLD_MS) {
                minecraft.setScreen(new SpellHubScreen());
                radialOpenedThisHold = true;
                spellBookKeyWasDown = true;
                return;
            }
        }

        if (!down) {
            radialOpenedThisHold = false;
        }

        spellBookKeyWasDown = down;
    }

    /** Raw GLFW state of the bound key — works even while a Screen is open. */
    private static boolean isSpellBookKeyPhysicallyDown(Minecraft minecraft) {
        InputConstants.Key key = KeyBindings.OPEN_SPELL_BOOK.getKey();
        long window = minecraft.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, key.getValue())
                    == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        }
        return InputConstants.isKeyDown(window, key.getValue());
    }

    private static void resetHoldState() {
        spellBookKeyWasDown = false;
        radialOpenedThisHold = false;
        spellBookHoldStartMs = 0L;
    }
}
