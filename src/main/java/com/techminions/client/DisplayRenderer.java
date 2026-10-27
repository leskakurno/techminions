package com.techminions.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.techminions.DisplayBlock;
import com.techminions.DisplayBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Рисует выставленные предметы над Пьедесталом, Витриной и Стойкой яиц: медленно вращаются. */
public class DisplayRenderer implements BlockEntityRenderer<DisplayBlockEntity> {
    private final ItemRenderer itemRenderer;

    public DisplayRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(DisplayBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        if (be.getLevel() == null) {
            return;
        }
        float time = be.getLevel().getGameTime() + partialTick;
        double height = be.getBlockState().getBlock() instanceof DisplayBlock block ? block.getKind().itemHeight() : 0.6;
        int lightAbove = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above());
        int count = be.size();
        for (int i = 0; i < count; i++) {
            ItemStack stack = be.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            double x = count == 1 ? 0.5 : 0.2 + i * 0.3;
            pose.pushPose();
            pose.translate(x, height + 0.1 * Math.sin(time / 20.0 + i), 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(time * 2.0F));
            float scale = count == 1 ? 0.55F : 0.4F;
            pose.scale(scale, scale, scale);
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, lightAbove, OverlayTexture.NO_OVERLAY, pose, buffer,
                    be.getLevel(), i);
            pose.popPose();
        }
    }
}
