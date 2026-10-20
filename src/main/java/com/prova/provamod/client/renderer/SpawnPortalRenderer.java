package com.prova.provamod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.prova.provamod.entity.SpawnPortalEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public class SpawnPortalRenderer extends EntityRenderer<SpawnPortalEntity, EntityRenderState> {

    public SpawnPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        // Portal visual presence is primarily rendered through the dynamic particle vortex and cosmic illumination
    }
}
