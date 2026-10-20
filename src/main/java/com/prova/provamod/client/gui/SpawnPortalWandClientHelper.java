package com.prova.provamod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

public class SpawnPortalWandClientHelper {

    public static void openScreen(InteractionHand hand, String currentMobId) {
        Minecraft mc = Minecraft.getInstance();
        PortalMobRegistry.ensureInitialized(mc.level);
        mc.setScreen(new SpawnPortalWandScreen(hand, currentMobId));
    }
}
