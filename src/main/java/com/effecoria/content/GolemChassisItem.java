package com.effecoria.content;

import java.util.List;

import com.effecoria.core.technomagic.ConstructBindingService;
import com.effecoria.core.technomagic.ImprintData;
import com.effecoria.entity.PhiConstructEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/** Blank or imprinted chassis — imprint via Ψ-imprinter; imprinted chassis spawns a Φ-construct. */
public final class GolemChassisItem extends Item {
    public GolemChassisItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        if (!ImprintData.isImprintedChassis(stack)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server) || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos place = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(place).canBeReplaced()) {
            return InteractionResult.FAIL;
        }
        PhiConstructEntity construct = ModEntities.PHI_CONSTRUCT.get().create(server);
        if (construct == null) {
            return InteractionResult.FAIL;
        }
        construct.moveTo(place.getX() + 0.5, place.getY(), place.getZ() + 0.5, player.getYRot(), 0f);
        construct.tame(player);
        construct.setOrderedToSit(false);
        int tier = ImprintData.tier(stack);
        construct.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                .setBaseValue(36.0 + tier * 4.0);
        construct.setHealth(construct.getMaxHealth());
        ConstructBindingService.register(construct, player);
        server.addFreshEntity(construct);
        level.gameEvent(player, GameEvent.ENTITY_PLACE, place);
        level.playSound(null, place, SoundEvents.IRON_GOLEM_REPAIR, SoundSource.BLOCKS, 0.8f, 1.2f);
        player.displayClientMessage(Component.translatable("message.effecoria.construct_spawned"), true);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (ImprintData.isImprintedChassis(stack)) {
            tooltip.add(Component.translatable("item.effecoria.golem_chassis.imprinted", ImprintData.tier(stack)));
        } else {
            tooltip.add(Component.translatable("item.effecoria.golem_chassis.blank"));
        }
    }
}
