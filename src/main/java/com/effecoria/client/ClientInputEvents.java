package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.client.gui.SpellHubScreen;
import com.effecoria.config.BalanceConfig;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.network.ModNetworking;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientInputEvents {
    private static final long HOLD_MS = 150L;

    private static boolean spellBookKeyWasDown;
    private static long spellBookHoldStartMs;
    private static boolean radialOpenedThisHold;

    private static boolean castKeyWasDown;
    private static long castHoldStartMs;
    private static float castCharge;

    private static long lastMatterLinkMs;
    private static boolean matterChannelWasDown;

    private static boolean harpyJumpWasDown;
    private static boolean varanagiJumpWasDown;
    private static boolean varanagiJumpSent;

    private ClientInputEvents() {}

    /**
     * Spell hub is a Screen, so vanilla KeyMappings go idle — reinject WASD/sprint from
     * physical keys so you can keep running while picking a spell.
     */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof SpellHubScreen) || !(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        var options = minecraft.options;
        boolean up = isKeyPhysicallyDown(minecraft, options.keyUp.getKey());
        boolean down = isKeyPhysicallyDown(minecraft, options.keyDown.getKey());
        boolean left = isKeyPhysicallyDown(minecraft, options.keyLeft.getKey());
        boolean right = isKeyPhysicallyDown(minecraft, options.keyRight.getKey());
        boolean jumping = isKeyPhysicallyDown(minecraft, options.keyJump.getKey());
        boolean shift = isKeyPhysicallyDown(minecraft, options.keyShift.getKey());
        boolean sprint = isKeyPhysicallyDown(minecraft, options.keySprint.getKey());

        var input = event.getInput();
        input.up = up;
        input.down = down;
        input.left = left;
        input.right = right;
        input.jumping = jumping;
        input.shiftKeyDown = shift;

        float forward = (up == down) ? 0f : (up ? 1f : -1f);
        float strafe = (left == right) ? 0f : (left ? 1f : -1f);
        if (shift) {
            // KeyboardInput uses ~0.3 when sneaking.
            forward *= 0.3f;
            strafe *= 0.3f;
        }
        input.forwardImpulse = forward;
        input.leftImpulse = strafe;

        // Keep / start sprint while moving forward in the hub.
        if (forward > 0f && (sprint || player.isSprinting())) {
            player.setSprinting(true);
        } else if (forward <= 0f) {
            player.setSprinting(false);
        }
    }

    /** 0..1 charge while holding cast; 0 when idle. */
    public static float castChargeFraction() {
        return castCharge;
    }

    public static boolean isCastCharging() {
        return castKeyWasDown && castCharge > 0.001f;
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        if (!isCycleModifierPhysicallyDown(minecraft)) {
            return;
        }
        PlayerPsiData data = minecraft.player.getData(ModAttachments.PSI.get());
        if (!data.initiated() || data.knownSpells().isEmpty()) {
            return;
        }
        event.setCanceled(true);
        int delta = event.getScrollDeltaY() > 0 ? -1 : 1;
        data.cycleSpell(delta);
        PacketDistributor.sendToServer(new ModNetworking.CycleSpellPayload(delta));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            resetHoldState();
            resetCastCharge();
            return;
        }

        boolean down = isSpellBookKeyPhysicallyDown(minecraft);

        if (minecraft.screen instanceof SpellHubScreen hub) {
            if (!down) {
                hub.completeSelection();
                resetHoldState();
            } else {
                spellBookKeyWasDown = true;
            }
            resetCastCharge();
            return;
        }

        // Swallow press events — release-to-cast is driven by physical key state.
        while (KeyBindings.CAST_SPELL.consumeClick()) {
            // no-op
        }

        if (minecraft.screen == null) {
            tickCastCharge(minecraft);
            tickHarpyFlap(minecraft);
            tickVaranagiClimb(minecraft);
        } else {
            resetCastCharge();
            harpyJumpWasDown = false;
            varanagiJumpWasDown = false;
            varanagiJumpSent = false;
        }

        while (KeyBindings.OPEN_SEAL_EDITOR.consumeClick()) {
            if (minecraft.screen != null) {
                break;
            }
            PlayerPsiData data = minecraft.player.getData(ModAttachments.PSI.get());
            if (!data.initiated() || data.school() != com.effecoria.core.magic.MagicSchool.SEALS) {
                minecraft.player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.effecoria.seal.need_school"),
                        true);
                break;
            }
            var hit = minecraft.player.pick(8.0, 0f, false);
            if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
                minecraft.player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.effecoria.seal.need_block"),
                        true);
                break;
            }
            var blockHit = (net.minecraft.world.phys.BlockHitResult) hit;
            minecraft.setScreen(new com.effecoria.client.gui.SealProgramScreen(blockHit.getBlockPos()));
        }

        if (minecraft.screen == null) {
            tickMatterInput(minecraft);
            tickArmorInput(minecraft);
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

    private static void tickCastCharge(Minecraft minecraft) {
        boolean castDown = isCastKeyPhysicallyDown(minecraft);
        PlayerPsiData data = minecraft.player.getData(ModAttachments.PSI.get());
        if (!data.initiated() || data.selectedSpell() == null) {
            if (castKeyWasDown && !castDown) {
                // ignore release without a spell
            }
            resetCastCharge();
            castKeyWasDown = castDown;
            return;
        }

        long chargeMs = BalanceConfig.CAST_CHARGE_MS.get();
        if (castDown && !castKeyWasDown) {
            castHoldStartMs = System.currentTimeMillis();
            castCharge = 0f;
        }
        if (castDown) {
            long held = System.currentTimeMillis() - castHoldStartMs;
            castCharge = Mth.clamp(held / (float) chargeMs, 0f, 1f);
        } else if (castKeyWasDown) {
            float charge = castCharge;
            int index = data.selectedSpellIndex();
            PacketDistributor.sendToServer(new ModNetworking.CastSpellPayload(index, charge));
            castCharge = 0f;
            castHoldStartMs = 0L;
        }
        castKeyWasDown = castDown;
    }

    private static void tickMatterInput(Minecraft minecraft) {
        PlayerPsiData data = minecraft.player.getData(ModAttachments.PSI.get());
        if (!data.initiated() || data.school() != com.effecoria.core.magic.MagicSchool.ELEMENTAL) {
            if (matterChannelWasDown) {
                PacketDistributor.sendToServer(new ModNetworking.MatterChannelPayload(false));
                matterChannelWasDown = false;
            }
            while (KeyBindings.MATTER_LINK.consumeClick()) {
                // swallow
            }
            return;
        }

        while (KeyBindings.MATTER_LINK.consumeClick()) {
            long now = System.currentTimeMillis();
            if (now - lastMatterLinkMs <= 280L && ClientMatterBondState.active()) {
                PacketDistributor.sendToServer(new ModNetworking.MatterThrowPayload());
                lastMatterLinkMs = 0L;
            } else {
                PacketDistributor.sendToServer(new ModNetworking.MatterLinkPayload());
                lastMatterLinkMs = now;
            }
        }

        boolean channelDown = isKeyPhysicallyDown(minecraft, KeyBindings.MATTER_CHANNEL.getKey());
        if (channelDown != matterChannelWasDown) {
            PacketDistributor.sendToServer(new ModNetworking.MatterChannelPayload(channelDown));
            matterChannelWasDown = channelDown;
        }
    }

    private static void tickArmorInput(Minecraft minecraft) {
        while (KeyBindings.ARMOR_ABILITY.consumeClick()) {
            PacketDistributor.sendToServer(new ModNetworking.ArmorAbilityPayload());
        }
        while (KeyBindings.ARMOR_ABILITY_CYCLE.consumeClick()) {
            ClientArmorAbilityState.cycleLocal();
            PacketDistributor.sendToServer(new ModNetworking.ArmorAbilityCyclePayload());
        }
    }

    private static void tickHarpyFlap(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            harpyJumpWasDown = false;
            return;
        }
        PlayerPsiData data = player.getData(ModAttachments.PSI.get());
        boolean harpy = data.race().orElse(null) == com.effecoria.core.progression.PlayerRace.HARPY;
        boolean jumpDown = isKeyPhysicallyDown(minecraft, minecraft.options.keyJump.getKey());
        if (harpy && !player.onGround() && jumpDown && !harpyJumpWasDown) {
            PacketDistributor.sendToServer(new ModNetworking.HarpyFlapPayload());
        }
        harpyJumpWasDown = jumpDown;
    }

    private static void tickVaranagiClimb(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            varanagiJumpWasDown = false;
            varanagiJumpSent = false;
            return;
        }
        PlayerPsiData data = player.getData(ModAttachments.PSI.get());
        boolean varanagi = data.race().orElse(null) == com.effecoria.core.progression.PlayerRace.VARANAGI;
        if (!varanagi) {
            if (varanagiJumpSent) {
                PacketDistributor.sendToServer(new ModNetworking.VaranagiClimbJumpPayload(false));
                varanagiJumpSent = false;
            }
            varanagiJumpWasDown = false;
            return;
        }

        boolean jumpDown = isKeyPhysicallyDown(minecraft, minecraft.options.keyJump.getKey());
        com.effecoria.core.progression.VaranagiClimbService.setJumpHeld(player, jumpDown);

        boolean nearWall = player.onClimbable()
                || com.effecoria.core.progression.VaranagiClimbService.isAgainstClimbSurface(player)
                || com.effecoria.core.progression.VaranagiClimbService.isNearClimbSurface(player, 0.35);
        if (!nearWall) {
            if (varanagiJumpSent) {
                PacketDistributor.sendToServer(new ModNetworking.VaranagiClimbJumpPayload(false));
                varanagiJumpSent = false;
            }
        } else if (jumpDown != varanagiJumpSent) {
            PacketDistributor.sendToServer(new ModNetworking.VaranagiClimbJumpPayload(jumpDown));
            varanagiJumpSent = jumpDown;
        }

        // Client prediction for vine-like wall cling.
        com.effecoria.core.progression.VaranagiClimbService.tick(player);

        if (nearWall
                && player.isSprinting()
                && jumpDown
                && !varanagiJumpWasDown
                && !player.onGround()) {
            PacketDistributor.sendToServer(new ModNetworking.VaranagiClimbDashPayload());
        }
        varanagiJumpWasDown = jumpDown;
    }

    private static void resetCastCharge() {
        castKeyWasDown = false;
        castHoldStartMs = 0L;
        castCharge = 0f;
    }

    private static boolean isSpellBookKeyPhysicallyDown(Minecraft minecraft) {
        return isKeyPhysicallyDown(minecraft, KeyBindings.OPEN_SPELL_BOOK.getKey());
    }

    private static boolean isCastKeyPhysicallyDown(Minecraft minecraft) {
        return isKeyPhysicallyDown(minecraft, KeyBindings.CAST_SPELL.getKey());
    }

    private static boolean isCycleModifierPhysicallyDown(Minecraft minecraft) {
        return isKeyPhysicallyDown(minecraft, KeyBindings.CYCLE_SPELL_MODIFIER.getKey());
    }

    private static boolean isKeyPhysicallyDown(Minecraft minecraft, InputConstants.Key key) {
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
