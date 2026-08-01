package com.effecoria.client.render;

import com.effecoria.entity.RootCageEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Static mangrove-root cage scaled to the captive's hitbox. */
public class RootCageRenderer extends EntityRenderer<RootCageEntity> {
    private static final BlockState ROOTS = Blocks.MANGROVE_ROOTS.defaultBlockState();
    private static final BlockState MUDDY = Blocks.MUDDY_MANGROVE_ROOTS.defaultBlockState();

    public RootCageRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(
            RootCageEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        float width = entity.getCageWidth();
        float height = entity.getCageHeight();
        float integrity = entity.getIntegrityRatio();
        int strands = Mth.clamp(Math.round(5 + width * 4.5f), 6, 14);
        // Damaged cages shed outer strands.
        int visible = Math.max(3, Math.round(strands * (0.45f + 0.55f * integrity)));
        double radius = width * 0.42;

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        float age = entity.tickCount + partialTick;

        poseStack.pushPose();
        // Entity origin is feet; lift slightly so roots sit in dirt.
        poseStack.translate(-0.5, 0.0, -0.5);

        for (int i = 0; i < visible; i++) {
            double angle = (Math.PI * 2.0 * i) / strands + age * 0.01;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            float lean = (float) Math.sin(angle * 2.0 + age * 0.03) * 8.0f;
            float strandH = height * (0.75f + 0.25f * integrity) * (0.85f + (i % 3) * 0.08f);

            poseStack.pushPose();
            poseStack.translate(x + 0.5, 0.0, z + 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees((float) (angle * (180.0 / Math.PI))));
            poseStack.mulPose(Axis.ZP.rotationDegrees(lean));
            float xz = Mth.clamp(0.35f + width * 0.12f, 0.35f, 0.85f) * (0.7f + 0.3f * integrity);
            poseStack.scale(xz, strandH, xz);
            BlockState state = (i % 3 == 0) ? MUDDY : ROOTS;
            dispatcher.renderSingleBlock(state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }

        // Low root mat under the feet.
        poseStack.pushPose();
        poseStack.translate(0.5, 0.02, 0.5);
        float mat = Mth.clamp(width * 0.9f, 0.8f, 2.4f);
        poseStack.scale(mat, 0.35f * integrity + 0.15f, mat);
        poseStack.translate(-0.5, 0.0, -0.5);
        dispatcher.renderSingleBlock(MUDDY, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RootCageEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/block/mangrove_roots.png");
    }
}
