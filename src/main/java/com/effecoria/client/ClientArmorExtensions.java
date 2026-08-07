package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.client.render.PhiChitinBootsArmorModel;
import com.effecoria.client.render.PhiChitinChestArmorModel;
import com.effecoria.client.render.PhiChitinHelmetArmorModel;
import com.effecoria.content.ModItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientArmorExtensions {
    private ClientArmorExtensions() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(
                PhiChitinChestArmorModel.LAYER, PhiChitinChestArmorModel::createBodyLayer);
        event.registerLayerDefinition(
                PhiChitinHelmetArmorModel.LAYER, PhiChitinHelmetArmorModel::createBodyLayer);
        event.registerLayerDefinition(
                PhiChitinBootsArmorModel.LAYER, PhiChitinBootsArmorModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(
                new IClientItemExtensions() {
                    private PhiChitinChestArmorModel chestModel;

                    @Override
                    @SuppressWarnings("unchecked")
                    public HumanoidModel<?> getHumanoidArmorModel(
                            LivingEntity entity,
                            ItemStack stack,
                            EquipmentSlot slot,
                            HumanoidModel<?> original) {
                        if (chestModel == null) {
                            chestModel = new PhiChitinChestArmorModel(
                                    Minecraft.getInstance()
                                            .getEntityModels()
                                            .bakeLayer(PhiChitinChestArmorModel.LAYER));
                        }
                        ((HumanoidModel<LivingEntity>) original).copyPropertiesTo(chestModel);
                        chestModel.setAllVisible(false);
                        chestModel.body.visible = true;
                        chestModel.leftArm.visible = false;
                        chestModel.rightArm.visible = false;
                        return chestModel;
                    }
                },
                ModItems.PHI_CHITIN_CHESTPLATE.get());

        event.registerItem(
                new IClientItemExtensions() {
                    private PhiChitinHelmetArmorModel helmetModel;

                    @Override
                    @SuppressWarnings("unchecked")
                    public HumanoidModel<?> getHumanoidArmorModel(
                            LivingEntity entity,
                            ItemStack stack,
                            EquipmentSlot slot,
                            HumanoidModel<?> original) {
                        if (helmetModel == null) {
                            helmetModel = new PhiChitinHelmetArmorModel(
                                    Minecraft.getInstance()
                                            .getEntityModels()
                                            .bakeLayer(PhiChitinHelmetArmorModel.LAYER));
                        }
                        ((HumanoidModel<LivingEntity>) original).copyPropertiesTo(helmetModel);
                        helmetModel.setAllVisible(false);
                        helmetModel.head.visible = true;
                        helmetModel.hat.visible = false;
                        return helmetModel;
                    }
                },
                ModItems.PHI_CHITIN_HELMET.get());

        event.registerItem(
                new IClientItemExtensions() {
                    private PhiChitinBootsArmorModel bootsModel;

                    @Override
                    @SuppressWarnings("unchecked")
                    public HumanoidModel<?> getHumanoidArmorModel(
                            LivingEntity entity,
                            ItemStack stack,
                            EquipmentSlot slot,
                            HumanoidModel<?> original) {
                        if (bootsModel == null) {
                            bootsModel = new PhiChitinBootsArmorModel(
                                    Minecraft.getInstance()
                                            .getEntityModels()
                                            .bakeLayer(PhiChitinBootsArmorModel.LAYER));
                        }
                        ((HumanoidModel<LivingEntity>) original).copyPropertiesTo(bootsModel);
                        bootsModel.setAllVisible(false);
                        bootsModel.leftLeg.visible = true;
                        bootsModel.rightLeg.visible = true;
                        return bootsModel;
                    }
                },
                ModItems.PHI_CHITIN_BOOTS.get());
    }
}
