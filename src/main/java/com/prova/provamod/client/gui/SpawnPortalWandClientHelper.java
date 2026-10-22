package com.prova.provamod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

import java.util.List;

public class SpawnPortalWandClientHelper {

    public static void openScreen(InteractionHand hand, String currentMobId) {
        openScreen(hand, currentMobId, List.of());
    }

    public static void openScreen(InteractionHand hand, String currentMobId, List<String> favorites) {
        Minecraft mc = Minecraft.getInstance();
        PortalMobRegistry.ensureInitialized(mc.level);
        mc.setScreen(new SpawnPortalWandScreen(hand, currentMobId, favorites));
    }
}
