package com.effecoria.world;

import com.effecoria.armor.EssoniteArmorService;
import com.effecoria.content.ModBlockTags;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItems;
import com.effecoria.content.ModMobEffects;
import com.effecoria.content.PhiHarnessItems;
import com.effecoria.effect.spatial.SpatialAugments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Catalogued Φ-radiation shielding (plateau caves / Φ-core / wastes / spire).
 * Returns a 0–1 attenuation factor; {@link Shield#absolute()} fully nullifies exposure.
 */
public final class PhiRadiationService {
    private PhiRadiationService() {}

    public record Shield(float factor, boolean omega, boolean absolute) {
        public float remaining() {
            if (absolute) {
                return 0f;
            }
            return Mth.clamp(1f - factor, 0f, 1f);
        }

        public boolean adequate() {
            return absolute || factor >= 0.5f;
        }
    }

    public static Shield evaluate(Player player) {
        if (player.getAbilities().instabuild || player.isSpectator()) {
            return new Shield(1f, true, true);
        }
        if (SpatialAugments.hasCocoon(player, player.level().getGameTime())) {
            return new Shield(1f, true, true);
        }

        float factor = 0f;
        boolean omega = false;

        // --- Passive metal / void ---
        int goldPieces = 0;
        for (ItemStack armor : player.getArmorSlots()) {
            if (isGoldItem(armor)) {
                goldPieces++;
            }
        }
        if (isGoldItem(player.getMainHandItem()) || isGoldItem(player.getOffhandItem())) {
            goldPieces = Math.max(goldPieces, 1);
        }
        if (goldPieces > 0) {
            factor = Math.max(factor, Math.min(0.95f, 0.28f * goldPieces));
        }

        if (hasItem(player, ModItems.GOLD_AMULET.get())
                || player.getOffhandItem().is(ModItems.GOLD_AMULET.get())
                || player.getMainHandItem().is(ModItems.GOLD_AMULET.get())) {
            factor = Math.max(factor, 0.55f);
        }

        float jewelry = com.effecoria.core.artifact.JewelryPassives.maxPhiShield(player);
        if (jewelry > 0f) {
            factor = Math.max(factor, jewelry);
        }

        if (hasItem(player, ModItems.LEAD_CLOAK.get())
                || player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.LEAD_CLOAK.get())) {
            factor = Math.max(factor, 0.72f);
        } else if (hasItem(player, ModItems.LEAD_FILTER.get())
                || player.getMainHandItem().is(ModItems.LEAD_FILTER.get())
                || player.getOffhandItem().is(ModItems.LEAD_FILTER.get())) {
            factor = Math.max(factor, 0.45f);
        }

        if (hasItem(player, ModItems.VOID_OBSIDIAN.get())
                || hasItem(player, ModItems.VOID_OBSIDIAN_INSERT.get())
                || player.getOffhandItem().is(ModItems.VOID_OBSIDIAN.get())
                || player.getMainHandItem().is(ModItems.VOID_OBSIDIAN_INSERT.get())) {
            factor = Math.max(factor, 0.65f);
            omega = true;
        }

        // --- Active Φ tech ---
        if (EssoniteArmorService.hasAny(player)) {
            float suit = 0.55f;
            ItemStack cell = PhiHarnessItems.findPhiCell(player);
            if (!cell.isEmpty() && PhiHarnessItems.cellCharge(cell) > 0.05f) {
                suit = 0.82f; // Φ-suit analogue: contour + charged cell
            }
            factor = Math.max(factor, suit);
        } else {
            ItemStack cell = PhiHarnessItems.findPhiCell(player);
            if (!cell.isEmpty() && PhiHarnessItems.cellCharge(cell) > 0.05f) {
                factor = Math.max(factor, 0.35f);
            }
        }

        if (PhiHarnessItems.bestFocusTier(player) > 0) {
            factor = Math.max(factor, 0.3f);
        }

        if (player.getMainHandItem().is(ModItems.ESSENCE_PARASOL.get())
                || player.getOffhandItem().is(ModItems.ESSENCE_PARASOL.get())) {
            factor = Math.max(factor, 0.5f);
        }

        // --- Alchemy / chemistry ---
        if (player.hasEffect(ModMobEffects.PHI_RESISTANCE)) {
            factor = Math.max(factor, 0.5f);
        }
        if (player.hasEffect(ModMobEffects.PHI_CLAY_SALVE)) {
            factor = Math.max(factor, 0.3f);
        }
        if (player.hasEffect(ModMobEffects.LEAD_SATURATION)) {
            factor = Math.max(factor, 0.6f);
        }

        // Lead chamber / zero-flux shelter nearby
        if (nearZeroFluxShelter(player)) {
            factor = Math.max(factor, 0.4f);
        }

        return new Shield(Mth.clamp(factor, 0f, 1f), omega, false);
    }

    /** Legacy boolean used by older call sites — true if shield is at least mid-tier. */
    public static boolean hasPhiProtection(Player player) {
        return evaluate(player).adequate();
    }

    public static float applyDamage(Player player, float baseDamage) {
        if (baseDamage <= 0f) {
            return 0f;
        }
        return baseDamage * evaluate(player).remaining();
    }

    private static boolean nearZeroFluxShelter(Player player) {
        Level level = player.level();
        BlockPos origin = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-2, -1, -2), origin.offset(2, 2, 2))) {
            if (level.getBlockState(pos).is(ModBlockTags.ZERO_FLUX)
                    || level.getBlockState(pos).is(ModBlocks.VOID_OBSIDIAN.get())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasItem(Player player, net.minecraft.world.item.Item item) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGoldItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(ModItems.GOLD_FILTER.get()) || stack.is(ModItems.GOLD_AMULET.get())) {
            return true;
        }
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && "minecraft".equals(key.getNamespace()) && key.getPath().startsWith("gold_");
    }
}
