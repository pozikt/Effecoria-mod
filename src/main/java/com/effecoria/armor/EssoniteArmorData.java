package com.effecoria.armor;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** CustomData keys for essonite armor charge, phonemes, and camo. */
public final class EssoniteArmorData {
    public static final String CHARGE_KEY = "ArmorPhiCharge";
    public static final String PHONEME_KEY = "ArmorPhoneme";
    public static final String CAMO_KEY = "ArmorCamo";
    public static final String ABNEGATIO_READY_KEY = "AbnegatioReady";
    public static final String UMBRA_ACTIVE_KEY = "UmbraActive";

    public static final String PLAYER_ABILITY_KEY = "effecoria:armor_ability";
    public static final String PLAYER_WINGS_UNTIL = "effecoria:armor_wings_until";
    public static final String PLAYER_SKIN_UNTIL = "effecoria:armor_skin_until";
    public static final String PLAYER_OMEGA_UNTIL = "effecoria:armor_omega_until";
    public static final String PLAYER_FLASH_CD = "effecoria:armor_flash_cd";
    public static final String PLAYER_ABILITY_CD = "effecoria:armor_ability_cd";

    private EssoniteArmorData() {}

    public static boolean isEssonite(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof EssoniteArmorItem;
    }

    public static Optional<EssoniteArmorTier> tierOf(ItemStack stack) {
        if (stack.getItem() instanceof EssoniteArmorItem item) {
            return Optional.of(item.tier());
        }
        return Optional.empty();
    }

    public static float charge(ItemStack stack) {
        if (!isEssonite(stack)) {
            return 0f;
        }
        return Mth.clamp(readFloat(stack, CHARGE_KEY, 0f), 0f, 1f);
    }

    public static void setCharge(ItemStack stack, float value) {
        if (!isEssonite(stack)) {
            return;
        }
        writeFloat(stack, CHARGE_KEY, Mth.clamp(value, 0f, 1f));
    }

    public static void addCharge(ItemStack stack, float delta) {
        setCharge(stack, charge(stack) + delta);
    }

    public static Optional<EssonitePhoneme> phoneme(ItemStack stack) {
        if (!isEssonite(stack)) {
            return Optional.empty();
        }
        return EssonitePhoneme.fromId(readString(stack, PHONEME_KEY, ""));
    }

    public static void setPhoneme(ItemStack stack, EssonitePhoneme phoneme) {
        if (!isEssonite(stack)) {
            return;
        }
        writeString(stack, PHONEME_KEY, phoneme.id());
        if (phoneme == EssonitePhoneme.ABNEGATIO) {
            writeBool(stack, ABNEGATIO_READY_KEY, true);
        }
    }

    public static boolean hasPhoneme(ItemStack stack, EssonitePhoneme phoneme) {
        return phoneme(stack).filter(p -> p == phoneme).isPresent();
    }

    public static String camo(ItemStack stack) {
        return isEssonite(stack) ? readString(stack, CAMO_KEY, "none") : "none";
    }

    public static void setCamo(ItemStack stack, String camo) {
        if (!isEssonite(stack)) {
            return;
        }
        writeString(stack, CAMO_KEY, camo == null ? "none" : camo.toLowerCase(Locale.ROOT));
    }

    public static boolean abnegatioReady(ItemStack stack) {
        return isEssonite(stack) && readBool(stack, ABNEGATIO_READY_KEY, true);
    }

    public static void setAbnegatioReady(ItemStack stack, boolean ready) {
        if (isEssonite(stack)) {
            writeBool(stack, ABNEGATIO_READY_KEY, ready);
        }
    }

    public static boolean umbraActive(ItemStack stack) {
        return isEssonite(stack) && readBool(stack, UMBRA_ACTIVE_KEY, false);
    }

    public static void setUmbraActive(ItemStack stack, boolean active) {
        if (isEssonite(stack)) {
            writeBool(stack, UMBRA_ACTIVE_KEY, active);
        }
    }

    public static float slotWeight(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 0.18f;
            case CHEST -> 0.40f;
            case LEGS -> 0.24f;
            case FEET -> 0.18f;
            default -> 0f;
        };
    }

    /** Weighted average charge across worn essonite pieces (0..1). */
    public static float poolCharge(Player player) {
        float sum = 0f;
        float weight = 0f;
        for (EquipmentSlot slot : new EquipmentSlot[] {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack stack = player.getItemBySlot(slot);
            Optional<EssoniteArmorTier> tier = tierOf(stack);
            if (tier.isEmpty()) {
                continue;
            }
            float w = slotWeight(slot) * tier.get().capacityWeight();
            sum += charge(stack) * w;
            weight += w;
        }
        return weight > 0.001f ? Mth.clamp(sum / weight, 0f, 1f) : 0f;
    }

    /** Best (highest) tier among worn pieces. */
    public static Optional<EssoniteArmorTier> bestWornTier(Player player) {
        EssoniteArmorTier best = null;
        for (EquipmentSlot slot : new EquipmentSlot[] {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            Optional<EssoniteArmorTier> tier = tierOf(player.getItemBySlot(slot));
            if (tier.isPresent() && (best == null || tier.get().rank() > best.rank())) {
                best = tier.get();
            }
        }
        return Optional.ofNullable(best);
    }

    public static Optional<EssoniteArmorTier> chestTier(Player player) {
        return tierOf(player.getItemBySlot(EquipmentSlot.CHEST));
    }

    public static Optional<EssoniteArmorTier> helmetTier(Player player) {
        return tierOf(player.getItemBySlot(EquipmentSlot.HEAD));
    }

    /** Drain from pool proportionally across pieces. Returns amount actually drained (0..requested in pool units). */
    public static float drainPool(Player player, float fractionOfFullPool) {
        float need = Mth.clamp(fractionOfFullPool, 0f, 1f);
        if (need <= 0f) {
            return 0f;
        }
        float available = poolCharge(player);
        float take = Math.min(need, available);
        if (take <= 0.0001f) {
            return 0f;
        }
        float remaining = take;
        for (EquipmentSlot slot : new EquipmentSlot[] {
            EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.HEAD, EquipmentSlot.FEET
        }) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!isEssonite(stack)) {
                continue;
            }
            float c = charge(stack);
            float drain = Math.min(c, remaining);
            setCharge(stack, c - drain);
            remaining -= drain;
            if (remaining <= 0.0001f) {
                break;
            }
        }
        return take;
    }

    public static void distributeCharge(Player player, float delta) {
        if (delta == 0f) {
            return;
        }
        for (EquipmentSlot slot : new EquipmentSlot[] {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack stack = player.getItemBySlot(slot);
            Optional<EssoniteArmorTier> tier = tierOf(stack);
            if (tier.isEmpty()) {
                continue;
            }
            float share = delta * slotWeight(slot);
            addCharge(stack, share);
        }
    }

    public static EssoniteArmorAbility selectedAbility(Player player) {
        String id = player.getPersistentData().getString(PLAYER_ABILITY_KEY);
        return EssoniteArmorAbility.fromId(id).orElse(EssoniteArmorAbility.FLASH);
    }

    public static void setSelectedAbility(Player player, EssoniteArmorAbility ability) {
        player.getPersistentData().putString(PLAYER_ABILITY_KEY, ability.id());
    }

    public static void appendTooltip(ItemStack stack, EssoniteArmorTier tier, List<Component> tooltip) {
        tooltip.add(Component.translatable("item.effecoria.essonite_armor.tier." + tier.name().toLowerCase(Locale.ROOT)));
        int pct = Math.round(charge(stack) * 100f);
        tooltip.add(Component.translatable("item.effecoria.essonite_armor.charge", pct));
        phoneme(stack).ifPresent(p -> tooltip.add(Component.translatable("item.effecoria.essonite_armor.phoneme." + p.id())));
        String camo = camo(stack);
        if (!"none".equals(camo)) {
            tooltip.add(Component.translatable("item.effecoria.essonite_armor.camo." + camo));
        }
    }

    private static float readFloat(ItemStack stack, String key, float fallback) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        return tag.contains(key) ? tag.getFloat(key) : fallback;
    }

    private static void writeFloat(ItemStack stack, String key, float value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putFloat(key, value));
    }

    private static String readString(ItemStack stack, String key, String fallback) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        return tag.contains(key) ? tag.getString(key) : fallback;
    }

    private static void writeString(ItemStack stack, String key, String value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(key, value));
    }

    private static boolean readBool(ItemStack stack, String key, boolean fallback) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        return tag.contains(key) ? tag.getBoolean(key) : fallback;
    }

    private static void writeBool(ItemStack stack, String key, boolean value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(key, value));
    }
}
