package com.effecoria.content;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.effecoria.core.disease.DiseaseService;
import com.effecoria.core.disease.PhiDisease;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Consumable that cures one or more Φ-diseases. */
public final class DiseaseCureItem extends Item {
    private final Set<PhiDisease> targets;
    private final String hintKey;
    private final Consumer<ServerPlayer> afterCure;
    private final UseAnim anim;
    private final int useTicks;

    public DiseaseCureItem(
            Properties properties,
            Set<PhiDisease> targets,
            String hintKey,
            Consumer<ServerPlayer> afterCure,
            UseAnim anim,
            int useTicks) {
        super(properties.stacksTo(8));
        this.targets = targets;
        this.hintKey = hintKey;
        this.afterCure = afterCure == null ? p -> {} : afterCure;
        this.anim = anim;
        this.useTicks = useTicks;
    }

    public static DiseaseCureItem of(Properties properties, String hintKey, PhiDisease... diseases) {
        return new DiseaseCureItem(
                properties, EnumSet.copyOf(List.of(diseases)), hintKey, null, UseAnim.DRINK, 32);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            boolean any = false;
            for (PhiDisease disease : targets) {
                if (DiseaseService.cure(player, disease)) {
                    any = true;
                }
            }
            afterCure.accept(player);
            if (!any) {
                player.displayClientMessage(Component.translatable("message.effecoria.disease_cure_none"), true);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.7f, 1.0f);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return useTicks;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return anim;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(hintKey));
    }
}
