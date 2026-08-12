package com.effecoria.item;

import com.effecoria.core.glue.EssenceGlueService;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Φ-glue axe — RMB pos1, RMB pos2 glues the cuboid; Shift+RMB clears selection. */
public final class EssenceGlueItem extends Item {
    public EssenceGlueItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null || level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            EssenceGlueService.clearSelection(sp);
            return InteractionResult.CONSUME;
        }

        BlockPos pos = ctx.getClickedPos();
        EssenceGlueService.selectCorner(server, sp, pos);

        if (!player.getAbilities().instabuild) {
            ctx.getItemInHand().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
        return InteractionResult.CONSUME;
    }
}
