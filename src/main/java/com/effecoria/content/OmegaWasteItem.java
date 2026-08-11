package com.effecoria.content;

import java.util.List;

import com.effecoria.core.disease.DiseaseService;
import com.effecoria.core.disease.PhiDisease;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.world.OmegaScarService;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Lead foil saturated with Ω — bury on Scar surfaces to bleed off local entropy. */
public final class OmegaWasteItem extends Item {
    public OmegaWasteItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!isScarSurface(level, pos, state)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }

        if (player != null && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        server.sendParticles(
                ParticleTypes.SCULK_SOUL,
                pos.getX() + 0.5,
                pos.getY() + 1.0,
                pos.getZ() + 0.5,
                12,
                0.35,
                0.25,
                0.35,
                0.02);
        server.playSound(null, pos, SoundEvents.SCULK_BLOCK_BREAK, SoundSource.BLOCKS, 0.5f, 0.7f);

        AABB box = new AABB(pos).inflate(1.5);
        for (ServerPlayer nearby : server.getEntitiesOfClass(ServerPlayer.class, box)) {
            PlayerPsiData data = PsiHelper.get(nearby);
            data.setEntropyB(Math.max(0f, data.entropyB() - 0.08f));
            PsiHelper.set(nearby, data);
            nearby.syncData(ModAttachments.PSI.get());
            DiseaseService.cure(nearby, PhiDisease.OMEGA_SICKNESS);
        }

        if (player != null) {
            player.displayClientMessage(Component.translatable("message.effecoria.omega_waste_buried"), true);
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean isScarSurface(Level level, BlockPos pos, BlockState state) {
        if (state.is(ModBlocks.ROTTEN_MOSS.get())
                || state.is(ModBlocks.OMEGA_BLADES.get())
                || state.is(ModBlocks.OMEGA_TAINTED_OBSIDIAN.get())
                || state.is(ModBlocks.OMEGA_CRYSTAL.get())
                || state.is(ModBlocks.ELDRITCH_BLOOD_PUDDLE.get())) {
            return true;
        }
        return OmegaScarService.isBiome(level, pos);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.effecoria.omega_waste.hint"));
    }
}
