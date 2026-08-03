package com.effecoria.client.render;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.effecoria.entity.RootCageEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Living GeckoLib roots scaled to the captive and loosely pinned to humanoid limbs when available. */
public class RootCageRenderer extends GeoEntityRenderer<RootCageEntity> {
    public RootCageRenderer(EntityRendererProvider.Context context) {
        super(context, new RootCageModel());
        this.shadowRadius = 0.0f;
    }

    @Override
    public RenderType getRenderType(
            RootCageEntity animatable,
            ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void preRender(
            PoseStack poseStack,
            RootCageEntity animatable,
            BakedGeoModel model,
            @Nullable MultiBufferSource bufferSource,
            @Nullable VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            int colour) {
        LivingEntity captive = resolveCaptive(animatable);
        RootSkeletonHints hints = RootSkeletonHints.forCaptive(captive, partialTick);
        // Pivot wrap bones onto biped / humanoid anchors (reference model space).
        applyWrapBone(model, "wrap_neck", hints.neckX, hints.neckY, hints.neckZ);
        applyWrapBone(model, "wrap_torso", hints.torsoX, hints.torsoY, hints.torsoZ);
        applyWrapBone(model, "wrap_arm_l", hints.armLX, hints.armLY, hints.armLZ);
        applyWrapBone(model, "wrap_arm_r", hints.armRX, hints.armRY, hints.armRZ);
        applyWrapBone(model, "wrap_leg_l", hints.legLX, hints.legLY, hints.legLZ);
        applyWrapBone(model, "wrap_leg_r", hints.legRX, hints.legRY, hints.legRZ);

        float sx = Mth.clamp(animatable.getCageWidth() / 0.9f, 0.55f, 3.2f);
        float sy = Mth.clamp(animatable.getCageHeight() / 1.8f, 0.55f, 3.2f);
        poseStack.scale(sx, sy, sx);
        super.preRender(
                poseStack,
                animatable,
                model,
                bufferSource,
                buffer,
                isReRender,
                partialTick,
                packedLight,
                packedOverlay,
                colour);
    }

    private static void applyWrapBone(BakedGeoModel model, String name, float x, float y, float z) {
        Optional<GeoBone> bone = model.getBone(name);
        if (bone.isEmpty()) {
            return;
        }
        bone.get().updatePivot(x, y, z);
    }

    @Override
    public void actuallyRender(
            PoseStack poseStack,
            RootCageEntity animatable,
            BakedGeoModel model,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            int colour) {
        float integrity = animatable.getIntegrityRatio();
        int alpha = Math.round(Mth.clamp(0.4f + 0.6f * integrity, 0.4f, 1f) * 255f);
        int tinted = (alpha << 24) | (colour & 0x00FFFFFF);
        super.actuallyRender(
                poseStack,
                animatable,
                model,
                renderType,
                bufferSource,
                buffer,
                isReRender,
                partialTick,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                tinted);
    }

    @Nullable
    private static LivingEntity resolveCaptive(RootCageEntity cage) {
        Optional<UUID> id = cage.getCaptiveId();
        if (id.isEmpty() || cage.level() == null) {
            return null;
        }
        UUID captiveId = id.get();
        AABB box = cage.getBoundingBox().inflate(3.0);
        for (Entity entity : cage.level().getEntities(cage, box)) {
            if (entity instanceof LivingEntity living && captiveId.equals(living.getUUID())) {
                return living;
            }
        }
        return null;
    }
}
