package com.lordlox.magicalwands.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

public class GamblingWandClientHelper {

    public static void openScreen(InteractionHand hand, int currentBet) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new GamblingWandScreen(hand, currentBet));
    }

    public static void triggerRollOverlay(int rollResult, int betAmount, Component rewardDescription) {
        GamblingD20Overlay.startRoll(rollResult, betAmount, rewardDescription);
    }
}
