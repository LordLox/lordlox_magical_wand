package com.prova.provamod;

import com.prova.provamod.client.renderer.BlackHoleRenderer;
import com.prova.provamod.client.renderer.SpawnPortalRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = Prova.MODID, dist = Dist.CLIENT)
public final class ProvaClient {
    public ProvaClient(IEventBus modEventBus) {
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::registerRenderers);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        // Client-specific initialization
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Prova.BLACK_HOLE.get(), BlackHoleRenderer::new);
        event.registerEntityRenderer(Prova.SPAWN_PORTAL.get(), SpawnPortalRenderer::new);
    }
}
