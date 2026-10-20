package com.prova.provamod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.prova.provamod.entity.BlackHoleEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public class BlackHoleRenderer extends EntityRenderer<BlackHoleEntity, EntityRenderState> {

    public BlackHoleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        // The visual essence of the black hole is rendered through the high-density cosmic particle vortex and lighting
    }
}
