package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.block.PhiTurretBlock;
import com.effecoria.block.TurretMountBlock;
import com.effecoria.core.tower.FacilityNames;
import com.effecoria.core.tower.NamedFacilityDevice;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import javax.annotation.Nullable;

/** Name-tag / sneak-clear for Lex Loci facility device labels. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class FacilityNameEvents {
    private FacilityNameEvents() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        BlockPos pos = event.getPos();
        NamedFacilityDevice named = resolveNamed(level, pos);
        if (named == null) {
            return;
        }

        ItemStack stack = player.getItemInHand(event.getHand());
        if (stack.is(Items.NAME_TAG) && stack.has(DataComponents.CUSTOM_NAME)) {
            String label = FacilityNames.sanitize(stack.getHoverName().getString());
            if (label.isEmpty()) {
                return;
            }
            String previous = named.facilityName();
            if (!named.setFacilityName(label)) {
                player.displayClientMessage(
                        Component.translatable("message.effecoria.tower.device_name_taken"), true);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                return;
            }
            if (!label.equals(previous) && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.35f, 1.4f);
            player.displayClientMessage(
                    Component.translatable("message.effecoria.tower.device_named", label), true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        if (player.isShiftKeyDown() && stack.isEmpty() && !named.facilityName().isEmpty()) {
            named.setFacilityName("");
            level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.4f, 0.8f);
            player.displayClientMessage(Component.translatable("message.effecoria.tower.device_unnamed"), true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @Nullable
    private static NamedFacilityDevice resolveNamed(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof NamedFacilityDevice named) {
            return named;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof PhiTurretBlock) {
            for (var dir : net.minecraft.core.Direction.values()) {
                BlockPos adj = pos.relative(dir);
                if (level.getBlockState(adj).getBlock() instanceof TurretMountBlock
                        && level.getBlockEntity(adj) instanceof NamedFacilityDevice named) {
                    return named;
                }
            }
        }
        return null;
    }
}
