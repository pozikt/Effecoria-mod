package com.effecoria.content;

import com.effecoria.EffecoriaMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EffecoriaMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EFFECORIA_TAB = CREATIVE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.effecoria"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> ModItems.RESONANCE_FOCUS.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModItems.ESSENITE_ORE.get());
                        output.accept(ModItems.DEEPSLATE_ESSENITE_ORE.get());
                        output.accept(ModItems.GRANITE_ESSENITE_ORE.get());
                        output.accept(ModItems.ANDESITE_ESSENITE_ORE.get());
                        output.accept(ModItems.DIORITE_ESSENITE_ORE.get());
                        output.accept(ModItems.TUFF_ESSENITE_ORE.get());
                        output.accept(ModItems.BASALT_ESSENITE_ORE.get());
                        output.accept(ModItems.ESSONITE_BLOCK.get());
                        output.accept(ModItems.STAR_ESSONITE_BLOCK.get());
                        output.accept(ModItems.WHISPERING_SPIRE_VENT.get());
                        output.accept(ModItems.PARCHED_SAND.get());
                        output.accept(ModItems.ASH_SOIL.get());
                        output.accept(ModItems.PARCHED_SANDSTONE.get());
                        output.accept(ModItems.VITRIFIED_DIRT.get());
                        output.accept(ModItems.VITRIFIED_STONE.get());
                        output.accept(ModItems.VITRIFIED_SAND.get());
                        output.accept(ModItems.VITRIFIED_LOG.get());
                        output.accept(ModItems.VITRIFIED_BRANCHES.get());
                        output.accept(ModItems.VITRIFIED_GEYSER_CRACK.get());
                        output.accept(ModItems.PHI_STONE.get());
                        output.accept(ModItems.PHI_DIRT.get());
                        output.accept(ModItems.PHI_GRASS.get());
                        output.accept(ModItems.ESSONITE_CRYSTAL.get());
                        output.accept(ModItems.ESSONITE_CRYSTAL_BUD_SMALL.get());
                        output.accept(ModItems.ESSONITE_CRYSTAL_BUD_MEDIUM.get());
                        output.accept(ModItems.ESSONITE_CRYSTAL_BUD_LARGE.get());
                        output.accept(ModItems.OMEGA_CRYSTAL.get());
                        output.accept(ModItems.OMEGA_CRYSTAL_BUD_SMALL.get());
                        output.accept(ModItems.OMEGA_CRYSTAL_BUD_MEDIUM.get());
                        output.accept(ModItems.OMEGA_CRYSTAL_BUD_LARGE.get());
                        output.accept(ModItems.OMEGA_CRYSTAL_SHARD.get());
                        output.accept(ModItems.OMEGA_BLADES.get());
                        output.accept(ModItems.ROTTEN_MOSS.get());
                        output.accept(ModItems.ELDRITCH_BLOOD_PUDDLE.get());
                        output.accept(ModItems.DISTORTED_BONE.get());
                        output.accept(ModItems.ESSONITE_DRIPSTONE_BLOCK.get());
                        output.accept(ModItems.ESSONITE_POINTED.get());
                        output.accept(ModItems.PHI_GEYSER.get());
                        output.accept(ModItems.VOID_OBSIDIAN.get());
                        output.accept(ModItems.ESSONITE_CRUST.get());
                        output.accept(ModItems.PHI_BLADES.get());
                        output.accept(ModItems.PHI_LOG.get());
                        output.accept(ModItems.PHI_PLANKS.get());
                        output.accept(ModItems.PHI_GLASS.get());
                        output.accept(ModItems.PHI_LEAVES.get());
                        output.accept(ModItems.PHI_SAPLING.get());
                        output.accept(ModItems.ANCIENT_ESSENCE_WOOD.get());
                        output.accept(ModItems.GOLDEN_BARK.get());
                        output.accept(ModItems.PHI_SNARE_VINE.get());
                        output.accept(ModItems.PHI_NUT.get());
                        output.accept(ModItems.GIANT_PHI_NUT.get());
                        output.accept(ModItems.ANCIENT_HEARTWOOD.get());
                        output.accept(ModItems.PHI_WATER_BUCKET.get());
                        output.accept(ModItems.PURIFIED_PHI_WATER_BUCKET.get());
                        output.accept(ModItems.PHI_FLASK.get());
                        output.accept(ModItems.ESSENCE_DEW.get());
                        output.accept(ModItems.PHI_FLASK_WATER.get());
                        output.accept(ModItems.BLOOD_VIAL_EMPTY.get());
                        output.accept(ModItems.BLOOD_VIAL.get());
                        output.accept(ModItems.MAGE_BLOOD_VIAL.get());
                        output.accept(ModItems.WYVERN_BLOOD_VIAL.get());
                        output.accept(ModItems.OMEGA_BLOOD_VIAL.get());
                        output.accept(ModItems.PHI_PAPER.get());
                        output.accept(ModItems.GOLD_FILTER.get());
                        output.accept(ModItems.LEAD_FILTER.get());
                        output.accept(ModItems.LEAD_CLOAK.get());
                        output.accept(ModItems.GOLD_AMULET.get());
                        output.accept(ModItems.ESSENCE_PARASOL.get());
                        output.accept(ModItems.POTION_PHI_RESISTANCE.get());
                        output.accept(ModItems.ESSENCE_CLAY_SALVE.get());
                        output.accept(ModItems.LEAD_PILL.get());
                        output.accept(ModItems.POTION_OMEGA_CLEANSE.get());
                        output.accept(ModItems.ANTI_PHI_SERUM.get());
                        output.accept(ModItems.LUNG_RINSE.get());
                        output.accept(ModItems.ESSENTOCYTE_KIT.get());
                        output.accept(ModItems.PSI_RESONATOR_THERAPY.get());
                        output.accept(ModItems.ORKANUMN_STIMULANT.get());
                        output.accept(ModItems.OMEGA_AMPUTATION_SALVE.get());
                        output.accept(ModItems.MORTAR_AND_PESTLE.get());
                        output.accept(ModItems.ESSENCE_BURNER.get());
                        output.accept(ModItems.ESSENCE_ALEMBIC.get());
                        output.accept(ModItems.POTION_PHI_TONIC.get());
                        output.accept(ModItems.POTION_PHI_RESONANCE.get());
                        output.accept(ModItems.POTION_PHI_STIMULANT.get());
                        output.accept(ModItems.ESSONITE_SHARD.get());
                        output.accept(ModItems.PURE_ESSONITE.get());
                        output.accept(ModItems.STAR_ESSONITE.get());
                        output.accept(ModItems.PHI_CHITIN.get());
                        output.accept(ModItems.PHI_CHITIN_HELMET.get());
                        output.accept(ModItems.PHI_CHITIN_CHESTPLATE.get());
                        output.accept(ModItems.PHI_CHITIN_LEGGINGS.get());
                        output.accept(ModItems.PHI_CHITIN_BOOTS.get());
                        output.accept(ModItems.CRYSTAL_ESSONITE_HELMET.get());
                        output.accept(ModItems.CRYSTAL_ESSONITE_CHESTPLATE.get());
                        output.accept(ModItems.CRYSTAL_ESSONITE_LEGGINGS.get());
                        output.accept(ModItems.CRYSTAL_ESSONITE_BOOTS.get());
                        output.accept(ModItems.STAR_ESSONITE_HELMET.get());
                        output.accept(ModItems.STAR_ESSONITE_CHESTPLATE.get());
                        output.accept(ModItems.STAR_ESSONITE_LEGGINGS.get());
                        output.accept(ModItems.STAR_ESSONITE_BOOTS.get());
                        output.accept(ModItems.VOID_OBSIDIAN_INSERT.get());
                        output.accept(ModItems.PSI_KEY.get());
                        output.accept(ModItems.PHI_PHONEME_FIRMITAS.get());
                        output.accept(ModItems.PHI_PHONEME_UMBRA.get());
                        output.accept(ModItems.PHI_PHONEME_ABNEGATIO.get());
                        output.accept(ModItems.PHI_PHONEME_SERVARE.get());
                        output.accept(ModItems.PHI_PHONEME_CLAUSURA.get());
                        output.accept(ModItems.ESSENITE_DUST.get());
                        output.accept(ModItems.VITRIFIED_GLASS_SHARD.get());
                        output.accept(ModItems.VITRIFIED_GLASS_SWORD.get());
                        output.accept(ModItems.VITRIFIED_GLASS_PICKAXE.get());
                        output.accept(ModItems.VITRIFIED_GLASS_AXE.get());
                        output.accept(ModItems.VITRIFIED_GLASS_SHOVEL.get());
                        output.accept(ModItems.VITRIFIED_GOLEM_CORE.get());
                        output.accept(ModItems.RESONANCE_FOCUS.get());
                        output.accept(ModItems.PHI_CELL.get());
                        output.accept(ModItems.BREATHING_SCROLL.get());
                        output.accept(ModItems.MAGIC_PRIMER.get());
                        output.accept(ModItems.PHI_LARVA_SPAWN_EGG.get());
                        output.accept(ModItems.CRYSTAL_CRAB_SPAWN_EGG.get());
                        output.accept(ModItems.EIDOS_SPAWN_EGG.get());
                        output.accept(ModItems.VITRIFIED_GOLEM_SPAWN_EGG.get());
                        output.accept(ModItems.ESSENCE_WYVERN_SPAWN_EGG.get());
                        output.accept(ModItems.ROTFANG_MINK_SPAWN_EGG.get());
                        output.accept(ModItems.OMEGA_SHADE_SPAWN_EGG.get());
                        output.accept(ModItems.OMEGA_WORM_SPAWN_EGG.get());
                        output.accept(ModItems.PHI_ENT_SPAWN_EGG.get());
                        output.accept(ModItems.PHI_LEMUR_SPAWN_EGG.get());
                        output.accept(ModItems.WAILER_BAT_SPAWN_EGG.get());
                        output.accept(ModItems.GLASS_WORM_SPAWN_EGG.get());
                    })
                    .build());
}
