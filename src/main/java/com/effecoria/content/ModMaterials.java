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

    /** Leather+ defense; durability factor 8 (~leather 5). */
    public static final Holder<ArmorMaterial> PHI_CHITIN = ARMOR_MATERIALS.register(
            "phi_chitin",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 1);
                        map.put(ArmorItem.Type.LEGGINGS, 3);
                        map.put(ArmorItem.Type.CHESTPLATE, 4);
                        map.put(ArmorItem.Type.HELMET, 1);
                        map.put(ArmorItem.Type.BODY, 4);
                    }),
                    12,
                    SoundEvents.ARMOR_EQUIP_TURTLE,
                    () -> Ingredient.of(ModItems.PHI_CHITIN.get()),
                    List.of(new ArmorMaterial.Layer(EffecoriaMod.id("phi_chitin"))),
                    0.0f,
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
}
