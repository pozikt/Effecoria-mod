package com.effecoria.content;

import java.util.EnumMap;
import java.util.List;

import com.effecoria.EffecoriaMod;
import com.google.common.base.Suppliers;

import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMaterials {
    private ModMaterials() {}

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, EffecoriaMod.MOD_ID);

    /** Basic essonite contour (Φ-chitin) — iron-ish defense. */
    public static final Holder<ArmorMaterial> PHI_CHITIN = ARMOR_MATERIALS.register(
            "phi_chitin",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 2);
                        map.put(ArmorItem.Type.LEGGINGS, 5);
                        map.put(ArmorItem.Type.CHESTPLATE, 6);
                        map.put(ArmorItem.Type.HELMET, 2);
                        map.put(ArmorItem.Type.BODY, 5);
                    }),
                    12,
                    SoundEvents.ARMOR_EQUIP_TURTLE,
                    () -> Ingredient.of(ModItems.PHI_CHITIN.get()),
                    List.of(new ArmorMaterial.Layer(EffecoriaMod.id("phi_chitin"))),
                    0.0f,
                    0.0f));

    /** Crystal essonite plates — diamond-ish. */
    public static final Holder<ArmorMaterial> CRYSTAL_ESSONITE = ARMOR_MATERIALS.register(
            "crystal_essonite",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 3);
                        map.put(ArmorItem.Type.LEGGINGS, 6);
                        map.put(ArmorItem.Type.CHESTPLATE, 8);
                        map.put(ArmorItem.Type.HELMET, 3);
                        map.put(ArmorItem.Type.BODY, 11);
                    }),
                    18,
                    SoundEvents.ARMOR_EQUIP_DIAMOND,
                    () -> Ingredient.of(ModItems.PURE_ESSONITE.get()),
                    List.of(new ArmorMaterial.Layer(EffecoriaMod.id("crystal_essonite"))),
                    2.0f,
                    0.0f));

    /** Star essonite — netherite-ish, focus-grade contour. */
    public static final Holder<ArmorMaterial> STAR_ESSONITE = ARMOR_MATERIALS.register(
            "star_essonite",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 3);
                        map.put(ArmorItem.Type.LEGGINGS, 6);
                        map.put(ArmorItem.Type.CHESTPLATE, 8);
                        map.put(ArmorItem.Type.HELMET, 3);
                        map.put(ArmorItem.Type.BODY, 11);
                    }),
                    22,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(ModItems.STAR_ESSONITE.get()),
                    List.of(new ArmorMaterial.Layer(EffecoriaMod.id("star_essonite"))),
                    3.0f,
                    0.1f));

    /** Mithril — diamond defense, light Φ-superconductor plates. */
    public static final Holder<ArmorMaterial> MITHRIL = ARMOR_MATERIALS.register(
            "mithril",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 3);
                        map.put(ArmorItem.Type.LEGGINGS, 6);
                        map.put(ArmorItem.Type.CHESTPLATE, 8);
                        map.put(ArmorItem.Type.HELMET, 3);
                        map.put(ArmorItem.Type.BODY, 11);
                    }),
                    22,
                    SoundEvents.ARMOR_EQUIP_DIAMOND,
                    () -> Ingredient.of(ModItems.MITHRIL_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(EffecoriaMod.id("mithril"))),
                    2.0f,
                    0.0f));

    /** Iron-ish damage, stone durability (131). */
    public static final Tier VITRIFIED_GLASS = new Tier() {
        private final java.util.function.Supplier<Ingredient> repair =
                Suppliers.memoize(() -> Ingredient.of(ModItems.VITRIFIED_GLASS_SHARD.get()));

        @Override
        public int getUses() {
            return 131;
        }

        @Override
        public float getSpeed() {
            return 6.0f;
        }

        @Override
        public float getAttackDamageBonus() {
            return 2.0f;
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return BlockTags.INCORRECT_FOR_IRON_TOOL;
        }

        @Override
        public int getEnchantmentValue() {
            return 14;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return repair.get();
        }
    };

    /** Diamond-class mining, high durability — Φ-superconductor tools. */
    public static final Tier MITHRIL_TOOLS = new Tier() {
        private final java.util.function.Supplier<Ingredient> repair =
                Suppliers.memoize(() -> Ingredient.of(ModItems.MITHRIL_INGOT.get()));

        @Override
        public int getUses() {
            return 1800;
        }

        @Override
        public float getSpeed() {
            return 8.0f;
        }

        @Override
        public float getAttackDamageBonus() {
            return 3.0f;
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
        }

        @Override
        public int getEnchantmentValue() {
            return 22;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return repair.get();
        }
    };
}
