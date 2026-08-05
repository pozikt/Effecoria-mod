package com.effecoria.effect.elemental;

import java.util.Optional;

import javax.annotation.Nullable;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.magic.SpellEffectEntry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Utility fire workings: sear one held raw food, or flash-smelt a placed ore block in-world.
 */
public final class ElementalCraftEffects {
    private ElementalCraftEffects() {}

    public static boolean canSearHeld(ServerPlayer caster) {
        return findCookableHand(caster) != null && findCookingResult(caster.serverLevel(), stackIn(caster, findCookableHand(caster))) != null;
    }

    public static boolean canSmeltBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0f) {
            return false;
        }
        ItemStack asItem = new ItemStack(state.getBlock().asItem());
        if (asItem.isEmpty()) {
            return false;
        }
        return findSmeltResult(level, asItem) != null;
    }

    /** Cook exactly one item from the held stack (main hand preferred, then offhand). */
    public static void sear(ServerPlayer caster, SpellEffectEntry effect, float power) {
        InteractionHand hand = findCookableHand(caster);
        if (hand == null) {
            caster.displayClientMessage(Component.translatable("message.effecoria.sear.need_raw"), true);
            return;
        }
        ItemStack held = caster.getItemInHand(hand);
        ItemStack one = held.copyWithCount(1);
        ItemStack cooked = findCookingResult(caster.serverLevel(), one);
        if (cooked == null || cooked.isEmpty()) {
            caster.displayClientMessage(Component.translatable("message.effecoria.sear.need_raw"), true);
            return;
        }

        held.shrink(1);
        if (held.isEmpty()) {
            caster.setItemInHand(hand, ItemStack.EMPTY);
        }
        giveOrDrop(caster, cooked.copy());

        ServerLevel level = caster.serverLevel();
        Vec3 handPos = caster.getEyePosition().add(caster.getLookAngle().scale(0.45));
        level.sendParticles(ParticleTypes.FLAME, handPos.x, handPos.y, handPos.z, 10, 0.12, 0.08, 0.12, 0.01);
        level.sendParticles(ParticleTypes.SMOKE, handPos.x, handPos.y, handPos.z, 6, 0.1, 0.08, 0.1, 0.01);
        level.sendParticles(
                ModParticleTypes.ELEMENTAL_EMBER.get(), handPos.x, handPos.y, handPos.z, 4, 0.1, 0.06, 0.1, 0.02);
        level.playSound(
                null,
                caster.blockPosition(),
                SoundEvents.FIRECHARGE_USE,
                SoundSource.PLAYERS,
                0.45f,
                1.25f + caster.getRandom().nextFloat() * 0.2f);
        level.playSound(
                null,
                caster.blockPosition(),
                SoundEvents.GENERIC_BURN,
                SoundSource.PLAYERS,
                0.35f,
                1.4f);
        // Tiny XP like a campfire cook.
        caster.giveExperiencePoints(Math.max(1, Math.round(1f + power * 0.02f)));
    }

    /** Smelt the looked-at placed block via furnace/blast recipes; drops the result in-world. */
    public static void oreSmelt(ServerPlayer caster, SpellEffectEntry effect, float power, @Nullable BlockPos target) {
        if (target == null) {
            caster.displayClientMessage(Component.translatable("message.effecoria.ore_smelt.need_block"), true);
            return;
        }
        ServerLevel level = caster.serverLevel();
        if (!level.mayInteract(caster, target)) {
            caster.displayClientMessage(Component.translatable("message.effecoria.ore_smelt.need_block"), true);
            return;
        }
        BlockState state = level.getBlockState(target);
        ItemStack asItem = new ItemStack(state.getBlock().asItem());
        ItemStack result = findSmeltResult(level, asItem);
        if (result == null || result.isEmpty()) {
            caster.displayClientMessage(Component.translatable("message.effecoria.ore_smelt.no_recipe"), true);
            return;
        }

        // Remove the ore without vanilla block drops — the spell yields the smelted product.
        level.levelEvent(2001, target, Block.getId(state));
        level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);

        ItemStack drop = result.copy();
        // If the smelt result is a full block (cobble→stone), place it back; else drop the item.
        if (drop.getItem() instanceof BlockItem blockItem && drop.getCount() == 1) {
            BlockState placed = blockItem.getBlock().defaultBlockState();
            if (placed.canSurvive(level, target)) {
                level.setBlock(target, placed, 3);
            } else {
                spawnDrop(level, target, drop);
            }
        } else {
            spawnDrop(level, target, drop);
        }

        Vec3 c = Vec3.atCenterOf(target);
        level.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 28, 0.3, 0.3, 0.3, 0.03);
        level.sendParticles(ParticleTypes.LAVA, c.x, c.y, c.z, 12, 0.25, 0.25, 0.25, 0.02);
        level.sendParticles(ParticleTypes.SMOKE, c.x, c.y + 0.35, c.z, 16, 0.25, 0.2, 0.25, 0.03);
        level.sendParticles(ModParticleTypes.ELEMENTAL_EMBER.get(), c.x, c.y, c.z, 14, 0.28, 0.28, 0.28, 0.04);
        level.sendParticles(ModParticleTypes.PHI_FLAME.get(), c.x, c.y + 0.2, c.z, 10, 0.2, 0.2, 0.2, 0.02);
        level.playSound(null, target, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.15f, 1.05f);
        level.playSound(null, target, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.85f, 0.8f);
        level.playSound(null, target, SoundEvents.GENERIC_BURN, SoundSource.BLOCKS, 0.55f, 0.9f);
        caster.displayClientMessage(Component.translatable("message.effecoria.ore_smelt.done"), true);
        caster.giveExperiencePoints(Math.max(1, Math.round(2f + power * 0.03f)));
    }

    @Nullable
    private static InteractionHand findCookableHand(ServerPlayer caster) {
        if (isCookable(caster.serverLevel(), caster.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }
        if (isCookable(caster.serverLevel(), caster.getOffhandItem())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static ItemStack stackIn(ServerPlayer caster, @Nullable InteractionHand hand) {
        return hand == null ? ItemStack.EMPTY : caster.getItemInHand(hand);
    }

    private static boolean isCookable(ServerLevel level, ItemStack stack) {
        return !stack.isEmpty() && findCookingResult(level, stack) != null;
    }

    @Nullable
    private static ItemStack findCookingResult(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        SingleRecipeInput input = new SingleRecipeInput(stack.copyWithCount(1));
        Optional<ItemStack> smelting = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, input, level)
                .map(holder -> assembleCooking(holder, input, level));
        if (smelting.isPresent() && isFoodLike(stack, smelting.get())) {
            return smelting.get();
        }
        Optional<ItemStack> smoking = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMOKING, input, level)
                .map(holder -> assembleCooking(holder, input, level));
        if (smoking.isPresent()) {
            return smoking.get();
        }
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.CAMPFIRE_COOKING, input, level)
                .map(holder -> assembleCooking(holder, input, level))
                .orElse(null);
    }

    /**
     * Sear is for food — reject ore/ingot smelting from the hand path even if SMELTING matches.
     * Campfire/smoking recipes are always food. For smelting, require the input or output to be edible.
     */
    private static boolean isFoodLike(ItemStack input, ItemStack output) {
        return input.getFoodProperties(null) != null || output.getFoodProperties(null) != null;
    }

    @Nullable
    private static ItemStack findSmeltResult(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        SingleRecipeInput input = new SingleRecipeInput(stack.copyWithCount(1));
        Optional<ItemStack> blasting = level.getRecipeManager()
                .getRecipeFor(RecipeType.BLASTING, input, level)
                .map(holder -> assembleCooking(holder, input, level));
        if (blasting.isPresent()) {
            return blasting.get();
        }
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, input, level)
                .map(holder -> assembleCooking(holder, input, level))
                .orElse(null);
    }

    private static <T extends AbstractCookingRecipe> ItemStack assembleCooking(
            RecipeHolder<T> holder, SingleRecipeInput input, ServerLevel level) {
        return holder.value().assemble(input, level.registryAccess());
    }

    private static void giveOrDrop(ServerPlayer caster, ItemStack stack) {
        if (!caster.getInventory().add(stack)) {
            caster.drop(stack, false);
        }
    }

    private static void spawnDrop(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemEntity entity = new ItemEntity(
                level, pos.getX() + 0.5, pos.getY() + 0.35, pos.getZ() + 0.5, stack);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }
}
