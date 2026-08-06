package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;

import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Chestplate armor model with protruding pauldron cubes (parented to body).
 * Texture: {@code textures/models/armor/phi_chitin_layer_1.png} at 64×64 —
 * vanilla body UV in the top 32 rows; pauldron cube nets at y≥32.
 */
public class PhiChitinChestArmorModel extends HumanoidArmorModel<LivingEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(EffecoriaMod.id("phi_chitin_chest"), "main");

    /** Cube size matching extras pack: 5×4×6 */
    private static final int PW = 5;
    private static final int PH = 4;
    private static final int PD = 6;

    public PhiChitinChestArmorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        // Match outer armor deformation so torso aligns with vanilla chestplate.
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(1.0F), 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.getChild("body");

        // Right shoulder (negative X): stick out from body side.
        body.addOrReplaceChild(
                "right_pauldron",
                CubeListBuilder.create()
                        .texOffs(0, 32)
                        .addBox(-9.0F, -1.5F, -3.0F, PW, PH, PD, new CubeDeformation(0.15F)),
                PartPose.ZERO);

        // Left shoulder (positive X).
        body.addOrReplaceChild(
                "left_pauldron",
                CubeListBuilder.create()
                        .texOffs(32, 32)
                        .addBox(4.0F, -1.5F, -3.0F, PW, PH, PD, new CubeDeformation(0.15F)),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }
}
