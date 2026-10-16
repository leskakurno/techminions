package com.techminions.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.techminions.MinionEntity;
import com.techminions.MinionType;
import com.techminions.TechMinions;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

public class MinionRenderer extends HumanoidMobRenderer<MinionEntity, HumanoidModel<MinionEntity>> {
    private static final Map<MinionType, ResourceLocation> TEXTURES = new EnumMap<>(MinionType.class);

    static {
        for (MinionType type : MinionType.values()) {
            TEXTURES.put(type, ResourceLocation.fromNamespaceAndPath(
                    TechMinions.MOD_ID, "textures/entity/minion_" + type.getSerializedName() + ".png"));
        }
    }

    public MinionRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(MinionEntity entity) {
        return TEXTURES.get(entity.getMinionType());
    }

    @Override
    protected void scale(MinionEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.8F, 0.8F, 0.8F);
    }
}
