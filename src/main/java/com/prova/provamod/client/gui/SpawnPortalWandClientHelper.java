package com.prova.provamod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpawnPortalWandClientHelper {

    public static void openScreen(InteractionHand hand, String currentMobId) {
        Minecraft.getInstance().setScreen(new SpawnPortalWandScreen(hand, currentMobId));
    }
}
