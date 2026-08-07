package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;

import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * Boots armor model declaring {@code 64×64} texture size so foot UVs match
 * {@code phi_chitin_layer_1.png}. Default 64×32 models sample the pauldron
 * half of the atlas and look like a waist plate.
 */
public class PhiChitinBootsArmorModel extends HumanoidArmorModel<LivingEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(EffecoriaMod.id("phi_chitin_boots"), "main");

    public PhiChitinBootsArmorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(1.0F), 0.0F);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
