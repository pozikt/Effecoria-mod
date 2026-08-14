package com.effecoria.content;

import java.util.List;

import com.effecoria.core.alchemy.PhiBusNetwork;
import com.effecoria.entity.PhiFilamentEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Places Φ-filaments between two terminals (bus, mithril, circuitry, machines). */
public final class MithrilWireItem extends Item {
    private static final String KEY_A = "FilamentA";
    private static final String KEY_DIM = "FilamentDim";

    public MithrilWireItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!PhiBusNetwork.isConductor(state) && level.getBlockEntity(pos) == null) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockPos stored = readAnchor(stack);
        String dim = readDim(stack);
        String here = level.dimension().location().toString();
        if (stored == null || dim == null) {
            writeAnchor(stack, pos, level.dimension());
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.effecoria.filament.first"), true);
            }
            return InteractionResult.CONSUME;
        }
        if (!dim.equals(here)) {
            clearAnchor(stack);
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.effecoria.filament.wrong_dim"), true);
            }
            return InteractionResult.FAIL;
        }
        if (PhiFilamentEntity.tooFar(stored, pos)) {
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable("message.effecoria.filament.too_far", PhiFilamentEntity.MAX_RANGE),
                        true);
            }
            return InteractionResult.FAIL;
        }
        PhiFilamentEntity filament = new PhiFilamentEntity(level, stored, pos);
        level.addFreshEntity(filament);
        level.playSound(null, pos, SoundEvents.BEEHIVE_SHEAR, SoundSource.BLOCKS, 0.5f, 1.6f);
        clearAnchor(stack);
        if (player != null && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.effecoria.filament.linked"), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.effecoria.mithril_wire.hint"));
        if (readAnchor(stack) != null) {
            tooltip.add(Component.translatable("item.effecoria.mithril_wire.pending"));
        }
    }

    private static BlockPos readAnchor(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (!tag.contains(KEY_A)) {
            return null;
        }
        return BlockPos.of(tag.getLong(KEY_A));
    }

    private static String readDim(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        return tag.contains(KEY_DIM) ? tag.getString(KEY_DIM) : null;
    }

    private static void writeAnchor(ItemStack stack, BlockPos pos, ResourceKey<Level> dim) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putLong(KEY_A, pos.asLong());
            tag.putString(KEY_DIM, dim.location().toString());
        });
    }

    private static void clearAnchor(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove(KEY_A);
            tag.remove(KEY_DIM);
        });
    }
}
